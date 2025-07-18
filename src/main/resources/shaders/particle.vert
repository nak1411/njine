#version 330 core
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 velocity;
layout (location = 2) in float life;
layout (location = 3) in float size;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform float time;

out float particleLife;
out vec2 texCoords;

void main() {
    // Animate particle position based on velocity and time
    vec3 animatedPos = position + velocity * time;

    vec4 worldPos = modelMatrix * vec4(animatedPos, 1.0);
    vec4 viewPos = viewMatrix * worldPos;

    // Billboard effect - face camera
    vec3 right = vec3(viewMatrix[0][0], viewMatrix[1][0], viewMatrix[2][0]);
    vec3 up = vec3(viewMatrix[0][1], viewMatrix[1][1], viewMatrix[2][1]);

    // Expand point to quad
    int vertexIndex = gl_VertexID % 4;
    vec2 offset;

    switch(vertexIndex) {
        case 0: offset = vec2(-1, -1); texCoords = vec2(0, 0); break;
        case 1: offset = vec2( 1, -1); texCoords = vec2(1, 0); break;
        case 2: offset = vec2(-1,  1); texCoords = vec2(0, 1); break;
        case 3: offset = vec2( 1,  1); texCoords = vec2(1, 1); break;
    }

    vec3 finalPos = worldPos.xyz + (right * offset.x + up * offset.y) * size;

    particleLife = life;
    gl_Position = projectionMatrix * viewMatrix * vec4(finalPos, 1.0);
}