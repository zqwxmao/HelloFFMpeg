package com.michael.libplayer.ffmpeg;

import android.view.Surface;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/11/18
 * @Description: This is
 */
public class FFMpegHandle {

    private static volatile FFMpegHandle INTANCE;

    public static FFMpegHandle getInstance() {
        if (INTANCE == null) {
            synchronized (FFMpegHandle.class) {
                if (INTANCE == null) {
                    INTANCE = new FFMpegHandle();
                }
            }
        }
        return INTANCE;
    }

    static {
        System.loadLibrary("ffmpeg-handle");
    }

    public native void pushRtmpFile(String path);
    public native void pushFFMpegFile(String rtmpURL, String filePath);
    public native void renderFFmpeg(String path, Surface surface, int version);
    public native int connect(String url, int minWidth, int maxWidth, int timeOut);
    public native int close();
    public native int sendVideoSpec(byte[] sps, int spsLen, byte[] pps, int ppsLen, long timeStamps);
    public native int sendVideoData(byte[] frame, int frameLen, long timeStamps);
    public native int sendAudioSpec(byte[] aacSpec, int aacSpecLen);
    public native int sendAudioData(byte[] aacData, int aacDataLen, long timeStamps);
}
