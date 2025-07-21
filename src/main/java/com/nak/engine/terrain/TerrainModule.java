package com.nak.engine.terrain;

import com.nak.engine.camera.Camera;
import com.nak.engine.config.TerrainSettings;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.CameraMovedEvent;
import com.nak.engine.terrain.generation.NoiseGenerator;
import com.nak.engine.terrain.generation.TerrainGenerator;
import com.nak.engine.terrain.lod.LODManager;
import com.nak.engine.terrain.streaming.TerrainStreamer;
import org.joml.Vector3f;

public class TerrainModule extends Module {
    private TerrainSettings settings;
    private EventBus eventBus;

    // Core terrain components
    private TerrainGenerator generator;
    private TerrainStreamer streamer;
    private LODManager lodManager;
    private TerrainRenderer renderer;
    private TerrainPhysics physics;

    private TerrainManager terrainManager;

    // State
    private Vector3f lastCameraPosition = new Vector3f();
    private boolean initialized = false;

    @Override
    public String getName() {
        return "Terrain";
    }

    @Override
    public int getInitializationPriority() {
        return 100; // Initialize early, but after core systems
    }

    @Override
    public void initialize() {
        try {
            // Get configuration with fallback to defaults
            settings = getOptionalService(TerrainSettings.class);
            if (settings == null) {
                System.out.println("TerrainSettings not found, creating default settings");
                settings = createDefaultTerrainSettings();

                terrainManager = new TerrainManager();
                // Register the default settings for other systems
                serviceLocator.register(TerrainSettings.class, settings);
            }

            eventBus = getService(EventBus.class);

            // Initialize components
            generator = new NoiseGenerator(settings);
            streamer = new TerrainStreamer(settings, generator);
            lodManager = new LODManager(settings);
            renderer = new TerrainRenderer();
            physics = new TerrainPhysics(generator);

            // Register services
            serviceLocator.register(TerrainGenerator.class, generator);
            serviceLocator.register(TerrainStreamer.class, streamer);
            serviceLocator.register(LODManager.class, lodManager);
            serviceLocator.register(TerrainRenderer.class, renderer);
            serviceLocator.register(TerrainPhysics.class, physics);
            serviceLocator.register(TerrainModule.class, this);

            // Register for events
            eventBus.register(this);

            initialized = true;
            System.out.println("Terrain module initialized with settings: " + getTerrainInfo());

        } catch (Exception e) {
            System.err.println("Failed to initialize terrain module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Terrain module initialization failed", e);
        }
    }

    private TerrainSettings createDefaultTerrainSettings() {
        TerrainSettings defaultSettings = new TerrainSettings();

        try {
            // Validate the default settings
            defaultSettings.validate();
            System.out.println("Created and validated default terrain settings");
        } catch (Exception e) {
            System.err.println("Warning: Default terrain settings validation failed: " + e.getMessage());
            // Continue with potentially invalid settings rather than fail completely
        }

        return defaultSettings;
    }

    private String getTerrainInfo() {
        return String.format("WorldSize=%.0f ChunkSize=%.0f MaxLOD=%d Octaves=%d",
                settings.getWorldSize(),
                settings.getBaseChunkSize(),
                settings.getMaxLODLevel(),
                settings.getOctaves());
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Get current camera position
            Camera camera = serviceLocator.getOptional(Camera.class);
            if (camera == null) {
                System.err.println("❌ No camera for terrain updates");
                return;
            }

            Vector3f cameraPos = camera.getPosition();

            // CRITICAL: Update TerrainManager with camera position
            if (terrainManager != null) {
                terrainManager.update(cameraPos, deltaTime);
            }

            // Update other components
            if (streamer != null) {
                streamer.updateCameraPosition(cameraPos);
                streamer.update(deltaTime);
            }

            if (lodManager != null) {
                lodManager.updateCameraPosition(cameraPos);
                lodManager.update(deltaTime);
            }

            if (renderer != null) {
                renderer.update(deltaTime);
            }

            lastCameraPosition.set(cameraPos);

        } catch (Exception e) {
            System.err.println("❌ Error updating terrain module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = 5)
    public void onCameraMoved(CameraMovedEvent event) {
        try {
            Vector3f newPos = event.getNewPosition();
            float distance = lastCameraPosition.distance(newPos);

            // Only update if camera moved significantly
            if (distance > settings.getUpdateThreshold()) {
                if (streamer != null) {
                    streamer.updateCameraPosition(newPos);
                }
                if (lodManager != null) {
                    lodManager.updateCameraPosition(newPos);
                }
                lastCameraPosition.set(newPos);
            }
        } catch (Exception e) {
            System.err.println("Error handling camera moved event: " + e.getMessage());
        }
    }

    public void render() {
        if (!initialized) {
            System.err.println("❌ TerrainModule.render() called but not initialized");
            return;
        }

        try {
            // Use TerrainManager for rendering instead of basic renderer
            if (terrainManager != null) {
                terrainManager.render();
                System.out.println("🌍 TerrainModule.render() called TerrainManager");
            } else {
                System.err.println("❌ No TerrainManager available for rendering");
            }

        } catch (Exception e) {
            System.err.println("❌ Error rendering terrain: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ADD: Getter for external access
    public TerrainManager getTerrainManager() {
        return terrainManager;
    }


    public float getHeightAt(float x, float z) {
        if (physics != null) {
            try {
                return physics.getHeightAt(x, z);
            } catch (Exception e) {
                System.err.println("Error getting height at " + x + ", " + z + ": " + e.getMessage());
                return 0.0f;
            }
        }
        return 0.0f;
    }

    public void invalidateArea(Vector3f center, float radius) {
        if (streamer != null) {
            try {
                streamer.invalidateArea(center, radius);
            } catch (Exception e) {
                System.err.println("Error invalidating terrain area: " + e.getMessage());
            }
        }
    }

    @Override
    public void cleanup() {
        try {
            initialized = false;

            if (renderer != null) {
                renderer.cleanup();
                renderer = null;
            }
            if (streamer != null) {
                streamer.cleanup();
                streamer = null;
            }
            if (generator != null) {
                generator.cleanup();
                generator = null;
            }

            System.out.println("Terrain module cleaned up");

        } catch (Exception e) {
            System.err.println("Error during terrain module cleanup: " + e.getMessage());
        }
    }

    // Getters for debugging/monitoring
    public String getPerformanceInfo() {
        if (!initialized) return "Terrain: Not initialized";

        try {
            if (streamer != null) {
                return streamer.getPerformanceInfo();
            }
            return "Terrain: Initialized but no streamer";
        } catch (Exception e) {
            return "Terrain: Error getting performance info";
        }
    }

    public int getActiveChunkCount() {
        if (streamer != null) {
            try {
                return streamer.getActiveChunkCount();
            } catch (Exception e) {
                System.err.println("Error getting active chunk count: " + e.getMessage());
            }
        }
        return 0;
    }

    public int getVisibleChunkCount() {
        if (renderer != null) {
            try {
                return renderer.getVisibleChunkCount();
            } catch (Exception e) {
                System.err.println("Error getting visible chunk count: " + e.getMessage());
            }
        }
        return 0;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public TerrainSettings getSettings() {
        return settings;
    }
}