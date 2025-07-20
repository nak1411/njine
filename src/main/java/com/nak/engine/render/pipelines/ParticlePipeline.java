package com.nak.engine.render.pipelines;

import com.nak.engine.config.RenderSettings;
import com.nak.engine.render.RenderContext;

public class ParticlePipeline {
    private final RenderSettings settings;
    private boolean initialized = false;

    public ParticlePipeline(RenderSettings settings) {
        this.settings = settings;
    }

    public void update(float deltaTime) {
        // Update particle systems
    }

    public void render(RenderContext context) {
        if (!initialized) return;

        // Render particle systems
    }

    public void reloadShader() {
        System.out.println("Reloading particle shader in pipeline");
    }

    public void onResize(int width, int height) {
        // Handle viewport resize for particles
    }

    public void cleanup() {
        initialized = false;
    }
}