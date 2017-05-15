package com.aol.mobile.sdk.renderer.viewmodel;

import android.support.annotation.Nullable;

public class VideoVM {

    @Nullable
    public String videoUrl;
    @Nullable
    public String subtitleUrl;
    public boolean shouldPlay;
    public Long seekPosition;

    public boolean isScalable = true;
    public boolean isMaintainAspectRatio = true;
    public boolean isMuted;
    public double longitude;
    public double latitude;
}
