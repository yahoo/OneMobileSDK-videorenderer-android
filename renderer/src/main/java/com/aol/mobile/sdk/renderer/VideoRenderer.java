/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

public interface VideoRenderer {
    /**
     * Renders video view model
     *
     * @param videoVM instance {@link VideoVM}
     */
    void render(@NonNull VideoVM videoVM);

    /**
     * Disposes all resources
     */
    void dispose();

    interface Producer {
        @NonNull
        VideoRenderer createRenderer(@NonNull Context context);
    }

    VideoRenderer getAdRenderer();
}
