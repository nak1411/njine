package com.nak.engine.config;

public class TerrainSettings implements Validatable {
    // World generation
    private float worldSize = 2048.0f;
    private float baseChunkSize = 64.0f;
    private int maxLODLevel = 4;
    private long seed = System.currentTimeMillis();

    // Noise parameters
    private float baseAmplitude = 30.0f;
    private float baseFrequency = 0.005f;
    private int octaves = 7;
    private float persistence = 0.55f;
    private float lacunarity = 2.1f;

    // Chunk management
    private int maxActiveChunks = 75;
    private int maxBufferUpdatesPerFrame = 3;
    private float updateThreshold = 8.0f;

    // Generation quality
    private int maxResolution = 128;
    private int minResolution = 16;
    private boolean enableAsyncGeneration = true;
    private int generationThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

    // Biomes
    private boolean enableBiomes = true;
    private float biomeScale = 0.001f;
    private float temperatureScale = 0.002f;
    private float humidityScale = 0.003f;

    @Override
    public void validate() throws ValidationException {
        if (worldSize <= 0) {
            throw new ValidationException("World size must be positive");
        }

        if (baseChunkSize <= 0) {
            throw new ValidationException("Base chunk size must be positive");
        }

        if (maxLODLevel < 0) {
            throw new ValidationException("Max LOD level cannot be negative");
        }

        if (baseAmplitude < 0) {
            throw new ValidationException("Base amplitude cannot be negative");
        }

        if (baseFrequency <= 0) {
            throw new ValidationException("Base frequency must be positive");
        }

        if (octaves <= 0) {
            throw new ValidationException("Octaves must be positive");
        }

        if (persistence <= 0 || persistence >= 1) {
            throw new ValidationException("Persistence must be between 0 and 1");
        }

        if (lacunarity <= 1) {
            throw new ValidationException("Lacunarity must be greater than 1");
        }

        if (maxActiveChunks <= 0) {
            throw new ValidationException("Max active chunks must be positive");
        }

        if (maxBufferUpdatesPerFrame <= 0) {
            throw new ValidationException("Max buffer updates per frame must be positive");
        }

        if (updateThreshold <= 0) {
            throw new ValidationException("Update threshold must be positive");
        }

        if (maxResolution <= minResolution) {
            throw new ValidationException("Max resolution must be greater than min resolution");
        }

        if (generationThreads <= 0) {
            throw new ValidationException("Generation threads must be positive");
        }
    }

    // Getters and setters
    public float getWorldSize() { return worldSize; }
    public void setWorldSize(float worldSize) { this.worldSize = worldSize; }

    public float getBaseChunkSize() { return baseChunkSize; }
    public void setBaseChunkSize(float baseChunkSize) { this.baseChunkSize = baseChunkSize; }

    public int getMaxLODLevel() { return maxLODLevel; }
    public void setMaxLODLevel(int maxLODLevel) { this.maxLODLevel = maxLODLevel; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public float getBaseAmplitude() { return baseAmplitude; }
    public void setBaseAmplitude(float baseAmplitude) { this.baseAmplitude = baseAmplitude; }

    public float getBaseFrequency() { return baseFrequency; }
    public void setBaseFrequency(float baseFrequency) { this.baseFrequency = baseFrequency; }

    public int getOctaves() { return octaves; }
    public void setOctaves(int octaves) { this.octaves = octaves; }

    public float getPersistence() { return persistence; }
    public void setPersistence(float persistence) { this.persistence = persistence; }

    public float getLacunarity() { return lacunarity; }
    public void setLacunarity(float lacunarity) { this.lacunarity = lacunarity; }

    public int getMaxActiveChunks() { return maxActiveChunks; }
    public void setMaxActiveChunks(int maxActiveChunks) { this.maxActiveChunks = maxActiveChunks; }

    public int getMaxBufferUpdatesPerFrame() { return maxBufferUpdatesPerFrame; }
    public void setMaxBufferUpdatesPerFrame(int maxBufferUpdatesPerFrame) { this.maxBufferUpdatesPerFrame = maxBufferUpdatesPerFrame; }

    public float getUpdateThreshold() { return updateThreshold; }
    public void setUpdateThreshold(float updateThreshold) { this.updateThreshold = updateThreshold; }

    public int getMaxResolution() { return maxResolution; }
    public void setMaxResolution(int maxResolution) { this.maxResolution = maxResolution; }

    public int getMinResolution() { return minResolution; }
    public void setMinResolution(int minResolution) { this.minResolution = minResolution; }

    public boolean isEnableAsyncGeneration() { return enableAsyncGeneration; }
    public void setEnableAsyncGeneration(boolean enableAsyncGeneration) { this.enableAsyncGeneration = enableAsyncGeneration; }

    public int getGenerationThreads() { return generationThreads; }
    public void setGenerationThreads(int generationThreads) { this.generationThreads = generationThreads; }

    public boolean isEnableBiomes() { return enableBiomes; }
    public void setEnableBiomes(boolean enableBiomes) { this.enableBiomes = enableBiomes; }

    public float getBiomeScale() { return biomeScale; }
    public void setBiomeScale(float biomeScale) { this.biomeScale = biomeScale; }

    public float getTemperatureScale() { return temperatureScale; }
    public void setTemperatureScale(float temperatureScale) { this.temperatureScale = temperatureScale; }

    public float getHumidityScale() { return humidityScale; }
    public void setHumidityScale(float humidityScale) { this.humidityScale = humidityScale; }
}