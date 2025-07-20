package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FreeCamera extends Camera {
    private float pitch = 0.0f;
    private float yaw = -90.0f;
    private float sensitivity = 0.1f;
    private float speed = 8.0f;

    @Override
    public void update(float deltaTime) {
        updateCameraVectors();
    }

    @Override
    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(direction);
        return new Matrix4f().lookAt(position, center, up);
    }

    private void updateCameraVectors() {
        Vector3f front = new Vector3f();
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

        direction = front.normalize();
        direction.cross(new Vector3f(0, 1, 0), right);
        right.normalize();
        right.cross(direction, up);
        up.normalize();
    }

    public void processMouseMovement(float xOffset, float yOffset) {
        xOffset *= sensitivity;
        yOffset *= sensitivity;

        yaw += xOffset;
        pitch += yOffset;

        // Constrain pitch
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
    }

    public void processKeyboard(Direction direction, float deltaTime) {
        float velocity = speed * deltaTime;

        switch (direction) {
            case FORWARD -> position.add(new Vector3f(this.direction).mul(velocity));
            case BACKWARD -> position.sub(new Vector3f(this.direction).mul(velocity));
            case LEFT -> position.sub(new Vector3f(right).mul(velocity));
            case RIGHT -> position.add(new Vector3f(right).mul(velocity));
            case UP -> position.add(new Vector3f(up).mul(velocity));
            case DOWN -> position.sub(new Vector3f(up).mul(velocity));
        }
    }

    public enum Direction {
        FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN
    }

    // Getters and setters
    public float getSensitivity() { return sensitivity; }
    public void setSensitivity(float sensitivity) { this.sensitivity = sensitivity; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
}
