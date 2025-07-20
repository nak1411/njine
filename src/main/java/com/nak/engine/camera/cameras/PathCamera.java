package com.nak.engine.camera.cameras;

import com.nak.engine.camera.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class PathCamera extends Camera {
    private final List<Vector3f> pathPoints = new ArrayList<>();
    private float currentTime = 0.0f;
    private float totalTime = 10.0f;
    private boolean looping = true;
    private boolean playing = false;

    public void addPathPoint(Vector3f point) {
        pathPoints.add(new Vector3f(point));
    }

    public void clearPath() {
        pathPoints.clear();
    }

    public void play() {
        playing = true;
        currentTime = 0.0f;
    }

    public void stop() {
        playing = false;
        currentTime = 0.0f;
    }

    public void pause() {
        playing = false;
    }

    @Override
    public void update(float deltaTime) {
        if (!playing || pathPoints.size() < 2) return;

        currentTime += deltaTime;

        if (currentTime >= totalTime) {
            if (looping) {
                currentTime = 0.0f;
            } else {
                playing = false;
                return;
            }
        }

        float t = currentTime / totalTime;
        interpolatePosition(t);
    }

    @Override
    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(direction);
        return new Matrix4f().lookAt(position, center, up);
    }

    private void interpolatePosition(float t) {
        if (pathPoints.size() < 2) return;

        float segmentLength = 1.0f / (pathPoints.size() - 1);
        int segment = Math.min((int)(t / segmentLength), pathPoints.size() - 2);
        float localT = (t - segment * segmentLength) / segmentLength;

        Vector3f start = pathPoints.get(segment);
        Vector3f end = pathPoints.get(segment + 1);

        position.set(start).lerp(end, localT);

        // Update direction to look toward next point
        if (segment + 1 < pathPoints.size() - 1) {
            direction = new Vector3f(pathPoints.get(segment + 1)).sub(position).normalize();
        }
    }

    // Getters and setters
    public float getTotalTime() { return totalTime; }
    public void setTotalTime(float totalTime) { this.totalTime = totalTime; }

    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public boolean isPlaying() { return playing; }
}
