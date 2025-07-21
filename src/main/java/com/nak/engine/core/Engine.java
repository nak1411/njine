package com.nak.engine.core;

import com.nak.engine.config.ConfigManager;
import com.nak.engine.config.EngineConfig;
import com.nak.engine.core.lifecycle.Cleanupable;
import com.nak.engine.core.lifecycle.Initializable;
import com.nak.engine.core.lifecycle.Updateable;
import com.nak.engine.events.EventBus;
import com.nak.engine.render.RenderModule;
import com.nak.engine.render.WindowModule;

public class Engine implements Initializable, Updateable, Cleanupable {
    private final ModuleManager moduleManager;
    private final ServiceLocator serviceLocator;
    private final EventBus eventBus;
    private final ResourceManager resourceManager;

    private EngineConfig config;
    private WindowModule windowModule;
    private boolean initialized = false;
    private boolean running = false;

    // Performance tracking
    private long lastFrameTime = 0;
    private float deltaTime = 0;

    public Engine() {
        this.eventBus = new EventBus();
        this.serviceLocator = new ServiceLocator();
        this.resourceManager = new ResourceManager();
        this.moduleManager = new ModuleManager(serviceLocator, eventBus);

        // Register core services
        serviceLocator.register(EventBus.class, eventBus);
        serviceLocator.register(ResourceManager.class, resourceManager);
        serviceLocator.register(Engine.class, this);
    }

    public Engine withModule(Module module) {
        moduleManager.addModule(module);
        return this;
    }

    public Engine configure(EngineConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Engine already initialized");
        }

        try {
            // Load default config if none provided
            if (config == null) {
                config = ConfigManager.loadDefault();
            }

            serviceLocator.register(EngineConfig.class, config);

            // Initialize modules in dependency order
            moduleManager.initialize();

            // Get window module for main loop
            windowModule = moduleManager.getModule(WindowModule.class);
            if (windowModule == null) {
                throw new RuntimeException("WindowModule is required but not found");
            }

            initialized = true;
            System.out.println("Engine initialized successfully");

        } catch (Exception e) {
            cleanup();
            throw new RuntimeException("Failed to initialize engine", e);
        }
    }

    public void run() {
        if (!initialized) {
            throw new IllegalStateException("Engine not initialized");
        }

        running = true;
        lastFrameTime = System.nanoTime();

        // Get modules
        RenderModule renderModule = moduleManager.getModule(RenderModule.class);
        WindowModule windowModule = moduleManager.getModule(WindowModule.class);

        System.out.println("🚀 Starting main engine loop...");

        // Validate readiness before starting
        validateSystemReadiness(renderModule, windowModule);

        int frameCount = 0;
        int readinessCheckCount = 0;

        try {
            while (running && !windowModule.shouldClose()) {
                long currentTime = System.nanoTime();
                deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
                lastFrameTime = currentTime;
                deltaTime = Math.min(deltaTime, 0.25f);

                // Update all modules
                update(deltaTime);

                // CRITICAL: Process events every frame
                eventBus.processEvents();

                // Render frame
                if (renderModule != null && renderModule.isOpenGLReady()) {
                    renderModule.render();
                    frameCount++;

                    // Periodic success confirmation
                    if (frameCount == 1) {
                        System.out.println("✅ First frame rendered successfully!");
                    }
                    if (frameCount % 300 == 0) {
                        System.out.println("✅ " + frameCount + " frames rendered");
                    }
                } else {
                    // Check readiness status periodically
                    readinessCheckCount++;
                    if (readinessCheckCount % 60 == 0) { // Every second at 60fps
                        System.err.println("⏳ Frame " + readinessCheckCount + ": Still waiting for OpenGL readiness");

                        if (renderModule == null) {
                            System.err.println("   ❌ RenderModule is null!");
                        } else {
                            System.err.println("   ❌ RenderModule.isOpenGLReady() = false");

                            // Emergency re-attempt every 5 seconds
                            if (readinessCheckCount % 300 == 0) {
                                System.err.println("🚨 Emergency: Re-attempting OpenGL setup...");
                                try {
                                    renderModule.setupOpenGLResourcesDirectly();
                                } catch (Exception e) {
                                    System.err.println("Emergency setup failed: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                // Swap buffers
                windowModule.swapBuffers();

                // Prevent 100% CPU usage
                Thread.yield();
            }
        } catch (Exception e) {
            System.err.println("❌ Error in main loop: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void validateSystemReadiness(RenderModule renderModule, WindowModule windowModule) {
        System.out.println("🔍 Validating system readiness...");

        if (windowModule == null) {
            throw new RuntimeException("WindowModule not found!");
        }

        if (renderModule == null) {
            throw new RuntimeException("RenderModule not found!");
        }

        System.out.println("🔍 WindowModule context created: " + windowModule.isContextCreated());
        System.out.println("🔍 RenderModule OpenGL ready: " + renderModule.isOpenGLReady());

        if (windowModule.isContextCreated() && !renderModule.isOpenGLReady()) {
            System.err.println("⚠️  DETECTED: OpenGL context exists but RenderModule not ready");
            System.err.println("⚠️  This indicates the OpenGLContextReadyEvent was not properly delivered");

            // Force event processing
            System.out.println("🔄 Force processing events...");
            eventBus.processEvents();

            // Check again
            if (!renderModule.isOpenGLReady()) {
                System.err.println("❌ Event processing didn't help - using direct setup");
                try {
                    renderModule.setupOpenGLResourcesDirectly();
                } catch (Exception e) {
                    System.err.println("Direct setup also failed: " + e.getMessage());
                }
            }
        }

        System.out.println("🔍 Validation complete");
    }

    private void limitFrameRate() {
        if (config.getTargetFps() <= 0) return;

        try {
            long targetFrameTime = 1_000_000_000L / config.getTargetFps();
            long currentTime = System.nanoTime();
            long frameTime = currentTime - lastFrameTime;

            if (frameTime < targetFrameTime) {
                long sleepTime = (targetFrameTime - frameTime) / 1_000_000; // Convert to milliseconds
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Update all modules
            moduleManager.update(deltaTime);

            // Update resource manager
            resourceManager.update(deltaTime);

        } catch (Exception e) {
            System.err.println("Error updating engine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void render() {
        try {
            // Get render module and call its render method
            com.nak.engine.render.RenderModule renderModule =
                    moduleManager.getModule(com.nak.engine.render.RenderModule.class);
            if (renderModule != null) {
                renderModule.render();
            }

            // Get terrain module and call its render method
            com.nak.engine.terrain.TerrainModule terrainModule =
                    moduleManager.getModule(com.nak.engine.terrain.TerrainModule.class);
            if (terrainModule != null) {
                terrainModule.render();
            }

        } catch (Exception e) {
            System.err.println("Error rendering frame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (windowModule != null) {
            windowModule.setShouldClose(true);
        }
    }

    @Override
    public void cleanup() {
        try {
            running = false;

            System.out.println("Cleaning up engine...");

            if (moduleManager != null) {
                moduleManager.cleanup();
            }

            if (resourceManager != null) {
                resourceManager.cleanup();
            }

            if (eventBus != null) {
                eventBus.cleanup();
            }

            System.out.println("Engine cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during engine cleanup: " + e.getMessage());
        }
    }

    // Getters
    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public EngineConfig getConfig() {
        return config;
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isRunning() {
        return running;
    }

    public WindowModule getWindowModule() {
        return windowModule;
    }
}