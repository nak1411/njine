package com.nak.engine.render;

import com.nak.engine.camera.Camera;
import com.nak.engine.config.RenderSettings;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.OpenGLContextReadyEvent;
import com.nak.engine.events.events.ShaderReloadedEvent;
import com.nak.engine.events.events.WindowResizeEvent;
import com.nak.engine.render.culling.FrustumCuller;
import com.nak.engine.render.pipelines.ParticlePipeline;
import com.nak.engine.render.pipelines.SkyboxPipeline;
import com.nak.engine.render.pipelines.TerrainPipeline;
import com.nak.engine.render.pipelines.UIPipeline;
import com.nak.engine.terrain.TerrainManager;
import com.nak.engine.terrain.TerrainModule;

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
    private boolean openGLReady = false;

    // Terrain
    private TerrainModule terrainModule;
    private TerrainManager terrainManager;

    @Override
    public String getName() {
        return "Render";
    }

    @Override
    public int getInitializationPriority() {
        return 200; // Initialize after WindowModule (priority 25)
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
            System.out.println("🔧 EventBus obtained: " + (eventBus != null));

            eventBus.register(this);
            System.out.println("RenderModule registered for events");

            // Initialize non-OpenGL components first
            renderQueue = new RenderQueue();
            renderContext = new RenderContext();
            frustumCuller = new FrustumCuller();

            // Register services early for other modules
            serviceLocator.register(RenderQueue.class, renderQueue);
            serviceLocator.register(RenderContext.class, renderContext);
            serviceLocator.register(FrustumCuller.class, frustumCuller);
            serviceLocator.register(RenderModule.class, this);

            System.out.println("🔧 Registering RenderModule for events...");
            eventBus.register(this);
            System.out.println("🔧 RenderModule registered successfully");

            initialized = true;
            System.out.println("Render module initialized (waiting for OpenGL context)");

            checkForExistingOpenGLContext();

        } catch (Exception e) {
            System.err.println("Failed to initialize render module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Render module initialization failed", e);
        }
    }

    // ADD: Method to handle case where OpenGL context already exists
    private void checkForExistingOpenGLContext() {
        try {
            // Try to get WindowModule and check if context exists
            WindowModule windowModule = serviceLocator.getOptional(WindowModule.class);
            if (windowModule != null && windowModule.isContextCreated()) {
                System.out.println("🔧 OpenGL context already exists - setting up resources directly");

                // Context exists but event might have been missed - set up directly
                setupOpenGLResourcesDirectly();
            } else {
                System.out.println("🔧 No existing OpenGL context found - waiting for event");
            }
        } catch (Exception e) {
            System.out.println("🔧 Could not check existing context: " + e.getMessage());
            // This is fine - we'll wait for the event
        }
    }



    public void setupOpenGLResourcesDirectly() {
        if (openGLReady) {
            System.out.println("✅ OpenGL resources already initialized");
            return;
        }

        try {
            System.out.println("🔧 Setting up OpenGL resources directly...");

            // Verify OpenGL context is available
            String glVersion = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
            if (glVersion == null) {
                System.err.println("❌ No OpenGL context available for direct setup");
                return;
            }

            System.out.println("🔧 OpenGL Version: " + glVersion);

            // Setup OpenGL state
            setupOpenGLState();

            // Initialize rendering pipelines
            terrainPipeline = new TerrainPipeline(settings);
            skyboxPipeline = new SkyboxPipeline(settings);
            particlePipeline = new ParticlePipeline(settings);
            uiPipeline = new UIPipeline(settings);

            openGLReady = true;
            System.out.println("✅ OpenGL resources initialized directly");

        } catch (Exception e) {
            System.err.println("❌ Failed to setup OpenGL resources directly: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Event handler for OpenGL context ready - this is the clean way to handle initialization timing
     */
    @EventHandler(priority = 100) // High priority to initialize early
    public void onOpenGLContextReady(OpenGLContextReadyEvent event) {
        if (openGLReady) {
            System.out.println("OpenGL resources already initialized, ignoring duplicate event");
            return;
        }

        try {
            System.out.println("Received OpenGL context ready event, initializing render resources...");
            System.out.println("OpenGL Info - Version: " + event.getGlVersion() +
                    ", Renderer: " + event.getGlRenderer());

            // Now it's safe to make OpenGL calls
            setupOpenGLState();

            // Initialize rendering pipelines (these need OpenGL context)
            terrainPipeline = new TerrainPipeline(settings);
            skyboxPipeline = new SkyboxPipeline(settings);
            particlePipeline = new ParticlePipeline(settings);
            uiPipeline = new UIPipeline(settings);

            // FIX: Try multiple approaches to get TerrainModule
            terrainModule = acquireTerrainModule();

            openGLReady = true;
            System.out.println("✓ Render module OpenGL resources initialized successfully");

        } catch (Exception e) {
            System.err.println("✗ Failed to initialize OpenGL resources: " + e.getMessage());
            e.printStackTrace();
            // Don't throw here - let the application continue, but log the failure
        }
    }

    private RenderSettings createDefaultRenderSettings() {
        RenderSettings defaultSettings = new RenderSettings();
        try {
            defaultSettings.validate();
            System.out.println("Created default render settings");
        } catch (Exception e) {
            System.err.println("Warning: Default render settings validation failed: " + e.getMessage());
        }
        return defaultSettings;
    }

    private TerrainModule acquireTerrainModule() {
        // Strategy 1: Direct lookup from ServiceLocator
        TerrainModule module = serviceLocator.getOptional(TerrainModule.class);
        if (module != null) {
            System.out.println("🎯 Connected to TerrainModule via direct lookup");
            return module;
        }

        // Strategy 2: Check if service exists but retrieval failed
        if (serviceLocator.hasService(TerrainModule.class)) {
            try {
                module = serviceLocator.get(TerrainModule.class);
                System.out.println("🎯 Connected to TerrainModule via forced lookup");
                return module;
            } catch (Exception e) {
                System.err.println("⚠️ TerrainModule exists in ServiceLocator but retrieval failed: " + e.getMessage());
            }
        }

        // Strategy 3: Debug ServiceLocator state
        System.err.println("🔍 ServiceLocator Debug Info:");
        System.err.println("  - Has TerrainModule service: " + serviceLocator.hasService(TerrainModule.class));

        // Strategy 4: Try to get via ModuleManager if we have access to it
        try {
            // Get engine instance through service locator (if registered)
            if (serviceLocator.hasService("ModuleManager")) {
                Object moduleManagerObj = serviceLocator.get("ModuleManager", Object.class);
                if (moduleManagerObj instanceof com.nak.engine.core.ModuleManager) {
                    com.nak.engine.core.ModuleManager mgr = (com.nak.engine.core.ModuleManager) moduleManagerObj;
                    module = mgr.getModule(TerrainModule.class);
                    if (module != null) {
                        System.out.println("🎯 Connected to TerrainModule via ModuleManager");
                        // Also register it in ServiceLocator for future use (safe registration)
                        safeRegisterTerrainModule(module);
                        return module;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to get TerrainModule via ModuleManager: " + e.getMessage());
        }

        System.err.println("❌ TerrainModule not found via any strategy - will retry during render");
        return null;
    }

    private void setupOpenGLState() {
        try {
            // Verify OpenGL context is active
            String glVersion = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
            if (glVersion == null) {
                throw new RuntimeException("No active OpenGL context found");
            }

            System.out.println("Configuring OpenGL state for version: " + glVersion);

            // Enable depth testing
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

            // Enable back-face culling
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_BACK);


            // Set clear color
            org.lwjgl.opengl.GL11.glClearColor(0.1f, 0.2f, 0.3f, 1.0f);

            // Configure blending for transparency
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(
                    org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
            );

            // Configure MSAA if available and enabled
            if (settings.getMaxVisibleChunks() > 0) { // Using this as MSAA placeholder
                try {
                    org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL13.GL_MULTISAMPLE);
                    System.out.println("MSAA enabled");
                } catch (Exception e) {
                    System.out.println("MSAA not available or failed to enable: " + e.getMessage());
                }
            }

            // Configure wireframe mode if enabled
            if (settings.isWireframeMode()) {
                org.lwjgl.opengl.GL11.glPolygonMode(
                        org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK,
                        org.lwjgl.opengl.GL11.GL_LINE
                );
                System.out.println("Wireframe mode enabled");
            }

            // Log render settings
            logRenderSettings();

            System.out.println("OpenGL state configured successfully");

        } catch (Exception e) {
            System.err.println("Error configuring OpenGL state: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void logRenderSettings() {
        System.out.printf("Render Settings - MaxChunks: %d, VSync: %s, Wireframe: %s%n",
                settings.getMaxVisibleChunks(),
                settings.isVsync() ? "ON" : "OFF",  // Fixed method name
                settings.isWireframeMode() ? "ON" : "OFF");
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized || !openGLReady) return;

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
        if (!openGLReady) {
            System.err.println("⚠️  Render called but OpenGL not ready");
            return;
        }

        try {
            // Get camera for rendering
            Camera camera = serviceLocator.getOptional(Camera.class);
            if (camera == null) {
                System.err.println("❌ No camera available for rendering");
                return;
            }

            // Clear screen
            org.lwjgl.opengl.GL11.glClear(
                    org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT |
                            org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
            );

            // Render terrain
            renderTerrain(camera);

            // Check for OpenGL errors
            int error = org.lwjgl.opengl.GL11.glGetError();
            if (error != org.lwjgl.opengl.GL11.GL_NO_ERROR) {
                System.err.println("OpenGL error during rendering: " + error);
            }

        } catch (Exception e) {
            System.err.println("Error in RenderModule.render(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ADD: Terrain rendering method
    private void renderTerrain(Camera camera) {
        // Lazy loading: If terrainModule is null, try to acquire it again
        if (terrainModule == null) {
            terrainModule = acquireTerrainModule();

            // If still null, try one more time with a brief wait for async initialization
            if (terrainModule == null) {
                System.err.println("❌ TerrainModule not available for rendering - skipping terrain render");
                return;
            }
        }

        try {
            // Let terrain module handle its own rendering
            terrainModule.render();

        } catch (Exception e) {
            System.err.println("Error rendering terrain: " + e.getMessage());
            e.printStackTrace();

            // Reset terrainModule to null so we retry acquisition next frame
            terrainModule = null;
        }
    }

    public void refreshTerrainConnection() {
        terrainModule = acquireTerrainModule();
        if (terrainModule != null) {
            System.out.println("✓ TerrainModule connection refreshed successfully");
        }
    }

    private void beginFrame() {
        try {
            // Clear buffers
            org.lwjgl.opengl.GL11.glClear(
                    org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT |
                            org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
            );

            // Clear render queue for new frame
            renderQueue.clear();

        } catch (Exception e) {
            System.err.println("Error beginning frame: " + e.getMessage());
            throw e;
        }
    }

    private void renderScene() {
        try {
            // Render pipelines in order (back to front for transparency)
            if (skyboxPipeline != null) {
                skyboxPipeline.render(renderContext);
            }

            if (terrainPipeline != null) {
                terrainPipeline.render(renderContext);
            }

            if (particlePipeline != null) {
                particlePipeline.render(renderContext);
            }

            // UI should be rendered last (on top)
            if (uiPipeline != null) {
                uiPipeline.render(renderContext);
            }

        } catch (Exception e) {
            System.err.println("Error rendering scene: " + e.getMessage());
            throw e;
        }
    }

    private void endFrame() {
        try {
            org.lwjgl.opengl.GL11.glFlush();
        } catch (Exception e) {
            System.err.println("Error ending frame: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void cleanup() {
        try {
            if (eventBus != null) {
                eventBus.unregister(this);
            }

            // Cleanup pipelines
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

            // Cleanup other components (these don't have cleanup methods based on your codebase)
            renderQueue = null;
            renderContext = null;
            frustumCuller = null;

            openGLReady = false;
            initialized = false;

            System.out.println("Render module cleaned up");

        } catch (Exception e) {
            System.err.println("Error cleaning up render module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Event handlers
    @EventHandler
    public void onShaderReloaded(ShaderReloadedEvent event) {
        if (!initialized || !openGLReady) return;

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
                System.out.println("Reloaded shader: " + event.getShaderName());
            }
        } catch (Exception e) {
            System.err.println("Error handling shader reload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onWindowResize(WindowResizeEvent event) {
        if (!initialized || !openGLReady) return;

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

            System.out.println("Render module adapted to window resize: " +
                    event.getNewWidth() + "x" + event.getNewHeight());

        } catch (Exception e) {
            System.err.println("Error handling window resize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void safeRegisterTerrainModule(TerrainModule module) {
        try {
            serviceLocator.register(TerrainModule.class, module);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to register TerrainModule in ServiceLocator: " + e.getMessage());
            // Fall back to name-based registration
            serviceLocator.register("TerrainModule", module);
        }
    }

    // Getters for access to rendering components
    public RenderQueue getRenderQueue() {
        return renderQueue;
    }

    public boolean isTerrainAvailable() {
        if (terrainModule == null) {
            terrainModule = acquireTerrainModule();
        }
        return terrainModule != null;
    }

    public RenderContext getRenderContext() {
        return renderContext;
    }

    public FrustumCuller getFrustumCuller() {
        return frustumCuller;
    }

    public boolean isOpenGLReady() {
        return openGLReady;
    }

    // Pipeline getters for advanced use cases
    public TerrainPipeline getTerrainPipeline() {
        return terrainPipeline;
    }

    public SkyboxPipeline getSkyboxPipeline() {
        return skyboxPipeline;
    }

    public ParticlePipeline getParticlePipeline() {
        return particlePipeline;
    }

    public UIPipeline getUIPipeline() {
        return uiPipeline;
    }
}