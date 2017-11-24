package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.aol.mobile.sdk.chromecast.CastRenderer;
import com.aol.mobile.sdk.chromecast.CastRendererImpl;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.viewmodel.CastVideoVMTranslator;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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
        }
    }

    @Nullable
    private CastRenderer getCastRenderer(@NonNull Context context) {
        return new CastRendererImpl(context);
    }
}
