package com.nak.engine.render;

import com.nak.engine.debug.TerrainDebugUtility;
import com.nak.engine.entity.Camera;
import com.nak.engine.input.InputHandler;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private long window;
    private boolean running = false;
    private int windowWidth = 1920;
    private int windowHeight = 1080;

    // Timing variables
    private static final double TARGET_FPS = 60.0;
    private static final double TARGET_UPS = 60.0;
    private static final double NS_PER_UPDATE = 1_000_000_000.0 / TARGET_UPS;
    private static final double NS_PER_FRAME = 1_000_000_000.0 / TARGET_FPS;

    // Performance tracking
    private int fps = 0;
    private int ups = 0;
    private long lastSecond = 0;
    private int frameCount = 0;
    private int updateCount = 0;
    private long totalFrameTime = 0;
    private long maxFrameTime = 0;

    // Game components
    private GameState gameState;
    private MasterRenderer renderer;
    private InputHandler inputHandler;
    private Camera camera;
    private Matrix4f projectionMatrix;
    private FloatBuffer matrixBuffer;
    private TerrainManager terrainManager;

    private TerrainDebugUtility.TerrainLoadMonitor terrainMonitor;
    private boolean terrainDebugEnabled = false;
    private long lastTerrainDebugTime = 0;

    // Rendering settings
    private boolean vsyncEnabled = true;
    private boolean fullscreen = false;
    private int msaaSamples = 4;

    public void init() {
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW with modern OpenGL context
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        //glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        //glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Anti-aliasing
        glfwWindowHint(GLFW_SAMPLES, msaaSamples);

        // Create window
        window = glfwCreateWindow(windowWidth, windowHeight, "Enhanced Terrain Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        setupCallbacks();
        setupWindow();
        initializeOpenGL();
        initializeGameComponents();

        running = true;
        System.out.println("Enhanced Terrain Engine initialized successfully!");
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("Graphics Card: " + glGetString(GL_RENDERER));
    }

    private void setupCallbacks() {
        // Window size callback
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            this.windowWidth = width;
            this.windowHeight = height;
            glViewport(0, 0, width, height);

            // Update projection matrix
            updateProjectionMatrix();

            // Update camera aspect ratio
            camera.setAspectRatio((float) width / (float) height);
        });

        // Window focus callback
        glfwSetWindowFocusCallback(window, (window, focused) -> {
            if (inputHandler != null) {
                inputHandler.onWindowFocus(focused);
            }
        });

        // Cursor position callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (camera != null && inputHandler != null && inputHandler.isMouseLocked()) {
                camera.processMouse(xpos, ypos);
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

            glfwSetWindowPos(window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2);
        }

        // Make OpenGL context current
        glfwMakeContextCurrent(window);

        // Set v-sync
        glfwSwapInterval(vsyncEnabled ? 1 : 0);

        // Show window
        glfwShowWindow(window);
    }

    private void initializeOpenGL() {
        GL.createCapabilities();

        // Enable features
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // Check for OpenGL errors after each call
        checkGLError("After basic setup");

        // MSAA setup with error checking
        if (msaaSamples > 0) {
            try {
                glEnable(GL_MULTISAMPLE);
                checkGLError("After MSAA enable");
            } catch (Exception e) {
                System.err.println("MSAA not supported, disabling");
                msaaSamples = 0;
            }
        }

        // Set clear color
        glClearColor(0.05f, 0.05f, 0.1f, 1.0f);

        // Create matrix buffer
        matrixBuffer = MemoryUtil.memAllocFloat(16);

        // Setup projection matrix
        updateProjectionMatrix();
    }

    private void updateProjectionMatrix() {
        projectionMatrix = new Matrix4f();
        projectionMatrix.perspective(
                (float) Math.toRadians(45.0f),
                (float) windowWidth / (float) windowHeight,
                0.1f,
                1000.0f
        );

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(projectionMatrix.get(matrixBuffer));
        glMatrixMode(GL_MODELVIEW);
    }

    private void initializeGameComponents() {
        // Initialize game components
        camera = new Camera();
        camera.setAspectRatio((float) windowWidth / (float) windowHeight);

        gameState = new GameState();
        terrainManager = new TerrainManager();

        renderer = new MasterRenderer(terrainManager);
        inputHandler = new InputHandler(window, camera);

        // Initialize terrain debugging
        terrainMonitor = new TerrainDebugUtility.TerrainLoadMonitor();
        terrainDebugEnabled = Boolean.parseBoolean(System.getProperty("terrain.debug", "false"));

        System.out.println("Game components initialized");
        System.out.println(inputHandler.getControlsHelp());

        // Run initial terrain diagnostics
        if (terrainDebugEnabled) {
            TerrainDebugUtility.debugTerrainSystem(terrainManager, camera.getPosition());
        }
    }

    public void run() {
        init();

        long lastTime = System.nanoTime();
        double accumulator = 0.0;
        long frameTimer = System.nanoTime();

        System.out.println("Starting main loop...");

        while (!glfwWindowShouldClose(window) && running) {
            long frameStart = System.nanoTime();
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
            lastTime = currentTime;

            // Cap delta time to prevent spiral of death
            deltaTime = Math.min(deltaTime, 0.25);
            accumulator += deltaTime * 1_000_000_000.0;

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

            // Render
            render(interpolation);
            frameCount++;

            // Calculate frame time
            long frameEnd = System.nanoTime();
            long frameTime = frameEnd - frameStart;
            totalFrameTime += frameTime;
            maxFrameTime = Math.max(maxFrameTime, frameTime);

            // Frame limiting (only if vsync is disabled)
            if (!vsyncEnabled) {
                if (frameTime < NS_PER_FRAME) {
                    try {
                        Thread.sleep((long) ((NS_PER_FRAME - frameTime) / 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // Update performance counters
            updatePerformanceCounters(currentTime);

            // Poll events
            glfwPollEvents();
        }

        cleanup();
        System.out.println("Application terminated successfully");
    }

    private void update(float deltaTime) {
        // Update game state
        gameState.update(deltaTime);

        // Update camera
        camera.update(deltaTime);

        // Update terrain with camera position and debug monitoring
        Vector3f cameraPos = camera.getPosition();

        // Validate camera position before updating terrain
        if (!Float.isFinite(cameraPos.x) || !Float.isFinite(cameraPos.y) || !Float.isFinite(cameraPos.z)) {
            System.err.println("WARNING: Invalid camera position detected: " + cameraPos);
            cameraPos.set(0, 10, 3); // Reset to safe position
            camera.setPosition(cameraPos);
        }

        try {
            terrainManager.update(cameraPos, deltaTime);
        } catch (Exception e) {
            System.err.println("Error updating terrain: " + e.getMessage());
            e.printStackTrace();
        }

        // Monitor terrain loading progress
        if (terrainDebugEnabled) {
            terrainMonitor.update(terrainManager);

            // Periodic detailed diagnostics
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTerrainDebugTime > 10000) { // Every 10 seconds
                TerrainDebugUtility.diagnoseCommonIssues(terrainManager, cameraPos);
                lastTerrainDebugTime = currentTime;
            }
        }

        // Handle special input commands
        handleSpecialCommands();
    }

    private void handleSpecialCommands() {
        // Check for application exit
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS &&
                glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            running = false;
        }

        // Toggle fullscreen
        if (glfwGetKey(window, GLFW_KEY_F11) == GLFW_PRESS) {
            toggleFullscreen();
        }

        // Toggle vsync
        if (glfwGetKey(window, GLFW_KEY_V) == GLFW_PRESS &&
                glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            toggleVSync();
        }

        // Toggle terrain debugging
        if (glfwGetKey(window, GLFW_KEY_F5) == GLFW_PRESS) {
            toggleTerrainDebug();
        }

        // Run terrain stress test
        if (glfwGetKey(window, GLFW_KEY_F6) == GLFW_PRESS &&
                glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) {
            runTerrainStressTest();
        }

        // Test terrain height generation
        if (glfwGetKey(window, GLFW_KEY_F7) == GLFW_PRESS) {
            testTerrainHeight();
        }
    }

    private void toggleTerrainDebug() {
        terrainDebugEnabled = !terrainDebugEnabled;
        System.out.println("Terrain debugging " + (terrainDebugEnabled ? "enabled" : "disabled"));

        if (terrainDebugEnabled) {
            TerrainDebugUtility.debugTerrainSystem(terrainManager, camera.getPosition());
        }
    }

    private void runTerrainStressTest() {
        System.out.println("Running terrain stress test...");
        TerrainDebugUtility.stressTest(terrainManager);
    }

    private void testTerrainHeight() {
        TerrainDebugUtility.testHeightGeneration(terrainManager);
    }

    private void render(float interpolation) {
        // Clear buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Set wireframe mode based on input
        if (inputHandler.isWireframeMode()) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        // Apply camera transformation
        glLoadIdentity();
        camera.applyTransform(matrixBuffer);

        // Render scene
        renderer.render(gameState, camera, interpolation);

        // Render debug information if enabled
        if (inputHandler.isShowDebugInfo()) {
            renderDebugInfo();
        }

        // Swap buffers
        glfwSwapBuffers(window);
    }

    private void renderDebugInfo() {
        // Switch to 2D rendering for debug text
        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Update debug title with terrain info
        updateDebugTitle();

        // Print debug info to console if terrain debugging is enabled
        if (terrainDebugEnabled && System.currentTimeMillis() % 5000 < 100) { // Every 5 seconds, briefly
            System.out.println("=== DEBUG INFO ===");
            System.out.println("Camera: " + camera.getPosition());
            System.out.println(terrainManager.getPerformanceInfo());
            System.out.println("==================");
        }

        // Restore 3D rendering
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_DEPTH_TEST);
    }

    private void updateDebugTitle() {
        String terrainInfo = "";
        if (terrainDebugEnabled) {
            terrainInfo = String.format(" | Terrain: A=%d V=%d R=%d",
                    terrainManager.getActiveChunkCount(),
                    terrainManager.getVisibleChunkCount(),
                    terrainManager.getChunksRendered());
        }

        String title = String.format(
                "Enhanced Terrain Engine - FPS: %d, UPS: %d, Avg: %.2fms, Max: %.2fms, Pos: %.1f,%.1f,%.1f%s",
                fps, ups,
                (totalFrameTime / Math.max(1, frameCount)) / 1_000_000.0,
                maxFrameTime / 1_000_000.0,
                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z,
                terrainInfo
        );
        glfwSetWindowTitle(window, title);
    }

    private void updatePerformanceCounters(long currentTime) {
        if (currentTime - lastSecond >= 1_000_000_000L) {
            fps = frameCount;
            ups = updateCount;

            // Reset counters
            frameCount = 0;
            updateCount = 0;
            totalFrameTime = 0;
            maxFrameTime = 0;
            lastSecond = currentTime;

            // Print performance info to console
            if (inputHandler.isShowDebugInfo()) {
                System.out.printf("Performance: FPS=%d, UPS=%d, Camera: %.1f,%.1f,%.1f%n",
                        fps, ups, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
            }
        }
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            // Get primary monitor
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);

            // Store window position and size for restoration
            glfwSetWindowMonitor(window, monitor, 0, 0,
                    vidMode.width(), vidMode.height(), vidMode.refreshRate());
        } else {
            // Return to windowed mode
            glfwSetWindowMonitor(window, NULL, 100, 100,
                    windowWidth, windowHeight, GLFW_DONT_CARE);
        }
    }

    private void toggleVSync() {
        vsyncEnabled = !vsyncEnabled;
        glfwSwapInterval(vsyncEnabled ? 1 : 0);
        System.out.println("V-Sync " + (vsyncEnabled ? "enabled" : "disabled"));
    }

    private void checkGLError(String operation) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "Unknown error " + error;
                    break;
            }
            throw new RuntimeException("OpenGL error during " + operation + ": " + errorString);
        }
    }

    private void cleanup() {
        System.out.println("Cleaning up resources...");

        // Cleanup game components
        if (terrainManager != null) terrainManager.cleanup();
        if (renderer != null) renderer.cleanup();

        // Free memory
        if (matrixBuffer != null) {
            MemoryUtil.memFree(matrixBuffer);
        }

        // Destroy window and terminate GLFW
        glfwDestroyWindow(window);
        glfwTerminate();

        // Free error callback
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    // Getters for external access
    public long getWindow() {
        return window;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isVSyncEnabled() {
        return vsyncEnabled;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public int getFPS() {
        return fps;
    }

    public int getUPS() {
        return ups;
    }

    // Setters
    public void setRunning(boolean running) {
        this.running = running;
    }
}
