package com.michael.libplayer.activity;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceHolder;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;
import com.michael.libplayer.opengl.renderer.PlayerGLSurfaceViewRenderer;
import com.michael.libplayer.opengl.view.PlayerCustomGLSurfaceView;
import com.michael.libplayer.util.CameraManager;

public class PlayerGLSurfaceViewPreviewActivity extends BaseActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private PlayerCustomGLSurfaceView glSurfaceView;
    private PlayerGLSurfaceViewRenderer glSurfaceViewRenderer;
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_glsurface_view_preview);

        glSurfaceView = findViewById(R.id.player_gl_sf);
        glSurfaceViewRenderer = new PlayerGLSurfaceViewRenderer();
        cameraManager = new CameraManager();
        glSurfaceView.init(glSurfaceViewRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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
