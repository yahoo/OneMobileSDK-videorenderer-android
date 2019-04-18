/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.renderer.chromecast.CastRenderer;
import com.aol.mobile.sdk.renderer.chromecast.CastRendererImpl;
import com.aol.mobile.sdk.renderer.chromecast.CastVideoVMTranslator;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.google.android.gms.cast.framework.CastContext.getSharedInstance;

@PublicApi
public final class ExoFlatRenderer extends ExoVideoRenderer {
    private CastRenderer castRenderer;
    private CastVideoVMTranslator translator;
    private Context context;

    public ExoFlatRenderer(@NonNull Context context) {
        super(context);
        View flatRendererView = new FlatRendererView(context, this);
        setRenderer(flatRendererView);
        this.context = context;
    }

    public void render(@NonNull VideoVM videoVM) {
        if (videoVM.isCasting) {
            showCastView();
        } else {
            showFlatView(videoVM);
        }

        if (castRenderer != null && translator != null) {
            castRenderer.render(translator.translate(videoVM));
            videoVM.shouldPlay = false;
        }
        super.render(videoVM);
    }

    private void showCastView() {
        if (castRenderer == null) {
            castRenderer = getCastRenderer();
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

    @NonNull
    private CastRenderer getCastRenderer() {
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
        getHandler().post(
                () -> getSharedInstance(context).getSessionManager().endCurrentSession(true)
        );
    }
}
