#version 330 core

in vec2 texCoords;
in vec3 worldPos;
in vec3 fragPos;
in vec3 fragNormal;
in float waveHeight;

// Lighting uniforms
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 lightDirection;
uniform float ambientStrength;
uniform float specularStrength;
uniform float shininess;

// Camera uniforms
uniform vec3 viewPosition;
uniform vec3 fogColor;
uniform float fogDensity;
uniform float time;

// Water-specific uniforms
uniform sampler2D waterTexture;
uniform sampler2D normalMap;
uniform bool useWaterTexture;
uniform bool useNormalMap;
uniform vec4 waterTint;

out vec4 fragColor;

// Lighting calculation function
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

// Fog calculation functions
float calculateFog(vec3 worldPos) {
    float distance = length(viewPosition - worldPos);
    return exp(-fogDensity * distance);
}

vec3 applyFog(vec3 color, float fogFactor) {
    return mix(fogColor, color, clamp(fogFactor, 0.0, 1.0));
}

void main() {
    // Animated texture coordinates for water movement
    vec2 animatedTexCoords = texCoords + vec2(time * 0.05, time * 0.03);
    vec2 animatedTexCoords2 = texCoords + vec2(-time * 0.02, time * 0.04);

    // Base water color
    vec4 waterColor = vec4(0.2, 0.6, 1.0, 0.8);

    // Apply water tint if provided
    if (length(waterTint.rgb) > 0.1) {
        waterColor = waterTint;
    }

    // Sample water texture if available
    if (useWaterTexture && textureSize(waterTexture, 0).x > 1) {
        vec4 texSample1 = texture(waterTexture, animatedTexCoords);
        vec4 texSample2 = texture(waterTexture, animatedTexCoords2);
        waterColor = mix(texSample1, texSample2, 0.5);
    }

    // Calculate normal for lighting
    vec3 normal = normalize(fragNormal);

    // Enhanced normal from normal map if available
    if (useNormalMap && textureSize(normalMap, 0).x > 1) {
        vec3 normalSample1 = texture(normalMap, animatedTexCoords).rgb * 2.0 - 1.0;
        vec3 normalSample2 = texture(normalMap, animatedTexCoords2).rgb * 2.0 - 1.0;
        normal = normalize(normal + normalSample1 * 0.5 + normalSample2 * 0.3);
    }

    // Calculate lighting
    vec3 lighting = calculateLighting(fragPos, normal);

    // Add reflection and refraction effects
    vec3 viewDir = normalize(viewPosition - fragPos);
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.0);

    // Base water color with lighting
    vec3 finalColor = waterColor.rgb * lighting;

    // Add fresnel reflection effect (simulate sky reflection)
    vec3 skyColor = vec3(0.5, 0.7, 1.0);
    finalColor = mix(finalColor, skyColor, fresnel * 0.4);

    // Add foam effect based on wave height
    float foam = clamp(abs(waveHeight) * 3.0, 0.0, 1.0);
    finalColor = mix(finalColor, vec3(1.0), foam * 0.4);

    // Add subtle caustics effect
    float caustics = sin(fragPos.x * 0.1 + time * 2.0) * sin(fragPos.z * 0.1 + time * 1.8) * 0.5 + 0.5;
    finalColor += caustics * 0.1 * vec3(0.8, 1.0, 1.2);

    // Apply fog
    float fogFactor = calculateFog(fragPos);
    finalColor = applyFog(finalColor, fogFactor);

    // Water transparency based on depth and waves
    float alpha = 0.8 + waveHeight * 0.1 + fresnel * 0.1;
    alpha = clamp(alpha, 0.6, 1.0);

    fragColor = vec4(finalColor, alpha);
}