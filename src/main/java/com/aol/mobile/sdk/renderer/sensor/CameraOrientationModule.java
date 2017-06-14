package com.aol.mobile.sdk.renderer.sensor;

import android.support.annotation.Nullable;

import com.aol.mobile.sdk.renderer.VideoRenderer;

import static com.aol.mobile.sdk.renderer.utils.AngleHelper.asPiMinusPiRange;

public final class CameraOrientationModule {
    @Nullable
    private VideoRenderer.Listener listener;
    @Nullable
    private Camera camera;
    @Nullable
    private Device center;

    public void setListener(@Nullable VideoRenderer.Listener listener) {
        this.listener = listener;
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

        if (listener != null && camera != null) {
            double longitude = asPiMinusPiRange(center.azim - azimuth);
            double latitude = asPiMinusPiRange(center.roll - roll);

            camera = new Camera(longitude, latitude);
            listener.onCameraDirectionChanged(camera.lng, camera.lat);
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
