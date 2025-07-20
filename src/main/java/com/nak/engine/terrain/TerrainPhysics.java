package com.nak.engine.terrain;

import com.nak.engine.terrain.generation.NoiseGenerator;
import com.nak.engine.terrain.generation.TerrainGenerator;

public class TerrainPhysics {
    private final TerrainGenerator generator;

    public TerrainPhysics(TerrainGenerator generator) {
        this.generator = generator;
    }

    public float getHeightAt(float x, float z) {
        if (generator instanceof NoiseGenerator) {
            return ((NoiseGenerator) generator).generateHeight(x, z);
        }
        return 0.0f;
    }
}
