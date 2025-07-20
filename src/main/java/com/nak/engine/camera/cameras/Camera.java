package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class Camera {
    // Camera state
    private Vector3f position = new Vector3f(0, 10, 3);
    private Vector3f direction = new Vector3f(0, 0, -1);
    private Vector3f up = new Vector3f(0, 1, 0);
    private Vector3f right = new Vector3f(1, 0, 0);

    // Camera parameters
    private float fov = 45.0f;
    private float aspectRatio = 16.0f / 9.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 1000.0f;

    // Mouse control
    private float pitch = 0.0f;
    private float yaw = -90.0f;
    private float sensitivity = 0.1f;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean firstMouse = true;

    // Movement
    private boolean flying = true;
    private float speed = 8.0f;

    public void update(float deltaTime) {
        updateCameraVectors();
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

    public void processMouse(double xPos, double yPos) {
        if (firstMouse) {
            lastMouseX = xPos;
            lastMouseY = yPos;
            firstMouse = false;
        }

        float xOffset = (float) (xPos - lastMouseX);
        float yOffset = (float) (lastMouseY - yPos); // Reversed
        lastMouseX = xPos;
        lastMouseY = yPos;

        processMouseMovement(xOffset, yOffset);
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

    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(direction);
        return new Matrix4f().lookAt(position, center, up);
    }

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane
        );
    }

    public void applyTransform(FloatBuffer matrixBuffer) {
        Matrix4f viewMatrix = getViewMatrix();
        matrixBuffer.clear();
        viewMatrix.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public void setPosition(Vector3f position) { this.position.set(position); }

    public Vector3f getDirection() { return new Vector3f(direction); }
    public Vector3f getUp() { return new Vector3f(up); }
    public Vector3f getRight() { return new Vector3f(right); }

    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }

    public void setAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }
    public float getAspectRatio() { return aspectRatio; }

    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public float getSensitivity() { return sensitivity; }
    public void setSensitivity(float sensitivity) { this.sensitivity = sensitivity; }
}
