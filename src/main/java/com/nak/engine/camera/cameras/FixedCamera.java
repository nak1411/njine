package com.nak.engine.camera.cameras;

import com.nak.engine.camera.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FixedCamera extends Camera {
    private Vector3f target = new Vector3f();

    public FixedCamera() {
        super();
    }

    public FixedCamera(Vector3f position, Vector3f target) {
        this.position.set(position);
        this.target.set(target);
        updateDirection();
    }

    @Override
    public void update(float deltaTime) {
        updateDirection();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, target, up);
    }

    private void updateDirection() {
        direction = new Vector3f(target).sub(position).normalize();
        direction.cross(new Vector3f(0, 1, 0), right);
        right.normalize();
        right.cross(direction, up);
        up.normalize();
    }

    public Vector3f getTarget() { return new Vector3f(target); }
    public void setTarget(Vector3f target) {
        this.target.set(target);
        updateDirection();
    }
}
