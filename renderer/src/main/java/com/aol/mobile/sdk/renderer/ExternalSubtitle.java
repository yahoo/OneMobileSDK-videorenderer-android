/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;

public final class ExternalSubtitle {
    @NonNull
    public final String language;
    @NonNull
    public final String url;
    @NonNull
    public final String format;

    public ExternalSubtitle(@NonNull String language, @NonNull String url, @NonNull String format) {
        this.language = language;
        this.url = url;
        this.format = format;
    }
}
