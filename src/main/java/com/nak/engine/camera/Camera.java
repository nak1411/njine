package com.nak.engine.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class Camera {
    protected Vector3f position = new Vector3f();
    protected Vector3f direction = new Vector3f(0, 0, -1);
    protected Vector3f up = new Vector3f(0, 1, 0);
    protected Vector3f right = new Vector3f(1, 0, 0);

    protected float fov = 45.0f;
    protected float aspectRatio = 16.0f / 9.0f;
    protected float nearPlane = 0.1f;
    protected float farPlane = 1000.0f;

    public abstract void update(float deltaTime);
    public abstract Matrix4f getViewMatrix();

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane
        );
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
}
