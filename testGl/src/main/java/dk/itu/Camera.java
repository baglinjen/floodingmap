package dk.itu;

import glm_.mat4x4.Mat4;

import static glm_.Java.glm;

public class Camera {
    private double[] bounds;
    private static final Mat4 std = new Mat4(1.0f);
    private static final float[] stdFloatArray = std.toFloatArray();

    public Camera(double[] bounds) {
        this.bounds = bounds;
    }

    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    public float[] getModelMatrixFloatArray() {
        return stdFloatArray;
    }

    public float[] getViewMatrixFloatArray() {
        return stdFloatArray;
    }

    public float[] getProjectionMatrixFloatArray() {
        return stdFloatArray;
//        return glm.ortho((float) bounds[0], (float) bounds[2], (float) bounds[1], (float) bounds[3], 0f, 100f).toFloatArray();
    }
}
