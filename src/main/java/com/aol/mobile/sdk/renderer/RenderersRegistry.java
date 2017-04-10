package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class RenderersRegistry {
    private static final HashMap<String, Constructor<? extends VideoRenderer>> renderCreators = new HashMap<>();

    public enum ContentType {
        FLAT("stock_flat"),
        STOCK_360("stock_360"),
        STOCK_360_FISH_EYE("stock_360_fisheye");

        @NonNull
        private final String value;

        ContentType(@NonNull String value) {
            this.value = value;
        }
    }

    static {
        register(ExoFlatRenderer.class, ContentType.FLAT.value);
        register(ExoSphereRenderer.class, ContentType.STOCK_360.value);
        register(ExoFishEyeRenderer.class, ContentType.STOCK_360_FISH_EYE.value);
    }

    public static void register(@NonNull Class<? extends VideoRenderer> rendererClass, @NonNull String renderId) {
        try {
            renderCreators.put(renderId, rendererClass.getConstructor(Context.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static VideoRenderer getRendererFor(@NonNull Context context, @NonNull ContentType contentType) {
        Constructor<? extends VideoRenderer> constructor = renderCreators.get(contentType.value);
        if (constructor == null) throw new RuntimeException();

        try {
            return constructor.newInstance(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
