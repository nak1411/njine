package com.nak.engine.camera;

import com.nak.engine.camera.cameras.OrbitCamera;
import com.nak.engine.events.events.InputActionEvent;
import org.joml.Vector3f;

/**
 * Enhanced orbit controller with improved mouse and keyboard controls
 */
public class OrbitController {
    private OrbitCamera camera;

    // Sensitivity settings
    private float orbitSensitivity = 0.5f;
    private float zoomSensitivity = 1.0f;
    private float panSensitivity = 0.01f;

    // Input state
    private boolean orbiting = false;
    private boolean panning = false;
    private boolean zooming = false;

    // Mouse tracking
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean firstMouse = true;

    // Keyboard state
    private boolean[] movementKeys = new boolean[6]; // Forward, Back, Left, Right, Up, Down
    private float keyboardOrbitSpeed = 45.0f; // degrees per second
    private float keyboardZoomSpeed = 5.0f;   // units per second

    // Auto-focus settings
    private boolean autoFocusEnabled = false;
    private float autoFocusDistance = 15.0f;

    public OrbitController() {
        // Default constructor
    }

    public OrbitController(OrbitCamera camera) {
        setCamera(camera);
    }

    public void setCamera(OrbitCamera camera) {
        this.camera = camera;
    }

    public void update(float deltaTime) {
        if (camera == null) return;

        // Process keyboard input for orbit controls
        processKeyboardInput(deltaTime);

        // Update camera
        camera.update(deltaTime);
    }

    private void processKeyboardInput(float deltaTime) {
        if (!hasValidCamera()) return;

        // Keyboard orbit controls
        float azimuthDelta = 0;
        float elevationDelta = 0;
        float zoomDelta = 0;

        if (movementKeys[2]) azimuthDelta -= keyboardOrbitSpeed * deltaTime;  // Left
        if (movementKeys[3]) azimuthDelta += keyboardOrbitSpeed * deltaTime;  // Right
        if (movementKeys[0]) elevationDelta += keyboardOrbitSpeed * deltaTime; // Forward (up)
        if (movementKeys[1]) elevationDelta -= keyboardOrbitSpeed * deltaTime; // Back (down)
        if (movementKeys[4]) zoomDelta -= keyboardZoomSpeed * deltaTime;       // Up (zoom in)
        if (movementKeys[5]) zoomDelta += keyboardZoomSpeed * deltaTime;       // Down (zoom out)

        // Apply movement
        if (Math.abs(azimuthDelta) > 0.01f || Math.abs(elevationDelta) > 0.01f) {
            camera.orbit(azimuthDelta, elevationDelta);
        }

        if (Math.abs(zoomDelta) > 0.01f) {
            camera.zoom(zoomDelta);
        }
    }

    public void handleInput(InputActionEvent event) {
        if (!hasValidCamera()) return;

        boolean pressed = event.getType() == InputActionEvent.Type.PRESSED ||
                event.getType() == InputActionEvent.Type.HELD;

        switch (event.getAction()) {
            case MOVE_FORWARD -> movementKeys[0] = pressed;
            case MOVE_BACKWARD -> movementKeys[1] = pressed;
            case MOVE_LEFT -> movementKeys[2] = pressed;
            case MOVE_RIGHT -> movementKeys[3] = pressed;
            case MOVE_UP -> movementKeys[4] = pressed;
            case MOVE_DOWN -> movementKeys[5] = pressed;

            // Special orbit camera controls
            case RESET_CAMERA -> {
                if (event.getType() == InputActionEvent.Type.PRESSED) {
                    camera.reset();
                }
            }

            case TOGGLE_CAMERA_MODE -> {
                if (event.getType() == InputActionEvent.Type.PRESSED) {
                    toggleAutoRotate();
                }
            }
        }
    }

    public void startOrbit() {
        orbiting = true;
        firstMouse = true;
    }

    public void stopOrbit() {
        orbiting = false;
    }

    public void startPan() {
        panning = true;
        firstMouse = true;
    }

    public void stopPan() {
        panning = false;
    }

    public void startZoom() {
        zooming = true;
    }

    public void stopZoom() {
        zooming = false;
    }

    public void processMouseMovement(double xpos, double ypos) {
        if (!hasValidCamera()) return;

        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
            return;
        }

        float xOffset = (float) (xpos - lastMouseX);
        float yOffset = (float) (lastMouseY - ypos); // Reversed since y-coordinates go from bottom to top

        lastMouseX = xpos;
        lastMouseY = ypos;

        if (orbiting) {
            camera.orbit(
                    xOffset * orbitSensitivity,
                    yOffset * orbitSensitivity
            );
        }

        if (panning) {
            // Pan based on current distance for proportional movement
            float panScale = camera.getDistance() * panSensitivity;
            camera.pan(
                    -xOffset * panScale,
                    yOffset * panScale
            );
        }
    }

    public void processScroll(float scrollOffset) {
        if (!hasValidCamera()) return;

        float zoomDelta = -scrollOffset * zoomSensitivity;
        camera.zoom(zoomDelta);
    }

    public void processMouseButton(int button, int action, int mods) {
        boolean pressed = (action == 1); // GLFW_PRESS

        switch (button) {
            case 0: // Left mouse button - orbit
                if (pressed) {
                    startOrbit();
                } else {
                    stopOrbit();
                }
                break;

            case 1: // Right mouse button - pan
                if (pressed) {
                    startPan();
                } else {
                    stopPan();
                }
                break;

            case 2: // Middle mouse button - zoom
                if (pressed) {
                    startZoom();
                } else {
                    stopZoom();
                }
                break;
        }
    }

    public void setTarget(Vector3f target) {
        if (hasValidCamera()) {
            camera.setTarget(target);

            if (autoFocusEnabled) {
                camera.setDistance(autoFocusDistance);
            }
        }
    }

    public void focusOn(Vector3f target, float distance) {
        if (hasValidCamera()) {
            camera.focusOn(target, distance);
        }
    }

    public void lookAt(Vector3f point) {
        if (hasValidCamera()) {
            camera.lookAt(point);
        }
    }

    public void toggleAutoRotate() {
        if (hasValidCamera()) {
            camera.setAutoRotateEnabled(!camera.isAutoRotateEnabled());
            System.out.println("Auto-rotate " + (camera.isAutoRotateEnabled() ? "enabled" : "disabled"));
        }
    }

    public void toggleSmoothing() {
        if (hasValidCamera()) {
            camera.setSmoothEnabled(!camera.isSmoothEnabled());
            System.out.println("Orbit smoothing " + (camera.isSmoothEnabled() ? "enabled" : "disabled"));
        }
    }

    /**
     * Set orbit position and zoom to frame a bounding box
     */
    public void frameBounds(Vector3f min, Vector3f max) {
        if (!hasValidCamera()) return;

        // Calculate center and size of bounds
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        Vector3f size = new Vector3f(max).sub(min);
        float maxDimension = Math.max(size.x, Math.max(size.y, size.z));

        // Set camera to frame the bounds
        setTarget(center);

        // Calculate appropriate distance based on field of view
        float fov = camera.getFov();
        float distance = maxDimension / (2.0f * (float) Math.tan(Math.toRadians(fov / 2.0f)));
        distance *= 1.5f; // Add some margin

        camera.setDistance(distance);
    }

    /**
     * Animate orbit to a specific position
     */
    public void orbitTo(float azimuth, float elevation, float distance, float duration) {
        if (!hasValidCamera()) return;

        // Enable smoothing for animation
        boolean wasSmooth = camera.isSmoothEnabled();
        camera.setSmoothEnabled(true);

        // Set target values
        camera.setOrbit(azimuth, elevation);
        camera.setDistance(distance);

        // TODO: Could implement a timer to restore smoothing setting after animation
    }

    private boolean hasValidCamera() {
        return camera != null;
    }

    // Getters and setters
    public float getOrbitSensitivity() { return orbitSensitivity; }
    public void setOrbitSensitivity(float sensitivity) {
        this.orbitSensitivity = Math.max(0.1f, Math.min(5.0f, sensitivity));
    }

    public float getZoomSensitivity() { return zoomSensitivity; }
    public void setZoomSensitivity(float sensitivity) {
        this.zoomSensitivity = Math.max(0.1f, Math.min(5.0f, sensitivity));
    }

    public float getPanSensitivity() { return panSensitivity; }
    public void setPanSensitivity(float sensitivity) {
        this.panSensitivity = Math.max(0.001f, Math.min(0.1f, sensitivity));
    }

    public float getKeyboardOrbitSpeed() { return keyboardOrbitSpeed; }
    public void setKeyboardOrbitSpeed(float speed) {
        this.keyboardOrbitSpeed = Math.max(1.0f, Math.min(180.0f, speed));
    }

    public float getKeyboardZoomSpeed() { return keyboardZoomSpeed; }
    public void setKeyboardZoomSpeed(float speed) {
        this.keyboardZoomSpeed = Math.max(0.1f, Math.min(50.0f, speed));
    }

    public boolean isAutoFocusEnabled() { return autoFocusEnabled; }
    public void setAutoFocusEnabled(boolean autoFocusEnabled) { this.autoFocusEnabled = autoFocusEnabled; }

    public float getAutoFocusDistance() { return autoFocusDistance; }
    public void setAutoFocusDistance(float autoFocusDistance) {
        this.autoFocusDistance = Math.max(1.0f, autoFocusDistance);
    }

    // State queries
    public boolean isOrbiting() { return orbiting; }
    public boolean isPanning() { return panning; }
    public boolean isZooming() { return zooming; }

    /**
     * Get current control state as debug string
     */
    public String getDebugInfo() {
        if (!hasValidCamera()) {
            return "OrbitController: No camera attached";
        }

        return String.format(
                "OrbitController: Orbit=%s Pan=%s Zoom=%s Sens=%.1f/%.1f/%.3f Auto=%s\n%s",
                orbiting ? "ON" : "OFF",
                panning ? "ON" : "OFF",
                zooming ? "ON" : "OFF",
                orbitSensitivity, zoomSensitivity, panSensitivity,
                camera.isAutoRotateEnabled() ? "ON" : "OFF",
                camera.getDebugInfo()
        );
    }

    /**
     * Get usage instructions
     */
    public String getUsageInstructions() {
        return """
            === ORBIT CAMERA CONTROLS ===
            Left Mouse + Drag: Orbit around target
            Right Mouse + Drag: Pan target position
            Mouse Wheel: Zoom in/out
            WASD: Keyboard orbit (W/S = elevation, A/D = azimuth)
            Space/Shift: Keyboard zoom (in/out)
            R: Reset camera position
            T: Toggle auto-rotation
            ===========================
            """;
    }
}