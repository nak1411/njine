package com.nak.engine.render;

import com.nak.engine.core.Module;
import com.nak.engine.config.RenderSettings;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.ShaderReloadedEvent;
import com.nak.engine.events.events.WindowResizeEvent;

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
        settings = getService(RenderSettings.class);
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
        System.out.println("Render module initialized");
    }

    private void setupOpenGLState() {
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
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Update rendering context
            renderContext.update(deltaTime);

            // Update pipelines
            terrainPipeline.update(deltaTime);
            skyboxPipeline.update(deltaTime);
            particlePipeline.update(deltaTime);
            uiPipeline.update(deltaTime);

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
            skyboxPipeline.render(renderContext);
            terrainPipeline.render(renderContext);
            particlePipeline.render(renderContext);
            uiPipeline.render(renderContext);

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
        org.lwjgl.opengl.GL11.glClear(
                org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT |
                        org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
        );
        renderQueue.clear();
    }

    private void endFrame() {
        org.lwjgl.opengl.GL11.glFlush();
    }

    @EventHandler
    public void onShaderReloaded(ShaderReloadedEvent event) {
        if (event.isSuccess()) {
            // Update pipelines that use the reloaded shader
            switch (event.getShaderName()) {
                case "terrain" -> terrainPipeline.reloadShader();
                case "skybox" -> skyboxPipeline.reloadShader();
                case "particle" -> particlePipeline.reloadShader();
                case "ui" -> uiPipeline.reloadShader();
            }
        }
    }

    @EventHandler
    public void onWindowResize(WindowResizeEvent event) {
        // Update viewport
        org.lwjgl.opengl.GL11.glViewport(0, 0, event.getNewWidth(), event.getNewHeight());

        // Update render context
        renderContext.setViewportSize(event.getNewWidth(), event.getNewHeight());

        // Update pipelines
        terrainPipeline.onResize(event.getNewWidth(), event.getNewHeight());
        skyboxPipeline.onResize(event.getNewWidth(), event.getNewHeight());
        particlePipeline.onResize(event.getNewWidth(), event.getNewHeight());
        uiPipeline.onResize(event.getNewWidth(), event.getNewHeight());
    }

    @Override
    public void cleanup() {
        if (terrainPipeline != null) terrainPipeline.cleanup();
        if (skyboxPipeline != null) skyboxPipeline.cleanup();
        if (particlePipeline != null) particlePipeline.cleanup();
        if (uiPipeline != null) uiPipeline.cleanup();

        initialized = false;
        System.out.println("Render module cleaned up");
    }

    // Getters for external access
    public boolean isWireframeEnabled() {
        return settings.isWireframeMode();
    }

    public void setWireframeEnabled(boolean enabled) {
        settings.setWireframeMode(enabled);
    }

    public RenderContext getRenderContext() {
        return renderContext;
    }
}
