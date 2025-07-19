package com.nak.engine.debug;

import com.nak.engine.terrain.TerrainChunk;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Utility class for debugging terrain loading and rendering issues
 */
public class TerrainDebugUtility {

    public static void debugTerrainSystem(TerrainManager terrainManager, Vector3f cameraPos) {
        System.out.println("=== TERRAIN SYSTEM DEBUG ===");

        if (terrainManager == null) {
            System.err.println("ERROR: TerrainManager is null!");
            return;
        }

        System.out.println("Camera Position: " + cameraPos);
        System.out.println("Terrain Manager Initialized: " + terrainManager.isInitialized());
        System.out.println(terrainManager.getPerformanceInfo());
        System.out.println("Active Chunks (static): " + TerrainChunk.getActiveChunkCount());

        // Check OpenGL state
        checkOpenGLState();

        System.out.println("=== END TERRAIN DEBUG ===");
    }

    private static void checkOpenGLState() {
        System.out.println("OpenGL State Check:");

        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.err.println("  OpenGL Error: " + getErrorString(error));
        } else {
            System.out.println("  OpenGL State: OK");
        }

        // Check enabled capabilities
        System.out.println("  Depth Test: " + (GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? "Enabled" : "Disabled"));
        System.out.println("  Cull Face: " + (GL11.glIsEnabled(GL11.GL_CULL_FACE) ? "Enabled" : "Disabled"));
        System.out.println("  Blend: " + (GL11.glIsEnabled(GL11.GL_BLEND) ? "Enabled" : "Disabled"));
    }

    private static String getErrorString(int error) {
        return switch (error) {
            case GL11.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL11.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL11.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL11.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            default -> "Unknown error " + error;
        };
    }

    /**
     * Test terrain height generation at various points
     */
    public static void testHeightGeneration(TerrainManager terrainManager) {
        System.out.println("=== TERRAIN HEIGHT TEST ===");

        Vector3f[] testPoints = {
                new Vector3f(0, 0, 0),
                new Vector3f(32, 0, 32),
                new Vector3f(-32, 0, -32),
                new Vector3f(100, 0, 100),
                new Vector3f(-100, 0, -100)
        };

        for (Vector3f point : testPoints) {
            float height = terrainManager.getHeightAt(point.x, point.z);
            System.out.printf("Height at (%.1f, %.1f): %.2f\n", point.x, point.z, height);
        }

        System.out.println("=== END HEIGHT TEST ===");
    }

    /**
     * Monitor terrain loading progress
     */
    public static class TerrainLoadMonitor {
        private long lastUpdate = 0;
        private int lastActiveChunks = 0;
        private int lastVisibleChunks = 0;

        public void update(TerrainManager terrainManager) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastUpdate > 2000) { // Update every 2 seconds
                int currentActive = terrainManager.getActiveChunkCount();
                int currentVisible = terrainManager.getVisibleChunkCount();

                if (currentActive != lastActiveChunks || currentVisible != lastVisibleChunks) {
                    System.out.println("Terrain Update: Active=" + currentActive +
                            " (+" + (currentActive - lastActiveChunks) + "), " +
                            "Visible=" + currentVisible +
                            " (+" + (currentVisible - lastVisibleChunks) + ")");
                }

                lastActiveChunks = currentActive;
                lastVisibleChunks = currentVisible;
                lastUpdate = currentTime;
            }
        }
    }

    /**
     * Simple terrain stress test
     */
    public static void stressTest(TerrainManager terrainManager) {
        System.out.println("=== TERRAIN STRESS TEST ===");

        Vector3f testPos = new Vector3f();
        long startTime = System.currentTimeMillis();

        // Simulate rapid camera movement
        for (int i = 0; i < 100; i++) {
            testPos.set(i * 10, 0, i * 10);
            terrainManager.update(testPos, 0.016f); // 60 FPS delta

            if (i % 20 == 0) {
                System.out.println("Stress test progress: " + i + "% - " +
                        terrainManager.getPerformanceInfo());
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Stress test completed in " + (endTime - startTime) + "ms");
        System.out.println("Final state: " + terrainManager.getPerformanceInfo());
        System.out.println("=== END STRESS TEST ===");
    }

    /**
     * Check for common terrain issues
     */
    public static void diagnoseCommonIssues(TerrainManager terrainManager, Vector3f cameraPos) {
        System.out.println("=== TERRAIN DIAGNOSIS ===");

        // Check 1: No chunks loading
        if (terrainManager.getActiveChunkCount() == 0) {
            System.err.println("ISSUE: No terrain chunks are being created!");
            System.err.println("  - Check if TerrainManager.update() is being called");
            System.err.println("  - Verify camera position is reasonable: " + cameraPos);
        }

        // Check 2: Chunks not visible
        if (terrainManager.getActiveChunkCount() > 0 && terrainManager.getVisibleChunkCount() == 0) {
            System.err.println("ISSUE: Chunks exist but none are visible!");
            System.err.println("  - Check frustum culling parameters");
            System.err.println("  - Verify chunk bounds and camera position");
        }

        // Check 3: No chunks rendering
        if (terrainManager.getVisibleChunkCount() > 0 && terrainManager.getChunksRendered() == 0) {
            System.err.println("ISSUE: Visible chunks exist but none are rendering!");
            System.err.println("  - Check if chunk.createBuffers() is being called");
            System.err.println("  - Verify OpenGL state and shader programs");
        }

        // Check 4: Camera position issues
        if (!Float.isFinite(cameraPos.x) || !Float.isFinite(cameraPos.y) || !Float.isFinite(cameraPos.z)) {
            System.err.println("ISSUE: Camera position contains invalid values: " + cameraPos);
        }

        // Check 5: Extreme camera position
        float distanceFromOrigin = cameraPos.length();
        if (distanceFromOrigin > 10000) {
            System.err.println("WARNING: Camera very far from origin (" + distanceFromOrigin + " units)");
            System.err.println("  - This may cause precision issues");
        }

        System.out.println("=== END DIAGNOSIS ===");
    }
}