package com.nak.engine.events.events;

import com.nak.engine.events.Event;

public class ShaderReloadedEvent extends Event {
    private final String shaderName;
    private final boolean success;
    private final String errorMessage;

    public ShaderReloadedEvent(String shaderName, boolean success, String errorMessage) {
        this.shaderName = shaderName;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getEventName() {
        return "ShaderReloaded";
    }

    public String getShaderName() {
        return shaderName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}