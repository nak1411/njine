package com.nak.engine.events.events;

import com.nak.engine.events.Event;
import org.joml.Vector3f;

public class CameraMovedEvent extends Event {
    private final Vector3f oldPosition;
    private final Vector3f newPosition;
    private final Vector3f velocity;

    public CameraMovedEvent(Vector3f oldPosition, Vector3f newPosition, Vector3f velocity) {
        this.oldPosition = new Vector3f(oldPosition);
        this.newPosition = new Vector3f(newPosition);
        this.velocity = new Vector3f(velocity);
    }

    @Override
    public String getEventName() {
        return "CameraMoved";
    }

    public Vector3f getOldPosition() {
        return new Vector3f(oldPosition);
    }

    public Vector3f getNewPosition() {
        return new Vector3f(newPosition);
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public float getDistanceMoved() {
        return oldPosition.distance(newPosition);
    }
}