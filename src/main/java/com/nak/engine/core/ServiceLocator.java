package com.nak.engine.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceLocator {
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<String, Object> namedServices = new ConcurrentHashMap<>();

    public <T> void register(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
    }

    public <T> void register(String name, T implementation) {
        namedServices.put(name, implementation);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException("Service not found: " + serviceClass.getSimpleName());
        }
        return service;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<T> expectedClass) {
        Object service = namedServices.get(name);
        if (service == null) {
            throw new ServiceNotFoundException("Named service not found: " + name);
        }

        if (!expectedClass.isInstance(service)) {
            throw new ServiceNotFoundException("Service " + name + " is not of expected type: " + expectedClass.getSimpleName());
        }

        return (T) service;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOptional(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    public boolean hasService(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    public boolean hasService(String name) {
        return namedServices.containsKey(name);
    }

    public void unregister(Class<?> serviceClass) {
        services.remove(serviceClass);
    }

    public void unregister(String name) {
        namedServices.remove(name);
    }

    public void clear() {
        services.clear();
        namedServices.clear();
    }

    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }
}