package com.aol.mobile.sdk.renderer.viewmodel;

import com.aol.mobile.sdk.chromecast.CastVideoVM;

public class CastVideoVMTranslator {
    private CastVideoVM castVideoVM = new CastVideoVM();
    private VideoVM.Callbacks callbacks;

    public CastVideoVM translate(final VideoVM videoVM) {
        if (castVideoVM.callbacks == null) {
            castVideoVM.callbacks = new CastVideoVM.Callbacks() {
                @Override
                public void onDurationReceived(long duration) {
                    if (callbacks == null) return;
                    callbacks.onDurationReceived(duration);
                }

                @Override
                public void onVideoPositionUpdated(long position) {
                    if (callbacks == null) return;
                    callbacks.onVideoPositionUpdated(position);
                }

                @Override
                public void onVideoPlaybackFlagUpdated(boolean isActuallyPlaying) {
                    if (callbacks == null) return;
                    callbacks.onVideoPlaybackFlagUpdated(isActuallyPlaying);
                }

                @Override
                public void onVideoEnded() {
                    if (callbacks == null) return;
                    callbacks.onVideoEnded();
                }

                @Override
                public void onSeekPerformed() {
                    if (callbacks == null) return;
                    callbacks.onSeekPerformed();
                }
            };
        }
        castVideoVM.videoUrl = videoVM.videoUrl;
        castVideoVM.title = videoVM.title;
        castVideoVM.seekPosition = videoVM.seekPosition;
        castVideoVM.currentPosition = videoVM.currentPosition;
        castVideoVM.isMuted = videoVM.isMuted;
        castVideoVM.shouldPlay = videoVM.shouldPlay;
        castVideoVM.isLive = videoVM.isLive;
        castVideoVM.isActive = videoVM.isActive;
        callbacks = videoVM.callbacks;
        return castVideoVM;
    }
}
