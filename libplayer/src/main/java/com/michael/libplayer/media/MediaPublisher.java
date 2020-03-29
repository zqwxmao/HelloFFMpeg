package com.michael.libplayer.media;

import com.michael.libplayer.media.bean.MediaPublisherConfig;

import java.util.concurrent.LinkedBlockingQueue;

public class MediaPublisher {
    private MediaPublisherConfig mediaPublisherConfig;
    private LinkedBlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();
    private Thread workThread;

    private MediaPublisher(MediaPublisherConfig mediaPublisherConfig) {
        this.mediaPublisherConfig = mediaPublisherConfig;
    }
    public static MediaPublisher newInstance(MediaPublisherConfig mediaPublisherConfig) {
        return new MediaPublisher(mediaPublisherConfig);
    }

    public void init() {
        this.workThread = new Thread("publish-work-thread") {
            @Override
            public void run() {
                super.run();
            }
        };
    }
}
