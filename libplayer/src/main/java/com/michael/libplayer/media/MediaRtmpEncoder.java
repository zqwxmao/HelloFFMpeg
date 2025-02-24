package com.michael.libplayer.media;

import android.media.MediaCodec;
import android.util.Log;

import com.michael.libplayer.activity.PlayerCameraRecordMuxerActivity;
import com.michael.libplayer.ffmpeg.FFMpegHandle;
import com.michael.libplayer.media.bean.MediaPublisherConfig;
import com.michael.libplayer.media.util.NumberUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaRtmpEncoder {

    private static final String TAG = PlayerCameraRecordMuxerActivity.TAG + MediaRtmpEncoder.class.getSimpleName();

    public static final int NAL_SLICE = 1;
    public static final int NAL_SLICE_DPA = 2;
    public static final int NAL_SLICE_DPB = 3;
    public static final int NAL_SLICE_DPC = 4;
    public static final int NAL_SLICE_IDR = 5;
    public static final int NAL_SEI = 6;
    public static final int NAL_SPS = 7;
    public static final int NAL_PPS = 8;
    public static final int NAL_AUD = 9;
    public static final int NAL_FILLER = 12;

    private Thread taskExecutionThread;

    private LinkedBlockingQueue<Runnable> workQueue;

    private volatile boolean taskExecutionWorking;
    private volatile boolean isConnecting;
    private MediaPublisher mediaPublisher;

    public MediaRtmpEncoder(MediaPublisherConfig mediaPublisherConfig) {
        workQueue = new LinkedBlockingQueue<>();
        mediaPublisher = MediaPublisher.newInstance(mediaPublisherConfig);
    }

    public void init() {
        taskExecutionThread = new Thread("Thread-workExecutionEncoder") {
            @Override
            public void run() {
                while (taskExecutionWorking && !Thread.interrupted()) {
                    Log.i(TAG, Thread.currentThread().getId()+"-"+Thread.currentThread().getName());
                    try {
                        Runnable runnable = workQueue.take();
                        runnable.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
        taskExecutionWorking = true;
        taskExecutionThread.start();
    }

    public void encodeAudioFrame(MediaMuxerThread.MuxerData muxerData) {
        encodeAacFrame(muxerData.getByteBuffer(), muxerData.getBufferInfo());
    }

    public void encodeVideoFrame(MediaMuxerThread.MuxerData muxerData) {
        encodeAvcFrame(muxerData.getByteBuffer(), muxerData.getBufferInfo());
    }

    private void encodeAvcFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
        int startCodeOffset = 4;
        if (bb.get(2) == 0x01) {
            startCodeOffset = 3;
        }
        int type = bb.get(startCodeOffset) & 0x1f;
        /*FloatBuffer floatBuffer = bb.asFloatBuffer();
        float[] floats = new float[floatBuffer.limit()];
        floatBuffer.get(floats);
        Log.d(TAG, "bb=" + Arrays.toString(floats));*/
        Log.i(TAG, "hooory!   video type= "+type);
        if (type == NAL_SPS || type == NAL_PPS) {
            final byte[] sps = new byte[vBufferInfo.size - 4 * 3];//sps和pps合并到一条数组中，sps字节数是总字节数减去sps起始码和pps起始码和pps4个字节数 即共 12字节；
            final byte[] pps = new byte[4];//打印发现pps为后4个字节；
            bb.getInt();
            bb.get(sps, 0, sps.length);
            bb.getInt();
            bb.get(pps, 0, pps.length);
            Log.i(TAG, "解析得到 sps:" + Arrays.toString(sps) + ",PPS=" + Arrays.toString(pps));
            Runnable runnable = () -> mediaPublisher.sendVideoSpec(sps, pps, vBufferInfo.presentationTimeUs / 1000L);
            try {
                workQueue.put(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (type == NAL_SLICE || type == NAL_SLICE_IDR) {
            byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            Log.i(TAG, "hooory!   FRAME "+ NumberUtil.encodeHex(bytes));
            Runnable runnable = () -> mediaPublisher.sendVideoData(bytes, vBufferInfo.presentationTimeUs / 1000L);
            try {
                workQueue.put(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void encodeAacFrame(ByteBuffer bb, final MediaCodec.BufferInfo vBufferInfo) {
        if (vBufferInfo.size == 2) {
            final byte[] aacSpec = new byte[2];
            bb.get(aacSpec);
            Log.i(TAG, "hooory!   AAC SPEC "+ NumberUtil.encodeHex(aacSpec));
            Runnable runnable = () -> mediaPublisher.sendAudioSpec(aacSpec);
            try {
                workQueue.put(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            final byte[] aacData = new byte[vBufferInfo.size];
            bb.get(aacData);
            Log.i(TAG, "hooory!   AAC DATA "+ NumberUtil.encodeHex(aacData));
            Runnable runnable = () -> mediaPublisher.sendAudioData(aacData, vBufferInfo.presentationTimeUs / 1000L);
            try {
                workQueue.put(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startGather() {
        MediaMuxerThread.startMuxer(false, muxerData -> {
            if (muxerData != null) {
                if (muxerData.getTrackIndex() == MediaMuxerThread.TRACK_VIDEO) {
                    encodeVideoFrame(muxerData);
                } else if (muxerData.getTrackIndex() == MediaMuxerThread.TRACK_AUDIO) {
                    encodeAudioFrame(muxerData);
                }
            }
        });
    }

    public void stopGather() {
        MediaMuxerThread.stopMuxer();
    }

    public void startEncoder() {

    }

    public void stopEncoder() {

    }

    public void startPublish() {
        Runnable runnable = () -> {
            workQueue.clear();
            int ret = mediaPublisher.init();
            Log.d(TAG, "rtmp connect result : "+ret);
            if (ret < 0) {
                Log.e(TAG, "rtmp connect failed. ret is : "+ret);
            } else {
                isConnecting = true;
            }
        };
        workQueue.add(runnable);
    }

    public void stopPublish() {
        Runnable runnable = () -> {
            taskExecutionWorking = false;
            taskExecutionThread.interrupt();
            int ret = mediaPublisher.close();
            if (ret < 0) {
                Log.e(TAG, "rtmp close failed. ret is : "+ret);
            }
            isConnecting = false;
        };
        workQueue.add(runnable);
    }
}
