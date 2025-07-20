package com.nak.engine.events.events;

import com.nak.engine.events.Event;
import org.joml.Vector3f;

public class TerrainLoadedEvent extends Event {
    private final Vector3f chunkPosition;
    private final float chunkSize;
    private final int lodLevel;
    private final boolean success;
    private final String errorMessage;

    public TerrainLoadedEvent(Vector3f chunkPosition, float chunkSize, int lodLevel, boolean success, String errorMessage) {
        this.chunkPosition = new Vector3f(chunkPosition);
        this.chunkSize = chunkSize;
        this.lodLevel = lodLevel;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getEventName() {
        return "TerrainChunkLoaded";
    }

    public Vector3f getChunkPosition() {
        return new Vector3f(chunkPosition);
    }

    public float getChunkSize() {
        return chunkSize;
    }

    public int getLodLevel() {
        return lodLevel;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}