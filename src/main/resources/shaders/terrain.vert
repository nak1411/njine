#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 color;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform mat4 lightSpaceMatrix;

out vec3 fragPos;
out vec2 texCoords;
out vec3 fragNormal;
out vec3 fragTangent;
out vec3 vertexColor;
out vec4 fragPosLightSpace;
out float height;

void main() {
    vec4 worldPos = modelMatrix * vec4(position, 1.0);
    fragPos = worldPos.xyz;
    texCoords = texCoord;
    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
    fragTangent = mat3(transpose(inverse(modelMatrix))) * tangent;
    vertexColor = color;
    fragPosLightSpace = lightSpaceMatrix * worldPos;
    height = position.y;

    gl_Position = projectionMatrix * viewMatrix * worldPos;
}