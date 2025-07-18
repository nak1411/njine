package com.nak.engine.shader;

@FunctionalInterface
public interface ShaderReloadListener {
    /**
     * Called when a shader program has been successfully reloaded.
     *
     * @param programName The name of the shader program that was reloaded
     */
    void onShaderReloaded(String programName);
}
