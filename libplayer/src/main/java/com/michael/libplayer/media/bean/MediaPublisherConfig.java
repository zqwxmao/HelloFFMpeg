package com.michael.libplayer.media.bean;

import android.media.AudioFormat;

public class MediaPublisherConfig {
    private final int fps;
    private final int minWidth;
    private final int maxWidth;
    private final int timeOut;
    private final String publishUrl;
    private final int audioFormat;
    private final int channelConfig;
    private final int bitrate;

    public MediaPublisherConfig(int fps, int minWidth, int maxWidth, int timeOut, String publishUrl, int audioFormat, int channelConfig, int bitrate) {
        this.fps = fps;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.timeOut = timeOut;
        this.publishUrl = publishUrl;
        this.audioFormat = audioFormat;
        this.channelConfig = channelConfig;
        this.bitrate = bitrate;
    }

    public static class Builder {
        private int fps;
        private int minWidth;
        private int maxWidth;
        private int timeOut;
        private String publishUrl;
        private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        private int bitrate = 700 * 1000;
        public Builder() {
            fps = 30;
            minWidth = 320;
            maxWidth = 720;
            timeOut = 1000;
        }

        public Builder setFps(int fps) {
            this.fps = fps;
            return this;
        }

        public Builder setMinWidth(int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Builder setMaxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder setTimeOut(int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public Builder setPublishUrl(String publishUrl) {
            this.publishUrl = publishUrl;
            return this;
        }

        public Builder setAudioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        public Builder setChannelConfig(int channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public Builder setBitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public MediaPublisherConfig build() {
            return new MediaPublisherConfig(fps, minWidth, maxWidth, timeOut, publishUrl, audioFormat, channelConfig, bitrate);
        }
    }

    public int getFps() {
        return fps;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public String getPublishUrl() {
        return publishUrl;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getBitrate() {
        return bitrate;
    }
}
