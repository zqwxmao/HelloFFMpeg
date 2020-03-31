package com.michael.libplayer.media;

import android.media.MediaCodec;
import android.util.Log;

import com.michael.libplayer.activity.PlayerCameraRecordMuxerActivity;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaRtmpEncoder {

    private static final String TAG = PlayerCameraRecordMuxerActivity.TAG + MediaRtmpEncoder.class.getSimpleName();

    private Thread videoThread;
    private Thread audioThread;

    private LinkedBlockingQueue<MediaMuxerThread.MuxerData> videoQueue;
    private LinkedBlockingQueue<MediaMuxerThread.MuxerData> audioQueue;

    private volatile boolean videoWorking;
    private volatile boolean audioWorking;

    public MediaRtmpEncoder() {
        videoThread = new Thread("Thread-videoRtmpEncoder") {
            @Override
            public void run() {
                while (videoWorking && !Thread.interrupted()) {
                    Log.i(TAG, Thread.currentThread().getId()+"-"+Thread.currentThread().getName());
                    try {
                        MediaMuxerThread.MuxerData muxerData = videoQueue.take();
                        encodeAvcFrame(muxerData.getByteBuffer(), muxerData.getBufferInfo());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        audioThread = new Thread("Audio-audioRtmpEncoder") {
            @Override
            public void run() {
                while (audioWorking && !Thread.interrupted()) {
                    Log.i(TAG, Thread.currentThread().getId()+"-"+Thread.currentThread().getName());
                    try {
                        MediaMuxerThread.MuxerData muxerData = audioQueue.take();
                        encodeAacFrame(muxerData.getByteBuffer(), muxerData.getBufferInfo());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
    }

    public void addVideoData(MediaMuxerThread.MuxerData muxerData) {
        videoQueue.add(muxerData);
    }

    public void addAudioData(MediaMuxerThread.MuxerData muxerData) {
        audioQueue.add(muxerData);
    }

    private void encodeAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
//        int offset = 4;
//        if (bb.get(2) == 0x01) {
//            offset = 3;
//        }
//        int type = bb.get(offset) & 0x1f;
        /*FloatBuffer floatBuffer = bb.asFloatBuffer();
        float[] floats = new float[floatBuffer.limit()];
        floatBuffer.get(floats);
        Log.d(TAG, "bb=" + Arrays.toString(floats));*/
//        Log.i(TAG, "hooory!   video type= "+type);
    }

    private void encodeAacFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {

    }

    public void start() {
        if (videoQueue == null) {
            videoQueue = new LinkedBlockingQueue<>();
        } else {
            videoQueue.clear();
        }
        if (audioQueue == null) {
            audioQueue = new LinkedBlockingQueue<>();
        } else {
            audioQueue.clear();
        }
        videoWorking = true;
        audioWorking = true;
        if (!videoThread.isAlive()) videoThread.start();
        if (!audioThread.isAlive()) audioThread.start();
    }

    public void stop() {
        videoWorking = false;
        audioWorking = false;
        videoThread.interrupt();
        audioThread.interrupt();
    }
}
