package com.nak.engine.config;

import org.joml.Vector3f;

public class RenderSettings implements Validatable {
    // Display settings
    private int windowWidth = 1920;
    private int windowHeight = 1080;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private int targetFPS = 60;

    // Graphics quality
    private int maxTextureSize = 4096;
    private boolean enableAnisotropicFiltering = true;
    private float anisotropyLevel = 16.0f;
    private boolean enableMipmapping = true;

    // Performance settings
    private boolean enableFrustumCulling = true;
    private boolean enableOcclusionCulling = false;
    private int maxDrawCalls = 1000;
    private boolean enableInstancing = true;

    // Chunk/Terrain rendering settings
    private float viewDistance = 200.0f;
    private int maxVisibleChunks = 25;
    private boolean enableFog = true;
    private float fogDensity = 0.008f;

    // Debug settings
    private boolean enableWireframe = false;
    private boolean showDebugInfo = false;
    private boolean enableGLDebug = false;

    // Lighting settings
    private Vector3f ambientColor = new Vector3f(0.15f, 0.15f, 0.15f);
    private Vector3f lightDirection = new Vector3f(0.2f, -1.0f, 0.3f);
    private Vector3f lightColor = new Vector3f(1.0f, 0.9f, 0.8f);
    private float lightIntensity = 1.0f;
    private float shininess = 32.0f;
    private boolean enableDynamicLighting = true;
    private float ambientStrength = 0.3f;
    private float specularStrength = 0.5f;

    // Shadow settings
    private boolean enableShadows = true;
    private int shadowMapSize = 2048;
    private float shadowBias = 0.005f;

    // Post-processing
    private boolean enablePostProcessing = true;
    private boolean enableBloom = false;
    private boolean enableToneMapping = true;

    // Debug rendering
    private boolean wireframeMode = false;
    private boolean showChunkBounds = false;
    private boolean showFrustumCulling = false;

    @Override
    public void validate() throws ValidationException {
        if (windowWidth <= 0 || windowHeight <= 0) {
            throw new ValidationException("Window dimensions must be positive");
        }

        if (targetFPS <= 0) {
            throw new ValidationException("Target FPS must be positive");
        }

        if (maxTextureSize <= 0) {
            throw new ValidationException("Max texture size must be positive");
        }

        if (anisotropyLevel < 1.0f) {
            throw new ValidationException("Anisotropy level must be >= 1.0");
        }

        if (maxDrawCalls <= 0) {
            throw new ValidationException("Max draw calls must be positive");
        }

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

        if (shadowMapSize <= 0) {
            throw new ValidationException("Shadow map size must be positive");
        }

        if (lightIntensity < 0) {
            throw new ValidationException("Light intensity cannot be negative");
        }

        if (shininess < 0) {
            throw new ValidationException("Shininess cannot be negative");
        }
    }

    // ==========  ALL GETTERS ==========

    // Display settings
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public boolean isFullscreen() { return fullscreen; }
    public boolean isVsync() { return vsync; }
    public int getTargetFPS() { return targetFPS; }

    // Graphics quality
    public int getMaxTextureSize() { return maxTextureSize; }
    public boolean isEnableAnisotropicFiltering() { return enableAnisotropicFiltering; }
    public float getAnisotropyLevel() { return anisotropyLevel; }
    public boolean isEnableMipmapping() { return enableMipmapping; }

    // Performance settings
    public boolean isEnableFrustumCulling() { return enableFrustumCulling; }
    public boolean isEnableOcclusionCulling() { return enableOcclusionCulling; }
    public int getMaxDrawCalls() { return maxDrawCalls; }
    public boolean isEnableInstancing() { return enableInstancing; }

    // Chunk/Terrain rendering settings - THIS FIXES THE getMaxVisibleChunks ERROR
    public float getViewDistance() { return viewDistance; }
    public int getMaxVisibleChunks() { return maxVisibleChunks; }
    public boolean isEnableFog() { return enableFog; }
    public float getFogDensity() { return fogDensity; }

    // Debug settings
    public boolean isEnableWireframe() { return enableWireframe; }
    public boolean isShowDebugInfo() { return showDebugInfo; }
    public boolean isEnableGLDebug() { return enableGLDebug; }

    // Lighting settings
    public Vector3f getAmbientColor() { return new Vector3f(ambientColor); }
    public Vector3f getLightDirection() { return new Vector3f(lightDirection); }
    public Vector3f getLightColor() { return new Vector3f(lightColor); }
    public float getLightIntensity() { return lightIntensity; }
    public float getShininess() { return shininess; }
    public boolean isEnableDynamicLighting() { return enableDynamicLighting; }
    public float getAmbientStrength() { return ambientStrength; }
    public float getSpecularStrength() { return specularStrength; }

    // Shadow settings
    public boolean isEnableShadows() { return enableShadows; }
    public int getShadowMapSize() { return shadowMapSize; }
    public float getShadowBias() { return shadowBias; }

    // Post-processing
    public boolean isEnablePostProcessing() { return enablePostProcessing; }
    public boolean isEnableBloom() { return enableBloom; }
    public boolean isEnableToneMapping() { return enableToneMapping; }

    // Debug rendering
    public boolean isWireframeMode() { return wireframeMode; }
    public boolean isShowChunkBounds() { return showChunkBounds; }
    public boolean isShowFrustumCulling() { return showFrustumCulling; }

    // ==========  ALL SETTERS ==========

    // Display settings
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }
    public void setVsync(boolean vsync) { this.vsync = vsync; }
    public void setTargetFPS(int targetFPS) { this.targetFPS = targetFPS; }

    // Graphics quality
    public void setMaxTextureSize(int maxTextureSize) { this.maxTextureSize = maxTextureSize; }
    public void setEnableAnisotropicFiltering(boolean enableAnisotropicFiltering) { this.enableAnisotropicFiltering = enableAnisotropicFiltering; }
    public void setAnisotropyLevel(float anisotropyLevel) { this.anisotropyLevel = anisotropyLevel; }
    public void setEnableMipmapping(boolean enableMipmapping) { this.enableMipmapping = enableMipmapping; }

    // Performance settings
    public void setEnableFrustumCulling(boolean enableFrustumCulling) { this.enableFrustumCulling = enableFrustumCulling; }
    public void setEnableOcclusionCulling(boolean enableOcclusionCulling) { this.enableOcclusionCulling = enableOcclusionCulling; }
    public void setMaxDrawCalls(int maxDrawCalls) { this.maxDrawCalls = maxDrawCalls; }
    public void setEnableInstancing(boolean enableInstancing) { this.enableInstancing = enableInstancing; }

    // Chunk/Terrain rendering settings
    public void setViewDistance(float viewDistance) { this.viewDistance = viewDistance; }
    public void setMaxVisibleChunks(int maxVisibleChunks) { this.maxVisibleChunks = maxVisibleChunks; }
    public void setEnableFog(boolean enableFog) { this.enableFog = enableFog; }
    public void setFogDensity(float fogDensity) { this.fogDensity = fogDensity; }

    // Debug settings
    public void setEnableWireframe(boolean enableWireframe) { this.enableWireframe = enableWireframe; }
    public void setShowDebugInfo(boolean showDebugInfo) { this.showDebugInfo = showDebugInfo; }
    public void setEnableGLDebug(boolean enableGLDebug) { this.enableGLDebug = enableGLDebug; }

    // Lighting settings
    public void setAmbientColor(Vector3f ambientColor) { this.ambientColor.set(ambientColor); }
    public void setLightDirection(Vector3f lightDirection) { this.lightDirection.set(lightDirection); }
    public void setLightColor(Vector3f lightColor) { this.lightColor.set(lightColor); }
    public void setLightIntensity(float lightIntensity) { this.lightIntensity = lightIntensity; }
    public void setShininess(float shininess) { this.shininess = shininess; }
    public void setEnableDynamicLighting(boolean enableDynamicLighting) { this.enableDynamicLighting = enableDynamicLighting; }
    public void setAmbientStrength(float ambientStrength) { this.ambientStrength = ambientStrength; }
    public void setSpecularStrength(float specularStrength) { this.specularStrength = specularStrength; }

    // Shadow settings
    public void setEnableShadows(boolean enableShadows) { this.enableShadows = enableShadows; }
    public void setShadowMapSize(int shadowMapSize) { this.shadowMapSize = shadowMapSize; }
    public void setShadowBias(float shadowBias) { this.shadowBias = shadowBias; }

    // Post-processing
    public void setEnablePostProcessing(boolean enablePostProcessing) { this.enablePostProcessing = enablePostProcessing; }
    public void setEnableBloom(boolean enableBloom) { this.enableBloom = enableBloom; }
    public void setEnableToneMapping(boolean enableToneMapping) { this.enableToneMapping = enableToneMapping; }

    // Debug rendering
    public void setWireframeMode(boolean wireframeMode) { this.wireframeMode = wireframeMode; }
    public void setShowChunkBounds(boolean showChunkBounds) { this.showChunkBounds = showChunkBounds; }
    public void setShowFrustumCulling(boolean showFrustumCulling) { this.showFrustumCulling = showFrustumCulling; }
}
