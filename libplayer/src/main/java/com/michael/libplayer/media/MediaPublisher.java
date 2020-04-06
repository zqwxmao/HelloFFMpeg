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
        int ret = FFMpegHandle.getInstance().connect();

        return ret;
    }

    public int close() {
        int ret = FFMpegHandle.getInstance().close();

        return ret;
    }

    public void sendVideoSpec(byte[] sps, byte[] pps, long timeStamps) {

    }
}
