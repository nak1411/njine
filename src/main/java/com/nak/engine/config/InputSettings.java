package com.nak.engine.config;

import java.util.HashMap;
import java.util.Map;
import static org.lwjgl.glfw.GLFW.*;

public class InputSettings implements Validatable {
    // Mouse settings
    private float mouseSensitivity = 1.0f;
    private boolean invertY = false;
    private float scrollSensitivity = 1.0f;
    private boolean rawMouseInput = false;

    // Movement settings
    private float movementSpeed = 8.0f;
    private float sprintMultiplier = 2.5f;
    private float slowMultiplier = 0.3f;
    private float acceleration = 25.0f;
    private float friction = 15.0f;

    // Camera settings
    private float smoothing = 0.85f;
    private boolean enableHeadBob = true;
    private float headBobIntensity = 0.02f;
    private boolean enableCameraShake = true;

    // Key bindings (action name -> key code)
    private Map<String, Integer> keyBindings = new HashMap<>();

    public InputSettings() {
        initializeDefaultKeyBindings();
    }

    private void initializeDefaultKeyBindings() {
        // Movement
        keyBindings.put("move_forward", GLFW_KEY_W);
        keyBindings.put("move_backward", GLFW_KEY_S);
        keyBindings.put("move_left", GLFW_KEY_A);
        keyBindings.put("move_right", GLFW_KEY_D);
        keyBindings.put("move_up", GLFW_KEY_SPACE);
        keyBindings.put("move_down", GLFW_KEY_LEFT_SHIFT);
        keyBindings.put("sprint", GLFW_KEY_LEFT_CONTROL);
        keyBindings.put("walk", GLFW_KEY_LEFT_ALT);

        // Camera
        keyBindings.put("reset_camera", GLFW_KEY_R);
        keyBindings.put("toggle_flying", GLFW_KEY_F);
        keyBindings.put("camera_shake_test", GLFW_KEY_T);

        // Debug
        keyBindings.put("toggle_debug", GLFW_KEY_F1);
        keyBindings.put("toggle_wireframe", GLFW_KEY_F2);
        keyBindings.put("toggle_performance", GLFW_KEY_F3);
        keyBindings.put("reload_shaders", GLFW_KEY_F5);
        keyBindings.put("terrain_stress_test", GLFW_KEY_F6);

        // System
        keyBindings.put("toggle_fullscreen", GLFW_KEY_F11);
        keyBindings.put("screenshot", GLFW_KEY_F12);
        keyBindings.put("exit", GLFW_KEY_ESCAPE);
        keyBindings.put("console", GLFW_KEY_GRAVE_ACCENT);

        // Presets
        keyBindings.put("preset_1", GLFW_KEY_1);
        keyBindings.put("preset_2", GLFW_KEY_2);
        keyBindings.put("preset_3", GLFW_KEY_3);
        keyBindings.put("preset_4", GLFW_KEY_4);
        keyBindings.put("preset_5", GLFW_KEY_5);
    }

    @Override
    public void validate() throws ValidationException {
        if (mouseSensitivity <= 0) {
            throw new ValidationException("Mouse sensitivity must be positive");
        }

        if (scrollSensitivity <= 0) {
            throw new ValidationException("Scroll sensitivity must be positive");
        }

        if (movementSpeed <= 0) {
            throw new ValidationException("Movement speed must be positive");
        }

        if (sprintMultiplier <= 1) {
            throw new ValidationException("Sprint multiplier must be greater than 1");
        }

        if (slowMultiplier <= 0 || slowMultiplier >= 1) {
            throw new ValidationException("Slow multiplier must be between 0 and 1");
        }

        if (acceleration <= 0) {
            throw new ValidationException("Acceleration must be positive");
        }

        if (friction < 0) {
            throw new ValidationException("Friction cannot be negative");
        }

        if (smoothing < 0 || smoothing > 1) {
            throw new ValidationException("Smoothing must be between 0 and 1");
        }

        if (headBobIntensity < 0) {
            throw new ValidationException("Head bob intensity cannot be negative");
        }
    }

    // Getters and setters
    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float mouseSensitivity) { this.mouseSensitivity = mouseSensitivity; }

    public boolean isInvertY() { return invertY; }
    public void setInvertY(boolean invertY) { this.invertY = invertY; }

    public float getScrollSensitivity() { return scrollSensitivity; }
    public void setScrollSensitivity(float scrollSensitivity) { this.scrollSensitivity = scrollSensitivity; }

    public boolean isRawMouseInput() { return rawMouseInput; }
    public void setRawMouseInput(boolean rawMouseInput) { this.rawMouseInput = rawMouseInput; }

    public float getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(float movementSpeed) { this.movementSpeed = movementSpeed; }

    public float getSprintMultiplier() { return sprintMultiplier; }
    public void setSprintMultiplier(float sprintMultiplier) { this.sprintMultiplier = sprintMultiplier; }

    public float getSlowMultiplier() { return slowMultiplier; }
    public void setSlowMultiplier(float slowMultiplier) { this.slowMultiplier = slowMultiplier; }

    public float getAcceleration() { return acceleration; }
    public void setAcceleration(float acceleration) { this.acceleration = acceleration; }

    public float getFriction() { return friction; }
    public void setFriction(float friction) { this.friction = friction; }

    public float getSmoothing() { return smoothing; }
    public void setSmoothing(float smoothing) { this.smoothing = smoothing; }

    public boolean isEnableHeadBob() { return enableHeadBob; }
    public void setEnableHeadBob(boolean enableHeadBob) { this.enableHeadBob = enableHeadBob; }

    public float getHeadBobIntensity() { return headBobIntensity; }
    public void setHeadBobIntensity(float headBobIntensity) { this.headBobIntensity = headBobIntensity; }

    public boolean isEnableCameraShake() { return enableCameraShake; }
    public void setEnableCameraShake(boolean enableCameraShake) { this.enableCameraShake = enableCameraShake; }

    public Map<String, Integer> getKeyBindings() { return new HashMap<>(keyBindings); }
    public void setKeyBindings(Map<String, Integer> keyBindings) { this.keyBindings = new HashMap<>(keyBindings); }

    public Integer getKeyBinding(String action) {
        return keyBindings.get(action);
    }

    public void setKeyBinding(String action, int keyCode) {
        keyBindings.put(action, keyCode);
    }

    public void removeKeyBinding(String action) {
        keyBindings.remove(action);
    }
}
