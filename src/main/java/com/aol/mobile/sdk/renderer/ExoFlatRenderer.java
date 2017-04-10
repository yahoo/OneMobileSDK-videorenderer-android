package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.internal.FlatRendererView;

public final class ExoFlatRenderer extends ExoVideoRenderer {
    public ExoFlatRenderer(@NonNull Context context) {
        super(context);
        setRenderer(new FlatRendererView(getContext(), this));
    }
}
