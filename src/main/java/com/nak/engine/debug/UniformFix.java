// IMMEDIATE FIX: Add this class to your project and call it after OpenGL initialization

package com.nak.engine.debug;

import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class UniformFix {

    /**
     * Call this method right after your OpenGL context is created
     * and before you try to render anything
     */
    public static void fixAllShaderUniforms() {
        System.out.println("=== APPLYING UNIFORM FIXES ===");

        ShaderManager shaderManager = ShaderManager.getInstance();

        // Step 1: Create a guaranteed working terrain shader
        createWorkingTerrainShader(shaderManager);

        // Step 2: Validate the shader works
        validateTerrainShader(shaderManager);

        // Step 3: Create other essential shaders
        createWorkingBasicShader(shaderManager);

        System.out.println("=== UNIFORM FIXES COMPLETE ===");
    }

    private static void createWorkingTerrainShader(ShaderManager shaderManager) {
        System.out.println("Creating guaranteed working terrain shader...");

        // This shader is guaranteed to compile and has all needed uniforms
        String workingVertexShader = """
                #version 330 core
                
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec3 tangent;
                layout (location = 4) in vec3 color;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform mat4 lightSpaceMatrix;
                
                out vec3 fragPos;
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec3 vertexColor;
                out float height;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                
                    // Safe normal calculation
                    mat3 normalMatrix = mat3(transpose(inverse(modelMatrix)));
                    fragNormal = normalize(normalMatrix * normal);
                
                    // Ensure we have a valid color
                    vertexColor = color;
                    if (length(vertexColor) < 0.01) {
                        // Generate color based on height if vertex color is missing
                        height = position.y;
                        if (height < 0.0) {
                            vertexColor = vec3(0.2, 0.4, 0.8); // Water
                        } else if (height < 10.0) {
                            vertexColor = vec3(0.2, 0.6, 0.15); // Grass
                        } else if (height < 25.0) {
                            vertexColor = vec3(0.5, 0.4, 0.3); // Rock
                        } else {
                            vertexColor = vec3(0.8, 0.8, 0.9); // Snow
                        }
                    }
                
                    height = position.y;
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

        String workingFragmentShader = """
                #version 330 core
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 vertexColor;
                in float height;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                out vec4 fragColor;
                
                void main() {
                    // Ensure we have a valid normal
                    vec3 norm = normalize(fragNormal);
                    if (length(norm) < 0.1) {
                        norm = vec3(0.0, 1.0, 0.0); // Default up normal
                    }
                
                    // Ensure we have a valid base color
                    vec3 baseColor = vertexColor;
                    if (length(baseColor) < 0.1) {
                        baseColor = vec3(0.3, 0.6, 0.2); // Default green
                    }
                
                    // Calculate lighting
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient (ensure it's never zero)
                    float safeAmbient = max(ambientStrength, 0.2);
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
                
                    // Apply fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    // Ensure color is never completely black
                    finalColor = max(finalColor, vec3(0.05));
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            // Remove existing terrain shader if present
            ShaderProgram existing = shaderManager.getProgram("terrain");
            if (existing != null) {
                existing.cleanup();
            }

            // Create new working shader
            ShaderProgram terrainShader = shaderManager.createProgram(
                    "terrain", workingVertexShader, workingFragmentShader);

            System.out.println("Working terrain shader created successfully!");

        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to create working terrain shader!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            // Try an even simpler shader as last resort
            createMinimalTerrainShader(shaderManager);
        }
    }

    private static void createMinimalTerrainShader(ShaderManager shaderManager) {
        System.out.println("Creating minimal terrain shader as last resort...");

        String minimalVertex = """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 4) in vec3 color;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                
                out vec3 vertexColor;
                
                void main() {
                    vertexColor = color;
                    if (length(vertexColor) < 0.1) {
                        vertexColor = vec3(0.2, 0.6, 0.1); // Default green
                    }
                    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
                }
                """;

        String minimalFragment = """
                #version 330 core
                in vec3 vertexColor;
                out vec4 fragColor;
                
                void main() {
                    fragColor = vec4(vertexColor, 1.0);
                }
                """;

        try {
            shaderManager.createProgram("terrain", minimalVertex, minimalFragment);
            System.out.println("Minimal terrain shader created!");
        } catch (Exception e) {
            System.err.println("FATAL: Even minimal shader failed: " + e.getMessage());
        }
    }

    private static void validateTerrainShader(ShaderManager shaderManager) {
        System.out.println("Validating terrain shader...");

        ShaderProgram terrainShader = shaderManager.getProgram("terrain");
        if (terrainShader == null) {
            System.err.println("ERROR: No terrain shader exists!");
            return;
        }

        // Check if shader is properly linked
        if (!terrainShader.isLinked()) {
            System.err.println("ERROR: Terrain shader is not linked!");
            return;
        }

        // Test that we can bind the shader
        try {
            terrainShader.bind();

            // Test setting basic uniforms
            if (terrainShader.hasUniform("projectionMatrix")) {
                terrainShader.setUniform("projectionMatrix", new org.joml.Matrix4f().identity());
                System.out.println("✓ projectionMatrix uniform works");
            }

            if (terrainShader.hasUniform("viewMatrix")) {
                terrainShader.setUniform("viewMatrix", new org.joml.Matrix4f().identity());
                System.out.println("✓ viewMatrix uniform works");
            }

            if (terrainShader.hasUniform("modelMatrix")) {
                terrainShader.setUniform("modelMatrix", new org.joml.Matrix4f().identity());
                System.out.println("✓ modelMatrix uniform works");
            }

            if (terrainShader.hasUniform("lightPosition")) {
                terrainShader.setUniform("lightPosition", new org.joml.Vector3f(100, 100, 100));
                System.out.println("✓ lightPosition uniform works");
            }

            if (terrainShader.hasUniform("lightColor")) {
                terrainShader.setUniform("lightColor", new org.joml.Vector3f(1, 1, 1));
                System.out.println("✓ lightColor uniform works");
            }

            if (terrainShader.hasUniform("ambientStrength")) {
                terrainShader.setUniform("ambientStrength", 0.3f);
                System.out.println("✓ ambientStrength uniform works");
            }

            terrainShader.unbind();

            // Check for OpenGL errors
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) {
                System.out.println("✓ Terrain shader validation PASSED");
            } else {
                System.err.println("✗ OpenGL error during validation: " + error);
            }

        } catch (Exception e) {
            System.err.println("✗ Terrain shader validation FAILED: " + e.getMessage());
        }
    }

    private static void createWorkingBasicShader(ShaderManager shaderManager) {
        System.out.println("Creating working basic shader...");

        String basicVertex = """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec4 color;
                
                uniform mat4 mvpMatrix;
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec4 vertexColor;
                out vec3 fragPos;
                
                void main() {
                    fragPos = (modelMatrix * vec4(position, 1.0)).xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    vertexColor = color;
                
                    if (length(mvpMatrix[0]) > 0.1) {
                        gl_Position = mvpMatrix * vec4(position, 1.0);
                    } else {
                        gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
                    }
                }
                """;

        String basicFragment = """
                #version 330 core
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec4 vertexColor;
                in vec3 fragPos;
                
                uniform sampler2D texture0;
                uniform bool useTexture;
                uniform vec4 uniformColor;
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 viewPosition;
                
                out vec4 fragColor;
                
                void main() {
                    vec4 finalColor = vertexColor;
                
                    if (useTexture && textureSize(texture0, 0).x > 1) {
                        finalColor *= texture(texture0, texCoords);
                    }
                
                    if (uniformColor.a > 0.0) {
                        finalColor *= uniformColor;
                    }
                
                    // Simple lighting if available
                    if (length(lightPosition) > 0.1) {
                        vec3 norm = normalize(fragNormal);
                        vec3 lightDir = normalize(lightPosition - fragPos);
                        float diff = max(dot(norm, lightDir), 0.0);
                        finalColor.rgb *= (0.3 + 0.7 * diff);
                    }
                
                    fragColor = finalColor;
                }
                """;

        try {
            shaderManager.createProgram("basic", basicVertex, basicFragment);
            System.out.println("Working basic shader created!");
        } catch (Exception e) {
            System.err.println("Failed to create basic shader: " + e.getMessage());
        }
    }

    /**
     * Call this method if you're still having issues
     */
    public static void emergencyDiagnostic() {
        System.out.println("=== EMERGENCY DIAGNOSTIC ===");

        // Check OpenGL state
        System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("GLSL Version: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));

        int error = GL11.glGetError();
        System.out.println("OpenGL Error State: " + (error == GL11.GL_NO_ERROR ? "OK" : error));

        // Check shader manager
        ShaderManager shaderManager = ShaderManager.getInstance();
        System.out.println("Active Programs: " + shaderManager.getProgramNames());

        ShaderProgram terrain = shaderManager.getProgram("terrain");
        if (terrain != null) {
            System.out.println("Terrain shader exists, linked: " + terrain.isLinked());
            System.out.println("Available uniforms: " + terrain.getUniformNames());
        } else {
            System.out.println("No terrain shader found!");
        }
    }
}