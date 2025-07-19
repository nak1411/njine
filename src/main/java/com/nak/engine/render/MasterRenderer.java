// Fixed MasterRenderer.java with proper lighting setup
package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import com.nak.engine.shader.ShaderReloadListener;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import com.nak.engine.util.ShaderUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer implements ShaderReloadListener {

    // Shader manager
    private final ShaderManager shaderManager;

    // Rendering components
    private final TerrainManager terrainManager;
    private final AtmosphericRenderer atmosphericRenderer;
    private final ParticleRenderer particleRenderer;
    private final SkyRenderer skyRenderer;
    private final PostProcessor postProcessor;
    private final UIRenderer uiRenderer;
    private final ObjectRenderer objectRenderer;

    // Matrices
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();

    // Lighting system - FIXED: Initialize with proper values
    private final Vector3f sunDirection = new Vector3f(0.3f, -0.7f, 0.5f).normalize();
    private final Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.8f);
    private final Vector3f ambientColor = new Vector3f(0.3f, 0.35f, 0.5f);
    private final Vector3f lightPosition = new Vector3f(100.0f, 100.0f, 100.0f);
    private float dayNightCycle = 0.0f;

    // Lighting parameters - FIXED: Set proper default values
    private float ambientStrength = 0.3f;
    private float specularStrength = 0.5f;
    private float shininess = 32.0f;

    // Rendering settings
    private boolean wireframeEnabled = false;
    private boolean debugRenderingEnabled = false;
    private float fogDensity = 0.008f;
    private final Vector3f fogColor = new Vector3f(0.5f, 0.6f, 0.7f);

    public MasterRenderer(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;
        this.shaderManager = ShaderManager.getInstance();

        // Initialize rendering components
        this.atmosphericRenderer = new AtmosphericRenderer();
        this.particleRenderer = new ParticleRenderer();
        this.skyRenderer = new SkyRenderer();
        this.postProcessor = new PostProcessor();
        this.uiRenderer = new UIRenderer();
        this.objectRenderer = new ObjectRenderer();

        // Setup shader system
        initializeShaders();

        // Listen for shader reloads
        shaderManager.addReloadListener(this);

        // Configure OpenGL state
        setupOpenGLState();
    }

    private void initializeShaders() {
        // Set shader directory
        shaderManager.setShaderDirectory("src/main/resources/shaders");

        // Enable hot reload in debug mode
        boolean isDevelopment = Boolean.parseBoolean(System.getProperty("development", "false"));
        shaderManager.setHotReloadEnabled(isDevelopment);

        // Load shader programs
        loadShaderPrograms();

        System.out.println("Initialized shader system with programs: " + shaderManager.getProgramNames());
    }

    private void loadShaderPrograms() {
        try {
            // FIXED: Load shaders without problematic includes first
            loadBasicShaders();

            // Then try to load from files
            tryLoadShaderFromFiles();
        } catch (Exception e) {
            System.out.println("Loading shaders from files failed, using built-in shaders: " + e.getMessage());
        }

        // Validate that we have all required shaders
        validateShaders();
    }

    // FIXED: Load basic shaders without includes to avoid compilation issues
    private void loadBasicShaders() {
        // Create a simple terrain shader without includes
        String simpleTerrainVert = """
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
                out vec3 fragTangent;
                out vec3 vertexColor;
                out vec4 fragPosLightSpace;
                out float height;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    fragTangent = mat3(transpose(inverse(modelMatrix))) * tangent;
                    vertexColor = color;
                    fragPosLightSpace = lightSpaceMatrix * worldPos;
                    height = position.y;
                
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;

        String simpleTerrainFrag = """
                #version 330 core
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 fragTangent;
                in vec3 vertexColor;
                in vec4 fragPosLightSpace;
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
                
                vec3 calculateLighting() {
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
                
                    return ambient + diffuse + specular;
                }
                
                void main() {
                    vec3 lighting = calculateLighting();
                    vec3 finalColor = vertexColor * lighting;
                
                    // Apply fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;

        try {
            shaderManager.createProgram("terrain", simpleTerrainVert, simpleTerrainFrag);
            System.out.println("Created simple terrain shader");
        } catch (Exception e) {
            System.err.println("Failed to create simple terrain shader: " + e.getMessage());
        }
    }

    private void tryLoadShaderFromFiles() {
        // Try to load terrain shader from files (this might fail due to includes)
        try {
            shaderManager.loadProgram("terrain_advanced", "terrain.vert", "terrain.frag");
            System.out.println("Loaded advanced terrain shader from files");
        } catch (Exception e) {
            System.out.println("Could not load advanced terrain shader: " + e.getMessage());
        }

        // Load other shaders
        try {
            shaderManager.loadProgram("skybox", "skybox.vert", "skybox.frag");
            shaderManager.loadProgram("particle", "particle.vert", "particle.frag");
            shaderManager.loadProgram("ui", "ui.vert", "ui.frag");
        } catch (Exception e) {
            System.out.println("Could not load additional shaders: " + e.getMessage());
        }
    }

    private void validateShaders() {
        String[] requiredPrograms = {"terrain", "basic", "skybox", "ui"};

        for (String programName : requiredPrograms) {
            ShaderProgram program = shaderManager.getProgram(programName);
            if (program == null) {
                System.err.println("Missing required shader program: " + programName);
            } else {
                validateProgramUniforms(program);
            }
        }
    }

    private void validateProgramUniforms(ShaderProgram program) {
        switch (program.getName()) {
            case "terrain":
                ShaderUtils.validateRequiredUniforms(program,
                        "projectionMatrix", "viewMatrix", "modelMatrix",
                        "lightPosition", "lightColor", "viewPosition");
                break;
            case "skybox":
                ShaderUtils.validateRequiredUniforms(program,
                        "projectionMatrix", "viewMatrix");
                break;
            case "ui":
                ShaderUtils.validateRequiredUniforms(program,
                        "projection");
                break;
        }
    }

    private void setupOpenGLState() {
        // Enable depth testing with optimizations
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);

        // Enable back-face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // Set clear color
        glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
    }

    /**
     * FIXED: Main render method with proper lighting setup
     */
    public void render(GameState gameState, Camera camera, float interpolation) {
        // Update time-based effects
        updateTimeBasedEffects(gameState.getTime());

        // Setup matrices
        setupMatrices(camera);

        // Begin frame
        beginFrame();

        // Render sky
        renderSkyWithShaders(camera);

        // FIXED: Render terrain with proper lighting
        renderTerrainWithProperLighting(camera);

        // Render objects
        renderObjectsWithShaders(gameState);

        // Render particles
        renderParticlesWithShaders(camera, gameState.getTime());

        // Post-processing
        if (postProcessor.isEnabled()) {
            postProcessor.process();
        }

        // Render UI
        renderUIWithShaders(camera, gameState);

        // End frame
        endFrame();
    }

    private void setupMatrices(Camera camera) {
        projectionMatrix.set(camera.getProjectionMatrix());
        viewMatrix.set(camera.getViewMatrix());
        modelMatrix.identity();
    }

    private void beginFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (wireframeEnabled) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }

    private void renderSkyWithShaders(Camera camera) {
        ShaderProgram skyboxProgram = shaderManager.getProgram("skybox");
        if (skyboxProgram != null) {
            skyboxProgram.bind();

            // Set camera uniforms
            ShaderUtils.setCameraUniforms(skyboxProgram, viewMatrix, projectionMatrix, camera.getPosition());

            // Set sky-specific uniforms
            skyboxProgram.setUniform("dayNightCycle", dayNightCycle);
            skyboxProgram.setUniform("sunDirection", sunDirection);
            skyboxProgram.setUniform("sunColor", sunColor);

            glDisable(GL_DEPTH_TEST);
            skyRenderer.render(camera, dayNightCycle, sunDirection, sunColor);
            glEnable(GL_DEPTH_TEST);

            skyboxProgram.unbind();
        }
    }

    private void renderTerrainWithProperLighting(Camera camera) {
        ShaderProgram terrainProgram = shaderManager.getProgram("terrain");

        // Check if we have terrain to render
        if (terrainManager.getVisibleChunkCount() == 0) {
            // Try to render without shader as fallback
            renderTerrainFallback();
            return;
        }

        if (terrainProgram != null && terrainProgram.isLinked()) {
            // Clear any existing OpenGL errors
            while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                // Clear error queue
            }

            try {
                terrainProgram.bind();

                // Check if binding succeeded
                int error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    System.err.println("Error binding terrain shader: " + error);
                    terrainProgram.unbind();
                    renderTerrainFallback();
                    return;
                }

                // Set uniforms safely - only set ones that exist
                setupTerrainUniforms(terrainProgram, camera);

                // Check for errors after setting uniforms
                error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    System.err.println("Error setting terrain uniforms: " + error);
                    terrainProgram.unbind();
                    renderTerrainFallback();
                    return;
                }

                // Render terrain chunks
                terrainManager.render();

                // Check for rendering errors
                error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    System.err.println("Error rendering terrain: " + error);
                }

                terrainProgram.unbind();

            } catch (Exception e) {
                System.err.println("Exception in terrain rendering: " + e.getMessage());
                e.printStackTrace();

                if (terrainProgram.isInUse()) {
                    terrainProgram.unbind();
                }

                renderTerrainFallback();
            }

        } else {
            System.err.println("No valid terrain shader available for rendering!");
            renderTerrainFallback();
        }
    }

    private void setupTerrainUniforms(ShaderProgram terrainProgram, Camera camera) {
        // Camera uniforms
        if (terrainProgram.hasUniform("projectionMatrix")) {
            terrainProgram.setUniform("projectionMatrix", projectionMatrix);
        }
        if (terrainProgram.hasUniform("viewMatrix")) {
            terrainProgram.setUniform("viewMatrix", viewMatrix);
        }
        if (terrainProgram.hasUniform("modelMatrix")) {
            terrainProgram.setUniform("modelMatrix", modelMatrix);
        }
        if (terrainProgram.hasUniform("viewPosition")) {
            terrainProgram.setUniform("viewPosition", camera.getPosition());
        }

        // Lighting uniforms
        if (terrainProgram.hasUniform("lightPosition")) {
            terrainProgram.setUniform("lightPosition", lightPosition);
        }
        if (terrainProgram.hasUniform("lightColor")) {
            terrainProgram.setUniform("lightColor", sunColor);
        }
        if (terrainProgram.hasUniform("lightDirection")) {
            terrainProgram.setUniform("lightDirection", sunDirection);
        }
        if (terrainProgram.hasUniform("ambientStrength")) {
            terrainProgram.setUniform("ambientStrength", ambientStrength);
        }
        if (terrainProgram.hasUniform("specularStrength")) {
            terrainProgram.setUniform("specularStrength", specularStrength);
        }
        if (terrainProgram.hasUniform("shininess")) {
            terrainProgram.setUniform("shininess", shininess);
        }

        // Fog uniforms
        if (terrainProgram.hasUniform("fogColor")) {
            terrainProgram.setUniform("fogColor", fogColor);
        }
        if (terrainProgram.hasUniform("fogDensity")) {
            terrainProgram.setUniform("fogDensity", fogDensity);
        }

        // Light space matrix (for shadows)
        if (terrainProgram.hasUniform("lightSpaceMatrix")) {
            Matrix4f lightSpaceMatrix = new Matrix4f().identity();
            terrainProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix);
        }

        // Texture scale
        if (terrainProgram.hasUniform("textureScale")) {
            terrainProgram.setUniform("textureScale", 16.0f);
        }

        // Shadow bias
        if (terrainProgram.hasUniform("shadowBias")) {
            terrainProgram.setUniform("shadowBias", 0.005f);
        }
    }

    private void renderTerrainFallback() {
        // Simple fallback rendering without shaders
        try {
            glDisable(GL_LIGHTING);
            glColor3f(0.3f, 0.6f, 0.2f); // Green color

            // Render terrain manager
            terrainManager.render();

            glEnable(GL_LIGHTING);

        } catch (Exception e) {
            System.err.println("Error in terrain fallback rendering: " + e.getMessage());
        }
    }

    private void renderObjectsWithShaders(GameState gameState) {
        ShaderProgram basicProgram = shaderManager.getProgram("basic");
        if (basicProgram != null) {
            basicProgram.bind();

            // Set MVP matrix for basic rendering
            Matrix4f mvpMatrix = new Matrix4f(projectionMatrix).mul(viewMatrix).mul(modelMatrix);
            basicProgram.setUniform("mvpMatrix", mvpMatrix);

            // Set lighting for objects too
            if (basicProgram.hasUniform("lightPosition")) {
                basicProgram.setUniform("lightPosition", lightPosition);
                basicProgram.setUniform("lightColor", sunColor);
            }

            // Render objects here
            // objectRenderer.render();

            basicProgram.unbind();
        }
    }

    private void renderParticlesWithShaders(Camera camera, float time) {
        ShaderProgram particleProgram = shaderManager.getProgram("particle");
        if (particleProgram == null) {
            particleProgram = shaderManager.getProgram("basic");
        }

        if (particleProgram != null) {
            particleProgram.bind();

            glDisable(GL_LIGHTING);
            glEnable(GL_BLEND);
            glDepthMask(false);

            ShaderUtils.setCameraUniforms(particleProgram, viewMatrix, projectionMatrix, camera.getPosition());
            ShaderUtils.setTimeUniform(particleProgram, time);

            particleRenderer.render(camera, time);

            glDepthMask(true);
            glDisable(GL_BLEND);
            glEnable(GL_LIGHTING);

            particleProgram.unbind();
        }
    }

    private void renderUIWithShaders(Camera camera, GameState gameState) {
        ShaderProgram uiProgram = shaderManager.getProgram("ui");
        if (uiProgram != null) {
            glDisable(GL_DEPTH_TEST);

            uiProgram.bind();

            // Setup 2D projection
            Matrix4f orthoProjection = new Matrix4f().ortho(0, 1920, 1080, 0, -1, 1);
            uiProgram.setUniform("projection", orthoProjection);

            // Render UI elements
            uiRenderer.renderCrosshair();

            if (debugRenderingEnabled) {
                uiRenderer.renderDebugInfo(getDebugInfo(camera));
            }

            uiProgram.unbind();

            glEnable(GL_DEPTH_TEST);
        }
    }

    private void endFrame() {
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glFlush();
    }

    private void updateTimeBasedEffects(float time) {
        // Update day/night cycle
        dayNightCycle += 0.001f;
        if (dayNightCycle > 2 * Math.PI) {
            dayNightCycle -= 2 * Math.PI;
        }

        // Calculate sun position and colors
        float sunHeight = (float) Math.sin(dayNightCycle);
        sunDirection.set(
                (float) Math.cos(dayNightCycle + Math.PI / 2) * 0.6f,
                sunHeight,
                (float) Math.sin(dayNightCycle + Math.PI / 2) * 0.3f
        ).normalize();

        // Update light position based on sun direction
        lightPosition.set(sunDirection).mul(1000.0f);

        // Update lighting colors based on time of day
        calculateLightingColors(sunHeight);
        updateFogSettings(sunHeight);
    }

    private void calculateLightingColors(float sunHeight) {
        if (sunHeight > 0) {
            float intensity = Math.min(1.0f, sunHeight * 2.0f);
            sunColor.set(1.0f, 0.95f + intensity * 0.05f, 0.8f + intensity * 0.2f);
            ambientColor.set(0.3f + intensity * 0.3f, 0.35f + intensity * 0.35f, 0.5f + intensity * 0.3f);
            ambientStrength = 0.3f + intensity * 0.2f;
        } else {
            float moonlight = Math.max(0, -sunHeight * 0.5f);
            sunColor.set(0.4f + moonlight * 0.2f, 0.4f + moonlight * 0.2f, 0.6f + moonlight * 0.3f);
            ambientColor.set(0.05f + moonlight * 0.1f, 0.05f + moonlight * 0.1f, 0.15f + moonlight * 0.2f);
            ambientStrength = 0.1f + moonlight * 0.2f;
        }
    }

    private void updateFogSettings(float sunHeight) {
        if (sunHeight > 0) {
            fogDensity = 0.005f + (1.0f - sunHeight) * 0.003f;
            fogColor.set(0.6f, 0.7f, 0.9f);
        } else {
            fogDensity = 0.012f;
            fogColor.set(0.1f, 0.1f, 0.2f);
        }
    }

    private String getDebugInfo(Camera camera) {
        Vector3f pos = camera.getPosition();
        return String.format(
                "=== ENHANCED RENDER DEBUG ===\n" +
                        "Position: %.1f, %.1f, %.1f\n" +
                        "FOV: %.1f°\n" +
                        "Day/Night: %.1f°\n" +
                        "Fog Density: %.4f\n" +
                        "Light Position: %.1f, %.1f, %.1f\n" +
                        "Sun Color: %.2f, %.2f, %.2f\n" +
                        "Ambient Strength: %.2f\n" +
                        "Active Shaders: %s",
                pos.x, pos.y, pos.z,
                camera.getFov(),
                Math.toDegrees(dayNightCycle),
                fogDensity,
                lightPosition.x, lightPosition.y, lightPosition.z,
                sunColor.x, sunColor.y, sunColor.z,
                ambientStrength,
                shaderManager.getProgramNames()
        );
    }

    @Override
    public void onShaderReloaded(String programName) {
        System.out.println("Shader program reloaded: " + programName);

        ShaderProgram program = shaderManager.getProgram(programName);
        if (program != null) {
            validateProgramUniforms(program);

            if (debugRenderingEnabled) {
                program.printUniforms();
            }
        }

        switch (programName) {
            case "terrain":
                System.out.println("Terrain shader reloaded - terrain rendering updated");
                break;
            case "skybox":
                System.out.println("Skybox shader reloaded - sky rendering updated");
                break;
            case "particle":
                System.out.println("Particle shader reloaded - particle effects updated");
                break;
        }
    }

    // Additional methods for debugging and control
    public void reloadShader(String programName) {
        shaderManager.reloadProgram(programName);
    }

    public void reloadAllShaders() {
        shaderManager.reloadAllPrograms();
    }

    public void setDebugRenderingEnabled(boolean enabled) {
        this.debugRenderingEnabled = enabled;

        if (enabled) {
            System.out.println("=== SHADER DEBUG INFO ===");
            for (String programName : shaderManager.getProgramNames()) {
                ShaderProgram program = shaderManager.getProgram(programName);
                if (program != null) {
                    System.out.println("Program: " + programName);
                    program.printUniforms();
                    System.out.println();
                }
            }
        }
    }

    public void setShaderHotReloadEnabled(boolean enabled) {
        shaderManager.setHotReloadEnabled(enabled);
        System.out.println("Shader hot reload " + (enabled ? "enabled" : "disabled"));
    }

    public ShaderManager getShaderManager() {
        return shaderManager;
    }

    public void cleanup() {
        try {
            shaderManager.removeReloadListener(this);

            atmosphericRenderer.cleanup();
            particleRenderer.cleanup();
            skyRenderer.cleanup();
            postProcessor.cleanup();
            uiRenderer.cleanup();
            objectRenderer.cleanup();

            shaderManager.cleanup();

        } catch (Exception e) {
            System.err.println("Error during enhanced renderer cleanup: " + e.getMessage());
        }
    }

    // Getters and setters
    public boolean isWireframeEnabled() {
        return wireframeEnabled;
    }

    public void setWireframeEnabled(boolean wireframeEnabled) {
        this.wireframeEnabled = wireframeEnabled;
    }

    public boolean isDebugRenderingEnabled() {
        return debugRenderingEnabled;
    }

    public float getFogDensity() {
        return fogDensity;
    }

    public void setFogDensity(float fogDensity) {
        this.fogDensity = Math.max(0.0f, Math.min(1.0f, fogDensity));
    }

    // FIXED: Add methods to control lighting
    public void setAmbientStrength(float ambientStrength) {
        this.ambientStrength = Math.max(0.0f, Math.min(1.0f, ambientStrength));
    }

    public void setSpecularStrength(float specularStrength) {
        this.specularStrength = Math.max(0.0f, Math.min(1.0f, specularStrength));
    }

    public void setSunColor(float r, float g, float b) {
        this.sunColor.set(r, g, b);
    }

    public Vector3f getSunDirection() {
        return new Vector3f(sunDirection);
    }

    public Vector3f getLightPosition() {
        return new Vector3f(lightPosition);
    }
}