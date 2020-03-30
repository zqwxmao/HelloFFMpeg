package com.michael.libplayer.activity;

import android.Manifest;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;
import com.michael.libplayer.media.MediaMuxerThread;
import com.michael.libplayer.media.MediaRtmpEncoder;
import com.michael.libplayer.media.VideoEncoderThread;
import com.michael.libplayer.util.CameraUtils;
import com.michael.libplayer.util.YUVUtils;

import java.io.IOException;
import java.util.List;

public class PlayerCameraRecordMuxerActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnClickListener {

    public static final String TAG = PlayerCameraRecordMuxerActivity.class.getSimpleName() + "/ ";

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    Button btnStartStop;
    Button btnStartPublishRtmp;
    Camera camera;

    private MediaRtmpEncoder mediaRtmpEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_camera_record_muxer);

        surfaceView = findViewById(R.id.surface);
        btnStartStop = findViewById(R.id.btnStartRecord);
        btnStartPublishRtmp= findViewById(R.id.btnStartPublishRtmp);
        btnStartStop.setOnClickListener(this::onClick);
        btnStartPublishRtmp.setOnClickListener(this::onClick);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    protected String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
        };
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        MediaMuxerThread.addVideoFrameData(YUVUtils.rotateYUVDegree270(data,VideoEncoderThread.IMAGE_WIDTH, VideoEncoderThread.IMAGE_HEIGHT));
        MediaMuxerThread.addVideoFrameData(data);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        MediaMuxerThread.stopMuxer();
        stopCamera();
    }

    private void startCamera() {
        camera = Camera.open(cameraId);
        camera.setDisplayOrientation(CameraUtils.getDisplayOrientation(cameraId, this));
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        int length = previewSizes.size();
        for (int i = 0; i < length; i++) {
            Log.e("zqwx", "SupportedPreviewSizes : " + previewSizes.get(i).width + "x" + previewSizes.get(i).height);
        }
        Log.e("zqwx", "surfaceView : " + surfaceView.getWidth()+", height : "+surfaceView.getHeight());
        /*if (length > 0) {
            if (length > 1) {
                boolean first = previewSizes.get(0).width > previewSizes.get(length-1).width;
                VideoEncoderThread.IMAGE_WIDTH = first ? previewSizes.get(0).width : previewSizes.get(length-1).width;
                VideoEncoderThread.IMAGE_HEIGHT = first ? previewSizes.get(0).height : previewSizes.get(length-1).height;
            } else {
                VideoEncoderThread.IMAGE_WIDTH = previewSizes.get(0).width;
                VideoEncoderThread.IMAGE_HEIGHT = previewSizes.get(0).height;
            }
        }
        VideoEncoderThread.IMAGE_WIDTH = surfaceView.getHeight();
        VideoEncoderThread.IMAGE_HEIGHT = surfaceView.getWidth();*/
        //这个宽高的设置和后面编解码设置一样， 否则不能正常处理
        parameters.setPreviewSize(VideoEncoderThread.IMAGE_WIDTH, VideoEncoderThread.IMAGE_HEIGHT);

        try {
            camera.setParameters(parameters);
            camera.setPreviewCallback(this);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void stopCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnStartRecord) {
            if (v.getTag() != null && v.getTag().toString().equalsIgnoreCase("stop")) {
                v.setTag("start");
                btnStartStop.setText(R.string.player_start_record_video);
                stopCamera();
                MediaMuxerThread.stopMuxer();
            } else {
                v.setTag("stop");
                btnStartStop.setText(R.string.player_stop_record_video);
                startCamera();
                MediaMuxerThread.startMuxer();
            }
        } else if (id == R.id.btnStartPublishRtmp) {
            if (v.getTag() != null && v.getTag().toString().equalsIgnoreCase("stop")) {
                v.setTag("start");
                btnStartPublishRtmp.setText(R.string.player_start_publish_rtmp);
                stopCamera();
                MediaMuxerThread.stopMuxer();
                mediaRtmpEncoder.stop();
            } else {
                v.setTag("stop");
                btnStartPublishRtmp.setText(R.string.player_stop_publish_rtmp);
                startCamera();
                if (mediaRtmpEncoder == null){
                    mediaRtmpEncoder = new MediaRtmpEncoder();
                }
                MediaMuxerThread.startMuxer(false, new MediaMuxerThread.ICallback() {
                    @Override
                    public void onWriteSampleData(MediaMuxerThread.MuxerData muxerData) {
                        if (muxerData != null) {
                            if (muxerData.getTrackIndex() == MediaMuxerThread.TRACK_VIDEO) {
                                mediaRtmpEncoder.addVideoData(muxerData);
                            } else if (muxerData.getTrackIndex() == MediaMuxerThread.TRACK_AUDIO) {
                                mediaRtmpEncoder.addAudioData(muxerData);
                            }
                        }
                    }
                });
                mediaRtmpEncoder.start();
            }
        }
    }
}
