package com.nak.engine.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String DEFAULT_CONFIG_FILE = "engine.properties";

    private static Path configDirectory;
    private static final Map<Class<?>, Object> configCache = new HashMap<>();

    static {
        configDirectory = Paths.get(DEFAULT_CONFIG_DIR);
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
    }

    public static EngineConfig loadDefault() {
        return load(DEFAULT_CONFIG_FILE, EngineConfig.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T load(String filename, Class<T> configClass) {
        // Check cache first
        T cached = (T) configCache.get(configClass);
        if (cached != null) {
            return cached;
        }

        Path configFile = configDirectory.resolve(filename);

        try {
            T config;
            if (Files.exists(configFile)) {
                config = loadFromFile(configFile, configClass);
            } else {
                // Create default config
                config = createDefaultConfig(configClass);
                save(filename, config);
            }

            // Validate config
            if (config instanceof Validatable) {
                ((Validatable) config).validate();
            }

            configCache.put(configClass, config);
            return config;

        } catch (Exception e) {
            System.err.println("Failed to load config " + filename + ": " + e.getMessage());
            // Return default config as fallback
            T defaultConfig = createDefaultConfig(configClass);
            configCache.put(configClass, defaultConfig);
            return defaultConfig;
        }
    }

    public static <T> void save(String filename, T config) {
        Path configFile = configDirectory.resolve(filename);

        try {
            saveToFile(configFile, config);
            configCache.put(config.getClass(), config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config " + filename, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadFromFile(Path file, Class<T> configClass) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(file)) {
            props.load(is);
        }

        return (T) ConfigReflectionUtils.createFromProperties(configClass, props);
    }

    private static <T> void saveToFile(Path file, T config) throws IOException {
        Properties props = ConfigReflectionUtils.convertToProperties(config);

        try (OutputStream os = Files.newOutputStream(file)) {
            props.store(os, "Auto-generated configuration file");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createDefaultConfig(Class<T> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create default config for " + configClass.getSimpleName(), e);
        }
    }

    public static void setConfigDirectory(Path directory) {
        configDirectory = directory;
        configCache.clear(); // Clear cache when directory changes
    }

    public static Path getConfigDirectory() {
        return configDirectory;
    }

    public static void clearCache() {
        configCache.clear();
    }

    public static <T> void invalidateCache(Class<T> configClass) {
        configCache.remove(configClass);
    }
}
