package com.michael.libplayer.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;


import com.michael.libplayer.activity.PlayerCameraRecordMuxerActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class AudioEncoderThread extends Thread {

    private static final String TAG = PlayerCameraRecordMuxerActivity.TAG + AudioEncoderThread.class.getSimpleName();

    public static final int SAMPLES_PER_FRAME = 1024;
    public static final int FRAMES_PER_BUFFER = 25;
    private static final int TIMEOUT_USEC = 10000;
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    /**
     * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
     */
    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_RATE = 64000;
    private static final int[] AUDIO_SOURCES = new int[]{MediaRecorder.AudioSource.DEFAULT};

    private final Object lock = new Object();
    private MediaCodec audioCodec;      // API >= 16(Android 4.1.2)
    private MediaCodec.BufferInfo bufferInfo;       // API >= 16(Android 4.1.2)
    private volatile boolean isExit = false;
    private volatile boolean isStart = false;
    private volatile boolean isMuxerReady = false;
    private WeakReference<MediaMuxerThread> mediaMuxerRunnable;
    private AudioRecord audioRecord;
    private long prevOutputPTSUs = 0L;
    private MediaFormat audioFormat;

    public AudioEncoderThread(WeakReference<MediaMuxerThread> reference) {
        this.mediaMuxerRunnable = reference;
        this.bufferInfo = new MediaCodec.BufferInfo();
        prepare();
    }

    private void prepare() {
        MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for "+MIME_TYPE);
            return;
        }
        Log.i(TAG, "selected audio codec : "+audioCodecInfo.getName());
        audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        Log.i(TAG, "audio formate : "+audioFormat);
    }

    private MediaCodecInfo selectAudioCodec(final String mimeType) {
        MediaCodecInfo result = null;
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j ++) {
                Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void startMediaCodec() throws IOException {
        audioCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioCodec.start();

        prepareAudioRecord();
        isStart = true;
    }

    private void stopMediaCodec() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        if (audioCodec != null) {
            audioCodec.stop();
            audioCodec.release();
            audioCodec = null;
        }
        isStart = false;
    }

    private void prepareAudioRecord() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
            if (bufferSize < minBufferSize) {
                bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
            }
            audioRecord = null;
            for (final int source : AUDIO_SOURCES) {
                try {
                    audioRecord = new AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                        audioRecord = null;
                    }
                } catch (Exception e) {
                    audioRecord = null;
                }
                if (audioRecord != null) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "AudioThread#run : ", e);
        }

        if (audioRecord != null) {
            audioRecord.startRecording();
        }
    }

    public synchronized void restart() {
        isStart = false;
        isMuxerReady = false;
    }

    public void exit() {
        isExit = true;
    }

    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock) {
            Log.i(TAG, Thread.currentThread().getId() + " audio - - setMuxerReady... "+muxerReady);
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    @Override
    public void run() {
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
        int readBytes;
        while (!isExit) {
            if (!isStart) {
                stopMediaCodec();

                Log.e(TAG, Thread.currentThread().getId() + " audio - - run... "+isMuxerReady);

                if (!isMuxerReady) {
                    synchronized (lock) {
                        try {
                            Log.e(TAG, "audio -- wait for muxer ready...");
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                if (isMuxerReady) {
                    try {
                        Log.e(TAG, "audio - - startmediacodec...");
                        startMediaCodec();
                    } catch (IOException e) {
                        e.printStackTrace();
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } else if (audioRecord != null) {
                byteBuffer.clear();
                readBytes = audioRecord.read(byteBuffer, SAMPLES_PER_FRAME);
                if (readBytes > 0) {
                    // set audio data to encoder
                    byteBuffer.position(readBytes);
                    byteBuffer.flip();
                    Log.e(TAG, "encode audio data : "+readBytes);
                    try {
                        encode(byteBuffer, readBytes, getPTSUs());
                    } catch (Exception e) {
                        Log.e(TAG, "encode audio data failed");
                        e.printStackTrace();
                    }
                }
            }
        }
        Log.i(TAG, "Audio record thread exit");
    }

    private void encode(final ByteBuffer byteBuffer, final int length, final long presentationTimeUs) {
        if (isExit) return;
        final ByteBuffer[] inputBuffers = audioCodec.getInputBuffers();
        final int inputBufferIndex = audioCodec.dequeueInputBuffer(TIMEOUT_USEC);
        //向编码器输入数据
        if (inputBufferIndex >= 0) {
            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            if (byteBuffer != null) {
                inputBuffer.put(byteBuffer);
            }
            if (length <= 0) {
                audioCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                audioCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
            }
        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // wait for MediaCodec encoder is ready to encode
            // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
            // will wait for maximum TIMEOUT_USEC(10msec) on each call
        }

        //获取解码后的数据
        final MediaMuxerThread muxer = mediaMuxerRunnable.get();
        if (muxer == null) {
            Log.i(TAG, "MediaMuxerRunnable is unexpectedly null");
            return;
        }
        ByteBuffer[] encoderOutputBuffers = audioCodec.getOutputBuffers();
        int encoderStatus;

        do {
            encoderStatus = audioCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = audioCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                final MediaFormat format = audioCodec.getOutputFormat(); // API >= 16
                MediaMuxerThread mediaMuxerRunnable = this.mediaMuxerRunnable.get();
                if (mediaMuxerRunnable == null) {
                    Log.e(TAG, "添加音轨 INFO_OUTPUT_FORMAT_CHANGED : "+format.toString());
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerThread.TRACK_AUDIO, format);
                }
            } else if (encoderStatus < 0) {
                Log.e(TAG, "encoderStatus < 0");
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if ( (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0 && muxer != null && muxer.isMuxerTrackAddDone()) {
                    bufferInfo.presentationTimeUs = getPTSUs();
                    Log.e(TAG, "发送音频数据 "+bufferInfo.size);
                    muxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO, encodedData, bufferInfo));
                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                }
                audioCodec.releaseOutputBuffer(encoderStatus, false);
            }
        } while (encoderStatus >= 0);
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }
}
