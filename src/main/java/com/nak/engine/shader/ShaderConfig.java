package com.nak.engine.shader;

import java.io.*;
import java.util.Properties;

public class ShaderConfig {
    private static final String DEFAULT_CONFIG_FILE = "shader_config.properties";

    private final Properties properties = new Properties();
    private String configFile;

    public ShaderConfig() {
        this(DEFAULT_CONFIG_FILE);
    }

    public ShaderConfig(String configFile) {
        this.configFile = configFile;
        loadDefaults();
        loadFromFile();
    }

    private void loadDefaults() {
        // Shader directories
        properties.setProperty("shader.directory", "src/main/resources/shaders");
        properties.setProperty("shader.backup.directory", "assets/shaders");

        // Hot reload settings
        properties.setProperty("shader.hotreload.enabled", "false");
        properties.setProperty("shader.hotreload.development", "true");

        // Validation settings
        properties.setProperty("shader.validation.strict", "true");
        properties.setProperty("shader.validation.warnings", "true");

        // Performance settings
        properties.setProperty("shader.cache.enabled", "true");
        properties.setProperty("shader.cache.size", "50");

        // Debug settings
        properties.setProperty("shader.debug.enabled", "false");
        properties.setProperty("shader.debug.print.uniforms", "false");
        properties.setProperty("shader.debug.log.reloads", "true");

        // Default shader programs
        properties.setProperty("shader.programs.required", "terrain,basic,skybox,ui");
        properties.setProperty("shader.programs.optional", "water,particle,shadow,post");
    }

    private void loadFromFile() {
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            System.out.println("Loaded shader configuration from: " + configFile);
        } catch (IOException e) {
            System.out.println("Could not load shader config file, using defaults: " + e.getMessage());
            createDefaultConfigFile();
        }
    }

    private void createDefaultConfigFile() {
        try (OutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "Shader Manager Configuration");
            System.out.println("Created default shader configuration file: " + configFile);
        } catch (IOException e) {
            System.err.println("Failed to create default config file: " + e.getMessage());
        }
    }

    public void saveToFile() {
        try (OutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "Shader Manager Configuration - Updated");
            System.out.println("Saved shader configuration to: " + configFile);
        } catch (IOException e) {
            System.err.println("Failed to save config file: " + e.getMessage());
        }
    }

    // Getter methods
    public String getShaderDirectory() {
        return properties.getProperty("shader.directory");
    }

    public String getBackupShaderDirectory() {
        return properties.getProperty("shader.backup.directory");
    }

    public boolean isHotReloadEnabled() {
        boolean development = Boolean.parseBoolean(System.getProperty("development", "false"));
        boolean configEnabled = Boolean.parseBoolean(properties.getProperty("shader.hotreload.enabled"));
        boolean devEnabled = Boolean.parseBoolean(properties.getProperty("shader.hotreload.development"));

        return configEnabled || (development && devEnabled);
    }

    public boolean isStrictValidation() {
        return Boolean.parseBoolean(properties.getProperty("shader.validation.strict"));
    }

    public boolean showValidationWarnings() {
        return Boolean.parseBoolean(properties.getProperty("shader.validation.warnings"));
    }

    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(properties.getProperty("shader.cache.enabled"));
    }

    public int getCacheSize() {
        return Integer.parseInt(properties.getProperty("shader.cache.size"));
    }

    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(properties.getProperty("shader.debug.enabled"));
    }

    public boolean printUniforms() {
        return Boolean.parseBoolean(properties.getProperty("shader.debug.print.uniforms"));
    }

    public boolean logReloads() {
        return Boolean.parseBoolean(properties.getProperty("shader.debug.log.reloads"));
    }

    public String[] getRequiredPrograms() {
        return properties.getProperty("shader.programs.required").split(",");
    }

    public String[] getOptionalPrograms() {
        return properties.getProperty("shader.programs.optional").split(",");
    }

    // Setter methods
    public void setShaderDirectory(String directory) {
        properties.setProperty("shader.directory", directory);
    }

    public void setHotReloadEnabled(boolean enabled) {
        properties.setProperty("shader.hotreload.enabled", String.valueOf(enabled));
    }

    public void setDebugEnabled(boolean enabled) {
        properties.setProperty("shader.debug.enabled", String.valueOf(enabled));
    }
}
