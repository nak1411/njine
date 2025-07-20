package com.nak.engine.events.events;

import com.nak.engine.events.Event;

/**
 * Event fired when the OpenGL context is ready for use.
 * This allows modules to initialize their OpenGL resources at the right time.
 */
public class OpenGLContextReadyEvent extends Event {
    private final long windowHandle;
    private final String glVersion;
    private final String glRenderer;
    private final String glVendor;
    private final int windowWidth;
    private final int windowHeight;

    public OpenGLContextReadyEvent(long windowHandle, String glVersion, String glRenderer,
                                   String glVendor, int windowWidth, int windowHeight) {
        this.windowHandle = windowHandle;
        this.glVersion = glVersion;
        this.glRenderer = glRenderer;
        this.glVendor = glVendor;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public String getGlVersion() {
        return glVersion;
    }

    public String getGlRenderer() {
        return glRenderer;
    }

    public String getGlVendor() {
        return glVendor;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    @Override
    public String getEventName() {
        return "OpenGLContextReady";
    }

    @Override
    public String toString() {
        return String.format("OpenGLContextReadyEvent{windowHandle=%d, version='%s', renderer='%s', vendor='%s', size=%dx%d}",
                windowHandle, glVersion, glRenderer, glVendor, windowWidth, windowHeight);
    }
}