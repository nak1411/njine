package com.nak.engine.camera.cameras;

import com.nak.engine.camera.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Enhanced fixed camera that looks at a specific target
 * Works with the existing Camera base class structure
 */
public class FixedCamera extends Camera {
    private Vector3f target = new Vector3f(0, 0, 0);
    private boolean autoUpdateVectors = true;

    // Smoothing for target changes
    private boolean smoothEnabled = false;
    private float smoothingFactor = 0.1f;
    private final Vector3f smoothTarget = new Vector3f();

    // Cached vectors for calculations
    private final Vector3f tempDirection = new Vector3f();
    private final Vector3f tempRight = new Vector3f();
    private final Vector3f tempUp = new Vector3f();

    public FixedCamera() {
        super();
        smoothTarget.set(target);
        updateCameraVectors();
    }

    public FixedCamera(Vector3f position, Vector3f target) {
        super();
        setPosition(position);
        this.target.set(target);
        smoothTarget.set(target);
        updateCameraVectors();
    }

    public FixedCamera(Vector3f position, Vector3f target, Vector3f up) {
        super();
        setPosition(position);
        this.target.set(target);
        smoothTarget.set(target);
        tempUp.set(up).normalize();
        updateCameraVectors();
    }

    @Override
    public void update(float deltaTime) {
        if (smoothEnabled) {
            applySmoothing(deltaTime);
        } else {
            smoothTarget.set(target);
        }

        if (autoUpdateVectors) {
            updateCameraVectors();
        }
    }

    private void applySmoothing(float deltaTime) {
        float smoothSpeed = smoothingFactor * 10.0f; // Convert to per-second rate
        float factor = Math.min(1.0f, smoothSpeed * deltaTime);
        smoothTarget.lerp(target, factor);
    }

    private void updateCameraVectors() {
        Vector3f currentPos = getPosition();

        // Calculate direction vector (from camera to target)
        tempDirection.set(smoothTarget).sub(currentPos);

        // Check if camera and target are at the same position
        if (tempDirection.length() < 0.001f) {
            // Default to looking forward
            tempDirection.set(0, 0, -1);
        } else {
            tempDirection.normalize();
        }

        // Calculate right vector (cross product of direction and world up)
        Vector3f worldUp = new Vector3f(0, 1, 0);
        tempDirection.cross(worldUp, tempRight);

        // Check for gimbal lock (looking straight up or down)
        if (tempRight.length() < 0.001f) {
            // Use a different reference vector when looking straight up/down
            Vector3f alternateUp = Math.abs(tempDirection.y) > 0.99f ?
                    new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            tempDirection.cross(alternateUp, tempRight);
        }
        tempRight.normalize();

        // Calculate up vector (cross product of right and direction)
        tempRight.cross(tempDirection, tempUp).normalize();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), smoothTarget, tempUp);
    }

    /**
     * Set both position and target simultaneously
     */
    public void setPositionAndTarget(Vector3f position, Vector3f target) {
        setPosition(position);
        setTarget(target);
    }

    /**
     * Look at a new target from current position
     */
    public void lookAt(Vector3f newTarget) {
        setTarget(newTarget);
    }

    /**
     * Move to a new position while maintaining current target
     */
    public void moveTo(Vector3f newPosition) {
        setPosition(newPosition);
        if (autoUpdateVectors) {
            updateCameraVectors();
        }
    }

    /**
     * Offset the camera position relative to its current orientation
     */
    public void offsetPosition(Vector3f offset) {
        Vector3f currentPos = getPosition();
        Vector3f newPos = new Vector3f(currentPos).add(offset);
        setPosition(newPos);
        if (autoUpdateVectors) {
            updateCameraVectors();
        }
    }

    /**
     * Offset the target position
     */
    public void offsetTarget(Vector3f offset) {
        target.add(offset);
        if (!smoothEnabled) {
            smoothTarget.add(offset);
        }
    }

    /**
     * Set camera to look from one point to another
     */
    public void setLookFromTo(Vector3f from, Vector3f to) {
        setPosition(from);
        setTarget(to);
    }

    /**
     * Get the distance to the target
     */
    public float getDistanceToTarget() {
        return getPosition().distance(target);
    }

    /**
     * Get the direction vector to target (normalized)
     */
    public Vector3f getDirectionToTarget() {
        Vector3f currentPos = getPosition();
        return new Vector3f(target).sub(currentPos).normalize();
    }

    /**
     * Check if the camera can see a point (basic frustum check)
     */
    public boolean canSeePoint(Vector3f point, float fovDegrees) {
        Vector3f toPoint = new Vector3f(point).sub(getPosition());
        if (toPoint.length() < 0.001f) return true;

        toPoint.normalize();
        float dot = toPoint.dot(tempDirection);
        float halfFov = (float) Math.toRadians(fovDegrees * 0.5f);

        return dot > Math.cos(halfFov);
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

    // Getters and setters
    public Vector3f getTarget() {
        return new Vector3f(target);
    }

    public void setTarget(Vector3f target) {
        this.target.set(target);
        if (!smoothEnabled) {
            smoothTarget.set(target);
        }
        if (autoUpdateVectors) {
            updateCameraVectors();
        }
    }

    public Vector3f getSmoothTarget() {
        return new Vector3f(smoothTarget);
    }

    public boolean isAutoUpdateVectors() {
        return autoUpdateVectors;
    }

    public void setAutoUpdateVectors(boolean autoUpdateVectors) {
        this.autoUpdateVectors = autoUpdateVectors;
        if (autoUpdateVectors) {
            updateCameraVectors();
        }
    }

    // Smoothing controls
    public boolean isSmoothEnabled() {
        return smoothEnabled;
    }

    public void setSmoothEnabled(boolean smoothEnabled) {
        this.smoothEnabled = smoothEnabled;
        if (!smoothEnabled) {
            smoothTarget.set(target);
        }
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(float smoothingFactor) {
        this.smoothingFactor = Math.max(0.01f, Math.min(1.0f, smoothingFactor));
    }

    /**
     * Force update of camera vectors
     */
    public void forceUpdateVectors() {
        updateCameraVectors();
    }

    /**
     * Get debug information about the camera state
     */
    public String getDebugInfo() {
        Vector3f pos = getPosition();
        return String.format(
                "FixedCamera: Pos(%.1f,%.1f,%.1f) Target(%.1f,%.1f,%.1f) Dist=%.1f Auto=%s Smooth=%s",
                pos.x, pos.y, pos.z,
                target.x, target.y, target.z,
                getDistanceToTarget(),
                autoUpdateVectors ? "ON" : "OFF",
                smoothEnabled ? "ON" : "OFF"
        );
    }
}