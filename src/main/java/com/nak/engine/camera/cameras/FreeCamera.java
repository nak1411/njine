package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Enhanced free camera with smooth movement and proper constraints
 * Works with the existing Camera base class structure
 */
public class FreeCamera extends Camera {
    // Rotation state
    private float pitch = 0.0f;
    private float yaw = -90.0f; // Start facing negative Z
    private float roll = 0.0f;  // Optional roll for advanced movement

    // Movement settings
    private float mouseSensitivity = 0.1f;
    private float movementSpeed = 8.0f;
    private float sprintMultiplier = 2.5f;
    private float slowMultiplier = 0.3f;

    // Movement smoothing
    private boolean smoothMovement = true;
    private float movementSmoothing = 0.15f;
    private final Vector3f velocity = new Vector3f();
    private final Vector3f targetVelocity = new Vector3f();

    // Rotation smoothing
    private boolean smoothRotation = false;
    private float rotationSmoothing = 0.1f;
    private float targetPitch = 0.0f;
    private float targetYaw = -90.0f;

    // Constraints
    private float maxPitch = 89.0f;
    private float minPitch = -89.0f;
    private boolean constrainPitch = true;
    private boolean constrainMovement = false;
    private Vector3f movementBounds = new Vector3f(1000, 1000, 1000); // +/- bounds from origin

    // Flight mode
    private boolean flyingEnabled = true;
    private float gravityStrength = 9.81f;
    private boolean onGround = false;

    // Cached vectors for calculations
    private final Vector3f tempDirection = new Vector3f();
    private final Vector3f tempRight = new Vector3f();
    private final Vector3f tempUp = new Vector3f();

    public FreeCamera() {
        super();
        updateCameraVectors();
    }

    public FreeCamera(Vector3f position) {
        super();
        setPosition(position);
        updateCameraVectors();
    }

    public FreeCamera(Vector3f position, float yaw, float pitch) {
        super();
        setPosition(position);
        this.yaw = yaw;
        this.pitch = pitch;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        updateCameraVectors();
    }

    @Override
    public void update(float deltaTime) {
        // Apply rotation smoothing
        if (smoothRotation) {
            applyRotationSmoothing(deltaTime);
        } else {
            pitch = targetPitch;
            yaw = targetYaw;
        }

        // Update camera vectors
        updateCameraVectors();

        // Apply movement smoothing
        if (smoothMovement) {
            applyMovementSmoothing(deltaTime);
        }

        // Apply movement
        applyMovement(deltaTime);

        // Apply constraints
        if (constrainMovement) {
            applyMovementConstraints();
        }
    }

    private void applyRotationSmoothing(float deltaTime) {
        float rotSpeed = rotationSmoothing * 10.0f;
        float factor = Math.min(1.0f, rotSpeed * deltaTime);

        // Smooth pitch
        pitch = lerp(pitch, targetPitch, factor);

        // Smooth yaw with angle wrapping
        yaw = lerpAngle(yaw, targetYaw, factor);
    }

    private void applyMovementSmoothing(float deltaTime) {
        float moveSpeed = movementSmoothing * 10.0f;
        float factor = Math.min(1.0f, moveSpeed * deltaTime);

        velocity.lerp(targetVelocity, factor);
    }

    private void applyMovement(float deltaTime) {
        if (velocity.length() > 0.001f) {
            Vector3f movement = new Vector3f(velocity).mul(deltaTime);
            Vector3f currentPos = getPosition();
            Vector3f newPos = new Vector3f(currentPos).add(movement);
            setPosition(newPos);
        }
    }

    private void applyMovementConstraints() {
        Vector3f pos = getPosition();
        boolean changed = false;

        if (Math.abs(pos.x) > movementBounds.x) {
            pos.x = Math.signum(pos.x) * movementBounds.x;
            changed = true;
        }
        if (Math.abs(pos.y) > movementBounds.y) {
            pos.y = Math.signum(pos.y) * movementBounds.y;
            changed = true;
        }
        if (Math.abs(pos.z) > movementBounds.z) {
            pos.z = Math.signum(pos.z) * movementBounds.z;
            changed = true;
        }

        if (changed) {
            setPosition(pos);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        if (diff > 180.0f) diff -= 360.0f;
        if (diff < -180.0f) diff += 360.0f;
        return a + diff * t;
    }

    private void updateCameraVectors() {
        // Calculate the new direction vector
        tempDirection.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        tempDirection.y = (float) Math.sin(Math.toRadians(pitch));
        tempDirection.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        tempDirection.normalize();

        // Calculate right vector
        tempDirection.cross(new Vector3f(0, 1, 0), tempRight).normalize();

        // Calculate up vector
        tempRight.cross(tempDirection, tempUp).normalize();

        // Apply roll if needed
        if (Math.abs(roll) > 0.001f) {
            applyRoll();
        }
    }

    private void applyRoll() {
        float rollRad = (float) Math.toRadians(roll);
        float cosRoll = (float) Math.cos(rollRad);
        float sinRoll = (float) Math.sin(rollRad);

        Vector3f newRight = new Vector3f(tempRight).mul(cosRoll).add(new Vector3f(tempUp).mul(sinRoll));
        Vector3f newUp = new Vector3f(tempUp).mul(cosRoll).sub(new Vector3f(tempRight).mul(sinRoll));

        tempRight.set(newRight);
        tempUp.set(newUp);
    }

    @Override
    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(getPosition()).add(tempDirection);
        return new Matrix4f().lookAt(getPosition(), center, tempUp);
    }

    /**
     * Process mouse movement for camera rotation
     */
    public void processMouseMovement(float xOffset, float yOffset) {
        processMouseMovement(xOffset, yOffset, true);
    }

    /**
     * Process mouse movement with optional constraint application
     */
    public void processMouseMovement(float xOffset, float yOffset, boolean constrainPitchParam) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;

        targetYaw += xOffset;
        targetPitch += yOffset;

        // Constrain pitch to prevent screen flipping
        if (constrainPitchParam && constrainPitch) {
            targetPitch = Math.max(minPitch, Math.min(maxPitch, targetPitch));
        }

        // Normalize yaw
        if (targetYaw > 360.0f) targetYaw -= 360.0f;
        if (targetYaw < 0.0f) targetYaw += 360.0f;

        if (!smoothRotation) {
            pitch = targetPitch;
            yaw = targetYaw;
        }
    }

    /**
     * Process keyboard input for camera movement
     */
    public void processKeyboard(Direction direction, float deltaTime) {
        processKeyboard(direction, deltaTime, false, false);
    }

    /**
     * Process keyboard input with modifiers
     */
    public void processKeyboard(Direction direction, float deltaTime, boolean sprint, boolean slow) {
        float velocity = movementSpeed * deltaTime;

        if (sprint) velocity *= sprintMultiplier;
        if (slow) velocity *= slowMultiplier;

        Vector3f movement = new Vector3f();

        switch (direction) {
            case FORWARD -> movement.set(tempDirection).mul(velocity);
            case BACKWARD -> movement.set(tempDirection).mul(-velocity);
            case LEFT -> movement.set(tempRight).mul(-velocity);
            case RIGHT -> movement.set(tempRight).mul(velocity);
            case UP -> {
                if (flyingEnabled) {
                    movement.set(tempUp).mul(velocity);
                } else {
                    movement.set(0, velocity, 0); // World up when not flying
                }
            }
            case DOWN -> {
                if (flyingEnabled) {
                    movement.set(tempUp).mul(-velocity);
                } else {
                    movement.set(0, -velocity, 0); // World down when not flying
                }
            }
        }

        if (smoothMovement) {
            targetVelocity.add(movement.div(deltaTime));
        } else {
            Vector3f currentPos = getPosition();
            setPosition(currentPos.add(movement));
        }
    }

    /**
     * Set movement velocity directly (useful for physics integration)
     */
    public void setVelocity(Vector3f velocity) {
        if (smoothMovement) {
            this.targetVelocity.set(velocity);
        } else {
            this.velocity.set(velocity);
        }
    }

    /**
     * Add velocity to current velocity
     */
    public void addVelocity(Vector3f deltaVelocity) {
        if (smoothMovement) {
            this.targetVelocity.add(deltaVelocity);
        } else {
            this.velocity.add(deltaVelocity);
        }
    }

    /**
     * Stop all movement
     */
    public void stopMovement() {
        velocity.zero();
        targetVelocity.zero();
    }

    /**
     * Look at a specific point
     */
    public void lookAt(Vector3f target) {
        Vector3f direction = new Vector3f(target).sub(getPosition());
        if (direction.length() > 0.001f) {
            direction.normalize();

            // Calculate yaw and pitch from direction
            targetPitch = (float) Math.toDegrees(Math.asin(direction.y));
            targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));

            if (!smoothRotation) {
                pitch = targetPitch;
                yaw = targetYaw;
            }
        }
    }

    /**
     * Reset camera orientation
     */
    public void resetOrientation() {
        targetPitch = 0.0f;
        targetYaw = -90.0f;
        roll = 0.0f;

        if (!smoothRotation) {
            pitch = targetPitch;
            yaw = targetYaw;
        }
    }

    /**
     * Get the current right vector
     */
    @Override
    public Vector3f getRight() {
        return new Vector3f(tempRight);
    }

    /**
     * Get the current up vector
     */
    @Override
    public Vector3f getUp() {
        return new Vector3f(tempUp);
    }

    /**
     * Get the current direction vector
     */
    @Override
    public Vector3f getDirection() {
        return new Vector3f(tempDirection);
    }

    /**
     * Get current velocity
     */
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    // Movement direction enum
    public enum Direction {
        FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN
    }

    // Getters and setters
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) {
        this.targetPitch = constrainPitch ?
                Math.max(minPitch, Math.min(maxPitch, pitch)) : pitch;
        if (!smoothRotation) this.pitch = this.targetPitch;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) {
        this.targetYaw = yaw;
        if (!smoothRotation) this.yaw = yaw;
    }

    public float getRoll() { return roll; }
    public void setRoll(float roll) { this.roll = roll; }

    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.01f, Math.min(5.0f, sensitivity));
    }

    public float getMovementSpeed() { return movementSpeed; }
    public void setMovementSpeed(float speed) {
        this.movementSpeed = Math.max(0.1f, speed);
    }

    public float getSprintMultiplier() { return sprintMultiplier; }
    public void setSprintMultiplier(float multiplier) {
        this.sprintMultiplier = Math.max(1.0f, multiplier);
    }

    public float getSlowMultiplier() { return slowMultiplier; }
    public void setSlowMultiplier(float multiplier) {
        this.slowMultiplier = Math.max(0.1f, Math.min(1.0f, multiplier));
    }

    public boolean isFlyingEnabled() { return flyingEnabled; }
    public void setFlyingEnabled(boolean flyingEnabled) { this.flyingEnabled = flyingEnabled; }

    public boolean isSmoothMovement() { return smoothMovement; }
    public void setSmoothMovement(boolean smoothMovement) {
        this.smoothMovement = smoothMovement;
        if (!smoothMovement) {
            velocity.set(targetVelocity);
        }
    }

    public float getMovementSmoothing() { return movementSmoothing; }
    public void setMovementSmoothing(float smoothing) {
        this.movementSmoothing = Math.max(0.01f, Math.min(1.0f, smoothing));
    }

    public boolean isSmoothRotation() { return smoothRotation; }
    public void setSmoothRotation(boolean smoothRotation) {
        this.smoothRotation = smoothRotation;
        if (!smoothRotation) {
            pitch = targetPitch;
            yaw = targetYaw;
        }
    }

    public float getRotationSmoothing() { return rotationSmoothing; }
    public void setRotationSmoothing(float smoothing) {
        this.rotationSmoothing = Math.max(0.01f, Math.min(1.0f, smoothing));
    }

    public boolean isConstrainPitch() { return constrainPitch; }
    public void setConstrainPitch(boolean constrainPitch) { this.constrainPitch = constrainPitch; }

    public float getMaxPitch() { return maxPitch; }
    public void setMaxPitch(float maxPitch) {
        this.maxPitch = Math.max(-90.0f, Math.min(90.0f, maxPitch));
    }

    public float getMinPitch() { return minPitch; }
    public void setMinPitch(float minPitch) {
        this.minPitch = Math.max(-90.0f, Math.min(90.0f, minPitch));
    }

    public boolean isConstrainMovement() { return constrainMovement; }
    public void setConstrainMovement(boolean constrainMovement) { this.constrainMovement = constrainMovement; }

    public Vector3f getMovementBounds() { return new Vector3f(movementBounds); }
    public void setMovementBounds(Vector3f bounds) { this.movementBounds.set(bounds); }

    /**
     * Get debug information about the camera state
     */
    public String getDebugInfo() {
        Vector3f pos = getPosition();
        return String.format(
                "FreeCamera: Pos(%.1f,%.1f,%.1f) Yaw=%.1f Pitch=%.1f Speed=%.1f Flying=%s Smooth=%s/%s",
                pos.x, pos.y, pos.z,
                yaw, pitch, movementSpeed,
                flyingEnabled ? "ON" : "OFF",
                smoothMovement ? "MOVE" : "OFF",
                smoothRotation ? "ROT" : "OFF"
        );
    }
}