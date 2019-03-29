/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;

public final class TextTrack {
    @NonNull
    public final String title;
    @NonNull
    public final String language;
    public final boolean isSelected;
    public final boolean isEmpty;
    public final boolean isFailed;
    @NonNull
    final Id id;

    TextTrack(@NonNull Id id, @NonNull String title, @NonNull String language, boolean isSelected) {
        this(id, title, language, isSelected, false);
    }

    TextTrack(@NonNull Id id, @NonNull String title, @NonNull String language, boolean isSelected, boolean isFailed) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.isSelected = isSelected;
        this.isEmpty = id.group == -1 && id.track == -1;
        this.isFailed = isFailed;
    }

    @NonNull
    public TextTrack withSelected(boolean isSelected) {
        return new TextTrack(id, title, language, isSelected, isFailed);
    }

    @NonNull
    public TextTrack withFailed(boolean isFailed) {
        return new TextTrack(id, title, language, false, isFailed);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof TextTrack))return false;
        TextTrack track = (TextTrack) other;
        return id.equals(track.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof Id))return false;
            Id id = (Id) other;
            return id.renderer == renderer && id.group == group && id.track == track;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + renderer;
            result = 31 * result + group;
            result = 31 * result + track;
            return result;
        }
    }
}
