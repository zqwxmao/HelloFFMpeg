package com.michael.libplayer.media;

public abstract class AbstractMediaEncoderThread extends Thread {
    protected boolean isMuxed;
    public AbstractMediaEncoderThread(boolean isMuxed) {
        this.isMuxed = isMuxed;
    }
}
