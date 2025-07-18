#version 330 core
in vec2 texCoords;
in vec3 fragNormal;
in vec4 vertexColor;
in vec3 fragPos;

uniform sampler2D texture0;
uniform bool useTexture;
uniform vec4 uniformColor;
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 viewPosition;

out vec4 fragColor;

void main() {
    vec4 finalColor = vertexColor;

    if (useTexture && textureSize(texture0, 0).x > 1) {
        finalColor *= texture(texture0, texCoords);
    }

    if (uniformColor.a > 0.0) {
        finalColor *= uniformColor;
    }

    // Simple lighting if light is available
    if (length(lightPosition) > 0.1) {
        vec3 norm = normalize(fragNormal);
        vec3 lightDir = normalize(lightPosition - fragPos);
        float diff = max(dot(norm, lightDir), 0.0);
        finalColor.rgb *= (0.3 + 0.7 * diff);
    }

    fragColor = finalColor;
}