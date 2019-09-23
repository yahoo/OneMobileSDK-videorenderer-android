/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;


import android.support.annotation.NonNull;

import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

public class ErrorInfo {

    private VideoVM.Callbacks.Error errorType;
    private Exception exception;
    private String errorMessage;

    public ErrorInfo(@NonNull VideoVM.Callbacks.Error errorType, @NonNull Exception exception, @NonNull String errorMessage) {
        this.errorType = errorType;
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    public VideoVM.Callbacks.Error getErrorType() {
        return errorType;
    }

    public Exception getException() {
        return exception;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}