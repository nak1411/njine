#version 330 core

in vec3 fragPos;
in vec2 texCoords;
in vec3 fragNormal;
in vec3 fragTangent;
in vec3 vertexColor;
in vec4 fragPosLightSpace;
in float height;

// Camera uniforms (from common.glsl)
uniform vec3 viewPosition;
uniform vec3 fogColor;
uniform float fogDensity;
uniform float time;

// Light uniforms (from lighting.glsl)
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 lightDirection;
uniform float ambientStrength;
uniform float specularStrength;
uniform float shininess;

// Terrain-specific uniforms
uniform sampler2D grassTexture;
uniform sampler2D rockTexture;
uniform sampler2D sandTexture;
uniform sampler2D snowTexture;
uniform sampler2D shadowMap;
uniform float textureScale;
uniform float shadowBias;

out vec4 fragColor;

// Utility functions (from common.glsl)
float calculateFog(vec3 worldPos) {
    float distance = length(viewPosition - worldPos);
    return exp(-fogDensity * distance);
}

vec3 applyFog(vec3 color, float fogFactor) {
    return mix(fogColor, color, clamp(fogFactor, 0.0, 1.0));
}

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);

    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

// Lighting functions (from lighting.glsl)
vec3 calculateLighting(vec3 worldPos, vec3 normal) {
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(lightPosition - worldPos);

    // Ambient
    vec3 ambient = ambientStrength * lightColor;

    // Diffuse
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // Specular
    vec3 viewDir = normalize(viewPosition - worldPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
    vec3 specular = specularStrength * spec * lightColor;

    return ambient + diffuse + specular;
}

vec3 calculateTerrainColor() {
    vec3 normal = normalize(fragNormal);
    float slope = 1.0 - normal.y;

    // Base terrain color from vertex attributes
    vec3 baseColor = vertexColor;

    // Ensure we have a valid base color
    if (length(baseColor) < 0.1) {
        baseColor = vec3(0.3, 0.6, 0.2); // Default green
    }

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
    // Only calculate shadows if shadow map is available
    if (textureSize(shadowMap, 0).x <= 1) {
        return 0.0;
    }

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
    float shadow = calculateShadow();

    vec3 finalColor = terrainColor * lighting * (1.0 - shadow * 0.3);

    // Apply fog
    float fogFactor = calculateFog(fragPos);
    finalColor = applyFog(finalColor, fogFactor);

    // Add subtle noise for texture variation
    float noiseVal = noise(texCoords * textureScale) * 0.05 + 0.95;
    finalColor *= noiseVal;

    // Ensure the color is never completely black
    finalColor = max(finalColor, vec3(0.05));

    fragColor = vec4(finalColor, 1.0);
}