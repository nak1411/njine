package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

class MasterRenderer {

    private TerrainManager terrainManager;

    public MasterRenderer(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;
    }

    public void render(GameState gameState, Camera camera, float interpolation) {
        // Enable wireframe mode for better terrain visualization
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Render terrain
        terrainManager.render();

        // Switch back to filled mode for other objects
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // Draw some reference cubes
        drawCubes(gameState);

        // Draw camera info
        drawCameraInfo(camera);
    }

    private void drawCubes(GameState gameState) {
        // Draw some animated cubes
        for (int i = 0; i < 5; i++) {
            glPushMatrix();

            float x = (float) Math.cos(gameState.getTime() + i) * 3.0f;
            float z = (float) Math.sin(gameState.getTime() + i) * 3.0f;
            float y = (float) Math.sin(gameState.getTime() * 2 + i) * 0.5f + 1.0f;

            glTranslatef(x, y, z);
            glRotatef(gameState.getTime() * 50 + i * 30, 1.0f, 1.0f, 0.0f);

            drawCube();

            glPopMatrix();
        }
    }

    private void drawCube() {
        glBegin(GL_QUADS);

        // Front face
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);

        // Back face
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);

        // Top face
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);

        // Bottom face
        glColor3f(1.0f, 1.0f, 0.0f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);

        // Right face
        glColor3f(1.0f, 0.0f, 1.0f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);

        // Left face
        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);

        glEnd();
    }

    private void drawCameraInfo(Camera camera) {
        // Draw a simple coordinate system at camera position
        Vector3f pos = camera.getPosition();

        glPushMatrix();
        glTranslatef(pos.x, pos.y - 1.0f, pos.z);

        glBegin(GL_LINES);
        // X axis - Red
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(1.0f, 0.0f, 0.0f);

        // Y axis - Green
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 1.0f, 0.0f);

        // Z axis - Blue
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 1.0f);
        glEnd();

        glPopMatrix();
    }

    public void cleanup() {
        // Cleanup resources
    }
}