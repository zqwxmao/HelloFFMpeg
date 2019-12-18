package com.michael.libplayer.opengl.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.michael.libplayer.opengl.renderer.PlayerGLSurfaceViewRenderer;
import com.michael.libplayer.util.CameraManager;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/17
 * @Description: This is
 */
public class PlayerCustomGLSurfaceView extends GLSurfaceView {

    private PlayerGLSurfaceViewRenderer renderer;

    public PlayerCustomGLSurfaceView(Context context) {
        super(context);
    }

    public PlayerCustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(PlayerGLSurfaceViewRenderer renderer, CameraManager cameraManager, boolean isPreviewStarted) {
        this.renderer = renderer;
        setEGLContextClientVersion(2);
        this.renderer.init(cameraManager, this, isPreviewStarted);
        setRenderer(this.renderer);
    }
}
