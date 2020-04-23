package com.michael.libplayer.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.michael.libplayer.ffmpeg.FFMpegHandle;
import com.michael.libplayer.util.Utils;
import com.michael.libplayer.util.pool.ThreadPoolManager;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2020/4/23
 * @Description: This is
 */
public class FFMPegVideoView extends SurfaceView {

    private SurfaceHolder surfaceHolder;
    private ICallback callback;

    public FFMPegVideoView(Context context) {
        super(context);
        init();
    }

    public FFMPegVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FFMPegVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.surfaceHolder = getHolder();
        this.surfaceHolder.setFormat(PixelFormat.RGBA_8888);
    }

    public void player(String path) {
        if (Utils.isEmpty(path)) return;
        ThreadPoolManager.getInstance().start(() -> {
            Surface surface = this.surfaceHolder.getSurface();
            FFMpegHandle.getInstance().renderFFmpeg(path, surface, Build.VERSION.SDK_INT);
            if (callback != null) {
                callback.onFinish();
            }
        });
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }

    public interface ICallback {
        void onFinish();
        void onResume();
        void onPause();
    }
}
