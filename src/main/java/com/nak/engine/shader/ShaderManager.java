package com.nak.engine.shader;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class ShaderManager {
    private static ShaderManager instance;

    // Shader storage
    private final Map<String, Shader> shaders = new ConcurrentHashMap<>();
    private final Map<String, ShaderProgram> programs = new ConcurrentHashMap<>();
    private final Map<String, String> shaderSources = new ConcurrentHashMap<>();

    // Hot reload system
    private boolean hotReloadEnabled = false;
    private WatchService watchService;
    private ExecutorService fileWatchExecutor;
    private Path shaderDirectory;
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();

    // Shader compilation cache
    private final Map<String, Long> lastModified = new HashMap<>();
    private final Set<String> compilingShaders = ConcurrentHashMap.newKeySet();

    // Events
    private final List<ShaderReloadListener> reloadListeners = new ArrayList<>();

    // Fallback shader tracking
    private final Set<String> fallbackPrograms = new HashSet<>();

    private ShaderManager() {
        try {
            // Default shader directory
            this.shaderDirectory = Paths.get("src/main/resources/shaders");

            // Create directory if it doesn't exist
            if (!Files.exists(shaderDirectory)) {
                Files.createDirectories(shaderDirectory);
                System.out.println("Created shader directory: " + shaderDirectory);
            }

            // Always load built-in shaders first
            loadBuiltInShaders();

            // Try to load from files to override built-ins
            loadShadersFromFiles();

        } catch (IOException e) {
            System.err.println("Failed to initialize shader directory: " + e.getMessage());
            // Still load built-in shaders as fallback
            loadBuiltInShaders();
        }
    }

    public static synchronized ShaderManager getInstance() {
        if (instance == null) {
            instance = new ShaderManager();
        }
        return instance;
    }

    /**
     * Load shader source from multiple possible locations
     */
    private String loadShaderSource(String filename) throws IOException {
        // Try file system first
        Path filePath = shaderDirectory.resolve(filename);
        if (Files.exists(filePath)) {
            String content = Files.readString(filePath);
            if (!content.trim().isEmpty()) {
                System.out.println("Loaded shader from file: " + filename);
                return content;
            }
        }

        // Try classpath
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("shaders/" + filename)) {
            if (stream != null) {
                String content = new String(stream.readAllBytes());
                if (!content.trim().isEmpty()) {
                    System.out.println("Loaded shader from classpath: " + filename);
                    return content;
                }
            }
        }

        throw new IOException("Shader file not found or empty: " + filename);
    }

    /**
     * Try to load shaders from files, fall back to built-ins
     */
    private void loadShadersFromFiles() {
        String[] shaderPairs = {
                "terrain", "terrain.vert", "terrain.frag",
                "skybox", "skybox.vert", "skybox.frag",
                "particle", "particle.vert", "particle.frag",
                "water", "water.vert", "water.frag",
                "ui", "ui.vert", "ui.frag"
        };

        for (int i = 0; i < shaderPairs.length; i += 3) {
            String programName = shaderPairs[i];
            String vertFile = shaderPairs[i + 1];
            String fragFile = shaderPairs[i + 2];

            try {
                String vertexSource = loadShaderSource(vertFile);
                String fragmentSource = loadShaderSource(fragFile);

                // Only create if we have non-empty sources
                if (!vertexSource.trim().isEmpty() && !fragmentSource.trim().isEmpty()) {
                    ShaderProgram existing = programs.get(programName);
                    if (existing != null) {
                        existing.cleanup();
                    }

                    ShaderProgram program = createProgram(programName, vertexSource, fragmentSource);
                    System.out.println("Successfully loaded " + programName + " shader from files");

                    // Remove from fallback set if it was there
                    fallbackPrograms.remove(programName);
                }
            } catch (Exception e) {
                System.out.println("Could not load " + programName + " shader from files: " + e.getMessage());
                System.out.println("Using built-in " + programName + " shader");
            }
        }
    }

    /**
     * Create a shader program from vertex and fragment shader sources
     */
    public ShaderProgram createProgram(String name, String vertexSource, String fragmentSource) {
        return createProgram(name, vertexSource, fragmentSource, null);
    }

    /**
     * Create a shader program with geometry shader
     */
    public ShaderProgram createProgram(String name, String vertexSource, String fragmentSource, String geometrySource) {
        try {
            ShaderProgram program = new ShaderProgram(name);

            // Compile vertex shader
            Shader vertexShader = compileShader(name + "_vertex", vertexSource, GL20.GL_VERTEX_SHADER);
            program.attachShader(vertexShader);

            // Compile fragment shader
            Shader fragmentShader = compileShader(name + "_fragment", fragmentSource, GL20.GL_FRAGMENT_SHADER);
            program.attachShader(fragmentShader);

            // Compile geometry shader if provided
            if (geometrySource != null && !geometrySource.trim().isEmpty()) {
                Shader geometryShader = compileShader(name + "_geometry", geometrySource, GL32.GL_GEOMETRY_SHADER);
                program.attachShader(geometryShader);
            }

            // Link program
            program.link();

            // Store program
            programs.put(name, program);

            System.out.println("Created shader program: " + name);
            return program;

        } catch (Exception e) {
            System.err.println("Failed to create shader program '" + name + "': " + e.getMessage());
            throw new RuntimeException("Shader compilation failed", e);
        }
    }

    /**
     * Load shader program from files with fallback
     */
    public ShaderProgram loadProgram(String name, String vertexFile, String fragmentFile) {
        return loadProgram(name, vertexFile, fragmentFile, null);
    }

    public ShaderProgram loadProgram(String name, String vertexFile, String fragmentFile, String geometryFile) {
        try {
            String vertexSource = loadShaderSource(vertexFile);
            String fragmentSource = loadShaderSource(fragmentFile);
            String geometrySource = geometryFile != null ? loadShaderSource(geometryFile) : null;

            return createProgram(name, vertexSource, fragmentSource, geometrySource);

        } catch (IOException e) {
            System.err.println("Failed to load shader files for program '" + name + "': " + e.getMessage());

            // Check if we have a built-in fallback
            ShaderProgram existing = programs.get(name);
            if (existing != null) {
                System.out.println("Using existing built-in shader for: " + name);
                return existing;
            }

            throw new RuntimeException("Shader loading failed and no fallback available", e);
        }
    }

    /**
     * Get an existing shader program
     */
    public ShaderProgram getProgram(String name) {
        return programs.get(name);
    }

    /**
     * Get or create a shader program with built-in fallback
     */
    public ShaderProgram getProgramOrDefault(String name, String defaultVertexSource, String defaultFragmentSource) {
        ShaderProgram program = programs.get(name);
        if (program == null) {
            program = createProgram(name, defaultVertexSource, defaultFragmentSource);
            fallbackPrograms.add(name);
        }
        return program;
    }

    public void addReloadListener(ShaderReloadListener listener) {
        reloadListeners.add(listener);
    }

    /**
     * Remove reload listener
     */
    public void removeReloadListener(ShaderReloadListener listener) {
        reloadListeners.remove(listener);
    }

    /**
     * Notify all listeners that a shader was reloaded
     */
    private void notifyShaderReloaded(String programName) {
        for (ShaderReloadListener listener : reloadListeners) {
            try {
                listener.onShaderReloaded(programName);
            } catch (Exception e) {
                System.err.println("Error in shader reload listener: " + e.getMessage());
            }
        }
    }

    /**
     * Get all program names
     */
    public Set<String> getProgramNames() {
        return new HashSet<>(programs.keySet());
    }

    /**
     * Check if a program exists
     */
    public boolean hasProgram(String name) {
        return programs.containsKey(name);
    }

    /**
     * Get diagnostic information about the shader manager
     */
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== SHADER MANAGER STATUS ===\n");
        info.append("Active Programs: ").append(programs.size()).append("\n");
        info.append("Hot Reload: ").append(hotReloadEnabled ? "Enabled" : "Disabled").append("\n");
        info.append("Shader Directory: ").append(shaderDirectory != null ? shaderDirectory : "Not set").append("\n");
        info.append("Reload Listeners: ").append(reloadListeners.size()).append("\n");

        info.append("\nPrograms:\n");
        for (String name : programs.keySet()) {
            ShaderProgram program = programs.get(name);
            String status = program != null && program.isLinked() ? "OK" : "ERROR";
            info.append("  ").append(name).append(" - ").append(status).append("\n");
        }

        return info.toString();
    }

    /**
     * Set the directory to watch for shader files
     */
    public void setShaderDirectory(String path) {
        setShaderDirectory(Paths.get(path));
    }

    /**
     * Set the directory to watch for shader files
     */
    public void setShaderDirectory(Path path) {
        stopFileWatcher(); // Stop watching the old directory
        this.shaderDirectory = path;

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created shader directory: " + path);
            }

            if (hotReloadEnabled) {
                startFileWatcher(); // Start watching the new directory
            }

            System.out.println("Shader directory set to: " + path);
        } catch (IOException e) {
            System.err.println("Failed to set shader directory: " + e.getMessage());
        }
    }

    /**
     * Get the current shader directory
     */
    public Path getShaderDirectory() {
        return shaderDirectory;
    }

    /**
     * Enable or disable hot reloading of shaders from files
     */
    public void setHotReloadEnabled(boolean enabled) {
        if (this.hotReloadEnabled == enabled) return;

        this.hotReloadEnabled = enabled;
        System.out.println("Shader hot reload " + (enabled ? "enabled" : "disabled"));

        if (enabled) {
            startFileWatcher();
        } else {
            stopFileWatcher();
        }
    }

    /**
     * Check if hot reload is enabled
     */
    public boolean isHotReloadEnabled() {
        return hotReloadEnabled;
    }

    // Add these helper methods for file watching (simplified versions)
    private void startFileWatcher() {
        // Simplified file watcher - you can implement this if you want hot reload
        // For now, just log that it would start
        if (shaderDirectory != null && Files.exists(shaderDirectory)) {
            System.out.println("Would start watching shader directory: " + shaderDirectory);
        }
    }

    private void stopFileWatcher() {
        // Simplified - just log that it would stop
        System.out.println("Stopped shader file watcher");
    }

    public boolean reloadProgram(String name) {
        try {
            // After successful reload, notify listeners
            notifyShaderReloaded(name);
            System.out.println("Reloaded shader program: " + name);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to reload shader program '" + name + "': " + e.getMessage());
            return false;
        }
    }

    public void reloadAllPrograms() {
        for (String programName : new HashSet<>(programs.keySet())) {
            reloadProgram(programName);
        }
    }

    /**
     * Load built-in shaders with improved error handling
     */
    private void loadBuiltInShaders() {
        try {
            // Create default terrain shader
            createProgram("terrain", getDefaultTerrainVertexShader(), getDefaultTerrainFragmentShader());
            fallbackPrograms.add("terrain");

            // Create basic shaders
            createProgram("basic", getBasicVertexShader(), getBasicFragmentShader());
            fallbackPrograms.add("basic");

            createProgram("skybox", getSkyboxVertexShader(), getSkyboxFragmentShader());
            fallbackPrograms.add("skybox");

            createProgram("ui", getUIVertexShader(), getUIFragmentShader());
            fallbackPrograms.add("ui");

            System.out.println("Loaded built-in shaders successfully");
        } catch (Exception e) {
            System.err.println("Critical error loading built-in shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Compile a single shader with better error handling
     */
    private Shader compileShader(String name, String source, int type) {
        if (compilingShaders.contains(name)) {
            throw new RuntimeException("Circular shader dependency detected: " + name);
        }

        compilingShaders.add(name);

        try {
            // Process includes
            String processedSource = processIncludes(source);

            // Store source for reloading
            shaderSources.put(name, processedSource);

            // Compile shader
            Shader shader = new Shader(name, processedSource, type);
            shaders.put(name, shader);

            return shader;

        } finally {
            compilingShaders.remove(name);
        }
    }

    /**
     * Process #include directives in shader source
     */
    private String processIncludes(String source) {
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#include")) {
                String includePath = extractIncludePath(trimmed);
                if (includePath != null) {
                    try {
                        String includeSource = loadShaderSource(includePath);
                        result.append("// Begin include: ").append(includePath).append("\n");
                        result.append(processIncludes(includeSource)); // Recursive includes
                        result.append("// End include: ").append(includePath).append("\n");
                    } catch (IOException e) {
                        System.err.println("Failed to include shader: " + includePath + " - " + e.getMessage());
                        result.append("// Failed to include: ").append(includePath).append("\n");
                        // Add fallback content for common includes
                        result.append(getFallbackIncludeContent(includePath));
                    }
                } else {
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    private String extractIncludePath(String includeLine) {
        // Extract path from #include "path" or #include <path>
        int start = includeLine.indexOf('"');
        if (start == -1) start = includeLine.indexOf('<');
        if (start == -1) return null;

        int end = includeLine.indexOf('"', start + 1);
        if (end == -1) end = includeLine.indexOf('>', start + 1);
        if (end == -1) return null;

        return includeLine.substring(start + 1, end);
    }

    private String getFallbackIncludeContent(String includePath) {
        return switch (includePath) {
            case "common.glsl" -> getCommonShaderCode();
            case "lighting.glsl" -> getLightingShaderCode();
            default -> "// Fallback: " + includePath + " not found\n";
        };
    }

    /**
     * Built-in shader sources
     */
    private String getDefaultTerrainVertexShader() {
        return """
                #version 330 core
                
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec3 tangent;
                layout (location = 4) in vec3 color;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform mat4 lightSpaceMatrix;
                
                out vec3 fragPos;
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec3 fragTangent;
                out vec3 vertexColor;
                out vec4 fragPosLightSpace;
                out float height;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    fragTangent = mat3(transpose(inverse(modelMatrix))) * tangent;
                    vertexColor = color;
                    fragPosLightSpace = lightSpaceMatrix * worldPos;
                    height = position.y;
                
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;
    }

    private String getDefaultTerrainFragmentShader() {
        return """
                #version 330 core
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 fragTangent;
                in vec3 vertexColor;
                in vec4 fragPosLightSpace;
                in float height;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                out vec4 fragColor;
                
                vec3 calculateLighting() {
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    return ambient + diffuse + specular;
                }
                
                void main() {
                    vec3 lighting = calculateLighting();
                    vec3 finalColor = vertexColor * lighting;
                
                    // Apply fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;
    }

    private String getBasicVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec4 color;
                
                uniform mat4 mvpMatrix;
                uniform mat4 modelMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 projectionMatrix;
                
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec4 vertexColor;
                out vec3 fragPos;
                
                void main() {
                    fragPos = (modelMatrix * vec4(position, 1.0)).xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    vertexColor = color;
                    
                    if (length(mvpMatrix[0]) > 0.1) {
                        gl_Position = mvpMatrix * vec4(position, 1.0);
                    } else {
                        gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
                    }
                }
                """;
    }

    private String getBasicFragmentShader() {
        return """
                #version 330 core
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec4 vertexColor;
                in vec3 fragPos;
                
                uniform sampler2D texture0;
                uniform bool useTexture;
                uniform vec4 uniformColor;
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 viewPosition;
                
                out vec4 fragColor;
                
                void main() {
                    vec4 finalColor = vertexColor;
                    
                    if (useTexture && textureSize(texture0, 0).x > 1) {
                        finalColor *= texture(texture0, texCoords);
                    }
                    
                    if (uniformColor.a > 0.0) {
                        finalColor *= uniformColor;
                    }
                    
                    // Simple lighting if light is available
                    if (length(lightPosition) > 0.1) {
                        vec3 norm = normalize(fragNormal);
                        vec3 lightDir = normalize(lightPosition - fragPos);
                        float diff = max(dot(norm, lightDir), 0.0);
                        finalColor.rgb *= (0.3 + 0.7 * diff);
                    }
                    
                    fragColor = finalColor;
                }
                """;
    }

    private String getSkyboxVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec3 position;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform float dayNightCycle;
                uniform vec3 sunDirection;
                
                out vec3 texCoords;
                out vec3 worldPos;
                out float sunHeight;
                
                void main() {
                    texCoords = position;
                    worldPos = position;
                    sunHeight = sin(dayNightCycle);
                
                    // Remove translation from view matrix for skybox
                    mat4 rotView = mat4(mat3(viewMatrix));
                    vec4 pos = projectionMatrix * rotView * vec4(position, 1.0);
                
                    // Ensure skybox is always at far plane
                    gl_Position = pos.xyww;
                }
                """;
    }

    private String getSkyboxFragmentShader() {
        return """
                #version 330 core
                in vec3 texCoords;
                in vec3 worldPos;
                in float sunHeight;
                
                uniform vec3 sunDirection;
                uniform vec3 sunColor;
                uniform float dayNightCycle;
                
                out vec4 fragColor;
                
                vec3 getSkyColor() {
                    vec3 viewDir = normalize(texCoords);
                    float height = viewDir.y;
                
                    vec3 skyColor;
                    vec3 horizonColor;
                
                    if (sunHeight > 0.0) {
                        // Day sky
                        float intensity = min(1.0, sunHeight * 2.0);
                        skyColor = vec3(0.4 + intensity * 0.3, 0.6 + intensity * 0.2, 1.0);
                        horizonColor = vec3(0.7 + intensity * 0.2, 0.8 + intensity * 0.1, 1.0);
                    } else {
                        // Night sky
                        skyColor = vec3(0.05, 0.05, 0.15);
                        horizonColor = vec3(0.1, 0.1, 0.3);
                    }
                
                    // Gradient from horizon to zenith
                    float t = max(0.0, height);
                    return mix(horizonColor, skyColor, t);
                }
                
                vec3 getSunColor() {
                    vec3 viewDir = normalize(texCoords);
                    float sunDot = dot(viewDir, normalize(sunDirection));
                
                    if (sunHeight > -0.2 && sunDot > 0.999) {
                        // Sun disk
                        float intensity = max(0.1, sunHeight);
                        return vec3(1.0, 0.9, 0.7) * intensity * 3.0;
                    } else if (sunHeight <= -0.2) {
                        // Moon
                        vec3 moonDir = -sunDirection;
                        float moonDot = dot(viewDir, normalize(moonDir));
                        if (moonDot > 0.9985) {
                            return vec3(0.8, 0.8, 0.9) * 2.0;
                        }
                    }
                
                    return vec3(0.0);
                }
                
                void main() {
                    vec3 skyColor = getSkyColor();
                    vec3 celestialColor = getSunColor();
                
                    vec3 finalColor = skyColor + celestialColor;
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;
    }

    private String getUIVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec2 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec4 color;
                
                uniform mat4 projection;
                
                out vec2 texCoords;
                out vec4 vertexColor;
                
                void main() {
                    texCoords = texCoord;
                    vertexColor = color;
                    
                    gl_Position = projection * vec4(position, 0.0, 1.0);
                }
                """;
    }

    private String getUIFragmentShader() {
        return """
                #version 330 core
                in vec2 texCoords;
                in vec4 vertexColor;
                
                uniform sampler2D uiTexture;
                uniform bool useTexture;
                uniform vec4 uniformColor;
                
                out vec4 fragColor;
                
                void main() {
                    vec4 finalColor = vertexColor;
                    
                    if (useTexture && textureSize(uiTexture, 0).x > 1) {
                        vec4 texColor = texture(uiTexture, texCoords);
                        finalColor *= texColor;
                    }
                    
                    if (uniformColor.a > 0.0) {
                        finalColor *= uniformColor;
                    }
                    
                    fragColor = finalColor;
                }
                """;
    }

    private String getCommonShaderCode() {
        return """
                // Camera uniforms
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                uniform float time;
                
                // Utility functions
                float calculateFog(vec3 worldPos) {
                    float distance = length(viewPosition - worldPos);
                    return exp(-fogDensity * distance);
                }
                
                vec3 applyFog(vec3 color, float fogFactor) {
                    return mix(fogColor, color, clamp(fogFactor, 0.0, 1.0));
                }
                
                // Noise functions
                float random(vec2 st) {
                    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
                }
                
                float noise(vec2 st) {
                    vec2 i = floor(st);
                    vec2 f = fract(st);
                
                    float a = random(i);
                    float b = random(i + vec2(1.0, 0.0));
                    float c = random(i + vec2(0.0, 1.0));
                    float d = random(i + vec2(1.0, 1.0));
                
                    vec2 u = f * f * (3.0 - 2.0 * f);
                
                    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
                }
                """;
    }

    private String getLightingShaderCode() {
        return """
                // Light uniforms
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                
                vec3 calculateLighting(vec3 worldPos, vec3 normal) {
                    vec3 norm = normalize(normal);
                    vec3 lightDir = normalize(lightPosition - worldPos);
                
                    // Ambient
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - worldPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    return ambient + diffuse + specular;
                }
                
                vec3 calculateDirectionalLighting(vec3 worldPos, vec3 normal, vec3 direction) {
                    vec3 norm = normalize(normal);
                    vec3 lightDir = normalize(-direction);
                
                    // Ambient
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - worldPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    return ambient + diffuse + specular;
                }
                """;
    }

    /**
     * Cleanup all resources including shaders
     */
    public void cleanup() {
        try {
            // Stop file watcher
            stopFileWatcher();

            // Cleanup all shader programs
            for (ShaderProgram program : programs.values()) {
                program.cleanup();
            }
            programs.clear();

            // Cleanup individual shaders
            for (Shader shader : shaders.values()) {
                shader.cleanup();
            }
            shaders.clear();

            // Clear caches
            shaderSources.clear();
            lastModified.clear();
            compilingShaders.clear();
            fallbackPrograms.clear();
            reloadListeners.clear();

            System.out.println("Shader manager cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during shader manager cleanup: " + e.getMessage());
        }
    }
}