package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Enhanced orbit camera with smooth controls and proper constraints
 * Works with the existing Camera base class structure
 */
public class OrbitCamera extends Camera {
    private Vector3f target = new Vector3f(0, 0, 0);
    private float distance = 10.0f;
    private float azimuth = 0.0f;    // Horizontal rotation (yaw)
    private float elevation = 20.0f; // Vertical rotation (pitch)

    // Camera constraints
    private float minDistance = 1.0f;
    private float maxDistance = 100.0f;
    private float minElevation = -85.0f;
    private float maxElevation = 85.0f;

    // Smoothing and animation
    private float smoothingFactor = 0.1f;
    private boolean smoothEnabled = true;

    // Target smoothing for smooth target transitions
    private final Vector3f smoothTarget = new Vector3f();

    // Rotation smoothing
    private float targetAzimuth = 0.0f;
    private float targetElevation = 20.0f;
    private float targetDistance = 10.0f;

    // Auto-rotation
    private boolean autoRotateEnabled = false;
    private float autoRotateSpeed = 10.0f; // degrees per second

    // Cached vectors for calculations
    private final Vector3f tempDirection = new Vector3f();
    private final Vector3f tempRight = new Vector3f();
    private final Vector3f tempUp = new Vector3f();

    public OrbitCamera() {
        super();
        smoothTarget.set(target);
        updateCameraVectors();
    }

    public OrbitCamera(Vector3f target, float distance) {
        super();
        this.target.set(target);
        this.distance = distance;
        this.targetDistance = distance;
        smoothTarget.set(target);
        updateCameraVectors();
    }

    public OrbitCamera(Vector3f target, float distance, float azimuth, float elevation) {
        super();
        this.target.set(target);
        this.distance = distance;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.targetDistance = distance;
        this.targetAzimuth = azimuth;
        this.targetElevation = elevation;
        smoothTarget.set(target);
        updateCameraVectors();
    }

    @Override
    public void update(float deltaTime) {
        // Update auto-rotation
        if (autoRotateEnabled) {
            targetAzimuth += autoRotateSpeed * deltaTime;
            if (targetAzimuth > 360.0f) {
                targetAzimuth -= 360.0f;
            }
        }

        // Apply smoothing
        if (smoothEnabled) {
            applySmoothing(deltaTime);
        } else {
            // No smoothing - use target values directly
            azimuth = targetAzimuth;
            elevation = targetElevation;
            distance = targetDistance;
            smoothTarget.set(target);
        }

        updateCameraVectors();
    }

    private void applySmoothing(float deltaTime) {
        float smoothSpeed = smoothingFactor * 10.0f; // Convert to per-second rate
        float factor = Math.min(1.0f, smoothSpeed * deltaTime);

        // Smooth rotation
        azimuth = lerpAngle(azimuth, targetAzimuth, factor);
        elevation = lerp(elevation, targetElevation, factor);

        // Smooth distance
        distance = lerp(distance, targetDistance, factor);

        // Smooth target position
        smoothTarget.lerp(target, factor);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float lerpAngle(float a, float b, float t) {
        // Handle angle wrapping for smooth rotation
        float diff = b - a;
        if (diff > 180.0f) diff -= 360.0f;
        if (diff < -180.0f) diff += 360.0f;

        float result = a + diff * t;
        if (result > 360.0f) result -= 360.0f;
        if (result < 0.0f) result += 360.0f;

        return result;
    }

    private void updateCameraVectors() {
        // Convert spherical coordinates to Cartesian
        float elevationRad = (float) Math.toRadians(elevation);
        float azimuthRad = (float) Math.toRadians(azimuth);

        float x = distance * (float) (Math.cos(elevationRad) * Math.cos(azimuthRad));
        float y = distance * (float) Math.sin(elevationRad);
        float z = distance * (float) (Math.cos(elevationRad) * Math.sin(azimuthRad));

        // Calculate new camera position
        Vector3f newPosition = new Vector3f(smoothTarget).add(x, y, z);
        setPosition(newPosition);

        // Calculate direction vector (from camera to target)
        tempDirection.set(smoothTarget).sub(newPosition).normalize();

        // Calculate right vector (cross product of direction and world up)
        tempDirection.cross(new Vector3f(0, 1, 0), tempRight).normalize();

        // Calculate up vector (cross product of right and direction)
        tempRight.cross(tempDirection, tempUp).normalize();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), smoothTarget, tempUp);
    }

    /**
     * Orbit around the target by the specified angles
     */
    public void orbit(float deltaAzimuth, float deltaElevation) {
        targetAzimuth += deltaAzimuth;
        targetElevation += deltaElevation;

        // Wrap azimuth
        if (targetAzimuth > 360.0f) targetAzimuth -= 360.0f;
        if (targetAzimuth < 0.0f) targetAzimuth += 360.0f;

        // Constrain elevation
        targetElevation = Math.max(minElevation, Math.min(maxElevation, targetElevation));
    }

    /**
     * Zoom in/out by changing the distance to target
     */
    public void zoom(float deltaDistance) {
        targetDistance += deltaDistance;
        targetDistance = Math.max(minDistance, Math.min(maxDistance, targetDistance));
    }

    /**
     * Pan the camera (move the target position)
     */
    public void pan(float deltaX, float deltaY) {
        // Calculate pan vectors in camera space
        Vector3f rightPan = new Vector3f(tempRight).mul(deltaX);
        Vector3f upPan = new Vector3f(tempUp).mul(deltaY);

        // Apply pan to target
        target.add(rightPan).add(upPan);
    }

    /**
     * Set the orbit angles directly
     */
    public void setOrbit(float azimuth, float elevation) {
        this.targetAzimuth = azimuth;
        this.targetElevation = Math.max(minElevation, Math.min(maxElevation, elevation));
    }

    /**
     * Look at a specific point from current distance
     */
    public void lookAt(Vector3f point) {
        Vector3f direction = new Vector3f(point).sub(target);
        if (direction.length() > 0.001f) {
            direction.normalize();

            // Calculate azimuth and elevation from direction
            targetElevation = (float) Math.toDegrees(Math.asin(direction.y));
            targetAzimuth = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
        }
    }

    /**
     * Focus on a target with optional distance adjustment
     */
    public void focusOn(Vector3f newTarget, float newDistance) {
        setTarget(newTarget);
        setDistance(newDistance);
    }

    /**
     * Reset camera to default position
     */
    public void reset() {
        setTarget(new Vector3f(0, 0, 0));
        setDistance(10.0f);
        setOrbit(0.0f, 20.0f);
    }

    /**
     * Get the forward direction (opposite of camera direction)
     */
    public Vector3f getForward() {
        return new Vector3f(tempDirection).negate();
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
     * Get the current direction vector (camera to target)
     */
    @Override
    public Vector3f getDirection() {
        return new Vector3f(tempDirection);
    }

    /**
     * Get distance to target considering terrain height
     */
    public float getGroundDistance() {
        Vector3f currentPos = getPosition();
        Vector3f groundTarget = new Vector3f(target.x, currentPos.y, target.z);
        return currentPos.distance(groundTarget);
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
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.targetDistance = Math.max(minDistance, Math.min(maxDistance, distance));
        if (!smoothEnabled) {
            this.distance = this.targetDistance;
        }
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.targetAzimuth = azimuth;
        if (!smoothEnabled) {
            this.azimuth = azimuth;
        }
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.targetElevation = Math.max(minElevation, Math.min(maxElevation, elevation));
        if (!smoothEnabled) {
            this.elevation = this.targetElevation;
        }
    }

    // Constraint getters and setters
    public float getMinDistance() { return minDistance; }
    public void setMinDistance(float minDistance) {
        this.minDistance = Math.max(0.1f, minDistance);
        this.targetDistance = Math.max(this.minDistance, this.targetDistance);
    }

    public float getMaxDistance() { return maxDistance; }
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = Math.max(this.minDistance, maxDistance);
        this.targetDistance = Math.min(this.maxDistance, this.targetDistance);
    }

    public float getMinElevation() { return minElevation; }
    public void setMinElevation(float minElevation) {
        this.minElevation = Math.max(-90.0f, minElevation);
        this.targetElevation = Math.max(this.minElevation, this.targetElevation);
    }

    public float getMaxElevation() { return maxElevation; }
    public void setMaxElevation(float maxElevation) {
        this.maxElevation = Math.min(90.0f, maxElevation);
        this.targetElevation = Math.min(this.maxElevation, this.targetElevation);
    }

    // Smoothing controls
    public boolean isSmoothEnabled() { return smoothEnabled; }
    public void setSmoothEnabled(boolean smoothEnabled) { this.smoothEnabled = smoothEnabled; }

    public float getSmoothingFactor() { return smoothingFactor; }
    public void setSmoothingFactor(float smoothingFactor) {
        this.smoothingFactor = Math.max(0.01f, Math.min(1.0f, smoothingFactor));
    }

    // Auto-rotation controls
    public boolean isAutoRotateEnabled() { return autoRotateEnabled; }
    public void setAutoRotateEnabled(boolean autoRotateEnabled) { this.autoRotateEnabled = autoRotateEnabled; }

    public float getAutoRotateSpeed() { return autoRotateSpeed; }
    public void setAutoRotateSpeed(float autoRotateSpeed) { this.autoRotateSpeed = autoRotateSpeed; }

    /**
     * Get debug information about the camera state
     */
    public String getDebugInfo() {
        Vector3f pos = getPosition();
        return String.format(
                "OrbitCamera: Target(%.1f,%.1f,%.1f) Dist=%.1f Az=%.1f El=%.1f Pos(%.1f,%.1f,%.1f)",
                target.x, target.y, target.z,
                distance, azimuth, elevation,
                pos.x, pos.y, pos.z
        );
    }
}