package com.nak.engine.shader;

import java.util.HashMap;
import java.util.Map;

public class ShaderCache {
    private final Map<String, ShaderProgram> cache = new HashMap<>();
    private final int maxSize;

    public ShaderCache() {
        this.maxSize = 50;
    }

    public ShaderCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public void put(String name, ShaderProgram program) {
        if (cache.size() >= maxSize) {
            // Remove oldest entry (simple LRU)
            String oldestKey = cache.keySet().iterator().next();
            ShaderProgram old = cache.remove(oldestKey);
            if (old != null) {
                old.cleanup();
            }
        }

        cache.put(name, program);
    }

    public ShaderProgram get(String name) {
        return cache.get(name);
    }

    public boolean contains(String name) {
        return cache.containsKey(name);
    }

    public void remove(String name) {
        ShaderProgram program = cache.remove(name);
        if (program != null) {
            program.cleanup();
        }
    }

    public void cleanup() {
        for (ShaderProgram program : cache.values()) {
            program.cleanup();
        }
        cache.clear();
    }
}