package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aol.mobile.sdk.renderer.sensor.CameraOrientationModule;
import com.aol.mobile.sdk.renderer.sensor.CameraOrientationSensor;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import static java.lang.Math.abs;

class ThreeSixtyRenderer extends ExoVideoRenderer {
    private static final double E = .00000000001;
    @NonNull
    private final CameraOrientationModule cameraOrientationModule = new CameraOrientationModule();
    @NonNull
    private final CameraOrientationSensor cameraOrientationSensor;
    @Nullable
    private String videoUrl;
    private double lng, lat;

    public ThreeSixtyRenderer(@NonNull Context context) {
        super(context);
        cameraOrientationSensor = new CameraOrientationSensor(context, new CameraOrientationSensor.Listener() {
            @Override
            public void onOrientationChange(float azimuth, float pitch, float roll) {
                cameraOrientationModule.updateDeviceOrientation(azimuth, pitch, roll);
            }
        });
    }

    @Override
    public void setListener(@Nullable Listener listener) {
        super.setListener(listener);
        cameraOrientationModule.setListener(listener);
    }

    @Override
    public void render(VideoVM videoVM) {
        super.render(videoVM);

        boolean isSourceChanged;
        if (videoUrl != videoVM.videoUrl) {
            videoUrl = videoVM.videoUrl;
            isSourceChanged = true;
        } else {
            isSourceChanged = false;
        }

        boolean isLngChanged = abs(lng - videoVM.longitude) > E;
        boolean isLatChanged = abs(lat - videoVM.latitude) > E;

        lng = videoVM.longitude;
        lat = videoVM.latitude;

        if (isSourceChanged || isLngChanged || isLatChanged) {
            cameraOrientationModule.updateCameraPosition(lng, lat, isSourceChanged);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        cameraOrientationSensor.dispose();
    }
}
