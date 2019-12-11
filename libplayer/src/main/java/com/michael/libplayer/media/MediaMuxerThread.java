package com.michael.libplayer.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.michael.libplayer.activity.PlayerCameraRecordMuxerActivity;
import com.michael.libplayer.util.FileUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Vector;

public class MediaMuxerThread extends Thread {

    private static final String TAG = PlayerCameraRecordMuxerActivity.TAG + MediaMuxerThread.class.getSimpleName();

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    private static volatile MediaMuxerThread INSTANCE;

    private final Object lock = new Object();

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;

    //音视频轨添加状态
    private volatile boolean isVideoTrackAdd = false;
    private volatile boolean isAudioTrackAdd = false;
    private volatile boolean isExit = false;

    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;
    private FileUtils fileSwapHelper;
    private VideoEncoderThread videoEncoderThread;
    private AudioEncoderThread audioEncoderThread;

    private MediaMuxerThread(){}

    public static void startMuxer() {
        if (INSTANCE == null) {
            synchronized (MediaMuxerThread.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MediaMuxerThread();
                    INSTANCE.start();
                }
            }
        }
    }

    public static void stopMuxer() {
        if (INSTANCE != null) {
            INSTANCE.exit();
            try {
                //调用此方法的主线程等待子线程执行完之后再执行
                INSTANCE.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            INSTANCE = null;
        }
    }

    private void readyStart() throws IOException {
        fileSwapHelper.requestSwapFile(true);
        readyStart(fileSwapHelper.getNextFileName());
    }

    private void readyStart(String filePath) throws IOException {
        isExit = false;
        isVideoTrackAdd = false;
        isAudioTrackAdd = false;
        muxerDatas.clear();

        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        Log.e(TAG, "mediaMuxer init : "+mediaMuxer);
        if (audioEncoderThread != null) {
            audioEncoderThread.setMuxerReady(true);
        }
        if (videoEncoderThread != null) {
            videoEncoderThread.setMuxerReady(true);
        }
        Log.e(TAG, "readyStart 保存至 ： "+filePath);
    }

    public static void addVideoFrameData(byte[] data) {
        if (INSTANCE != null) {
            INSTANCE.addVideoData(data);
        }
    }

    private void addVideoData(byte[] data) {
        if (videoEncoderThread != null) {
            videoEncoderThread.add(data);
        }
    }

    public void addMuxerData(MuxerData data) {
        if (!isMuxerTrackAddDone()) {
            return;
        }

        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 添加视频轨/音轨
     * @param index
     * @param mediaFormat
     */
    public synchronized void addTrackIndex(int index, MediaFormat mediaFormat) {
        Log.e(TAG, "添加音视频轨 判断是否已添加 addTrackIndex index "+index+" ! curThread : "+ Thread.currentThread());
        if (isMuxerTrackAddDone()) {
            Log.e(TAG, "添加音视频轨 判断是否已添加 isMuxerTrackAddDone : "+isMuxerTrackAddDone());
            return;
        }
        //如果已经添加了音视频轨就不做处理了
        if ((index == TRACK_VIDEO && isVideoTrackAdd()) || (index == TRACK_AUDIO && isAudioTrackAdd())) {
            Log.e(TAG, "添加音视频轨 判断是否已添加 isVideoTrackAdd : "+isVideoTrackAdd()+", isAudioTrackAdd : " + isAudioTrackAdd());
            return;
        }
        if (mediaMuxer != null) {
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            } catch (Exception e) {
                Log.e(TAG, "index : "+index+", addTrack 异常 : " + e.toString());
                return;
            }
            if (index == TRACK_VIDEO) {
                videoTrackIndex = track;
                isVideoTrackAdd = true;
                Log.e(TAG, "添加视频轨完成");
            } else {
                audioTrackIndex = track;
                isAudioTrackAdd = true;
                Log.e(TAG, "添加音频轨完成");
            }
            requestStart();
        }
    }

    private void requestStart() {
        synchronized (lock) {
            Log.e(TAG, "requestStart 启动混合器.. : "+isAudioTrackAdd+", "+isVideoTrackAdd);
            if (isMuxerTrackAddDone()) {
                mediaMuxer.start();
                Log.e(TAG, "requestStart 启动混合器..开始等待数据输入...");
                lock.notify();
            }
        }
    }

    /**
     * 当前音视频合成器是否添加了音视频轨
     * @return
     */
    public boolean isMuxerTrackAddDone() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }

    public boolean isAudioTrackAdd() {
        return isAudioTrackAdd;
    }

    public boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    private void initMuxer() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileUtils();
        audioEncoderThread = new AudioEncoderThread(new WeakReference<>(this));
        videoEncoderThread = new VideoEncoderThread(VideoEncoderThread.IMAGE_WIDTH, VideoEncoderThread.IMAGE_HEIGHT, new WeakReference<>(this));
        audioEncoderThread.start();
        videoEncoderThread.start();
        try {
            readyStart();
        } catch (IOException e) {
            Log.e(TAG, "initMuxer 异常 : "+ e.getMessage());
        }
    }

    @Override
    public void run() {
        super.run();

        //
        initMuxer();
        while (!isExit) {
            if (isMuxerTrackAddDone()) {
                if (muxerDatas.isEmpty()) {
                    synchronized (lock) {
                        try {
                            Log.e(TAG, "等待混合数据");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (fileSwapHelper.requestSwapFile()) {
                        //需要切换文件
                        String nextFileName = fileSwapHelper.getNextFileName();
                        Log.e(TAG, "正在重启混合器... "+nextFileName);
                        restart(nextFileName);
                    } else {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        Log.e(TAG, "写入混合数据 "+data.bufferInfo.size);
                        try {
                            mediaMuxer.writeSampleData(track, data.byteBuffer, data.bufferInfo);
                        } catch (Exception e) {
                            Log.e(TAG, "写入混合数据失败 ： "+e.getMessage());
                        }
                    }
                }
            } else {
                synchronized (lock) {
                    try {
                        Log.e(TAG, "等待音视轨添加");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG, "addTrack异常 : "+e.getMessage());
                    }
                }
            }
        }
        readyStop();
        Log.e(TAG, "混合器退出。。。");
    }

    private void restart() {
        fileSwapHelper.requestSwapFile(true);
        String nextFileName = fileSwapHelper.getNextFileName();
        restart(nextFileName);
    }

    private void restart(String fileName) {
        restartAudioVideo();
        readyStop();

        try {
            readyStart(fileName);
        } catch (Exception e) {
            Log.e(TAG, "readyStart 重启混合器失败，尝试再次重启 "+e.getMessage());
            restart();
            return;
        }
        Log.e(TAG, "重启混合器完成");
    }

    private void readyStop() {
        if (mediaMuxer != null) {
            try {
                Field field = mediaMuxer.getClass().getDeclaredField("mState");
                field.setAccessible(true);
                Log.e(TAG, "mediaMuxer stop 状态 ： "+field.get(mediaMuxer));
                mediaMuxer.stop();
            } catch (Exception e) {
                Log.e(TAG, "mediaMuxer stop 异常 ： "+e.getMessage()+", "+e.getCause());
            }
            try {
                mediaMuxer.release();
            } catch (Exception e) {
                Log.e(TAG, "mediaMuxer release 异常 : "+e.getMessage());
            }
            mediaMuxer = null;
        }
    }

    private void restartAudioVideo() {
        if (audioEncoderThread != null) {
            audioTrackIndex = -1;
            isAudioTrackAdd = false;
            audioEncoderThread.restart();
        }
        if (videoEncoderThread != null) {
            videoTrackIndex = -1;
            isVideoTrackAdd = false;
            videoEncoderThread.restart();
        }
    }

    private void exit() {
        if (videoEncoderThread != null) {
            videoEncoderThread.exit();
            try {
                videoEncoderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioEncoderThread != null) {
            audioEncoderThread.exit();
            try {
                audioEncoderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 封装需要传递的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuffer;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuffer = byteBuffer;
            this.bufferInfo = bufferInfo;
        }
    }
}
