#version 330 core
#include "common.glsl"
#include "lighting.glsl"

in vec3 fragPos;
in vec2 texCoords;
in vec3 fragNormal;
in vec3 fragTangent;
in vec3 vertexColor;
in vec4 fragPosLightSpace;
in float height;

uniform sampler2D grassTexture;
uniform sampler2D rockTexture;
uniform sampler2D sandTexture;
uniform sampler2D snowTexture;
uniform sampler2D shadowMap;

uniform float textureScale;
uniform float shadowBias;

out vec4 fragColor;

vec3 calculateTerrainColor() {
    vec3 normal = normalize(fragNormal);
    float slope = 1.0 - normal.y;

    // Base terrain color from vertex attributes
    vec3 baseColor = vertexColor;

    // Add variation based on height and slope
    if (height < -1.0) {
        // Water/beach areas
        baseColor = mix(vec3(0.2, 0.4, 0.8), vec3(0.8, 0.7, 0.5), clamp((height + 5.0) / 4.0, 0.0, 1.0));
    } else if (height > 35.0) {
        // Snow areas
        float snowFactor = clamp((height - 30.0) / 10.0, 0.0, 1.0);
        baseColor = mix(baseColor, vec3(0.9, 0.9, 0.95), snowFactor);
    }

    // Add slope-based variation
    if (slope > 0.3) {
        baseColor = mix(baseColor, vec3(0.6, 0.5, 0.4), slope * 0.5);
    }

    return baseColor;
}

float calculateShadow() {
    // Perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;

    if (projCoords.z > 1.0) return 0.0;

    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;

    float bias = max(shadowBias * (1.0 - dot(normalize(fragNormal), normalize(-lightDirection))), shadowBias);
    float shadow = currentDepth - bias > closestDepth ? 1.0 : 0.0;

    return shadow;
}

void main() {
    vec3 terrainColor = calculateTerrainColor();
    vec3 lighting = calculateLighting(fragPos, fragNormal);

    // Apply shadow (if shadow mapping is available)
    float shadow = 0.0;
    if (textureSize(shadowMap, 0).x > 1) {
        shadow = calculateShadow();
    }

    vec3 finalColor = terrainColor * lighting * (1.0 - shadow * 0.3);

    // Apply fog
    float fogFactor = calculateFog(fragPos);
    finalColor = applyFog(finalColor, fogFactor);

    // Add subtle noise for texture variation
    float noiseVal = noise(texCoords * textureScale) * 0.05 + 0.95;
    finalColor *= noiseVal;

    fragColor = vec4(finalColor, 1.0);
}