package com.nak.engine.shader;

import com.nak.engine.core.Module;
import com.nak.engine.core.ResourceManager;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.events.ShaderReloadedEvent;

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

        // Load built-in shaders
        loadBuiltInShaders();

        // Load shaders from files
        loadShadersFromFiles();

        // Setup hot reload if enabled
        boolean debugMode = getOptionalService(com.nak.engine.config.EngineConfig.class)
                .map(config -> config.isEnableHotReload())
                .orElse(false);

        if (debugMode) {
            setupHotReload();
        }

        // Register services
        serviceLocator.register(ShaderModule.class, this);

        System.out.println("Shader module initialized with " + programs.size() + " programs");
    }

    private void loadBuiltInShaders() {
        try {
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

        } catch (Exception e) {
            System.err.println("Failed to load built-in shaders: " + e.getMessage());
        }
    }

    private void loadShadersFromFiles() {
        if (!Files.exists(shaderDirectory)) {
            return;
        }

        try {
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
            validator.validateVertexShader(processedVertSource);
            validator.validateFragmentShader(processedFragSource);

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
            cache.put(name, program);

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
        // Simple include processing - could be enhanced
        return source.replaceAll("#include\\s+\"([^\"]+)\"", (match) -> {
            String includePath = match.group(1);
            try {
                return Files.readString(shaderDirectory.resolve(includePath));
            } catch (Exception e) {
                System.err.println("Failed to include shader: " + includePath);
                return "// Failed to include: " + includePath;
            }
        });
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

            // Start watch thread
            Thread watchThread = new Thread(this::watchForChanges);
            watchThread.setDaemon(true);
            watchThread.setName("ShaderHotReload");
            watchThread.start();

            hotReloadEnabled = true;
            System.out.println("Shader hot reload enabled");

        } catch (Exception e) {
            System.err.println("Failed to setup shader hot reload: " + e.getMessage());
        }
    }

    private void watchForChanges() {
        while (hotReloadEnabled && watchService != null) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path changed = (Path) event.context();
                    String filename = changed.toString();

                    if (filename.endsWith(".vert") || filename.endsWith(".frag")) {
                        String shaderName = filename.substring(0, filename.lastIndexOf('.'));
                        reloadShader(shaderName);
                    }
                }

                key.reset();

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Error in shader hot reload: " + e.getMessage());
            }
        }
    }

    public boolean reloadShader(String name) {
        try {
            ShaderSource source = sources.get(name);
            if (source == null) {
                System.err.println("No source found for shader: " + name);
                return false;
            }

            // Cleanup old program
            ShaderProgram oldProgram = programs.get(name);
            if (oldProgram != null) {
                oldProgram.cleanup();
            }

            // Reload from files
            Path vertPath = shaderDirectory.resolve(name + ".vert");
            Path fragPath = shaderDirectory.resolve(name + ".frag");

            if (Files.exists(vertPath) && Files.exists(fragPath)) {
                String vertSource = Files.readString(vertPath);
                String fragSource = Files.readString(fragPath);

                createProgram(name, vertSource, fragSource);

                // Post reload event
                eventBus.post(new ShaderReloadedEvent(name, true, null));

                System.out.println("Reloaded shader: " + name);
                return true;
            }

        } catch (Exception e) {
            String error = "Failed to reload shader '" + name + "': " + e.getMessage();
            System.err.println(error);

            eventBus.post(new ShaderReloadedEvent(name, false, error));
            return false;
        }

        return false;
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
