package com.nak.engine.render;

import com.nak.engine.camera.Camera;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

class ParticleRenderer {
    private final List<Particle> particles;
    private final Random random = new Random();
    private float lastSpawnTime = 0;

    public ParticleRenderer() {
        this.particles = new ArrayList<>();
    }

    public void render(Camera camera, float time) {
        // Spawn new particles occasionally
        if (time - lastSpawnTime > 0.1f && particles.size() < 100) {
            spawnParticle(camera.getPosition());
            lastSpawnTime = time;
        }

        // Update and render particles
        particles.removeIf(p -> p.life <= 0);

        glPointSize(2.0f);
        glBegin(GL_POINTS);

        for (Particle particle : particles) {
            particle.update();

            // Color based on life
            float alpha = particle.life / 100.0f;
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glVertex3f(particle.x, particle.y, particle.z);
        }

        glEnd();
    }

    private void spawnParticle(Vector3f cameraPos) {
        Particle particle = new Particle();
        particle.x = cameraPos.x + (random.nextFloat() - 0.5f) * 50;
        particle.y = cameraPos.y + 20 + random.nextFloat() * 10;
        particle.z = cameraPos.z + (random.nextFloat() - 0.5f) * 50;
        particle.vx = (random.nextFloat() - 0.5f) * 0.1f;
        particle.vy = -0.2f - random.nextFloat() * 0.1f;
        particle.vz = (random.nextFloat() - 0.5f) * 0.1f;
        particle.life = 80 + random.nextFloat() * 40;
        particles.add(particle);
    }

    public void cleanup() {
        particles.clear();
    }

    private static class Particle {
        public float x, y, z;
        public float vx, vy, vz;
        public float life;

        public void update() {
            x += vx;
            y += vy;
            z += vz;
            life--;
        }
    }
}