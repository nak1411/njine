#version 330 core
in vec3 texCoords;
in vec3 worldPos;
in float sunHeight;

uniform vec3 sunDirection;
uniform vec3 sunColor;
uniform float dayNightCycle;

out vec4 fragColor;

vec3 getSkyColor() {
    vec3 viewDir = normalize(texCoords);
    float height = viewDir.y;

    vec3 skyColor;
    vec3 horizonColor;

    if (sunHeight > 0.0) {
        // Day sky
        float intensity = min(1.0, sunHeight * 2.0);
        skyColor = vec3(0.4 + intensity * 0.3, 0.6 + intensity * 0.2, 1.0);
        horizonColor = vec3(0.7 + intensity * 0.2, 0.8 + intensity * 0.1, 1.0);
    } else {
        // Night sky
        skyColor = vec3(0.05, 0.05, 0.15);
        horizonColor = vec3(0.1, 0.1, 0.3);
    }

    // Gradient from horizon to zenith
    float t = max(0.0, height);
    return mix(horizonColor, skyColor, t);
}

vec3 getSunColor() {
    vec3 viewDir = normalize(texCoords);
    float sunDot = dot(viewDir, normalize(sunDirection));

    if (sunHeight > -0.2 && sunDot > 0.999) {
        // Sun disk
        float intensity = max(0.1, sunHeight);
        return vec3(1.0, 0.9, 0.7) * intensity * 3.0;
    } else if (sunHeight <= -0.2) {
        // Moon
        vec3 moonDir = -sunDirection;
        float moonDot = dot(viewDir, normalize(moonDir));
        if (moonDot > 0.9985) {
            return vec3(0.8, 0.8, 0.9) * 2.0;
        }
    }

    return vec3(0.0);
}

void main() {
    vec3 skyColor = getSkyColor();
    vec3 celestialColor = getSunColor();

    vec3 finalColor = skyColor + celestialColor;

    fragColor = vec4(finalColor, 1.0);
}