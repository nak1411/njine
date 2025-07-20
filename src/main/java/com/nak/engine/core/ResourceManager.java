package com.nak.engine.core;

import com.nak.engine.core.lifecycle.Cleanupable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager implements Cleanupable {
    private final Map<String, Object> resources = new ConcurrentHashMap<>();
    private final Map<String, ResourceLoader<?>> loaders = new HashMap<>();
    private final Set<String> loadingResources = ConcurrentHashMap.newKeySet();

    public ResourceManager() {
        // Register default loaders
        registerLoader("txt", new TextResourceLoader());
        registerLoader("properties", new PropertiesResourceLoader());
    }

    public <T> void registerLoader(String extension, ResourceLoader<T> loader) {
        loaders.put(extension.toLowerCase(), loader);
    }

    @SuppressWarnings("unchecked")
    public <T> T load(String resourcePath, Class<T> expectedType) {
        // Check if already loaded
        Object existing = resources.get(resourcePath);
        if (existing != null) {
            if (expectedType.isInstance(existing)) {
                return (T) existing;
            } else {
                throw new RuntimeException("Resource " + resourcePath + " is not of expected type");
            }
        }

        // Prevent circular loading
        if (loadingResources.contains(resourcePath)) {
            throw new RuntimeException("Circular resource dependency detected: " + resourcePath);
        }

        try {
            loadingResources.add(resourcePath);

            String extension = getFileExtension(resourcePath);
            ResourceLoader<?> loader = loaders.get(extension);

            if (loader == null) {
                throw new RuntimeException("No loader registered for extension: " + extension);
            }

            Object resource = loader.load(resourcePath);
            if (!expectedType.isInstance(resource)) {
                throw new RuntimeException("Loaded resource is not of expected type");
            }

            resources.put(resourcePath, resource);
            return (T) resource;

        } finally {
            loadingResources.remove(resourcePath);
        }
    }

    public void unload(String resourcePath) {
        Object resource = resources.remove(resourcePath);
        if (resource instanceof Cleanupable) {
            ((Cleanupable) resource).cleanup();
        }
    }

    public boolean isLoaded(String resourcePath) {
        return resources.containsKey(resourcePath);
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    @Override
    public void cleanup() {
        for (Map.Entry<String, Object> entry : resources.entrySet()) {
            Object resource = entry.getValue();
            if (resource instanceof Cleanupable) {
                try {
                    ((Cleanupable) resource).cleanup();
                } catch (Exception e) {
                    System.err.println("Error cleaning up resource " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }
        resources.clear();
        loaders.clear();
    }

    // Resource loader interface
    public interface ResourceLoader<T> {
        T load(String resourcePath);
    }

    // Default loaders
    private static class TextResourceLoader implements ResourceLoader<String> {
        @Override
        public String load(String resourcePath) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new RuntimeException("Resource not found: " + resourcePath);
                }
                return new String(is.readAllBytes());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load text resource: " + resourcePath, e);
            }
        }
    }

    private static class PropertiesResourceLoader implements ResourceLoader<Properties> {
        @Override
        public Properties load(String resourcePath) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new RuntimeException("Resource not found: " + resourcePath);
                }
                Properties props = new Properties();
                props.load(is);
                return props;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load properties resource: " + resourcePath, e);
            }
        }
    }
}