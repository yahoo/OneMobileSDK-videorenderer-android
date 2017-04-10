package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.gles.Sphere;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;

public final class ExoSphereRenderer extends ExoVideoRenderer {
    public ExoSphereRenderer(@NonNull Context context) {
        super(context);
        setRenderer(new GlEsRendererView(getContext(), new Sphere(90, 500f), this));
    }
}
