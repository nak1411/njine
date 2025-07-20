package com.nak.engine.events.events;

import com.nak.engine.events.Event;

public class WindowResizeEvent extends Event {
    private final int oldWidth;
    private final int oldHeight;
    private final int newWidth;
    private final int newHeight;

    public WindowResizeEvent(int oldWidth, int oldHeight, int newWidth, int newHeight) {
        this.oldWidth = oldWidth;
        this.oldHeight = oldHeight;
        this.newWidth = newWidth;
        this.newHeight = newHeight;
    }

    @Override
    public String getEventName() {
        return "WindowResize";
    }

    public int getOldWidth() {
        return oldWidth;
    }

    public int getOldHeight() {
        return oldHeight;
    }

    public int getNewWidth() {
        return newWidth;
    }

    public int getNewHeight() {
        return newHeight;
    }

    public float getOldAspectRatio() {
        return (float) oldWidth / oldHeight;
    }

    public float getNewAspectRatio() {
        return (float) newWidth / newHeight;
    }
}
