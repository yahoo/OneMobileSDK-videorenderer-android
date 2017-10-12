package com.aol.mobile.sdk.renderer.producer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.ExoSphereRenderer;
import com.aol.mobile.sdk.renderer.VideoRenderer;

@SuppressWarnings("unused")
public final class ThreeSixtyRendererProducer implements VideoRenderer.Producer {
    @NonNull
    @Override
    public VideoRenderer createRenderer(@NonNull Context context) {
        return new ExoSphereRenderer(context);
    }
}
