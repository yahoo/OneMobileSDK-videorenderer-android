package com.aol.mobile.sdk.renderer.producer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.ExoFlatRenderer;
import com.aol.mobile.sdk.renderer.VideoRenderer;


@SuppressWarnings("unused")
public final class FlatRendererProducer implements VideoRenderer.Producer {
    @NonNull
    @Override
    public VideoRenderer createRenderer(@NonNull Context context) {
        return new ExoFlatRenderer(context);
    }
}
