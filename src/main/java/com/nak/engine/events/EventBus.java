package com.nak.engine.events;

import com.nak.engine.core.lifecycle.Cleanupable;
import com.nak.engine.events.annotations.EventHandler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus implements Cleanupable {
    private final Map<Class<? extends Event>, List<EventListenerWrapper>> listeners = new ConcurrentHashMap<>();
    private final Queue<Event> eventQueue = new LinkedList<>();
    private final Set<Object> registeredObjects = ConcurrentHashMap.newKeySet();

    private boolean processingEvents = false;
    private int maxEventQueueSize = 1000;

    public void register(Object listener) {
        if (registeredObjects.contains(listener)) {
            return; // Already registered
        }

        registeredObjects.add(listener);

        // Find all methods with @EventHandler annotation
        Class<?> clazz = listener.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                registerEventHandler(listener, method);
            }
        }
    }

    private void registerEventHandler(Object listener, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            throw new IllegalArgumentException("Event handler methods must have exactly one parameter");
        }

        Class<?> eventType = paramTypes[0];
        if (!Event.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException("Event handler parameter must extend Event class");
        }

        @SuppressWarnings("unchecked")
        Class<? extends Event> eventClass = (Class<? extends Event>) eventType;

        EventHandler annotation = method.getAnnotation(EventHandler.class);
        EventListenerWrapper wrapper = new EventListenerWrapper(listener, method, annotation.priority());

        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(wrapper);

        // Sort by priority (higher priority first)
        listeners.get(eventClass).sort((a, b) -> Integer.compare(b.priority, a.priority));

        method.setAccessible(true);
    }

    public void unregister(Object listener) {
        if (!registeredObjects.remove(listener)) {
            return; // Not registered
        }

        for (List<EventListenerWrapper> wrapperList : listeners.values()) {
            wrapperList.removeIf(wrapper -> wrapper.listener == listener);
        }
    }

    public void post(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        synchronized (eventQueue) {
            if (eventQueue.size() >= maxEventQueueSize) {
                System.err.println("Event queue full, dropping event: " + event.getClass().getSimpleName());
                return;
            }
            eventQueue.offer(event);
        }
    }

    public void postImmediate(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        deliverEvent(event);
    }

    public void processEvents() {
        if (processingEvents) {
            return; // Prevent recursive processing
        }

        processingEvents = true;
        try {
            Event event;
            while ((event = pollEvent()) != null) {
                deliverEvent(event);
            }
        } finally {
            processingEvents = false;
        }
    }

    private Event pollEvent() {
        synchronized (eventQueue) {
            return eventQueue.poll();
        }
    }

    private void deliverEvent(Event event) {
        Class<? extends Event> eventType = event.getClass();

        // Deliver to direct listeners
        List<EventListenerWrapper> directListeners = listeners.get(eventType);
        if (directListeners != null) {
            for (EventListenerWrapper wrapper : directListeners) {
                try {
                    wrapper.method.invoke(wrapper.listener, event);

                    if (event.isCancelled()) {
                        break; // Stop processing if event is cancelled
                    }
                } catch (Exception e) {
                    System.err.println("Error delivering event " + eventType.getSimpleName() +
                            " to " + wrapper.listener.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // Deliver to parent class listeners (inheritance support)
        Class<?> superClass = eventType.getSuperclass();
        while (superClass != null && Event.class.isAssignableFrom(superClass)) {
            @SuppressWarnings("unchecked")
            Class<? extends Event> superEventType = (Class<? extends Event>) superClass;

            List<EventListenerWrapper> parentListeners = listeners.get(superEventType);
            if (parentListeners != null) {
                for (EventListenerWrapper wrapper : parentListeners) {
                    try {
                        wrapper.method.invoke(wrapper.listener, event);

                        if (event.isCancelled()) {
                            return; // Stop all processing
                        }
                    } catch (Exception e) {
                        System.err.println("Error delivering event to parent listener: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            superClass = superClass.getSuperclass();
        }
    }

    public int getQueueSize() {
        synchronized (eventQueue) {
            return eventQueue.size();
        }
    }

    public void setMaxEventQueueSize(int maxSize) {
        this.maxEventQueueSize = Math.max(1, maxSize);
    }

    public void clearQueue() {
        synchronized (eventQueue) {
            eventQueue.clear();
        }
    }

    @Override
    public void cleanup() {
        clearQueue();
        listeners.clear();
        registeredObjects.clear();
    }

    // Inner class for wrapping event listeners
    private static class EventListenerWrapper {
        final Object listener;
        final Method method;
        final int priority;

        EventListenerWrapper(Object listener, Method method, int priority) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
        }
    }
}