package com.aol.mobile.sdk.renderer.viewmodel;

import android.support.annotation.Nullable;
import com.aol.mobile.sdk.renderer.AudioTrack;
import com.aol.mobile.sdk.renderer.CcTrack;

public class VideoVM {
    @Nullable
    public AudioTrack selectedAudioTrack;
    @Nullable
    public CcTrack selectedCcTrack;
    @Nullable
    public String videoUrl;
    @Nullable
    public String subtitleLang;
    @Nullable
    public String subtitleUrl;
    @Nullable
    public Long seekPosition;
    public boolean isScalable = true;
    public boolean isMaintainAspectRatio = true;
    public boolean isMuted;
    public boolean shouldPlay;
    public double longitude;
    public double latitude;
}
