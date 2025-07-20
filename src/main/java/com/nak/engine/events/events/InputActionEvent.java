package com.nak.engine.events.events;

import com.nak.engine.events.Event;
import com.nak.engine.input.InputAction;

public class InputActionEvent extends Event {
    public enum Type {
        PRESSED, RELEASED, HELD
    }

    private final InputAction action;
    private final Type type;
    private final int modifiers;

    public InputActionEvent(InputAction action, Type type, int modifiers) {
        this.action = action;
        this.type = type;
        this.modifiers = modifiers;
    }

    @Override
    public String getEventName() {
        return "InputAction";
    }

    public InputAction getAction() {
        return action;
    }

    public Type getType() {
        return type;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean hasModifier(int modifier) {
        return (modifiers & modifier) != 0;
    }
}