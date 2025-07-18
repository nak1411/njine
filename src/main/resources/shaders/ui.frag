#version 330 core
in vec2 texCoords;
in vec4 vertexColor;

uniform sampler2D uiTexture;
uniform bool useTexture;
uniform vec4 uniformColor;

out vec4 fragColor;

void main() {
    vec4 finalColor = vertexColor;

    if (useTexture && textureSize(uiTexture, 0).x > 1) {
        vec4 texColor = texture(uiTexture, texCoords);
        finalColor *= texColor;
    }

    if (uniformColor.a > 0.0) {
        finalColor *= uniformColor;
    }

    fragColor = finalColor;
}