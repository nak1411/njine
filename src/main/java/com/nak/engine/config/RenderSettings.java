package com.nak.engine.config;

public class RenderSettings implements Validatable {
    // Rendering quality
    private float viewDistance = 200.0f;
    private int maxVisibleChunks = 25;
    private boolean enableFog = true;
    private float fogDensity = 0.008f;

    // Lighting
    private boolean enableDynamicLighting = true;
    private float ambientStrength = 0.3f;
    private float specularStrength = 0.5f;
    private float shininess = 32.0f;

    // Shadows
    private boolean enableShadows = false;
    private int shadowMapSize = 1024;
    private float shadowBias = 0.005f;

    // Post-processing
    private boolean enablePostProcessing = false;
    private boolean enableBloom = false;
    private boolean enableToneMapping = false;

    // Debug rendering
    private boolean wireframeMode = false;
    private boolean showChunkBounds = false;
    private boolean showFrustumCulling = false;

    @Override
    public void validate() throws ValidationException {
        if (viewDistance <= 0) {
            throw new ValidationException("View distance must be positive");
        }

        if (maxVisibleChunks <= 0) {
            throw new ValidationException("Max visible chunks must be positive");
        }

        if (fogDensity < 0) {
            throw new ValidationException("Fog density cannot be negative");
        }

        if (ambientStrength < 0 || ambientStrength > 1) {
            throw new ValidationException("Ambient strength must be between 0 and 1");
        }

        if (specularStrength < 0 || specularStrength > 1) {
            throw new ValidationException("Specular strength must be between 0 and 1");
        }

        if (shininess <= 0) {
            throw new ValidationException("Shininess must be positive");
        }

        if (shadowMapSize <= 0 || (shadowMapSize & (shadowMapSize - 1)) != 0) {
            throw new ValidationException("Shadow map size must be a positive power of 2");
        }
    }

    // Getters and setters
    public float getViewDistance() { return viewDistance; }
    public void setViewDistance(float viewDistance) { this.viewDistance = viewDistance; }

    public int getMaxVisibleChunks() { return maxVisibleChunks; }
    public void setMaxVisibleChunks(int maxVisibleChunks) { this.maxVisibleChunks = maxVisibleChunks; }

    public boolean isEnableFog() { return enableFog; }
    public void setEnableFog(boolean enableFog) { this.enableFog = enableFog; }

    public float getFogDensity() { return fogDensity; }
    public void setFogDensity(float fogDensity) { this.fogDensity = fogDensity; }

    public boolean isEnableDynamicLighting() { return enableDynamicLighting; }
    public void setEnableDynamicLighting(boolean enableDynamicLighting) { this.enableDynamicLighting = enableDynamicLighting; }

    public float getAmbientStrength() { return ambientStrength; }
    public void setAmbientStrength(float ambientStrength) { this.ambientStrength = ambientStrength; }

    public float getSpecularStrength() { return specularStrength; }
    public void setSpecularStrength(float specularStrength) { this.specularStrength = specularStrength; }

    public float getShininess() { return shininess; }
    public void setShininess(float shininess) { this.shininess = shininess; }

    public boolean isEnableShadows() { return enableShadows; }
    public void setEnableShadows(boolean enableShadows) { this.enableShadows = enableShadows; }

    public int getShadowMapSize() { return shadowMapSize; }
    public void setShadowMapSize(int shadowMapSize) { this.shadowMapSize = shadowMapSize; }

    public float getShadowBias() { return shadowBias; }
    public void setShadowBias(float shadowBias) { this.shadowBias = shadowBias; }

    public boolean isEnablePostProcessing() { return enablePostProcessing; }
    public void setEnablePostProcessing(boolean enablePostProcessing) { this.enablePostProcessing = enablePostProcessing; }

    public boolean isEnableBloom() { return enableBloom; }
    public void setEnableBloom(boolean enableBloom) { this.enableBloom = enableBloom; }

    public boolean isEnableToneMapping() { return enableToneMapping; }
    public void setEnableToneMapping(boolean enableToneMapping) { this.enableToneMapping = enableToneMapping; }

    public boolean isWireframeMode() { return wireframeMode; }
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }

    public boolean isShowChunkBounds() { return showChunkBounds; }
    public void setShowChunkBounds(boolean showChunkBounds) { this.showChunkBounds = showChunkBounds; }

    public boolean isShowFrustumCulling() { return showFrustumCulling; }
    public void setShowFrustumCulling(boolean showFrustumCulling) { this.showFrustumCulling = showFrustumCulling; }
}
