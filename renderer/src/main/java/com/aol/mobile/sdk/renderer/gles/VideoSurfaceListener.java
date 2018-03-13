/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer.gles;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.view.Surface;

@MainThread
public interface VideoSurfaceListener {
    void onVideoSurfaceAvailable(@NonNull Surface surface);

    void onVideoSurfaceResized(int width, int height);

    void onVideoSurfaceReleased(@NonNull Surface surface);
}
