package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer {

    // Rendering components
    private final TerrainManager terrainManager;
    private final AtmosphericRenderer atmosphericRenderer;
    private final ParticleRenderer particleRenderer;
    private final SkyRenderer skyRenderer;
    private final PostProcessor postProcessor;
    private final UIRenderer uiRenderer;

    // Buffers and matrices
    private final FloatBuffer matrixBuffer;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f modelMatrix;

    // Lighting system
    private final LightingSystem lightingSystem;
    private float dayNightCycle = 0.0f;
    private final Vector3f sunDirection = new Vector3f();
    private final Vector3f sunColor = new Vector3f();
    private final Vector3f ambientColor = new Vector3f();

    // Rendering settings
    private boolean wireframeEnabled = false;
    private boolean debugRenderingEnabled = false;
    private float fogDensity = 0.008f;
    private final Vector3f fogColor = new Vector3f(0.5f, 0.6f, 0.7f);

    // Performance tracking
    private int trianglesRendered = 0;
    private int drawCalls = 0;
    private long renderTimeNanos = 0;
    private final RenderStats renderStats = new RenderStats();

    // Animated objects
    private final List<AnimatedObject> animatedObjects;
    private final ObjectRenderer objectRenderer;

    public MasterRenderer(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;

        // Initialize rendering components
        this.atmosphericRenderer = new AtmosphericRenderer();
        this.particleRenderer = new ParticleRenderer();
        this.skyRenderer = new SkyRenderer();
        this.postProcessor = new PostProcessor();
        this.uiRenderer = new UIRenderer();
        this.objectRenderer = new ObjectRenderer();

        // Initialize matrices and buffers
        this.matrixBuffer = BufferUtils.createFloatBuffer(16);
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();

        // Initialize lighting
        this.lightingSystem = new LightingSystem();

        // Initialize animated objects
        this.animatedObjects = new ArrayList<>();
        initializeAnimatedObjects();

        // Configure OpenGL state
        setupOpenGLState();
    }

    private void setupOpenGLState() {
        // Enable depth testing with optimizations
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);

        // Enable back-face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        // Enable blending for transparent objects
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Configure fog
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_EXP2);

        // Set clear color
        glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
    }

    private void initializeAnimatedObjects() {
        // Create some animated objects for demonstration
        for (int i = 0; i < 12; i++) {
            AnimatedObject obj = new AnimatedObject();
            obj.basePosition.set(
                    (float) (Math.random() - 0.5) * 80,
                    5.0f + (float) Math.random() * 10,
                    (float) (Math.random() - 0.5) * 80
            );
            obj.animationSpeed = 0.5f + (float) Math.random() * 1.5f;
            obj.animationOffset = (float) ((float) Math.random() * Math.PI * 2);
            obj.color.set(
                    0.3f + (float) Math.random() * 0.7f,
                    0.3f + (float) Math.random() * 0.7f,
                    0.3f + (float) Math.random() * 0.7f
            );
            animatedObjects.add(obj);
        }
    }

    /**
     * Main render method
     */
    public void render(GameState gameState, Camera camera, float interpolation) {
        long startTime = System.nanoTime();

        // Reset performance counters
        trianglesRendered = 0;
        drawCalls = 0;

        // Update time-based effects
        updateTimeBasedEffects(gameState.getTime());

        // Update lighting system
        lightingSystem.update(dayNightCycle, sunDirection, sunColor, ambientColor);

        // Setup matrices
        setupMatrices(camera);

        // Begin frame
        beginFrame();

        // Render in order: sky -> terrain -> objects -> particles -> UI
        renderSky(camera);
        renderTerrain(camera);
        renderAnimatedObjects(gameState);
        renderParticles(camera, gameState.getTime());
        renderAtmosphericEffects(camera);

        // Post-processing
        if (postProcessor.isEnabled()) {
            postProcessor.process();
        }

        // Render UI overlay
        renderUI(camera, gameState);

        // End frame
        endFrame();

        // Update performance metrics
        renderTimeNanos = System.nanoTime() - startTime;
        updateRenderStats();
    }

    private void updateTimeBasedEffects(float time) {
        // Update day/night cycle (24 minute cycle = 1440 seconds)
        dayNightCycle += 0.001f;
        if (dayNightCycle > 2 * Math.PI) {
            dayNightCycle -= 2 * Math.PI;
        }

        // Calculate sun position
        float sunHeight = (float) Math.sin(dayNightCycle);
        sunDirection.set(
                (float) Math.cos(dayNightCycle + Math.PI / 2) * 0.6f,
                sunHeight,
                (float) Math.sin(dayNightCycle + Math.PI / 2) * 0.3f
        ).normalize();

        // Calculate lighting colors
        calculateLightingColors(sunHeight);

        // Update fog based on time of day
        updateFogSettings(sunHeight);
    }

    private void calculateLightingColors(float sunHeight) {
        if (sunHeight > 0) {
            // Daytime lighting
            float intensity = Math.min(1.0f, sunHeight * 2.0f);
            sunColor.set(
                    1.0f,
                    0.95f + intensity * 0.05f,
                    0.8f + intensity * 0.2f
            );
            ambientColor.set(
                    0.3f + intensity * 0.3f,
                    0.35f + intensity * 0.35f,
                    0.5f + intensity * 0.3f
            );
        } else {
            // Nighttime lighting
            float moonlight = Math.max(0, -sunHeight * 0.5f);
            sunColor.set(
                    0.4f + moonlight * 0.2f,
                    0.4f + moonlight * 0.2f,
                    0.6f + moonlight * 0.3f
            );
            ambientColor.set(
                    0.05f + moonlight * 0.1f,
                    0.05f + moonlight * 0.1f,
                    0.15f + moonlight * 0.2f
            );
        }
    }

    private void updateFogSettings(float sunHeight) {
        if (sunHeight > 0) {
            // Clear day
            fogDensity = 0.005f + (1.0f - sunHeight) * 0.003f;
            fogColor.set(0.6f, 0.7f, 0.9f);
        } else {
            // Night fog
            fogDensity = 0.012f;
            fogColor.set(0.1f, 0.1f, 0.2f);
        }
    }

    private void setupMatrices(Camera camera) {
        // Get matrices from camera
        projectionMatrix.set(camera.getProjectionMatrix());
        viewMatrix.set(camera.getViewMatrix());

        // Set OpenGL matrices for legacy rendering
        glMatrixMode(GL_PROJECTION);
        projectionMatrix.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);

        glMatrixMode(GL_MODELVIEW);
        viewMatrix.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    private void beginFrame() {
        // Clear buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Set wireframe mode if enabled
        if (wireframeEnabled) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        // Setup fog
        glFogf(GL_FOG_DENSITY, fogDensity);
        float[] fogColorArray = {fogColor.x, fogColor.y, fogColor.z, 1.0f};
        glFogfv(GL_FOG_COLOR, fogColorArray);
    }

    private void renderSky(Camera camera) {
        glDisable(GL_DEPTH_TEST);
        skyRenderer.render(camera, dayNightCycle, sunDirection, sunColor);
        glEnable(GL_DEPTH_TEST);
        drawCalls++;
    }

    private void renderTerrain(Camera camera) {
        // Configure terrain rendering
        lightingSystem.applyTerrainLighting();

        // Render terrain
        terrainManager.render();
        drawCalls++;

        // Add terrain stats
        trianglesRendered += estimateTerrainTriangles();
    }

    private int estimateTerrainTriangles() {
        // Rough estimate based on visible chunks
        return terrainManager.getVisibleChunkCount() * 2048; // Assume ~2k triangles per chunk
    }

    private void renderAnimatedObjects(GameState gameState) {
        lightingSystem.applyObjectLighting();

        for (AnimatedObject obj : animatedObjects) {
            glPushMatrix();

            // Calculate animated position
            Vector3f pos = obj.getAnimatedPosition(gameState.getTime());
            glTranslatef(pos.x, pos.y, pos.z);

            // Calculate animated rotation
            Vector3f rotation = obj.getAnimatedRotation(gameState.getTime());
            glRotatef(rotation.x, 1, 0, 0);
            glRotatef(rotation.y, 0, 1, 0);
            glRotatef(rotation.z, 0, 0, 1);

            // Set color
            glColor3f(obj.color.x, obj.color.y, obj.color.z);

            // Render object
            objectRenderer.renderCube();
            trianglesRendered += 12; // Cube has 12 triangles

            glPopMatrix();
        }
        drawCalls += animatedObjects.size();
    }

    private void renderParticles(Camera camera, float time) {
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glDepthMask(false);

        particleRenderer.render(camera, time);
        drawCalls++;

        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_LIGHTING);
    }

    private void renderAtmosphericEffects(Camera camera) {
        atmosphericRenderer.render(camera, sunDirection, sunColor, fogColor, fogDensity);
        drawCalls++;
    }

    private void renderUI(Camera camera, GameState gameState) {
        // Disable depth testing for UI
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);

        // Setup 2D projection
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, 1920, 1080, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Render UI elements
        uiRenderer.renderCrosshair();

        if (debugRenderingEnabled) {
            uiRenderer.renderDebugInfo(getDebugInfo(camera));
        }

        // Restore matrices
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        // Re-enable depth testing
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);

        drawCalls++;
    }

    private void endFrame() {
        // Reset polygon mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // Flush OpenGL commands
        glFlush();
    }

    private void updateRenderStats() {
        renderStats.update(trianglesRendered, drawCalls, renderTimeNanos);
    }

    private String getDebugInfo(Camera camera) {
        Vector3f pos = camera.getPosition();
        Vector3f vel = camera.getVelocity();

        return String.format(
                "=== RENDER DEBUG ===\n" +
                        "Triangles: %,d\n" +
                        "Draw Calls: %d\n" +
                        "Render Time: %.2f ms\n" +
                        "FPS: %.1f\n" +
                        "%s\n" +
                        "Position: %.1f, %.1f, %.1f\n" +
                        "Velocity: %.2f\n" +
                        "FOV: %.1f°\n" +
                        "Day/Night: %.1f°\n" +
                        "Fog Density: %.4f",
                trianglesRendered,
                drawCalls,
                renderTimeNanos / 1_000_000.0,
                renderStats.getAverageFPS(),
                terrainManager.getPerformanceInfo(),
                pos.x, pos.y, pos.z,
                vel.length(),
                camera.getFov(),
                Math.toDegrees(dayNightCycle),
                fogDensity
        );
    }

    /**
     * Set rendering options
     */
    public void setWireframeEnabled(boolean enabled) {
        this.wireframeEnabled = enabled;
    }

    public void setDebugRenderingEnabled(boolean enabled) {
        this.debugRenderingEnabled = enabled;
    }

    public void setFogDensity(float density) {
        this.fogDensity = Math.max(0.0f, Math.min(1.0f, density));
    }

    /**
     * Camera shake effect
     */
    public void shake(float intensity, float duration) {
        // This would typically be handled by the camera
        // but can also add screen-space shake effects here
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        try {
            atmosphericRenderer.cleanup();
            particleRenderer.cleanup();
            skyRenderer.cleanup();
            postProcessor.cleanup();
            uiRenderer.cleanup();
            objectRenderer.cleanup();
            lightingSystem.cleanup();

            if (matrixBuffer != null) {
                org.lwjgl.system.MemoryUtil.memFree(matrixBuffer);
            }
        } catch (Exception e) {
            System.err.println("Error during renderer cleanup: " + e.getMessage());
        }
    }

    // Getters
    public boolean isWireframeEnabled() {
        return wireframeEnabled;
    }

    public boolean isDebugRenderingEnabled() {
        return debugRenderingEnabled;
    }

    public float getFogDensity() {
        return fogDensity;
    }

    public int getTrianglesRendered() {
        return trianglesRendered;
    }

    public int getDrawCalls() {
        return drawCalls;
    }

    public RenderStats getRenderStats() {
        return renderStats;
    }

    /**
     * Animated object class
     */
    private static class AnimatedObject {
        public final Vector3f basePosition = new Vector3f();
        public final Vector3f color = new Vector3f(1, 1, 1);
        public float animationSpeed = 1.0f;
        public float animationOffset = 0.0f;

        public Vector3f getAnimatedPosition(float time) {
            float animTime = time * animationSpeed + animationOffset;
            return new Vector3f(
                    basePosition.x + (float) Math.sin(animTime * 0.7f) * 3.0f,
                    basePosition.y + (float) Math.sin(animTime * 2.0f) * 2.0f,
                    basePosition.z + (float) Math.cos(animTime * 0.5f) * 3.0f
            );
        }

        public Vector3f getAnimatedRotation(float time) {
            float animTime = time * animationSpeed + animationOffset;
            return new Vector3f(
                    animTime * 45.0f,
                    animTime * 30.0f,
                    animTime * 60.0f
            );
        }
    }

    /**
     * Lighting system
     */
    private static class LightingSystem {
        public void update(float dayNightCycle, Vector3f sunDirection, Vector3f sunColor, Vector3f ambientColor) {
            try {
                glEnable(GL_LIGHTING);
                glEnable(GL_LIGHT0);

                // Set light position (directional light)
                float[] lightPos = {
                        sunDirection.x * 1000,
                        sunDirection.y * 1000,
                        sunDirection.z * 1000,
                        0.0f // Directional light
                };
                glLightfv(GL_LIGHT0, GL_POSITION, lightPos);

                // Set light colors
                float[] diffuse = {sunColor.x, sunColor.y, sunColor.z, 1.0f};
                float[] specular = {sunColor.x * 0.8f, sunColor.y * 0.8f, sunColor.z * 0.8f, 1.0f};
                float[] ambient = {ambientColor.x, ambientColor.y, ambientColor.z, 1.0f};

                glLightfv(GL_LIGHT0, GL_DIFFUSE, diffuse);
                glLightfv(GL_LIGHT0, GL_SPECULAR, specular);
                glLightModelfv(GL_LIGHT_MODEL_AMBIENT, ambient);

            } catch (Exception e) {
                System.err.println("Error updating lighting: " + e.getMessage());
                glDisable(GL_LIGHTING);
            }
        }

        public void applyTerrainLighting() {
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            float[] specular = {0.2f, 0.2f, 0.2f, 1.0f};
            glMaterialfv(GL_FRONT, GL_SPECULAR, specular);
            glMaterialf(GL_FRONT, GL_SHININESS, 16.0f);
        }

        public void applyObjectLighting() {
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            float[] specular = {0.5f, 0.5f, 0.5f, 1.0f};
            glMaterialfv(GL_FRONT, GL_SPECULAR, specular);
            glMaterialf(GL_FRONT, GL_SHININESS, 64.0f);
        }

        public void cleanup() {
            glDisable(GL_LIGHTING);
            glDisable(GL_COLOR_MATERIAL);
        }
    }

    /**
     * Performance statistics tracking
     */
    public static class RenderStats {
        private static final int SAMPLE_SIZE = 60;
        private final long[] frameTimes = new long[SAMPLE_SIZE];
        private final int[] triangleCounts = new int[SAMPLE_SIZE];
        private final int[] drawCallCounts = new int[SAMPLE_SIZE];
        private int sampleIndex = 0;
        private int sampleCount = 0;

        public void update(int triangles, int drawCalls, long renderTimeNanos) {
            frameTimes[sampleIndex] = renderTimeNanos;
            triangleCounts[sampleIndex] = triangles;
            drawCallCounts[sampleIndex] = drawCalls;

            sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;
            sampleCount = Math.min(sampleCount + 1, SAMPLE_SIZE);
        }

        public double getAverageFrameTime() {
            if (sampleCount == 0) return 0;

            long sum = 0;
            for (int i = 0; i < sampleCount; i++) {
                sum += frameTimes[i];
            }
            return (double) sum / sampleCount / 1_000_000.0; // Convert to milliseconds
        }

        public double getAverageFPS() {
            double frameTime = getAverageFrameTime();
            return frameTime > 0 ? 1000.0 / frameTime : 0;
        }

        public double getAverageTriangles() {
            if (sampleCount == 0) return 0;

            long sum = 0;
            for (int i = 0; i < sampleCount; i++) {
                sum += triangleCounts[i];
            }
            return (double) sum / sampleCount;
        }

        public double getAverageDrawCalls() {
            if (sampleCount == 0) return 0;

            long sum = 0;
            for (int i = 0; i < sampleCount; i++) {
                sum += drawCallCounts[i];
            }
            return (double) sum / sampleCount;
        }
    }
}