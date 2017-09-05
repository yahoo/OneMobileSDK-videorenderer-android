package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;

abstract class Track {
    @NonNull
    public final Track.Id id;
    @NonNull
    protected final Format format;
    @Nullable
    public final String mimeType;
    @Nullable
    public final String language;
    @Nullable
    public final String title;
    @Nullable
    public final boolean isSelected;
    public final boolean isDefault;

    Track(@NonNull Track.Id id, @NonNull Format format, boolean isSelected) {
        this.id = id;
        this.format = format;
        this.title = format.id;
        this.mimeType = format.sampleMimeType;
        this.language = format.language;
        this.isSelected = isSelected;
        this.isDefault = (format.selectionFlags & C.SELECTION_FLAG_DEFAULT) == 0;
    }

    public static final class Id {
        public final int renderer;
        public final int group;
        public final int track;

        public Id(int renderer, int group, int track) {
            this.renderer = renderer;
            this.group = group;
            this.track = track;
        }
    }
}
