package com.nak.engine.camera.cameras;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced path camera that follows a predefined path with smooth interpolation
 * Works with the existing Camera base class structure
 */
public class PathCamera extends Camera {
    // Path data
    private final List<PathPoint> pathPoints = new ArrayList<>();
    private final List<Vector3f> positions = new ArrayList<>();
    private final List<Vector3f> targets = new ArrayList<>();

    // Animation state
    private float currentTime = 0.0f;
    private float totalTime = 10.0f;
    private float playbackSpeed = 1.0f;
    private boolean looping = true;
    private boolean playing = false;
    private boolean paused = false;

    // Interpolation settings
    private InterpolationType interpolationType = InterpolationType.SMOOTH;
    private boolean smoothOrientation = true;
    private float orientationSmoothing = 0.1f;

    // Look-at behavior
    private LookAtMode lookAtMode = LookAtMode.PATH_DIRECTION;
    private Vector3f fixedTarget = new Vector3f(0, 0, 0);
    private boolean autoCalculateUp = true;

    // Current state
    private int currentSegment = 0;
    private float segmentProgress = 0.0f;

    // Cached vectors for calculations
    private final Vector3f tempDirection = new Vector3f();
    private final Vector3f tempRight = new Vector3f();
    private final Vector3f tempUp = new Vector3f();
    private final Vector3f currentTarget = new Vector3f();

    // Animation events
    private final List<PathEventListener> eventListeners = new ArrayList<>();

    public PathCamera() {
        super();
        initializeDefaultPath();
    }

    public PathCamera(List<Vector3f> pathPositions) {
        super();
        setPath(pathPositions);
    }

    public PathCamera(List<Vector3f> pathPositions, List<Vector3f> pathTargets) {
        super();
        setPath(pathPositions, pathTargets);
    }

    private void initializeDefaultPath() {
        // Create a simple default path
        addPathPoint(new Vector3f(0, 10, 10));
        addPathPoint(new Vector3f(10, 10, 0));
        addPathPoint(new Vector3f(0, 10, -10));
        addPathPoint(new Vector3f(-10, 10, 0));
    }

    @Override
    public void update(float deltaTime) {
        if (!playing || paused || pathPoints.isEmpty()) {
            return;
        }

        // Update animation time
        currentTime += deltaTime * playbackSpeed;

        // Handle looping and completion
        if (currentTime >= totalTime) {
            if (looping) {
                currentTime = currentTime % totalTime;
                notifyEvent(PathEvent.LOOP);
            } else {
                currentTime = totalTime;
                playing = false;
                notifyEvent(PathEvent.COMPLETED);
                return;
            }
        }

        // Calculate current position and orientation
        updatePositionAlongPath();
        updateCameraVectors();
    }

    private void updatePositionAlongPath() {
        if (positions.isEmpty()) return;

        // Calculate progress along entire path (0.0 to 1.0)
        float pathProgress = currentTime / totalTime;

        // Find current segment and local progress
        float segmentCount = positions.size() - 1;
        if (segmentCount <= 0) {
            setPosition(positions.get(0));
            if (!targets.isEmpty()) {
                currentTarget.set(targets.get(0));
            }
            return;
        }

        float exactSegment = pathProgress * segmentCount;
        currentSegment = Math.min((int) exactSegment, (int) segmentCount - 1);
        segmentProgress = exactSegment - currentSegment;

        // Interpolate position
        Vector3f interpolatedPos = interpolatePosition(currentSegment, segmentProgress);
        setPosition(interpolatedPos);

        // Interpolate target if available
        if (!targets.isEmpty() && targets.size() > currentSegment + 1) {
            interpolateTarget(currentSegment, segmentProgress);
        } else {
            calculateDefaultTarget();
        }
    }

    private Vector3f interpolatePosition(int segment, float t) {
        Vector3f p0 = positions.get(segment);
        Vector3f p1 = positions.get(segment + 1);

        switch (interpolationType) {
            case LINEAR:
                return new Vector3f(p0).lerp(p1, t);

            case SMOOTH:
                // Smooth step interpolation
                float smoothT = smoothStep(t);
                return new Vector3f(p0).lerp(p1, smoothT);

            case CATMULL_ROM:
                return catmullRomInterpolation(segment, t);

            default:
                return new Vector3f(p0).lerp(p1, t);
        }
    }

    private void interpolateTarget(int segment, float t) {
        Vector3f t0 = targets.get(segment);
        Vector3f t1 = targets.get(segment + 1);

        switch (interpolationType) {
            case LINEAR:
                currentTarget.set(t0).lerp(t1, t);
                break;

            case SMOOTH:
                float smoothT = smoothStep(t);
                currentTarget.set(t0).lerp(t1, smoothT);
                break;

            case CATMULL_ROM:
                currentTarget.set(catmullRomTargetInterpolation(segment, t));
                break;

            default:
                currentTarget.set(t0).lerp(t1, t);
                break;
        }
    }

    private void calculateDefaultTarget() {
        switch (lookAtMode) {
            case PATH_DIRECTION:
                calculatePathDirectionTarget();
                break;
            case FIXED_TARGET:
                currentTarget.set(fixedTarget);
                break;
            case NEXT_POINT:
                calculateNextPointTarget();
                break;
            case SMOOTH_AHEAD:
                calculateSmoothAheadTarget();
                break;
        }
    }

    private void calculatePathDirectionTarget() {
        Vector3f currentPos = getPosition();

        // Calculate direction based on path progression
        if (currentSegment < positions.size() - 1) {
            Vector3f nextPos = positions.get(currentSegment + 1);
            Vector3f direction = new Vector3f(nextPos).sub(currentPos);
            if (direction.length() > 0.001f) {
                currentTarget.set(currentPos).add(direction.normalize().mul(10.0f));
            } else {
                currentTarget.set(currentPos).add(0, 0, -10);
            }
        } else {
            // At the end of path, look forward
            currentTarget.set(currentPos).add(0, 0, -10);
        }
    }

    private void calculateNextPointTarget() {
        if (currentSegment + 1 < positions.size()) {
            currentTarget.set(positions.get(currentSegment + 1));
        } else if (!positions.isEmpty()) {
            currentTarget.set(positions.get(0)); // Look at start if at end
        }
    }

    private void calculateSmoothAheadTarget() {
        Vector3f currentPos = getPosition();

        // Look ahead on the path for smoother camera movement
        float lookAheadTime = 2.0f; // Look 2 seconds ahead
        float futureTime = Math.min(currentTime + lookAheadTime, totalTime);
        float futureProgress = futureTime / totalTime;

        Vector3f futurePos = getPositionAtProgress(futureProgress);
        Vector3f direction = new Vector3f(futurePos).sub(currentPos);

        if (direction.length() > 0.001f) {
            currentTarget.set(currentPos).add(direction.normalize().mul(5.0f));
        } else {
            currentTarget.set(currentPos).add(tempDirection.mul(5.0f));
        }
    }

    private Vector3f getPositionAtProgress(float progress) {
        float segmentCount = positions.size() - 1;
        if (segmentCount <= 0) return positions.get(0);

        float exactSegment = progress * segmentCount;
        int segment = Math.min((int) exactSegment, (int) segmentCount - 1);
        float t = exactSegment - segment;

        return interpolatePosition(segment, t);
    }

    private Vector3f catmullRomInterpolation(int segment, float t) {
        // Get control points for Catmull-Rom spline
        Vector3f p0 = segment > 0 ? positions.get(segment - 1) : positions.get(segment);
        Vector3f p1 = positions.get(segment);
        Vector3f p2 = positions.get(segment + 1);
        Vector3f p3 = segment + 2 < positions.size() ? positions.get(segment + 2) : positions.get(segment + 1);

        return catmullRom(p0, p1, p2, p3, t);
    }

    private Vector3f catmullRomTargetInterpolation(int segment, float t) {
        if (targets.size() < 2) return currentTarget;

        Vector3f t0 = segment > 0 ? targets.get(segment - 1) : targets.get(segment);
        Vector3f t1 = targets.get(segment);
        Vector3f t2 = targets.get(Math.min(segment + 1, targets.size() - 1));
        Vector3f t3 = segment + 2 < targets.size() ? targets.get(segment + 2) : t2;

        return catmullRom(t0, t1, t2, t3, t);
    }

    private Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        Vector3f result = new Vector3f();
        result.add(new Vector3f(p1).mul(2.0f));
        result.add(new Vector3f(p2).sub(p0).mul(t));
        result.add(new Vector3f(p0).mul(2.0f).sub(new Vector3f(p1).mul(5.0f)).add(new Vector3f(p2).mul(4.0f)).sub(p3).mul(t2));
        result.add(new Vector3f(p1).mul(3.0f).sub(p0).sub(new Vector3f(p2).mul(3.0f)).add(p3).mul(t3));

        return result.mul(0.5f);
    }

    private float smoothStep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private void updateCameraVectors() {
        Vector3f currentPos = getPosition();

        // Calculate direction vector (from camera to target)
        tempDirection.set(currentTarget).sub(currentPos);

        if (tempDirection.length() < 0.001f) {
            // Default direction if target is too close
            tempDirection.set(0, 0, -1);
        } else {
            tempDirection.normalize();
        }

        // Calculate right vector
        Vector3f worldUp = new Vector3f(0, 1, 0);
        tempDirection.cross(worldUp, tempRight);

        // Handle gimbal lock
        if (tempRight.length() < 0.001f) {
            Vector3f alternateUp = Math.abs(tempDirection.y) > 0.99f ?
                    new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            tempDirection.cross(alternateUp, tempRight);
        }
        tempRight.normalize();

        // Calculate up vector
        tempRight.cross(tempDirection, tempUp).normalize();
    }

    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), currentTarget, tempUp);
    }

    // Path management methods
    public void addPathPoint(Vector3f position) {
        positions.add(new Vector3f(position));
        pathPoints.add(new PathPoint(position, null, currentTime));
    }

    public void addPathPoint(Vector3f position, Vector3f target) {
        positions.add(new Vector3f(position));
        targets.add(new Vector3f(target));
        pathPoints.add(new PathPoint(position, target, currentTime));
    }

    public void addPathPoint(Vector3f position, Vector3f target, float timeStamp) {
        positions.add(new Vector3f(position));
        if (target != null) {
            targets.add(new Vector3f(target));
        }
        pathPoints.add(new PathPoint(position, target, timeStamp));
    }

    public void insertPathPoint(int index, Vector3f position) {
        if (index >= 0 && index <= positions.size()) {
            positions.add(index, new Vector3f(position));
            pathPoints.add(index, new PathPoint(position, null, 0));
        }
    }

    public void removePathPoint(int index) {
        if (index >= 0 && index < positions.size()) {
            positions.remove(index);
            pathPoints.remove(index);
            if (index < targets.size()) {
                targets.remove(index);
            }
        }
    }

    public void clearPath() {
        positions.clear();
        targets.clear();
        pathPoints.clear();
        currentTime = 0.0f;
        currentSegment = 0;
        segmentProgress = 0.0f;
    }

    public void setPath(List<Vector3f> pathPositions) {
        clearPath();
        for (Vector3f pos : pathPositions) {
            addPathPoint(pos);
        }
    }

    public void setPath(List<Vector3f> pathPositions, List<Vector3f> pathTargets) {
        clearPath();
        for (int i = 0; i < pathPositions.size(); i++) {
            Vector3f target = i < pathTargets.size() ? pathTargets.get(i) : null;
            addPathPoint(pathPositions.get(i), target);
        }
    }

    // Animation control methods
    public void play() {
        playing = true;
        paused = false;
        notifyEvent(PathEvent.STARTED);
    }

    public void pause() {
        paused = true;
        notifyEvent(PathEvent.PAUSED);
    }

    public void resume() {
        if (paused) {
            paused = false;
            notifyEvent(PathEvent.RESUMED);
        }
    }

    public void stop() {
        playing = false;
        paused = false;
        currentTime = 0.0f;
        currentSegment = 0;
        segmentProgress = 0.0f;
        notifyEvent(PathEvent.STOPPED);
    }

    public void restart() {
        currentTime = 0.0f;
        currentSegment = 0;
        segmentProgress = 0.0f;
        play();
    }

    public void seekToTime(float time) {
        currentTime = Math.max(0, Math.min(totalTime, time));
        updatePositionAlongPath();
        notifyEvent(PathEvent.SEEKED);
    }

    public void seekToProgress(float progress) {
        seekToTime(progress * totalTime);
    }

    // Utility methods
    public Vector3f getCurrentTarget() {
        return new Vector3f(currentTarget);
    }

    public float getPathLength() {
        if (positions.size() < 2) return 0.0f;

        float totalLength = 0.0f;
        for (int i = 0; i < positions.size() - 1; i++) {
            totalLength += positions.get(i).distance(positions.get(i + 1));
        }
        return totalLength;
    }

    public float getProgress() {
        return totalTime > 0 ? currentTime / totalTime : 0.0f;
    }

    public Vector3f getPositionAtTime(float time) {
        float progress = totalTime > 0 ? time / totalTime : 0.0f;
        return getPositionAtProgress(Math.max(0, Math.min(1, progress)));
    }

    // Event system
    public void addEventListener(PathEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(PathEventListener listener) {
        eventListeners.remove(listener);
    }

    private void notifyEvent(PathEvent event) {
        for (PathEventListener listener : eventListeners) {
            listener.onPathEvent(event, currentTime, getProgress());
        }
    }

    // Override inherited vector methods
    @Override
    public Vector3f getRight() {
        return new Vector3f(tempRight);
    }

    @Override
    public Vector3f getUp() {
        return new Vector3f(tempUp);
    }

    @Override
    public Vector3f getDirection() {
        return new Vector3f(tempDirection);
    }

    // Getters and setters
    public float getTotalTime() { return totalTime; }
    public void setTotalTime(float totalTime) {
        this.totalTime = Math.max(0.1f, totalTime);
    }

    public float getPlaybackSpeed() { return playbackSpeed; }
    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = Math.max(0.1f, Math.min(10.0f, speed));
    }

    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public boolean isPlaying() { return playing; }
    public boolean isPaused() { return paused; }

    public InterpolationType getInterpolationType() { return interpolationType; }
    public void setInterpolationType(InterpolationType type) { this.interpolationType = type; }

    public LookAtMode getLookAtMode() { return lookAtMode; }
    public void setLookAtMode(LookAtMode mode) { this.lookAtMode = mode; }

    public Vector3f getFixedTarget() { return new Vector3f(fixedTarget); }
    public void setFixedTarget(Vector3f target) { this.fixedTarget.set(target); }

    public float getCurrentTime() { return currentTime; }
    public int getCurrentSegment() { return currentSegment; }
    public float getSegmentProgress() { return segmentProgress; }

    public List<Vector3f> getPositions() { return new ArrayList<>(positions); }
    public List<Vector3f> getTargets() { return new ArrayList<>(targets); }
    public int getPathPointCount() { return positions.size(); }

    // Enums
    public enum InterpolationType {
        LINEAR,      // Simple linear interpolation
        SMOOTH,      // Smooth step interpolation
        CATMULL_ROM  // Catmull-Rom spline (smooth curves)
    }

    public enum LookAtMode {
        PATH_DIRECTION, // Look in the direction of movement
        FIXED_TARGET,   // Look at a fixed point
        NEXT_POINT,     // Look at the next path point
        SMOOTH_AHEAD    // Look ahead on the path for smoother movement
    }

    public enum PathEvent {
        STARTED, PAUSED, RESUMED, STOPPED, COMPLETED, LOOP, SEEKED
    }

    // Event listener interface
    public interface PathEventListener {
        void onPathEvent(PathEvent event, float currentTime, float progress);
    }

    // Internal classes
    private static class PathPoint {
        public final Vector3f position;
        public final Vector3f target;
        public final float timeStamp;

        public PathPoint(Vector3f position, Vector3f target, float timeStamp) {
            this.position = new Vector3f(position);
            this.target = target != null ? new Vector3f(target) : null;
            this.timeStamp = timeStamp;
        }
    }

    /**
     * Get debug information about the camera state
     */
    public String getDebugInfo() {
        Vector3f pos = getPosition();
        return String.format(
                "PathCamera: Pos(%.1f,%.1f,%.1f) Progress=%.1f%% Segment=%d/%d Playing=%s Loop=%s Mode=%s",
                pos.x, pos.y, pos.z,
                getProgress() * 100,
                currentSegment, positions.size() - 1,
                playing ? "ON" : "OFF",
                looping ? "ON" : "OFF",
                lookAtMode
        );
    }
}