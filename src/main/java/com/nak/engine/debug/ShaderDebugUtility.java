package com.nak.engine.debug;

import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Utility class for debugging shader compilation and linking issues
 */
public class ShaderDebugUtility {

    public static void debugShaderSystem() {
        System.out.println("=== SHADER SYSTEM DEBUG ===");

        ShaderManager shaderManager = ShaderManager.getInstance();

        // Check OpenGL context
        checkOpenGLContext();

        // Check shader manager state
        System.out.println("Shader Manager Diagnostic:");
        System.out.println(shaderManager.getDiagnosticInfo());

        // Test each program
        for (String programName : shaderManager.getProgramNames()) {
            debugShaderProgram(programName, shaderManager.getProgram(programName));
        }

        System.out.println("=== END SHADER DEBUG ===");
    }

    private static void checkOpenGLContext() {
        System.out.println("OpenGL Context Check:");
        System.out.println("  OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("  GLSL Version: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("  Renderer: " + GL11.glGetString(GL11.GL_RENDERER));
        System.out.println("  Vendor: " + GL11.glGetString(GL11.GL_VENDOR));

        // Check for errors
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            System.err.println("  OpenGL Error: " + error);
        } else {
            System.out.println("  OpenGL State: OK");
        }
        System.out.println();
    }

    private static void debugShaderProgram(String name, ShaderProgram program) {
        System.out.println("Shader Program: " + name);

        if (program == null) {
            System.err.println("  ERROR: Program is null!");
            return;
        }

        System.out.println("  Program ID: " + program.getProgramId());
        System.out.println("  Linked: " + program.isLinked());
        System.out.println("  In Use: " + program.isInUse());

        // Check program status
        int linkStatus = GL20.glGetProgrami(program.getProgramId(), GL20.GL_LINK_STATUS);
        System.out.println("  Link Status: " + (linkStatus == GL11.GL_TRUE ? "SUCCESS" : "FAILED"));

        if (linkStatus != GL11.GL_TRUE) {
            String infoLog = GL20.glGetProgramInfoLog(program.getProgramId());
            System.err.println("  Link Error: " + infoLog);
        }

        // Check validation status
        GL20.glValidateProgram(program.getProgramId());
        int validateStatus = GL20.glGetProgrami(program.getProgramId(), GL20.GL_VALIDATE_STATUS);
        System.out.println("  Validate Status: " + (validateStatus == GL11.GL_TRUE ? "SUCCESS" : "FAILED"));

        if (validateStatus != GL11.GL_TRUE) {
            String infoLog = GL20.glGetProgramInfoLog(program.getProgramId());
            System.err.println("  Validation Warning: " + infoLog);
        }

        // List uniforms
        System.out.println("  Uniforms:");
        for (String uniformName : program.getUniformNames()) {
            System.out.println("    - " + uniformName);
        }

        // Check specific terrain shader uniforms
        if (name.equals("terrain")) {
            checkTerrainShaderUniforms(program);
        }

        System.out.println();
    }

    private static void checkTerrainShaderUniforms(ShaderProgram program) {
        String[] requiredUniforms = {
                "projectionMatrix", "viewMatrix", "modelMatrix",
                "lightPosition", "lightColor", "lightDirection",
                "ambientStrength", "specularStrength", "shininess",
                "viewPosition", "fogColor", "fogDensity"
        };

        System.out.println("  Required Terrain Uniforms Check:");
        boolean allPresent = true;

        for (String uniform : requiredUniforms) {
            boolean present = program.hasUniform(uniform);
            System.out.println("    " + uniform + ": " + (present ? "✓" : "✗"));
            if (!present) allPresent = false;
        }

        if (!allPresent) {
            System.err.println("  WARNING: Missing required uniforms for terrain shader!");
            System.err.println("  This will cause blue terrain with no lighting.");
        }
    }

    /**
     * Quick fix for common shader issues
     */
    public static void applyShaderFixes() {
        System.out.println("Applying shader fixes...");

        ShaderManager shaderManager = ShaderManager.getInstance();

        // Force recreation of terrain shader without includes
        createFixedTerrainShader(shaderManager);

        System.out.println("Shader fixes applied.");
    }

    private static void createFixedTerrainShader(ShaderManager shaderManager) {
        // Simple terrain vertex shader without includes
        String fixedVertexShader = """
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
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    vertexColor = color;
                    height = position.y;
                
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

        // Simple terrain fragment shader without includes
        String fixedFragmentShader = """
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
                    // Ensure we have valid vertex color
                    vec3 baseColor = vertexColor;
                    if (length(baseColor) < 0.1) {
                        // Fallback color if vertex color is too dark
                        baseColor = vec3(0.3, 0.6, 0.2); // Green
                    }
                
                    // Calculate lighting
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    vec3 lighting = ambient + diffuse + specular;
                    vec3 finalColor = baseColor * lighting;
                
                    // Apply fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            // Remove existing terrain shader if it exists
            ShaderProgram existingTerrain = shaderManager.getProgram("terrain");
            if (existingTerrain != null) {
                existingTerrain.cleanup();
            }

            // Create the fixed terrain shader
            shaderManager.createProgram("terrain", fixedVertexShader, fixedFragmentShader);
            System.out.println("Created fixed terrain shader without includes");

        } catch (Exception e) {
            System.err.println("Failed to create fixed terrain shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test shader with dummy data to verify it works
     */
    public static void testShaderWithDummyData(String programName) {
        ShaderManager shaderManager = ShaderManager.getInstance();
        ShaderProgram program = shaderManager.getProgram(programName);

        if (program == null) {
            System.err.println("Cannot test shader '" + programName + "' - not found");
            return;
        }

        System.out.println("Testing shader: " + programName);

        try {
            program.bind();

            // Set dummy uniforms
            if (program.hasUniform("projectionMatrix")) {
                program.setUniform("projectionMatrix", new org.joml.Matrix4f().identity());
            }
            if (program.hasUniform("viewMatrix")) {
                program.setUniform("viewMatrix", new org.joml.Matrix4f().identity());
            }
            if (program.hasUniform("modelMatrix")) {
                program.setUniform("modelMatrix", new org.joml.Matrix4f().identity());
            }
            if (program.hasUniform("lightPosition")) {
                program.setUniform("lightPosition", new org.joml.Vector3f(100, 100, 100));
            }
            if (program.hasUniform("lightColor")) {
                program.setUniform("lightColor", new org.joml.Vector3f(1, 1, 1));
            }
            if (program.hasUniform("lightDirection")) {
                program.setUniform("lightDirection", new org.joml.Vector3f(0, -1, 0));
            }
            if (program.hasUniform("ambientStrength")) {
                program.setUniform("ambientStrength", 0.3f);
            }
            if (program.hasUniform("specularStrength")) {
                program.setUniform("specularStrength", 0.5f);
            }
            if (program.hasUniform("shininess")) {
                program.setUniform("shininess", 32.0f);
            }
            if (program.hasUniform("viewPosition")) {
                program.setUniform("viewPosition", new org.joml.Vector3f(0, 10, 3));
            }
            if (program.hasUniform("fogColor")) {
                program.setUniform("fogColor", new org.joml.Vector3f(0.5f, 0.6f, 0.7f));
            }
            if (program.hasUniform("fogDensity")) {
                program.setUniform("fogDensity", 0.008f);
            }

            program.unbind();

            // Check for OpenGL errors
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) {
                System.out.println("  Shader test PASSED - no OpenGL errors");
            } else {
                System.err.println("  Shader test FAILED - OpenGL error: " + error);
            }

        } catch (Exception e) {
            System.err.println("  Shader test FAILED - Exception: " + e.getMessage());
        }
    }

    /**
     * Print detailed information about terrain vertex colors
     */
    public static void debugVertexColors() {
        System.out.println("=== VERTEX COLOR DEBUG ===");
        System.out.println("Check TerrainChunk.calculateEnhancedVertexColor() method:");
        System.out.println("- Ensure height ranges are correct");
        System.out.println("- Verify color values are in [0,1] range");
        System.out.println("- Check that colors are not all blue (0,0,1)");
        System.out.println();

        // Simulate vertex color calculation for different heights
        System.out.println("Sample vertex colors for different heights:");
        float[] testHeights = {-5.0f, 0.0f, 5.0f, 15.0f, 25.0f, 35.0f};

        for (float height : testHeights) {
            org.joml.Vector3f color = simulateVertexColor(height);
            System.out.printf("  Height %.1f: RGB(%.2f, %.2f, %.2f)\n",
                    height, color.x, color.y, color.z);
        }
    }

    private static org.joml.Vector3f simulateVertexColor(float height) {
        // Simplified version of the vertex color calculation
        org.joml.Vector3f color = new org.joml.Vector3f();

        if (height < -1.0f) {
            // Water - blue
            color.set(0.2f, 0.4f, 0.8f);
        } else if (height < 8.0f) {
            // Grassland - green
            color.set(0.15f, 0.5f, 0.1f);
        } else if (height < 20.0f) {
            // Hills - brown/green mix
            float transition = (height - 8.0f) / 12.0f;
            color.set(0.3f + transition * 0.3f, 0.4f - transition * 0.1f, 0.2f);
        } else if (height < 35.0f) {
            // Rocky terrain
            color.set(0.5f, 0.4f, 0.3f);
        } else {
            // Snow
            color.set(0.9f, 0.9f, 0.95f);
        }

        return color;
    }
}