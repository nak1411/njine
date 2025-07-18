package com.nak.engine.terrain;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

class TerrainChunk {
    private float[] vertices;
    private int[] indices;
    private boolean generated = false;
    private boolean visible = false;
    private Vector3f position;
    private float size;
    private int level;

    public TerrainChunk(Vector3f position, float size, int level) {
        this.position = new Vector3f(position);
        this.size = size;
        this.level = level;
    }

    public void generate() {
        if (generated) return;

        int resolution = Math.max(16, 64 >> level); // Higher LOD = more detail
        vertices = new float[resolution * resolution * 3];
        indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // Generate vertices with noise
        int vertexIndex = 0;
        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                float worldX = position.x + (x / (float) (resolution - 1)) * size;
                float worldZ = position.z + (z / (float) (resolution - 1)) * size;
                float height = getHeight(worldX, worldZ);

                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = height;
                vertices[vertexIndex++] = worldZ;
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

        generated = true;
    }

    private float getHeight(float x, float z) {
        // Multi-octave noise for realistic terrain
        float height = 0;
        float amplitude = 20.0f;
        float frequency = 0.01f;

        for (int i = 0; i < 4; i++) {
            height += amplitude * noise(x * frequency, z * frequency);
            amplitude *= 0.5f;
            frequency *= 2.0f;
        }

        return height;
    }

    private float noise(float x, float z) {
        // Simple noise function (in practice, use a proper noise library)
        return (float) (Math.sin(x) * Math.cos(z) * 0.5 +
                Math.sin(x * 2.1) * Math.cos(z * 1.9) * 0.3 +
                Math.sin(x * 4.3) * Math.cos(z * 3.7) * 0.2);
    }

    public void render() {
        if (!generated || !visible) return;

        // Create buffers and populate them
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices);
        vertexBuffer.flip();

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices);
        indexBuffer.flip();

        // Enable vertex arrays
        glEnableClientState(GL_VERTEX_ARRAY);

        // Set vertex pointer
        glVertexPointer(3, GL_FLOAT, 0, vertexBuffer);

        // Draw elements
        glDrawElements(GL_TRIANGLES, indexBuffer);

        // Disable vertex arrays
        glDisableClientState(GL_VERTEX_ARRAY);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
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
}
