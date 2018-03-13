/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.renderer.gles.FishEyeSphere;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;

@PublicApi
public final class ExoFishEyeRenderer extends ThreeSixtyRenderer {
    public ExoFishEyeRenderer(@NonNull Context context) {
        super(context);

        setRenderer(new GlEsRendererView(context, new FishEyeSphere(90, 500f), this));
    }
}
