/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

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

    /**
     * Gets renderer viewport as android {@link View} instance
     *
     * @return android {@link View} port
     */
    @NonNull
    View getViewport();

    interface Producer {
        @NonNull
        VideoRenderer createRenderer(@NonNull Context context);
    }
}
