package com.aol.mobile.sdk.renderer.producer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.ExoFishEyeRenderer;
import com.aol.mobile.sdk.renderer.VideoRenderer;

@SuppressWarnings("unused")
public class FishEyeRendererProducer implements VideoRenderer.Producer {
    @NonNull
    @Override
    public VideoRenderer createRenderer(@NonNull Context context) {
        return new ExoFishEyeRenderer(context);
    }
}
