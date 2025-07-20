package com.nak.engine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderContext {
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projectionMatrix = new Matrix4f();
    private Vector3f cameraPosition = new Vector3f();
    private int viewportWidth = 1920;
    private int viewportHeight = 1080;
    private float time = 0.0f;

    public void update(float deltaTime) {
        time += deltaTime;
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    // Getters and setters
    public Matrix4f getViewMatrix() { return viewMatrix; }
    public void setViewMatrix(Matrix4f viewMatrix) { this.viewMatrix.set(viewMatrix); }

    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    public void setProjectionMatrix(Matrix4f projectionMatrix) { this.projectionMatrix.set(projectionMatrix); }

    public Vector3f getCameraPosition() { return cameraPosition; }
    public void setCameraPosition(Vector3f cameraPosition) { this.cameraPosition.set(cameraPosition); }

    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }

    public float getTime() { return time; }
}
