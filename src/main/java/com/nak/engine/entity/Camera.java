package com.nak.engine.entity;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.glLoadMatrixf;

public class Camera {
    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;
    private Vector3f worldUp;

    // Euler angles
    private float pitch, yaw, roll;

    // Movement parameters
    private float baseSpeed = 8.0f;
    private float sprintMultiplier = 2.5f;
    private float sensitivity = 0.1f;
    private float smoothing = 0.85f;

    // Smooth movement
    private Vector3f velocity;
    private Vector3f targetVelocity;
    private float acceleration = 25.0f;
    private float friction = 15.0f;

    // Mouse input smoothing
    private boolean firstMouse = true;
    private double lastX = 400, lastY = 300;
    private float smoothMouseX = 0, smoothMouseY = 0;

    // Camera states
    private boolean isFlying = true;
    private boolean isGrounded = false;
    private float groundHeight = 0;
    private float eyeHeight = 1.8f;

    // Advanced features
    private Matrix4f viewMatrix;
    private float fov = 45.0f;
    private float aspectRatio = 16.0f / 9.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;

    // Camera shake
    private Vector3f shakeOffset;
    private float shakeIntensity = 0;
    private float shakeDuration = 0;

    // Bob effect for walking
    private float bobTimer = 0;
    private float bobIntensity = 0.02f;
    private boolean enableBob = true;

    public Camera() {
        this.position = new Vector3f(0.0f, 100.0f, 3.0f);
        this.worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.up = new Vector3f();
        this.right = new Vector3f();
        this.velocity = new Vector3f();
        this.targetVelocity = new Vector3f();
        this.shakeOffset = new Vector3f();

        this.pitch = 0.0f;
        this.yaw = -90.0f; // Start looking down negative Z
        this.roll = 0.0f;
        this.viewMatrix = new Matrix4f();

        updateCameraVectors();
    }

    public void update(float deltaTime) {
        // Update smooth movement
        updateMovement(deltaTime);

        // Update camera shake
        updateCameraShake(deltaTime);

        // Update head bob if walking
        if (enableBob && !isFlying && targetVelocity.length() > 0.1f) {
            updateHeadBob(deltaTime);
        }

        // Update vectors
        updateCameraVectors();
    }

    private void updateMovement(float deltaTime) {
        // Smooth velocity interpolation
        Vector3f velocityDiff = new Vector3f(targetVelocity).sub(velocity);

        if (velocityDiff.length() > 0.01f) {
            // Accelerate towards target velocity
            Vector3f acceleration = new Vector3f(velocityDiff).mul(this.acceleration * deltaTime);
            velocity.add(acceleration);
        } else {
            // Apply friction when no input
            velocity.mul(Math.max(0, 1.0f - friction * deltaTime));
        }

        // Apply velocity to position
        Vector3f movement = new Vector3f(velocity).mul(deltaTime);
        position.add(movement);

        // Ground collision if not flying
        if (!isFlying) {
            handleGroundCollision();
        }
    }

    private void handleGroundCollision() {
        // Simple ground collision - in a real game you'd query the terrain height
        float desiredHeight = groundHeight + eyeHeight;
        if (position.y < desiredHeight) {
            position.y = desiredHeight;
            velocity.y = Math.max(0, velocity.y); // Stop downward velocity
            isGrounded = true;
        } else {
            isGrounded = false;
        }
    }

    private void updateCameraShake(float deltaTime) {
        if (shakeDuration > 0) {
            shakeDuration -= deltaTime;

            // Generate random shake offset
            float intensity = shakeIntensity * (shakeDuration / 1.0f); // Fade out over time
            shakeOffset.x = (float) (Math.random() - 0.5) * intensity;
            shakeOffset.y = (float) (Math.random() - 0.5) * intensity;
            shakeOffset.z = (float) (Math.random() - 0.5) * intensity;
        } else {
            shakeOffset.set(0, 0, 0);
        }
    }

    private void updateHeadBob(float deltaTime) {
        bobTimer += deltaTime * targetVelocity.length() * 2.0f;
        // Bob will be applied in the view matrix calculation
    }

    public void processMouse(double xpos, double ypos) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
        }

        double xoffset = xpos - lastX;
        double yoffset = lastY - ypos; // Reversed since y-coordinates range from bottom to top
        lastX = xpos;
        lastY = ypos;

        // Apply sensitivity
        xoffset *= sensitivity;
        yoffset *= sensitivity;

        // Smooth mouse input
        smoothMouseX = smoothMouseX * smoothing + (float) xoffset * (1.0f - smoothing);
        smoothMouseY = smoothMouseY * smoothing + (float) yoffset * (1.0f - smoothing);

        yaw += smoothMouseX;
        pitch += smoothMouseY;

        // Constrain pitch
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        updateCameraVectors();
    }

    // Enhanced movement methods with smooth acceleration
    public void moveForward(float deltaTime, boolean sprinting) {
        float speed = baseSpeed * (sprinting ? sprintMultiplier : 1.0f);
        Vector3f moveDir = isFlying ? new Vector3f(front) : new Vector3f(front.x, 0, front.z).normalize();
        Vector3f acceleration = new Vector3f(moveDir).mul(speed);
        targetVelocity.add(acceleration.mul(deltaTime * 10)); // Quick response
    }

    public void moveBackward(float deltaTime, boolean sprinting) {
        float speed = baseSpeed * (sprinting ? sprintMultiplier : 1.0f);
        Vector3f moveDir = isFlying ? new Vector3f(front) : new Vector3f(front.x, 0, front.z).normalize();
        Vector3f acceleration = new Vector3f(moveDir).mul(-speed);
        targetVelocity.add(acceleration.mul(deltaTime * 10));
    }

    public void moveLeft(float deltaTime, boolean sprinting) {
        float speed = baseSpeed * (sprinting ? sprintMultiplier : 1.0f);
        Vector3f acceleration = new Vector3f(right).mul(-speed);
        targetVelocity.add(acceleration.mul(deltaTime * 10));
    }

    public void moveRight(float deltaTime, boolean sprinting) {
        float speed = baseSpeed * (sprinting ? sprintMultiplier : 1.0f);
        Vector3f acceleration = new Vector3f(right).mul(speed);
        targetVelocity.add(acceleration.mul(deltaTime * 10));
    }

    public void moveUp(float deltaTime) {
        if (isFlying) {
            Vector3f acceleration = new Vector3f(worldUp).mul(baseSpeed);
            targetVelocity.add(acceleration.mul(deltaTime * 10));
        } else {
            // Jump
            if (isGrounded) {
                velocity.y = 8.0f; // Jump velocity
            }
        }
    }

    public void moveDown(float deltaTime) {
        if (isFlying) {
            Vector3f acceleration = new Vector3f(worldUp).mul(-baseSpeed);
            targetVelocity.add(acceleration.mul(deltaTime * 10));
        }
    }

    public void stopMovement() {
        targetVelocity.set(0, targetVelocity.y, 0); // Keep Y velocity for gravity
    }

    private void updateCameraVectors() {
        // Calculate new front vector
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        newFront.y = (float) Math.sin(Math.toRadians(pitch));
        newFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

        front = newFront.normalize();

        // Calculate right and up vectors
        front.cross(worldUp, right);
        right.normalize();
        right.cross(front, up);
        up.normalize();
    }

    public void applyTransform(FloatBuffer matrixBuffer) {
        // Calculate final position with effects
        Vector3f finalPosition = new Vector3f(position);

        // Add camera shake
        finalPosition.add(shakeOffset);

        // Add head bob
        if (enableBob && !isFlying) {
            finalPosition.y += (float) Math.sin(bobTimer) * bobIntensity * targetVelocity.length();
        }

        Vector3f center = new Vector3f(finalPosition).add(front);
        viewMatrix.identity();
        viewMatrix.lookAt(finalPosition, center, up);

        matrixBuffer.clear();
        viewMatrix.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    // Camera shake effect
    public void shake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
    }

    // Zoom effect
    public void zoom(float zoomDelta) {
        fov -= zoomDelta;
        fov = Math.max(1.0f, Math.min(120.0f, fov));
    }

    // Smooth look at target
    public void lookAt(Vector3f target, float deltaTime, float speed) {
        Vector3f direction = new Vector3f(target).sub(position).normalize();

        // Calculate target yaw and pitch
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float targetPitch = (float) Math.toDegrees(Math.asin(-direction.y));

        // Smoothly interpolate to target
        float lerpSpeed = speed * deltaTime;
        yaw = lerp(yaw, targetYaw, lerpSpeed);
        pitch = lerp(pitch, targetPitch, lerpSpeed);

        updateCameraVectors();
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    // Orbit around a point
    public void orbitAround(Vector3f center, float deltaTime, float orbitSpeed) {
        Vector3f toCenter = new Vector3f(position).sub(center);
        float distance = toCenter.length();

        // Rotate around Y axis
        float angle = orbitSpeed * deltaTime;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float newX = toCenter.x * cos - toCenter.z * sin;
        float newZ = toCenter.x * sin + toCenter.z * cos;

        position.set(center.x + newX, position.y, center.z + newZ);

        // Look at center
        lookAt(center, deltaTime, 5.0f);
    }

    // Collision detection with terrain
    public void setGroundHeight(float height) {
        this.groundHeight = height;
    }

    // Camera modes
    public void setFlying(boolean flying) {
        this.isFlying = flying;
        if (!flying) {
            // Reset Y velocity when switching to ground mode
            velocity.y = 0;
            targetVelocity.y = 0;
        }
    }

    // Getters and setters
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getFront() {
        return new Vector3f(front);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public Vector3f getRight() {
        return new Vector3f(right);
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }

    public float getSensitivity() {
        return sensitivity;
    }

    public float getFov() {
        return fov;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setSpeed(float speed) {
        this.baseSpeed = speed;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    // Get view and projection matrices
    public Matrix4f getViewMatrix() {
        Vector3f finalPosition = new Vector3f(position).add(shakeOffset);
        if (enableBob && !isFlying) {
            finalPosition.y += (float) Math.sin(bobTimer) * bobIntensity * targetVelocity.length();
        }

        Vector3f center = new Vector3f(finalPosition).add(front);
        return new Matrix4f().lookAt(finalPosition, center, up);
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, nearPlane, farPlane);
    }

    // Ray casting for mouse picking
    public Vector3f getMouseRay(float mouseX, float mouseY, float screenWidth, float screenHeight) {
        // Convert mouse coordinates to normalized device coordinates
        float x = (2.0f * mouseX) / screenWidth - 1.0f;
        float y = 1.0f - (2.0f * mouseY) / screenHeight;

        // Create ray in clip space
        Vector3f rayClip = new Vector3f(x, y, -1.0f);

        // Convert to eye space
        Matrix4f projInverse = new Matrix4f(getProjectionMatrix()).invert();
        Vector3f rayEye = projInverse.transformProject(rayClip);
        rayEye.z = -1.0f;

        // Convert to world space
        Matrix4f viewInverse = new Matrix4f(getViewMatrix()).invert();
        Vector3f rayWorld = viewInverse.transformDirection(rayEye);

        return rayWorld.normalize();
    }
}
