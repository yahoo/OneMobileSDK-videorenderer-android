package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


public final class AudioTrack {
    @NonNull
    public final String title;
    public final boolean isSelected;
    @Nullable
    final String language;

    AudioTrack(@Nullable String language, @NonNull String title, boolean isSelected) {
        this.language = language;
        this.title = title;
        this.isSelected = isSelected;
    }

    @NonNull
    public AudioTrack withSelected(boolean isSelected) {
        return new AudioTrack(language, title, isSelected);
    }
}
