package com.nak.engine.debug;

// SOLUTION 2: Complete diagnostic and fix system
// Use this to identify and fix shader issues

import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class CompleteShaderDiagnostic {

    /**
     * MAIN METHOD: Call this right after OpenGL initialization
     */
    public static void diagnoseAndFixAllShaders() {
        System.out.println("=".repeat(60));
        System.out.println("COMPLETE SHADER DIAGNOSTIC AND FIX SYSTEM");
        System.out.println("=".repeat(60));

        // Step 1: Check OpenGL state
        checkOpenGLState();

        // Step 2: Diagnose current shader system
        diagnoseShaderSystem();

        // Step 3: Fix terrain shader specifically
        fixTerrainShader();

        // Step 4: Test all shaders
        testAllShaders();

        // Step 5: Provide recommendations
        provideRecommendations();

        System.out.println("=".repeat(60));
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("=".repeat(60));
    }

    private static void checkOpenGLState() {
        System.out.println("\n1. OPENGL STATE CHECK");
        System.out.println("-".repeat(30));

        try {
            String version = GL11.glGetString(GL11.GL_VERSION);
            String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            String vendor = GL11.glGetString(GL11.GL_VENDOR);

            System.out.println("OpenGL Version: " + version);
            System.out.println("GLSL Version: " + glslVersion);
            System.out.println("Renderer: " + renderer);
            System.out.println("Vendor: " + vendor);

            // Check for errors
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) {
                System.out.println("✓ OpenGL state: OK");
            } else {
                System.err.println("✗ OpenGL error: " + error);
            }

            // Check minimum requirements
            if (version != null && version.contains("3.3")) {
                System.out.println("✓ OpenGL 3.3+ supported");
            } else {
                System.err.println("✗ Warning: OpenGL 3.3 may not be supported");
            }

        } catch (Exception e) {
            System.err.println("✗ Error checking OpenGL state: " + e.getMessage());
        }
    }

    private static void diagnoseShaderSystem() {
        System.out.println("\n2. SHADER SYSTEM DIAGNOSIS");
        System.out.println("-".repeat(30));

        ShaderManager shaderManager = ShaderManager.getInstance();

        System.out.println("Shader Manager Status:");
        System.out.println(shaderManager.getDiagnosticInfo());

        // Check each shader program
        for (String programName : shaderManager.getProgramNames()) {
            ShaderProgram program = shaderManager.getProgram(programName);
            diagnoseSingleShader(programName, program);
        }
    }

    private static void diagnoseSingleShader(String name, ShaderProgram program) {
        System.out.println("\nShader Program: " + name);

        if (program == null) {
            System.err.println("  ✗ Program is NULL");
            return;
        }

        System.out.println("  Program ID: " + program.getProgramId());
        System.out.println("  Linked: " + program.isLinked());

        if (!program.isLinked()) {
            System.err.println("  ✗ Program not linked");
            return;
        }

        // Check link status
        int linkStatus = GL20.glGetProgrami(program.getProgramId(), GL20.GL_LINK_STATUS);
        if (linkStatus == GL11.GL_TRUE) {
            System.out.println("  ✓ Link status: SUCCESS");
        } else {
            System.err.println("  ✗ Link status: FAILED");
            String infoLog = GL20.glGetProgramInfoLog(program.getProgramId());
            System.err.println("  Link error: " + infoLog);
            return;
        }

        // Check validation
        GL20.glValidateProgram(program.getProgramId());
        int validateStatus = GL20.glGetProgrami(program.getProgramId(), GL20.GL_VALIDATE_STATUS);
        if (validateStatus == GL11.GL_TRUE) {
            System.out.println("  ✓ Validation: SUCCESS");
        } else {
            System.err.println("  ✗ Validation: FAILED");
            String infoLog = GL20.glGetProgramInfoLog(program.getProgramId());
            System.err.println("  Validation error: " + infoLog);
        }

        // List uniforms
        System.out.println("  Uniforms (" + program.getUniformNames().size() + "):");
        for (String uniform : program.getUniformNames()) {
            System.out.println("    - " + uniform);
        }

        // Check terrain-specific uniforms
        if (name.equals("terrain")) {
            checkTerrainSpecificUniforms(program);
        }
    }

    private static void checkTerrainSpecificUniforms(ShaderProgram program) {
        String[] requiredUniforms = {
                "projectionMatrix", "viewMatrix", "modelMatrix",
                "lightPosition", "lightColor", "lightDirection",
                "ambientStrength", "specularStrength", "shininess",
                "viewPosition", "fogColor", "fogDensity"
        };

        System.out.println("  Terrain uniform check:");
        boolean allPresent = true;

        for (String uniform : requiredUniforms) {
            boolean present = program.hasUniform(uniform);
            System.out.println("    " + uniform + ": " + (present ? "✓" : "✗"));
            if (!present) allPresent = false;
        }

        if (!allPresent) {
            System.err.println("  ✗ MISSING REQUIRED UNIFORMS - This will cause blue terrain!");
        } else {
            System.out.println("  ✓ All required uniforms present");
        }
    }

    private static void fixTerrainShader() {
        System.out.println("\n3. TERRAIN SHADER FIX");
        System.out.println("-".repeat(30));

        ShaderManager shaderManager = ShaderManager.getInstance();

        // Remove broken terrain shader if it exists
        ShaderProgram existing = shaderManager.getProgram("terrain");
        if (existing != null) {
            System.out.println("Removing existing terrain shader...");
            existing.cleanup();
        }

        // Create guaranteed working terrain shader
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
                
                    // Ensure valid vertex color
                    vertexColor = color;
                    if (length(vertexColor) < 0.01) {
                        float h = position.y;
                        if (h < 0.0) {
                            vertexColor = vec3(0.2, 0.4, 0.8);      // Water
                        } else if (h < 10.0) {
                            vertexColor = vec3(0.2, 0.6, 0.15);     // Grass
                        } else if (h < 25.0) {
                            vertexColor = vec3(0.5, 0.4, 0.3);      // Rock
                        } else {
                            vertexColor = vec3(0.8, 0.8, 0.9);      // Snow
                        }
                    }
                
                    height = position.y;
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

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
                    // Safe normal calculation
                    vec3 norm = normalize(fragNormal);
                    if (length(norm) < 0.1) {
                        norm = vec3(0.0, 1.0, 0.0);
                    }
                
                    // Safe vertex color
                    vec3 baseColor = vertexColor;
                    if (length(baseColor) < 0.1) {
                        baseColor = vec3(0.3, 0.6, 0.2);
                    }
                
                    // Lighting calculation
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    float safeAmbient = max(ambientStrength, 0.2);
                    vec3 ambient = safeAmbient * lightColor;
                
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), max(shininess, 1.0));
                    vec3 specular = specularStrength * spec * lightColor;
                
                    vec3 lighting = ambient + diffuse + specular;
                    vec3 finalColor = baseColor * lighting;
                
                    // Fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    // Ensure never completely black
                    finalColor = max(finalColor, vec3(0.05));
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            ShaderProgram terrainShader = shaderManager.createProgram(
                    "terrain", fixedVertexShader, fixedFragmentShader
            );

            System.out.println("✓ Fixed terrain shader created successfully");

            // Verify it has all required uniforms
            checkTerrainSpecificUniforms(terrainShader);

        } catch (Exception e) {
            System.err.println("✗ Failed to create fixed terrain shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAllShaders() {
        System.out.println("\n4. SHADER TESTING");
        System.out.println("-".repeat(30));

        ShaderManager shaderManager = ShaderManager.getInstance();

        for (String programName : shaderManager.getProgramNames()) {
            testShaderWithDummyData(programName);
        }
    }

    private static void testShaderWithDummyData(String programName) {
        ShaderManager shaderManager = ShaderManager.getInstance();
        ShaderProgram program = shaderManager.getProgram(programName);

        if (program == null) {
            System.err.println("✗ Cannot test shader '" + programName + "' - not found");
            return;
        }

        System.out.println("Testing shader: " + programName);

        try {
            program.bind();

            // Set common uniforms with dummy data
            setDummyUniforms(program);

            program.unbind();

            // Check for OpenGL errors
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) {
                System.out.println("  ✓ Test PASSED");
            } else {
                System.err.println("  ✗ Test FAILED - OpenGL error: " + error);
            }

        } catch (Exception e) {
            System.err.println("  ✗ Test FAILED - Exception: " + e.getMessage());
        }
    }

    private static void setDummyUniforms(ShaderProgram program) {
        // Set all possible uniforms with safe dummy values
        Matrix4f identity = new Matrix4f().identity();
        Vector3f defaultVec3 = new Vector3f(1, 1, 1);

        if (program.hasUniform("projectionMatrix")) {
            program.setUniform("projectionMatrix", identity);
        }
        if (program.hasUniform("viewMatrix")) {
            program.setUniform("viewMatrix", identity);
        }
        if (program.hasUniform("modelMatrix")) {
            program.setUniform("modelMatrix", identity);
        }
        if (program.hasUniform("lightSpaceMatrix")) {
            program.setUniform("lightSpaceMatrix", identity);
        }
        if (program.hasUniform("lightPosition")) {
            program.setUniform("lightPosition", new Vector3f(100, 100, 100));
        }
        if (program.hasUniform("lightColor")) {
            program.setUniform("lightColor", defaultVec3);
        }
        if (program.hasUniform("lightDirection")) {
            program.setUniform("lightDirection", new Vector3f(0, -1, 0));
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
            program.setUniform("viewPosition", new Vector3f(0, 10, 3));
        }
        if (program.hasUniform("fogColor")) {
            program.setUniform("fogColor", new Vector3f(0.5f, 0.6f, 0.7f));
        }
        if (program.hasUniform("fogDensity")) {
            program.setUniform("fogDensity", 0.008f);
        }
        if (program.hasUniform("time")) {
            program.setUniform("time", 1.0f);
        }
        if (program.hasUniform("dayNightCycle")) {
            program.setUniform("dayNightCycle", 0.5f);
        }
        if (program.hasUniform("sunDirection")) {
            program.setUniform("sunDirection", new Vector3f(0.3f, -0.7f, 0.5f));
        }
        if (program.hasUniform("sunColor")) {
            program.setUniform("sunColor", defaultVec3);
        }
    }

    private static void provideRecommendations() {
        System.out.println("\n5. RECOMMENDATIONS");
        System.out.println("-".repeat(30));

        ShaderManager shaderManager = ShaderManager.getInstance();
        ShaderProgram terrainShader = shaderManager.getProgram("terrain");

        if (terrainShader == null || !terrainShader.isLinked()) {
            System.out.println("❌ CRITICAL: No working terrain shader");
            System.out.println("   → Run CompleteShaderDiagnostic.fixTerrainShader()");
            System.out.println("   → Check console for shader compilation errors");
            return;
        }

        // Check if terrain has all required uniforms
        String[] required = {
                "projectionMatrix", "viewMatrix", "modelMatrix",
                "lightPosition", "lightColor", "ambientStrength", "viewPosition"
        };

        boolean hasAllRequired = true;
        for (String uniform : required) {
            if (!terrainShader.hasUniform(uniform)) {
                hasAllRequired = false;
                break;
            }
        }

        if (!hasAllRequired) {
            System.out.println("❌ PROBLEM: Terrain shader missing required uniforms");
            System.out.println("   → This will cause blue terrain with no lighting");
            System.out.println("   → Run CompleteShaderDiagnostic.fixTerrainShader()");
        } else {
            System.out.println("✅ SUCCESS: Terrain shader has all required uniforms");
        }

        System.out.println("\n💡 GENERAL RECOMMENDATIONS:");
        System.out.println("1. Remove #include directives from shader files - they cause compilation issues");
        System.out.println("2. Always check shader compilation/linking status before use");
        System.out.println("3. Set ambientStrength >= 0.2 to avoid completely black terrain");
        System.out.println("4. Ensure vertex colors are valid (not all zeros)");
        System.out.println("5. Call this diagnostic after any shader changes");

        System.out.println("\n🔧 TO FIX BLUE TERRAIN:");
        System.out.println("1. Make sure terrain shader compiles successfully");
        System.out.println("2. Check that all lighting uniforms are being set");
        System.out.println("3. Verify ambientStrength > 0");
        System.out.println("4. Ensure vertex colors are calculated correctly");
        System.out.println("5. Test with higher ambient lighting (0.5+) first");
    }

    /**
     * Quick fix method you can call from anywhere
     */
    public static void quickFixTerrain() {
        System.out.println("QUICK TERRAIN FIX - Creating guaranteed working shader...");

        try {
            fixTerrainShader();

            // Test the fixed shader
            testShaderWithDummyData("terrain");

            System.out.println("✓ Quick terrain fix completed");

        } catch (Exception e) {
            System.err.println("✗ Quick fix failed: " + e.getMessage());
        }
    }

    /**
     * Emergency method - creates absolute minimal working shader
     */
    public static void createEmergencyShader() {
        System.out.println("EMERGENCY: Creating minimal working shader...");

        String emergencyVertex = """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 4) in vec3 color;
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                out vec3 vertexColor;
                void main() {
                    vertexColor = length(color) > 0.1 ? color : vec3(0.2, 0.6, 0.1);
                    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
                }
                """;

        String emergencyFragment = """
                #version 330 core
                in vec3 vertexColor;
                uniform float ambientStrength;
                out vec4 fragColor;
                void main() {
                    float ambient = max(ambientStrength, 0.5);
                    fragColor = vec4(vertexColor * ambient, 1.0);
                }
                """;

        try {
            ShaderManager shaderManager = ShaderManager.getInstance();
            shaderManager.createProgram("terrain", emergencyVertex, emergencyFragment);
            System.out.println("✓ Emergency shader created - terrain should now be visible");
        } catch (Exception e) {
            System.err.println("✗ Emergency shader failed: " + e.getMessage());
        }
    }
}