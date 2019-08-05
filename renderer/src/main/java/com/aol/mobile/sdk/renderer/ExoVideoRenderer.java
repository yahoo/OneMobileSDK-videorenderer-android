/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
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
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.DefaultMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONNECTION;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONTENT;
import static com.google.android.exoplayer2.Format.createTextSampleFormat;
import static com.google.android.exoplayer2.RendererCapabilities.FORMAT_HANDLED;
import static com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP;
import static com.google.android.exoplayer2.util.Util.areEqual;
import static com.google.android.exoplayer2.util.Util.getUserAgent;
import static com.google.android.exoplayer2.util.Util.inferContentType;

class ExoVideoRenderer extends FrameLayout implements VideoRenderer, VideoSurfaceListener,
        Player.EventListener, VideoListener {
    @NonNull
    private final SubtitleView subtitleView;
    @Nullable
    protected VideoVM.Callbacks callbacks;
    @Nullable
    private TrackSelection.Factory selectionFactory;
    @Nullable
    private DefaultTrackSelector trackSelector;
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
    private AudioTrack audioTrack;
    @Nullable
    private TextTrack textTrack;
    @Nullable
    private Long duration;
    private boolean shouldPlay;
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
    @Nullable
    private String subtitleLang;
    private boolean endReported;

    public ExoVideoRenderer(@NonNull Context context) {
        super(context);

        subtitleView = new SubtitleView(context);


        this.context = context;
    }

    @NonNull
    private MediaSource buildMediaSource(@NonNull String videoUriString, @Nullable String subtitleLang, @Nullable String subtitleUriString) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                getUserAgent(context, "VideoSDK"), bandwidthMeter,
                8000, 8000, true);

        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(context, bandwidthMeter, httpDataSourceFactory);


        Uri videoUri = Uri.parse(videoUriString);
        MediaSource source;
        final Handler handler = new Handler();
        switch (inferContentType(videoUri.getLastPathSegment())) {
            case C.TYPE_HLS:
                HlsMediaSource.Factory hlsMediaFactory = new HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true);
                source = hlsMediaFactory.createMediaSource(videoUri, handler, new DefaultMediaSourceEventListener() {

                    @Override
                    public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
                        if (callbacks != null && mediaLoadData != null && mediaLoadData.trackFormat != null) {
                            callbacks.onHlsBitrateUpdated(mediaLoadData.trackFormat.bitrate);
                        }
                    }

                    @Override
                    public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
                        if (shouldPlay && exoPlayer != null && !exoPlayer.getPlayWhenReady()) {
                            playVideo(videoUrl, ExoVideoRenderer.this.subtitleLang, subtitleUrl);
                        }
                    }
                });
                break;

            default:
                ExtractorMediaSource.Factory mediaFactory = new ExtractorMediaSource.Factory(dataSourceFactory);
                source = mediaFactory.createMediaSource(videoUri, handler, null);
                Uri subtitleUri = subtitleUriString == null ? null : Uri.parse(subtitleUriString);
                if (subtitleUri != null) {
                    SingleSampleMediaSource.Factory srtMediaFactory = new SingleSampleMediaSource.Factory(dataSourceFactory);
                    Format textFormat = createTextSampleFormat(subtitleLang, APPLICATION_SUBRIP, null, C.POSITION_UNSET, C.POSITION_UNSET, subtitleLang, null, 9223372036854775807L);
                    MediaSource subtitleSource = srtMediaFactory.createMediaSource(subtitleUri, textFormat, C.TIME_UNSET);
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
        addView(subtitleView, MATCH_PARENT, MATCH_PARENT);
    }

    private void playVideo(@Nullable String videoUrl, @Nullable String subLang, @Nullable String subtitleUrl) {
        duration = null;
        if (exoPlayer != null) {
            if (streamRenderer instanceof FlatRendererView) {
                Matrix matrix = new Matrix();
                matrix.setScale(0, 0, 0, 0);
                ((FlatRendererView) streamRenderer).setTransform(matrix);
            }
            subtitleView.onCues(Collections.emptyList());
            exoPlayer.removeTextOutput(subtitleView);
            exoPlayer.removeListener(this);
            exoPlayer.removeVideoListener(this);
            exoPlayer.setVideoSurface(null);
            exoPlayer.release();
            trackSelector = null;
            selectionFactory = null;
            exoPlayer = null;
            progressTimer.stop();
        }

        this.videoUrl = videoUrl;
        this.subtitleUrl = subtitleUrl;
        this.subtitleLang = subLang;
        if (videoUrl == null) {
            if (callbacks != null) {
                callbacks.onVideoFrameGone();
            }
            return;
        }

        updateExoPlayerSource(buildMediaSource(videoUrl, subtitleLang, subtitleUrl));
    }

    private void updateExoPlayerSource(@NonNull final MediaSource source) {
        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter()));
        selectionFactory = new FixedTrackSelection.Factory();
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        exoPlayer.prepare(source);

        for (int rendererIndex = 0; rendererIndex < exoPlayer.getRendererCount(); rendererIndex++) {
            if (exoPlayer.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                trackSelector.setRendererDisabled(rendererIndex, true);
                break;
            }
        }
        exoPlayer.addTextOutput(subtitleView);
        exoPlayer.addListener(this);
        exoPlayer.addVideoListener(this);
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
        if (callbacks != null) {
            callbacks.onViewportResized(viewportWidth, viewportHeight);
        }
    }

    public void render(@NonNull VideoVM videoVM) {
        boolean areVisible = subtitleView.getVisibility() == VISIBLE;
        if (areVisible != videoVM.areSubtitlesEnabled) {
            subtitleView.setVisibility(videoVM.areSubtitlesEnabled ? VISIBLE : GONE);
        }

        if (videoVM.callbacks != null) {
            renderCallbacks(videoVM.callbacks);
        }

        this.scalable = videoVM.isScalable;
        this.maintainAspectRatio = videoVM.isMaintainAspectRatio;
        if (!areEqual(videoUrl, videoVM.videoUrl)) {
            playVideo(videoVM.videoUrl, videoVM.subtitleLang, videoVM.subtitleUrl);
        }

        Long seekPos = videoVM.seekPosition;
        if (seekPos != null) {
            seekTo(seekPos);
        }

        if (videoVM.shouldPlay) {
            resumePlayback();
        } else {
            pausePlayback();
        }

        if (streamRenderer instanceof GlEsRendererView) {
            ((GlEsRendererView) streamRenderer).setCameraOrientation(videoVM.longitude, videoVM.latitude);
        }

        if (videoVM.isMuted != isMuted) {
            setMute(videoVM.isMuted);
        }

        if (videoVM.selectedAudioTrack != audioTrack) {
            audioTrack = videoVM.selectedAudioTrack;
            switchToAudioTrack(audioTrack);
        }

        if (videoVM.selectedTextTrack != textTrack) {
            textTrack = videoVM.selectedTextTrack;
            switchToTextTrack(textTrack);
        }
    }

    private void renderCallbacks(@NonNull VideoVM.Callbacks callbacks) {
        if (this.callbacks != callbacks) {
            this.callbacks = callbacks;
            if (getWidth() != 0 && getHeight() != 0) {
                callbacks.onViewportResized(getWidth(), getHeight());
            }
        }
    }

    private void switchToAudioTrack(@Nullable AudioTrack audioTrack) {
        if (audioTrack == null || trackSelector == null) return;

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        trackSelector.clearSelectionOverrides(audioTrack.id.renderer);
        trackSelector.setParameters(trackSelector.getParameters()
                .buildUpon()
                .setPreferredAudioLanguage(null)
                .build());

        if (audioTrack.language == null || audioTrack.language.length() == 0) {
            TrackGroupArray trackGroups = info.getTrackGroups(audioTrack.id.renderer);
            DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(audioTrack.id.group, audioTrack.id.track);
            trackSelector.setSelectionOverride(audioTrack.id.renderer, trackGroups, override);
        } else {
            trackSelector.setParameters(trackSelector.getParameters()
                    .buildUpon()
                    .setPreferredAudioLanguage(audioTrack.language)
                    .build());
        }
    }

    private void switchToTextTrack(@Nullable TextTrack textTrack) {
        if (textTrack == null || trackSelector == null) return;

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (textTrack.id.group < 0 || textTrack.id.track < 0) {
            trackSelector.clearSelectionOverrides(textTrack.id.renderer);
            trackSelector.setRendererDisabled(textTrack.id.renderer, true);
        } else {
            TrackGroupArray trackGroups = info.getTrackGroups(textTrack.id.renderer);
            DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(textTrack.id.group, textTrack.id.track);
            trackSelector.setRendererDisabled(textTrack.id.renderer, false);
            trackSelector.setSelectionOverride(textTrack.id.renderer, trackGroups, override);
        }
    }

    private void pausePlayback() {
        if (shouldPlay) {
            shouldPlay = false;

            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
                progressTimer.stop();
            }
        }
    }

    private void resumePlayback() {
        if (!shouldPlay) {
            // Update style and size from CaptioningManager
            subtitleView.setUserDefaultStyle();
            subtitleView.setUserDefaultTextSize();

            shouldPlay = true;

            if (exoPlayer != null) {
                if (exoPlayer.isCurrentWindowDynamic() && exoPlayer.isCurrentWindowSeekable()) {
                    exoPlayer.seekToDefaultPosition();
                }
                exoPlayer.setPlayWhenReady(true);
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
            if (callbacks != null) {
                callbacks.onSeekPerformed();
            }
        }
    }

    private void updateDuration() {
        if (exoPlayer == null || exoPlayer.getDuration() == C.TIME_UNSET && exoPlayer.getCurrentPosition() == 0) {
            return;
        }
        long duration = exoPlayer.getDuration();
        this.duration = duration == C.TIME_UNSET || exoPlayer.isCurrentWindowDynamic() ? 0 : duration;
        if (callbacks != null) {
            callbacks.onDurationReceived(this.duration);
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (exoPlayer == null) return;

        switch (playbackState) {
            case Player.STATE_READY:
                if (callbacks != null) {
                    callbacks.onVideoPlaybackFlagUpdated(playWhenReady);
                }
                if (playWhenReady)
                    progressTimer.start();
                endReported = false;
                break;

            case Player.STATE_IDLE:
            case Player.STATE_BUFFERING:
                progressTimer.stop();
                if (callbacks != null) {
                    callbacks.onVideoPlaybackFlagUpdated(false);
                }
                endReported = false;
                break;

            case Player.STATE_ENDED:
                if (!exoPlayer.isCurrentWindowDynamic()) {
                    progressTimer.stop();
                    if (callbacks != null) {
                        if (this.duration == null) {
                            callbacks.onErrorOccurred(CONTENT);
                            return;
                        }
                        callbacks.onVideoPositionUpdated(exoPlayer.getDuration());
                    }
                }

                if (callbacks != null && !endReported) {
                    callbacks.onVideoPlaybackFlagUpdated(false);
                    callbacks.onVideoEnded();
                    endReported = true;
                }
                break;
        }
    }

    @Override
    public void onRepeatModeChanged(int i) {
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onTracksChanged(TrackGroupArray unused, TrackSelectionArray trackSelections) {
        if (exoPlayer == null || callbacks == null) return;

        final MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();

        HashMap<String, AudioTrack> audioTracks = new HashMap<>();
        LinkedList<TextTrack> textTracks = new LinkedList<>();
        boolean hasSelectedCc = false;
        int textRendererIndex = -1;

        for (int rendererIndex = 0; rendererIndex < exoPlayer.getRendererCount(); rendererIndex++) {
            TrackSelection trackSelection = trackSelections.get(rendererIndex);
            TrackGroupArray trackGroups = info.getTrackGroups(rendererIndex);
            int rendererType = exoPlayer.getRendererType(rendererIndex);
            if (rendererType == C.TRACK_TYPE_TEXT) textRendererIndex = rendererIndex;

            for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                TrackGroup trackGroup = trackGroups.get(groupIndex);

                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    if (info.getTrackFormatSupport(rendererIndex, groupIndex, trackIndex) != FORMAT_HANDLED)
                        continue;

                    Format format = trackGroup.getFormat(trackIndex);

                    boolean isSelected = trackSelection != null &&
                            trackGroups.indexOf(trackSelection.getTrackGroup()) == groupIndex &&
                            trackIndex == trackSelection.getSelectedIndexInTrackGroup();

                    switch (rendererType) {
                        case C.TRACK_TYPE_AUDIO:
                            String key = format.language == null ? format.id : format.language;
                            AudioTrack audioTrack = audioTracks.get(key);

                            if (audioTrack == null) {
                                audioTrack = new AudioTrack(new AudioTrack.Id(rendererIndex, groupIndex, trackIndex), format.language, format.id, isSelected);
                            } else {
                                audioTrack = audioTrack.withSelected(audioTrack.isSelected || isSelected);
                            }

                            audioTracks.put(format.language, audioTrack);
                            break;

                        case C.TRACK_TYPE_TEXT:
                            if (!hasSelectedCc && isSelected) hasSelectedCc = true;

                            if (!MimeTypes.APPLICATION_CEA608.equals(format.sampleMimeType) || isSelected) {
                                TextTrack.Id id = new TextTrack.Id(rendererIndex, groupIndex, trackIndex);
                                TextTrack textTrack = new TextTrack(id, format.language, isSelected);
                                textTracks.add(textTrack);
                            }
                            break;
                    }
                }
            }
        }

        TextTrack.Id id = new TextTrack.Id(textRendererIndex, -1, -1);
        Collections.sort(textTracks, (o1, o2) -> o1.title.compareTo(o2.title));
        textTracks.addFirst(new TextTrack(id, "None", !hasSelectedCc));

        LinkedList<AudioTrack> audioTrackList = new LinkedList<>(audioTracks.values());
        Collections.sort(audioTrackList, (o1, o2) -> o1.title.compareTo(o2.title));
        callbacks.onTrackInfoAvailable(audioTrackList, textTracks);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error.getCause() != null && error.getCause().getClass() == BehindLiveWindowException.class) {
            playVideo(videoUrl, subtitleLang, subtitleUrl);
            return;
        }

        String message = error.getCause() == null ? null : error.getCause().getMessage();
        boolean isConnectionError = hasConnectionError(error.getMessage()) || hasConnectionError(message);

        if (isConnectionError && subtitleUrl != null) {
            playVideo(videoUrl, null, null);
            return;
        }

        if (callbacks != null) {
            if (isConnectionError) {
                callbacks.onErrorOccurred(CONNECTION);
            } else {
                callbacks.onErrorOccurred(CONTENT);
            }
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
    }

    private boolean hasConnectionError(@Nullable String message) {
        return message != null &&
                (message.contains("Unable to connect to") || message.contains("Response code"));
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    }

    @Override
    public void onSeekProcessed() {
    }

    @Override
    public void dispose() {
        callbacks = null;

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

    @Override
    public void onVideoSurfaceAvailable(@NonNull final Surface surface) {
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
        if (callbacks != null) {
            callbacks.onVideoFrameGone();
        }

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
        if (callbacks != null) {
            callbacks.onVideoFrameVisible();
        }
    }

    private class ProgressTimer {
        @NonNull
        private Runnable action = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null && exoPlayer != null) {
                    long position = exoPlayer.getCurrentPosition();
                    if (position < 0) {
                        position = 0;
                    }
                    int bufferedPercentage = exoPlayer.getBufferedPercentage();
                    bufferedPercentage = bufferedPercentage > 100 ? 100 : bufferedPercentage;
                    bufferedPercentage = bufferedPercentage < 0 ? 0 : bufferedPercentage;
                    callbacks.onVideoBufferUpdated(bufferedPercentage);
                    if (duration == null) {
                        updateDuration();
                    }
                    if (duration != null && exoPlayer.getPlaybackState() == Player.STATE_READY
                            && shouldPlay && (position <= duration || duration == 0)) {
                        callbacks.onVideoPositionUpdated(position);
                    }
                }
                postDelayed(action, 200);
            }
        };

        public void start() {
            removeCallbacks(action);
            action.run();
        }

        void stop() {
            removeCallbacks(action);
        }
    }
}
