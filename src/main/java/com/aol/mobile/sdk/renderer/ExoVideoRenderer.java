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
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONNECTION;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONTENT;
import static com.google.android.exoplayer2.Format.createTextSampleFormat;
import static com.google.android.exoplayer2.RendererCapabilities.FORMAT_HANDLED;
import static com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP;
import static com.google.android.exoplayer2.util.Util.getUserAgent;
import static com.google.android.exoplayer2.util.Util.inferContentType;

class ExoVideoRenderer extends FrameLayout implements VideoRenderer, VideoSurfaceListener,
        Player.EventListener, SimpleExoPlayer.VideoListener {

    @NonNull
    private final Handler handler = new Handler();
    @NonNull
    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    @NonNull
    private final TrackSelection.Factory adaptiveTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
    @NonNull
    private final DefaultExtractorsFactory defaultExtractorsFactory = new DefaultExtractorsFactory();
    @NonNull
    private final TrackSelection.Factory selectionFactory = new FixedTrackSelection.Factory();
    @NonNull
    private final DataSource.Factory dataSourceFactory;
    @NonNull
    private final DefaultTrackSelector trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
    @Nullable
    protected VideoVM.Callbacks callbacks;
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
    @Nullable
    private String subtitleLang;

    public ExoVideoRenderer(@NonNull Context context) {
        super(context);
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                getUserAgent(context, "VideoSDK"), bandwidthMeter, 8000, 8000, true);

        this.dataSourceFactory = new DefaultDataSourceFactory(context, bandwidthMeter, httpDataSourceFactory);
        this.context = context;
    }

    @NonNull
    private MediaSource buildMediaSource(@NonNull String videoUriString, @Nullable String subtitleLang, @Nullable String subtitleUriString) {
        Uri videoUri = Uri.parse(videoUriString);
        MediaSource source;

        switch (inferContentType(videoUri.getLastPathSegment())) {
            case C.TYPE_HLS:
                source = new HlsMediaSource(videoUri, dataSourceFactory, handler, new AdaptiveMediaSourceEventListener() {
                    @Override
                    public void onLoadStarted(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2) {
                    }

                    @Override
                    public void onLoadCompleted(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4) {
                        if (callbacks != null && format != null) {
                            callbacks.onHlsBitrateUpdated(format.bitrate);
                        }
                    }

                    @Override
                    public void onLoadCanceled(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4) {
                    }

                    @Override
                    public void onLoadError(DataSpec dataSpec, int i, int i1, Format format, int i2, Object o, long l, long l1, long l2, long l3, long l4, IOException e, boolean b) {
                        if (shouldPlay && exoPlayer != null && !exoPlayer.getPlayWhenReady()) {
                            playVideo(videoUrl, ExoVideoRenderer.this.subtitleLang, subtitleUrl);
                        }
                    }

                    @Override
                    public void onUpstreamDiscarded(int i, long l, long l1) {
                    }

                    @Override
                    public void onDownstreamFormatChanged(int i, Format format, int i1, Object o, long l) {
                    }
                });
                break;

            default:
                source = new ExtractorMediaSource(videoUri, dataSourceFactory, defaultExtractorsFactory, handler, null);
                Uri subtitleUri = subtitleUriString == null ? null : Uri.parse(subtitleUriString);
                if (subtitleUri != null) {
                    Format textFormat = createTextSampleFormat(subtitleLang, APPLICATION_SUBRIP, null, C.POSITION_UNSET, C.POSITION_UNSET, subtitleLang, null, 9223372036854775807L);
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

    private void playVideo(@Nullable String videoUrl, @Nullable String subLang, @Nullable String subtitleUrl) {
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
        this.subtitleLang = subLang;
        if (videoUrl == null) return;

        updateExoPlayerSource(buildMediaSource(videoUrl, subtitleLang, subtitleUrl));
    }

    private void updateExoPlayerSource(@NonNull final MediaSource source) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        exoPlayer.prepare(source);
        exoPlayer.addTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(@Nullable List<Cue> cues) {
                if (callbacks == null) return;
                if (cues != null && cues.size() > 0) {
                    callbacks.onSubtitleUpdated(cues.get(0).text);
                } else {
                    callbacks.onSubtitleUpdated(null);
                }
            }
        });

        for (int rendererIndex = 0; rendererIndex < exoPlayer.getRendererCount(); rendererIndex++) {
            if (exoPlayer.getRendererType(rendererIndex) == C.TRACK_TYPE_TEXT) {
                trackSelector.setRendererDisabled(rendererIndex, true);
                break;
            }
        }

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
        renderCallbacks(videoVM.callbacks);

        this.scalable = videoVM.isScalable;
        this.maintainAspectRatio = videoVM.isMaintainAspectRatio;
        if (videoVM.videoUrl != null && !videoVM.videoUrl.equals(videoUrl)) {
            playVideo(videoVM.videoUrl, videoVM.subtitleLang, videoVM.subtitleUrl);
        } else if (videoVM.videoUrl == null && videoUrl != null) {
            playVideo(null, null, null);
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
        if (audioTrack == null) return;

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        trackSelector.clearSelectionOverrides(audioTrack.id.renderer);
        trackSelector.setParameters(trackSelector.getParameters().withPreferredAudioLanguage(null));

        if (audioTrack.language == null || audioTrack.language.length() == 0) {
            TrackGroupArray trackGroups = info.getTrackGroups(audioTrack.id.renderer);
            SelectionOverride override = new SelectionOverride(selectionFactory, audioTrack.id.group, audioTrack.id.track);
            trackSelector.setSelectionOverride(audioTrack.id.renderer, trackGroups, override);
        } else {
            trackSelector.setParameters(trackSelector.getParameters().withPreferredAudioLanguage(audioTrack.language));
        }
    }

    private void switchToTextTrack(@Nullable TextTrack textTrack) {
        if (textTrack == null) return;

        MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
        if (textTrack.id.group < 0 || textTrack.id.track < 0) {
            trackSelector.clearSelectionOverrides(textTrack.id.renderer);
            trackSelector.setRendererDisabled(textTrack.id.renderer, true);
        } else {
            TrackGroupArray trackGroups = info.getTrackGroups(textTrack.id.renderer);
            SelectionOverride override = new SelectionOverride(selectionFactory, textTrack.id.group, textTrack.id.track);
            trackSelector.setRendererDisabled(textTrack.id.renderer, false);
            trackSelector.setSelectionOverride(textTrack.id.renderer, trackGroups, override);
        }
    }

    private void pausePlayback() {
        if (shouldPlay) {
            shouldPlay = false;

            if (exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_READY) {
                exoPlayer.setPlayWhenReady(false);
                progressTimer.stop();
                if (callbacks != null) {
                    callbacks.onVideoPlaybackFlagUpdated(exoPlayer.getPlayWhenReady());
                }
            }
        }
    }

    private void resumePlayback() {
        if (!shouldPlay) {
            shouldPlay = true;

            if (exoPlayer != null && exoPlayer.getPlaybackState() == Player.STATE_READY) {
                if (exoPlayer.isCurrentWindowDynamic() && exoPlayer.isCurrentWindowSeekable()) {
                    exoPlayer.seekToDefaultPosition();
                }
                exoPlayer.setPlayWhenReady(true);
                progressTimer.start();
                if (callbacks != null) {
                    callbacks.onVideoPlaybackFlagUpdated(exoPlayer.getPlayWhenReady());
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

        if (callbacks != null) {
            callbacks.onSeekPerformed();
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
        if (this.playbackState != playbackState) {
            this.playbackState = playbackState;
            assert exoPlayer != null;

            switch (playbackState) {
                case Player.STATE_READY:
                    progressTimer.start();
                    exoPlayer.setPlayWhenReady(shouldPlay);
                    if (callbacks != null) {
                        callbacks.onVideoPlaybackFlagUpdated(shouldPlay);
                    }
                    break;

                case Player.STATE_IDLE:
                case Player.STATE_BUFFERING:
                    progressTimer.stop();
                    exoPlayer.setPlayWhenReady(false);
                    if (callbacks != null) {
                        callbacks.onVideoPlaybackFlagUpdated(false);
                    }
                    break;

                case Player.STATE_ENDED:
                    if (!exoPlayer.isCurrentWindowDynamic()) {
                        exoPlayer.setPlayWhenReady(false);
                        progressTimer.stop();
                        if (callbacks != null) {
                            if (this.duration == null) {
                                callbacks.onErrorOccurred(CONTENT);
                                return;
                            }
                            callbacks.onVideoPositionUpdated(exoPlayer.getDuration());
                        }
                        if (callbacks != null) {
                            callbacks.onVideoPlaybackFlagUpdated(false);
                            callbacks.onVideoEnded();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int i) {
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
    }

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
        Collections.sort(textTracks, new Comparator<TextTrack>() {
            @Override
            public int compare(TextTrack o1, TextTrack o2) {
                return o1.title.compareTo(o2.title);
            }
        });
        textTracks.addFirst(new TextTrack(id, "None", !hasSelectedCc));

        LinkedList<AudioTrack> audioTrackList = new LinkedList<>(audioTracks.values());
        Collections.sort(audioTrackList, new Comparator<AudioTrack>() {
            @Override
            public int compare(AudioTrack o1, AudioTrack o2) {
                return o1.title.compareTo(o2.title);
            }
        });
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

    @NonNull
    @Override
    public View getViewport() {
        return this;
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
                    if (duration != null && position > duration && playbackState != Player.STATE_ENDED
                            && exoPlayer.getPlayWhenReady()) {
                        onPlayerStateChanged(shouldPlay, Player.STATE_ENDED);
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
