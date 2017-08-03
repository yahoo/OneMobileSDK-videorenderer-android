/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.aol.mobile.sdk.renderer.gles.VideoSurfaceListener;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.aol.mobile.sdk.renderer.VideoRenderer.Listener.Error.CONNECTION;
import static com.aol.mobile.sdk.renderer.VideoRenderer.Listener.Error.CONTENT;
import static com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP;
import static com.google.android.exoplayer2.util.Util.getUserAgent;
import static com.google.android.exoplayer2.util.Util.inferContentType;

class ExoVideoRenderer extends FrameLayout implements VideoRenderer, VideoSurfaceListener,
        ExoPlayer.EventListener, SimpleExoPlayer.VideoListener {

    @NonNull
    private final Handler handler = new Handler();
    @NonNull
    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    @NonNull
    private final TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
    @NonNull
    private final DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();
    @NonNull
    private final DataSource.Factory dataSourceFactory;
    @NonNull
    private final DefaultTrackSelector trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
    @Nullable
    protected Listener listener;
    @Nullable
    private SimpleExoPlayer exoPlayer;
    @NonNull
    private ProgressTimer progressTimer = new ProgressTimer();
    private boolean isMuted = false;
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
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                getUserAgent(context, "VideoSDK"), bandwidthMeter, 8000, 8000, true);

        this.dataSourceFactory = new DefaultDataSourceFactory(context, bandwidthMeter, httpDataSourceFactory);
        this.context = context;
    }

    @NonNull
    public MediaSource buildMediaSource(@NonNull String videoUriString, @Nullable String subtitleUriString) {
        Uri videoUri = Uri.parse(videoUriString);
        MediaSource source;

        switch (inferContentType(videoUri.getLastPathSegment())) {
            case C.TYPE_HLS:
                source = new HlsMediaSource(videoUri, dataSourceFactory, handler, null);
                break;

            default:
                source = new ExtractorMediaSource(videoUri, dataSourceFactory, defaultExtractorsFactory, handler, null);

                Uri subtitleUri = subtitleUriString == null ? null : Uri.parse(subtitleUriString);
                if (subtitleUri != null) {
                    Format textFormat = Format.createTextSampleFormat(null, APPLICATION_SUBRIP, null,
                            Format.NO_VALUE, C.SELECTION_FLAG_DEFAULT, "en", null);
                    MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, dataSourceFactory,
                            textFormat, C.TIME_UNSET);
                    source = new MergingMediaSource(source, subtitleSource);
                }
                break;
        }

        return source;
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
            progressTimer.stop();
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
                exoPlayer = null;
            }
        }
        this.videoUrl = videoUrl;
        this.subtitleUrl = subtitleUrl;
        if (videoUrl == null) return;

        updateExoPlayerSource(buildMediaSource(videoUrl, subtitleUrl));
    }

    private void updateExoPlayerSource(@NonNull final MediaSource source) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        exoPlayer.prepare(source);
        exoPlayer.setTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(@Nullable List<Cue> cues) {
                if (listener == null) return;
                if (cues != null && cues.size() > 0) {
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

    public void render(VideoVM videoVM) {
        this.scalable = videoVM.isScalable;
        this.maintainAspectRatio = videoVM.isMaintainAspectRatio;
        if (videoVM.videoUrl != null && !videoVM.videoUrl.equals(videoUrl)) {
            playVideo(videoVM.videoUrl, videoVM.subtitleUrl);
        } else if (videoVM.videoUrl == null && videoUrl != null) {
            playVideo(null, null);
        }

        if (videoVM.shouldPlay) {
            resumePlayback();
        } else {
            pausePlayback();
        }

        if (streamRenderer instanceof GlEsRendererView) {
            ((GlEsRendererView) streamRenderer).setCameraOrientation(videoVM.longitude, videoVM.latitude);
        }

        Long seekPos = videoVM.seekPosition;
        if (seekPos != null) {
            seekTo(seekPos);
        }

        if (videoVM.isMuted != isMuted) {
            setMute(videoVM.isMuted);
        }
    }

    private void pausePlayback() {
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

    private void resumePlayback() {
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

    private void setMute(boolean isMuted) {
        this.isMuted = isMuted;
        if (exoPlayer != null) {
            exoPlayer.setVolume(isMuted ? 0f : 1f);
        }
    }

    private void seekTo(long position) {
        if (exoPlayer != null && Math.abs(exoPlayer.getCurrentPosition() - position) > 100) {
            exoPlayer.seekTo(position);
        }

        if (listener != null) {
            listener.onSeekPerformed();
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
                        exoPlayer.setPlayWhenReady(false);
                        progressTimer.stop();
                        if (listener != null) {
                            if (this.duration == null) {
                                listener.onErrorOccurred(CONTENT);
                                return;
                            }
                            listener.onVideoPositionUpdated(exoPlayer.getDuration());
                        }
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
        isLive = timeline != null && !timeline.isEmpty() &&
                timeline.getWindow(0, new Timeline.Window()).isDynamic;
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
            playVideo(videoUrl, null);
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
        return message != null &&
                (message.contains("Unable to connect to") || message.contains("Response code"));
    }

    @Override
    public void onPositionDiscontinuity() {
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
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
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        videoWidth = width;
        videoHeight = height;
        scaleViewport();
    }

    @Override
    public void onRenderedFirstFrame() {

    }

    private class ProgressTimer {
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
