/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import com.aol.mobile.sdk.annotations.PublicApi;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;

@PublicApi
public final class ExoFlatRenderer extends ExoVideoRenderer {

    public ExoFlatRenderer(@NonNull Context context) {
        super(context);
        View flatRendererView = new FlatRendererView(context, this);
        setRenderer(flatRendererView);
    }
}
