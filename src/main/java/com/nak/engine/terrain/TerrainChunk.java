package com.nak.engine.terrain;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class TerrainChunk {
    private float[] vertices;
    private int[] indices;
    private boolean generated = false;
    private boolean visible = false;
    private boolean buffersCreated = false;

    private Vector3f position;
    private float size;
    private int level;
    private float maxHeight = 0;
    private float minHeight = 0;

    // OpenGL buffer objects
    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int indexCount = 0;

    // Improved noise parameters
    private static final float AMPLITUDE = 25.0f;
    private static final float FREQUENCY = 0.008f;
    private static final int OCTAVES = 6;
    private static final float PERSISTENCE = 0.5f;
    private static final float LACUNARITY = 2.0f;

    public TerrainChunk(Vector3f position, float size, int level) {
        this.position = new Vector3f(position);
        this.size = size;
        this.level = level;
    }

    public void generate() {
        if (generated) return;

        // Adaptive resolution based on level
        int resolution = Math.max(32, 128 >> level);
        int vertexSize = 11; // 3 pos + 2 tex + 3 norm + 3 color
        vertices = new float[resolution * resolution * vertexSize];
        indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // Calculate height bounds for this chunk
        calculateHeightBounds();

        // Generate vertices
        generateVertices(resolution);

        // Generate indices
        generateIndices(resolution);

        indexCount = indices.length;
        generated = true;
    }

    private void calculateHeightBounds() {
        minHeight = Float.MAX_VALUE;
        maxHeight = Float.MIN_VALUE;

        // Sample heights at chunk corners and center
        Vector3f[] samplePoints = {
                new Vector3f(position.x, 0, position.z),
                new Vector3f(position.x + size, 0, position.z),
                new Vector3f(position.x, 0, position.z + size),
                new Vector3f(position.x + size, 0, position.z + size),
                new Vector3f(position.x + size / 2, 0, position.z + size / 2)
        };

        for (Vector3f point : samplePoints) {
            float height = getHeight(point.x, point.z);
            minHeight = Math.min(minHeight, height);
            maxHeight = Math.max(maxHeight, height);
        }

        // Add some padding
        minHeight -= 5.0f;
        maxHeight += 5.0f;
    }

    private void generateVertices(int resolution) {
        int vertexIndex = 0;

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                float worldX = position.x + (x / (float) (resolution - 1)) * size;
                float worldZ = position.z + (z / (float) (resolution - 1)) * size;
                float height = getHeight(worldX, worldZ);

                // Position
                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = height;
                vertices[vertexIndex++] = worldZ;

                // Texture coordinates (with tiling)
                float texScale = 0.1f; // Adjust for texture tiling
                vertices[vertexIndex++] = (x / (float) (resolution - 1)) * texScale;
                vertices[vertexIndex++] = (z / (float) (resolution - 1)) * texScale;

                // Calculate normal using cross product method
                Vector3f normal = calculateNormal(worldX, worldZ);
                vertices[vertexIndex++] = normal.x;
                vertices[vertexIndex++] = normal.y;
                vertices[vertexIndex++] = normal.z;

                // Vertex color based on height and slope
                Vector3f color = calculateVertexColor(height, normal);
                vertices[vertexIndex++] = color.x;
                vertices[vertexIndex++] = color.y;
                vertices[vertexIndex++] = color.z;
            }
        }
    }

    private Vector3f calculateNormal(float x, float z) {
        float offset = 1.0f;

        // Sample neighboring heights
        float heightL = getHeight(x - offset, z);
        float heightR = getHeight(x + offset, z);
        float heightD = getHeight(x, z - offset);
        float heightU = getHeight(x, z + offset);

        // Calculate normal using finite differences
        Vector3f normal = new Vector3f(heightL - heightR, 2.0f * offset, heightD - heightU);
        return normal.normalize();
    }

    private Vector3f calculateVertexColor(float height, Vector3f normal) {
        // Height-based coloring
        float normalizedHeight = (height - minHeight) / Math.max(1.0f, maxHeight - minHeight);

        // Slope-based coloring (steeper = rockier)
        float slope = 1.0f - normal.y; // 0 = flat, 1 = vertical

        Vector3f color = new Vector3f();

        if (height < -2.0f) {
            // Water/shore - blue tint
            color.set(0.3f, 0.5f, 0.8f);
        } else if (height < 5.0f) {
            // Low areas - green grass
            color.set(0.2f + slope * 0.3f, 0.6f - slope * 0.2f, 0.1f);
        } else if (height < 15.0f) {
            // Mid areas - mixed grass/rock
            float rockiness = slope * 0.7f + normalizedHeight * 0.3f;
            color.set(0.4f + rockiness * 0.3f, 0.5f - rockiness * 0.2f, 0.2f);
        } else if (height < 25.0f) {
            // High areas - rocky
            color.set(0.5f + slope * 0.2f, 0.4f + slope * 0.1f, 0.3f);
        } else {
            // Peak areas - snow
            color.set(0.8f + slope * 0.1f, 0.8f + slope * 0.1f, 0.9f);
        }

        return color;
    }

    private void generateIndices(int resolution) {
        int indexIndex = 0;
        for (int z = 0; z < resolution - 1; z++) {
            for (int x = 0; x < resolution - 1; x++) {
                int topLeft = z * resolution + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * resolution + x;
                int bottomRight = bottomLeft + 1;

                // Triangle 1
                indices[indexIndex++] = topLeft;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = topRight;

                // Triangle 2
                indices[indexIndex++] = topRight;
                indices[indexIndex++] = bottomLeft;
                indices[indexIndex++] = bottomRight;
            }
        }
    }

    public void createBuffers() {
        if (!generated || buffersCreated) return;

        // Generate VAO
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Generate and bind VBO
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices);
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Generate and bind EBO
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices);
        indexBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Configure vertex attributes
        int stride = 11 * Float.BYTES;

        // Position attribute (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinate attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normal attribute (location = 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Color attribute (location = 3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
        buffersCreated = true;
    }

    private float getHeight(float x, float z) {
        return generatePerlinNoise(x, z);
    }

    private float generatePerlinNoise(float x, float z) {
        float height = 0;
        float amplitude = AMPLITUDE;
        float frequency = FREQUENCY;

        for (int i = 0; i < OCTAVES; i++) {
            height += amplitude * improvedNoise(x * frequency, z * frequency);
            amplitude *= PERSISTENCE;
            frequency *= LACUNARITY;
        }

        // Add some interesting features
        height += ridgedNoise(x * 0.005f, z * 0.005f) * 15.0f;
        height += billowyNoise(x * 0.02f, z * 0.02f) * 8.0f;

        return height;
    }

    private float improvedNoise(float x, float z) {
        // Improved noise with better distribution
        float n1 = (float) (Math.sin(x * 0.7 + Math.cos(z * 0.3)) * Math.cos(z * 0.8 + Math.sin(x * 0.2)));
        float n2 = (float) (Math.sin(x * 1.3 + Math.cos(z * 1.7)) * Math.cos(z * 1.1 + Math.sin(x * 0.9)));
        float n3 = (float) (Math.sin(x * 2.7 + Math.cos(z * 2.1)) * Math.cos(z * 2.3 + Math.sin(x * 1.8)));
        float n4 = (float) (Math.sin(x * 5.1 + Math.cos(z * 4.3)) * Math.cos(z * 4.7 + Math.sin(x * 3.2)));

        return (n1 * 0.5f + n2 * 0.25f + n3 * 0.125f + n4 * 0.0625f);
    }

    private float ridgedNoise(float x, float z) {
        // Ridged noise for mountain ridges
        float noise = improvedNoise(x, z);
        return 1.0f - Math.abs(noise);
    }

    private float billowyNoise(float x, float z) {
        // Billowy noise for rolling hills
        return Math.abs(improvedNoise(x, z));
    }

    public void render() {
        if (!generated || !visible || !buffersCreated) return;

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void renderLegacy() {
        if (!generated || !visible) return;

        // Enhanced legacy rendering with vertex colors
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < indices.length; i += 3) {
            for (int j = 0; j < 3; j++) {
                int vertexIndex = indices[i + j] * 11;

                // Color
                glColor3f(vertices[vertexIndex + 8], vertices[vertexIndex + 9], vertices[vertexIndex + 10]);

                // Normal
                glNormal3f(vertices[vertexIndex + 5], vertices[vertexIndex + 6], vertices[vertexIndex + 7]);

                // Texture coordinates
                glTexCoord2f(vertices[vertexIndex + 3], vertices[vertexIndex + 4]);

                // Position
                glVertex3f(vertices[vertexIndex], vertices[vertexIndex + 1], vertices[vertexIndex + 2]);
            }
        }
        glEnd();
    }

    // Frustum culling check
    public boolean isInFrustum(Vector3f cameraPos, Vector3f cameraFront, float fov, float far) {
        // Simple distance-based culling for now
        Vector3f chunkCenter = new Vector3f(position.x + size / 2, (maxHeight + minHeight) / 2, position.z + size / 2);
        float distance = cameraPos.distance(chunkCenter);
        float chunkRadius = (float) Math.sqrt(size * size + (maxHeight - minHeight) * (maxHeight - minHeight));

        return distance - chunkRadius < far;
    }

    public void cleanup() {
        if (buffersCreated) {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            buffersCreated = false;
        }
    }

    // Getters and setters
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible && generated && !buffersCreated) {
            createBuffers();
        }
    }

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

    public boolean areBuffersCreated() {
        return buffersCreated;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public float getMinHeight() {
        return minHeight;
    }

    // Get height at specific world coordinates (for collision detection)
    public float getHeightAt(float worldX, float worldZ) {
        if (!generated || worldX < position.x || worldX > position.x + size ||
                worldZ < position.z || worldZ > position.z + size) {
            return 0; // Outside chunk bounds
        }
        return getHeight(worldX, worldZ);
    }
}
