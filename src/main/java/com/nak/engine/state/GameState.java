package com.nak.engine.state;

public class GameState {
    private float time = 0.0f;

    public void update(float deltaTime) {
        time += deltaTime;
    }

    public float getTime() {
        return time;
    }
}