package com.aol.mobile.sdk.renderer.viewmodel;

import android.support.annotation.Nullable;

public class VideoVM {

    public boolean shouldPlay;
    public String videoUrl;
    public Long seekPosition;
    public boolean isScalable = true;
    public boolean isMaintainAspectRatio = true;

    public boolean isSourceChanged = true;
    public boolean isMuted;
    public boolean isMuteChanged = true;
    public boolean isPresented;

    public int videoIndex = -1;
    @Nullable
    public String subtitleUrl;
    public double longitude;
    public double latitude;
    public boolean isGeometryChanged;
    public boolean isCameraOrientationChanged;
    public boolean hadError;

}
