package com.nak.engine.terrain.lod;

import com.nak.engine.config.TerrainSettings;
import org.joml.Vector3f;

public class LODManager {
    private final TerrainSettings settings;
    private Vector3f cameraPosition = new Vector3f();

    public LODManager(TerrainSettings settings) {
        this.settings = settings;
    }

    public void update(float deltaTime) {
        // Update LOD calculations
    }

    public void updateCameraPosition(Vector3f position) {
        this.cameraPosition.set(position);
    }

    public int calculateLOD(Vector3f chunkPosition, float chunkSize) {
        Vector3f chunkCenter = new Vector3f(chunkPosition).add(chunkSize / 2, 0, chunkSize / 2);
        float distance = cameraPosition.distance(chunkCenter);

        // Calculate LOD based on distance
        if (distance < 64) return 0;      // Highest detail
        if (distance < 128) return 1;     // High detail
        if (distance < 256) return 2;     // Medium detail
        if (distance < 512) return 3;     // Low detail
        return settings.getMaxLODLevel(); // Lowest detail
    }
}
