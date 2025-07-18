package com.nak.engine.shader;

import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class TerrainShader {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    // Uniform locations
    private int projectionMatrixLocation;
    private int viewMatrixLocation;
    private int modelMatrixLocation;
    private int lightSpaceMatrixLocation;

    // Light uniforms
    private int lightPositionLocation;
    private int lightColorLocation;
    private int lightDirectionLocation;
    private int ambientStrengthLocation;
    private int specularStrengthLocation;
    private int shininessLocation;

    // Shadow uniforms
    private int shadowMapLocation;
    private int shadowBiasLocation;

    // Terrain uniforms
    private int heightMapLocation;
    private int grassTextureLocation;
    private int rockTextureLocation;
    private int sandTextureLocation;
    private int snowTextureLocation;
    private int textureScaleLocation;

    // Camera uniforms
    private int viewPositionLocation;
    private int fogColorLocation;
    private int fogDensityLocation;

    public TerrainShader() throws Exception {
        programId = GL20.glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create shader program");
        }

        createVertexShader(getTerrainVertexShader());
        createFragmentShader(getTerrainFragmentShader());
        link();

        // Get uniform locations
        getAllUniformLocations();
    }

    private void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL20.GL_VERTEX_SHADER);
    }

    private void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL20.GL_FRAGMENT_SHADER);
    }

    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = GL20.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        GL20.glShaderSource(shaderId, shaderCode);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader code: " + GL20.glGetShaderInfoLog(shaderId, 1024));
        }

        GL20.glAttachShader(programId, shaderId);
        return shaderId;
    }

    private void link() throws Exception {
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId);
        }

        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader code: " + GL20.glGetProgramInfoLog(programId, 1024));
        }
    }

    private void getAllUniformLocations() {
        projectionMatrixLocation = GL20.glGetUniformLocation(programId, "projectionMatrix");
        viewMatrixLocation = GL20.glGetUniformLocation(programId, "viewMatrix");
        modelMatrixLocation = GL20.glGetUniformLocation(programId, "modelMatrix");
        lightSpaceMatrixLocation = GL20.glGetUniformLocation(programId, "lightSpaceMatrix");

        lightPositionLocation = GL20.glGetUniformLocation(programId, "lightPosition");
        lightColorLocation = GL20.glGetUniformLocation(programId, "lightColor");
        lightDirectionLocation = GL20.glGetUniformLocation(programId, "lightDirection");
        ambientStrengthLocation = GL20.glGetUniformLocation(programId, "ambientStrength");
        specularStrengthLocation = GL20.glGetUniformLocation(programId, "specularStrength");
        shininessLocation = GL20.glGetUniformLocation(programId, "shininess");

        shadowMapLocation = GL20.glGetUniformLocation(programId, "shadowMap");
        shadowBiasLocation = GL20.glGetUniformLocation(programId, "shadowBias");

        heightMapLocation = GL20.glGetUniformLocation(programId, "heightMap");
        grassTextureLocation = GL20.glGetUniformLocation(programId, "grassTexture");
        rockTextureLocation = GL20.glGetUniformLocation(programId, "rockTexture");
        sandTextureLocation = GL20.glGetUniformLocation(programId, "sandTexture");
        snowTextureLocation = GL20.glGetUniformLocation(programId, "snowTexture");
        textureScaleLocation = GL20.glGetUniformLocation(programId, "textureScale");

        viewPositionLocation = GL20.glGetUniformLocation(programId, "viewPosition");
        fogColorLocation = GL20.glGetUniformLocation(programId, "fogColor");
        fogDensityLocation = GL20.glGetUniformLocation(programId, "fogDensity");
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    // Matrix uniform methods
    public void setProjectionMatrix(FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(projectionMatrixLocation, false, matrix);
    }

    public void setViewMatrix(FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(viewMatrixLocation, false, matrix);
    }

    public void setModelMatrix(FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(modelMatrixLocation, false, matrix);
    }

    public void setLightSpaceMatrix(FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(lightSpaceMatrixLocation, false, matrix);
    }

    // Light uniform methods
    public void setLightPosition(float x, float y, float z) {
        GL20.glUniform3f(lightPositionLocation, x, y, z);
    }

    public void setLightColor(float r, float g, float b) {
        GL20.glUniform3f(lightColorLocation, r, g, b);
    }

    public void setLightDirection(float x, float y, float z) {
        GL20.glUniform3f(lightDirectionLocation, x, y, z);
    }

    public void setAmbientStrength(float strength) {
        GL20.glUniform1f(ambientStrengthLocation, strength);
    }

    public void setSpecularStrength(float strength) {
        GL20.glUniform1f(specularStrengthLocation, strength);
    }

    public void setShininess(float shininess) {
        GL20.glUniform1f(shininessLocation, shininess);
    }

    // Shadow uniform methods
    public void setShadowMap(int textureUnit) {
        GL20.glUniform1i(shadowMapLocation, textureUnit);
    }

    public void setShadowBias(float bias) {
        GL20.glUniform1f(shadowBiasLocation, bias);
    }

    // Terrain texture methods
    public void setHeightMap(int textureUnit) {
        GL20.glUniform1i(heightMapLocation, textureUnit);
    }

    public void setGrassTexture(int textureUnit) {
        GL20.glUniform1i(grassTextureLocation, textureUnit);
    }

    public void setRockTexture(int textureUnit) {
        GL20.glUniform1i(rockTextureLocation, textureUnit);
    }

    public void setSandTexture(int textureUnit) {
        GL20.glUniform1i(sandTextureLocation, textureUnit);
    }

    public void setSnowTexture(int textureUnit) {
        GL20.glUniform1i(snowTextureLocation, textureUnit);
    }

    public void setTextureScale(float scale) {
        GL20.glUniform1f(textureScaleLocation, scale);
    }

    // Camera and fog methods
    public void setViewPosition(float x, float y, float z) {
        GL20.glUniform3f(viewPositionLocation, x, y, z);
    }

    public void setFogColor(float r, float g, float b) {
        GL20.glUniform3f(fogColorLocation, r, g, b);
    }

    public void setFogDensity(float density) {
        GL20.glUniform1f(fogDensityLocation, density);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }
    }

    private String getTerrainVertexShader() {
        return """
                #version 330 core
                
                layout (location = 0) in vec3 position;
                layout (location = 1) in vec2 texCoord;
                layout (location = 2) in vec3 normal;
                
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform mat4 lightSpaceMatrix;
                
                out vec3 fragPos;
                out vec2 texCoords;
                out vec3 fragNormal;
                out vec4 fragPosLightSpace;
                out float height;
                
                void main() {
                    vec4 worldPos = modelMatrix * vec4(position, 1.0);
                    fragPos = worldPos.xyz;
                    texCoords = texCoord;
                    fragNormal = mat3(transpose(inverse(modelMatrix))) * normal;
                    fragPosLightSpace = lightSpaceMatrix * worldPos;
                    height = position.y;
                
                    gl_Position = projectionMatrix * viewMatrix * worldPos;
                }
                """;
    }

    private String getTerrainFragmentShader() {
        return """
                #version 330 core
                
                in vec3 fragPos;
                in vec2 texCoords;
                in vec3 fragNormal;
                in vec4 fragPosLightSpace;
                in float height;
                
                uniform vec3 lightPosition;
                uniform vec3 lightColor;
                uniform vec3 lightDirection;
                uniform float ambientStrength;
                uniform float specularStrength;
                uniform float shininess;
                
                uniform sampler2D shadowMap;
                uniform float shadowBias;
                
                uniform sampler2D heightMap;
                uniform sampler2D grassTexture;
                uniform sampler2D rockTexture;
                uniform sampler2D sandTexture;
                uniform sampler2D snowTexture;
                uniform float textureScale;
                
                uniform vec3 viewPosition;
                uniform vec3 fogColor;
                uniform float fogDensity;
                
                out vec4 fragColor;
                
                float calculateShadow() {
                    // Convert to NDC coordinates
                    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
                    projCoords = projCoords * 0.5 + 0.5;
                
                    // Check if fragment is outside shadow map
                    if (projCoords.z > 1.0) return 0.0;
                
                    // Get current depth
                    float currentDepth = projCoords.z;
                
                    // PCF (Percentage Closer Filtering) for softer shadows
                    float shadow = 0.0;
                    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
                
                    for (int x = -1; x <= 1; ++x) {
                        for (int y = -1; y <= 1; ++y) {
                            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
                            shadow += currentDepth - shadowBias > pcfDepth ? 1.0 : 0.0;
                        }
                    }
                    shadow /= 9.0;
                
                    return shadow;
                }
                
                vec3 calculateLighting() {
                    vec3 norm = normalize(fragNormal);
                    vec3 lightDir = normalize(lightPosition - fragPos);
                
                    // Ambient lighting
                    vec3 ambient = ambientStrength * lightColor;
                
                    // Diffuse lighting
                    float diff = max(dot(norm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                
                    // Specular lighting
                    vec3 viewDir = normalize(viewPosition - fragPos);
                    vec3 reflectDir = reflect(-lightDir, norm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
                    vec3 specular = specularStrength * spec * lightColor;
                
                    return ambient + diffuse + specular;
                }
                
                vec3 blendTextures() {
                    vec2 scaledTexCoords = texCoords * textureScale;
                
                    // Sample textures
                    vec3 grass = texture(grassTexture, scaledTexCoords).rgb;
                    vec3 rock = texture(rockTexture, scaledTexCoords).rgb;
                    vec3 sand = texture(sandTexture, scaledTexCoords).rgb;
                    vec3 snow = texture(snowTexture, scaledTexCoords).rgb;
                
                    // Height-based blending
                    float grassWeight = smoothstep(0.0, 0.3, height) * (1.0 - smoothstep(0.3, 0.6, height));
                    float rockWeight = smoothstep(0.2, 0.5, height) * (1.0 - smoothstep(0.5, 0.8, height));
                    float sandWeight = 1.0 - smoothstep(0.0, 0.2, height);
                    float snowWeight = smoothstep(0.7, 1.0, height);
                
                    // Normalize weights
                    float totalWeight = grassWeight + rockWeight + sandWeight + snowWeight;
                    if (totalWeight > 0.0) {
                        grassWeight /= totalWeight;
                        rockWeight /= totalWeight;
                        sandWeight /= totalWeight;
                        snowWeight /= totalWeight;
                    }
                
                    // Blend textures
                    vec3 blended = grass * grassWeight + rock * rockWeight + 
                                  sand * sandWeight + snow * snowWeight;
                
                    return blended;
                }
                
                void main() {
                    // Calculate base color from blended textures
                    vec3 baseColor = blendTextures();
                
                    // Calculate lighting
                    vec3 lighting = calculateLighting();
                
                    // Calculate shadow
                    float shadow = calculateShadow();
                
                    // Apply shadow to lighting (preserve ambient)
                    vec3 ambient = ambientStrength * lightColor;
                    vec3 finalLighting = ambient + (lighting - ambient) * (1.0 - shadow);
                
                    // Apply lighting to base color
                    vec3 finalColor = baseColor * finalLighting;
                
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
