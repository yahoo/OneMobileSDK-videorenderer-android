/*
 * Copyright 2019, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer.chromecast;

import android.content.Context;
import android.util.Log;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.renderer.R;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;

import java.util.List;

import static java.util.Collections.singletonList;

@PublicApi
public class CastOptionsProvider implements OptionsProvider {
    @Override
    public CastOptions getCastOptions(Context context) {
        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(singletonList(MediaIntentReceiver.ACTION_STOP_CASTING), new int[]{0})
                .build();

        CastMediaOptions mediaOptions = new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .build();

        String appId = context.getString(R.string.app_id);

        Log.d("CASTTT", "App id: " + appId);
        return new CastOptions.Builder()
                .setReceiverApplicationId(appId)
                .setCastMediaOptions(mediaOptions)
                .setEnableReconnectionService(true)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}