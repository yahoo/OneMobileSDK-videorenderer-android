/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer.internal;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;
import java.util.List;

public final class OneExoPlayer implements ExoPlayer {

    public final static int VIDEO_RENDERER = 0;
    public final static int AUDIO_RENDERER = 1;
    public final static int TEXT_RENDERER = 2;
    private static final int MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50;
    @NonNull
    private final ExoPlayer player;
    @NonNull
    private final Renderer[] renderers;
    @NonNull
    private final ComponentListener componentListener;
    @NonNull
    private final Handler mainHandler;
    @Nullable
    private Surface surface;
    private boolean ownsSurface;
    @Nullable
    private SurfaceHolder surfaceHolder;
    @Nullable
    private TextRenderer.Output textOutput;
    @Nullable
    private VideoListener videoListener;


    /* package */ OneExoPlayer(@NonNull Context context, @NonNull TrackSelector trackSelector,
                               @NonNull LoadControl loadControl, long allowedVideoJoiningTimeMs) {
        mainHandler = new Handler();
        componentListener = new ComponentListener();

        renderers = buildRenderers(context, allowedVideoJoiningTimeMs);
        player = ExoPlayerFactory.newInstance(renderers, trackSelector, loadControl);
    }

    public Renderer getRenderer(int index) {
        return renderers[index];
    }

    public void setVideoSurface(@Nullable Surface surface) {
        removeSurfaceCallbacks();
        setVideoSurfaceInternal(surface, false);
    }

    public void setVideoListener(@NonNull VideoListener listener) {
        videoListener = listener;
    }

    public void setTextOutput(@NonNull TextRenderer.Output output) {
        textOutput = output;
    }

    @Override
    public void addListener(EventListener listener) {
        player.addListener(listener);
    }

    @Override
    public void removeListener(EventListener listener) {
        player.removeListener(listener);
    }

    @Override
    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    @Override
    public void prepare(MediaSource mediaSource) {
        player.prepare(mediaSource);
    }

    @Override
    public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetTimeline) {
        player.prepare(mediaSource, resetPosition, resetTimeline);
    }

    @Override
    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    @Override
    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    @Override
    public boolean isLoading() {
        return player.isLoading();
    }

    @Override
    public void seekToDefaultPosition() {
        player.seekToDefaultPosition();
    }

    @Override
    public void seekToDefaultPosition(int windowIndex) {
        player.seekToDefaultPosition(windowIndex);
    }

    @Override
    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    @Override
    public void seekTo(int windowIndex, long positionMs) {
        player.seekTo(windowIndex, positionMs);
    }

    @Override
    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {
    }

    @Override
    public PlaybackParameters getPlaybackParameters() {
        return null;
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void release() {
        player.release();
        removeSurfaceCallbacks();
        if (surface != null) {
            if (ownsSurface) {
                surface.release();
            }
            surface = null;
        }
    }

    @Override
    public void sendMessages(ExoPlayerMessage... messages) {
        player.sendMessages(messages);
    }

    @Override
    public void blockingSendMessages(ExoPlayerMessage... messages) {
        player.blockingSendMessages(messages);
    }

    @Override
    public int getRendererCount() {
        return 1;
    }

    @Override
    public int getRendererType(int index) {
        return index;
    }

    @Override
    @Nullable
    public TrackGroupArray getCurrentTrackGroups() {
        return null;
    }

    @Override
    @Nullable
    public TrackSelectionArray getCurrentTrackSelections() {
        return null;
    }

    @Override
    public int getCurrentPeriodIndex() {
        return player.getCurrentPeriodIndex();
    }

    @Override
    public int getCurrentWindowIndex() {
        return player.getCurrentWindowIndex();
    }

    @Override
    public long getDuration() {
        return player.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    @Override
    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    @Override
    public boolean isCurrentWindowDynamic() {
        return false;
    }

    @Override
    public boolean isCurrentWindowSeekable() {
        return false;
    }

    @Override
    @NonNull
    public Timeline getCurrentTimeline() {
        return player.getCurrentTimeline();
    }

    @Override
    @Nullable
    public Object getCurrentManifest() {
        return player.getCurrentManifest();
    }

    @NonNull
    private Renderer[] buildRenderers(@NonNull Context context, long allowedVideoJoiningTimeMs) {
        ArrayList<Renderer> renderersList = new ArrayList<>();
        MediaCodecVideoRenderer videoRenderer = new MediaCodecVideoRenderer(context,
                MediaCodecSelector.DEFAULT, allowedVideoJoiningTimeMs, null, false, mainHandler,
                componentListener, MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
        renderersList.add(videoRenderer);

        Renderer audioRenderer = new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT,
                null, true, mainHandler, null, AudioCapabilities.getCapabilities(context));

        renderersList.add(audioRenderer);

        Renderer textRenderer = new TextRenderer(componentListener, mainHandler.getLooper());
        renderersList.add(textRenderer);
        return renderersList.toArray(new Renderer[renderersList.size()]);
    }

    private void removeSurfaceCallbacks() {
        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(componentListener);
            surfaceHolder = null;
        }
    }

    public void setVolume(float volume) {
        player.sendMessages(new ExoPlayerMessage(getRenderer(AUDIO_RENDERER), C.MSG_SET_VOLUME, volume));
    }

    private void setVideoSurfaceInternal(@Nullable Surface surface, boolean ownsSurface) {
        ExoPlayerMessage[] messages = new ExoPlayerMessage[1];
        int count = 0;
        for (Renderer renderer : renderers) {
            if (renderer.getTrackType() == C.TRACK_TYPE_VIDEO) {
                messages[count++] = new ExoPlayerMessage(renderer, C.MSG_SET_SURFACE, surface);
            }
        }
        if (this.surface != null && this.surface != surface) {
            if (this.ownsSurface) {
                this.surface.release();
            }
            player.blockingSendMessages(messages);
        } else {
            player.sendMessages(messages);
        }
        this.surface = surface;
        this.ownsSurface = ownsSurface;
    }

    public interface VideoListener {

        void onVideoSizeChanged(int width, int height);
    }

    private final class ComponentListener implements VideoRendererEventListener,
            TextRenderer.Output, SurfaceHolder.Callback {

        @Override
        public void onVideoEnabled(DecoderCounters counters) {
        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs,
                                              long initializationDurationMs) {
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
        }

        @Override
        public void onDroppedFrames(int count, long elapsed) {
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            if (videoListener != null) {
                videoListener.onVideoSizeChanged(width, height);
            }
        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {
        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {
        }

        @Override
        public void onCues(List<Cue> cues) {
            if (textOutput != null) {
                textOutput.onCues(cues);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setVideoSurfaceInternal(holder.getSurface(), false);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            setVideoSurfaceInternal(null, false);
        }
    }
}
