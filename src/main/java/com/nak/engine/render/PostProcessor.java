package com.nak.engine.render;

class PostProcessor {
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void process() {
        if (!enabled) return;

        // Placeholder for post-processing effects
        // In a real implementation, this would:
        // 1. Render scene to framebuffer
        // 2. Apply effects (bloom, tone mapping, etc.)
        // 3. Present final image
    }

    public void cleanup() {
        // Cleanup framebuffers and textures
    }
}