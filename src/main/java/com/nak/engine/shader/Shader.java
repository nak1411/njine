package com.nak.engine.shader;

import org.lwjgl.opengl.GL20;

public class Shader {
    private final int shaderId;
    private final String name;
    private final int type;
    private boolean compiled = false;

    public Shader(String name, String source, int type) {
        this.name = name;
        this.type = type;
        this.shaderId = GL20.glCreateShader(type);

        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader '" + name + "'. Type: " + getTypeString(type));
        }

        compile(source);
    }

    private void compile(String source) {
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            String error = GL20.glGetShaderInfoLog(shaderId, 1024);
            cleanup();
            throw new RuntimeException("Error compiling shader '" + name + "':\n" + error);
        }

        compiled = true;
    }

    private String getTypeString(int type) {
        return switch (type) {
            case GL20.GL_VERTEX_SHADER -> "VERTEX";
            case GL20.GL_FRAGMENT_SHADER -> "FRAGMENT";
            case 0x8DD9 -> "GEOMETRY"; // GL_GEOMETRY_SHADER
            case 0x91B9 -> "COMPUTE";  // GL_COMPUTE_SHADER
            default -> "UNKNOWN(" + type + ")";
        };
    }

    public void cleanup() {
        if (shaderId != 0) {
            GL20.glDeleteShader(shaderId);
        }
    }

    // Getters
    public int getShaderId() {
        return shaderId;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public boolean isCompiled() {
        return compiled;
    }
}
