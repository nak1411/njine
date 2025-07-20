package com.nak.engine.input;

import com.nak.engine.camera.Camera;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private final long window;
    private final Camera camera;

    // Input state management
    private final Map<InputAction, Boolean> actionStates = new HashMap<>();
    private final Map<InputAction, Boolean> previousActionStates = new HashMap<>();

    // Mouse state
    private double mouseX, mouseY;
    private double previousMouseX, previousMouseY;
    private boolean[] mouseButtons = new boolean[8];
    private boolean[] previousMouseButtons = new boolean[8];
    private double scrollOffset = 0;

    // Input settings
    private boolean mouseLocked = true;
    private boolean inputEnabled = true;
    private float mouseSensitivity = 1.0f;
    private float movementSpeed = 1.0f;

    // Movement state
    private Vector3f inputVector = new Vector3f();
    private boolean sprintMode = false;
    private boolean walkMode = false;

    // Debug state
    private boolean showDebugInfo = false;
    private boolean wireframeMode = false;

    // Key bindings
    private final Map<Integer, InputAction> keyBindings = new HashMap<>();

    public InputHandler(long window, Camera camera) {
        this.window = window;
        this.camera = camera;

        initializeActionStates();
        setupDefaultKeyBindings();
        setupCallbacks();

        // Lock mouse initially
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    private void initializeActionStates() {
        for (InputAction action : InputAction.values()) {
            actionStates.put(action, false);
            previousActionStates.put(action, false);
        }
    }

    private void setupDefaultKeyBindings() {
        // Movement
        keyBindings.put(GLFW_KEY_W, InputAction.MOVE_FORWARD);
        keyBindings.put(GLFW_KEY_S, InputAction.MOVE_BACKWARD);
        keyBindings.put(GLFW_KEY_A, InputAction.MOVE_LEFT);
        keyBindings.put(GLFW_KEY_D, InputAction.MOVE_RIGHT);
        keyBindings.put(GLFW_KEY_SPACE, InputAction.MOVE_UP);
        keyBindings.put(GLFW_KEY_LEFT_SHIFT, InputAction.MOVE_DOWN);

        // Debug
        keyBindings.put(GLFW_KEY_F1, InputAction.TOGGLE_DEBUG);
        keyBindings.put(GLFW_KEY_F2, InputAction.TOGGLE_WIREFRAME);
        keyBindings.put(GLFW_KEY_F11, InputAction.TOGGLE_FULLSCREEN);

        // System
        keyBindings.put(GLFW_KEY_ESCAPE, InputAction.TOGGLE_MOUSE_LOCK);
    }

    private void setupCallbacks() {
        // Key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (!inputEnabled) return;

            InputAction inputAction = keyBindings.get(key);
            if (inputAction != null) {
                boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
                setActionState(inputAction, pressed);

                if (action == GLFW_PRESS) {
                    handleKeyPress(inputAction, mods);
                }
            }
        });

        // Mouse callbacks
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (!inputEnabled || button >= mouseButtons.length) return;
            mouseButtons[button] = (action == GLFW_PRESS);
        });

        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (!inputEnabled) return;
            scrollOffset = yoffset;
        });

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (!inputEnabled) return;
            mouseX = xpos;
            mouseY = ypos;
        });
    }

    public void processInput(float deltaTime) {
        if (!inputEnabled) return;

        updateInputStates();
        processMovement(deltaTime);

        // Reset per-frame data
        scrollOffset = 0;
    }

    private void updateInputStates() {
        // Store previous states
        for (InputAction action : InputAction.values()) {
            previousActionStates.put(action, actionStates.get(action));
        }

        // Update by polling
        for (Map.Entry<Integer, InputAction> entry : keyBindings.entrySet()) {
            boolean pressed = glfwGetKey(window, entry.getKey()) == GLFW_PRESS;
            setActionState(entry.getValue(), pressed);
        }
    }

    private void setActionState(InputAction action, boolean state) {
        actionStates.put(action, state);
    }

    private void processMovement(float deltaTime) {
        inputVector.zero();

        if (isActionActive(InputAction.MOVE_FORWARD)) inputVector.z -= 1.0f;
        if (isActionActive(InputAction.MOVE_BACKWARD)) inputVector.z += 1.0f;
        if (isActionActive(InputAction.MOVE_LEFT)) inputVector.x -= 1.0f;
        if (isActionActive(InputAction.MOVE_RIGHT)) inputVector.x += 1.0f;

        if (inputVector.length() > 0) {
            inputVector.normalize();
        }

        sprintMode = isActionActive(InputAction.MOVE_DOWN);
        walkMode = isActionActive(InputAction.WALK);
    }

    private void handleKeyPress(InputAction action, int mods) {
        switch (action) {
            case TOGGLE_MOUSE_LOCK:
                toggleMouseLock();
                break;
            case TOGGLE_DEBUG:
                showDebugInfo = !showDebugInfo;
                System.out.println("Debug info " + (showDebugInfo ? "enabled" : "disabled"));
                break;
            case TOGGLE_WIREFRAME:
                wireframeMode = !wireframeMode;
                System.out.println("Wireframe mode " + (wireframeMode ? "ENABLED" : "DISABLED"));
                break;
        }
    }

    private void toggleMouseLock() {
        mouseLocked = !mouseLocked;
        glfwSetInputMode(window, GLFW_CURSOR,
                mouseLocked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    // Query methods
    public boolean isActionActive(InputAction action) {
        return actionStates.getOrDefault(action, false);
    }

    public boolean wasActionPressed(InputAction action) {
        boolean current = actionStates.getOrDefault(action, false);
        boolean previous = previousActionStates.getOrDefault(action, false);
        return current && !previous;
    }

    // Getters
    public boolean isShowDebugInfo() {
        return showDebugInfo;
    }

    public boolean isWireframeMode() {
        return wireframeMode;
    }

    public boolean isMouseLocked() {
        return mouseLocked;
    }

    public boolean isInputEnabled() {
        return inputEnabled;
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    public void setShowDebugInfo(boolean show) {
        this.showDebugInfo = show;
    }

    public void setWireframeMode(boolean wireframe) {
        this.wireframeMode = wireframe;
    }

    public void onWindowFocus(boolean focused) {
        if (!focused && mouseLocked) {
            mouseLocked = false;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    public String getControlsHelp() {
        return """
                === CONTROLS ===
                WASD - Move
                Space - Move Up
                Shift - Move Down
                F1 - Toggle Debug
                F2 - Toggle Wireframe
                F11 - Toggle Fullscreen
                Esc - Toggle Mouse Lock
                Mouse - Look Around
                ===============
                """;
    }
}