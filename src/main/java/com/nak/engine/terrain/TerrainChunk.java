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

class TerrainChunk {
    private float[] vertices;
    private int[] indices;
    private boolean generated = false;
    private boolean visible = false;
    private boolean buffersCreated = false;

    private Vector3f position;
    private float size;
    private int level;

    // OpenGL buffer objects
    private int vao = 0;
    private int vbo = 0;
    private int ebo = 0;
    private int indexCount = 0;

    public TerrainChunk(Vector3f position, float size, int level) {
        this.position = new Vector3f(position);
        this.size = size;
        this.level = level;
    }

    public void generate() {
        if (generated) return;

        int resolution = Math.max(16, 64 >> level);
        int vertexSize = 8; // 3 position + 2 texCoord + 3 normal
        vertices = new float[resolution * resolution * vertexSize];
        indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // Generate vertices with proper normals and texture coordinates
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

                // Texture coordinates
                vertices[vertexIndex++] = x / (float) (resolution - 1);
                vertices[vertexIndex++] = z / (float) (resolution - 1);

                // Calculate normal (finite difference method)
                Vector3f normal = calculateNormal(worldX, worldZ);
                vertices[vertexIndex++] = normal.x;
                vertices[vertexIndex++] = normal.y;
                vertices[vertexIndex++] = normal.z;
            }
        }

        // Generate indices
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

        indexCount = indices.length;
        generated = true;
    }

    private Vector3f calculateNormal(float x, float z) {
        float offset = 0.5f;
        float heightL = getHeight(x - offset, z);
        float heightR = getHeight(x + offset, z);
        float heightD = getHeight(x, z - offset);
        float heightU = getHeight(x, z + offset);

        Vector3f normal = new Vector3f(heightL - heightR, 2.0f, heightD - heightU);
        return normal.normalize();
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
        int stride = 8 * Float.BYTES;

        // Position attribute (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinate attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normal attribute (location = 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        buffersCreated = true;
    }

    private float getHeight(float x, float z) {
        // Improved multi-octave noise
        float height = 0;
        float amplitude = 20.0f;
        float frequency = 0.01f;
        float lacunarity = 2.0f;
        float persistence = 0.5f;

        for (int i = 0; i < 6; i++) {
            height += amplitude * improvedNoise(x * frequency, z * frequency);
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return height;
    }

    private float improvedNoise(float x, float z) {
        // Better noise function with more natural patterns
        float n1 = (float) (Math.sin(x * 0.7) * Math.cos(z * 0.8) * 0.5);
        float n2 = (float) (Math.sin(x * 1.3) * Math.cos(z * 1.1) * 0.25);
        float n3 = (float) (Math.sin(x * 2.7) * Math.cos(z * 2.3) * 0.125);
        float n4 = (float) (Math.sin(x * 5.1) * Math.cos(z * 4.7) * 0.0625);

        return (n1 + n2 + n3 + n4) / (0.5f + 0.25f + 0.125f + 0.0625f);
    }

    public void render() {
        if (!generated || !visible || !buffersCreated) return;

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void renderLegacy() {
        if (!generated || !visible) return;

        // Fallback to immediate mode for compatibility
        glBegin(GL_TRIANGLES);
        for (int i = 0; i < indices.length; i += 3) {
            for (int j = 0; j < 3; j++) {
                int vertexIndex = indices[i + j] * 8;

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
}
