package com.nak.engine.render;

import com.nak.engine.entity.Camera;
import com.nak.engine.state.GameState;
import com.nak.engine.terrain.TerrainManager;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer {

    private FloatBuffer materialBuffer;
    private TerrainManager terrainManager;
    private AtmosphericEffects atmosphericEffects;
    private ParticleSystem particleSystem;
    private SkyRenderer skyRenderer;
    private List<Vector3f> cloudPositions;
    private float dayNightCycle = 0.0f;

    public MasterRenderer(TerrainManager terrainManager) {
        this.terrainManager = terrainManager;
        this.atmosphericEffects = new AtmosphericEffects();
        this.particleSystem = new ParticleSystem();
        this.skyRenderer = new SkyRenderer();
        this.materialBuffer = BufferUtils.createFloatBuffer(4);
        initializeClouds();
    }

    private void initializeClouds() {
        cloudPositions = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            cloudPositions.add(new Vector3f(
                    (float) (Math.random() - 0.5) * 200,
                    20 + (float) Math.random() * 15,
                    (float) (Math.random() - 0.5) * 200
            ));
        }
    }

    public void render(GameState gameState, Camera camera, float interpolation) {
        // Update day/night cycle
        dayNightCycle += 0.005f;
        if (dayNightCycle > 2 * Math.PI) dayNightCycle -= 2 * Math.PI;

        // Calculate sun position and lighting
        Vector3f sunDirection = calculateSunDirection(dayNightCycle);
        Vector3f sunColor = calculateSunColor(dayNightCycle);
        Vector3f ambientColor = calculateAmbientColor(dayNightCycle);

        // Set global lighting
        setupLighting(sunDirection, sunColor, ambientColor);

        // Render sky first
        skyRenderer.render(camera, dayNightCycle, sunDirection);

        // Enable depth testing for terrain and objects
        glEnable(GL_DEPTH_TEST);

        // Render terrain with enhanced effects
        renderEnhancedTerrainWithBuffers(camera, sunDirection, sunColor, ambientColor);

        // Render atmospheric effects
        atmosphericEffects.render(camera, sunDirection, sunColor);

        // Render clouds
        renderClouds(camera, gameState.getTime());

        // Render particles (rain, snow, etc.)
        particleSystem.update(gameState.getTime());
        particleSystem.render(camera);

        // Render animated objects
        renderAnimatedObjectsWithBuffers(gameState, sunColor, ambientColor);

        // Render UI elements
        renderUI(camera, gameState);
    }

    private Vector3f calculateSunDirection(float dayNightCycle) {
        float x = (float) Math.cos(dayNightCycle) * 0.5f;
        float y = (float) Math.sin(dayNightCycle);
        float z = (float) Math.sin(dayNightCycle * 0.3f) * 0.2f;
        return new Vector3f(x, y, z).normalize();
    }

    private Vector3f calculateSunColor(float dayNightCycle) {
        float sunHeight = (float) Math.sin(dayNightCycle);
        if (sunHeight > 0) {
            // Daytime - transition from orange to white to orange
            float intensity = Math.min(1.0f, sunHeight * 2.0f);
            return new Vector3f(1.0f, 0.9f + intensity * 0.1f, 0.7f + intensity * 0.3f);
        } else {
            // Nighttime - moon light
            return new Vector3f(0.3f, 0.3f, 0.5f);
        }
    }

    private Vector3f calculateAmbientColor(float dayNightCycle) {
        float sunHeight = (float) Math.sin(dayNightCycle);
        if (sunHeight > 0) {
            // Daytime ambient
            float intensity = Math.min(1.0f, sunHeight * 1.5f);
            return new Vector3f(0.4f + intensity * 0.2f, 0.4f + intensity * 0.3f, 0.6f + intensity * 0.2f);
        } else {
            // Nighttime ambient
            return new Vector3f(0.1f, 0.1f, 0.2f);
        }
    }

    private void setupLighting(Vector3f sunDirection, Vector3f sunColor, Vector3f ambientColor) {
        try {
            // Setup OpenGL fixed pipeline lighting
            glEnable(GL_LIGHTING);
            glEnable(GL_LIGHT0);

            // FIXED: Proper array sizes for OpenGL calls
            float[] lightPos = {sunDirection.x * 1000, sunDirection.y * 1000, sunDirection.z * 1000, 0.0f};
            float[] lightColor = {sunColor.x, sunColor.y, sunColor.z, 1.0f};
            float[] ambientLight = {ambientColor.x, ambientColor.y, ambientColor.z, 1.0f};

            glLightfv(GL_LIGHT0, GL_POSITION, lightPos);
            glLightfv(GL_LIGHT0, GL_DIFFUSE, lightColor);
            glLightfv(GL_LIGHT0, GL_SPECULAR, lightColor);
            glLightModelfv(GL_LIGHT_MODEL_AMBIENT, ambientLight);
        } catch (Exception e) {
            System.err.println("Error setting up lighting: " + e.getMessage());
            // Disable lighting on error
            glDisable(GL_LIGHTING);
        }
    }

    private void renderEnhancedTerrain(Camera camera, Vector3f sunDirection, Vector3f sunColor, Vector3f ambientColor) {
        try {
            // Enable material properties
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            // FIXED: Set terrain material properties with proper array sizes
            float[] specular = {0.1f, 0.1f, 0.1f, 1.0f}; // Must be 4 elements
            float[] shininess = {32.0f}; // Can be 1 element for GL_SHININESS

            glMaterialfv(GL_FRONT, GL_SPECULAR, specular);
            glMaterialfv(GL_FRONT, GL_SHININESS, shininess);

            // Render terrain with wireframe overlay for detail
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glColor3f(0.3f, 0.6f, 0.2f); // Base terrain color
            terrainManager.render();

            // Add wireframe overlay for detail
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glColor3f(0.2f, 0.4f, 0.1f); // Darker green for wireframe
            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonOffset(-1.0f, -1.0f);
            terrainManager.render();
            glDisable(GL_POLYGON_OFFSET_LINE);

            // Reset to fill mode
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glDisable(GL_COLOR_MATERIAL);

        } catch (Exception e) {
            System.err.println("Error rendering enhanced terrain: " + e.getMessage());
            // Fallback to simple rendering
            try {
                glColor3f(0.3f, 0.6f, 0.2f);
                terrainManager.render();
            } catch (Exception e2) {
                System.err.println("Even simple terrain rendering failed: " + e2.getMessage());
            }
        }
    }

    // Enhanced terrain rendering with proper buffers:
    private void renderEnhancedTerrainWithBuffers(Camera camera, Vector3f sunDirection, Vector3f sunColor, Vector3f ambientColor) {
        try {
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            // Set specular material using FloatBuffer
            materialBuffer.clear();
            materialBuffer.put(0.1f).put(0.1f).put(0.1f).put(1.0f);
            materialBuffer.flip();
            glMaterialfv(GL_FRONT, GL_SPECULAR, materialBuffer);

            // Set shininess using single value
            glMaterialf(GL_FRONT, GL_SHININESS, 32.0f);

            // Render terrain
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glColor3f(0.3f, 0.6f, 0.2f);
            terrainManager.render();

            // Wireframe overlay
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glColor3f(0.2f, 0.4f, 0.1f);
            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonOffset(-1.0f, -1.0f);
            terrainManager.render();
            glDisable(GL_POLYGON_OFFSET_LINE);

            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glDisable(GL_COLOR_MATERIAL);

        } catch (Exception e) {
            System.err.println("Error rendering enhanced terrain: " + e.getMessage());
            // Fallback
            glColor3f(0.3f, 0.6f, 0.2f);
            terrainManager.render();
        }
    }

    private void renderClouds(Camera camera, float time) {
        try {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_LIGHTING);

            for (int i = 0; i < cloudPositions.size(); i++) {
                Vector3f cloudPos = cloudPositions.get(i);

                // Move clouds slowly
                cloudPos.x += 0.01f * Math.sin(time * 0.1f + i);
                cloudPos.z += 0.005f * Math.cos(time * 0.15f + i);

                // Wrap around world
                if (cloudPos.x > 100) cloudPos.x = -100;
                if (cloudPos.x < -100) cloudPos.x = 100;
                if (cloudPos.z > 100) cloudPos.z = -100;
                if (cloudPos.z < -100) cloudPos.z = 100;

                renderCloud(cloudPos, time + i, camera);
            }

            glDisable(GL_BLEND);
            glEnable(GL_LIGHTING);
        } catch (Exception e) {
            System.err.println("Error rendering clouds: " + e.getMessage());
        }
    }

    private void renderCloud(Vector3f position, float time, Camera camera) {
        try {
            glPushMatrix();
            glTranslatef(position.x, position.y, position.z);

            // Simple cloud rendering with multiple spheres
            glColor4f(0.9f, 0.9f, 0.9f, 0.6f);

            for (int i = 0; i < 5; i++) {
                glPushMatrix();
                float offsetX = (float) Math.sin(time * 0.5f + i) * 2.0f;
                float offsetY = (float) Math.cos(time * 0.3f + i) * 0.5f;
                float offsetZ = (float) Math.sin(time * 0.4f + i * 0.7f) * 1.5f;

                glTranslatef(offsetX, offsetY, offsetZ);
                renderSphere(1.5f + (float) Math.sin(time + i) * 0.3f);
                glPopMatrix();
            }

            glPopMatrix();
        } catch (Exception e) {
            System.err.println("Error rendering individual cloud: " + e.getMessage());
        }
    }

    private void renderSphere(float radius) {
        try {
            // Simple sphere approximation using quads
            int segments = 12;
            for (int i = 0; i < segments; i++) {
                float theta1 = (float) (i * 2 * Math.PI / segments);
                float theta2 = (float) ((i + 1) * 2 * Math.PI / segments);

                glBegin(GL_TRIANGLE_STRIP);
                for (int j = 0; j <= segments; j++) {
                    float phi = (float) (j * Math.PI / segments);

                    float x1 = (float) (radius * Math.cos(theta1) * Math.sin(phi));
                    float y1 = (float) (radius * Math.cos(phi));
                    float z1 = (float) (radius * Math.sin(theta1) * Math.sin(phi));

                    float x2 = (float) (radius * Math.cos(theta2) * Math.sin(phi));
                    float y2 = (float) (radius * Math.cos(phi));
                    float z2 = (float) (radius * Math.sin(theta2) * Math.sin(phi));

                    glVertex3f(x1, y1, z1);
                    glVertex3f(x2, y2, z2);
                }
                glEnd();
            }
        } catch (Exception e) {
            System.err.println("Error rendering sphere: " + e.getMessage());
        }
    }

    private void renderAnimatedObjects(GameState gameState, Vector3f sunColor, Vector3f ambientColor) {
        try {
            // Enhanced animated cubes with better materials
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            // FIXED: Proper array sizes
            float[] specular = {0.5f, 0.5f, 0.5f, 1.0f}; // Must be 4 elements
            float[] shininess = {64.0f}; // 1 element for shininess
            glMaterialfv(GL_FRONT, GL_SPECULAR, specular);
            glMaterialfv(GL_FRONT, GL_SHININESS, shininess);

            for (int i = 0; i < 8; i++) {
                glPushMatrix();

                float x = (float) Math.cos(gameState.getTime() * 0.5f + i * 0.8f) * 5.0f;
                float z = (float) Math.sin(gameState.getTime() * 0.3f + i * 0.9f) * 5.0f;
                float y = (float) Math.sin(gameState.getTime() * 2 + i) * 1.0f + 3.0f;

                glTranslatef(x, y, z);
                glRotatef(gameState.getTime() * 30 + i * 45, 1.0f, 1.0f, 0.0f);

                // Color based on position and time
                float r = 0.5f + 0.5f * (float) Math.sin(gameState.getTime() + i);
                float g = 0.5f + 0.5f * (float) Math.cos(gameState.getTime() * 0.7f + i);
                float b = 0.5f + 0.5f * (float) Math.sin(gameState.getTime() * 1.3f + i);
                glColor3f(r, g, b);

                renderEnhancedCube();
                glPopMatrix();
            }

            glDisable(GL_COLOR_MATERIAL);
        } catch (Exception e) {
            System.err.println("Error rendering animated objects: " + e.getMessage());
        }
    }

    // Enhanced animated objects with proper buffers:
    private void renderAnimatedObjectsWithBuffers(GameState gameState, Vector3f sunColor, Vector3f ambientColor) {
        try {
            glEnable(GL_COLOR_MATERIAL);
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);

            // Set specular material using FloatBuffer
            materialBuffer.clear();
            materialBuffer.put(0.5f).put(0.5f).put(0.5f).put(1.0f);
            materialBuffer.flip();
            glMaterialfv(GL_FRONT, GL_SPECULAR, materialBuffer);

            // Set shininess
            glMaterialf(GL_FRONT, GL_SHININESS, 64.0f);

            for (int i = 0; i < 8; i++) {
                glPushMatrix();

                float x = (float) Math.cos(gameState.getTime() * 0.5f + i * 0.8f) * 5.0f;
                float z = (float) Math.sin(gameState.getTime() * 0.3f + i * 0.9f) * 5.0f;
                float y = (float) Math.sin(gameState.getTime() * 2 + i) * 1.0f + 3.0f;

                glTranslatef(x, y, z);
                glRotatef(gameState.getTime() * 30 + i * 45, 1.0f, 1.0f, 0.0f);

                float r = 0.5f + 0.5f * (float) Math.sin(gameState.getTime() + i);
                float g = 0.5f + 0.5f * (float) Math.cos(gameState.getTime() * 0.7f + i);
                float b = 0.5f + 0.5f * (float) Math.sin(gameState.getTime() * 1.3f + i);
                glColor3f(r, g, b);

                renderEnhancedCube();
                glPopMatrix();
            }

            glDisable(GL_COLOR_MATERIAL);
        } catch (Exception e) {
            System.err.println("Error rendering animated objects: " + e.getMessage());
        }
    }

    private void renderEnhancedCube() {
        try {
            glBegin(GL_QUADS);

            // Front face
            glNormal3f(0.0f, 0.0f, 1.0f);
            glVertex3f(-0.5f, -0.5f, 0.5f);
            glVertex3f(0.5f, -0.5f, 0.5f);
            glVertex3f(0.5f, 0.5f, 0.5f);
            glVertex3f(-0.5f, 0.5f, 0.5f);

            // Back face
            glNormal3f(0.0f, 0.0f, -1.0f);
            glVertex3f(-0.5f, -0.5f, -0.5f);
            glVertex3f(-0.5f, 0.5f, -0.5f);
            glVertex3f(0.5f, 0.5f, -0.5f);
            glVertex3f(0.5f, -0.5f, -0.5f);

            // Top face
            glNormal3f(0.0f, 1.0f, 0.0f);
            glVertex3f(-0.5f, 0.5f, -0.5f);
            glVertex3f(-0.5f, 0.5f, 0.5f);
            glVertex3f(0.5f, 0.5f, 0.5f);
            glVertex3f(0.5f, 0.5f, -0.5f);

            // Bottom face
            glNormal3f(0.0f, -1.0f, 0.0f);
            glVertex3f(-0.5f, -0.5f, -0.5f);
            glVertex3f(0.5f, -0.5f, -0.5f);
            glVertex3f(0.5f, -0.5f, 0.5f);
            glVertex3f(-0.5f, -0.5f, 0.5f);

            // Right face
            glNormal3f(1.0f, 0.0f, 0.0f);
            glVertex3f(0.5f, -0.5f, -0.5f);
            glVertex3f(0.5f, 0.5f, -0.5f);
            glVertex3f(0.5f, 0.5f, 0.5f);
            glVertex3f(0.5f, -0.5f, 0.5f);

            // Left face
            glNormal3f(-1.0f, 0.0f, 0.0f);
            glVertex3f(-0.5f, -0.5f, -0.5f);
            glVertex3f(-0.5f, -0.5f, 0.5f);
            glVertex3f(-0.5f, 0.5f, 0.5f);
            glVertex3f(-0.5f, 0.5f, -0.5f);

            glEnd();
        } catch (Exception e) {
            System.err.println("Error rendering cube: " + e.getMessage());
        }
    }

    private void renderUI(Camera camera, GameState gameState) {
        try {
            // Disable depth testing for UI
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_LIGHTING);

            // Set up orthographic projection for UI
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, 1920, 1080, 0, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();

            // Render crosshair
            glColor3f(1.0f, 1.0f, 1.0f);
            glBegin(GL_LINES);
            glVertex2f(960 - 10, 540);
            glVertex2f(960 + 10, 540);
            glVertex2f(960, 540 - 10);
            glVertex2f(960, 540 + 10);
            glEnd();

            // Restore matrices
            glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);

            // Re-enable depth testing
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_LIGHTING);
        } catch (Exception e) {
            System.err.println("Error rendering UI: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            if (terrainManager != null) terrainManager.cleanup();
            if (atmosphericEffects != null) atmosphericEffects.cleanup();
            if (particleSystem != null) particleSystem.cleanup();
            if (skyRenderer != null) skyRenderer.cleanup();

            // Free the material buffer
            if (materialBuffer != null) {
                org.lwjgl.system.MemoryUtil.memFree(materialBuffer);
            }
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    // Inner classes for atmospheric effects
    private static class AtmosphericEffects {
        public void render(Camera camera, Vector3f sunDirection, Vector3f sunColor) {
            try {
                // Render atmospheric haze, fog, etc.
                renderFog(camera, sunColor);
            } catch (Exception e) {
                System.err.println("Error rendering atmospheric effects: " + e.getMessage());
            }
        }

        private void renderFog(Camera camera, Vector3f sunColor) {
            try {
                // Simple fog effect using OpenGL fog
                glEnable(GL_FOG);
                float[] fogColor = {sunColor.x * 0.5f, sunColor.y * 0.5f, sunColor.z * 0.5f + 0.2f, 1.0f};
                glFogfv(GL_FOG_COLOR, fogColor);
                glFogf(GL_FOG_DENSITY, 0.01f);
                glFogi(GL_FOG_MODE, GL_EXP2);
            } catch (Exception e) {
                System.err.println("Error setting up fog: " + e.getMessage());
                glDisable(GL_FOG);
            }
        }

        public void cleanup() {
            try {
                glDisable(GL_FOG);
            } catch (Exception e) {
                System.err.println("Error cleaning up atmospheric effects: " + e.getMessage());
            }
        }
    }

    private static class ParticleSystem {
        private List<Particle> particles = new ArrayList<>();

        public void update(float time) {
            try {
                // Update particle positions, add/remove particles
                particles.removeIf(p -> p.life <= 0);

                // Add new particles occasionally
                if (Math.random() < 0.1f) {
                    particles.add(new Particle(
                            (float) (Math.random() - 0.5) * 50,
                            30 + (float) Math.random() * 20,
                            (float) (Math.random() - 0.5) * 50,
                            time
                    ));
                }

                // Update existing particles
                for (Particle p : particles) {
                    p.update();
                }
            } catch (Exception e) {
                System.err.println("Error updating particles: " + e.getMessage());
            }
        }

        public void render(Camera camera) {
            try {
                glDisable(GL_LIGHTING);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glColor4f(1.0f, 1.0f, 1.0f, 0.8f);
                glPointSize(3.0f);

                glBegin(GL_POINTS);
                for (Particle p : particles) {
                    glVertex3f(p.x, p.y, p.z);
                }
                glEnd();

                glDisable(GL_BLEND);
                glEnable(GL_LIGHTING);
            } catch (Exception e) {
                System.err.println("Error rendering particles: " + e.getMessage());
            }
        }

        public void cleanup() {
            particles.clear();
        }
    }

    private static class Particle {
        float x, y, z;
        float vx, vy, vz;
        float life;

        public Particle(float x, float y, float z, float time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = (float) (Math.random() - 0.5) * 0.1f;
            this.vy = -0.2f - (float) Math.random() * 0.1f;
            this.vz = (float) (Math.random() - 0.5) * 0.1f;
            this.life = 100 + (float) Math.random() * 50;
        }

        public void update() {
            x += vx;
            y += vy;
            z += vz;
            life--;
        }
    }

    private static class SkyRenderer {
        public void render(Camera camera, float dayNightCycle, Vector3f sunDirection) {
            try {
                // Render sky gradient
                glDisable(GL_DEPTH_TEST);
                glDisable(GL_LIGHTING);

                // Sky dome or gradient
                renderSkyGradient(dayNightCycle);

                // Render sun/moon
                renderSun(sunDirection, dayNightCycle);

                glEnable(GL_DEPTH_TEST);
                glEnable(GL_LIGHTING);
            } catch (Exception e) {
                System.err.println("Error rendering sky: " + e.getMessage());
            }
        }

        private void renderSkyGradient(float dayNightCycle) {
            try {
                glBegin(GL_QUADS);

                // Calculate sky colors based on time of day
                float sunHeight = (float) Math.sin(dayNightCycle);
                Vector3f horizonColor, zenithColor;

                if (sunHeight > 0) {
                    // Daytime colors
                    horizonColor = new Vector3f(0.8f, 0.9f, 1.0f);
                    zenithColor = new Vector3f(0.5f, 0.7f, 1.0f);
                } else {
                    // Nighttime colors
                    horizonColor = new Vector3f(0.1f, 0.1f, 0.2f);
                    zenithColor = new Vector3f(0.0f, 0.0f, 0.1f);
                }

                // Render sky quad
                glColor3f(horizonColor.x, horizonColor.y, horizonColor.z);
                glVertex3f(-1000, -100, -1000);
                glVertex3f(1000, -100, -1000);
                glColor3f(zenithColor.x, zenithColor.y, zenithColor.z);
                glVertex3f(1000, 100, -1000);
                glVertex3f(-1000, 100, -1000);

                glEnd();
            } catch (Exception e) {
                System.err.println("Error rendering sky gradient: " + e.getMessage());
            }
        }

        private void renderSun(Vector3f sunDirection, float dayNightCycle) {
            try {
                float sunHeight = (float) Math.sin(dayNightCycle);
                if (sunHeight > -0.1f) {
                    glPushMatrix();
                    glTranslatef(sunDirection.x * 500, sunDirection.y * 500, sunDirection.z * 500);

                    glColor3f(1.0f, 1.0f, 0.8f);
                    renderSphere(10.0f);

                    glPopMatrix();
                }
            } catch (Exception e) {
                System.err.println("Error rendering sun: " + e.getMessage());
            }
        }

        private void renderSphere(float radius) {
            try {
                // Simple sphere for sun
                int segments = 16;
                for (int i = 0; i < segments; i++) {
                    float theta1 = (float) (i * 2 * Math.PI / segments);
                    float theta2 = (float) ((i + 1) * 2 * Math.PI / segments);

                    glBegin(GL_TRIANGLE_STRIP);
                    for (int j = 0; j <= segments; j++) {
                        float phi = (float) (j * Math.PI / segments);

                        float x1 = (float) (radius * Math.cos(theta1) * Math.sin(phi));
                        float y1 = (float) (radius * Math.cos(phi));
                        float z1 = (float) (radius * Math.sin(theta1) * Math.sin(phi));

                        float x2 = (float) (radius * Math.cos(theta2) * Math.sin(phi));
                        float y2 = (float) (radius * Math.cos(phi));
                        float z2 = (float) (radius * Math.sin(theta2) * Math.sin(phi));

                        glVertex3f(x1, y1, z1);
                        glVertex3f(x2, y2, z2);
                    }
                    glEnd();
                }
            } catch (Exception e) {
                System.err.println("Error rendering sphere in sky: " + e.getMessage());
            }
        }

        public void cleanup() {
            // Cleanup sky resources
        }
    }
}