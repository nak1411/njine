package com.nak.engine.core;

import com.nak.engine.config.ConfigManager;
import com.nak.engine.config.EngineConfig;
import com.nak.engine.core.lifecycle.Cleanupable;
import com.nak.engine.core.lifecycle.Initializable;
import com.nak.engine.core.lifecycle.Updateable;
import com.nak.engine.events.EventBus;

public class Engine implements Initializable, Updateable, Cleanupable {
    private final ModuleManager moduleManager;
    private final ServiceLocator serviceLocator;
    private final EventBus eventBus;
    private final ResourceManager resourceManager;

    private EngineConfig config;
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

        try {
            while (running) {
                long currentTime = System.nanoTime();
                deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
                lastFrameTime = currentTime;

                // Cap delta time to prevent spiral of death
                deltaTime = Math.min(deltaTime, 0.25f);

                update(deltaTime);

                // Process events
                eventBus.processEvents();

                // Simple frame rate limiting
                if (config.getTargetFPS() > 0) {
                    long frameTime = System.nanoTime() - currentTime;
                    long targetFrameTime = 1_000_000_000L / config.getTargetFPS();

                    if (frameTime < targetFrameTime) {
                        try {
                            Thread.sleep((targetFrameTime - frameTime) / 1_000_000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in main loop: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!running) return;

        try {
            moduleManager.update(deltaTime);
        } catch (Exception e) {
            System.err.println("Error updating modules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void cleanup() {
        try {
            running = false;

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
}
