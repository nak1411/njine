package com.nak.engine.shader;

import com.nak.engine.core.Module;
import com.nak.engine.core.ResourceManager;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.events.ShaderReloadedEvent;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShaderModule extends Module {
    private EventBus eventBus;
    private ResourceManager resourceManager;

    // Shader management
    private final Map<String, ShaderProgram> programs = new ConcurrentHashMap<>();
    private final Map<String, ShaderSource> sources = new HashMap<>();
    private ShaderValidator validator;
    private ShaderCache cache;

    // Hot reload
    private WatchService watchService;
    private Path shaderDirectory;
    private boolean hotReloadEnabled = false;
    private boolean openGLResourcesCreated = false;

    @Override
    public String getName() {
        return "Shader";
    }

    @Override
    public int getInitializationPriority() {
        return 50; // Initialize early for other systems
    }

    @Override
    public void initialize() {
        eventBus = getService(EventBus.class);
        resourceManager = getService(ResourceManager.class);

        // Initialize shader components
        validator = new ShaderValidator();
        cache = new ShaderCache();
        shaderDirectory = Paths.get("src/main/resources/shaders");

        // DON'T load shaders here - wait for OpenGL context
        System.out.println("Shader module initialized (waiting for OpenGL context)");

        // Setup hot reload if enabled
        boolean debugMode = false;
        com.nak.engine.config.EngineConfig engineConfig = getOptionalService(com.nak.engine.config.EngineConfig.class);
        if (engineConfig != null) {
            debugMode = engineConfig.isEnableHotReload();
        }

        if (debugMode) {
            setupHotReload();
        }

        // Register services
        serviceLocator.register(ShaderModule.class, this);
    }

    /**
     * Call this after OpenGL context is created to actually create the shader programs
     */
    public void createOpenGLResources() {
        if (openGLResourcesCreated) {
            System.out.println("OpenGL resources already created, skipping");
            return;
        }

        try {
            System.out.println("Creating OpenGL shader resources...");

            // Now we can safely create OpenGL resources
            loadBuiltInShaders();
            loadShadersFromFiles();

            openGLResourcesCreated = true;
            System.out.println("Shader module OpenGL resources created with " + programs.size() + " programs");

        } catch (Exception e) {
            System.err.println("Failed to create OpenGL shader resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadBuiltInShaders() {
        try {
            System.out.println("Loading built-in shaders...");

            // Create basic terrain shader
            createProgram("terrain",
                    getBuiltInVertexShader("terrain"),
                    getBuiltInFragmentShader("terrain"));

            createProgram("basic",
                    getBuiltInVertexShader("basic"),
                    getBuiltInFragmentShader("basic"));

            createProgram("skybox",
                    getBuiltInVertexShader("skybox"),
                    getBuiltInFragmentShader("skybox"));

            createProgram("ui",
                    getBuiltInVertexShader("ui"),
                    getBuiltInFragmentShader("ui"));

            System.out.println("Built-in shaders loaded successfully");

        } catch (Exception e) {
            System.err.println("Failed to load built-in shaders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadShadersFromFiles() {
        if (!Files.exists(shaderDirectory)) {
            System.out.println("Shader directory not found, skipping file loading");
            return;
        }

        try {
            System.out.println("Loading shaders from files...");

            // Load shader pairs
            String[] shaderNames = {"terrain", "basic", "skybox", "ui", "particle", "water"};

            for (String name : shaderNames) {
                Path vertPath = shaderDirectory.resolve(name + ".vert");
                Path fragPath = shaderDirectory.resolve(name + ".frag");

                if (Files.exists(vertPath) && Files.exists(fragPath)) {
                    try {
                        String vertSource = Files.readString(vertPath);
                        String fragSource = Files.readString(fragPath);

                        // Replace built-in shader if it exists
                        if (programs.containsKey(name)) {
                            programs.get(name).cleanup();
                        }

                        createProgram(name, vertSource, fragSource);
                        System.out.println("Loaded shader from files: " + name);

                    } catch (Exception e) {
                        System.err.println("Failed to load shader " + name + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading shaders from files: " + e.getMessage());
        }
    }

    public ShaderProgram createProgram(String name, String vertexSource, String fragmentSource) {
        try {
            // Process includes and macros
            String processedVertSource = processShaderSource(vertexSource);
            String processedFragSource = processShaderSource(fragmentSource);

            // Validate shaders
            if (validator != null) {
                validator.validateVertexShader(processedVertSource);
                validator.validateFragmentShader(processedFragSource);
            }

            // Create program
            ShaderProgram program = new ShaderProgram(name);

            // Compile and attach shaders
            Shader vertexShader = new Shader(name + "_vertex", processedVertSource,
                    org.lwjgl.opengl.GL20.GL_VERTEX_SHADER);
            Shader fragmentShader = new Shader(name + "_fragment", processedFragSource,
                    org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER);

            program.attachShader(vertexShader);
            program.attachShader(fragmentShader);
            program.link();

            // Cache the program
            programs.put(name, program);
            if (cache != null) {
                cache.put(name, program);
            }

            // Store sources for hot reload
            sources.put(name, new ShaderSource(vertexSource, fragmentSource));

            return program;

        } catch (Exception e) {
            System.err.println("Failed to create shader program '" + name + "': " + e.getMessage());
            throw new RuntimeException("Shader compilation failed", e);
        }
    }

    private String processShaderSource(String source) {
        // Process #include directives
        source = processIncludes(source);
        // Process #define macros
        source = processMacros(source);
        return source;
    }

    private String processIncludes(String source) {
        // Simple include processing
        String[] lines = source.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#include")) {
                String includePath = extractIncludePath(trimmed);
                if (includePath != null) {
                    try {
                        String includeSource = Files.readString(shaderDirectory.resolve(includePath));
                        result.append("// Begin include: ").append(includePath).append("\n");
                        result.append(includeSource);
                        result.append("// End include: ").append(includePath).append("\n");
                    } catch (Exception e) {
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
        // Extract path from #include "path" or #include <path>
        int start = includeLine.indexOf('"');
        if (start == -1) start = includeLine.indexOf('<');
        if (start == -1) return null;

        int end = includeLine.indexOf('"', start + 1);
        if (end == -1) end = includeLine.indexOf('>', start + 1);
        if (end == -1) return null;

        return includeLine.substring(start + 1, end);
    }

    private String processMacros(String source) {
        // Add common macros
        StringBuilder macros = new StringBuilder();
        macros.append("#define ENGINE_VERSION 100\n");
        macros.append("#define PI 3.14159265359\n");
        return macros + source;
    }

    private void setupHotReload() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            shaderDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE);

            hotReloadEnabled = true;
            System.out.println("Shader hot reload enabled");

        } catch (Exception e) {
            System.err.println("Failed to setup shader hot reload: " + e.getMessage());
        }
    }

    public ShaderProgram getProgram(String name) {
        return programs.get(name);
    }

    public boolean hasProgram(String name) {
        return programs.containsKey(name);
    }

    @Override
    public void cleanup() {
        hotReloadEnabled = false;

        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                System.err.println("Error closing watch service: " + e.getMessage());
            }
        }

        for (ShaderProgram program : programs.values()) {
            program.cleanup();
        }
        programs.clear();
        sources.clear();

        if (cache != null) {
            cache.cleanup();
        }

        System.out.println("Shader module cleaned up");
    }

    // Built-in shader sources
    private String getBuiltInVertexShader(String name) {
        return switch (name) {
            case "terrain" -> """
                #version 330 core
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                layout (location = 3) in vec3 tangent;
                layout (location = 4) in vec3 color;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                
                out vec3 fragPos;
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec3 vertexColor;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    vertexColor = color;
                    
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;
            case "basic" -> """
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
            case "skybox" -> """
                #version 330 core
                layout (location = 0) in vec3 position;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                
                out vec3 texCoords;
                
                void main() {
                    texCoords = position;
                    mat4 rotView = mat4(mat3(viewMatrix));
                    vec4 pos = projectionMatrix * rotView * vec4(position, 1.0);
                    gl_Position = pos.xyww;
                }
                """;
            case "ui" -> """
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
            default -> throw new IllegalArgumentException("Unknown built-in vertex shader: " + name);
        };
    }

    private String getBuiltInFragmentShader(String name) {
        return switch (name) {
            case "terrain" -> """
                #version 330 core
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 vertexColor;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 viewPosition;
                
                out vec4 fragColor;
                
                void main() {
                    vec3 color = vertexColor;
                    if (length(color) < 0.1) {
                        color = vec3(0.3, 0.6, 0.2);
                    }
                    
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                    float diff = max(dot(norm, lightDir), 0.0);
                    
                    vec3 ambient = 0.3 * lightColor;
                    vec3 diffuse = diff * lightColor;
                    
                    vec3 result = (ambient + diffuse) * color;
                    fragColor = vec4(result, 1.0);
                }
                """;
            case "basic" -> """
                #version 330 core
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec4 vertexColor;
                in vec3 fragPos;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                
                out vec4 fragColor;
                
                void main() {
                    vec4 finalColor = vertexColor;
                    
                    if (length(lightPosition) > 0.1) {
                        vec3 norm = normalize(fragNormal);
                        vec3 lightDir = normalize(lightPosition - fragPos);
                        float diff = max(dot(norm, lightDir), 0.0);
                        finalColor.rgb *= (0.3 + 0.7 * diff);
                    }
                    
                    fragColor = finalColor;
                }
                """;
            case "skybox" -> """
                #version 330 core
                in vec3 texCoords;
                
                out vec4 fragColor;
                
                void main() {
                    vec3 viewDir = normalize(texCoords);
                    float height = viewDir.y;
                    
                    vec3 skyColor = vec3(0.4, 0.6, 1.0);
                    vec3 horizonColor = vec3(0.7, 0.8, 1.0);
                    
                    float t = max(0.0, height);
                    vec3 color = mix(horizonColor, skyColor, t);
                    
                    fragColor = vec4(color, 1.0);
                }
                """;
            case "ui" -> """
                #version 330 core
                in vec2 texCoords;
                in vec4 vertexColor;
                
                out vec4 fragColor;
                
                void main() {
                    fragColor = vertexColor;
                }
                """;
            default -> throw new IllegalArgumentException("Unknown built-in fragment shader: " + name);
        };
    }

    // Helper classes
    private static class ShaderSource {
        final String vertexSource;
        final String fragmentSource;

        ShaderSource(String vertexSource, String fragmentSource) {
            this.vertexSource = vertexSource;
            this.fragmentSource = fragmentSource;
        }
    }
}