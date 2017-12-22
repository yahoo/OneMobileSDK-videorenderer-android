/*
 * Copyright (c) 2017. Oath.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
