package com.nak.engine.render;

import com.nak.engine.camera.Camera;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

class SkyRenderer {
    public void render(Camera camera, float dayNightCycle, Vector3f sunDirection, Vector3f sunColor) {
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        // Render sky dome
        renderSkyDome(dayNightCycle);

        // Render sun/moon
        renderCelestialBodies(sunDirection, dayNightCycle);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
    }

    private void renderSkyDome(float dayNightCycle) {
        float sunHeight = (float) Math.sin(dayNightCycle);

        // Calculate sky colors
        Vector3f horizonColor, zenithColor;
        if (sunHeight > 0) {
            // Day sky
            float intensity = Math.min(1.0f, sunHeight * 2.0f);
            horizonColor = new Vector3f(0.7f + intensity * 0.2f, 0.8f + intensity * 0.1f, 1.0f);
            zenithColor = new Vector3f(0.4f + intensity * 0.3f, 0.6f + intensity * 0.2f, 1.0f);
        } else {
            // Night sky
            horizonColor = new Vector3f(0.1f, 0.1f, 0.3f);
            zenithColor = new Vector3f(0.05f, 0.05f, 0.15f);
        }

        // Render sky gradient
        glBegin(GL_TRIANGLE_FAN);
        glColor3f(zenithColor.x, zenithColor.y, zenithColor.z);
        glVertex3f(0, 1000, 0); // Top center

        glColor3f(horizonColor.x, horizonColor.y, horizonColor.z);
        for (int i = 0; i <= 16; i++) {
            float angle = (float) (i * 2 * Math.PI / 16);
            glVertex3f((float) Math.cos(angle) * 1000, -100, (float) Math.sin(angle) * 1000);
        }
        glEnd();
    }

    private void renderCelestialBodies(Vector3f sunDirection, float dayNightCycle) {
        float sunHeight = (float) Math.sin(dayNightCycle);

        if (sunHeight > -0.2f) {
            // Render sun
            glPushMatrix();
            glTranslatef(sunDirection.x * 800, sunDirection.y * 800, sunDirection.z * 800);

            float intensity = Math.max(0.1f, sunHeight);
            glColor3f(1.0f * intensity, 0.9f * intensity, 0.7f * intensity);

            renderSphere(15.0f, 8);
            glPopMatrix();
        } else {
            // Render moon
            Vector3f moonDir = new Vector3f(-sunDirection.x, Math.abs(sunDirection.y), -sunDirection.z);
            glPushMatrix();
            glTranslatef(moonDir.x * 800, moonDir.y * 800, moonDir.z * 800);

            glColor3f(0.8f, 0.8f, 0.9f);
            renderSphere(12.0f, 8);
            glPopMatrix();
        }
    }

    private void renderSphere(float radius, int segments) {
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
        // Nothing to cleanup for sky renderer
    }
}