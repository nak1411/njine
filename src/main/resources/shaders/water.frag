#version 330 core

in vec2 texCoords;
in vec3 worldPos;
in float waveHeight;

uniform sampler2D waterTexture;
uniform sampler2D normalMap;

out vec4 fragColor;

void main() {
    // Animated texture coordinates
    vec2 animatedTexCoords = texCoords + vec2(time * 0.05, time * 0.03);
    vec2 animatedTexCoords2 = texCoords + vec2(-time * 0.02, time * 0.04);

    // Sample water color (if texture available)
    vec4 waterColor = vec4(0.2, 0.6, 1.0, 0.8);
    if (textureSize(waterTexture, 0).x > 1) {
        waterColor = texture(waterTexture, animatedTexCoords);
    }

    // Calculate normal
    vec3 normal = vec3(0, 1, 0);
    if (textureSize(normalMap, 0).x > 1) {
        vec3 normalSample1 = texture(normalMap, animatedTexCoords).rgb * 2.0 - 1.0;
        vec3 normalSample2 = texture(normalMap, animatedTexCoords2).rgb * 2.0 - 1.0;
        normal = normalize(normalSample1 + normalSample2);
    } else {
        // Generate procedural normal from waves
        float dWave_dx = cos(worldPos.x * 0.02 + time * 2.0) * 0.02 * 0.5;
        float dWave_dz = -sin(worldPos.z * 0.015 + time * 1.5) * 0.015 * 0.3;
        normal = normalize(vec3(-dWave_dx, 1.0, -dWave_dz));
    }

    // Calculate lighting
    vec3 lighting = calculateLighting(worldPos, normal);

    // Add reflection and refraction effects
    vec3 viewDir = normalize(viewPosition - worldPos);
    float fresnel = pow(1.0 - max(dot(normal, viewDir), 0.0), 2.0);

    vec3 finalColor = waterColor.rgb * lighting;
    finalColor = mix(finalColor, vec3(0.2, 0.6, 1.0), fresnel * 0.3);

    // Add foam effect based on wave height
    float foam = clamp(abs(waveHeight) * 2.0, 0.0, 1.0);
    finalColor = mix(finalColor, vec3(1.0), foam * 0.3);

    // Apply fog
    float fogFactor = calculateFog(worldPos);
    finalColor = applyFog(finalColor, fogFactor);

    fragColor = vec4(finalColor, 0.8 + waveHeight * 0.1);
}