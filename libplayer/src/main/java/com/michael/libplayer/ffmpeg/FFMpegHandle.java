package com.michael.libplayer.ffmpeg;

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
}
