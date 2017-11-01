package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

import java.lang.reflect.Constructor;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public final class ExoFlatRenderer extends ExoVideoRenderer {
    private CastRenderer castRenderer;
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

        if (castRenderer != null) {
            super.render(castRenderer.render(videoVM));
        } else {
            super.render(videoVM);
        }
    }

    private void showCastView(@NonNull VideoVM videoVM) {
        if (castRenderer == null) {
            castRenderer = getCastRenderer(getContext());
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

    private CastRenderer getCastRenderer(@NonNull Context context) {
        try {
            Class castRendererClass = Class.forName("com.aol.mobile.sdk.chromecast.CastRendererImpl");
            Constructor<?> constructor = castRendererClass.getConstructor(Context.class);
            return (CastRenderer) constructor.newInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
