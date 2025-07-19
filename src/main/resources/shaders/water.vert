#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform float time;

out vec2 texCoords;
out vec3 worldPos;
out vec3 fragPos;
out vec3 fragNormal;
out float waveHeight;

void main() {
    vec4 worldPosition = modelMatrix * vec4(position, 1.0);

    // Add wave animation with proper time scaling
    float wave1 = sin(worldPosition.x * 0.02 + time * 2.0) * 0.5;
    float wave2 = cos(worldPosition.z * 0.015 + time * 1.5) * 0.3;
    float wave3 = sin(worldPosition.x * 0.05 + worldPosition.z * 0.05 + time * 3.0) * 0.1;

    waveHeight = wave1 + wave2 + wave3;
    worldPosition.y += waveHeight;

    worldPos = worldPosition.xyz;
    fragPos = worldPosition.xyz;
    texCoords = texCoord;

    // Calculate normal for lighting (approximate)
    float dWave_dx = cos(worldPosition.x * 0.02 + time * 2.0) * 0.02 * 0.5;
    float dWave_dz = -sin(worldPosition.z * 0.015 + time * 1.5) * 0.015 * 0.3;
    fragNormal = normalize(vec3(-dWave_dx, 1.0, -dWave_dz));

    gl_Position = projectionMatrix * viewMatrix * worldPosition;
}