package com.michael.libplayer.media;

import com.michael.libplayer.ffmpeg.FFMpegHandle;
import com.michael.libplayer.media.bean.MediaPublisherConfig;

public class MediaPublisher {

    private MediaPublisherConfig mediaPublisherConfig;

    private MediaPublisher(MediaPublisherConfig mediaPublisherConfig) {
        this.mediaPublisherConfig = mediaPublisherConfig;
    }
    public static MediaPublisher newInstance(MediaPublisherConfig mediaPublisherConfig) {
        return new MediaPublisher(mediaPublisherConfig);
    }

    public int init() {
        int ret = FFMpegHandle.getInstance().connect(mediaPublisherConfig.getPublishUrl(), mediaPublisherConfig.getMinWidth(), mediaPublisherConfig.getMaxWidth(), mediaPublisherConfig.getTimeOut());

        return ret;
    }

    public int close() {
        int ret = FFMpegHandle.getInstance().close();

        return ret;
    }

    public void sendVideoSpec(byte[] sps, byte[] pps, long timeStamps) {
        FFMpegHandle.getInstance().sendVideoSpec(sps, sps.length, pps, pps.length, timeStamps);
    }

    public void sendVideoData(byte[] frame, long timeStamps) {
        FFMpegHandle.getInstance().sendVideoData(frame, frame.length, timeStamps);
    }

    public void sendAudioSpec(byte[] aacSpec) {
        FFMpegHandle.getInstance().sendAudioSpec(aacSpec, aacSpec.length);
    }

    public void sendAudioData(byte[] aacData, long timeStamps) {
        FFMpegHandle.getInstance().sendAudioData(aacData, aacData.length, timeStamps);
    }
}
