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

package com.aol.mobile.sdk.renderer.sensor;

import android.support.annotation.Nullable;

import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import static com.aol.mobile.sdk.renderer.utils.AngleHelper.asPiMinusPiRange;

public final class CameraOrientationModule {
    @Nullable
    private VideoVM.Callbacks callbacks;
    @Nullable
    private Camera camera;
    @Nullable
    private Device center;

    public void setCallbacks(@Nullable VideoVM.Callbacks callbacks) {
        if (this.callbacks != callbacks) {
            this.callbacks = callbacks;
        }
    }

    public void updateCameraPosition(double longitude, double latitude, boolean isCentered) {
        if (isCentered) center = null;

        // Changing central device orientation vector according to camera move
        if (center != null && camera != null) {
            double longDiff = longitude - camera.lng;
            double latDiff = latitude - camera.lat;
            double azim = asPiMinusPiRange(center.azim + longDiff);
            double roll = asPiMinusPiRange(center.roll + latDiff);

            center = new Device(azim, center.pitch, roll);
        }

        camera = new Camera(longitude, latitude);
    }

    public void updateDeviceOrientation(double azimuth, double pitch, double roll) {
        if (center == null) {
            if (camera == null) {
                center = new Device(azimuth, pitch, roll);
            } else {
                double centerAzim = asPiMinusPiRange(camera.lng + azimuth);
                double centerRoll = asPiMinusPiRange(camera.lat + roll);
                center = new Device(centerAzim, pitch, centerRoll);
            }
        }

        if (callbacks != null && camera != null) {
            double longitude = asPiMinusPiRange(center.azim - azimuth);
            double latitude = asPiMinusPiRange(center.roll - roll);

            camera = new Camera(longitude, latitude);
            callbacks.onCameraDirectionChanged(camera.lng, camera.lat);
        }
    }

    static class Device {
        final double azim;
        final double pitch;
        final double roll;

        Device(double azim, double pitch, double roll) {
            this.azim = azim;
            this.pitch = pitch;
            this.roll = roll;
        }
    }

    static class Camera {
        final double lng;
        final double lat;

        Camera(double lng, double lat) {
            this.lng = lng;
            this.lat = lat;
        }
    }
}
