package com.aol.mobile.sdk.renderer.gles;

import android.opengl.Matrix;
import android.support.annotation.NonNull;

public final class Camera {
    private static final double HALF_PI = Math.PI / 2.;

    private final float fov;
    private final float zFar;
    private final float zNear;
    @NonNull
    private final float[] viewMatrix = new float[16];
    @NonNull
    private final float[] projectionMatrix = new float[16];
    @NonNull
    private final float[] mvpMatrix = new float[16];

    public Camera(float fov, float zNear, float zFar) {
        this.fov = fov;
        this.zNear = zNear;
        this.zFar = zFar;

        lookAt(0, 0);
    }

    public void updateViewportSize(int width, int height) {
        float aspect = (float) width / height;
        float fovY = aspect < 1 ? fov / aspect : fov;

        Matrix.perspectiveM(projectionMatrix, 0, fovY, aspect, zNear, zFar);
    }

    @NonNull
    public float[] getMvpMatrix() {
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        return mvpMatrix;
    }

    public void lookAt(double longitude, double latitude) {
        double targetX = Math.sin(latitude + HALF_PI) * Math.cos(longitude - HALF_PI);
        double targetY = Math.cos(latitude + HALF_PI);
        double targetZ = Math.sin(latitude + HALF_PI) * Math.sin(longitude - HALF_PI);

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 0f, (float) targetX, (float) targetZ, (float) targetY, 0f, 0f, 1f);
    }
}
