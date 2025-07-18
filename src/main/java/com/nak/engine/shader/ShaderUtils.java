package com.nak.engine.shader;

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
     * Set common camera uniforms
     */
    public static void setCameraUniforms(ShaderProgram program, Matrix4f view, Matrix4f projection, Vector3f position) {
        if (program.hasUniform("viewMatrix")) {
            program.setUniform("viewMatrix", view);
        }
        if (program.hasUniform("projectionMatrix")) {
            program.setUniform("projectionMatrix", projection);
        }
        if (program.hasUniform("viewPosition")) {
            program.setUniform("viewPosition", position);
        }
    }

    /**
     * Set common lighting uniforms
     */
    public static void setLightingUniforms(ShaderProgram program, Vector3f lightPos, Vector3f lightColor,
                                           Vector3f lightDir, float ambient, float specular, float shininess) {
        if (program.hasUniform("lightPosition")) {
            program.setUniform("lightPosition", lightPos);
        }
        if (program.hasUniform("lightColor")) {
            program.setUniform("lightColor", lightColor);
        }
        if (program.hasUniform("lightDirection")) {
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
     * Set fog uniforms
     */
    public static void setFogUniforms(ShaderProgram program, Vector3f fogColor, float fogDensity) {
        if (program.hasUniform("fogColor")) {
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
        if (program.hasUniform("time")) {
            program.setUniform("time", time);
        }
    }

    /**
     * Validate shader program has required uniforms
     */
    public static boolean validateRequiredUniforms(ShaderProgram program, String... requiredUniforms) {
        for (String uniform : requiredUniforms) {
            if (!program.hasUniform(uniform)) {
                System.err.println("Missing required uniform '" + uniform + "' in program: " + program.getName());
                return false;
            }
        }
        return true;
    }
}
