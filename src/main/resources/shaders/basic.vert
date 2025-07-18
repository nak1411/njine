#version 330 core
layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec4 color;

uniform mat4 mvpMatrix;
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

out vec2 texCoords;
out vec3 fragNormal;
out vec4 vertexColor;
out vec3 fragPos;

void main() {
    fragPos = (modelMatrix * vec4(position, 1.0)).xyz;
    texCoords = texCoord;
    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
    vertexColor = color;

    if (length(mvpMatrix[0]) > 0.1) {
        gl_Position = mvpMatrix * vec4(position, 1.0);
    } else {
        gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
    }
}