package com.michael.libplayer.activity;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;
import com.michael.libplayer.opengl.renderer.PlayerGLSurfaceViewRenderer;
import com.michael.libplayer.opengl.view.PlayerCustomGLSurfaceView;
import com.michael.libplayer.util.CameraManager;

public class PlayerGLSurfaceViewPreviewActivity extends BaseActivity {

    private PlayerCustomGLSurfaceView glSurfaceView;
    private PlayerGLSurfaceViewRenderer glSurfaceViewRenderer;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_glsurface_view_preview);

        glSurfaceView = new PlayerCustomGLSurfaceView(this);
//        glSurfaceView = findViewById(R.id.player_gl_sf);
        cameraManager = new CameraManager();

        glSurfaceViewRenderer = new PlayerGLSurfaceViewRenderer();
        glSurfaceView.init(glSurfaceViewRenderer, cameraManager, false);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        FrameLayout frameLayout = findViewById(R.id.fl_glsurface);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.player_dimen_80);
        layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.player_dimen_80);
        frameLayout.addView(glSurfaceView, layoutParams);

        cameraManager.setActivityWeakReference(this);
        boolean openCamera = cameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        Log.e("zqwx", "- "+openCamera);
        if (openCamera) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraManager.stopPreview();
        cameraManager.releaseCamera();
    }
}
