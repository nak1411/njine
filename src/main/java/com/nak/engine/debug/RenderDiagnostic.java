// RenderDiagnostic.java - Comprehensive diagnostic tool for black screen issues
package com.nak.engine.debug;

import com.nak.engine.camera.Camera;
import com.nak.engine.render.MasterRenderer;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class RenderDiagnostic {

    public static class DiagnosticResult {
        public boolean hasOpenGLContext;
        public boolean hasValidViewport;
        public boolean hasTerrain;
        public boolean hasShaders;
        public boolean hasCamera;
        public boolean isRenderingEnabled;
        public String[] errors;
        public String[] warnings;

        public boolean isHealthy() {
            return hasOpenGLContext && hasValidViewport && hasCamera &&
                    isRenderingEnabled && (errors == null || errors.length == 0);
        }
    }

    /**
     * Comprehensive black screen diagnostic
     */
    public static DiagnosticResult diagnoseBlackScreen(
            MasterRenderer renderer,
            TerrainManager terrainManager,
            Camera camera) {

        System.out.println("=== COMPREHENSIVE BLACK SCREEN DIAGNOSTIC ===");

        DiagnosticResult result = new DiagnosticResult();
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();

        // 1. Check OpenGL Context
        try {
            String version = glGetString(GL_VERSION);
            if (version != null) {
                result.hasOpenGLContext = true;
                System.out.println("✓ OpenGL Context: " + version);
            } else {
                errors.add("OpenGL context not available");
                System.err.println("✗ OpenGL context not available");
            }
        } catch (Exception e) {
            errors.add("OpenGL context error: " + e.getMessage());
            System.err.println("✗ OpenGL context error: " + e.getMessage());
        }

        // 2. Check Viewport
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        if (viewport[2] > 0 && viewport[3] > 0) {
            result.hasValidViewport = true;
            System.out.println("✓ Viewport: " + viewport[2] + "x" + viewport[3]);
        } else {
            errors.add("Invalid viewport dimensions: " + viewport[2] + "x" + viewport[3]);
            System.err.println("✗ Invalid viewport: " + viewport[2] + "x" + viewport[3]);
        }

        // 3. Check Camera
        if (camera != null) {
            Vector3f pos = camera.getPosition();
            if (pos != null && isValidVector(pos)) {
                result.hasCamera = true;
                System.out.println("✓ Camera position: " + pos);
            } else {
                errors.add("Camera has invalid position");
                System.err.println("✗ Camera position invalid");
            }
        } else {
            errors.add("Camera is null");
            System.err.println("✗ Camera is null");
        }

        // 4. Check Clear Color
        float[] clearColor = new float[4];
        glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor);
        System.out.println("Clear color: " + java.util.Arrays.toString(clearColor));
        if (clearColor[0] == 0 && clearColor[1] == 0 && clearColor[2] == 0) {
            warnings.add("Clear color is pure black - might be causing black screen");
        }

        // 5. Check Depth Testing
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST);
        System.out.println("Depth testing: " + (depthTest ? "enabled" : "disabled"));

        // 6. Check Terrain
        if (terrainManager != null) {
            int activeChunks = terrainManager.getActiveChunkCount();
            int visibleChunks = terrainManager.getVisibleChunkCount();

            if (activeChunks > 0) {
                result.hasTerrain = true;
                System.out.println("✓ Terrain: " + activeChunks + " active, " + visibleChunks + " visible");

                if (visibleChunks == 0) {
                    warnings.add("Terrain exists but no chunks are visible");
                }
            } else {
                warnings.add("No terrain chunks loaded");
                System.out.println("⚠ No terrain chunks loaded");
            }
        } else {
            errors.add("TerrainManager is null");
            System.err.println("✗ TerrainManager is null");
        }

        // 7. Check Renderer
        if (renderer != null) {
            result.isRenderingEnabled = true;
            System.out.println("✓ MasterRenderer exists");
        } else {
            errors.add("MasterRenderer is null");
            System.err.println("✗ MasterRenderer is null");
        }

        // 8. OpenGL Error Check
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            errors.add("OpenGL error: " + getErrorString(error));
            System.err.println("✗ OpenGL error: " + getErrorString(error));
        } else {
            System.out.println("✓ No OpenGL errors");
        }

        result.errors = errors.toArray(new String[0]);
        result.warnings = warnings.toArray(new String[0]);

        System.out.println("=== DIAGNOSTIC COMPLETE ===");
        System.out.println("Health Status: " + (result.isHealthy() ? "HEALTHY" : "ISSUES FOUND"));

        return result;
    }

    /**
     * Apply automated fixes for common black screen issues
     */
    public static void applyQuickFixes(DiagnosticResult diagnostic,
                                       MasterRenderer renderer,
                                       TerrainManager terrainManager,
                                       Camera camera) {

        System.out.println("=== APPLYING QUICK FIXES ===");

        // Fix 1: Reset clear color to something visible
        glClearColor(0.2f, 0.3f, 0.5f, 1.0f);
        System.out.println("✓ Set visible clear color");

        // Fix 2: Ensure proper OpenGL state
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        System.out.println("✓ Reset OpenGL state");

        // Fix 3: Camera position validation
        if (camera != null) {
            Vector3f pos = camera.getPosition();
            if (!isValidVector(pos)) {
                camera.setPosition(new Vector3f(0, 50, 0));
                System.out.println("✓ Reset camera to valid position");
            }
        }

        // Fix 4: Force terrain regeneration if needed
        if (terrainManager != null && terrainManager.getActiveChunkCount() == 0) {
            terrainManager.update(camera.getPosition(), 0.016f);
            System.out.println("✓ Forced terrain update");
        }

        // Fix 5: Test basic rendering
        renderTestTriangle();

        System.out.println("=== FIXES APPLIED ===");
    }

    /**
     * Render a simple test triangle to verify basic OpenGL functionality
     */
    public static void renderTestTriangle() {
        System.out.println("Rendering test triangle...");

        // Save current state
        glPushMatrix();
        glLoadIdentity();

        // Disable depth test temporarily
        boolean wasDepthEnabled = glIsEnabled(GL_DEPTH_TEST);
        glDisable(GL_DEPTH_TEST);

        // Render simple triangle
        glBegin(GL_TRIANGLES);
        glColor3f(1.0f, 0.0f, 0.0f); // Red
        glVertex3f(0.0f, 0.5f, -1.0f);
        glColor3f(0.0f, 1.0f, 0.0f); // Green  
        glVertex3f(-0.5f, -0.5f, -1.0f);
        glColor3f(0.0f, 0.0f, 1.0f); // Blue
        glVertex3f(0.5f, -0.5f, -1.0f);
        glEnd();

        // Restore state
        if (wasDepthEnabled) {
            glEnable(GL_DEPTH_TEST);
        }
        glPopMatrix();

        int error = glGetError();
        if (error == GL_NO_ERROR) {
            System.out.println("✓ Test triangle rendered successfully");
        } else {
            System.err.println("✗ Test triangle failed: " + getErrorString(error));
        }
    }

    private static boolean isValidVector(Vector3f v) {
        return v != null &&
                Float.isFinite(v.x) && Float.isFinite(v.y) && Float.isFinite(v.z) &&
                !Float.isNaN(v.x) && !Float.isNaN(v.y) && !Float.isNaN(v.z);
    }

    private static String getErrorString(int error) {
        switch (error) {
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            default: return "Unknown error: " + error;
        }
    }
}