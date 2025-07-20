package com.nak.engine.input.handlers;

import com.nak.engine.input.InputModule;

public class MouseHandler {
    private final InputModule inputModule;
    private boolean[] buttonStates = new boolean[8];
    private boolean[] previousButtonStates = new boolean[8];
    private double mouseX, mouseY;
    private double scrollX, scrollY;

    public MouseHandler(InputModule inputModule) {
        this.inputModule = inputModule;
    }

    public void handleMouseButton(int button, int action, int mods) {
        if (button >= 0 && button < buttonStates.length) {
            buttonStates[button] = (action == 1); // PRESS
        }
    }

    public void handleMouseMove(double xpos, double ypos) {
        this.mouseX = xpos;
        this.mouseY = ypos;
    }

    public void handleScroll(double xoffset, double yoffset) {
        this.scrollX = xoffset;
        this.scrollY = yoffset;
    }

    public void update(float deltaTime) {
        System.arraycopy(buttonStates, 0, previousButtonStates, 0, buttonStates.length);
        // Reset scroll each frame
        scrollX = 0;
        scrollY = 0;
    }

    public boolean isButtonPressed(int button) {
        return button >= 0 && button < buttonStates.length && buttonStates[button];
    }

    public boolean wasButtonJustPressed(int button) {
        return button >= 0 && button < buttonStates.length &&
                buttonStates[button] && !previousButtonStates[button];
    }

    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }
    public double getScrollX() { return scrollX; }
    public double getScrollY() { return scrollY; }
}
