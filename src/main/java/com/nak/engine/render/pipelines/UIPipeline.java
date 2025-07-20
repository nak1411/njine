package com.nak.engine.render.pipelines;

import com.nak.engine.config.RenderSettings;
import com.nak.engine.render.RenderContext;

public class UIPipeline {
    private final RenderSettings settings;
    private boolean initialized = false;

    public UIPipeline(RenderSettings settings) {
        this.settings = settings;
    }

    public void update(float deltaTime) {
        // Update UI elements
    }

    public void render(RenderContext context) {
        if (!initialized) return;

        // Render UI elements
    }

    public void reloadShader() {
        System.out.println("Reloading UI shader in pipeline");
    }

    public void onResize(int width, int height) {
        // Handle viewport resize for UI
    }

    public void cleanup() {
        initialized = false;
    }
}