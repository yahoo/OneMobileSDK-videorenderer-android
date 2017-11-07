package com.aol.mobile.sdk.renderer;

import android.support.annotation.NonNull;
import android.view.View;

import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

public interface CastRenderer {

    @NonNull
    VideoVM render(@NonNull VideoVM videoVM);

    @NonNull
    View getViewport();
}
