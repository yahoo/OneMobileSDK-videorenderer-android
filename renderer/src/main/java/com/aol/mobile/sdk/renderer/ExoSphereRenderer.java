/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;


import android.content.Context;
import android.support.annotation.NonNull;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.renderer.gles.Sphere;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;

@PublicApi
public final class ExoSphereRenderer extends ThreeSixtyRenderer {
    public ExoSphereRenderer(@NonNull Context context) {
        super(context);
        setRenderer(new GlEsRendererView(getContext(), new Sphere(90, 500f), this));
    }
}
