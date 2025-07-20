package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class OrbitCamera extends Camera {
    private Vector3f target = new Vector3f();
    private float distance = 10.0f;
    private float azimuth = 0.0f;
    private float elevation = 0.0f;

    @Override
    public void update(float deltaTime) {
        updatePosition();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, target, up);
    }

    private void updatePosition() {
        float x = (float) (distance * Math.cos(elevation) * Math.cos(azimuth));
        float y = (float) (distance * Math.sin(elevation));
        float z = (float) (distance * Math.cos(elevation) * Math.sin(azimuth));

        position.set(target).add(x, y, z);
        direction = new Vector3f(target).sub(position).normalize();
    }

    public void orbit(float deltaAzimuth, float deltaElevation) {
        azimuth += deltaAzimuth;
        elevation += deltaElevation;

        // Constrain elevation
        elevation = Math.max(-89.0f, Math.min(89.0f, elevation));
    }

    public void zoom(float deltaDistance) {
        distance += deltaDistance;
        distance = Math.max(1.0f, Math.min(100.0f, distance));
    }

    // Getters and setters
    public Vector3f getTarget() { return new Vector3f(target); }
    public void setTarget(Vector3f target) { this.target.set(target); }

    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
}
