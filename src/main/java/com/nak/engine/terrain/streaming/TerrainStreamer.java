package com.nak.engine.terrain.streaming;

import com.nak.engine.config.TerrainSettings;
import com.nak.engine.terrain.TerrainChunk;
import com.nak.engine.terrain.generation.TerrainGenerator;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TerrainStreamer {
    private final TerrainSettings settings;
    private final TerrainGenerator generator;
    private final Map<String, TerrainChunk> chunks = new ConcurrentHashMap<>();
    private Vector3f cameraPosition = new Vector3f();

    public TerrainStreamer(TerrainSettings settings, TerrainGenerator generator) {
        this.settings = settings;
        this.generator = generator;
    }

    public void update(float deltaTime) {
        // Update terrain streaming based on camera position
    }

    public void updateCameraPosition(Vector3f position) {
        this.cameraPosition.set(position);
    }

    public void invalidateArea(Vector3f center, float radius) {
        // Invalidate chunks in the specified area
    }

    public String getPerformanceInfo() {
        return "Streamer: " + chunks.size() + " chunks";
    }

    public int getActiveChunkCount() {
        return chunks.size();
    }

    public void cleanup() {
        for (TerrainChunk chunk : chunks.values()) {
            chunk.cleanup();
        }
        chunks.clear();
    }
}
