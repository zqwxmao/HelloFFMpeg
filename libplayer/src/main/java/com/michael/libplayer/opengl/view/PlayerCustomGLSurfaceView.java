package com.michael.libplayer.opengl.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.michael.libplayer.util.CameraManager;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/17
 * @Description: This is
 */
public class PlayerCustomGLSurfaceView extends GLSurfaceView {

    private Renderer renderer;
    private CameraManager cameraManager;

    public PlayerCustomGLSurfaceView(Context context) {
        super(context);
    }

    public PlayerCustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Renderer renderer, CameraManager cameraManager) {
        this.renderer = renderer;
        this.cameraManager = cameraManager;
        setEGLContextClientVersion(2);
        setRenderer(this.renderer);
    }
}
