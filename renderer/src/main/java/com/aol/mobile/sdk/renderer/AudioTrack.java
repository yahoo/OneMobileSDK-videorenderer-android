package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


public final class AudioTrack {
    @NonNull
    public final String title;
    public final boolean isSelected;
    @Nullable
    final String language;
    @NonNull
    final Id id;

    AudioTrack(@NonNull Id id, @Nullable String language, @NonNull String title, boolean isSelected) {
        this.id = id;
        this.language = language;
        this.title = title;
        this.isSelected = isSelected;
    }

    @NonNull
    public AudioTrack withSelected(boolean isSelected) {
        return new AudioTrack(id, language, title, isSelected);
    }

    final static class Id {
        final int renderer;
        final int group;
        final int track;

        Id(int renderer, int group, int track) {
            this.renderer = renderer;
            this.group = group;
            this.track = track;
        }
    }
}
