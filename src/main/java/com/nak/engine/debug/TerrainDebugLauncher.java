package com.nak.engine.debug;

import com.nak.engine.render.Window;

/**
 * Debug launcher that helps identify and fix the blue terrain issue
 */
public class TerrainDebugLauncher {

    public static void main(String[] args) {
        System.out.println("=== TERRAIN ENGINE DEBUG LAUNCHER ===");

        // Enable debug mode
        System.setProperty("development", "true");

        try {
            // Create and initialize window
            Window window = new Window();

            // Hook into the initialization process to add debugging
            window.init();

            // Add debug code after OpenGL context is created
            System.out.println("\n--- POST-INITIALIZATION DEBUG ---");

            // Debug the shader system
            ShaderDebugUtility.debugShaderSystem();

            // Apply fixes for common issues
            ShaderDebugUtility.applyShaderFixes();

            // Test the terrain shader specifically
            ShaderDebugUtility.testShaderWithDummyData("terrain");

            // Debug vertex colors
            ShaderDebugUtility.debugVertexColors();

            System.out.println("\n--- STARTING MAIN LOOP ---");

            // Run the main loop
            window.run();

        } catch (Exception e) {
            System.err.println("Error during startup: " + e.getMessage());
            e.printStackTrace();

            // Try to provide helpful debugging information
            provideErrorHelp(e);
        }
    }

    private static void provideErrorHelp(Exception e) {
        System.err.println("\n=== ERROR HELP ===");

        String message = e.getMessage();
        if (message != null) {
            if (message.contains("shader") || message.contains("GLSL")) {
                System.err.println("SHADER ERROR DETECTED:");
                System.err.println("1. Check that shader files exist in src/main/resources/shaders/");
                System.err.println("2. Verify GLSL version compatibility (#version 330 core)");
                System.err.println("3. Remove #include directives if causing issues");
                System.err.println("4. Check for uniform name mismatches");
            }

            if (message.contains("OpenGL")) {
                System.err.println("OPENGL ERROR DETECTED:");
                System.err.println("1. Ensure graphics drivers are up to date");
                System.err.println("2. Check OpenGL version support (need 3.3+)");
                System.err.println("3. Verify LWJGL native libraries are available");
            }

            if (message.contains("terrain") || message.contains("blue")) {
                System.err.println("TERRAIN RENDERING ERROR:");
                System.err.println("1. Check lighting uniforms are being set");
                System.err.println("2. Verify vertex colors are not all blue");
                System.err.println("3. Ensure normal vectors are correct");
                System.err.println("4. Check ambient light strength > 0");
            }
        }

        System.err.println("\nTo fix blue terrain specifically:");
        System.err.println("1. Run with: java -Ddevelopment=true -jar yourapp.jar");
        System.err.println("2. Check console for shader compilation errors");
        System.err.println("3. Press F1 in-game to see debug info");
        System.err.println("4. Try pressing F4 to test lighting");
    }
}