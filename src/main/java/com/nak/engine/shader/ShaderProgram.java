package com.nak.engine.shader;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class ShaderProgram {
    private final int programId;
    private final String name;
    private final List<Shader> attachedShaders = new ArrayList<>();
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private final Map<String, UniformInfo> uniformInfo = new HashMap<>();
    private boolean linked = false;
    private boolean inUse = false;

    // Matrix buffer for uniform uploads
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(String name) {
        this.name = name;
        this.programId = GL20.glCreateProgram();

        if (programId == 0) {
            throw new RuntimeException("Could not create shader program: " + name);
        }
    }

    /**
     * Attach a shader to this program
     */
    public void attachShader(Shader shader) {
        if (linked) {
            throw new RuntimeException("Cannot attach shader to already linked program: " + name);
        }

        GL20.glAttachShader(programId, shader.getShaderId());
        attachedShaders.add(shader);
    }

    /**
     * Link the shader program
     */
    public void link() {
        if (linked) {
            throw new RuntimeException("Program already linked: " + name);
        }

        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            String error = GL20.glGetProgramInfoLog(programId, 1024);
            throw new RuntimeException("Error linking shader program '" + name + "':\n" + error);
        }

        // Detach shaders after linking (they're no longer needed)
        for (Shader shader : attachedShaders) {
            GL20.glDetachShader(programId, shader.getShaderId());
        }

        // Validate program
        GL20.glValidateProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader program '" + name + "':\n" +
                    GL20.glGetProgramInfoLog(programId, 1024));
        }

        linked = true;

        // Cache all uniform locations
        cacheUniformLocations();
    }

    /**
     * Cache all uniform locations for faster access
     */
    private void cacheUniformLocations() {
        int uniformCount = GL20.glGetProgrami(programId, GL20.GL_ACTIVE_UNIFORMS);

        // Create buffers for the glGetActiveUniform call
        IntBuffer sizeBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer typeBuffer = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < uniformCount; i++) {
            // Reset buffers
            sizeBuffer.clear();
            typeBuffer.clear();

            // Get uniform info - this is the corrected method call
            String uniformName = GL20.glGetActiveUniform(programId, i, sizeBuffer, typeBuffer);

            if (uniformName != null) {
                // Remove array indicators for cleaner names
                if (uniformName.contains("[")) {
                    uniformName = uniformName.substring(0, uniformName.indexOf('['));
                }

                int location = GL20.glGetUniformLocation(programId, uniformName);
                if (location != -1) {
                    uniformLocations.put(uniformName, location);

                    // Store uniform info
                    int size = sizeBuffer.get(0);
                    int type = typeBuffer.get(0);
                    uniformInfo.put(uniformName, new UniformInfo(uniformName, type, size));
                }
            }
        }

        System.out.println("Cached " + uniformLocations.size() + " uniforms for program: " + name);
    }

    /**
     * Bind this shader program for use
     */
    public void bind() {
        if (!linked) {
            throw new RuntimeException("Cannot bind unlinked shader program: " + name);
        }

        GL20.glUseProgram(programId);
        inUse = true;
    }

    /**
     * Unbind the shader program
     */
    public void unbind() {
        GL20.glUseProgram(0);
        inUse = false;
    }

    /**
     * Get uniform location with caching
     */
    private int getUniformLocation(String name) {
        Integer location = uniformLocations.get(name);
        if (location == null) {
            location = GL20.glGetUniformLocation(programId, name);
            if (location != -1) {
                uniformLocations.put(name, location);
            }
        }
        return location != null ? location : -1;
    }

    /**
     * Check if uniform exists
     */
    public boolean hasUniform(String name) {
        return getUniformLocation(name) != -1;
    }

    // Uniform setter methods
    public void setUniform(String name, boolean value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1i(location, value ? 1 : 0);
        }
    }

    public void setUniform(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }

    public void setUniform(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }

    public void setUniform(String name, Vector2f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform2f(location, value.x, value.y);
        }
    }

    public void setUniform(String name, float x, float y) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform2f(location, x, y);
        }
    }

    public void setUniform(String name, Vector3f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform3f(location, value.x, value.y, value.z);
        }
    }

    public void setUniform(String name, float x, float y, float z) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    public void setUniform(String name, Vector4f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform4f(location, value.x, value.y, value.z, value.w);
        }
    }

    public void setUniform(String name, float x, float y, float z, float w) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    public void setUniform(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            MATRIX_BUFFER.clear();
            matrix.get(MATRIX_BUFFER);
            GL20.glUniformMatrix4fv(location, false, MATRIX_BUFFER);
        }
    }

    public void setUniform(String name, FloatBuffer buffer) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniformMatrix4fv(location, false, buffer);
        }
    }

    // Array uniform setters
    public void setUniform(String name, int[] values) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1iv(location, values);
        }
    }

    public void setUniform(String name, float[] values) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1fv(location, values);
        }
    }

    /**
     * Set texture uniform
     */
    public void setTexture(String name, int textureUnit) {
        setUniform(name, textureUnit);
    }

    /**
     * Set multiple uniforms from a map
     */
    public void setUniforms(Map<String, Object> uniforms) {
        for (Map.Entry<String, Object> entry : uniforms.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Boolean) {
                setUniform(name, (Boolean) value);
            } else if (value instanceof Integer) {
                setUniform(name, (Integer) value);
            } else if (value instanceof Float) {
                setUniform(name, (Float) value);
            } else if (value instanceof Vector2f) {
                setUniform(name, (Vector2f) value);
            } else if (value instanceof Vector3f) {
                setUniform(name, (Vector3f) value);
            } else if (value instanceof Vector4f) {
                setUniform(name, (Vector4f) value);
            } else if (value instanceof Matrix4f) {
                setUniform(name, (Matrix4f) value);
            } else if (value instanceof int[]) {
                setUniform(name, (int[]) value);
            } else if (value instanceof float[]) {
                setUniform(name, (float[]) value);
            } else {
                System.err.println("Unsupported uniform type for '" + name + "': " + value.getClass());
            }
        }
    }

    /**
     * Get all uniform information
     */
    public Map<String, UniformInfo> getUniformInfo() {
        return new HashMap<>(uniformInfo);
    }

    /**
     * Get all uniform names
     */
    public Set<String> getUniformNames() {
        return new HashSet<>(uniformLocations.keySet());
    }

    /**
     * Print all uniforms (for debugging)
     */
    public void printUniforms() {
        System.out.println("Uniforms for program '" + name + "':");
        for (Map.Entry<String, UniformInfo> entry : uniformInfo.entrySet()) {
            UniformInfo info = entry.getValue();
            System.out.println("  " + info.name + " : " + getUniformTypeName(info.type) +
                    (info.size > 1 ? "[" + info.size + "]" : ""));
        }
    }

    private String getUniformTypeName(int type) {
        return switch (type) {
            case GL20.GL_FLOAT -> "float";
            case GL20.GL_FLOAT_VEC2 -> "vec2";
            case GL20.GL_FLOAT_VEC3 -> "vec3";
            case GL20.GL_FLOAT_VEC4 -> "vec4";
            case GL20.GL_INT -> "int";
            case GL20.GL_INT_VEC2 -> "ivec2";
            case GL20.GL_INT_VEC3 -> "ivec3";
            case GL20.GL_INT_VEC4 -> "ivec4";
            case GL20.GL_BOOL -> "bool";
            case GL20.GL_BOOL_VEC2 -> "bvec2";
            case GL20.GL_BOOL_VEC3 -> "bvec3";
            case GL20.GL_BOOL_VEC4 -> "bvec4";
            case GL20.GL_FLOAT_MAT2 -> "mat2";
            case GL20.GL_FLOAT_MAT3 -> "mat3";
            case GL20.GL_FLOAT_MAT4 -> "mat4";
            case GL20.GL_SAMPLER_2D -> "sampler2D";
            case GL20.GL_SAMPLER_CUBE -> "samplerCube";
            default -> "unknown(" + type + ")";
        };
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        unbind();

        if (programId != 0) {
            GL20.glDeleteProgram(programId);
        }

        // Note: Individual shaders are cleaned up by ShaderManager
        attachedShaders.clear();
        uniformLocations.clear();
        uniformInfo.clear();
    }

    // Getters
    public int getProgramId() {
        return programId;
    }

    public String getName() {
        return name;
    }

    public boolean isLinked() {
        return linked;
    }

    public boolean isInUse() {
        return inUse;
    }

    public List<Shader> getAttachedShaders() {
        return new ArrayList<>(attachedShaders);
    }

    /**
     * Uniform information holder
     */
    public static class UniformInfo {
        public final String name;
        public final int type;
        public final int size;

        public UniformInfo(String name, int type, int size) {
            this.name = name;
            this.type = type;
            this.size = size;
        }
    }
}