// EngineStartupDiagnostic.java - Comprehensive startup and black screen diagnostic
package com.nak.engine.debug;

import com.nak.engine.camera.Camera;
import com.nak.engine.render.MasterRenderer;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class EngineStartupDiagnostic {

    /**
     * Complete diagnostic suite for black screen issues
     * Call this from your main render loop or Window.render() method
     */
    public static void runCompleteDiagnostic(MasterRenderer renderer,
                                             TerrainManager terrainManager,
                                             Camera camera) {

        System.out.println("\n" + "=".repeat(60));
        System.out.println("ENGINE STARTUP DIAGNOSTIC - BLACK SCREEN ANALYSIS");
        System.out.println("=".repeat(60));

        // Step 1: OpenGL Context Verification
        checkOpenGLContext();

        // Step 2: Rendering Pipeline Check
        checkRenderingPipeline(renderer, terrainManager, camera);

        // Step 3: Camera and View System
        checkCameraSystem(camera);

        // Step 4: Terrain System
        checkTerrainSystem(terrainManager, camera);

        // Step 5: Immediate Fixes
        applyImmediateFixes(renderer, terrainManager, camera);

        // Step 6: Test Render
        performTestRender();

        System.out.println("=".repeat(60));
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("=".repeat(60) + "\n");
    }

    private static void checkOpenGLContext() {
        System.out.println("\n1. OPENGL CONTEXT CHECK:");
        System.out.println("-".repeat(30));

        try {
            String version = glGetString(GL_VERSION);
            String vendor = glGetString(GL_VENDOR);
            String renderer = glGetString(GL_RENDERER);

            if (version != null) {
                System.out.println("✓ OpenGL Version: " + version);
                System.out.println("✓ Vendor: " + vendor);
                System.out.println("✓ Renderer: " + renderer);
            } else {
                System.out.println("✗ CRITICAL: OpenGL context not available!");
                System.out.println("  → Check GLFW initialization");
                System.out.println("  → Check window creation");
                System.out.println("  → Check GL.createCapabilities() call");
                return;
            }

            // Check viewport
            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            System.out.println("✓ Viewport: " + viewport[0] + "," + viewport[1] + " " +
                    viewport[2] + "x" + viewport[3]);

            if (viewport[2] <= 0 || viewport[3] <= 0) {
                System.out.println("✗ WARNING: Invalid viewport dimensions!");
            }

            // Check clear color
            float[] clearColor = new float[4];
            glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor);
            System.out.printf("✓ Clear Color: %.2f, %.2f, %.2f, %.2f%n",
                    clearColor[0], clearColor[1], clearColor[2], clearColor[3]);

            if (clearColor[0] == 0 && clearColor[1] == 0 && clearColor[2] == 0) {
                System.out.println("⚠ WARNING: Clear color is black - this might cause black screen!");
            }

        } catch (Exception e) {
            System.out.println("✗ CRITICAL: OpenGL context error: " + e.getMessage());
        }
    }

    private static void checkRenderingPipeline(MasterRenderer renderer,
                                               TerrainManager terrainManager,
                                               Camera camera) {
        System.out.println("\n2. RENDERING PIPELINE CHECK:");
        System.out.println("-".repeat(30));

        if (renderer == null) {
            System.out.println("✗ CRITICAL: MasterRenderer is null!");
            System.out.println("  → Check MasterRenderer initialization in Window.initializeGameComponents()");
            System.out.println("  → Verify TerrainManager is passed to MasterRenderer constructor");
            return;
        } else {
            System.out.println("✓ MasterRenderer exists");
        }

        // Check OpenGL state
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFace = glIsEnabled(GL_CULL_FACE);

        System.out.println("✓ Depth testing: " + (depthTest ? "enabled" : "disabled"));
        System.out.println("✓ Face culling: " + (cullFace ? "enabled" : "disabled"));

        if (!depthTest) {
            System.out.println("⚠ WARNING: Depth testing disabled - 3D rendering may not work");
        }

        // Check for OpenGL errors
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println("✗ OpenGL Error: " + getOpenGLErrorString(error));
        } else {
            System.out.println("✓ No OpenGL errors");
        }
    }

    private static void checkCameraSystem(Camera camera) {
        System.out.println("\n3. CAMERA SYSTEM CHECK:");
        System.out.println("-".repeat(30));

        if (camera == null) {
            System.out.println("✗ CRITICAL: Camera is null!");
            return;
        }

        Vector3f position = camera.getPosition();
        if (position == null) {
            System.out.println("✗ CRITICAL: Camera position is null!");
            return;
        }

        System.out.printf("✓ Camera Position: %.2f, %.2f, %.2f%n",
                position.x, position.y, position.z);

        // Check for invalid values
        if (!Float.isFinite(position.x) || !Float.isFinite(position.y) || !Float.isFinite(position.z)) {
            System.out.println("✗ CRITICAL: Camera position contains invalid values!");
            System.out.println("  → Reset camera position to safe values");
            return;
        }

        // Check if camera is in reasonable position
        float distance = position.length();
        if (distance > 10000) {
            System.out.println("⚠ WARNING: Camera very far from origin (" + distance + " units)");
            System.out.println("  → This may cause precision issues");
        }

        // Check camera matrices
        try {
            org.joml.Matrix4f viewMatrix = camera.getViewMatrix();
            org.joml.Matrix4f projMatrix = camera.getProjectionMatrix();

            if (viewMatrix != null && projMatrix != null) {
                System.out.println("✓ Camera matrices available");
            } else {
                System.out.println("✗ WARNING: Camera matrices are null");
            }
        } catch (Exception e) {
            System.out.println("✗ ERROR: Camera matrix error: " + e.getMessage());
        }
    }

    private static void checkTerrainSystem(TerrainManager terrainManager, Camera camera) {
        System.out.println("\n4. TERRAIN SYSTEM CHECK:");
        System.out.println("-".repeat(30));

        if (terrainManager == null) {
            System.out.println("✗ CRITICAL: TerrainManager is null!");
            return;
        }

        // Check terrain state
        int activeChunks = terrainManager.getActiveChunkCount();
        int visibleChunks = terrainManager.getVisibleChunkCount();
        int renderedChunks = terrainManager.getChunksRendered();

        System.out.println("✓ TerrainManager exists");
        System.out.println("  Active chunks: " + activeChunks);
        System.out.println("  Visible chunks: " + visibleChunks);
        System.out.println("  Rendered chunks: " + renderedChunks);

        if (activeChunks == 0) {
            System.out.println("⚠ WARNING: No terrain chunks loaded!");
            System.out.println("  → Call terrainManager.update(cameraPos, deltaTime)");
            System.out.println("  → Check if terrain generation is working");
        }

        if (activeChunks > 0 && visibleChunks == 0) {
            System.out.println("⚠ WARNING: Chunks exist but none are visible!");
            System.out.println("  → Check camera position relative to terrain");
            System.out.println("  → Check view distance settings");
            System.out.println("  → Check frustum culling");
        }

        if (visibleChunks > 0 && renderedChunks == 0) {
            System.out.println("⚠ WARNING: Visible chunks exist but none are rendering!");
            System.out.println("  → Check chunk.createBuffers() calls");
            System.out.println("  → Check OpenGL buffer creation");
            System.out.println("  → Check shader compilation");
        }

        // Test height generation
        if (camera != null) {
            try {
                Vector3f pos = camera.getPosition();
                float height = terrainManager.getHeightAt(pos.x, pos.z);
                System.out.printf("✓ Height at camera: %.2f%n", height);

                if (height == 0.0f) {
                    System.out.println("⚠ WARNING: Height is exactly 0.0 - may indicate terrain issues");
                }
            } catch (Exception e) {
                System.out.println("✗ ERROR: Height generation failed: " + e.getMessage());
            }
        }
    }

    private static void applyImmediateFixes(MasterRenderer renderer,
                                            TerrainManager terrainManager,
                                            Camera camera) {
        System.out.println("\n5. APPLYING IMMEDIATE FIXES:");
        System.out.println("-".repeat(30));

        // Fix 1: Set visible clear color
        System.out.println("→ Setting visible clear color...");
        glClearColor(0.2f, 0.4f, 0.6f, 1.0f);
        System.out.println("✓ Clear color set to blue");

        // Fix 2: Ensure proper OpenGL state
        System.out.println("→ Configuring OpenGL state...");
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        System.out.println("✓ OpenGL state configured");

        // Fix 3: Camera position validation
        if (camera != null) {
            Vector3f pos = camera.getPosition();
            if (pos == null || !Float.isFinite(pos.x) || !Float.isFinite(pos.y) || !Float.isFinite(pos.z)) {
                System.out.println("→ Fixing camera position...");
                camera.setPosition(new Vector3f(0, 50, 0));
                System.out.println("✓ Camera reset to safe position (0, 50, 0)");
            }
        }

        // Fix 4: Force terrain update
        if (terrainManager != null && camera != null) {
            if (terrainManager.getActiveChunkCount() == 0) {
                System.out.println("→ Forcing terrain generation...");
                terrainManager.update(camera.getPosition(), 0.016f);
                System.out.println("✓ Terrain update forced");
            }
        }

        // Fix 5: Clear any OpenGL errors
        System.out.println("→ Clearing OpenGL errors...");
        while (glGetError() != GL_NO_ERROR) { /* consume all errors */ }
        System.out.println("✓ OpenGL errors cleared");
    }

    private static void performTestRender() {
        System.out.println("\n6. TEST RENDER:");
        System.out.println("-".repeat(30));

        try {
            // Clear screen
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Test basic rendering with immediate mode
            System.out.println("→ Rendering test triangle...");

            glPushMatrix();
            glLoadIdentity();

            // Disable depth test for simple test
            glDisable(GL_DEPTH_TEST);

            // Render colored triangle
            glBegin(GL_TRIANGLES);
            glColor3f(1.0f, 0.0f, 0.0f); // Red
            glVertex3f(0.0f, 0.5f, -1.0f);
            glColor3f(0.0f, 1.0f, 0.0f); // Green
            glVertex3f(-0.5f, -0.5f, -1.0f);
            glColor3f(0.0f, 0.0f, 1.0f); // Blue
            glVertex3f(0.5f, -0.5f, -1.0f);
            glEnd();

            // Re-enable depth test
            glEnable(GL_DEPTH_TEST);
            glPopMatrix();

            // Check for errors
            int error = glGetError();
            if (error == GL_NO_ERROR) {
                System.out.println("✓ Test triangle rendered successfully!");
                System.out.println("  If you still see black screen, the issue is in your main render loop");
            } else {
                System.out.println("✗ Test render failed: " + getOpenGLErrorString(error));
            }

        } catch (Exception e) {
            System.out.println("✗ Test render exception: " + e.getMessage());
        }
    }

    /**
     * Quick diagnostic that can be called from render loop
     */
    public static void quickDiagnostic() {
        System.out.println("\n=== QUICK RENDER DIAGNOSTIC ===");

        // Check if we can render at all
        try {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Simple test quad
            glLoadIdentity();
            glBegin(GL_QUADS);
            glColor3f(0.3f, 0.5f, 0.8f);
            glVertex2f(-0.5f, -0.5f);
            glVertex2f(0.5f, -0.5f);
            glVertex2f(0.5f, 0.5f);
            glVertex2f(-0.5f, 0.5f);
            glEnd();

            int error = glGetError();
            if (error == GL_NO_ERROR) {
                System.out.println("✓ Basic rendering works - check your render pipeline");
            } else {
                System.out.println("✗ Basic rendering failed: " + getOpenGLErrorString(error));
            }

        } catch (Exception e) {
            System.out.println("✗ Critical rendering error: " + e.getMessage());
        }

        System.out.println("=== END QUICK DIAGNOSTIC ===\n");
    }

    /**
     * Emergency render fix - call this if everything else fails
     */
    public static void emergencyRenderFix() {
        System.out.println("=== EMERGENCY RENDER FIX ===");

        try {
            // Reset everything to safe defaults
            glClearColor(0.1f, 0.3f, 0.6f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Reset matrices
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(-1, 1, -1, 1, -1, 1);

            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            // Disable potentially problematic features
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            glDisable(GL_BLEND);
            glDisable(GL_TEXTURE_2D);

            // Render emergency content
            glColor3f(1.0f, 1.0f, 1.0f);
            glBegin(GL_QUADS);
            glVertex2f(-0.8f, -0.8f);
            glVertex2f(0.8f, -0.8f);
            glVertex2f(0.8f, 0.8f);
            glVertex2f(-0.8f, 0.8f);
            glEnd();

            System.out.println("✓ Emergency render fix applied");
            System.out.println("If you see a white square, OpenGL is working");

        } catch (Exception e) {
            System.out.println("✗ Emergency fix failed: " + e.getMessage());
            System.out.println("This indicates a fundamental OpenGL context problem");
        }

        System.out.println("=== END EMERGENCY FIX ===");
    }

    private static String getOpenGLErrorString(int error) {
        switch (error) {
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            default: return "Unknown error: " + error;
        }
    }

    /**
     * Call this from your main render method to identify the issue
     */
    public static void integrateWithRenderLoop(MasterRenderer renderer,
                                               TerrainManager terrainManager,
                                               Camera camera) {

        // This should be called once per frame in your render() method
        System.out.println("Frame render start - checking systems...");

        // Quick system check
        if (renderer == null) {
            System.err.println("RENDER ERROR: MasterRenderer is null");
            emergencyRenderFix();
            return;
        }

        if (camera == null) {
            System.err.println("RENDER ERROR: Camera is null");
            emergencyRenderFix();
            return;
        }

        // Clear and test
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("RENDER ERROR: OpenGL error before main render: " + getOpenGLErrorString(error));
        }

        System.out.println("Systems OK - proceeding with normal render");
    }
}