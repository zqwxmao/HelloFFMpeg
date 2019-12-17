package com.michael.libplayer.activity;

import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;
import com.michael.libplayer.util.CameraManager;

public class PlayerOpenGLPreviewActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_player_open_glpreview);

        surfaceView = findViewById(R.id.player_sf);
        surfaceView.getHolder().addCallback(this);
        cameraManager = new CameraManager();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.surfaceHolder = holder;
        cameraManager.setWidth(width);
        cameraManager.setHeight(height);
        cameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        cameraManager.setPreviewSurfaceHolder(surfaceHolder);
        cameraManager.setPreviewCallback(this);
        cameraManager.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.surfaceHolder = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
