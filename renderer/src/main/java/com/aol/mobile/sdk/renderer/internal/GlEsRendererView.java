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

package com.aol.mobile.sdk.renderer.internal;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.aol.mobile.sdk.renderer.gles.GLESModel;
import com.aol.mobile.sdk.renderer.gles.SceneRenderer;
import com.aol.mobile.sdk.renderer.gles.VideoSurfaceListener;

@MainThread
public final class GlEsRendererView extends GLSurfaceView {
    private final SceneRenderer renderer;
    private double longitude;
    private double latitude;

    public GlEsRendererView(@NonNull Context context) {
        this(context, null);
    }

    public GlEsRendererView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        if (isInEditMode()) {
            renderer = null;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public GlEsRendererView(@NonNull Context context, @NonNull GLESModel model, @NonNull VideoSurfaceListener videoSurfaceListener) {
        super(context);

        setEGLContextClientVersion(2);

        setRenderer(renderer = new SceneRenderer(model, videoSurfaceListener));
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void dispose() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.dispose();
            }
        });
    }

    public void setCameraOrientation(final double longitude, final double latitude) {
        if (longitude == this.longitude && latitude == this.latitude) return;
        this.longitude = longitude;
        this.latitude = latitude;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setCameraOrientation(longitude, latitude);
            }
        });
    }
}
