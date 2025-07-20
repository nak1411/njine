package com.nak.engine.events.events;

import com.nak.engine.events.Event;

public class EngineStateEvent extends Event {
    public enum State {
        INITIALIZING, INITIALIZED, RUNNING, STOPPING, STOPPED, ERROR
    }

    private final State state;
    private final String message;

    public EngineStateEvent(State state, String message) {
        this.state = state;
        this.message = message;
    }

    @Override
    public String getEventName() {
        return "EngineState";
    }

    public State getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }
}
