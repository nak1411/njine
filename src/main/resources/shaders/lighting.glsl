// Light uniforms
uniform vec3 lightPosition;
uniform vec3 lightColor;
uniform vec3 lightDirection;
uniform float ambientStrength;
uniform float specularStrength;
uniform float shininess;

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

vec3 calculateDirectionalLighting(vec3 worldPos, vec3 normal, vec3 direction) {
    vec3 norm = normalize(normal);
    vec3 lightDir = normalize(-direction);

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