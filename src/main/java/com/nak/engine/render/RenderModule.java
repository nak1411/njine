package com.nak.engine.render;

import com.nak.engine.config.RenderSettings;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.ShaderReloadedEvent;
import com.nak.engine.events.events.WindowResizeEvent;
import com.nak.engine.render.culling.FrustumCuller;
import com.nak.engine.render.pipelines.ParticlePipeline;
import com.nak.engine.render.pipelines.SkyboxPipeline;
import com.nak.engine.render.pipelines.TerrainPipeline;
import com.nak.engine.render.pipelines.UIPipeline;

public class RenderModule extends Module {
    private RenderSettings settings;
    private EventBus eventBus;

    // Rendering components
    private RenderQueue renderQueue;
    private RenderContext renderContext;
    private FrustumCuller frustumCuller;

    // Rendering pipelines
    private TerrainPipeline terrainPipeline;
    private SkyboxPipeline skyboxPipeline;
    private ParticlePipeline particlePipeline;
    private UIPipeline uiPipeline;

    // State
    private boolean initialized = false;

    @Override
    public String getName() {
        return "Render";
    }

    @Override
    public int getInitializationPriority() {
        return 200; // Initialize after core systems
    }

    @Override
    public void initialize() {
        try {
            // Get configuration with fallback to defaults
            settings = getOptionalService(RenderSettings.class);
            if (settings == null) {
                System.out.println("RenderSettings not found, creating default settings");
                settings = createDefaultRenderSettings();
                serviceLocator.register(RenderSettings.class, settings);
            }

            eventBus = getService(EventBus.class);

            // Initialize rendering infrastructure
            renderQueue = new RenderQueue();
            renderContext = new RenderContext();
            frustumCuller = new FrustumCuller();

            // Initialize rendering pipelines
            terrainPipeline = new TerrainPipeline(settings);
            skyboxPipeline = new SkyboxPipeline(settings);
            particlePipeline = new ParticlePipeline(settings);
            uiPipeline = new UIPipeline(settings);

            // Configure OpenGL state
            setupOpenGLState();

            // Register services
            serviceLocator.register(RenderQueue.class, renderQueue);
            serviceLocator.register(RenderContext.class, renderContext);
            serviceLocator.register(FrustumCuller.class, frustumCuller);

            // Register for events
            eventBus.register(this);

            initialized = true;
            System.out.println("Render module initialized with settings: " + getRenderInfo());

        } catch (Exception e) {
            System.err.println("Failed to initialize render module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Render module initialization failed", e);
        }
    }

    private RenderSettings createDefaultRenderSettings() {
        RenderSettings defaultSettings = new RenderSettings();

        try {
            defaultSettings.validate();
            System.out.println("Created and validated default render settings");
        } catch (Exception e) {
            System.err.println("Warning: Default render settings validation failed: " + e.getMessage());
        }

        return defaultSettings;
    }

    private String getRenderInfo() {
        return String.format("ViewDistance=%.0f MaxChunks=%d Fog=%s Wireframe=%s",
                settings.getViewDistance(),
                settings.getMaxVisibleChunks(),
                settings.isEnableFog() ? "ON" : "OFF",
                settings.isWireframeMode() ? "ON" : "OFF");
    }

    private void setupOpenGLState() {
        try {
            // Enable depth testing
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

            // Enable back-face culling
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_BACK);

            // Set clear color
            org.lwjgl.opengl.GL11.glClearColor(0.1f, 0.2f, 0.3f, 1.0f);

            // Configure MSAA if enabled
            if (settings.getMaxVisibleChunks() > 0) { // Using this as a placeholder for MSAA setting
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL13.GL_MULTISAMPLE);
            }

            System.out.println("OpenGL state configured successfully");

        } catch (Exception e) {
            System.err.println("Warning: Failed to configure some OpenGL settings: " + e.getMessage());
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Update rendering context
            renderContext.update(deltaTime);

            // Update pipelines
            if (terrainPipeline != null) {
                terrainPipeline.update(deltaTime);
            }
            if (skyboxPipeline != null) {
                skyboxPipeline.update(deltaTime);
            }
            if (particlePipeline != null) {
                particlePipeline.update(deltaTime);
            }
            if (uiPipeline != null) {
                uiPipeline.update(deltaTime);
            }

        } catch (Exception e) {
            System.err.println("Error updating render module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void render() {
        if (!initialized) return;

        try {
            // Begin frame
            beginFrame();

            // Set wireframe mode
            if (settings.isWireframeMode()) {
                org.lwjgl.opengl.GL11.glPolygonMode(
                        org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK,
                        org.lwjgl.opengl.GL11.GL_LINE
                );
            }

            // Render pipelines in order
            if (skyboxPipeline != null) {
                skyboxPipeline.render(renderContext);
            }
            if (terrainPipeline != null) {
                terrainPipeline.render(renderContext);
            }
            if (particlePipeline != null) {
                particlePipeline.render(renderContext);
            }
            if (uiPipeline != null) {
                uiPipeline.render(renderContext);
            }

            // Reset state
            if (settings.isWireframeMode()) {
                org.lwjgl.opengl.GL11.glPolygonMode(
                        org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK,
                        org.lwjgl.opengl.GL11.GL_FILL
                );
            }

            // End frame
            endFrame();

        } catch (Exception e) {
            System.err.println("Error rendering frame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void beginFrame() {
        try {
            org.lwjgl.opengl.GL11.glClear(
                    org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT |
                            org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
            );
            renderQueue.clear();
        } catch (Exception e) {
            System.err.println("Error beginning frame: " + e.getMessage());
        }
    }

    private void endFrame() {
        try {
            org.lwjgl.opengl.GL11.glFlush();
        } catch (Exception e) {
            System.err.println("Error ending frame: " + e.getMessage());
        }
    }

    @EventHandler
    public void onShaderReloaded(ShaderReloadedEvent event) {
        if (!initialized) return;

        try {
            if (event.isSuccess()) {
                // Update pipelines that use the reloaded shader
                switch (event.getShaderName()) {
                    case "terrain" -> {
                        if (terrainPipeline != null) terrainPipeline.reloadShader();
                    }
                    case "skybox" -> {
                        if (skyboxPipeline != null) skyboxPipeline.reloadShader();
                    }
                    case "particle" -> {
                        if (particlePipeline != null) particlePipeline.reloadShader();
                    }
                    case "ui" -> {
                        if (uiPipeline != null) uiPipeline.reloadShader();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling shader reload: " + e.getMessage());
        }
    }

    @EventHandler
    public void onWindowResize(WindowResizeEvent event) {
        if (!initialized) return;

        try {
            // Update viewport
            org.lwjgl.opengl.GL11.glViewport(0, 0, event.getNewWidth(), event.getNewHeight());

            // Update render context
            renderContext.setViewportSize(event.getNewWidth(), event.getNewHeight());

            // Update pipelines
            if (terrainPipeline != null) {
                terrainPipeline.onResize(event.getNewWidth(), event.getNewHeight());
            }
            if (skyboxPipeline != null) {
                skyboxPipeline.onResize(event.getNewWidth(), event.getNewHeight());
            }
            if (particlePipeline != null) {
                particlePipeline.onResize(event.getNewWidth(), event.getNewHeight());
            }
            if (uiPipeline != null) {
                uiPipeline.onResize(event.getNewWidth(), event.getNewHeight());
            }

        } catch (Exception e) {
            System.err.println("Error handling window resize: " + e.getMessage());
        }
    }

    @Override
    public void cleanup() {
        try {
            initialized = false;

            if (terrainPipeline != null) {
                terrainPipeline.cleanup();
                terrainPipeline = null;
            }
            if (skyboxPipeline != null) {
                skyboxPipeline.cleanup();
                skyboxPipeline = null;
            }
            if (particlePipeline != null) {
                particlePipeline.cleanup();
                particlePipeline = null;
            }
            if (uiPipeline != null) {
                uiPipeline.cleanup();
                uiPipeline = null;
            }

            System.out.println("Render module cleaned up");

        } catch (Exception e) {
            System.err.println("Error during render module cleanup: " + e.getMessage());
        }
    }

    // Getters for external access
    public boolean isWireframeEnabled() {
        return settings != null && settings.isWireframeMode();
    }

    public void setWireframeEnabled(boolean enabled) {
        if (settings != null) {
            settings.setWireframeMode(enabled);
        }
    }

    public RenderContext getRenderContext() {
        return renderContext;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public RenderSettings getSettings() {
        return settings;
    }
}