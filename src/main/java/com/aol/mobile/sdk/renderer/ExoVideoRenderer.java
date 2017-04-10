/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.aol.mobile.sdk.renderer.gles.VideoSurfaceListener;
import com.aol.mobile.sdk.renderer.internal.ExoHelper;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;
import com.aol.mobile.sdk.renderer.internal.OneExoPlayer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.aol.mobile.sdk.renderer.VideoRenderer.Listener.Error.CONNECTION;
import static com.aol.mobile.sdk.renderer.VideoRenderer.Listener.Error.CONTENT;

class ExoVideoRenderer extends FrameLayout implements VideoRenderer, VideoSurfaceListener,
        ExoPlayer.EventListener, OneExoPlayer.VideoListener {

    @NonNull
    private final Handler handler = new Handler();
    @Nullable
    private OneExoPlayer exoPlayer;
    @NonNull
    private ProgressTimer progressTimer = new ProgressTimer();
    private boolean isMuted = false;

    @Nullable
    private Listener listener;
    @Nullable
    private View streamRenderer;
    @Nullable
    private Surface surface;
    @NonNull
    private Context context;
    @Nullable
    private Long duration;
    private boolean shouldPlay;
    private int playbackState;

    private int videoWidth = Integer.MIN_VALUE;
    private int videoHeight = Integer.MIN_VALUE;
    private int viewportWidth = Integer.MIN_VALUE;
    private int viewportHeight = Integer.MIN_VALUE;

    private boolean scalable = true;
    private boolean maintainAspectRatio = true;
    @Nullable
    private String videoUrl;
    @Nullable
    private String subtitleUrl;
    private boolean isLive;

    public ExoVideoRenderer(@NonNull Context context) {
        super(context);
        this.context = context;

    }

    protected void setRenderer(@NonNull View renderer) {
        this.streamRenderer = renderer;

        removeAllViews();
        addView(streamRenderer, MATCH_PARENT, MATCH_PARENT);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        if (listener != null && getWidth() != 0 && getHeight() != 0) {
            listener.onViewportResized(getWidth(), getHeight());
        }
    }

    private void playVideo(@Nullable String videoUrl, @Nullable String subtitleUrl) {
        duration = null;
        if (this.videoUrl != null) {
            if (exoPlayer != null) {
                exoPlayer.setVideoSurface(null);
                if (streamRenderer instanceof FlatRendererView) {
                    Matrix matrix = new Matrix();
                    matrix.setScale(0, 0, 0, 0);
                    ((FlatRendererView) streamRenderer).setTransform(matrix);
                }
                exoPlayer.stop();
                exoPlayer.removeListener(this);
                exoPlayer.release();
            }
        }
        this.videoUrl = videoUrl;
        this.subtitleUrl = subtitleUrl;
        if (videoUrl == null) {
            progressTimer.stop();
            return;
        }

        MediaSource source = ExoHelper.buildMediaSource(videoUrl, subtitleUrl, handler, context);
        updateExoPlayerSource(source);
    }

    private void updateExoPlayerSource(@NonNull MediaSource source) {
        exoPlayer = ExoHelper.getExoPlayer(context, handler);
        exoPlayer.prepare(source);
        exoPlayer.setTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(List<Cue> cues) {
                if (listener == null) return;
                if (cues.size() > 0) {
                    listener.onSubtitleUpdated(cues.get(0).text);
                } else {
                    listener.onSubtitleUpdated(null);
                }
            }
        });

        exoPlayer.addListener(this);
        exoPlayer.setVideoListener(this);
        exoPlayer.setVolume(isMuted ? 0f : 1f);
        exoPlayer.setVideoSurface(surface);
        exoPlayer.setPlayWhenReady(shouldPlay);
    }

    private void scaleViewport() {
        if (viewportHeight <= 0 || viewportWidth <= 0 ||
                videoWidth <= 0 || videoHeight <= 0) return;

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        float viewportRatio = viewportWidth / (float) viewportHeight;
        float videoRatio = videoWidth / (float) videoHeight;

        if (scalable || videoWidth > viewportWidth || videoHeight > viewportHeight) {
            if (videoRatio > viewportRatio) {
                scaleY = viewportRatio / videoRatio;
            } else {
                if (maintainAspectRatio) {
                    scaleX = videoRatio / viewportRatio;
                }
            }
        } else {
            scaleX = (float) videoWidth / viewportWidth;
            scaleY = (float) videoHeight / viewportHeight;
        }

        if (streamRenderer instanceof FlatRendererView) {
            Matrix matrix = new Matrix();
            matrix.setScale(scaleX, scaleY, viewportWidth / 2, viewportHeight / 2);

            ((FlatRendererView) streamRenderer).setTransform(matrix);
        }
        if (listener != null) {
            listener.onViewportResized(viewportWidth, viewportHeight);
        }
    }

    @Override
    public void presentUrl(@Nullable String videoUrl, @Nullable String subtitleUrl) {
        playVideo(videoUrl, subtitleUrl);
    }

    @Override
    public void pausePlayback() {
        if (shouldPlay) {
            shouldPlay = false;

            if (exoPlayer != null && exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                exoPlayer.setPlayWhenReady(false);
                progressTimer.stop();
                if (listener != null) {
                    listener.onVideoPlaybackFlagUpdated(exoPlayer.getPlayWhenReady());
                }
            }
        }
    }

    @Override
    public void resumePlayback() {
        if (!shouldPlay) {
            shouldPlay = true;

            if (exoPlayer != null && exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                exoPlayer.setPlayWhenReady(true);
                progressTimer.start();
                if (listener != null) {
                    listener.onVideoPlaybackFlagUpdated(exoPlayer.getPlayWhenReady());
                }
            }
        }
    }

    @Override
    public void mute() {
        isMuted = true;
        if (exoPlayer != null) {
            exoPlayer.setVolume(0f);
        }
    }

    @Override
    public void unmute() {
        isMuted = false;
        if (exoPlayer != null) {
            exoPlayer.setVolume(1f);
        }
    }

    @Override
    public void seekTo(long position) {
        if (exoPlayer != null && Math.abs(exoPlayer.getCurrentPosition() - position) > 100) {
            exoPlayer.seekTo(position);
        }

        if (listener != null) {
            listener.onSeekPerformed();
        }
    }

    @Override
    public void setCameraOrientation(double longitude, double latitude) {
        if (streamRenderer instanceof GlEsRendererView) {
            ((GlEsRendererView) streamRenderer).setCameraOrientation(longitude, latitude);
        }
    }

    private void updateDuration() {
        if (exoPlayer == null || exoPlayer.getDuration() == C.TIME_UNSET && exoPlayer.getCurrentPosition() == 0) {
            return;
        }
        long duration = exoPlayer.getDuration();
        boolean isLive = exoPlayer.getCurrentTimeline()
                .getWindow(exoPlayer.getCurrentWindowIndex(), new Timeline.Window()).isDynamic;
        this.duration = duration == C.TIME_UNSET || isLive ? 0 : duration;
        if (listener != null) {
            listener.onDurationReceived(this.duration);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (this.playbackState != playbackState) {
            this.playbackState = playbackState;
            assert exoPlayer != null;

            switch (playbackState) {
                case ExoPlayer.STATE_READY:
                    progressTimer.start();
                    exoPlayer.setPlayWhenReady(shouldPlay);
                    if (listener != null) {
                        listener.onVideoPlaybackFlagUpdated(shouldPlay);
                    }
                    break;

                case ExoPlayer.STATE_IDLE:
                case ExoPlayer.STATE_BUFFERING:
                    progressTimer.stop();
                    exoPlayer.setPlayWhenReady(false);
                    if (listener != null) {
                        listener.onVideoPlaybackFlagUpdated(false);
                    }
                    break;

                case ExoPlayer.STATE_ENDED:
                    if (!isLive) {
                        progressTimer.stop();
                        if (listener != null) {
                            if (this.duration == null) {
                                listener.onErrorOccurred(CONTENT);
                                return;
                            }
                            listener.onVideoPositionUpdated(exoPlayer.getDuration());
                        }
                        exoPlayer.setPlayWhenReady(false);
                        if (listener != null) {
                            listener.onVideoPlaybackFlagUpdated(false);
                            listener.onVideoEnded();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        if (timeline != null && !timeline.isEmpty()) {
            isLive = timeline.getWindow(0, new Timeline.Window()).isDynamic;
        } else {
            isLive = false;
        }
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error.getCause() != null && error.getCause().getClass() == BehindLiveWindowException.class) {
            playVideo(videoUrl, subtitleUrl);
            return;
        }
        String message = error.getCause() == null ? null : error.getCause().getMessage();
        boolean isConnectionError = hasConnectionError(error.getMessage()) || hasConnectionError(message);
        if (isConnectionError && subtitleUrl != null) {
            subtitleUrl = null;
            playVideo(videoUrl, subtitleUrl);
            return;
        }
        if (listener != null) {
            if (isConnectionError) {
                listener.onErrorOccurred(CONNECTION);
            } else {
                listener.onErrorOccurred(CONTENT);
            }
        }
    }

    private boolean hasConnectionError(@Nullable String message) {
        if (message == null) return false;
        return message.contains("Unable to connect to") || message.contains("Response code");
    }

    @Override
    public void onPositionDiscontinuity() {
    }

    @Override
    public void dispose() {
        listener = null;

        if (streamRenderer != null) {
            if (exoPlayer != null) {
                exoPlayer.setVideoSurface(null);
            }

            if (streamRenderer instanceof GlEsRendererView) {
                ((GlEsRendererView) streamRenderer).dispose();
            }

            streamRenderer = null;
        }
        progressTimer.stop();
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.removeListener(this);
            exoPlayer.release();
        }
    }

    @NonNull
    @Override
    public View getViewport() {
        return this;
    }

    @Override
    public void setScalable(boolean scalable) {
        this.scalable = scalable;
    }

    @Override
    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }

    @Override
    public void onVideoSurfaceAvailable(@NonNull Surface surface) {
        this.surface = surface;

        if (exoPlayer != null) {
            exoPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void onVideoSurfaceResized(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;

        scaleViewport();
    }

    @Override
    public void onVideoSurfaceReleased(@NonNull Surface surface) {
        if (this.surface == surface) {
            this.surface = null;

            if (exoPlayer != null) {
                exoPlayer.setVideoSurface(null);
            }
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        scaleViewport();
    }

    class ProgressTimer {
        @NonNull
        private Handler handler = new Handler();
        @NonNull
        private Runnable action = new Runnable() {
            @Override
            public void run() {
                if (listener != null && exoPlayer != null) {
                    long position = exoPlayer.getCurrentPosition();
                    if (position < 0) {
                        position = 0;
                    }
                    int bufferedPercentage = exoPlayer.getBufferedPercentage();
                    bufferedPercentage = bufferedPercentage > 100 ? 100 : bufferedPercentage;
                    bufferedPercentage = bufferedPercentage < 0 ? 0 : bufferedPercentage;
                    listener.onVideoBufferUpdated(bufferedPercentage);
                    if (duration == null) {
                        updateDuration();
                    }
                    if (duration != null) {
                        if (exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY &&
                                shouldPlay && position <= duration) {
                            listener.onVideoPositionUpdated(position);
                        }
                        if (position > duration && playbackState != ExoPlayer.STATE_ENDED
                                && exoPlayer.getPlayWhenReady()) {
                            onPlayerStateChanged(shouldPlay, ExoPlayer.STATE_ENDED);
                        }
                    }
                }
                handler.postDelayed(action, 200);
            }
        };

        public void start() {
            handler.removeCallbacks(action);
            action.run();
        }

        public void stop() {
            handler.removeCallbacks(action);
        }
    }
}
