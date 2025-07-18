// Updated MasterRenderer.java - Integration with ShaderManager
package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.shader.ShaderManager;
import com.nak.engine.shader.ShaderProgram;
import com.nak.engine.shader.ShaderReloadListener;
import com.nak.engine.shader.ShaderUtils;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer implements ShaderReloadListener {

    // Shader manager
    private final ShaderManager shaderManager;

    // Rendering components (existing)
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

    // Lighting system
    private final Vector3f sunDirection = new Vector3f();
    private final Vector3f sunColor = new Vector3f();
    private final Vector3f ambientColor = new Vector3f();
    private float dayNightCycle = 0.0f;

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
        // Set shader directory (optional - for development)
        shaderManager.setShaderDirectory("src/main/resources/shaders");

        // Enable hot reload in debug mode
        boolean isDevelopment = Boolean.parseBoolean(System.getProperty("development", "false"));
        shaderManager.setHotReloadEnabled(isDevelopment);

        // Load or create shader programs
        loadShaderPrograms();

        System.out.println("Initialized shader system with programs: " + shaderManager.getProgramNames());
    }

    private void loadShaderPrograms() {
        try {
            // Try to load from files first, fall back to built-in
            tryLoadShaderFromFiles();
        } catch (Exception e) {
            System.out.println("Loading shaders from files failed, using built-in shaders");
            // Built-in shaders are automatically loaded by ShaderManager
        }

        // Validate that we have all required shaders
        validateShaders();
    }

    private void tryLoadShaderFromFiles() {
        // Load terrain shader from files if available
        try {
            shaderManager.loadProgram("terrain", "terrain.vert", "terrain.frag");
        } catch (Exception e) {
            System.out.println("Could not load terrain shader from files: " + e.getMessage());
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
        // Ensure we have all required shader programs
        String[] requiredPrograms = {"terrain", "basic", "skybox", "ui"};

        for (String programName : requiredPrograms) {
            ShaderProgram program = shaderManager.getProgram(programName);
            if (program == null) {
                System.err.println("Missing required shader program: " + programName);
            } else {
                // Validate required uniforms for each program
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
     * Main render method with enhanced shader management
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

        // Render terrain with enhanced shaders
        renderTerrainWithShaders(camera);

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

    private void renderTerrainWithShaders(Camera camera) {
        ShaderProgram terrainProgram = shaderManager.getProgram("terrain");
        if (terrainProgram != null) {
            terrainProgram.bind();

            // Set camera uniforms
            ShaderUtils.setCameraUniforms(terrainProgram, viewMatrix, projectionMatrix, camera.getPosition());

            // Set model matrix
            terrainProgram.setUniform("modelMatrix", modelMatrix);

            // Set lighting uniforms
            ShaderUtils.setLightingUniforms(terrainProgram,
                    new Vector3f(sunDirection).mul(1000), sunColor, sunDirection,
                    0.3f, 0.5f, 32.0f);

            // Set fog uniforms
            ShaderUtils.setFogUniforms(terrainProgram, fogColor, fogDensity);

            // Set time uniform for animations
            ShaderUtils.setTimeUniform(terrainProgram, dayNightCycle);

            // Render terrain
            terrainManager.render();

            terrainProgram.unbind();
        } else {
            // Fallback to legacy rendering
            terrainManager.render();
        }
    }

    private void renderObjectsWithShaders(GameState gameState) {
        ShaderProgram basicProgram = shaderManager.getProgram("basic");
        if (basicProgram != null) {
            basicProgram.bind();

            // Set MVP matrix for basic rendering
            Matrix4f mvpMatrix = new Matrix4f(projectionMatrix).mul(viewMatrix).mul(modelMatrix);
            basicProgram.setUniform("mvpMatrix", mvpMatrix);

            // Render objects here
            // objectRenderer.render();

            basicProgram.unbind();
        }
    }

    private void renderParticlesWithShaders(Camera camera, float time) {
        // Particles might use a specialized shader or the basic one
        ShaderProgram particleProgram = shaderManager.getProgram("particle");
        if (particleProgram == null) {
            particleProgram = shaderManager.getProgram("basic");
        }

        if (particleProgram != null) {
            particleProgram.bind();

            glDisable(GL_LIGHTING);
            glEnable(GL_BLEND);
            glDepthMask(false);

            // Set uniforms for particles
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

        // Update lighting colors based on time of day
        calculateLightingColors(sunHeight);
        updateFogSettings(sunHeight);
    }

    private void calculateLightingColors(float sunHeight) {
        if (sunHeight > 0) {
            float intensity = Math.min(1.0f, sunHeight * 2.0f);
            sunColor.set(1.0f, 0.95f + intensity * 0.05f, 0.8f + intensity * 0.2f);
            ambientColor.set(0.3f + intensity * 0.3f, 0.35f + intensity * 0.35f, 0.5f + intensity * 0.3f);
        } else {
            float moonlight = Math.max(0, -sunHeight * 0.5f);
            sunColor.set(0.4f + moonlight * 0.2f, 0.4f + moonlight * 0.2f, 0.6f + moonlight * 0.3f);
            ambientColor.set(0.05f + moonlight * 0.1f, 0.05f + moonlight * 0.1f, 0.15f + moonlight * 0.2f);
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
                        "Active Shaders: %s",
                pos.x, pos.y, pos.z,
                camera.getFov(),
                Math.toDegrees(dayNightCycle),
                fogDensity,
                shaderManager.getProgramNames()
        );
    }

    /**
     * Handle shader reload events
     */
    @Override
    public void onShaderReloaded(String programName) {
        System.out.println("Shader program reloaded: " + programName);

        // Re-validate uniforms after reload
        ShaderProgram program = shaderManager.getProgram(programName);
        if (program != null) {
            validateProgramUniforms(program);

            // Print available uniforms for debugging
            if (debugRenderingEnabled) {
                program.printUniforms();
            }
        }

        // Perform any specific actions based on which shader was reloaded
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

    /**
     * Reload specific shader program
     */
    public void reloadShader(String programName) {
        shaderManager.reloadProgram(programName);
    }

    /**
     * Reload all shaders
     */
    public void reloadAllShaders() {
        shaderManager.reloadAllPrograms();
    }

    /**
     * Enable/disable debug features
     */
    public void setDebugRenderingEnabled(boolean enabled) {
        this.debugRenderingEnabled = enabled;

        if (enabled) {
            // Print current shader information
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

    /**
     * Enable/disable shader hot reloading
     */
    public void setShaderHotReloadEnabled(boolean enabled) {
        shaderManager.setHotReloadEnabled(enabled);
        System.out.println("Shader hot reload " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Get shader manager for external access
     */
    public ShaderManager getShaderManager() {
        return shaderManager;
    }

    /**
     * Cleanup all resources including shaders
     */
    public void cleanup() {
        try {
            // Remove shader reload listener
            shaderManager.removeReloadListener(this);

            // Cleanup rendering components
            atmosphericRenderer.cleanup();
            particleRenderer.cleanup();
            skyRenderer.cleanup();
            postProcessor.cleanup();
            uiRenderer.cleanup();
            objectRenderer.cleanup();

            // Cleanup shader manager (this will cleanup all shader programs)
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
}

// Example usage in main application
class ShaderManagerExample {

    public static void main(String[] args) {
        // Enable development mode for hot reloading
        System.setProperty("development", "true");

        // Your existing initialization code...

        // Get shader manager instance
        ShaderManager shaderManager = ShaderManager.getInstance();

        // Set custom shader directory
        shaderManager.setShaderDirectory("assets/shaders");

        // Enable hot reloading for development
        shaderManager.setHotReloadEnabled(true);

        // Create custom shaders
        createCustomShaders(shaderManager);

        // Add reload listener for custom handling
        shaderManager.addReloadListener(programName -> {
            System.out.println("Custom handler: Shader " + programName + " was reloaded!");
        });
    }

    private static void createCustomShaders(ShaderManager shaderManager) {
        // Create a custom water shader
        String waterVertexShader = """
                #version 330 core
                #include "common.glsl"
                
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform float time;
                
                out vec2 texCoords;
                out vec3 worldPos;
                out float waveHeight;
                
                void main() {
                    vec4 worldPosition = modelMatrix * vec4(position, 1.0);
                
                    // Add wave animation
                    float wave1 = sin(worldPosition.x * 0.02 + time * 2.0) * 0.5;
                    float wave2 = cos(worldPosition.z * 0.015 + time * 1.5) * 0.3;
                    waveHeight = wave1 + wave2;
                    worldPosition.y += waveHeight;
                
                    worldPos = worldPosition.xyz;
                    texCoords = texCoord;
                
                    gl_Position = projectionMatrix * viewMatrix * worldPosition;
                }
                """;

        String waterFragmentShader = """
                #version 330 core
                #include "common.glsl"
                #include "lighting.glsl"
                
                in vec2 texCoords;
                in vec3 worldPos;
                in float waveHeight;
                
                uniform sampler2D waterTexture;
                uniform sampler2D normalMap;
                uniform float time;
                uniform vec3 viewPosition;
                
                out vec4 fragColor;
                
                void main() {
                    // Animated texture coordinates
                    vec2 animatedTexCoords = texCoords + vec2(time * 0.05, time * 0.03);
                
                    // Sample textures
                    vec4 waterColor = texture(waterTexture, animatedTexCoords);
                    vec3 normal = texture(normalMap, animatedTexCoords * 2.0).rgb * 2.0 - 1.0;
                
                    // Calculate lighting
                    vec3 lighting = calculateLighting(worldPos, normal);
                
                    // Add reflection and refraction effects
                    vec3 viewDir = normalize(viewPosition - worldPos);
                    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.0);
                
                    vec3 finalColor = waterColor.rgb * lighting;
                    finalColor = mix(finalColor, vec3(0.2, 0.6, 1.0), fresnel * 0.3);
                
                    // Apply fog
                    float fogFactor = calculateFog(worldPos);
                    finalColor = applyFog(finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 0.8 + waveHeight * 0.1);
                }
                """;

        // Create the water shader program
        shaderManager.createProgram("water", waterVertexShader, waterFragmentShader);

        System.out.println("Created custom water shader");
    }
}