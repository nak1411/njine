package com.nak.engine.render;

import com.nak.engine.config.EngineConfig;
import com.nak.engine.core.Module;
import com.nak.engine.events.EventBus;
import com.nak.engine.events.events.WindowResizeEvent;
import com.nak.engine.events.events.OpenGLContextReadyEvent;
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

            // Mark context as created
            contextCreated = true;
            initialized = true;

            // Fire event to notify all modules that OpenGL context is ready
            notifyOpenGLContextReady();

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

        // Debug context for development
        if (config.isDebugMode()) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }

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

            // Update OpenGL viewport if context is ready
            if (contextCreated) {
                glViewport(0, 0, width, height);
            }

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

        // Additional callbacks for input handling
        setupInputCallbacks();
    }

    private void setupInputCallbacks() {
        // Key callback - simplified without calling InputModule methods that don't exist
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            // Input handling can be implemented here or delegated to InputModule
            // For now, just basic logging
            if (action == GLFW_PRESS) {
                System.out.println("Key pressed: " + key);
            }
        });

        // Mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            // Basic mouse handling
            if (action == GLFW_PRESS) {
                System.out.println("Mouse button pressed: " + button);
            }
        });

        // Cursor position callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            // Mouse movement handling
        });

        // Scroll callback
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            // Scroll handling
            System.out.println("Scroll: " + xoffset + ", " + yoffset);
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

        glfwFocusWindow(window);
    }

    private void initializeOpenGL() {
        try {
            // Create OpenGL capabilities - CRITICAL for LWJGL
            GL.createCapabilities();

            // ADD THIS: Verify context is working
            String version = glGetString(GL_VERSION);
            if (version == null) {
                throw new RuntimeException("OpenGL context failed to initialize");
            }
            System.out.println("OpenGL Context Verified: " + version);


            // ADD THIS: Set visible clear color immediately
            glClearColor(0.2f, 0.4f, 0.6f, 1.0f);

            // Verify context is working
            String glVersion = glGetString(GL_VERSION);
            String glRenderer = glGetString(GL_RENDERER);
            String glVendor = glGetString(GL_VENDOR);

            if (glVersion == null) {
                throw new RuntimeException("Failed to create OpenGL context");
            }

            // Log OpenGL info
            System.out.println("OpenGL Version: " + glVersion);
            System.out.println("Graphics Card: " + glRenderer);
            System.out.println("OpenGL Vendor: " + glVendor);

            // Set initial viewport
            glViewport(0, 0, windowWidth, windowHeight);

            // Basic OpenGL setup that's safe to do immediately
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glClearColor(0.1f, 0.1f, 0.2f, 1.0f);

            System.out.println("OpenGL context initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize OpenGL context: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("OpenGL context initialization failed", e);
        }
    }

    private void notifyOpenGLContextReady() {
        try {
            // Get OpenGL info for the event
            String glVersion = glGetString(GL_VERSION);
            String glRenderer = glGetString(GL_RENDERER);
            String glVendor = glGetString(GL_VENDOR);

            // Create and fire the event
            OpenGLContextReadyEvent event = new OpenGLContextReadyEvent(
                    window, glVersion, glRenderer, glVendor, windowWidth, windowHeight
            );

            System.out.println("Broadcasting OpenGL context ready event...");
            eventBus.postImmediate(event);
            eventBus.post(event);

            // Give the event bus time to process
            eventBus.processEvents();

            System.out.println("OpenGL context notification complete");

        } catch (Exception e) {
            System.err.println("Failed to notify about OpenGL context: " + e.getMessage());
            e.printStackTrace();
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
        if (initialized && contextCreated) {
            glfwSwapBuffers(window);
        }
    }

    public void setShouldClose(boolean shouldClose) {
        if (window != NULL) {
            glfwSetWindowShouldClose(window, shouldClose);
        }
    }

    public boolean shouldClose() {
        return initialized && glfwWindowShouldClose(window);
    }

    @Override
    public void cleanup() {
        try {
            if (window != NULL) {
                glfwDestroyWindow(window);
                window = NULL;
            }

            glfwTerminate();
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null) {
                callback.free();
            }

            contextCreated = false;
            initialized = false;

            System.out.println("Window module cleaned up");

        } catch (Exception e) {
            System.err.println("Error cleaning up window module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Getters
    public long getWindowHandle() { return window; }
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public boolean isContextCreated() { return contextCreated; }
}