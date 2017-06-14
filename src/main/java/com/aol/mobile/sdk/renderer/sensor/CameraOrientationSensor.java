package com.aol.mobile.sdk.renderer.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.WindowManager;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;
import static android.hardware.SensorManager.AXIS_MINUS_Y;
import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Y;
import static android.hardware.SensorManager.SENSOR_DELAY_FASTEST;
import static android.hardware.SensorManager.getOrientation;
import static android.hardware.SensorManager.getRotationMatrixFromVector;
import static android.hardware.SensorManager.remapCoordinateSystem;
import static java.lang.Math.abs;

@SuppressWarnings("SuspiciousNameCombination")
public final class CameraOrientationSensor {
    @NonNull
    private final Listener listener;
    @NonNull
    private final SensorManager sensorManager;
    @NonNull
    private final WindowManager windowManager;
    @Nullable
    private final Sensor rotationVectorSensor;

    private final SensorEventListener eventListener = new SensorEventListener() {
        private static final float E = .00000000001f;
        private final float[] rotationMatrix = new float[9];
        private final float[] orientationAngles = new float[3];
        @Nullable
        private float[] prevAngles;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return;

            switch (event.sensor.getType()) {
                case TYPE_ROTATION_VECTOR:
                    float[] rotationMatrix = new float[9];
                    getRotationMatrixFromVector(rotationMatrix, event.values);

                    switch (windowManager.getDefaultDisplay().getRotation()) {
                        case Surface.ROTATION_90:
                            remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_MINUS_Y, this.rotationMatrix);
                            break;

                        case Surface.ROTATION_270:
                            remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Y, this.rotationMatrix);
                            break;

                        case Surface.ROTATION_180:
                            remapCoordinateSystem(rotationMatrix, AXIS_MINUS_Y, AXIS_X, this.rotationMatrix);
                            break;

                        default:
                        case Surface.ROTATION_0:
                            remapCoordinateSystem(rotationMatrix, AXIS_Y, AXIS_X, this.rotationMatrix);
                            break;
                    }

                    getOrientation(this.rotationMatrix, orientationAngles);

                    if (prevAngles != null && isOrientationChanged(prevAngles, orientationAngles)) {
                        listener.onOrientationChange(orientationAngles[0], orientationAngles[1], orientationAngles[2]);
                    } else {
                        prevAngles = new float[3];
                    }

                    System.arraycopy(orientationAngles, 0, prevAngles, 0, prevAngles.length);
                    break;
            }
        }

        private boolean isOrientationChanged(float[] prevAngles, float[] newAngles) {
            return abs(prevAngles[0] - newAngles[0]) > E ||
                    abs(prevAngles[1] - newAngles[1]) > E ||
                    abs(prevAngles[2] - newAngles[2]) > E;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public CameraOrientationSensor(@NonNull Context context, @NonNull Listener listener) {
        this.listener = listener;

        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR);
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(eventListener, rotationVectorSensor, SENSOR_DELAY_FASTEST);
        }
    }

    public void dispose() {
        if (rotationVectorSensor != null) {
            sensorManager.unregisterListener(eventListener);
        }
    }

    public interface Listener {
        void onOrientationChange(float azimuth, float pitch, float roll);
    }
}