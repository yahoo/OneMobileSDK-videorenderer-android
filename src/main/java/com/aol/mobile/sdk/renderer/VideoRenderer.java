/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;

public interface VideoRenderer {
    /**
     * Sets renderer listener
     *
     * @param listener {@link Listener} instance
     */
    void setListener(@Nullable Listener listener);

    /**
     * Disposes all resources
     */
    void dispose();

    void render(VideoVM videoVM);

    /**
     * Gets renderer viewport as android {@link View} instance
     *
     * @return android {@link View} port
     */
    @NonNull
    View getViewport();


    interface Listener {
        /**
         * Fired when new subtitle need to be shown
         *
         * @param subtitle text of subtitle to show
         */
        void onSubtitleUpdated(@Nullable CharSequence subtitle);

        /**
         * Fired when duration of video is set.
         *
         * @param duration time in milliseconds, may be 0 if duration is not defined (live stream)
         */
        void onDurationReceived(long duration);

        /**
         * Fired when new position of playback reached
         *
         * @param position time in milliseconds
         */
        void onVideoPositionUpdated(long position);

        /**
         * Fired when playback buffer amount changed
         *
         * @param bufferedPercentage value from 0 to 100
         */
        void onVideoBufferUpdated(int bufferedPercentage);

        /**
         * Fired when viewport size changed
         *
         * @param viewWidth  viewport width in pixels
         * @param viewHeight viewport height in pixels
         */
        void onViewportResized(int viewWidth, int viewHeight);

        /**
         * Fired when actual playback flag is changed
         *
         * @param isActuallyPlaying <b>true</b> if video is actually playing, <b>false</b> otherwise
         */
        void onVideoPlaybackFlagUpdated(boolean isActuallyPlaying);

        /**
         * Fired when video reached its end
         */
        void onVideoEnded();

        /**
         * Fired when error encountered during video playback
         *
         * @param error one of {@link Error}
         */
        void onErrorOccurred(@NonNull Error error);


        void onCameraDirectionChanged(double lng, double lat);

        /**
         * Fired when seek if performed.
         */
        void onSeekPerformed();

        /**
         * Fired when hls bitrate is updated.
         */
        void onHlsBitrateUpdated(long bitrate);

        enum Error {
            /**
             * Bad internet or slow, timed out connection error.
             */
            CONNECTION,
            /**
             * Broken video error.
             */
            CONTENT
        }
    }

    interface Producer {
        @NonNull
        VideoRenderer createRenderer(@NonNull Context context);
    }
}
