package com.nak.engine.render;

import com.nak.engine.config.EngineConfig;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.events.WindowResizeEvent;
import com.nak.engine.shader.ShaderModule;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class WindowModule extends Module {
    private EngineConfig config;
    private EventBus eventBus;

    private long window;
    private int windowWidth;
    private int windowHeight;
    private boolean initialized = false;
    private boolean contextCreated = false;

    @Override
    public String getName() {
        return "Window";
    }

    @Override
    public int getInitializationPriority() {
        return 25; // Initialize early, before graphics systems
    }

    @Override
    public void initialize() {
        try {
            // Get configuration
            config = getOptionalService(EngineConfig.class);
            if (config == null) {
                config = createDefaultEngineConfig();
                serviceLocator.register(EngineConfig.class, config);
            }

            eventBus = getService(EventBus.class);

            // Initialize window dimensions
            windowWidth = config.getWindowWidth();
            windowHeight = config.getWindowHeight();

            // Initialize GLFW
            initializeGLFW();

            // Create window
            createWindow();

            // Setup window
            setupWindow();

            // Initialize OpenGL context
            initializeOpenGL();

            // Register services
            serviceLocator.register(WindowModule.class, this);
            serviceLocator.register("windowHandle", window);

            // Notify other modules that OpenGL context is ready
            notifyOpenGLContextReady();

            initialized = true;
            contextCreated = true;

            System.out.println("Window module initialized: " + windowWidth + "x" + windowHeight +
                    (config.isFullscreen() ? " (fullscreen)" : " (windowed)"));

        } catch (Exception e) {
            System.err.println("Failed to initialize window module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Window module initialization failed", e);
        }
    }

    private EngineConfig createDefaultEngineConfig() {
        EngineConfig defaultConfig = new EngineConfig();
        try {
            defaultConfig.validate();
            System.out.println("Created default engine config");
        } catch (Exception e) {
            System.err.println("Warning: Default engine config validation failed: " + e.getMessage());
        }
        return defaultConfig;
    }

    private void initializeGLFW() {
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        System.out.println("GLFW initialized successfully");
    }

    private void createWindow() {
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);

        // Anti-aliasing
        if (config.getMsaaSamples() > 0) {
            glfwWindowHint(GLFW_SAMPLES, config.getMsaaSamples());
        }

        // Create window
        window = glfwCreateWindow(windowWidth, windowHeight, config.getTitle(), NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup callbacks
        setupCallbacks();

        System.out.println("Window created successfully");
    }

    private void setupCallbacks() {
        // Window size callback
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            int oldWidth = this.windowWidth;
            int oldHeight = this.windowHeight;

            this.windowWidth = width;
            this.windowHeight = height;

            glViewport(0, 0, width, height);

            if (eventBus != null) {
                eventBus.post(new WindowResizeEvent(oldWidth, oldHeight, width, height));
            }
        });

        // Window close callback
        glfwSetWindowCloseCallback(window, (window) -> {
            if (config.isExitOnWindowClose()) {
                // Will be handled by main loop
            }
        });
    }

    private void setupWindow() {
        // Center window
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            if (vidmode != null) {
                glfwSetWindowPos(window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2);
            }
        }

        // Make OpenGL context current
        glfwMakeContextCurrent(window);

        // Set v-sync
        glfwSwapInterval(config.isVsync() ? 1 : 0);

        // Show window
        glfwShowWindow(window);
    }

    private void initializeOpenGL() {
        // Create OpenGL capabilities
        GL.createCapabilities();

        // Log OpenGL info
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("Graphics Card: " + glGetString(GL_RENDERER));
        System.out.println("OpenGL Vendor: " + glGetString(GL_VENDOR));

        // Set initial viewport
        glViewport(0, 0, windowWidth, windowHeight);

        // Basic OpenGL setup
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);

        System.out.println("OpenGL context initialized successfully");
    }

    private void notifyOpenGLContextReady() {
        // Notify ShaderModule that OpenGL context is ready
        ShaderModule shaderModule = getOptionalService(ShaderModule.class);
        if (shaderModule != null) {
            shaderModule.createOpenGLResources();
        }

        // Notify InputModule about window handle
        com.nak.engine.input.InputModule inputModule = getOptionalService(com.nak.engine.input.InputModule.class);
        if (inputModule != null) {
            inputModule.setWindowHandle(window);
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        try {
            // Poll GLFW events
            glfwPollEvents();

        } catch (Exception e) {
            System.err.println("Error updating window module: " + e.getMessage());
        }
    }

    public void swapBuffers() {
        if (initialized) {
            glfwSwapBuffers(window);
        }
    }

    public boolean shouldClose() {
        return initialized && glfwWindowShouldClose(window);
    }

    public void setShouldClose(boolean shouldClose) {
        if (initialized) {
            glfwSetWindowShouldClose(window, shouldClose);
        }
    }

    public void toggleFullscreen() {
        if (!initialized) return;

        try {
            boolean isFullscreen = config.isFullscreen();
            config.setFullscreen(!isFullscreen);

            if (config.isFullscreen()) {
                // Switch to fullscreen
                long monitor = glfwGetPrimaryMonitor();
                GLFWVidMode vidMode = glfwGetVideoMode(monitor);
                if (vidMode != null) {
                    glfwSetWindowMonitor(window, monitor, 0, 0,
                            vidMode.width(), vidMode.height(), vidMode.refreshRate());
                }
            } else {
                // Switch to windowed
                glfwSetWindowMonitor(window, NULL, 100, 100,
                        windowWidth, windowHeight, GLFW_DONT_CARE);
            }
        } catch (Exception e) {
            System.err.println("Error toggling fullscreen: " + e.getMessage());
        }
    }

    @Override
    public void cleanup() {
        try {
            initialized = false;
            contextCreated = false;

            if (window != NULL) {
                glfwDestroyWindow(window);
                window = NULL;
            }

            glfwTerminate();

            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null) {
                callback.free();
            }

            System.out.println("Window module cleaned up");

        } catch (Exception e) {
            System.err.println("Error during window module cleanup: " + e.getMessage());
        }
    }

    // Getters
    public long getWindow() {
        return window;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isContextCreated() {
        return contextCreated;
    }

    public EngineConfig getConfig() {
        return config;
    }
}