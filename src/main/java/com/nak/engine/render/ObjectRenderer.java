package com.nak.engine.render;

import static org.lwjgl.opengl.GL11.*;

class ObjectRenderer {
    public void renderCube() {
        glBegin(GL_TRIANGLES);

        // Define cube vertices and normals
        float[][] vertices = {
                // Front face
                {-0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, 0.5f}, {0.5f, 0.5f, 0.5f},
                {-0.5f, -0.5f, 0.5f}, {0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f},
                // Back face
                {-0.5f, -0.5f, -0.5f}, {-0.5f, 0.5f, -0.5f}, {0.5f, 0.5f, -0.5f},
                {-0.5f, -0.5f, -0.5f}, {0.5f, 0.5f, -0.5f}, {0.5f, -0.5f, -0.5f},
                // Top face
                {-0.5f, 0.5f, -0.5f}, {-0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f},
                {-0.5f, 0.5f, -0.5f}, {0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, -0.5f},
                // Bottom face
                {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, 0.5f},
                {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, 0.5f}, {-0.5f, -0.5f, 0.5f},
                // Right face
                {0.5f, -0.5f, -0.5f}, {0.5f, 0.5f, -0.5f}, {0.5f, 0.5f, 0.5f},
                {0.5f, -0.5f, -0.5f}, {0.5f, 0.5f, 0.5f}, {0.5f, -0.5f, 0.5f},
                // Left face
                {-0.5f, -0.5f, -0.5f}, {-0.5f, -0.5f, 0.5f}, {-0.5f, 0.5f, 0.5f},
                {-0.5f, -0.5f, -0.5f}, {-0.5f, 0.5f, 0.5f}, {-0.5f, 0.5f, -0.5f}
        };

        float[][] normals = {
                // Front face normal
                {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1},
                // Back face normal
                {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1}, {0, 0, -1},
                // Top face normal
                {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0}, {0, 1, 0},
                // Bottom face normal
                {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0}, {0, -1, 0},
                // Right face normal
                {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0}, {1, 0, 0},
                // Left face normal
                {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}, {-1, 0, 0}
        };

        for (int i = 0; i < vertices.length; i++) {
            glNormal3f(normals[i][0], normals[i][1], normals[i][2]);
            glVertex3f(vertices[i][0], vertices[i][1], vertices[i][2]);
        }

        glEnd();
    }

    public void cleanup() {
        // Cleanup object rendering resources
    }
}
