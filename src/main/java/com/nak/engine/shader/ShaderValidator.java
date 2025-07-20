package com.nak.engine.shader;

public class ShaderValidator {

    public void validateVertexShader(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new RuntimeException("Vertex shader source is empty");
        }

        if (!source.contains("#version")) {
            throw new RuntimeException("Vertex shader missing version directive");
        }

        if (!source.contains("void main()")) {
            throw new RuntimeException("Vertex shader missing main function");
        }
    }

    public void validateFragmentShader(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new RuntimeException("Fragment shader source is empty");
        }

        if (!source.contains("#version")) {
            throw new RuntimeException("Fragment shader missing version directive");
        }

        if (!source.contains("void main()")) {
            throw new RuntimeException("Fragment shader missing main function");
        }
    }
}
