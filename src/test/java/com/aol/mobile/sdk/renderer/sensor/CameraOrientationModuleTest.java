package com.aol.mobile.sdk.renderer.sensor;

import com.aol.mobile.sdk.renderer.VideoRenderer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static java.lang.Math.toRadians;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CameraOrientationModuleTest {
    private static final float P = 0.0001f;

    private VideoRenderer.Listener listener;
    private CameraOrientationModule com;

    private float asRad(float degrees) {
        return (float) toRadians(degrees);
    }

    @Before
    public void setUp() throws Exception {
        listener = mock(VideoRenderer.Listener.class);
        com = new CameraOrientationModule();
        com.setListener(listener);
    }

    @Test
    public void testInitialState() {
        com.updateCameraPosition(0.0, 0.0, true);
        com.updateCameraPosition(0.0, 0.0, false);
        com.updateDeviceOrientation(1.5707963705062866, -0.0, -3.1415927410125732);

        verify(listener).onCameraDirectionChanged(eq(0, P), eq(0, P));
    }

    @Test
    public void testNoInteractionOnCameraUpdate() {
        com.updateCameraPosition(0f, 0f, true);
        com.updateCameraPosition(0f, 2f, false);
        com.updateCameraPosition(-1f, 2f, false);

        Mockito.verifyZeroInteractions(listener);
    }

    @Test
    public void testTrivialBehavior() {
        com.updateCameraPosition(0, 0, true);
        com.updateDeviceOrientation(asRad(0), 0, asRad(90));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(0, P));

        com.updateDeviceOrientation(asRad(10), 0, asRad(100));
        verify(listener).onCameraDirectionChanged(eq(asRad(-10), P), eq(asRad(-10), P));

        com.updateCameraPosition(0, 0, false);
        com.updateDeviceOrientation(asRad(20), 0, asRad(110));
        verify(listener, times(2)).onCameraDirectionChanged(eq(asRad(-10), P), eq(asRad(-10), P));

        com.updateDeviceOrientation(asRad(10), 0, asRad(100));
        verify(listener, times(2)).onCameraDirectionChanged(eq(0, P), eq(0, P));

        com.updateDeviceOrientation(asRad(0), 0, asRad(90));
        verify(listener).onCameraDirectionChanged(eq(asRad(10), P), eq(asRad(10), P));
    }

    @Test
    public void testCriticalAzimuthPoints() {
        com.updateCameraPosition(0f, 0f, true);
        com.updateDeviceOrientation(asRad(175), 0, 0);

        com.updateDeviceOrientation(asRad(-175), 0, 0);
        verify(listener).onCameraDirectionChanged(eq(asRad(-10), P), eq(0, P));

        com.updateDeviceOrientation(asRad(0), 0, 0);
        verify(listener).onCameraDirectionChanged(eq(asRad(175), P), eq(0, P));

        com.updateDeviceOrientation(asRad(180), 0, 0);
        verify(listener).onCameraDirectionChanged(eq(asRad(-5), P), eq(0, P));

        com.updateDeviceOrientation(asRad(-180), 0, 0);
        verify(listener, times(2)).onCameraDirectionChanged(eq(asRad(-5), P), eq(0, P));

        com.updateCameraPosition(0f, 0f, true);
        com.updateDeviceOrientation(asRad(90), 0, 0);
        verify(listener, times(2)).onCameraDirectionChanged(eq(0, P), eq(0, P));
    }

    @Test
    public void testCriticalRollPoints() {
        com.updateCameraPosition(0f, 0f, true);
        com.updateDeviceOrientation(asRad(180), 0, asRad(90));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(0), P));

        com.updateDeviceOrientation(asRad(180), 0, asRad(30));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(60), P));

        com.updateDeviceOrientation(asRad(180), 0, asRad(-100));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(-170), P));

        com.updateDeviceOrientation(asRad(180), 0, asRad(-110));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(-160), P));

        com.updateDeviceOrientation(asRad(180), 0, asRad(0));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(90), P));

        com.updateCameraPosition(0f, asRad(90), false);
        com.updateDeviceOrientation(asRad(180), 0, asRad(66));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(asRad(24), P));
    }

    @Test
    public void testOutOfScopeCameraDirectionChange() {
        com.updateCameraPosition(0f, 0f, true);
        com.updateDeviceOrientation(0, 0, asRad(30));
        verify(listener).onCameraDirectionChanged(eq(0, P), eq(0, P));

        com.updateDeviceOrientation(asRad(90), 0, asRad(0));
        verify(listener).onCameraDirectionChanged(eq(asRad(-90), P), eq(asRad(30), P));

        com.updateCameraPosition(asRad(-60), asRad(30), false);
        com.updateDeviceOrientation(asRad(90), 0, asRad(0));
        verify(listener).onCameraDirectionChanged(eq(asRad(-60), P), eq(asRad(30), P));

        com.updateCameraPosition(asRad(-180), asRad(-60), false);
        com.updateDeviceOrientation(asRad(90), 0, asRad(0));
        verify(listener).onCameraDirectionChanged(eq(asRad(180), P), eq(asRad(-60), P));
    }
}