/*
 * Copyright 2018, Oath Inc.
 * Licensed under the terms of the MIT License. See LICENSE.md file in project root for terms.
 */

package com.aol.mobile.sdk.renderer;

import android.content.Context;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import com.aol.mobile.sdk.renderer.gles.VideoSurfaceListener;
import com.aol.mobile.sdk.renderer.internal.FlatRendererView;
import com.aol.mobile.sdk.renderer.internal.GlEsRendererView;
import com.aol.mobile.sdk.renderer.internal.ttml.TtmlDecoder;
import com.aol.mobile.sdk.renderer.viewmodel.VideoVM;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.DefaultMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.text.SubtitleDecoder;
import com.google.android.exoplayer2.text.SubtitleDecoderException;
import com.google.android.exoplayer2.text.SubtitleDecoderFactory;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.text.cea.Cea608Decoder;
import com.google.android.exoplayer2.text.cea.Cea708Decoder;
import com.google.android.exoplayer2.text.dvb.DvbDecoder;
import com.google.android.exoplayer2.text.pgs.PgsDecoder;
import com.google.android.exoplayer2.text.ssa.SsaDecoder;
import com.google.android.exoplayer2.text.subrip.SubripDecoder;
import com.google.android.exoplayer2.text.tx3g.Tx3gDecoder;
import com.google.android.exoplayer2.text.webvtt.Mp4WebvttDecoder;
import com.google.android.exoplayer2.text.webvtt.WebvttDecoder;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONNECTION;
import static com.aol.mobile.sdk.renderer.viewmodel.VideoVM.Callbacks.Error.CONTENT;
import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
import static com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
import static com.google.android.exoplayer2.Format.createTextSampleFormat;
import static com.google.android.exoplayer2.RendererCapabilities.FORMAT_HANDLED;
import static com.google.android.exoplayer2.util.MimeTypes.APPLICATION_SUBRIP;
import static com.google.android.exoplayer2.util.MimeTypes.APPLICATION_TTML;
import static com.google.android.exoplayer2.util.MimeTypes.TEXT_SSA;
import static com.google.android.exoplayer2.util.MimeTypes.TEXT_VTT;
import static com.google.android.exoplayer2.util.Util.areEqual;
import static com.google.android.exoplayer2.util.Util.getUserAgent;
import static com.google.android.exoplayer2.util.Util.inferContentType;

class ExoVideoRenderer extends FrameLayout implements VideoRenderer, VideoSurfaceListener,
        Player.EventListener, VideoListener {
    @NonNull
    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder()
            .setInitialBitrateEstimate(12094635)
            .build();
    @NonNull
    private final SubtitleView subtitleView;
    @Nullable
    protected VideoVM.Callbacks callbacks;
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
    @NonNull
    private List<ExternalSubtitle> externalSubtitles = new ArrayList<>();
    private boolean endReported;
    private int extensionRendererMode = EXTENSION_RENDERER_MODE_ON;

    public ExoVideoRenderer(@NonNull Context context) {
        super(context);

        subtitleView = new SubtitleView(context);

        this.context = context;
    }

    @NonNull
    private MediaSource buildMediaSource(@NonNull String videoUriString, @NonNull List<ExternalSubtitle> externalSubtitles) {
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(
                getUserAgent(context, "VideoSDK"), bandwidthMeter,
                8000, 8000, true);

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, bandwidthMeter, httpDataSourceFactory);

        Uri videoUri = Uri.parse(videoUriString);
        MediaSource source;
        final Handler handler = new Handler();

        switch (inferContentType(videoUri.getLastPathSegment())) {
            case C.TYPE_HLS:
                HlsMediaSource.Factory hlsMediaFactory = new HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true);
                source = hlsMediaFactory.createMediaSource(videoUri);
                source.addEventListener(handler, new DefaultMediaSourceEventListener() {
                    @Override
                    public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
                        if (callbacks != null && mediaLoadData.trackFormat != null) {
                            callbacks.onHlsBitrateUpdated(mediaLoadData.trackFormat.bitrate);
                        }
                    }

                    @Override
                    public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
                        if (shouldPlay && exoPlayer != null && !exoPlayer.getPlayWhenReady()) {
                            playVideo(videoUrl, externalSubtitles);
                        }
                    }
                });
                break;

            default:
                ExtractorMediaSource.Factory mediaFactory = new ExtractorMediaSource.Factory(dataSourceFactory);
                source = mediaFactory.createMediaSource(videoUri);
                break;
        }

        for (ExternalSubtitle subtitle : externalSubtitles) {
            String format = getFormat(subtitle.format);
            SingleSampleMediaSource.Factory srtMediaFactory = new SingleSampleMediaSource.Factory(dataSourceFactory);
            Format textFormat = createTextSampleFormat(null, format, Format.NO_VALUE, subtitle.language);
            MediaSource subtitleSource = srtMediaFactory.createMediaSource(Uri.parse(subtitle.url), textFormat, C.TIME_UNSET);
            source = new MergingMediaSource(source, subtitleSource);
        }
        return source;
    }

    @NonNull
    private String getFormat(@NonNull String format) {
        switch (format.toUpperCase()) {
            case "SRT":
                return APPLICATION_SUBRIP;

            case "VTT":
                return TEXT_VTT;

            case "SSA":
                return TEXT_SSA;

            case "TT":
            case "DFXP":
            case "TTML":
                return APPLICATION_TTML;
        }

        return format;
    }

    protected void setRenderer(@NonNull View renderer) {
        this.streamRenderer = renderer;

        removeAllViews();
        addView(streamRenderer, MATCH_PARENT, MATCH_PARENT);
        addView(subtitleView, MATCH_PARENT, MATCH_PARENT);
    }

    private void playVideo(@Nullable String videoUrl, @NonNull List<ExternalSubtitle> externalSubtitles) {
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
            exoPlayer = null;
            progressTimer.stop();
        }

        this.videoUrl = videoUrl;
        if (this.externalSubtitles != externalSubtitles) {
            this.externalSubtitles.clear();
            this.externalSubtitles.addAll(externalSubtitles);
        }
        if (videoUrl == null) {
            if (callbacks != null) {
                callbacks.onVideoFrameGone();
            }
            return;
        }

        updateExoPlayerSource(buildMediaSource(videoUrl, this.externalSubtitles));
    }

    private void updateExoPlayerSource(@NonNull final MediaSource source) {
        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory(bandwidthMeter));
        exoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(context, extensionRendererMode) {
            @Override
            protected void buildTextRenderers(Context context, TextOutput output, Looper outputLooper, int extensionRendererMode, ArrayList<Renderer> out) {
                out.add(new TextRenderer(output, outputLooper, new SubtitleDecoderFactory() {

                    @Override
                    public boolean supportsFormat(Format format) {
                        String mimeType = format.sampleMimeType;
                        return MimeTypes.TEXT_VTT.equals(mimeType)
                                || MimeTypes.TEXT_SSA.equals(mimeType)
                                || MimeTypes.APPLICATION_TTML.equals(mimeType)
                                || MimeTypes.APPLICATION_MP4VTT.equals(mimeType)
                                || MimeTypes.APPLICATION_SUBRIP.equals(mimeType)
                                || MimeTypes.APPLICATION_TX3G.equals(mimeType)
                                || MimeTypes.APPLICATION_CEA608.equals(mimeType)
                                || MimeTypes.APPLICATION_MP4CEA608.equals(mimeType)
                                || MimeTypes.APPLICATION_CEA708.equals(mimeType)
                                || MimeTypes.APPLICATION_DVBSUBS.equals(mimeType)
                                || MimeTypes.APPLICATION_PGS.equals(mimeType);
                    }

                    @Override
                    public SubtitleDecoder createDecoder(Format format) {
                        if (format == null || format.sampleMimeType == null)
                            throw new IllegalArgumentException("Attempted to create decoder for unsupported format");

                        switch (format.sampleMimeType) {
                            case MimeTypes.TEXT_VTT:
                                return new WebvttDecoder();
                            case MimeTypes.TEXT_SSA:
                                return new SsaDecoder(format.initializationData);
                            case MimeTypes.APPLICATION_MP4VTT:
                                return new Mp4WebvttDecoder();
                            case MimeTypes.APPLICATION_TTML:
                                return new TtmlDecoder();
                            case MimeTypes.APPLICATION_SUBRIP:
                                return new SubripDecoder();
                            case MimeTypes.APPLICATION_TX3G:
                                return new Tx3gDecoder(format.initializationData);
                            case MimeTypes.APPLICATION_CEA608:
                            case MimeTypes.APPLICATION_MP4CEA608:
                                return new Cea608Decoder(format.sampleMimeType, format.accessibilityChannel);
                            case MimeTypes.APPLICATION_CEA708:
                                return new Cea708Decoder(format.accessibilityChannel);
                            case MimeTypes.APPLICATION_DVBSUBS:
                                return new DvbDecoder(format.initializationData);
                            case MimeTypes.APPLICATION_PGS:
                                return new PgsDecoder();
                            default:
                                throw new IllegalArgumentException(
                                        "Attempted to create decoder for unsupported format");
                        }
                    }
                }));
            }
        }, trackSelector);
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
            matrix.setScale(scaleX, scaleY, viewportWidth / 2f, viewportHeight / 2f);

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
        subtitleView.setApplyEmbeddedStyles(!videoVM.preferAccessibilityCcStyle);

        if (videoVM.callbacks != null) {
            renderCallbacks(videoVM.callbacks);
        }

        this.scalable = videoVM.isScalable;
        this.maintainAspectRatio = videoVM.isMaintainAspectRatio;
        if (videoVM.isRecovery || !areEqual(videoUrl, videoVM.videoUrl) ) {
            extensionRendererMode = videoVM.useSoftwareCodec ? EXTENSION_RENDERER_MODE_PREFER : EXTENSION_RENDERER_MODE_ON;
            playVideo(videoVM.videoUrl, videoVM.externalSubtitles);
            videoVM.isRecovery = false;
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
                                TextTrack textTrack = new TextTrack(id, getDisplayLanguage(format.language), format.language, isSelected);
                                textTracks.add(textTrack);
                            }
                            break;
                    }
                }
            }
        }

        TextTrack.Id id = new TextTrack.Id(textRendererIndex, -1, -1);
        Collections.sort(textTracks, (o1, o2) -> o1.title.compareTo(o2.title));
        textTracks.addFirst(new TextTrack(id, "None", "", !hasSelectedCc));

        LinkedList<AudioTrack> audioTrackList = new LinkedList<>(audioTracks.values());
        Collections.sort(audioTrackList, (o1, o2) -> o1.title.compareTo(o2.title));
        callbacks.onTrackInfoAvailable(audioTrackList, textTracks);
    }

    @NonNull
    private String getDisplayLanguage(@Nullable String language) {
        if (language == null) return "";

        try {
            Locale locale = new Locale(language);
            return locale.getDisplayLanguage();
        } catch (Exception e) {
            return language;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error.getCause() != null && error.getCause().getClass() == BehindLiveWindowException.class) {
            playVideo(videoUrl, externalSubtitles);
            return;
        }

        if (error.getCause() != null && error.getCause().getClass() == SubtitleDecoderException.class) {
            if (callbacks != null && textTrack != null) {
                callbacks.onTextTrackFailed(textTrack);
            }
            long position = exoPlayer != null ? exoPlayer.getContentPosition() : 0;
            playVideo(videoUrl, externalSubtitles);
            seekTo(position);
            return;
        }

        String message = error.getCause() == null ? null : error.getCause().getMessage();
        boolean isConnectionError = hasConnectionError(error.getMessage()) || hasConnectionError(message);

        if (callbacks != null) {
            if (isConnectionError) {
                callbacks.onErrorOccurred(CONNECTION);
                callbacks.onError(new ErrorInfo(CONNECTION, error, getExoplaybackExceptionCause(error)));
            } else {
                callbacks.onErrorOccurred(CONTENT);
                callbacks.onError(new ErrorInfo(CONTENT, error, getExoplaybackExceptionCause(error)));
            }
        }
    }



    /**
     * Extract detailed error message from {@link ExoPlaybackException}
     *
     * @param error
     * @return errorString {@link String}
     */
    private String getExoplaybackExceptionCause(ExoPlaybackException error) {
        String errorString = error.getMessage();
        String msg = "";

        switch (error.type) {
            case ExoPlaybackException.TYPE_RENDERER:
                if (error.type == ExoPlaybackException.TYPE_RENDERER) {
                    Exception cause = error.getRendererException();
                    if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                        // Special case for decoder initialization failures.
                        MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                                (MediaCodecRenderer.DecoderInitializationException) cause;
                        if (decoderInitializationException.decoderName == null) {
                            if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                                msg = "Unable to query device decoders";
                            } else if (decoderInitializationException.secureDecoderRequired) {
                                msg = String.format("This device does not provide a secure decoder for %s",
                                        decoderInitializationException.mimeType);
                            } else {
                                msg = String.format("This device does not provide a decoder for %s",
                                        decoderInitializationException.mimeType);
                            }
                        } else {
                            msg = String.format("Unable to instantiate decoder %s",
                                    decoderInitializationException.decoderName);
                        }
                    }
                }
                errorString = "Renderer Exception - " + msg;
                break;
            case ExoPlaybackException.TYPE_SOURCE:
                Exception cause = error.getSourceException();
                if (cause instanceof HttpDataSource.HttpDataSourceException) {
                    msg = "HttpDataSource.HttpDataSourceException: "+ errorString;
                } else if(cause instanceof HttpDataSource.InvalidContentTypeException) {
                    msg = "HttpDataSource.InvalidContentTypeException: "+ errorString;
                } else if(cause instanceof HttpDataSource.InvalidResponseCodeException) {
                    msg = "HttpDataSource.InvalidResponseCodeException: "+ errorString;
                }
                errorString = "Source Exception - " + msg;
                break;
            case ExoPlaybackException.TYPE_UNEXPECTED:
                msg = error.getUnexpectedException().getMessage();
                errorString = "Unexpected Exception - " + msg;
                break;
        }
        return errorString;
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
