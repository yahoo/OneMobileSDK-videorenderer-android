/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.aol.mobile.sdk.renderer.gles.VideoSurfaceListener;

@SuppressLint("ViewConstructor")
public final class FlatRendererView extends TextureView {
    public FlatRendererView(@NonNull Context context, @NonNull final VideoSurfaceListener videoSurfaceListener) {
        super(context);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Nullable
            private Surface surface;

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                this.surface = new Surface(surface);
                videoSurfaceListener.onVideoSurfaceAvailable(this.surface);
                videoSurfaceListener.onVideoSurfaceResized(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                videoSurfaceListener.onVideoSurfaceResized(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (this.surface != null) {
                    this.surface.release();
                    videoSurfaceListener.onVideoSurfaceReleased(this.surface);
                    this.surface = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }
}
