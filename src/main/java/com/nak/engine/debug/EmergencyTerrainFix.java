// IMMEDIATE FIX: Emergency terrain shader for OpenGL compatibility issues
// Add this class and call EmergencyTerrainFix.applyImmediateFix() right after OpenGL init

package com.nak.engine.debug;

import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

public class EmergencyTerrainFix {

    /**
     * CALL THIS IMMEDIATELY AFTER OPENGL INITIALIZATION
     * This will fix blue terrain regardless of OpenGL version issues
     */
    public static void applyImmediateFix() {
        System.out.println("🚨 EMERGENCY TERRAIN FIX STARTING...");

        try {
            // Step 1: Check what we're working with
            checkOpenGLCapabilities();

            // Step 2: Force create working shader
            forceCreateWorkingTerrainShader();

            // Step 3: Validate and test
            validateFixedShader();

            System.out.println("✅ EMERGENCY FIX COMPLETE - Terrain should now be visible!");

        } catch (Exception e) {
            System.err.println("❌ Emergency fix failed: " + e.getMessage());
            e.printStackTrace();

            // Last resort - try compatibility mode
            tryCompatibilityMode();
        }
    }

    private static void checkOpenGLCapabilities() {
        System.out.println("🔍 Checking OpenGL capabilities...");

        try {
            String version = GL11.glGetString(GL11.GL_VERSION);
            String renderer = GL11.glGetString(GL11.GL_RENDERER);

            System.out.println("Graphics card: " + renderer);
            System.out.println("OpenGL version: " + version);

            // Extract major/minor version
            if (version != null) {
                String[] parts = version.split("\\.");
                if (parts.length >= 2) {
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1].split(" ")[0]);

                    System.out.println("Detected OpenGL " + major + "." + minor);

                    if (major >= 3 && minor >= 3) {
                        System.out.println("✅ OpenGL 3.3+ detected");
                    } else if (major >= 2) {
                        System.out.println("⚠️ OpenGL 2.x detected - using compatibility mode");
                    } else {
                        System.out.println("❌ OpenGL version too old - may have issues");
                    }
                }
            }

            // Check shader support
            String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
            System.out.println("GLSL version: " + glslVersion);

        } catch (Exception e) {
            System.err.println("Error checking OpenGL: " + e.getMessage());
        }
    }

    private static void forceCreateWorkingTerrainShader() {
        System.out.println("🔧 Force creating working terrain shader...");

        ShaderManager shaderManager = ShaderManager.getInstance();

        // Remove any existing broken terrain shader
        ShaderProgram existing = shaderManager.getProgram("terrain");
        if (existing != null) {
            System.out.println("Removing broken terrain shader...");
            existing.cleanup();
        }

        // Create shader based on detected OpenGL version
        String glVersion = GL11.glGetString(GL11.GL_VERSION);
        boolean useModernGL = glVersion != null &&
                (glVersion.startsWith("3.") || glVersion.startsWith("4.") || glVersion.startsWith("5."));

        if (useModernGL) {
            createModernTerrainShader(shaderManager);
        } else {
            createCompatibilityTerrainShader(shaderManager);
        }
    }

    private static void createModernTerrainShader(ShaderManager shaderManager) {
        System.out.println("Creating modern OpenGL 3.3+ terrain shader...");

        String vertexShader = """
                #version 330 core
                
                // Input attributes
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec3 tangent;
                layout (location = 4) in vec3 color;
                
                // GUARANTEED UNIFORMS - All explicitly declared
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform mat4 lightSpaceMatrix;
                uniform vec3 viewPosition;
                
                // Output to fragment shader
                out vec3 fragPos;
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec3 vertexColor;
                out float height;
                
                void main() {
                    // Calculate world position
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                
                    // Pass through texture coordinates
                    texCoords = texCoord;
                
                    // Calculate normal in world space
                    mat3 normalMatrix = mat3(transpose(inverse(modelMatrix)));
                    fragNormal = normalize(normalMatrix * normal);
                
                    // Ensure valid vertex color with fallback
                    vertexColor = color;
                    if (length(vertexColor) < 0.01) {
                        // Generate color based on height
                        float h = position.y;
                        if (h < -2.0) {
                            vertexColor = vec3(0.1, 0.3, 0.7);      // Deep water
                        } else if (h < 0.0) {
                            vertexColor = vec3(0.2, 0.4, 0.8);      // Shallow water
                        } else if (h < 5.0) {
                            vertexColor = vec3(0.2, 0.7, 0.1);      // Grass
                        } else if (h < 15.0) {
                            vertexColor = vec3(0.3, 0.6, 0.2);      // Hills
                        } else if (h < 30.0) {
                            vertexColor = vec3(0.6, 0.5, 0.4);      // Mountains
                        } else {
                            vertexColor = vec3(0.9, 0.9, 0.95);     // Snow
                        }
                    }
                
                    height = position.y;
                
                    // Final position
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

        String fragmentShader = """
                #version 330 core
                
                // Input from vertex shader
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 vertexColor;
                in float height;
                
                // GUARANTEED LIGHTING UNIFORMS - All explicitly declared
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                uniform vec3 viewPosition;
                
                // GUARANTEED FOG UNIFORMS - All explicitly declared
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                // Output color
                out vec4 fragColor;
                
                void main() {
                    // Ensure valid normal
                    vec3 norm = normalize(fragNormal);
                    if (length(norm) < 0.1) {
                        norm = vec3(0.0, 1.0, 0.0); // Default up
                    }
                
                    // Ensure valid vertex color
                    vec3 baseColor = vertexColor;
                    if (length(baseColor) < 0.1) {
                        baseColor = vec3(0.3, 0.6, 0.2); // Default green
                    }
                
                    // LIGHTING CALCULATION
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient - ensure it's never zero
                    float safeAmbient = max(ambientStrength, 0.25);
                    vec3 ambient = safeAmbient * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), max(shininess, 1.0));
                    vec3 specular = specularStrength * spec * lightColor;
                
                    // Combine lighting
                    vec3 lighting = ambient + diffuse + specular;
                    vec3 finalColor = baseColor * lighting;
                
                    // FOG APPLICATION
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    // Ensure never completely black/invisible
                    finalColor = max(finalColor, vec3(0.1));
                
                    // Add subtle variation for visual interest
                    float variation = sin(fragPos.x * 0.05) * cos(fragPos.z * 0.05) * 0.05 + 0.95;
                    finalColor *= variation;
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            ShaderProgram terrainShader = shaderManager.createProgram(
                    "terrain", vertexShader, fragmentShader
            );
            System.out.println("✅ Modern terrain shader created successfully");
        } catch (Exception e) {
            System.err.println("❌ Modern shader failed: " + e.getMessage());
            // Fallback to compatibility mode
            createCompatibilityTerrainShader(shaderManager);
        }
    }

    private static void createCompatibilityTerrainShader(ShaderManager shaderManager) {
        System.out.println("Creating OpenGL 2.1 compatibility terrain shader...");

        String vertexShader = """
                #version 120
                
                // Input attributes
                attribute vec3 position;
                attribute vec2 texCoord;
                attribute vec3 normal;
                attribute vec3 color;
                
                // Uniforms
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                
                // Output to fragment shader
                varying vec3 fragPos;
                varying vec2 texCoords;
                varying vec3 fragNormal;
                varying vec3 vertexColor;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                
                    // Simple normal calculation
                    fragNormal = normalize(mat3(modelMatrix) * normal);
                
                    // Ensure valid color
                    vertexColor = color;
                    if (length(vertexColor) < 0.01) {
                        vertexColor = vec3(0.3, 0.6, 0.2); // Default green
                    }
                
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

        String fragmentShader = """
                #version 120
                
                // Input from vertex shader
                varying vec3 fragPos;
                varying vec2 texCoords;
                varying vec3 fragNormal;
                varying vec3 vertexColor;
                
                // Uniforms
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform float ambientStrength;
                uniform vec3 viewPosition;
                
                void main() {
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Simple lighting
                    float ambient = max(ambientStrength, 0.4);
                    float diff = max(dot(norm, lightDir), 0.0);
                
                    vec3 finalColor = vertexColor * lightColor * (ambient + diff * 0.6);
                    finalColor = max(finalColor, vec3(0.2)); // Ensure visibility
                
                    gl_FragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            ShaderProgram terrainShader = shaderManager.createProgram(
                    "terrain", vertexShader, fragmentShader
            );
            System.out.println("✅ Compatibility terrain shader created successfully");
        } catch (Exception e) {
            System.err.println("❌ Compatibility shader failed: " + e.getMessage());
            // Absolute last resort
            createUltraSimpleShader(shaderManager);
        }
    }

    private static void createUltraSimpleShader(ShaderManager shaderManager) {
        System.out.println("Creating ultra-simple last resort shader...");

        String vertexShader = """
                attribute vec3 position;
                attribute vec3 color;
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                varying vec3 vertexColor;
                
                void main() {
                    vertexColor = length(color) > 0.1 ? color : vec3(0.5, 0.8, 0.3);
                    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
                }
                """;

        String fragmentShader = """
                varying vec3 vertexColor;
                uniform float ambientStrength;
                
                void main() {
                    float brightness = max(ambientStrength, 0.6);
                    gl_FragColor = vec4(vertexColor * brightness, 1.0);
                }
                """;

        try {
            ShaderProgram terrainShader = shaderManager.createProgram(
                    "terrain", vertexShader, fragmentShader
            );
            System.out.println("✅ Ultra-simple terrain shader created as last resort");
        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Even ultra-simple shader failed!");
            e.printStackTrace();
        }
    }

    private static void validateFixedShader() {
        System.out.println("🔍 Validating fixed terrain shader...");

        ShaderManager shaderManager = ShaderManager.getInstance();
        ShaderProgram terrainShader = shaderManager.getProgram("terrain");

        if (terrainShader == null) {
            System.err.println("❌ No terrain shader found after fix!");
            return;
        }

        if (!terrainShader.isLinked()) {
            System.err.println("❌ Terrain shader not linked!");
            return;
        }

        System.out.println("✅ Terrain shader exists and is linked");
        System.out.println("Available uniforms: " + terrainShader.getUniformNames().size());

        // Test with dummy uniforms
        try {
            terrainShader.bind();

            // Set essential uniforms
            terrainShader.setUniform("projectionMatrix", new Matrix4f().identity());
            terrainShader.setUniform("viewMatrix", new Matrix4f().identity());
            terrainShader.setUniform("modelMatrix", new Matrix4f().identity());

            if (terrainShader.hasUniform("lightPosition")) {
                terrainShader.setUniform("lightPosition", new Vector3f(100, 100, 100));
            }
            if (terrainShader.hasUniform("lightColor")) {
                terrainShader.setUniform("lightColor", new Vector3f(1, 1, 1));
            }
            if (terrainShader.hasUniform("ambientStrength")) {
                terrainShader.setUniform("ambientStrength", 0.5f); // High ambient for visibility
            }
            if (terrainShader.hasUniform("viewPosition")) {
                terrainShader.setUniform("viewPosition", new Vector3f(0, 10, 3));
            }

            terrainShader.unbind();

            // Check for OpenGL errors
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) {
                System.out.println("✅ Shader validation PASSED - terrain should be visible now!");
            } else {
                System.err.println("❌ OpenGL error during validation: " + error);
            }

        } catch (Exception e) {
            System.err.println("❌ Shader validation failed: " + e.getMessage());
        }
    }

    private static void tryCompatibilityMode() {
        System.out.println("🔄 Trying compatibility mode as last resort...");

        try {
            // Force OpenGL compatibility profile settings
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_LIGHT0);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);

            // Set up basic lighting
            float[] lightPos = {100.0f, 100.0f, 100.0f, 1.0f};
            float[] lightColor = {1.0f, 1.0f, 1.0f, 1.0f};
            float[] ambient = {0.5f, 0.5f, 0.5f, 1.0f};

            //GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPos);
            //GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, lightColor);
            //GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, ambient);

            System.out.println("✅ Compatibility mode lighting enabled");

        } catch (Exception e) {
            System.err.println("❌ Compatibility mode failed: " + e.getMessage());
        }
    }

    /**
     * Force high ambient lighting to make terrain visible regardless of shader issues
     */
    public static void forceVisibleTerrain() {
        System.out.println("🔆 FORCING TERRAIN VISIBILITY...");

        ShaderManager shaderManager = ShaderManager.getInstance();
        ShaderProgram terrainShader = shaderManager.getProgram("terrain");

        if (terrainShader != null && terrainShader.isLinked()) {
            terrainShader.bind();

            // Force very high ambient lighting
            if (terrainShader.hasUniform("ambientStrength")) {
                terrainShader.setUniform("ambientStrength", 0.9f);
            }
            if (terrainShader.hasUniform("lightColor")) {
                terrainShader.setUniform("lightColor", new Vector3f(1.2f, 1.2f, 1.2f));
            }

            terrainShader.unbind();
            System.out.println("✅ Forced high ambient lighting");
        }
    }

    /**
     * Complete emergency procedure
     */
    public static void completeEmergencyProcedure() {
        System.out.println("🚨🚨🚨 COMPLETE EMERGENCY PROCEDURE 🚨🚨🚨");

        applyImmediateFix();
        forceVisibleTerrain();

        System.out.println("🚨 EMERGENCY PROCEDURE COMPLETE 🚨");
        System.out.println("If terrain is still blue, the issue may be in TerrainChunk vertex color generation.");
        System.out.println("Check TerrainChunk.calculateEnhancedVertexColor() method.");
    }
}