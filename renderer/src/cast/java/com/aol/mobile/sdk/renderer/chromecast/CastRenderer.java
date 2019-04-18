/*
 * Copyright 2019, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer.chromecast;

import android.support.annotation.NonNull;
import android.view.View;

public interface CastRenderer {
    void render(@NonNull CastVideoVM videoVM);

    @NonNull
    View getViewport();
}
