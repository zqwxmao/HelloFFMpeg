package com.michael.libplayer.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.michael.libplayer.activity.PlayerCameraRecordMuxerActivity;
import com.michael.libplayer.util.YUVUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoEncoderThread extends AbstractMediaEncoderThread {

    private static final String TAG = PlayerCameraRecordMuxerActivity.TAG + VideoEncoderThread.class.getSimpleName();
    public static int IMAGE_HEIGHT = 720;
    public static int IMAGE_WIDTH = 1280;

    //编码相关参数
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;     //H.264 Advanced Video
    private static final int FRAME_RATE = 30;       //帧率
    private static final int IFRAME_INTERVAL = 10;      //I帧间隔(GOP)
    private static final int TIMEOUT_USEC = 10000;      //编码超时时间
    private static final int COMPRESS_RATIO = 256;
    private static int BIT_RATE = 480000;/* = IMAGE_HEIGHT * IMAGE_WIDTH * 3 * 8 * FRAME_RATE / COMPRESS_RATIO;*/       //bit rate CameraWrapper.

    //视频宽高参数
    private int width;
    private int height;

    //存储每一帧的数据Vector自增数组
    private LinkedBlockingQueue<byte[]> frameBytes;
    private byte[] frameData;

    private final Object lock = new Object();

    private MediaCodecInfo codecInfo;
    private MediaCodec videoCodec;      //Android硬解解码器
    private MediaCodec.BufferInfo bufferInfo;   //编解码Buffer相关信息

    private MediaFormat mediaFormat;        //音视频格式

    private volatile boolean isStart = false;
    private volatile boolean isExit = false;
    private volatile boolean isMuxerReady = false;
    private long prevOutputPTSUs = 0L;

    private ICallback callback;

    public VideoEncoderThread(int width, int height, boolean isMuxed) {
        super(isMuxed);
        this.width = width;
        this.height = height;
        this.frameBytes = new LinkedBlockingQueue<byte[]>();
        prepare();
    }

    private void prepare() {
        frameData = new byte[width * height * 3 / 2];
        bufferInfo = new MediaCodec.BufferInfo();
        codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            Log.e(TAG, "unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        Log.i(TAG, "selected video codec : "+codecInfo.getName());
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, IMAGE_HEIGHT * IMAGE_WIDTH * 3 * 8 * FRAME_RATE / COMPRESS_RATIO);
        // 调整码率的控流模式
//        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        int colorFormat = getColorFormat();
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
//        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        Log.i(TAG, "video formate : "+mediaFormat);
    }

    private int getColorFormat() {
        int colorFormats[] = codecInfo.getCapabilitiesForType(MIME_TYPE).colorFormats;
        int colorFormat = 0;

        outer :
        for (int i = 0; i < colorFormats.length; i++) {
            int format = colorFormats[i];
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    Log.d(TAG, "supported color format COLOR_FormatYUV420Planar:" + format);
                    colorFormat = format;
                    break outer;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    Log.d(TAG, "supported color format COLOR_FormatYUV420SemiPlanar:" + format);
                    colorFormat = format;
                    break outer;

                default:
                    Log.d(TAG, "other color format " + format);
            }
        }
        return colorFormat;
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo mediaCodecInfo = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (!info.isEncoder()) {
                continue;
            }
            String[] types = info.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                Log.i(TAG, "supportedType:" + info.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (mediaCodecInfo == null) {
                        mediaCodecInfo = info;
                        break;
                    }
                }
            }
        }
        return mediaCodecInfo;
    }

    private void startMediaCodec() throws IOException {
        videoCodec = MediaCodec.createByCodecName(codecInfo.getName());
        videoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        videoCodec.start();
        isStart = true;
    }

    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock) {
            Log.e(TAG, Thread.currentThread().getId() + " video - - setMuxerReady ... "+muxerReady);
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    public void add(byte[] data) {
        if (frameBytes != null && isMuxerReady) {
            frameBytes.add(data);
        }
    }

    public synchronized void restart() {
        isStart = false;
        isMuxerReady = false;
        frameBytes.clear();
    }

    public void exit() {
        isExit = true;
    }

    public void stopMuxerCodec() {
        if (videoCodec != null) {
            videoCodec.stop();
            videoCodec.release();
            videoCodec = null;
        }
        isStart = false;
    }

    @Override
    public void run() {
        while (!isExit) {
            if (!isStart) {
                stopMuxerCodec();

                if (!isMuxerReady) {
                    synchronized (lock) {
                        try {
                            Log.e(TAG, "video -- 等待混合器准备...");
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                if (isMuxerReady) {
                    try {
                        Log.e(TAG, "video -- startMediaCodec...");
                        startMediaCodec();
                    } catch (IOException e) {
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } else if (!frameBytes.isEmpty()) {
                try {
                    byte[] bytes = this.frameBytes.take();
                    Log.e(TAG, "解码视频数据： "+ (bytes!=null ? bytes.length : "null"));
                    encodeFrame(bytes);
                } catch (InterruptedException e) {
                    Log.e(TAG, "缓存读取视频数据出错： "+e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "解码视频数据出错： "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "Video record thread exit");
    }

    /**
     * 编码每一帧的数据
     * @param input
     */
    private void encodeFrame(byte[] input) {
        Log.i(TAG, "encodeFrame");

        switch (mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                YUVUtils.NV21ToYU12(input, frameData, this.width, this.height);
                break;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                YUVUtils.NV21toI420SemiPlanar(input, frameData, this.width, this.height);
                break;
        }

        int inputBufferIndex = videoCodec.dequeueInputBuffer(TIMEOUT_USEC);
        Log.i(TAG, "encodeFrame inputBufferIndex : "+inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(frameData);
            videoCodec.queueInputBuffer(inputBufferIndex, 0, frameData.length, getPTSUs(), 0);
        } else {

        }

        int outputBufferIndex;
        ByteBuffer[] outputBuffers = videoCodec.getOutputBuffers();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            outputBuffers = videoCodec.getOutputBuffers();
        }
        do {
            outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            Log.e(TAG, "发送视频数据 outputBufferIndex : "+outputBufferIndex);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = videoCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = videoCodec.getOutputFormat();
                if (this.callback != null) {
                    this.callback.onInfoFormatChanged(newFormat);
                }
            } else if (outputBufferIndex < 0) {

            } else {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    videoCodec.releaseOutputBuffer(outputBufferIndex,false);
                    break;
                }

                ByteBuffer outputBuffer;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = outputBuffers[outputBufferIndex];
                } else {
                    outputBuffer = videoCodec.getOutputBuffer(outputBufferIndex);
                }
                if (outputBuffer == null) {
                    Log.e(TAG, "encoderOutputBuffer "+outputBufferIndex + "was null ");
                    continue;
                }

                if (super.isMuxed && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    MediaFormat format = videoCodec.getOutputFormat();
                    format.setByteBuffer("csd-0",outputBuffer);
                    bufferInfo.size = 0;
                    Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                }
                if (bufferInfo.size != 0) {

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        //adjust the ByteBuffer values to match BufferInfo
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    }
                    if (this.callback != null) {
                        bufferInfo.presentationTimeUs = getPTSUs();
                        this.callback.onOutputVideoData(outputBuffer, bufferInfo);
                    }

                    Log.d(TAG, "sent "+bufferInfo.size + "frameBytes to muxer");
                }
                videoCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } while (outputBufferIndex >= 0);
        Log.i(TAG, "encodeFrame END ! ");
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return*/

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }

    public interface ICallback {
         void onInfoFormatChanged(MediaFormat newFormat);
         void onOutputVideoData(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
    }
}
