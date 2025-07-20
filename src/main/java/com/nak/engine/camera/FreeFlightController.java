package com.nak.engine.camera;

import com.nak.engine.camera.cameras.FreeCamera;
import com.nak.engine.events.events.InputActionEvent;
import org.joml.Vector3f;

public class FreeFlightController {
    private final FreeCamera camera;
    private float movementSpeed = 8.0f;
    private float sprintMultiplier = 2.5f;
    private float sensitivity = 0.1f;

    // Movement state
    private final Vector3f velocity = new Vector3f();
    private final Vector3f targetVelocity = new Vector3f();
    private boolean[] movementKeys = new boolean[6]; // Forward, Back, Left, Right, Up, Down
    private boolean sprintMode = false;

    public FreeFlightController(FreeCamera camera) {
        this.camera = camera;
    }

    public void update(float deltaTime) {
        updateMovement(deltaTime);
        applyMovement(deltaTime);
    }

    private void updateMovement(float deltaTime) {
        targetVelocity.zero();

        float speed = movementSpeed * (sprintMode ? sprintMultiplier : 1.0f);

        // Calculate movement direction
        if (movementKeys[0]) targetVelocity.add(camera.getDirection().mul(speed, new Vector3f()));
        if (movementKeys[1]) targetVelocity.sub(camera.getDirection().mul(speed, new Vector3f()));
        if (movementKeys[2]) targetVelocity.sub(camera.getRight().mul(speed, new Vector3f()));
        if (movementKeys[3]) targetVelocity.add(camera.getRight().mul(speed, new Vector3f()));
        if (movementKeys[4]) targetVelocity.add(camera.getUp().mul(speed, new Vector3f()));
        if (movementKeys[5]) targetVelocity.sub(camera.getUp().mul(speed, new Vector3f()));

        // Smooth velocity interpolation
        velocity.lerp(targetVelocity, Math.min(1.0f, deltaTime * 10.0f));
    }

    private void applyMovement(float deltaTime) {
        if (velocity.length() > 0.001f) {
            Vector3f movement = new Vector3f(velocity).mul(deltaTime);
            camera.setPosition(camera.getPosition().add(movement));
        }
    }

    public void handleInput(InputActionEvent event) {
        boolean pressed = event.getType() == InputActionEvent.Type.PRESSED ||
                event.getType() == InputActionEvent.Type.HELD;

        switch (event.getAction()) {
            case MOVE_FORWARD -> movementKeys[0] = pressed;
            case MOVE_BACKWARD -> movementKeys[1] = pressed;
            case MOVE_LEFT -> movementKeys[2] = pressed;
            case MOVE_RIGHT -> movementKeys[3] = pressed;
            case MOVE_UP -> movementKeys[4] = pressed;
            case MOVE_DOWN -> movementKeys[5] = pressed;
        }
    }

    public void processMouseMovement(float xOffset, float yOffset) {
        camera.processMouseMovement(xOffset * sensitivity, yOffset * sensitivity);
    }

    public void setSprintMode(boolean sprint) {
        this.sprintMode = sprint;
    }

    public void cleanup() {
        // Cleanup resources if needed
    }

    // Getters and setters
    public float getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(float speed) { this.movementSpeed = speed; }

    public float getSensitivity() { return sensitivity; }
    public void setSensitivity(float sensitivity) { this.sensitivity = sensitivity; }
}
