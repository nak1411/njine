package com.nak.engine.input;

import com.nak.engine.entity.Camera;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private long window;
    private Camera camera;

    // Key states for smooth movement
    private boolean[] keyStates = new boolean[GLFW_KEY_LAST];
    private boolean[] previousKeyStates = new boolean[GLFW_KEY_LAST];

    // Mouse state
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private boolean middleMousePressed = false;
    private double scrollOffset = 0;

    // Movement modifiers
    private boolean sprintMode = false;
    private boolean walkMode = false;
    private boolean crouchMode = false;

    // Camera modes
    private boolean freeCameraMode = true;
    private boolean mouseLocked = true;

    // Debug info
    private boolean showDebugInfo = false;
    private boolean wireframeMode = false;

    public InputHandler(long window, Camera camera) {
        this.window = window;
        this.camera = camera;
        setupCallbacks();
    }

    private void setupCallbacks() {
        // Mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                leftMousePressed = (action == GLFW_PRESS);
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                rightMousePressed = (action == GLFW_PRESS);
            } else if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
                middleMousePressed = (action == GLFW_PRESS);
            }
        });

        // Scroll callback for zoom
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            scrollOffset = yoffset;
            camera.zoom((float) yoffset * 2.0f);
        });

        // Key callback for toggle actions
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                handleKeyPress(key, mods);
            }
        });
    }

    private void handleKeyPress(int key, int mods) {
        switch (key) {
            case GLFW_KEY_ESCAPE:
                // Toggle mouse lock
                toggleMouseLock();
                break;

            case GLFW_KEY_F1:
                showDebugInfo = !showDebugInfo;
                break;

            case GLFW_KEY_F2:
                wireframeMode = !wireframeMode;
                break;

            case GLFW_KEY_F3:
                // Toggle camera mode
                camera.setFlying(!camera.isFlying());
                break;

            case GLFW_KEY_F4:
                // Camera shake test
                camera.shake(0.5f, 1.0f);
                break;

            case GLFW_KEY_R:
                // Reset camera position
                camera.setPosition(new Vector3f(0, 10, 3));
                break;

            case GLFW_KEY_T:
                // Teleport to random location
                float x = (float) (Math.random() - 0.5) * 100;
                float z = (float) (Math.random() - 0.5) * 100;
                camera.setPosition(new Vector3f(x, 20, z));
                break;

            case GLFW_KEY_G:
                // Toggle gravity/flying mode
                freeCameraMode = !freeCameraMode;
                camera.setFlying(freeCameraMode);
                break;
        }
    }

    public void processInput(float deltaTime) {
        // Store previous key states
        System.arraycopy(keyStates, 0, previousKeyStates, 0, keyStates.length);

        // Update current key states
        updateKeyStates();

        // Process movement modifiers
        processMovementModifiers();

        // Process camera movement
        processCameraMovement(deltaTime);

        // Process special actions
        processSpecialActions(deltaTime);

        // Reset scroll offset after processing
        scrollOffset = 0;
    }

    private void updateKeyStates() {
        for (int i = 0; i < keyStates.length; i++) {
            keyStates[i] = glfwGetKey(window, i) == GLFW_PRESS;
        }
    }

    private void processMovementModifiers() {
        // Sprint mode (Left Shift)
        sprintMode = keyStates[GLFW_KEY_LEFT_SHIFT];

        // Walk mode (Left Alt for slower movement)
        walkMode = keyStates[GLFW_KEY_LEFT_ALT];

        // Crouch mode (Left Control)
        crouchMode = keyStates[GLFW_KEY_LEFT_CONTROL];
    }

    private void processCameraMovement(float deltaTime) {
        boolean isMoving = false;

        // WASD movement
        if (keyStates[GLFW_KEY_W]) {
            camera.moveForward(deltaTime, sprintMode && !walkMode);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_S]) {
            camera.moveBackward(deltaTime, sprintMode && !walkMode);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_A]) {
            camera.moveLeft(deltaTime, sprintMode && !walkMode);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_D]) {
            camera.moveRight(deltaTime, sprintMode && !walkMode);
            isMoving = true;
        }

        // Vertical movement
        if (keyStates[GLFW_KEY_SPACE]) {
            camera.moveUp(deltaTime);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_LEFT_SHIFT] && camera.isFlying()) {
            camera.moveDown(deltaTime);
            isMoving = true;
        }

        // Arrow keys for fine movement
        if (keyStates[GLFW_KEY_UP]) {
            camera.moveForward(deltaTime * 0.3f, false);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_DOWN]) {
            camera.moveBackward(deltaTime * 0.3f, false);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_LEFT]) {
            camera.moveLeft(deltaTime * 0.3f, false);
            isMoving = true;
        }
        if (keyStates[GLFW_KEY_RIGHT]) {
            camera.moveRight(deltaTime * 0.3f, false);
            isMoving = true;
        }

        // Stop movement if no keys pressed
        if (!isMoving) {
            camera.stopMovement();
        }

        // Apply movement speed modifiers
        if (walkMode) {
            camera.setSpeed(3.0f); // Slower walk speed
        } else {
            camera.setSpeed(8.0f); // Normal speed
        }
    }

    private void processSpecialActions(float deltaTime) {
        // Number keys for preset positions
        if (wasKeyPressed(GLFW_KEY_1)) {
            camera.setPosition(new Vector3f(0, 20, 0)); // Origin overview
        }
        if (wasKeyPressed(GLFW_KEY_2)) {
            camera.setPosition(new Vector3f(50, 30, 50)); // Corner view
        }
        if (wasKeyPressed(GLFW_KEY_3)) {
            camera.setPosition(new Vector3f(0, 100, 0)); // High overview
        }

        // Camera sensitivity adjustment
        if (keyStates[GLFW_KEY_KP_ADD]) {
            camera.setSensitivity(camera.getSensitivity() + 0.01f);
        }
        if (keyStates[GLFW_KEY_KP_SUBTRACT]) {
            camera.setSensitivity(Math.max(0.01f, camera.getSensitivity() - 0.01f));
        }

        // FOV adjustment
        if (keyStates[GLFW_KEY_EQUAL]) {
            camera.setFov(Math.min(120.0f, camera.getFov() + 30.0f * deltaTime));
        }
        if (keyStates[GLFW_KEY_MINUS]) {
            camera.setFov(Math.max(10.0f, camera.getFov() - 30.0f * deltaTime));
        }

        // Mouse picking (when left mouse is clicked)
        if (leftMousePressed) {
            handleMousePicking();
        }
    }

    private void handleMousePicking() {
        // Get mouse position
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);

        // Get window size
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);

        // Calculate mouse ray
        Vector3f mouseRay = camera.getMouseRay((float) xpos[0], (float) ypos[0], width[0], height[0]);

        // Perform ray casting (would need terrain collision detection)
        performRayCast(camera.getPosition(), mouseRay);
    }

    private void performRayCast(Vector3f origin, Vector3f direction) {
        // Placeholder for ray casting implementation
        // In a real application, this would:
        // 1. Cast ray through terrain chunks
        // 2. Find intersection point
        // 3. Trigger appropriate action (place block, select object, etc.)

        System.out.println("Ray cast from " + origin + " in direction " + direction);
    }

    private boolean wasKeyPressed(int key) {
        return keyStates[key] && !previousKeyStates[key];
    }

    private boolean wasKeyReleased(int key) {
        return !keyStates[key] && previousKeyStates[key];
    }

    private void toggleMouseLock() {
        mouseLocked = !mouseLocked;
        if (mouseLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    // Handle window focus events
    public void onWindowFocus(boolean focused) {
        if (!focused) {
            // Release mouse when window loses focus
            mouseLocked = false;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    // Get input state for UI/debug display
    public String getInputDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== INPUT DEBUG ===\n");
        info.append("Mouse Locked: ").append(mouseLocked).append("\n");
        info.append("Sprint Mode: ").append(sprintMode).append("\n");
        info.append("Walk Mode: ").append(walkMode).append("\n");
        info.append("Crouch Mode: ").append(crouchMode).append("\n");
        info.append("Flying Mode: ").append(camera.isFlying()).append("\n");
        info.append("Grounded: ").append(camera.isGrounded()).append("\n");
        info.append("Camera FOV: ").append(String.format("%.1f", camera.getFov())).append("\n");
        info.append("Sensitivity: ").append(String.format("%.3f", camera.getSensitivity())).append("\n");

        Vector3f pos = camera.getPosition();
        info.append("Position: ").append(String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)).append("\n");

        Vector3f vel = camera.getVelocity();
        info.append("Velocity: ").append(String.format("%.1f", vel.length())).append("\n");

        info.append("Pitch: ").append(String.format("%.1f", camera.getPitch())).append("\n");
        info.append("Yaw: ").append(String.format("%.1f", camera.getYaw())).append("\n");

        return info.toString();
    }

    public String getControlsHelp() {
        return """
                === CONTROLS ===
                WASD - Move
                Mouse - Look around
                Space - Up/Jump
                Shift - Sprint/Down (flying)
                Alt - Walk slower
                Ctrl - Crouch
                
                Arrow Keys - Fine movement
                1,2,3 - Preset positions
                R - Reset position
                T - Random teleport
                G - Toggle flying/gravity
                F1 - Toggle debug info
                F2 - Toggle wireframe
                F3 - Toggle camera mode
                F4 - Test camera shake
                
                +/- - Adjust FOV
                Numpad +/- - Adjust sensitivity
                
                ESC - Toggle mouse lock
                """;
    }

    // Getters for external systems
    public boolean isShowDebugInfo() {
        return showDebugInfo;
    }

    public boolean isWireframeMode() {
        return wireframeMode;
    }

    public boolean isMouseLocked() {
        return mouseLocked;
    }

    public boolean isLeftMousePressed() {
        return leftMousePressed;
    }

    public boolean isRightMousePressed() {
        return rightMousePressed;
    }

    public boolean isMiddleMousePressed() {
        return middleMousePressed;
    }

    public double getScrollOffset() {
        return scrollOffset;
    }

    // Setters
    public void setShowDebugInfo(boolean show) {
        this.showDebugInfo = show;
    }

    public void setWireframeMode(boolean wireframe) {
        this.wireframeMode = wireframe;
    }
}