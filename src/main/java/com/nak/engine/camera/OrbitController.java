package com.nak.engine.camera;

import com.nak.engine.camera.cameras.OrbitCamera;
import org.joml.Vector3f;

public class OrbitController {
    private OrbitCamera camera;
    private float orbitSensitivity = 0.5f;
    private float zoomSensitivity = 1.0f;
    private boolean orbiting = false;
    private boolean zooming = false;

    public void setCamera(OrbitCamera camera) {
        this.camera = camera;
    }

    public void startOrbit() {
        orbiting = true;
    }

    public void stopOrbit() {
        orbiting = false;
    }

    public void startZoom() {
        zooming = true;
    }

    public void stopZoom() {
        zooming = false;
    }

    public void processMouseMovement(float xOffset, float yOffset) {
        if (camera == null) return;

        if (orbiting) {
            camera.orbit(
                    xOffset * orbitSensitivity,
                    yOffset * orbitSensitivity
            );
        }
    }

    public void processScroll(float scrollOffset) {
        if (camera == null) return;

        camera.zoom(scrollOffset * zoomSensitivity);
    }

    public void setTarget(Vector3f target) {
        if (camera != null) {
            camera.setTarget(target);
        }
    }

    // Getters and setters
    public float getOrbitSensitivity() { return orbitSensitivity; }
    public void setOrbitSensitivity(float sensitivity) { this.orbitSensitivity = sensitivity; }

    public float getZoomSensitivity() { return zoomSensitivity; }
    public void setZoomSensitivity(float sensitivity) { this.zoomSensitivity = sensitivity; }
}