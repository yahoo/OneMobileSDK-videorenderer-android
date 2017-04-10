/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
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
