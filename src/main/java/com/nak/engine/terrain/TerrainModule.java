package com.nak.engine.terrain;

import com.nak.engine.config.TerrainSettings;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.annotations.EventHandler;
import com.nak.engine.events.events.CameraMovedEvent;
import com.nak.engine.terrain.generation.TerrainGenerator;
import com.nak.engine.terrain.lod.LODManager;
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
        // Get configuration
        settings = getService(TerrainSettings.class);
        eventBus = getService(EventBus.class);

        // Initialize components
        generator = new NoiseTerrainGenerator(settings);
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

        // Register for events
        eventBus.register(this);

        initialized = true;
        System.out.println("Terrain module initialized");
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Update components
            streamer.update(deltaTime);
            lodManager.update(deltaTime);
            renderer.update(deltaTime);

        } catch (Exception e) {
            System.err.println("Error updating terrain module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = 5)
    public void onCameraMoved(CameraMovedEvent event) {
        Vector3f newPos = event.getNewPosition();
        float distance = lastCameraPosition.distance(newPos);

        // Only update if camera moved significantly
        if (distance > settings.getUpdateThreshold()) {
            streamer.updateCameraPosition(newPos);
            lodManager.updateCameraPosition(newPos);
            lastCameraPosition.set(newPos);
        }
    }

    public void render() {
        if (initialized && renderer != null) {
            renderer.render();
        }
    }

    public float getHeightAt(float x, float z) {
        return physics != null ? physics.getHeightAt(x, z) : 0.0f;
    }

    public void invalidateArea(Vector3f center, float radius) {
        if (streamer != null) {
            streamer.invalidateArea(center, radius);
        }
    }

    @Override
    public void cleanup() {
        if (renderer != null) renderer.cleanup();
        if (streamer != null) streamer.cleanup();
        if (generator != null) generator.cleanup();

        initialized = false;
        System.out.println("Terrain module cleaned up");
    }

    // Getters for debugging/monitoring
    public String getPerformanceInfo() {
        if (streamer == null) return "Terrain: Not initialized";
        return streamer.getPerformanceInfo();
    }

    public int getActiveChunkCount() {
        return streamer != null ? streamer.getActiveChunkCount() : 0;
    }

    public int getVisibleChunkCount() {
        return renderer != null ? renderer.getVisibleChunkCount() : 0;
    }
}
