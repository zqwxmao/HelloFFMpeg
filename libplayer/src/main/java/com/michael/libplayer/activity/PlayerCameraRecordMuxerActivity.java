package com.michael.libplayer.activity;

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
import com.michael.libplayer.media.VideoEncoderThread;

import java.io.IOException;
import java.util.List;

public class PlayerCameraRecordMuxerActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = PlayerCameraRecordMuxerActivity.class.getSimpleName() + "/ ";

    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    Button btnStartStop;
    TextView textView;
    Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_camera_record_muxer);

        surfaceView = findViewById(R.id.surface);
        btnStartStop = findViewById(R.id.btn);
        textView = findViewById(R.id.text);
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() != null && v.getTag().toString().equalsIgnoreCase("stop")) {
                    v.setTag("start");
                    btnStartStop.setText("start");
                    stopCamera();
                    MediaMuxerThread.stopMuxer();
                } else {
                    v.setTag("stop");
                    btnStartStop.setText("stop");
                    startCamera();
                    MediaMuxerThread.startMuxer();
                }
            }
        });

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
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
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        int length = previewSizes.size();
        for (int i = 0; i < length; i++) {
            Log.e("zqwx", "SupportedPreviewSizes : " + previewSizes.get(i).width + "x" + previewSizes.get(i).height);
        }
        if (length > 0) {
            VideoEncoderThread.IMAGE_WIDTH = previewSizes.get(0).width;
            VideoEncoderThread.IMAGE_HEIGHT = previewSizes.get(0).height;
        }
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
}
