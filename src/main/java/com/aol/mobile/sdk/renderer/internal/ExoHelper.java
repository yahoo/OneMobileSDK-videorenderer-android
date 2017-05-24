/*
 * Copyright (c) 2016 One by Aol : Publishers. All rights reserved.
 */

package com.aol.mobile.sdk.renderer.internal;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import static com.google.android.exoplayer2.util.Util.getUserAgent;

public class ExoHelper {
    public static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;
    @NonNull
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final int MIN_BUFFER_MS = 5000;
    private static final int MIN_REBUFFER_MS = 5000;

    @NonNull
    public static OneExoPlayer getExoPlayer(@NonNull Context context) {
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);

        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                MIN_BUFFER_MS,
                MIN_REBUFFER_MS);

        return new OneExoPlayer(context, trackSelector, loadControl, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
    }

    @NonNull
    public static MediaSource buildMediaSource(@NonNull String videoUriString, @Nullable String subtitleUriString,
                                               @NonNull Handler handler, @NonNull Context context) {
        Uri videoUri = Uri.parse(videoUriString);
        Uri subtitleUri = subtitleUriString == null ? null : Uri.parse(subtitleUriString);
        int type = Util.inferContentType(videoUri.getLastPathSegment());
        MediaSource source;
        switch (type) {
            case C.TYPE_HLS:
                source = new HlsMediaSource(videoUri, buildDataSourceFactory(context, BANDWIDTH_METER),
                        handler, null);
                break;
            default: {
                source = new ExtractorMediaSource(videoUri, buildDataSourceFactory(context, BANDWIDTH_METER),
                        new DefaultExtractorsFactory(), handler, null);
            }
        }
        if (subtitleUri != null) {
            Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                    null, Format.NO_VALUE, Format.NO_VALUE, "en", null);
            MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, buildDataSourceFactory(context, null),
                    textFormat, C.TIME_UNSET);
            source = new MergingMediaSource(source, subtitleSource);
        }
        return source;
    }

    @NonNull
    private static DataSource.Factory buildDataSourceFactory(@NonNull Context context, @Nullable DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(context, bandwidthMeter));
    }

    @NonNull
    private static HttpDataSource.Factory buildHttpDataSourceFactory(@NonNull Context context, @Nullable DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(getUserAgent(context, "VideoSDK"), bandwidthMeter, 8000, 8000, true);
    }
}
