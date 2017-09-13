package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;

public final class TextTrack {
    @NonNull
    public final String title;
    public final boolean isSelected;
    @NonNull
    final Id id;

    TextTrack(@NonNull Id id, @NonNull String title, boolean isSelected) {
        this.id = id;
        this.title = title;
        this.isSelected = isSelected;
    }

    @NonNull
    public TextTrack withSelected(boolean isSelected) {
        return new TextTrack(id, title, isSelected);
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
