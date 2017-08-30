package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;

public final class CcTrack extends Track {
    CcTrack(@NonNull Track.Id id, @NonNull Format format, boolean isSelected) {
        super(id, format, isSelected);
    }

    @NonNull
    public CcTrack withSelected(boolean isSelected) {
        return new CcTrack(id, format, isSelected);
    }
}
