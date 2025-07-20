package com.nak.engine.input.handlers;

import com.nak.engine.input.InputModule;

public class KeyboardHandler {
    private final InputModule inputModule;
    private boolean[] keyStates = new boolean[512];
    private boolean[] previousKeyStates = new boolean[512];

    public KeyboardHandler(InputModule inputModule) {
        this.inputModule = inputModule;
    }

    public void handleKey(int key, int action, int mods) {
        if (key >= 0 && key < keyStates.length) {
            keyStates[key] = (action == 1 || action == 2); // PRESS or REPEAT
        }
    }

    public void update(float deltaTime) {
        System.arraycopy(keyStates, 0, previousKeyStates, 0, keyStates.length);
    }

    public boolean isKeyPressed(int key) {
        return key >= 0 && key < keyStates.length && keyStates[key];
    }

    public boolean wasKeyJustPressed(int key) {
        return key >= 0 && key < keyStates.length &&
                keyStates[key] && !previousKeyStates[key];
    }

    public boolean wasKeyJustReleased(int key) {
        return key >= 0 && key < keyStates.length &&
                !keyStates[key] && previousKeyStates[key];
    }
}
