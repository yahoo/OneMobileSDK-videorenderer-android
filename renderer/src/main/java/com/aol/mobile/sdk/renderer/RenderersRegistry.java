package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static android.content.pm.PackageManager.GET_META_DATA;

public final class RenderersRegistry {
    private static final String RENDERER_PREFIX = "com.onemobilesdk.videorenderer";

    public static final String FLAT_RENDERER = BuildConfig.FLAT_RENDERER;

    @NonNull
    private final HashMap<String, VideoRenderer.Producer> registry = new HashMap<>();

    @NonNull
    private VideoRenderer.Producer getProducer(@NonNull String className) {
        Class<?> producerClass;

        try {
            producerClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class from metadata not found", e);
        }

        Object producer;

        try {
            producer = producerClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (producer instanceof VideoRenderer.Producer) {
            return ((VideoRenderer.Producer) producer);
        }

        throw new RuntimeException(className + " is not instance of VideoRenderer.Producer");
    }

    private void registerRenderer(@NonNull String rendererId, @NonNull VideoRenderer.Producer producer) {
        if (rendererId.length() == 0)
            throw new IllegalArgumentException("Renderer id must be in format <name@version>");

        if (rendererId.split("@").length != 2)
            throw new IllegalArgumentException("Renderer id must be in format <name@version>");

        registry.put(rendererId, producer);
    }

    private static Bundle getMetaBundle(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        ApplicationInfo info;

        try {
            info = packageManager.getApplicationInfo(packageName, GET_META_DATA);
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (info.metaData == null)
            throw new RuntimeException("No renderers metadata found in AndroidManifest");

        return info.metaData;
    }

    public RenderersRegistry(@NonNull Context context) {
        Bundle metaData = getMetaBundle(context);

        for (String key : metaData.keySet()) {
            String producerClass = metaData.getString(key);

            if (key.startsWith(RENDERER_PREFIX) && producerClass != null) {
                registerRenderer(key, getProducer(producerClass));
            }
        }
    }

    @NonNull
    public Collection<String> listRenderersId() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    @NonNull
    public VideoRenderer getRenderer(@NonNull String id, @NonNull Context context) {
        VideoRenderer.Producer producer = registry.get(id);
        if (producer == null) throw new RuntimeException("No renderer record found for id:" + id);
        return producer.createRenderer(context);
    }

    public boolean hasRenderer(@NonNull String renderer) {
        return registry.containsKey(renderer);
    }
}
