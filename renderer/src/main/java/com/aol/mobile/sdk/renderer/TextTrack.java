/*
 * Copyright (c) 2017. Oath.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;

public final class TextTrack {
    @NonNull
    public final String title;
    public final boolean isSelected;
    public final boolean isEmpty;
    @NonNull
    final Id id;

    TextTrack(@NonNull Id id, @NonNull String title, boolean isSelected) {
        this.id = id;
        this.title = title;
        this.isSelected = isSelected;
        this.isEmpty = id.group == -1 && id.track == -1;
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
