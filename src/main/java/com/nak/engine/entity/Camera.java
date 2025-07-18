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

    private float pitch, yaw;
    private float speed = 5.0f;
    private float sensitivity = 0.1f;

    private boolean firstMouse = true;
    private double lastX = 400, lastY = 300;

    private Matrix4f viewMatrix;

    public Camera() {
        this.position = new Vector3f(0.0f, 0.0f, 3.0f);
        this.worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.up = new Vector3f();
        this.right = new Vector3f();
        this.pitch = 0.0f;
        this.yaw = -90.0f; // Start looking down negative Z
        this.viewMatrix = new Matrix4f();

        updateCameraVectors();
    }

    public void update(float deltaTime) {
        // Camera updates happen in input processing
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

        xoffset *= sensitivity;
        yoffset *= sensitivity;

        yaw += xoffset;
        pitch += yoffset;

        // Constrain pitch
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        updateCameraVectors();
    }

    public void moveForward(float deltaTime) {
        Vector3f velocity = new Vector3f(front).mul(speed * deltaTime);
        position.add(velocity);
    }

    public void moveBackward(float deltaTime) {
        Vector3f velocity = new Vector3f(front).mul(speed * deltaTime);
        position.sub(velocity);
    }

    public void moveLeft(float deltaTime) {
        Vector3f velocity = new Vector3f(right).mul(speed * deltaTime);
        position.sub(velocity);
    }

    public void moveRight(float deltaTime) {
        Vector3f velocity = new Vector3f(right).mul(speed * deltaTime);
        position.add(velocity);
    }

    public void moveUp(float deltaTime) {
        Vector3f velocity = new Vector3f(up).mul(speed * deltaTime);
        position.add(velocity);
    }

    public void moveDown(float deltaTime) {
        Vector3f velocity = new Vector3f(up).mul(speed * deltaTime);
        position.sub(velocity);
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
        Vector3f center = new Vector3f(position).add(front);
        viewMatrix.identity();
        viewMatrix.lookAt(position, center, up);

        matrixBuffer.clear();
        viewMatrix.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getFront() { return new Vector3f(front); }
    public Vector3f getUp() { return new Vector3f(up); }
    public Vector3f getRight() { return new Vector3f(right); }
}
