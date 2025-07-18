package com.nak.engine.terrain;

import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.glColor3f;

public class TerrainManager {
    private TerrainOctreeNode rootNode;
    private static final float WORLD_SIZE = 1024.0f;
    private Vector3f lastCameraPos;
    private float updateThreshold = 5.0f; // Update when camera moves 5 units

    public TerrainManager() {
        rootNode = new TerrainOctreeNode(new Vector3f(0, 0, 0), WORLD_SIZE, 0);
        lastCameraPos = new Vector3f();
    }

    public void update(Vector3f cameraPos, float deltaTime) {
        // Only update if camera moved significantly
        if (lastCameraPos.distance(cameraPos) > updateThreshold) {
            rootNode.update(cameraPos);
            lastCameraPos.set(cameraPos);
        }
    }

    public void render() {
        glColor3f(0.4f, 0.7f, 0.2f); // Green terrain
        rootNode.render();
    }

    public void cleanup() {
        rootNode.cleanup();
    }
}