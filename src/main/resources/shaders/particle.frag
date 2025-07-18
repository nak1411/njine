#version 330 core
in float particleLife;
in vec2 texCoords;

uniform sampler2D particleTexture;
uniform vec4 particleColor;
uniform float time;

out vec4 fragColor;

void main() {
    // Create circular particle shape
    vec2 center = texCoords - 0.5;
    float dist = length(center);

    if (dist > 0.5) discard;

    // Fade based on distance from center
    float alpha = 1.0 - (dist / 0.5);
    alpha = pow(alpha, 2.0);

    // Fade based on particle life
    float lifeFade = clamp(particleLife / 100.0, 0.0, 1.0);
    alpha *= lifeFade;

    // Color variation
    vec3 color = particleColor.rgb;
    color += sin(time + particleLife * 0.1) * 0.1;

    fragColor = vec4(color, alpha * particleColor.a);
}