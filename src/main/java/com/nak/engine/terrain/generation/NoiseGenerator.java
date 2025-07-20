package com.nak.engine.terrain.generation;

import com.nak.engine.config.TerrainSettings;
import com.nak.engine.terrain.generation.TerrainGenerator;

public class NoiseGenerator implements TerrainGenerator {
    private final TerrainSettings settings;

    public NoiseGenerator(TerrainSettings settings) {
        this.settings = settings;
    }

    public float generateHeight(float x, float z) {
        float height = 0;
        float amplitude = settings.getBaseAmplitude();
        float frequency = settings.getBaseFrequency();

        for (int i = 0; i < settings.getOctaves(); i++) {
            height += amplitude * noise(x * frequency, z * frequency);
            amplitude *= settings.getPersistence();
            frequency *= settings.getLacunarity();
        }

        return height;
    }

    private float noise(float x, float z) {
        // Simple noise implementation
        x *= 0.01f;
        z *= 0.01f;

        return (float) (
                Math.sin(x * 0.754f + Math.cos(z * 0.234f)) *
                        Math.cos(z * 0.832f + Math.sin(x * 0.126f)) * 0.5f +
                        Math.sin(x * 1.347f + Math.cos(z * 1.726f)) *
                                Math.cos(z * 1.194f + Math.sin(x * 0.937f)) * 0.3f
        );
    }

    public void cleanup() {
        // No cleanup needed for noise generator
    }
}
