/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public interface VideoRenderer {
    /**
     * Sets renderer listener
     *
     * @param listener {@link Listener} instance
     */
    void setListener(@Nullable Listener listener);

    /**
     * Initiates playback of video with subtitles
     *
     * @param videoUrl    video source
     * @param subtitleUrl subtitles source, may be absent
     */
    void presentUrl(@Nullable String videoUrl, @Nullable String subtitleUrl);

    /**
     * Pauses video playback
     */
    void pausePlayback();

    /**
     * Resumes video playback
     */
    void resumePlayback();

    /**
     * Mutes audio volume
     */
    void mute();

    /**
     * Unmutes audio volume
     */
    void unmute();

    /**
     * Performs seek to video position. When seek is performed renderer should spawn
     * {@link Listener#onSeekPerformed()} event.
     *
     * @param position time in milliseconds.
     */
    void seekTo(long position);

    /**
     * Sets video observer's orientation in space. Default position <b>(0f, 0f)</b>
     *
     * @param longitude horizontal angle in radians from -180˚ to 180˚
     * @param latitude  vertical angle in radians from -85˚ to 85˚
     */
    void setCameraOrientation(double longitude, double latitude);

    /**
     * Sets video scalable flag. Default value is <b>true</b>
     *
     * @param scalable <b>true</b> if video should fill viewport, <b>false</b> otherwise
     */
    void setScalable(boolean scalable);

    /**
     * Sets flag for maintenance of video aspect ration. Works in conjunction with
     *
     * @param maintainAspectRatio <b>true</b> if renderer must respect video aspect ratio,
     *                            <b>false</b> otherwise
     */
    void setMaintainAspectRatio(boolean maintainAspectRatio);

    /**
     * Disposes all resources
     */
    void dispose();

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

        /**
         * Fired when seek if performed.
         *
         * @see VideoRenderer#seekTo(long)
         */
        void onSeekPerformed();

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
