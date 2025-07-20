package com.nak.engine.render.pipelines;

import com.nak.engine.config.RenderSettings;
import com.nak.engine.render.RenderContext;

public class SkyboxPipeline {
    private final RenderSettings settings;
    private boolean initialized = false;

    public SkyboxPipeline(RenderSettings settings) {
        this.settings = settings;
    }

    public void update(float deltaTime) {
        // Update skybox animation
    }

    public void render(RenderContext context) {
        if (!initialized) return;

        // Render skybox
    }

    public void reloadShader() {
        System.out.println("Reloading skybox shader in pipeline");
    }

    public void onResize(int width, int height) {
        // Handle viewport resize for skybox
    }

    public void cleanup() {
        initialized = false;
    }
}