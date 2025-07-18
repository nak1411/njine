#version 330 core
layout (location = 0) in vec3 position;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform float dayNightCycle;
uniform vec3 sunDirection;

out vec3 texCoords;
out vec3 worldPos;
out float sunHeight;

void main() {
    texCoords = position;
    worldPos = position;
    sunHeight = sin(dayNightCycle);

    // Remove translation from view matrix for skybox
    mat4 rotView = mat4(mat3(viewMatrix));
    vec4 pos = projectionMatrix * rotView * vec4(position, 1.0);

    // Ensure skybox is always at far plane
    gl_Position = pos.xyww;
}