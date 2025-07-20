package com.nak.engine.events;

/**
 * Base class for all events in the engine
 */
public abstract class Event {
    private boolean cancelled = false;
    private long timestamp = System.currentTimeMillis();

    /**
     * Get the name of this event
     */
    public abstract String getEventName();

    /**
     * Check if this event has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancel this event (if cancellable)
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Get the timestamp when this event was created
     */
    public long getTimestamp() {
        return timestamp;
    }
}