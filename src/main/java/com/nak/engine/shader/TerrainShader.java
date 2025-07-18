package com.nak.engine.shader;

import com.nak.engine.entity.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Enhanced terrain shader that works with the new shader management system
 */
public class TerrainShader {
    private final ShaderManager shaderManager;
    private final ShaderProgram program;

    public TerrainShader() {
        this.shaderManager = ShaderManager.getInstance();
        this.program = shaderManager.getProgramOrDefault("terrain",
                getDefaultVertexShader(), getDefaultFragmentShader());
    }

    /**
     * Bind shader and set all terrain-specific uniforms
     */
    public void bind(Camera camera, Matrix4f modelMatrix, TerrainRenderData renderData) {
        program.bind();

        // Camera uniforms
        ShaderUtils.setCameraUniforms(program,
                camera.getViewMatrix(),
                camera.getProjectionMatrix(),
                camera.getPosition());

        // Model matrix
        program.setUniform("modelMatrix", modelMatrix);

        // Lighting
        ShaderUtils.setLightingUniforms(program,
                renderData.lightPosition,
                renderData.lightColor,
                renderData.lightDirection,
                renderData.ambientStrength,
                renderData.specularStrength,
                renderData.shininess);

        // Terrain-specific uniforms
        setTerrainUniforms(renderData);

        // Time for animations
        ShaderUtils.setTimeUniform(program, renderData.time);
    }

    private void setTerrainUniforms(TerrainRenderData renderData) {
        // Texture uniforms
        if (program.hasUniform("grassTexture")) {
            program.setTexture("grassTexture", 0);
        }
        if (program.hasUniform("rockTexture")) {
            program.setTexture("rockTexture", 1);
        }
        if (program.hasUniform("sandTexture")) {
            program.setTexture("sandTexture", 2);
        }
        if (program.hasUniform("snowTexture")) {
            program.setTexture("snowTexture", 3);
        }

        // Terrain parameters
        program.setUniform("textureScale", renderData.textureScale);

        // Fog
        ShaderUtils.setFogUniforms(program, renderData.fogColor, renderData.fogDensity);

        // Shadow mapping if available
        if (program.hasUniform("shadowMap")) {
            program.setTexture("shadowMap", 4);
            program.setUniform("lightSpaceMatrix", renderData.lightSpaceMatrix);
            program.setUniform("shadowBias", renderData.shadowBias);
        }
    }

    public void unbind() {
        program.unbind();
    }

    public boolean isValid() {
        return program != null && program.isLinked();
    }

    public void reload() {
        shaderManager.reloadProgram("terrain");
    }

    // Data class for terrain rendering parameters
    public static class TerrainRenderData {
        public Vector3f lightPosition = new Vector3f();
        public Vector3f lightColor = new Vector3f(1, 1, 1);
        public Vector3f lightDirection = new Vector3f(0, -1, 0);
        public float ambientStrength = 0.3f;
        public float specularStrength = 0.5f;
        public float shininess = 32.0f;

        public Vector3f fogColor = new Vector3f(0.5f, 0.6f, 0.7f);
        public float fogDensity = 0.008f;

        public float textureScale = 16.0f;
        public float time = 0.0f;

        // Shadow mapping
        public Matrix4f lightSpaceMatrix = new Matrix4f();
        public float shadowBias = 0.005f;
    }

    private String getDefaultVertexShader() {
        return """
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
                """;
    }

    private String getDefaultFragmentShader() {
        return """
                #version 330 core
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec3 fragTangent;
                in vec3 vertexColor;
                in vec4 fragPosLightSpace;
                in float height;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                out vec4 fragColor;
                
                vec3 calculateLighting() {
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    return ambient + diffuse + specular;
                }
                
                void main() {
                    vec3 lighting = calculateLighting();
                    vec3 finalColor = vertexColor * lighting;
                
                    // Apply fog
                    float distance = length(viewPosition - fragPos);
                    float fogFactor = exp(-fogDensity * distance);
                    fogFactor = clamp(fogFactor, 0.0, 1.0);
                    finalColor = mix(fogColor, finalColor, fogFactor);
                
                    fragColor = vec4(finalColor, 1.0);
                }
                """;
    }
}