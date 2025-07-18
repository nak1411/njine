package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.input.InputHandler;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long window;
    private boolean running = false;

    // Timing variables
    private static final double TARGET_FPS = 60.0;
    private static final double TARGET_UPS = 60.0; // Updates per second
    private static final double NS_PER_UPDATE = 1_000_000_000.0 / TARGET_UPS;
    private static final double NS_PER_FRAME = 1_000_000_000.0 / TARGET_FPS;

    // Performance tracking
    private int fps = 0;
    private int ups = 0;
    private long lastSecond = 0;
    private int frameCount = 0;
    private int updateCount = 0;

    // Game state
    private GameState gameState;
    private MasterRenderer renderer;
    private InputHandler inputHandler;
    private Camera camera;
    private Matrix4f projectionMatrix;
    private FloatBuffer matrixBuffer;
    private TerrainManager terrainManager;

    public void init() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        //glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        //glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        //glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Create the window
        window = glfwCreateWindow(1920, 1080, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Set up a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Setup mouse callback for camera
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (camera != null) {
                camera.processMouse(xpos, ypos);
            }
        });

        // Hide and capture cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Get the thread stack and push a new frame
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window

            // Center window
            long monitor = glfwGetPrimaryMonitor();
            glfwGetMonitorWorkarea(monitor, null, null, pWidth, pHeight);
            glfwSetWindowPos(window,
                    (pWidth.get(0) - 1920) / 2,
                    (pHeight.get(0) - 1080) / 2);
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync(0 for manual limiting)
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create matrix buffer for OpenGL
        matrixBuffer = MemoryUtil.memAllocFloat(16);

        // Setup perspective projection using JOML
        projectionMatrix = new Matrix4f();
        projectionMatrix.perspective((float) Math.toRadians(45.0f), 1920.0f / 1080.0f, 0.1f, 1000.0f);

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(projectionMatrix.get(matrixBuffer));
        glMatrixMode(GL_MODELVIEW);

        // Initialize game components
        camera = new Camera();
        gameState = new GameState();
        terrainManager = new TerrainManager();
        renderer = new MasterRenderer(terrainManager);
        inputHandler = new InputHandler(window, camera);

        running = true;
    }

    public void run() {
        init();

        long lastTime = System.nanoTime();
        double accumulator = 0.0;
        long frameTimer = System.nanoTime();

        while (!glfwWindowShouldClose(window) && running) {
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastTime) / 1_000_000_000.0; // Convert to seconds
            lastTime = currentTime;

            // Cap delta time to prevent spiral of death
            deltaTime = Math.min(deltaTime, 0.25);

            accumulator += deltaTime * 1_000_000_000.0; // Convert back to nanoseconds

            // Handle input
            inputHandler.processInput((float) (NS_PER_UPDATE / 1_000_000_000.0));

            // Fixed timestep updates
            while (accumulator >= NS_PER_UPDATE) {
                update((float) (NS_PER_UPDATE / 1_000_000_000.0));
                accumulator -= NS_PER_UPDATE;
                updateCount++;
            }

            // Interpolation factor for smooth rendering
            float interpolation = (float) (accumulator / NS_PER_UPDATE);

            // Render with interpolation
            render(interpolation);
            frameCount++;

            // Frame limiting
            long frameTime = System.nanoTime() - frameTimer;
            if (frameTime < NS_PER_FRAME) {
                try {
                    Thread.sleep((long) ((NS_PER_FRAME - frameTime) / 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            frameTimer = System.nanoTime();

            // Update performance counters
            updatePerformanceCounters(currentTime);

            // Poll events
            glfwPollEvents();
        }

        cleanup();
    }

    private void update(float deltaTime) {
        camera.update(deltaTime);
        terrainManager.update(camera.getPosition(), deltaTime);
        gameState.update(deltaTime);
    }

    private void render(float interpolation) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glLoadIdentity();
        camera.applyTransform(matrixBuffer);

        renderer.render(gameState, camera, interpolation);

        glfwSwapBuffers(window);
    }

    private void updatePerformanceCounters(long currentTime) {
        if (currentTime - lastSecond >= 1_000_000_000L) {
            fps = frameCount;
            ups = updateCount;
            frameCount = 0;
            updateCount = 0;
            lastSecond = currentTime;

            // Update window title with performance info
            glfwSetWindowTitle(window, String.format("Game Loop - FPS: %d, UPS: %d", fps, ups));
        }
    }

    private void cleanup() {
        terrainManager.cleanup();
        renderer.cleanup();

        if (matrixBuffer != null) {
            MemoryUtil.memFree(matrixBuffer);
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
