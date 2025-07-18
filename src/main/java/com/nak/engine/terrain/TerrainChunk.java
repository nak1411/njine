package com.nak.engine.terrain;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class TerrainChunk {
    // Static thread pool for async generation
    private static final ExecutorService GENERATION_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2)
    );

    // Chunk data
    private float[] vertices;
    private int[] indices;
    private Vector3f position;
    private float size;
    private int level;
    private float maxHeight = Float.MIN_VALUE;
    private float minHeight = Float.MAX_VALUE;

    // State flags
    private volatile boolean generated = false;
    private volatile boolean generating = false;
    private boolean visible = false;
    private boolean buffersCreated = false;
    private boolean needsUpdate = false;

    // OpenGL objects
    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int indexCount = 0;

    // Enhanced noise parameters
    private static final float BASE_AMPLITUDE = 30.0f;
    private static final float BASE_FREQUENCY = 0.005f;
    private static final int OCTAVES = 7;
    private static final float PERSISTENCE = 0.55f;
    private static final float LACUNARITY = 2.1f;

    // LOD settings
    private static final int MAX_RESOLUTION = 256;
    private static final int MIN_RESOLUTION = 32;

    // Caching for performance
    private final FastNoise noiseGenerator;
    private CompletableFuture<Void> generationTask;

    private static final AtomicInteger activeChunks = new AtomicInteger(0);

    public TerrainChunk(Vector3f position, float size, int level) {
        this.position = new Vector3f(position);
        this.size = size;
        this.level = level;
        this.noiseGenerator = new FastNoise();
        activeChunks.incrementAndGet();
    }

    /**
     * Asynchronously generate terrain chunk data
     */
    public CompletableFuture<Void> generateAsync() {
        if (generated || generating) {
            return generationTask != null ? generationTask : CompletableFuture.completedFuture(null);
        }

        generating = true;
        generationTask = CompletableFuture.runAsync(this::generateTerrain, GENERATION_EXECUTOR)
                .whenComplete((result, throwable) -> {
                    generating = false;
                    if (throwable != null) {
                        System.err.println("Error generating terrain chunk: " + throwable.getMessage());
                    }
                });

        return generationTask;
    }

    /**
     * Synchronous generation for immediate needs
     */
    public void generate() {
        if (generated) return;
        generateTerrain();
    }

    private void generateTerrain() {
        // Calculate adaptive resolution based on level and size
        int resolution = calculateResolution();

        // Pre-calculate bounds for better culling
        calculatePreciseBounds(resolution);

        // Generate mesh data
        generateMeshData(resolution);

        generated = true;
        needsUpdate = true;
    }

    private int calculateResolution() {
        // Adaptive resolution based on LOD level
        int baseResolution = MAX_RESOLUTION >> Math.min(level, 3);
        return Math.max(MIN_RESOLUTION, baseResolution);
    }

    private void calculatePreciseBounds(int resolution) {
        minHeight = Float.MAX_VALUE;
        maxHeight = Float.MIN_VALUE;

        // Sample at higher resolution for precise bounds
        int sampleRes = Math.min(resolution, 64);
        float sampleStep = size / (sampleRes - 1);

        for (int z = 0; z < sampleRes; z++) {
            for (int x = 0; x < sampleRes; x++) {
                float worldX = position.x + x * sampleStep;
                float worldZ = position.z + z * sampleStep;
                float height = generateHeight(worldX, worldZ);

                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }

        // Add padding for safety
        float padding = (maxHeight - minHeight) * 0.1f + 2.0f;
        minHeight -= padding;
        maxHeight += padding;
    }

    private void generateMeshData(int resolution) {
        // Vertex layout: pos(3) + tex(2) + normal(3) + tangent(3) + color(3) = 14 floats
        int vertexSize = 14;
        vertices = new float[resolution * resolution * vertexSize];
        indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // Generate vertices with enhanced data
        generateVerticesOptimized(resolution, vertexSize);

        // Generate indices for triangle strips (more cache-friendly)
        generateOptimizedIndices(resolution);

        indexCount = indices.length;
    }

    private void generateVerticesOptimized(int resolution, int vertexSize) {
        float stepSize = size / (resolution - 1);
        int vertexIndex = 0;

        // Pre-calculate texture scale based on chunk size
        float texScale = Math.max(0.02f, 1.0f / size) * 16.0f;

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                float worldX = position.x + x * stepSize;
                float worldZ = position.z + z * stepSize;
                float height = generateHeight(worldX, worldZ);

                // Position
                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = height;
                vertices[vertexIndex++] = worldZ;

                // Texture coordinates with proper tiling
                vertices[vertexIndex++] = (float) x / (resolution - 1) * texScale;
                vertices[vertexIndex++] = (float) z / (resolution - 1) * texScale;

                // Calculate normal using finite differences
                Vector3f normal = calculateNormalOptimized(worldX, worldZ, stepSize);
                vertices[vertexIndex++] = normal.x;
                vertices[vertexIndex++] = normal.y;
                vertices[vertexIndex++] = normal.z;

                // Calculate tangent for normal mapping
                Vector3f tangent = calculateTangent(normal);
                vertices[vertexIndex++] = tangent.x;
                vertices[vertexIndex++] = tangent.y;
                vertices[vertexIndex++] = tangent.z;

                // Enhanced vertex color based on height, slope, and features
                Vector3f color = calculateEnhancedVertexColor(height, normal, worldX, worldZ);
                vertices[vertexIndex++] = color.x;
                vertices[vertexIndex++] = color.y;
                vertices[vertexIndex++] = color.z;
            }
        }
    }

    /**
     * Improved vertex color calculation with height-based biomes
     */
    private Vector3f calculateVertexColor(float height, Vector3f normal, float worldX, float worldZ) {
        // Normalize height for color calculation
        float normalizedHeight = Math.max(0, Math.min(1, (height + 10) / 50.0f));
        float slope = Math.max(0, 1.0f - normal.y);

        // Base color from height
        Vector3f color = new Vector3f();

        if (height < -2.0f) {
            // Deep water - blue
            color.set(0.1f, 0.3f, 0.8f);
        } else if (height < 0.0f) {
            // Shallow water/beach - blue to sand transition
            float t = (height + 2.0f) / 2.0f;
            color.set(0.1f + t * 0.5f, 0.3f + t * 0.4f, 0.8f - t * 0.3f);
        } else if (height < 5.0f) {
            // Grassland - green
            color.set(0.2f, 0.6f, 0.1f);
        } else if (height < 15.0f) {
            // Hills - green to brown transition
            float t = (height - 5.0f) / 10.0f;
            color.set(0.2f + t * 0.3f, 0.6f - t * 0.2f, 0.1f + t * 0.1f);
        } else if (height < 25.0f) {
            // Mountains - brown/rock
            color.set(0.5f, 0.4f, 0.3f);
        } else {
            // Snow caps - white
            float snowAmount = Math.min(1.0f, (height - 25.0f) / 10.0f);
            color.set(0.5f + snowAmount * 0.4f, 0.4f + snowAmount * 0.5f, 0.3f + snowAmount * 0.6f);
        }

        // Adjust for slope (rocky areas are darker)
        if (slope > 0.3f) {
            float rockiness = Math.min(1.0f, slope * 2.0f);
            color.mul(1.0f - rockiness * 0.3f);
            color.x += rockiness * 0.2f; // Add some brown
        }

        // Add some random variation
        float noise = (float) (Math.sin(worldX * 0.1f) * Math.cos(worldZ * 0.1f)) * 0.1f + 0.9f;
        color.mul(noise);

        // Clamp to valid range
        color.x = Math.max(0, Math.min(1, color.x));
        color.y = Math.max(0, Math.min(1, color.y));
        color.z = Math.max(0, Math.min(1, color.z));

        return color;
    }


    private Vector3f calculateNormalOptimized(float x, float z, float stepSize) {
        float h = stepSize * 0.5f; // Half step for better accuracy

        // Sample heights in 4 directions
        float hL = generateHeight(x - h, z);     // Left
        float hR = generateHeight(x + h, z);     // Right
        float hD = generateHeight(x, z - h);     // Down
        float hU = generateHeight(x, z + h);     // Up

        // Calculate gradients
        float dX = (hR - hL) / (2.0f * h);
        float dZ = (hU - hD) / (2.0f * h);

        // Create normal vector
        Vector3f normal = new Vector3f(-dX, 1.0f, -dZ);
        return normal.normalize();
    }

    private Vector3f calculateTangent(Vector3f normal) {
        // Calculate tangent vector for normal mapping
        Vector3f tangent = new Vector3f(1, 0, 0);
        if (Math.abs(normal.x) > 0.9f) {
            tangent.set(0, 1, 0);
        }
        return normal.cross(tangent, new Vector3f()).normalize();
    }

    private Vector3f calculateEnhancedVertexColor(float height, Vector3f normal, float worldX, float worldZ) {
        float normalizedHeight = (height - minHeight) / Math.max(1.0f, maxHeight - minHeight);
        float slope = 1.0f - normal.y;

        // Add noise for variation
        float variation = noiseGenerator.noise(worldX * 0.1f, worldZ * 0.1f) * 0.1f + 0.9f;

        Vector3f color = new Vector3f();

        if (height < -1.0f) {
            // Water/beach - blue to sandy
            float sandiness = Math.max(0, (height + 5.0f) / 4.0f);
            color.set(
                    0.2f + sandiness * 0.6f,
                    0.4f + sandiness * 0.4f,
                    0.8f - sandiness * 0.4f
            );
        } else if (height < 8.0f) {
            // Grassland with some rocks
            float rockiness = slope * 0.6f + normalizedHeight * 0.2f;
            color.set(
                    0.15f + rockiness * 0.4f,
                    0.5f - rockiness * 0.1f,
                    0.1f + rockiness * 0.1f
            );
        } else if (height < 20.0f) {
            // Mixed terrain - grass to rock transition
            float transition = (height - 8.0f) / 12.0f;
            float rockiness = slope * 0.8f + transition * 0.5f;
            color.set(
                    0.3f + rockiness * 0.3f,
                    0.4f - rockiness * 0.1f,
                    0.2f + rockiness * 0.1f
            );
        } else if (height < 35.0f) {
            // Rocky terrain
            color.set(
                    0.5f + slope * 0.2f,
                    0.4f + slope * 0.1f,
                    0.3f + slope * 0.05f
            );
        } else {
            // Snow-capped peaks
            float snowCover = Math.min(1.0f, (height - 30.0f) / 10.0f);
            color.set(
                    0.4f + snowCover * 0.5f,
                    0.4f + snowCover * 0.5f,
                    0.5f + snowCover * 0.4f
            );
        }

        // Apply variation
        color.mul(variation);

        return color;
    }

    private void generateOptimizedIndices(int resolution) {
        int indexIndex = 0;

        for (int z = 0; z < resolution - 1; z++) {
            for (int x = 0; x < resolution - 1; x++) {
                int topLeft = z * resolution + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * resolution + x;
                int bottomRight = bottomLeft + 1;

                // Create triangles in a way that's more cache-friendly
                indices[indexIndex++] = topLeft;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = topRight;

                indices[indexIndex++] = topRight;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = bottomRight;
            }
        }
    }

    float generateHeight(float x, float z) {
        return noiseGenerator.generateTerrainHeight(x, z);
    }

    /**
     * Create OpenGL buffers - must be called on the OpenGL thread
     */
    public void createBuffers() {
        if (!generated || buffersCreated || vertices == null || indices == null) {
            return; // Skip if not ready, already created, or data was freed
        }

        try {
            // Generate VAO
            vao = glGenVertexArrays();
            if (vao == 0) {
                throw new RuntimeException("Failed to generate VAO");
            }
            glBindVertexArray(vao);

            // Create and upload vertex data
            vbo = glGenBuffers();
            if (vbo == 0) {
                throw new RuntimeException("Failed to generate VBO");
            }
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
            vertexBuffer.put(vertices);
            vertexBuffer.flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

            // Create and upload index data
            ebo = glGenBuffers();
            if (ebo == 0) {
                throw new RuntimeException("Failed to generate EBO");
            }
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
            indexBuffer.put(indices);
            indexBuffer.flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

            // Setup vertex attributes
            setupVertexAttributes();

            glBindVertexArray(0);
            buffersCreated = true;
            needsUpdate = false;

            // Free CPU memory after successful GPU upload
            vertices = null;
            indices = null;

        } catch (Exception e) {
            System.err.println("Error creating buffers for terrain chunk: " + e.getMessage());
            cleanup(); // Clean up any partially created resources
        }
    }

    private void setupVertexAttributes() {
        int stride = 14 * Float.BYTES;

        // Position (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinates (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normal (location = 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Tangent (location = 3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        // Color (location = 4)
        glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * Float.BYTES);
        glEnableVertexAttribArray(4);
    }

    /**
     * Render the terrain chunk
     */
    public void render() {
        if (!isReadyToRender()) return;

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Enhanced frustum culling with bounding box
     */
    public boolean isInFrustum(Vector3f cameraPos, Vector3f cameraFront, float fov, float far, float near) {
        // Get chunk bounds
        Vector3f min = new Vector3f(position.x, minHeight, position.z);
        Vector3f max = new Vector3f(position.x + size, maxHeight, position.z + size);
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);

        // Distance culling
        float distance = cameraPos.distance(center);
        float chunkRadius = center.distance(max);

        if (distance - chunkRadius > far) return false;
        if (distance + chunkRadius < near) return false;

        // Frustum culling (simplified)
        Vector3f toChunk = new Vector3f(center).sub(cameraPos);
        float dot = toChunk.normalize().dot(cameraFront);
        float halfFov = (float) Math.toRadians(fov * 0.5f);

        return dot > Math.cos(halfFov) - (chunkRadius / distance);
    }

    /**
     * Get height at specific world coordinates using bilinear interpolation
     */
    public float getHeightAt(float worldX, float worldZ) {
        if (!generated || !isInBounds(worldX, worldZ)) {
            return generateHeight(worldX, worldZ); // Fallback to noise
        }

        // If we don't have vertex data anymore (freed after GPU upload), use noise
        if (vertices == null) {
            return generateHeight(worldX, worldZ);
        }

        // TODO: Implement bilinear interpolation of vertex heights
        // For now, return noise-based height
        return generateHeight(worldX, worldZ);
    }

    private boolean isInBounds(float worldX, float worldZ) {
        return worldX >= position.x && worldX <= position.x + size &&
                worldZ >= position.z && worldZ <= position.z + size;
    }

    public boolean isReadyToRender() {
        return generated && visible && buffersCreated;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible && generated && !buffersCreated && vertices != null) {
            needsUpdate = true;
        }
    }

    public boolean needsBufferUpdate() {
        return needsUpdate;
    }

    // Add memory monitoring
    public static int getActiveChunkCount() {
        return activeChunks.get();
    }

    public void cleanup() {
        if (buffersCreated) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            buffersCreated = false;
        }

        if (generationTask != null && !generationTask.isDone()) {
            generationTask.cancel(true);
        }

        vertices = null;
        indices = null;

        activeChunks.decrementAndGet();
    }

    // Getters
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public float getSize() {
        return size;
    }

    public int getLevel() {
        return level;
    }

    public boolean isGenerated() {
        return generated;
    }

    public boolean isGenerating() {
        return generating;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean areBuffersCreated() {
        return buffersCreated;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public float getMinHeight() {
        return minHeight;
    }

    /**
     * Shutdown the static thread pool - call this when the application exits
     */
    public static void shutdown() {
        GENERATION_EXECUTOR.shutdown();
    }

    /**
     * Fast noise generator for terrain generation
     */
    private static class FastNoise {
        public float generateTerrainHeight(float x, float z) {
            float height = 0;
            float amplitude = BASE_AMPLITUDE;
            float frequency = BASE_FREQUENCY;

            // Primary terrain layers
            for (int i = 0; i < OCTAVES; i++) {
                height += amplitude * noise(x * frequency, z * frequency);
                amplitude *= PERSISTENCE;
                frequency *= LACUNARITY;
            }

            // Add terrain features
            height += ridgedNoise(x * 0.003f, z * 0.003f) * 20.0f;
            height += billowyNoise(x * 0.015f, z * 0.015f) * 10.0f;

            // Add fine detail
            height += noise(x * 0.05f, z * 0.05f) * 2.0f;

            return height;
        }

        public float noise(float x, float z) {
            // Improved noise function with better distribution
            x *= 0.01f;
            z *= 0.01f;

            return (float) (
                    Math.sin(x * 0.754f + Math.cos(z * 0.234f)) *
                            Math.cos(z * 0.832f + Math.sin(x * 0.126f)) * 0.5f +
                            Math.sin(x * 1.347f + Math.cos(z * 1.726f)) *
                                    Math.cos(z * 1.194f + Math.sin(x * 0.937f)) * 0.3f +
                            Math.sin(x * 2.756f + Math.cos(z * 2.193f)) *
                                    Math.cos(z * 2.347f + Math.sin(x * 1.847f)) * 0.2f
            );
        }

        private float ridgedNoise(float x, float z) {
            float n = noise(x, z);
            return 1.0f - Math.abs(n);
        }

        private float billowyNoise(float x, float z) {
            return Math.abs(noise(x, z));
        }
    }
}