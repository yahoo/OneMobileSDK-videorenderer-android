package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import com.google.android.exoplayer2.Format;


public final class AudioTrack extends Track {
    public final int channelCount;
    public final int sampleRate;

    AudioTrack(@NonNull Id id, @NonNull Format format, boolean isSelected) {
        super(id, format, isSelected);
        this.channelCount = format.channelCount;
        this.sampleRate = format.sampleRate;
    }

    @NonNull
    public AudioTrack withSelected(boolean isSelected) {
        return new AudioTrack(id, format, isSelected);
    }
}
