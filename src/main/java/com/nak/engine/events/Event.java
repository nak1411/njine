package com.nak.engine.events;

public abstract class Event {
    private boolean cancelled = false;
    private final long timestamp;

    public Event() {
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public abstract String getEventName();

    @Override
    public String toString() {
        return getEventName() + "{timestamp=" + timestamp + ", cancelled=" + cancelled + "}";
    }
}