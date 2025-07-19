package com.nak.engine.input;

import com.nak.engine.entity.Camera;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private final long window;
    private final Camera camera;

    // Input state management
    private final Map<InputAction, Boolean> actionStates = new EnumMap<>(InputAction.class);
    private final Map<InputAction, Boolean> previousActionStates = new EnumMap<>(InputAction.class);
    private final EnumSet<InputAction> pressedThisFrame = EnumSet.noneOf(InputAction.class);
    private final EnumSet<InputAction> releasedThisFrame = EnumSet.noneOf(InputAction.class);

    // Mouse state
    private double mouseX, mouseY;
    private double previousMouseX, previousMouseY;
    private double mouseDeltaX, mouseDeltaY;
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
    private boolean crouchMode = false;

    // Debug and UI state
    private boolean showDebugInfo = false;
    private boolean wireframeMode = false;
    private boolean freeCameraMode = true;

    // Input smoothing
    private final InputSmoother inputSmoother = new InputSmoother();

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
        keyBindings.put(GLFW_KEY_LEFT_CONTROL, InputAction.CROUCH);

        // Arrow keys for fine movement
        keyBindings.put(GLFW_KEY_UP, InputAction.MOVE_FORWARD_SLOW);
        keyBindings.put(GLFW_KEY_DOWN, InputAction.MOVE_BACKWARD_SLOW);
        keyBindings.put(GLFW_KEY_LEFT, InputAction.MOVE_LEFT_SLOW);
        keyBindings.put(GLFW_KEY_RIGHT, InputAction.MOVE_RIGHT_SLOW);

        // Modifiers
        keyBindings.put(GLFW_KEY_LEFT_ALT, InputAction.WALK);

        // Camera controls
        keyBindings.put(GLFW_KEY_R, InputAction.RESET_CAMERA);
        keyBindings.put(GLFW_KEY_T, InputAction.TELEPORT_RANDOM);
        keyBindings.put(GLFW_KEY_G, InputAction.TOGGLE_GRAVITY);

        // Debug and settings
        keyBindings.put(GLFW_KEY_F1, InputAction.TOGGLE_DEBUG);
        keyBindings.put(GLFW_KEY_F2, InputAction.TOGGLE_WIREFRAME);
        keyBindings.put(GLFW_KEY_F3, InputAction.TOGGLE_CAMERA_MODE);
        keyBindings.put(GLFW_KEY_F4, InputAction.CAMERA_SHAKE_TEST);
        keyBindings.put(GLFW_KEY_F11, InputAction.TOGGLE_FULLSCREEN);

        // Preset positions
        keyBindings.put(GLFW_KEY_1, InputAction.PRESET_POSITION_1);
        keyBindings.put(GLFW_KEY_2, InputAction.PRESET_POSITION_2);
        keyBindings.put(GLFW_KEY_3, InputAction.PRESET_POSITION_3);
        keyBindings.put(GLFW_KEY_4, InputAction.PRESET_POSITION_4);
        keyBindings.put(GLFW_KEY_5, InputAction.PRESET_POSITION_5);

        // FOV and sensitivity
        keyBindings.put(GLFW_KEY_EQUAL, InputAction.INCREASE_FOV);
        keyBindings.put(GLFW_KEY_MINUS, InputAction.DECREASE_FOV);
        keyBindings.put(GLFW_KEY_KP_ADD, InputAction.INCREASE_SENSITIVITY);
        keyBindings.put(GLFW_KEY_KP_SUBTRACT, InputAction.DECREASE_SENSITIVITY);

        // Application control
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

        // Mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (!inputEnabled || button >= mouseButtons.length) return;

            mouseButtons[button] = (action == GLFW_PRESS);

            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                handleMousePicking();
            }
        });

        // Scroll callback
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (!inputEnabled) return;

            scrollOffset = yoffset;
            camera.zoom((float) yoffset * 2.0f);
        });

        // Cursor position callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (!inputEnabled) return;

            mouseX = xpos;
            mouseY = ypos;

            if (mouseLocked) {
                mouseDeltaX = mouseX - previousMouseX;
                mouseDeltaY = mouseY - previousMouseY;

                // Apply sensitivity and smoothing
                mouseDeltaX *= mouseSensitivity;
                mouseDeltaY *= mouseSensitivity;

                camera.processMouse(mouseX, mouseY);
            }

            previousMouseX = mouseX;
            previousMouseY = mouseY;
        });

        // Window focus callback
        glfwSetWindowFocusCallback(window, (window, focused) -> {
            if (!focused && mouseLocked) {
                toggleMouseLock();
            }
        });
    }

    public void processInput(float deltaTime) {
        if (!inputEnabled) return;

        updateInputStates();
        processMovement(deltaTime);
        processSpecialActions(deltaTime);
        inputSmoother.update(deltaTime);

        // Reset per-frame data
        scrollOffset = 0;
        mouseDeltaX = 0;
        mouseDeltaY = 0;
        pressedThisFrame.clear();
        releasedThisFrame.clear();
    }

    private void updateInputStates() {
        // Store previous states
        for (InputAction action : InputAction.values()) {
            previousActionStates.put(action, actionStates.get(action));
        }

        System.arraycopy(mouseButtons, 0, previousMouseButtons, 0, mouseButtons.length);

        // Update current states by polling
        for (Map.Entry<Integer, InputAction> entry : keyBindings.entrySet()) {
            boolean pressed = glfwGetKey(window, entry.getKey()) == GLFW_PRESS;
            setActionState(entry.getValue(), pressed);
        }

        // Update mouse buttons
        for (int i = 0; i < mouseButtons.length; i++) {
            mouseButtons[i] = glfwGetMouseButton(window, i) == GLFW_PRESS;
        }

        // Detect pressed/released this frame
        for (InputAction action : InputAction.values()) {
            boolean current = actionStates.get(action);
            boolean previous = previousActionStates.get(action);

            if (current && !previous) {
                pressedThisFrame.add(action);
            } else if (!current && previous) {
                releasedThisFrame.add(action);
            }
        }
    }

    private void setActionState(InputAction action, boolean state) {
        actionStates.put(action, state);
    }

    private void processMovement(float deltaTime) {
        // Reset input vector
        inputVector.zero();

        // Process movement inputs
        if (isActionActive(InputAction.MOVE_FORWARD) || isActionActive(InputAction.MOVE_FORWARD_SLOW)) {
            inputVector.z -= 1.0f;
        }
        if (isActionActive(InputAction.MOVE_BACKWARD) || isActionActive(InputAction.MOVE_BACKWARD_SLOW)) {
            inputVector.z += 1.0f;
        }
        if (isActionActive(InputAction.MOVE_LEFT) || isActionActive(InputAction.MOVE_LEFT_SLOW)) {
            inputVector.x -= 1.0f;
        }
        if (isActionActive(InputAction.MOVE_RIGHT) || isActionActive(InputAction.MOVE_RIGHT_SLOW)) {
            inputVector.x += 1.0f;
        }

        // Normalize diagonal movement
        if (inputVector.length() > 0) {
            inputVector.normalize();
        }

        // Apply movement modifiers
        sprintMode = isActionActive(InputAction.MOVE_DOWN) && camera.isFlying();
        walkMode = isActionActive(InputAction.WALK);
        crouchMode = isActionActive(InputAction.CROUCH);

        // Calculate movement speed
        float speed = movementSpeed;
        if (walkMode || isSlowMovement()) {
            speed *= 0.3f;
        }
        if (sprintMode && !walkMode) {
            speed *= 2.5f;
        }

        // Apply smooth movement
        Vector3f smoothedInput = inputSmoother.smoothInput(inputVector, deltaTime);

        // Send movement to camera
        if (smoothedInput.z < 0) {
            camera.moveForward(deltaTime * Math.abs(smoothedInput.z), sprintMode && !walkMode);
        } else if (smoothedInput.z > 0) {
            camera.moveBackward(deltaTime * smoothedInput.z, sprintMode && !walkMode);
        }

        if (smoothedInput.x < 0) {
            camera.moveLeft(deltaTime * Math.abs(smoothedInput.x), sprintMode && !walkMode);
        } else if (smoothedInput.x > 0) {
            camera.moveRight(deltaTime * smoothedInput.x, sprintMode && !walkMode);
        }

        // Vertical movement
        if (isActionActive(InputAction.MOVE_UP)) {
            camera.moveUp(deltaTime);
        }
        if (isActionActive(InputAction.MOVE_DOWN) && camera.isFlying()) {
            camera.moveDown(deltaTime);
        }

        // Stop movement if no input
        if (inputVector.length() == 0) {
            camera.stopMovement();
        }
    }

    private boolean isSlowMovement() {
        return isActionActive(InputAction.MOVE_FORWARD_SLOW) ||
                isActionActive(InputAction.MOVE_BACKWARD_SLOW) ||
                isActionActive(InputAction.MOVE_LEFT_SLOW) ||
                isActionActive(InputAction.MOVE_RIGHT_SLOW);
    }

    private void processSpecialActions(float deltaTime) {
        // FOV adjustment
        if (isActionActive(InputAction.INCREASE_FOV)) {
            camera.setFov(Math.min(120.0f, camera.getFov() + 60.0f * deltaTime));
        }
        if (isActionActive(InputAction.DECREASE_FOV)) {
            camera.setFov(Math.max(10.0f, camera.getFov() - 60.0f * deltaTime));
        }

        // Sensitivity adjustment
        if (isActionActive(InputAction.INCREASE_SENSITIVITY)) {
            camera.setSensitivity(Math.min(2.0f, camera.getSensitivity() + 1.0f * deltaTime));
        }
        if (isActionActive(InputAction.DECREASE_SENSITIVITY)) {
            camera.setSensitivity(Math.max(0.01f, camera.getSensitivity() - 1.0f * deltaTime));
        }
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
                // Note: The actual wireframe rendering is handled in MasterRenderer
                break;

            case TOGGLE_CAMERA_MODE:
                camera.setFlying(!camera.isFlying());
                System.out.println("Flying mode " + (camera.isFlying() ? "enabled" : "disabled"));
                break;

            case CAMERA_SHAKE_TEST:
                camera.shake(0.5f, 1.0f);
                System.out.println("Camera shake triggered");
                break;

            case RESET_CAMERA:
                camera.setPosition(new Vector3f(0, 20, 3));
                System.out.println("Camera reset to origin");
                break;

            case TELEPORT_RANDOM:
                float x = (float) (Math.random() - 0.5) * 200;
                float z = (float) (Math.random() - 0.5) * 200;
                camera.setPosition(new Vector3f(x, 30, z));
                System.out.println("Teleported to: " + x + ", 30, " + z);
                break;

            case TOGGLE_GRAVITY:
                freeCameraMode = !freeCameraMode;
                camera.setFlying(freeCameraMode);
                System.out.println("Gravity mode " + (freeCameraMode ? "off (flying)" : "on"));
                break;

            case PRESET_POSITION_1:
                camera.setPosition(new Vector3f(0, 20, 0));
                System.out.println("Moved to preset position 1");
                break;
            case PRESET_POSITION_2:
                camera.setPosition(new Vector3f(50, 30, 50));
                System.out.println("Moved to preset position 2");
                break;
            case PRESET_POSITION_3:
                camera.setPosition(new Vector3f(0, 100, 0));
                System.out.println("Moved to preset position 3");
                break;
            case PRESET_POSITION_4:
                camera.setPosition(new Vector3f(-50, 25, -50));
                System.out.println("Moved to preset position 4");
                break;
            case PRESET_POSITION_5:
                camera.setPosition(new Vector3f(100, 40, 0));
                System.out.println("Moved to preset position 5");
                break;

            default:
                break;
        }
    }

    private void handleMousePicking() {
        try {
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(window, xpos, ypos);

            int[] width = new int[1];
            int[] height = new int[1];
            glfwGetWindowSize(window, width, height);

            Vector3f mouseRay = camera.getMouseRay(
                    (float) xpos[0],
                    (float) ypos[0],
                    width[0],
                    height[0]
            );

            // Perform ray casting
            performRayCast(camera.getPosition(), mouseRay);
        } catch (Exception e) {
            System.err.println("Error in mouse picking: " + e.getMessage());
        }
    }

    private void performRayCast(Vector3f origin, Vector3f direction) {
        // Placeholder for ray casting implementation
        System.out.println("Ray cast from " + origin + " in direction " + direction);
    }

    private void toggleMouseLock() {
        mouseLocked = !mouseLocked;
        if (mouseLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    // Query methods
    public boolean isActionActive(InputAction action) {
        return actionStates.getOrDefault(action, false);
    }

    public boolean wasActionPressed(InputAction action) {
        return pressedThisFrame.contains(action);
    }

    public boolean wasActionReleased(InputAction action) {
        return releasedThisFrame.contains(action);
    }

    public boolean isMouseButtonPressed(int button) {
        return button >= 0 && button < mouseButtons.length && mouseButtons[button];
    }

    public boolean wasMouseButtonPressed(int button) {
        return button >= 0 && button < mouseButtons.length &&
                mouseButtons[button] && !previousMouseButtons[button];
    }

    // Getters and setters
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

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    public Vector3f getInputVector() {
        return new Vector3f(inputVector);
    }

    public void setShowDebugInfo(boolean show) {
        this.showDebugInfo = show;
    }

    public void setWireframeMode(boolean wireframe) {
        this.wireframeMode = wireframe;
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.1f, sensitivity);
    }

    public void setMovementSpeed(float speed) {
        this.movementSpeed = Math.max(0.1f, speed);
    }

    /**
     * Get debug information about input state
     */
    public String getInputDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== INPUT DEBUG ===\n");
        info.append("Mouse Locked: ").append(mouseLocked).append("\n");
        info.append("Input Enabled: ").append(inputEnabled).append("\n");
        info.append("Sprint Mode: ").append(sprintMode).append("\n");
        info.append("Walk Mode: ").append(walkMode).append("\n");
        info.append("Crouch Mode: ").append(crouchMode).append("\n");
        info.append("Flying Mode: ").append(camera.isFlying()).append("\n");
        info.append("Grounded: ").append(camera.isGrounded()).append("\n");
        info.append("Mouse Sensitivity: ").append(String.format("%.3f", mouseSensitivity)).append("\n");
        info.append("Movement Speed: ").append(String.format("%.3f", movementSpeed)).append("\n");
        info.append("Input Vector: ").append(String.format("%.2f, %.2f, %.2f",
                inputVector.x, inputVector.y, inputVector.z)).append("\n");

        Vector3f pos = camera.getPosition();
        info.append("Position: ").append(String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)).append("\n");

        Vector3f vel = camera.getVelocity();
        info.append("Velocity: ").append(String.format("%.2f", vel.length())).append("\n");

        return info.toString();
    }

    /**
     * Get help text for controls
     */
    public String getControlsHelp() {
        return """
                === ENHANCED CONTROLS ===
                MOVEMENT:
                  WASD - Move (hold Shift for sprint, Alt for walk)
                  Mouse - Look around
                  Space - Up/Jump
                  Left Shift - Sprint (when moving) / Down (when flying)
                  Left Alt - Walk slower
                  Left Ctrl - Crouch
                  Arrow Keys - Fine movement
                
                CAMERA:
                  R - Reset position
                  T - Random teleport
                  G - Toggle flying/gravity mode
                  F3 - Toggle camera mode
                  F4 - Test camera shake
                  1-5 - Preset positions
                
                VIEW:
                  +/- - Adjust FOV
                  Numpad +/- - Adjust mouse sensitivity
                  F1 - Toggle debug info
                  F2 - Toggle wireframe mode
                  F11 - Toggle fullscreen
                
                SYSTEM:
                  ESC - Toggle mouse lock
                  Left Mouse - Ray cast / Interact
                  Mouse Wheel - Zoom
                
                TIPS:
                - Use smooth movement for precise control
                - Combine modifiers for different movement speeds
                - Check debug info (F1) for performance metrics
                """;
    }

    /**
     * Bind a key to an action
     */
    public void bindKey(int key, InputAction action) {
        keyBindings.put(key, action);
    }

    /**
     * Remove a key binding
     */
    public void unbindKey(int key) {
        keyBindings.remove(key);
    }

    /**
     * Get current key binding for an action
     */
    public Integer getKeyForAction(InputAction action) {
        for (Map.Entry<Integer, InputAction> entry : keyBindings.entrySet()) {
            if (entry.getValue() == action) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Handle window focus events
     */
    public void onWindowFocus(boolean focused) {
        if (!focused) {
            // Release mouse lock when window loses focus for better UX
            if (mouseLocked) {
                mouseLocked = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }

            // Clear all input states when losing focus to prevent stuck keys
            for (InputAction action : InputAction.values()) {
                actionStates.put(action, false);
            }

            // Clear mouse button states
            for (int i = 0; i < mouseButtons.length; i++) {
                mouseButtons[i] = false;
            }

            // Reset input vector
            inputVector.zero();
            inputSmoother.reset();

            // Stop camera movement
            camera.stopMovement();
        }
        // Note: We don't automatically re-lock the mouse when gaining focus
        // to avoid surprising the user - they need to press ESC to re-lock
    }

    /**
     * Input action enumeration
     */
    public enum InputAction {
        // Movement
        MOVE_FORWARD,
        MOVE_BACKWARD,
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        MOVE_FORWARD_SLOW,
        MOVE_BACKWARD_SLOW,
        MOVE_LEFT_SLOW,
        MOVE_RIGHT_SLOW,

        // Movement modifiers
        WALK,
        CROUCH,

        // Camera
        RESET_CAMERA,
        TELEPORT_RANDOM,
        TOGGLE_GRAVITY,
        TOGGLE_CAMERA_MODE,
        CAMERA_SHAKE_TEST,

        // Preset positions
        PRESET_POSITION_1,
        PRESET_POSITION_2,
        PRESET_POSITION_3,
        PRESET_POSITION_4,
        PRESET_POSITION_5,

        // View adjustments
        INCREASE_FOV,
        DECREASE_FOV,
        INCREASE_SENSITIVITY,
        DECREASE_SENSITIVITY,

        // Debug and display
        TOGGLE_DEBUG,
        TOGGLE_WIREFRAME,
        TOGGLE_FULLSCREEN,

        // System
        TOGGLE_MOUSE_LOCK,
        EXIT_APPLICATION
    }

    /**
     * Input smoothing utility for better movement feel
     */
    private static class InputSmoother {
        private Vector3f smoothedInput = new Vector3f();
        private Vector3f inputVelocity = new Vector3f();
        private final float smoothTime = 0.1f;
        private final float maxSpeed = 10.0f;

        public void update(float deltaTime) {
            // Update method called each frame for any time-based smoothing
            // Currently just maintains the smoothing state
            // Could be extended for more complex smoothing behaviors
        }

        public Vector3f smoothInput(Vector3f targetInput, float deltaTime) {
            // Smooth the input using a damped spring
            smoothVector3f(smoothedInput, targetInput, inputVelocity, smoothTime, maxSpeed, deltaTime);
            return new Vector3f(smoothedInput);
        }

        private void smoothVector3f(Vector3f current, Vector3f target, Vector3f velocity,
                                    float smoothTime, float maxSpeed, float deltaTime) {
            float omega = 2.0f / smoothTime;
            float x = omega * deltaTime;
            float exp = 1.0f / (1.0f + x + 0.48f * x * x + 0.235f * x * x * x);

            Vector3f change = new Vector3f(current).sub(target);
            Vector3f originalTo = new Vector3f(target);

            float maxChange = maxSpeed * smoothTime;
            if (change.length() > maxChange) {
                change.normalize().mul(maxChange);
            }

            target.set(current).sub(change);

            Vector3f temp = new Vector3f(velocity).add(new Vector3f(change).mul(omega)).mul(deltaTime);
            velocity.set(velocity.sub(new Vector3f(change).mul(omega * deltaTime))).mul(exp);

            Vector3f result = new Vector3f(target).add(new Vector3f(change).add(temp).mul(exp));

            // Prevent overshooting
            Vector3f origMinusCurrent = new Vector3f(originalTo).sub(current);
            Vector3f resultMinusOrig = new Vector3f(result).sub(originalTo);

            if (origMinusCurrent.dot(resultMinusOrig) > 0) {
                result.set(originalTo);
                velocity.zero();
            }

            current.set(result);
        }

        public void reset() {
            smoothedInput.zero();
            inputVelocity.zero();
        }
    }
}