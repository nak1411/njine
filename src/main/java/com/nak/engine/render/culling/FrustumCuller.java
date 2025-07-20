package com.nak.engine.render.culling;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FrustumCuller {
    private final Vector4f[] planes = new Vector4f[6];

    public FrustumCuller() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Vector4f();
        }
    }

    public void updateFrustum(Matrix4f viewProjectionMatrix) {
        // Extract frustum planes from view-projection matrix
        Matrix4f m = new Matrix4f(viewProjectionMatrix);

        // Left plane
        planes[0].set(m.m30() + m.m00(), m.m31() + m.m01(), m.m32() + m.m02(), m.m33() + m.m03());
        // Right plane
        planes[1].set(m.m30() - m.m00(), m.m31() - m.m01(), m.m32() - m.m02(), m.m33() - m.m03());
        // Bottom plane
        planes[2].set(m.m30() + m.m10(), m.m31() + m.m11(), m.m32() + m.m12(), m.m33() + m.m13());
        // Top plane
        planes[3].set(m.m30() - m.m10(), m.m31() - m.m11(), m.m32() - m.m12(), m.m33() - m.m13());
        // Near plane
        planes[4].set(m.m30() + m.m20(), m.m31() + m.m21(), m.m32() + m.m22(), m.m33() + m.m23());
        // Far plane
        planes[5].set(m.m30() - m.m20(), m.m31() - m.m21(), m.m32() - m.m22(), m.m33() - m.m23());

        // Normalize planes
        for (Vector4f plane : planes) {
            float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
            if (length > 0) {
                plane.div(length);
            }
        }
    }

    public boolean isPointInFrustum(Vector3f point) {
        for (Vector4f plane : planes) {
            if (plane.x * point.x + plane.y * point.y + plane.z * point.z + plane.w <= 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isSphereInFrustum(Vector3f center, float radius) {
        for (Vector4f plane : planes) {
            if (plane.x * center.x + plane.y * center.y + plane.z * center.z + plane.w <= -radius) {
                return false;
            }
        }
        return true;
    }
}
