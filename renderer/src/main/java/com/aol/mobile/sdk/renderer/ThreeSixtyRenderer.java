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
    public void render(@NonNull VideoVM videoVM) {
        cameraOrientationModule.setCallbacks(videoVM.callbacks);
        super.render(videoVM);

        boolean isSourceChanged;
        if ((videoUrl == null && videoVM.videoUrl != null) ||
                (videoUrl != null && !videoUrl.equals(videoVM.videoUrl))) {
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
