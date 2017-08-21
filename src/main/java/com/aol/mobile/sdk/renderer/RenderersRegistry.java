package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;

public final class RenderersRegistry {
    public static final String FLAT_RENDERER = "com.onemobilesdk.videorenderer.flat@" + BuildConfig.VERSION_NAME;
    public static final String THREE_SIXTY_RENDERER = "com.onemobilesdk.videorenderer.360@" + BuildConfig.VERSION_NAME;
    public static final String FISH_EYE_RENDERER = "com.onemobilesdk.videorenderer.fisheye@" + BuildConfig.VERSION_NAME;

    @NonNull
    static final HashMap<String, VideoRenderer.Producer> registry = new HashMap<>();

    static {
        registerRenderer(FLAT_RENDERER, new VideoRenderer.Producer() {
            @NonNull
            @Override
            public VideoRenderer createRenderer(@NonNull Context context) {
                return new ExoFlatRenderer(context);
            }
        });

        registerRenderer(THREE_SIXTY_RENDERER, new VideoRenderer.Producer() {
            @NonNull
            @Override
            public VideoRenderer createRenderer(@NonNull Context context) {
                return new ExoSphereRenderer(context);
            }
        });

        registerRenderer(FISH_EYE_RENDERER, new VideoRenderer.Producer() {
            @NonNull
            @Override
            public VideoRenderer createRenderer(@NonNull Context context) {
                return new ExoFishEyeRenderer(context);
            }
        });
    }

    public static void registerRenderer(@NonNull String rendererId, @NonNull VideoRenderer.Producer producer) {
        if (rendererId.length() == 0)
            throw new IllegalArgumentException("Renderer id must be in format <name@version>");

        if (rendererId.split("@").length != 2)
            throw new IllegalArgumentException("Renderer id must be in format <name@version>");

        registry.put(rendererId, producer);
    }

    @NonNull
    public static Collection<String> listRenderers() {
        return registry.keySet();
    }

    @NonNull
    public static VideoRenderer getRenderer(@NonNull String id, @NonNull Context context) {
        VideoRenderer.Producer producer = registry.get(id);
        if (producer == null) throw new RuntimeException("No renderer record found for id:" + id);
        return producer.createRenderer(context);
    }

    public static boolean hasRenderer(@NonNull String renderer) {
        return registry.containsKey(renderer);
    }
}
