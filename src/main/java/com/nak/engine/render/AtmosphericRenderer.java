package com.nak.engine.render;

import com.nak.engine.camera.cameras.Camera;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

class AtmosphericRenderer {
    private final List<Cloud> clouds;
    private final Random random = new Random();

    public AtmosphericRenderer() {
        this.clouds = new ArrayList<>();
        initializeClouds();
    }

    private void initializeClouds() {
        for (int i = 0; i < 25; i++) {
            Cloud cloud = new Cloud();
            cloud.position.set(
                    (float) (random.nextGaussian() * 150),
                    20 + random.nextFloat() * 20,
                    (float) (random.nextGaussian() * 150)
            );
            cloud.scale = 0.8f + random.nextFloat() * 1.4f;
            cloud.speed = 0.5f + random.nextFloat() * 1.0f;
            cloud.opacity = 0.3f + random.nextFloat() * 0.4f;
            clouds.add(cloud);
        }
    }

    public void render(Camera camera, Vector3f sunDirection, Vector3f sunColor, Vector3f fogColor, float fogDensity) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_LIGHTING);

        // Render volumetric fog effect
        renderVolumetricFog(camera, sunDirection, sunColor, fogDensity);

        // Render clouds
        renderClouds(camera, sunColor);

        glEnable(GL_LIGHTING);
        glDisable(GL_BLEND);
    }

    private void renderVolumetricFog(Camera camera, Vector3f sunDirection, Vector3f sunColor, float fogDensity) {
        if (fogDensity < 0.001f) return;

        Vector3f cameraPos = camera.getPosition();
        float intensity = Math.min(1.0f, fogDensity * 10.0f);

        glColor4f(sunColor.x * 0.3f, sunColor.y * 0.3f, sunColor.z * 0.3f, intensity * 0.1f);

        // Simple fog planes
        for (int i = 0; i < 3; i++) {
            float distance = 50.0f + i * 100.0f;
            glPushMatrix();
            glTranslatef(cameraPos.x, 5.0f + i * 10.0f, cameraPos.z);
            glScalef(distance, 1.0f, distance);

            glBegin(GL_QUADS);
            glVertex3f(-1, 0, -1);
            glVertex3f(1, 0, -1);
            glVertex3f(1, 0, 1);
            glVertex3f(-1, 0, 1);
            glEnd();

            glPopMatrix();
        }
    }

    private void renderClouds(Camera camera, Vector3f sunColor) {
        Vector3f cameraPos = camera.getPosition();

        for (Cloud cloud : clouds) {
            // Update cloud position
            cloud.position.x += cloud.speed * 0.01f;
            if (cloud.position.x > 200) cloud.position.x = -200;

            // Distance culling
            float distance = cameraPos.distance(cloud.position);
            if (distance > 300) continue;

            // Render cloud
            glPushMatrix();
            glTranslatef(cloud.position.x, cloud.position.y, cloud.position.z);
            glScalef(cloud.scale, cloud.scale * 0.6f, cloud.scale);

            float alpha = cloud.opacity * Math.max(0.1f, 1.0f - distance / 300.0f);
            glColor4f(sunColor.x * 0.9f, sunColor.y * 0.9f, sunColor.z * 0.95f, alpha);

            // Render cloud as multiple spheres
            for (int i = 0; i < 5; i++) {
                glPushMatrix();
                float offsetX = (i - 2) * 2.0f + (float) Math.sin(System.currentTimeMillis() * 0.001f + i) * 0.5f;
                float offsetY = (float) Math.cos(System.currentTimeMillis() * 0.0008f + i) * 0.3f;
                float offsetZ = (float) Math.sin(System.currentTimeMillis() * 0.0012f + i) * 1.0f;

                glTranslatef(offsetX, offsetY, offsetZ);
                renderCloudSphere(2.0f + (float) Math.sin(System.currentTimeMillis() * 0.002f + i) * 0.5f);
                glPopMatrix();
            }

            glPopMatrix();
        }
    }

    private void renderCloudSphere(float radius) {
        int segments = 8; // Low poly for performance

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
    }

    public void cleanup() {
        clouds.clear();
    }

    private static class Cloud {
        public final Vector3f position = new Vector3f();
        public float scale = 1.0f;
        public float speed = 1.0f;
        public float opacity = 1.0f;
    }
}