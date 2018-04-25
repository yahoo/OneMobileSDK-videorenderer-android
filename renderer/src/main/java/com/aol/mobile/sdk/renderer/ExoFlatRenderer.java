/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    private Context context;
    private boolean isChromecastModulePresent;
    private OneCastManager oneCastManager;

    public ExoFlatRenderer(@NonNull Context context) {
        super(context);
        View flatRendererView = new FlatRendererView(context, this);
        setRenderer(flatRendererView);
        this.context = context;

        checkChromecastModulePresence();

        if (isChromecastModulePresent) {
            oneCastManager = new OneCastManager(context);
        }
    }

    private void checkChromecastModulePresence() {
        Context context = getContext();
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;
            isChromecastModulePresent = metaData != null && metaData.getString("com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME") != null;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
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

    @Nullable
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
        if (!isChromecastModulePresent)
            return;

        getHandler().post(new Runnable() {
            @Override
            public void run() {
                oneCastManager.stopCasting();
            }
        });
    }
}
