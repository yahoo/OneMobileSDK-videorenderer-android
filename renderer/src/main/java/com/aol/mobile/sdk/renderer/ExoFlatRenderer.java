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

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.chromecast.CastRenderer;
import com.aol.mobile.sdk.chromecast.CastRendererImpl;
import com.aol.mobile.sdk.chromecast.OneCastManager;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.viewmodel.CastVideoVMTranslator;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

@PublicApi
public final class ExoFlatRenderer extends ExoVideoRenderer {
    private CastRenderer castRenderer;
    private CastVideoVMTranslator translator;
    private View flatRendererView;

    public ExoFlatRenderer(@NonNull Context context) {
        super(context);
        flatRendererView = new FlatRendererView(getContext(), this);
        setRenderer(flatRendererView);
    }

    public void render(@NonNull VideoVM videoVM) {
        if (videoVM.isCasting) {
            showCastView(videoVM);
        } else {
            showFlatView(videoVM);
        }

        if (castRenderer != null && translator != null) {
            castRenderer.render(translator.translate(videoVM));
            videoVM.shouldPlay = false;
        }
        super.render(videoVM);
    }

    private void showCastView(@NonNull VideoVM videoVM) {
        if (castRenderer == null) {
            castRenderer = getCastRenderer(getContext());
            translator = new CastVideoVMTranslator();
            if (castRenderer != null) {
                addView(castRenderer.getViewport(), MATCH_PARENT, MATCH_PARENT);
            }
        }
    }

    private void showFlatView(@NonNull VideoVM videoVM) {
        if (castRenderer != null) {
            removeView(castRenderer.getViewport());
            if (videoVM.currentPosition != null) {
                videoVM.seekPosition = videoVM.currentPosition;
            }
            castRenderer = null;
            stopCasting();
        }
    }

    @Nullable
    private CastRenderer getCastRenderer(@NonNull Context context) {
        return new CastRendererImpl(context);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (castRenderer != null) {
            stopCasting();
        }
    }

    private void stopCasting() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                OneCastManager.stopCasting(getContext());
            }
        });
    }
}
