package com.nak.engine.util;

import com.nak.engine.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Utility class for common shader operations
 */
public class ShaderUtils {

    /**
     * Create a model-view-projection matrix
     */
    public static Matrix4f createMVPMatrix(Matrix4f model, Matrix4f view, Matrix4f projection) {
        return new Matrix4f(projection).mul(view).mul(model);
    }

    /**
     * Set common camera uniforms with null checks
     */
    public static void setCameraUniforms(ShaderProgram program, Matrix4f view, Matrix4f projection, Vector3f position) {
        if (program == null) return;

        if (program.hasUniform("viewMatrix") && view != null) {
            program.setUniform("viewMatrix", view);
        }
        if (program.hasUniform("projectionMatrix") && projection != null) {
            program.setUniform("projectionMatrix", projection);
        }
        if (program.hasUniform("viewPosition") && position != null) {
            program.setUniform("viewPosition", position);
        }
    }

    /**
     * Set common lighting uniforms with null checks
     */
    public static void setLightingUniforms(ShaderProgram program, Vector3f lightPos, Vector3f lightColor,
                                           Vector3f lightDir, float ambient, float specular, float shininess) {
        if (program == null) return;

        if (program.hasUniform("lightPosition") && lightPos != null) {
            program.setUniform("lightPosition", lightPos);
        }
        if (program.hasUniform("lightColor") && lightColor != null) {
            program.setUniform("lightColor", lightColor);
        }
        if (program.hasUniform("lightDirection") && lightDir != null) {
            program.setUniform("lightDirection", lightDir);
        }
        if (program.hasUniform("ambientStrength")) {
            program.setUniform("ambientStrength", ambient);
        }
        if (program.hasUniform("specularStrength")) {
            program.setUniform("specularStrength", specular);
        }
        if (program.hasUniform("shininess")) {
            program.setUniform("shininess", shininess);
        }
    }

    /**
     * Set fog uniforms with null checks
     */
    public static void setFogUniforms(ShaderProgram program, Vector3f fogColor, float fogDensity) {
        if (program == null) return;

        if (program.hasUniform("fogColor") && fogColor != null) {
            program.setUniform("fogColor", fogColor);
        }
        if (program.hasUniform("fogDensity")) {
            program.setUniform("fogDensity", fogDensity);
        }
    }

    /**
     * Set time uniform
     */
    public static void setTimeUniform(ShaderProgram program, float time) {
        if (program == null) return;

        if (program.hasUniform("time")) {
            program.setUniform("time", time);
        }
    }

    /**
     * Validate shader program has required uniforms
     */
    public static boolean validateRequiredUniforms(ShaderProgram program, String... requiredUniforms) {
        if (program == null) {
            System.err.println("Cannot validate uniforms - shader program is null");
            return false;
        }

        boolean allPresent = true;
        for (String uniform : requiredUniforms) {
            if (!program.hasUniform(uniform)) {
                System.err.println("Missing required uniform '" + uniform + "' in program: " + program.getName());
                allPresent = false;
            }
        }
        return allPresent;
    }

    /**
     * Safely set uniform with existence check
     */
    public static void safeSetUniform(ShaderProgram program, String name, Object value) {
        if (program == null || !program.hasUniform(name) || value == null) {
            return;
        }

        try {
            if (value instanceof Float) {
                program.setUniform(name, (Float) value);
            } else if (value instanceof Integer) {
                program.setUniform(name, (Integer) value);
            } else if (value instanceof Boolean) {
                program.setUniform(name, (Boolean) value);
            } else if (value instanceof Vector3f) {
                program.setUniform(name, (Vector3f) value);
            } else if (value instanceof Matrix4f) {
                program.setUniform(name, (Matrix4f) value);
            }
        } catch (Exception e) {
            System.err.println("Error setting uniform " + name + ": " + e.getMessage());
        }
    }
}
