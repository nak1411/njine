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
    private volatile float[] vertices;
    private volatile int[] indices;
    private final Vector3f position;
    private final float size;
    private final int level;
    private volatile float maxHeight = Float.MIN_VALUE;
    private volatile float minHeight = Float.MAX_VALUE;

    // State flags - use volatile for thread safety
    private volatile boolean generated = false;
    private volatile boolean generating = false;
    private volatile boolean visible = false;
    private volatile boolean buffersCreated = false;
    private volatile boolean needsUpdate = false;

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
    private static final int MAX_RESOLUTION = 128; // Reduced for stability
    private static final int MIN_RESOLUTION = 16;  // Increased minimum

    // Caching for performance
    private final FastNoise noiseGenerator;
    private volatile CompletableFuture<Void> generationTask;

    private static final AtomicInteger activeChunks = new AtomicInteger(0);

    public TerrainChunk(Vector3f position, float size, int level) {
        this.position = new Vector3f(position);
        this.size = size;
        this.level = level;
        this.noiseGenerator = new FastNoise();
        activeChunks.incrementAndGet();

        System.out.println("Created terrain chunk at " + position + " size=" + size + " level=" + level);
    }

    /**
     * Asynchronously generate terrain chunk data
     */
    public CompletableFuture<Void> generateAsync() {
        if (generated || generating) {
            return generationTask != null ? generationTask : CompletableFuture.completedFuture(null);
        }

        generating = true;
        generationTask = CompletableFuture.runAsync(() -> {
                    try {
                        generateTerrain();
                        System.out.println("Generated terrain chunk at " + position + " successfully");
                    } catch (Exception e) {
                        System.err.println("Error generating terrain chunk at " + position + ": " + e.getMessage());
                        e.printStackTrace();
                        generating = false;
                        throw new RuntimeException(e);
                    }
                }, GENERATION_EXECUTOR)
                .whenComplete((result, throwable) -> {
                    generating = false;
                    if (throwable != null) {
                        System.err.println("Error in terrain generation task: " + throwable.getMessage());
                        throwable.printStackTrace();
                    }
                });

        return generationTask;
    }

    /**
     * Synchronous generation for immediate needs
     */
    public void generate() {
        if (generated || generating) return;

        try {
            generateTerrain();
            System.out.println("Generated terrain chunk at " + position + " synchronously");
        } catch (Exception e) {
            System.err.println("Error in synchronous terrain generation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateTerrain() {
        try {
            // Calculate adaptive resolution based on level and size
            int resolution = calculateResolution();
            System.out.println("Generating terrain with resolution: " + resolution);

            // Pre-calculate bounds for better culling
            calculatePreciseBounds(resolution);
            System.out.println("Calculated bounds: min=" + minHeight + " max=" + maxHeight);

            // Generate mesh data
            generateMeshData(resolution);
            System.out.println("Generated mesh data: " + (vertices != null ? vertices.length : 0) + " vertices, " +
                    (indices != null ? indices.length : 0) + " indices");

            generated = true;
            needsUpdate = true;

        } catch (Exception e) {
            System.err.println("Exception in generateTerrain: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private int calculateResolution() {
        // Adaptive resolution based on LOD level - ensure minimum viable resolution
        int baseResolution = MAX_RESOLUTION >> Math.min(level, 3);
        int resolution = Math.max(MIN_RESOLUTION, baseResolution);

        // Ensure resolution is power of 2 + 1 for better mesh generation
        if (resolution < 17) resolution = 17;
        else if (resolution < 33) resolution = 33;
        else if (resolution < 65) resolution = 65;
        else resolution = 129;

        return resolution;
    }

    private void calculatePreciseBounds(int resolution) {
        minHeight = Float.MAX_VALUE;
        maxHeight = Float.MIN_VALUE;

        // Sample at regular intervals across the chunk
        int sampleRes = Math.min(resolution, 32);
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

        // Ensure valid bounds
        if (minHeight == Float.MAX_VALUE) minHeight = 0.0f;
        if (maxHeight == Float.MIN_VALUE) maxHeight = 0.0f;

        // Add padding for safety
        float padding = Math.max(2.0f, (maxHeight - minHeight) * 0.1f);
        minHeight -= padding;
        maxHeight += padding;
    }

    private void generateMeshData(int resolution) {
        if (resolution < 2) {
            throw new IllegalArgumentException("Resolution must be at least 2");
        }

        // Vertex layout: pos(3) + tex(2) + normal(3) + tangent(3) + color(3) = 14 floats
        int vertexSize = 14;
        int vertexCount = resolution * resolution;
        int triangleCount = (resolution - 1) * (resolution - 1) * 2;

        vertices = new float[vertexCount * vertexSize];
        indices = new int[triangleCount * 3];

        System.out.println("Allocating " + vertices.length + " floats for vertices and " +
                indices.length + " ints for indices");

        // Generate vertices with enhanced data
        generateVerticesOptimized(resolution, vertexSize);

        // Generate indices for triangles
        generateOptimizedIndices(resolution);

        indexCount = indices.length;

        // Validate generated data
        if (vertices.length == 0 || indices.length == 0) {
            throw new RuntimeException("Failed to generate valid mesh data");
        }
    }

    private void generateVerticesOptimized(int resolution, int vertexSize) {
        float stepSize = size / (resolution - 1);
        int vertexIndex = 0;

        // Pre-calculate texture scale based on chunk size
        float texScale = Math.max(0.02f, 1.0f / size) * 8.0f; // Reduced scale for better tiling

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                float worldX = position.x + x * stepSize;
                float worldZ = position.z + z * stepSize;
                float height = generateHeight(worldX, worldZ);

                // Validate height
                if (!Float.isFinite(height)) {
                    height = 0.0f;
                }

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
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f tangent = new Vector3f(1, 0, 0);

        if (Math.abs(normal.dot(tangent)) > 0.9f) {
            tangent.set(0, 0, 1);
        }

        return normal.cross(tangent, new Vector3f()).normalize();
    }

    private Vector3f calculateEnhancedVertexColor(float height, Vector3f normal, float worldX, float worldZ) {
        float normalizedHeight = Math.max(0, Math.min(1, (height - minHeight) / Math.max(1.0f, maxHeight - minHeight)));
        float slope = Math.max(0, Math.min(1, 1.0f - normal.y));

        // Add noise for variation
        float variation = noiseGenerator.noise(worldX * 0.05f, worldZ * 0.05f) * 0.1f + 0.9f;
        variation = Math.max(0.5f, Math.min(1.5f, variation)); // Clamp variation

        Vector3f color = new Vector3f();

        if (height < -1.0f) {
            // Water/beach - blue to sandy
            float sandiness = Math.max(0, Math.min(1, (height + 5.0f) / 4.0f));
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

        // Apply variation and ensure valid range
        color.mul(variation);
        color.x = Math.max(0.05f, Math.min(1.0f, color.x));
        color.y = Math.max(0.05f, Math.min(1.0f, color.y));
        color.z = Math.max(0.05f, Math.min(1.0f, color.z));

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

                // Validate indices
                if (topLeft >= 0 && topRight >= 0 && bottomLeft >= 0 && bottomRight >= 0 &&
                        topLeft < resolution * resolution && topRight < resolution * resolution &&
                        bottomLeft < resolution * resolution && bottomRight < resolution * resolution) {

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
    }

    float generateHeight(float x, float z) {
        try {
            float height = noiseGenerator.generateTerrainHeight(x, z);

            // Ensure height is finite
            if (!Float.isFinite(height)) {
                return 0.0f;
            }

            return height;
        } catch (Exception e) {
            System.err.println("Error generating height at " + x + ", " + z + ": " + e.getMessage());
            return 0.0f;
        }
    }

    /**
     * Create OpenGL buffers - must be called on the OpenGL thread
     */
    public void createBuffers() {
        if (!generated || buffersCreated || vertices == null || indices == null) {
            return; // Skip if not ready, already created, or data was freed
        }

        try {
            System.out.println("Creating buffers for chunk at " + position);

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

            System.out.println("Successfully created buffers for chunk at " + position);

        } catch (Exception e) {
            System.err.println("Error creating buffers for terrain chunk at " + position + ": " + e.getMessage());
            e.printStackTrace();
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
        if (!isReadyToRender()) {
            return;
        }

        try {
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        } catch (Exception e) {
            System.err.println("Error rendering terrain chunk at " + position + ": " + e.getMessage());
        }
    }

    /**
     * Enhanced frustum culling with bounding box
     */
    public boolean isInFrustum(Vector3f cameraPos, Vector3f cameraFront, float fov, float far, float near) {
        try {
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
            if (toChunk.length() < 0.001f) return true; // Very close, always visible

            float dot = toChunk.normalize().dot(cameraFront);
            float halfFov = (float) Math.toRadians(fov * 0.5f);

            return dot > Math.cos(halfFov) - (chunkRadius / Math.max(distance, 0.001f));
        } catch (Exception e) {
            System.err.println("Error in frustum culling: " + e.getMessage());
            return false;
        }
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
        return generated && visible && buffersCreated && vao != 0 && indexCount > 0;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible && generated && !buffersCreated && vertices != null) {
            needsUpdate = true;
        }
    }

    public boolean needsBufferUpdate() {
        return needsUpdate && generated && !buffersCreated;
    }

    // Add memory monitoring
    public static int getActiveChunkCount() {
        return activeChunks.get();
    }

    public void cleanup() {
        try {
            if (buffersCreated && vao != 0) {
                glDeleteVertexArrays(vao);
                glDeleteBuffers(vbo);
                glDeleteBuffers(ebo);
                buffersCreated = false;
                vao = 0;
                vbo = 0;
                ebo = 0;
            }

            if (generationTask != null && !generationTask.isDone()) {
                generationTask.cancel(true);
            }

            vertices = null;
            indices = null;

            activeChunks.decrementAndGet();
        } catch (Exception e) {
            System.err.println("Error during chunk cleanup: " + e.getMessage());
        }
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
        try {
            GENERATION_EXECUTOR.shutdown();
        } catch (Exception e) {
            System.err.println("Error shutting down generation executor: " + e.getMessage());
        }
    }

    /**
     * Fast noise generator for terrain generation
     */
    private static class FastNoise {
        public float generateTerrainHeight(float x, float z) {
            try {
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

                // Ensure height is reasonable
                height = Math.max(-50.0f, Math.min(100.0f, height));

                return height;
            } catch (Exception e) {
                System.err.println("Error in noise generation: " + e.getMessage());
                return 0.0f;
            }
        }

        public float noise(float x, float z) {
            try {
                // Improved noise function with better distribution
                x *= 0.01f;
                z *= 0.01f;

                float result = (float) (
                        Math.sin(x * 0.754f + Math.cos(z * 0.234f)) *
                                Math.cos(z * 0.832f + Math.sin(x * 0.126f)) * 0.5f +
                                Math.sin(x * 1.347f + Math.cos(z * 1.726f)) *
                                        Math.cos(z * 1.194f + Math.sin(x * 0.937f)) * 0.3f +
                                Math.sin(x * 2.756f + Math.cos(z * 2.193f)) *
                                        Math.cos(z * 2.347f + Math.sin(x * 1.847f)) * 0.2f
                );

                // Clamp result to reasonable range
                return Math.max(-1.0f, Math.min(1.0f, result));
            } catch (Exception e) {
                return 0.0f;
            }
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