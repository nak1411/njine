package com.nak.engine.shader;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ShaderManager() {
        try {
            // Default shader directory
            this.shaderDirectory = Paths.get("src/main/resources/shaders");

            // Create directory if it doesn't exist
            if (!Files.exists(shaderDirectory)) {
                Files.createDirectories(shaderDirectory);
                createDefaultShaders();
            }

            // Load built-in shaders
            loadBuiltInShaders();

        } catch (IOException e) {
            System.err.println("Failed to initialize shader directory: " + e.getMessage());
            // Fall back to built-in shaders only
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
     * Enable or disable hot reloading of shaders from files
     */
    public void setHotReloadEnabled(boolean enabled) {
        if (this.hotReloadEnabled == enabled) return;

        this.hotReloadEnabled = enabled;

        if (enabled) {
            startFileWatcher();
        } else {
            stopFileWatcher();
        }
    }

    /**
     * Set the directory to watch for shader files
     */
    public void setShaderDirectory(String path) {
        setShaderDirectory(Paths.get(path));
    }

    public void setShaderDirectory(Path path) {
        stopFileWatcher();
        this.shaderDirectory = path;

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            if (hotReloadEnabled) {
                startFileWatcher();
            }
        } catch (IOException e) {
            System.err.println("Failed to set shader directory: " + e.getMessage());
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
     * Load shader program from files
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
            throw new RuntimeException("Shader loading failed", e);
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
        }
        return program;
    }

    /**
     * Reload a specific shader program
     */
    public boolean reloadProgram(String name) {
        ShaderProgram existingProgram = programs.get(name);
        if (existingProgram == null) {
            System.err.println("Cannot reload non-existent program: " + name);
            return false;
        }

        try {
            // Create new program with same parameters
            ShaderProgram newProgram = new ShaderProgram(name);

            // Recompile shaders
            for (Shader shader : existingProgram.getAttachedShaders()) {
                String shaderName = shader.getName();
                String source = shaderSources.get(shaderName);

                if (source != null) {
                    Shader newShader = compileShader(shaderName, source, shader.getType());
                    newProgram.attachShader(newShader);
                }
            }

            // Link new program
            newProgram.link();

            // Replace old program
            existingProgram.cleanup();
            programs.put(name, newProgram);

            // Notify listeners
            notifyShaderReloaded(name);

            System.out.println("Reloaded shader program: " + name);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to reload shader program '" + name + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Reload all shader programs
     */
    public void reloadAllPrograms() {
        List<String> programNames = new ArrayList<>(programs.keySet());
        for (String name : programNames) {
            reloadProgram(name);
        }
    }

    /**
     * Compile a single shader
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
                        System.err.println("Failed to include shader: " + includePath);
                        result.append("// Failed to include: ").append(includePath).append("\n");
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
        // Support both "file.glsl" and <file.glsl> syntax
        if (includeLine.contains("\"")) {
            int start = includeLine.indexOf('"') + 1;
            int end = includeLine.lastIndexOf('"');
            if (start > 0 && end > start) {
                return includeLine.substring(start, end);
            }
        } else if (includeLine.contains("<")) {
            int start = includeLine.indexOf('<') + 1;
            int end = includeLine.lastIndexOf('>');
            if (start > 0 && end > start) {
                return includeLine.substring(start, end);
            }
        }
        return null;
    }

    /**
     * Load shader source from file
     */
    private String loadShaderSource(String filename) throws IOException {
        Path filePath = shaderDirectory.resolve(filename);

        if (!Files.exists(filePath)) {
            // Try loading from classpath
            try {
                return new String(Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream("shaders/" + filename)
                ).readAllBytes());
            } catch (Exception e) {
                throw new IOException("Shader file not found: " + filename);
            }
        }

        return Files.readString(filePath);
    }

    /**
     * Start file watching for hot reload
     */
    private void startFileWatcher() {
        if (fileWatchExecutor != null || shaderDirectory == null) return;

        try {
            watchService = FileSystems.getDefault().newWatchService();
            fileWatchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ShaderWatcher");
                t.setDaemon(true);
                return t;
            });

            // Register directory for watching
            WatchKey watchKey = shaderDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);
            watchKeys.put(watchKey, shaderDirectory);

            // Also watch subdirectories
            Files.walk(shaderDirectory)
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(shaderDirectory))
                    .forEach(dir -> {
                        try {
                            WatchKey key = dir.register(watchService,
                                    StandardWatchEventKinds.ENTRY_MODIFY,
                                    StandardWatchEventKinds.ENTRY_CREATE);
                            watchKeys.put(key, dir);
                        } catch (IOException e) {
                            System.err.println("Failed to watch shader subdirectory: " + dir);
                        }
                    });

            // Start watching
            fileWatchExecutor.submit(this::watchFiles);

            System.out.println("Started shader hot reload watching: " + shaderDirectory);

        } catch (IOException e) {
            System.err.println("Failed to start shader file watcher: " + e.getMessage());
        }
    }

    /**
     * File watching loop
     */
    private void watchFiles() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                Path dir = watchKeys.get(key);

                if (dir == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = dir.resolve(filename);

                    if (isShaderFile(filename.toString())) {
                        handleShaderFileChange(fullPath, kind);
                    }
                }

                if (!key.reset()) {
                    watchKeys.remove(key);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error in shader file watcher: " + e.getMessage());
        }
    }

    private boolean isShaderFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".glsl") ||
                lower.endsWith(".vert") ||
                lower.endsWith(".frag") ||
                lower.endsWith(".geom") ||
                lower.endsWith(".comp") ||
                lower.endsWith(".vs") ||
                lower.endsWith(".fs") ||
                lower.endsWith(".gs");
    }

    private void handleShaderFileChange(Path filePath, WatchEvent.Kind<?> kind) {
        try {
            // Small delay to ensure file is fully written
            Thread.sleep(100);

            // Check if file was actually modified
            long currentModified = Files.getLastModifiedTime(filePath).toMillis();
            String pathKey = filePath.toString();
            Long lastMod = lastModified.get(pathKey);

            if (lastMod != null && currentModified <= lastMod) {
                return; // No actual change
            }

            lastModified.put(pathKey, currentModified);

            System.out.println("Shader file changed: " + filePath.getFileName());

            // Reload affected programs
            reloadProgramsUsingFile(filePath.getFileName().toString());

        } catch (Exception e) {
            System.err.println("Error handling shader file change: " + e.getMessage());
        }
    }

    private void reloadProgramsUsingFile(String filename) {
        // This is a simple implementation - in practice you'd want to track
        // which programs use which files more precisely
        reloadAllPrograms();
    }

    /**
     * Stop file watching
     */
    private void stopFileWatcher() {
        if (fileWatchExecutor != null) {
            fileWatchExecutor.shutdown();
            fileWatchExecutor = null;
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                System.err.println("Error closing watch service: " + e.getMessage());
            }
            watchService = null;
        }

        watchKeys.clear();
    }

    /**
     * Load built-in shaders
     */
    private void loadBuiltInShaders() {
        // Create default terrain shader
        createProgram("terrain", getDefaultTerrainVertexShader(), getDefaultTerrainFragmentShader());

        // Create basic shaders
        createProgram("basic", getBasicVertexShader(), getBasicFragmentShader());
        createProgram("skybox", getSkyboxVertexShader(), getSkyboxFragmentShader());
        createProgram("ui", getUIVertexShader(), getUIFragmentShader());

        System.out.println("Loaded built-in shaders");
    }

    /**
     * Create default shader files in the shader directory
     */
    private void createDefaultShaders() {
        try {
            // Create terrain shaders
            Files.writeString(shaderDirectory.resolve("terrain.vert"), getDefaultTerrainVertexShader());
            Files.writeString(shaderDirectory.resolve("terrain.frag"), getDefaultTerrainFragmentShader());

            // Create basic shaders
            Files.writeString(shaderDirectory.resolve("basic.vert"), getBasicVertexShader());
            Files.writeString(shaderDirectory.resolve("basic.frag"), getBasicFragmentShader());

            // Create common includes
            Files.writeString(shaderDirectory.resolve("common.glsl"), getCommonShaderCode());
            Files.writeString(shaderDirectory.resolve("lighting.glsl"), getLightingShaderCode());

            System.out.println("Created default shader files");

        } catch (IOException e) {
            System.err.println("Failed to create default shader files: " + e.getMessage());
        }
    }

    /**
     * Add reload listener
     */
    public void addReloadListener(ShaderReloadListener listener) {
        reloadListeners.add(listener);
    }

    public void removeReloadListener(ShaderReloadListener listener) {
        reloadListeners.remove(listener);
    }

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
     * Cleanup all resources
     */
    public void cleanup() {
        stopFileWatcher();

        // Cleanup all programs
        for (ShaderProgram program : programs.values()) {
            program.cleanup();
        }
        programs.clear();

        // Cleanup all shaders
        for (Shader shader : shaders.values()) {
            shader.cleanup();
        }
        shaders.clear();

        shaderSources.clear();
        lastModified.clear();
        reloadListeners.clear();

        instance = null;
    }

    // Built-in shader sources
    private String getDefaultTerrainVertexShader() {
        return """
                #version 330 core
                #include "common.glsl"
                
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
                #include "common.glsl"
                #include "lighting.glsl"
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 fragTangent;
                in vec3 vertexColor;
                in vec4 fragPosLightSpace;
                in float height;
                
                out vec4 fragColor;
                
                void main() {
                    vec3 normal = normalize(fragNormal);
                    vec3 lighting = calculateLighting(fragPos, normal);
                    vec3 finalColor = vertexColor * lighting;
                
                    // Apply fog
                    float fogFactor = calculateFog(fragPos);
                    finalColor = applyFog(finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;
    }

    private String getBasicVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec3 color;
                
                uniform mat4 mvpMatrix;
                
                out vec3 vertColor;
                
                void main() {
                    vertColor = color;
                    gl_Position = mvpMatrix * vec4(position, 1.0);
                }
                """;
    }

    private String getBasicFragmentShader() {
        return """
                #version 330 core
                in vec3 vertColor;
                out vec4 fragColor;
                
                void main() {
                    fragColor = vec4(vertColor, 1.0);
                }
                """;
    }

    private String getSkyboxVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec3 position;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                
                out vec3 texCoords;
                
                void main() {
                    texCoords = position;
                    vec4 pos = projectionMatrix * viewMatrix * vec4(position, 1.0);
                    gl_Position = pos.xyww; // Ensure skybox is always at far plane
                }
                """;
    }

    private String getSkyboxFragmentShader() {
        return """
                #version 330 core
                in vec3 texCoords;
                out vec4 fragColor;
                
                uniform samplerCube skybox;
                
                void main() {
                    fragColor = texture(skybox, texCoords);
                }
                """;
    }

    private String getUIVertexShader() {
        return """
                #version 330 core
                layout (location = 0) in vec2 position;
                layout (location = 1) in vec2 texCoord;
                
                uniform mat4 projection;
                
                out vec2 texCoords;
                
                void main() {
                    texCoords = texCoord;
                    gl_Position = projection * vec4(position, 0.0, 1.0);
                }
                """;
    }

    private String getUIFragmentShader() {
        return """
                #version 330 core
                in vec2 texCoords;
                out vec4 fragColor;
                
                uniform sampler2D uiTexture;
                uniform vec4 color;
                
                void main() {
                    vec4 texColor = texture(uiTexture, texCoords);
                    fragColor = texColor * color;
                }
                """;
    }

    private String getCommonShaderCode() {
        return """
                // Common shader definitions and utility functions
                
                // Camera uniforms
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                // Time uniform
                uniform float time;
                
                // Utility functions
                float calculateFog(vec3 worldPos) {
                    float distance = length(viewPosition - worldPos);
                    return exp(-fogDensity * distance);
                }
                
                vec3 applyFog(vec3 color, float fogFactor) {
                    return mix(fogColor, color, clamp(fogFactor, 0.0, 1.0));
                }
                """;
    }

    private String getLightingShaderCode() {
        return """
                // Lighting calculations
                
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
                """;
    }

    // Interface for shader reload notifications
    public interface ShaderReloadListener {
        void onShaderReloaded(String programName);
    }
}
