package com.michael.libplayer.opengl.renderer;

import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.michael.libplayer.R;
import com.michael.libplayer.media.WrapRenderer;
import com.michael.libplayer.opengl.filter.GroupFilter;
import com.michael.libplayer.opengl.filter.OesFilter;
import com.michael.libplayer.opengl.filter.SixScreenFilter;
import com.michael.libplayer.opengl.filter.WaterMarkFilter;
import com.michael.libplayer.opengl.utils.MatrixUtils;
import com.michael.libplayer.opengl.view.PlayerCustomGLSurfaceView;
import com.michael.libplayer.util.CameraManager;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.michael.libplayer.media.WrapRenderer.TYPE_ORIGINAL;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/17
 * @Description: This is
 */
public class PlayerGLSurfaceViewRenderer implements GLSurfaceView.Renderer {

    private CameraManager cameraManager;
    private WeakReference<PlayerCustomGLSurfaceView> playerCustomGLSurfaceView;
    private boolean isPreviewStarted;

    //opengl
    private int mOESTextureId = -1;
    private GroupFilter groupFilter;
    private WaterMarkFilter waterMarkFilter;//水印滤镜

    private WrapRenderer wrapRenderer;
    private SurfaceTexture surfaceTexture;
    private float[] surfaceTextureMatrix = new float[16];
    private int typeIndex = -1;

    public PlayerGLSurfaceViewRenderer(int typeIndex) {
        this.typeIndex = typeIndex;
    }

    public void init(CameraManager cameraManager, PlayerCustomGLSurfaceView playerCustomGLSurfaceView, boolean isPreviewStarted) {
        this.cameraManager = cameraManager;
        this.playerCustomGLSurfaceView = new WeakReference<>(playerCustomGLSurfaceView);
        this.isPreviewStarted = isPreviewStarted;
        waterMarkFilter = new WaterMarkFilter().setMarkPosition(100, 100, 300, 300).setMark(BitmapFactory.decodeResource(playerCustomGLSurfaceView.getResources(), R.mipmap.ic_launcher_round));
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mOESTextureId = createTextureID(true);
        if (groupFilter == null) {
            groupFilter = new GroupFilter();
            groupFilter.addFilter(waterMarkFilter);
        }
        if (wrapRenderer == null) {
            wrapRenderer = new WrapRenderer(groupFilter, this.typeIndex);
        }
        wrapRenderer.create();
        wrapRenderer.setFlag(TYPE_ORIGINAL);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (wrapRenderer != null) {
            wrapRenderer.sizeChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(surfaceTextureMatrix);
        }
        if (!isPreviewStarted) {
            isPreviewStarted = true;
            initSurfaceTexture();
            return;
        }
        wrapRenderer.setTextureMatrix(surfaceTextureMatrix);
        wrapRenderer.draw(mOESTextureId);
//        groupFilter.draw(oesFilter.drawToTexture(mOESTextureId));
    }

    private boolean initSurfaceTexture() {
        if (cameraManager == null || playerCustomGLSurfaceView == null) {
            return false;
        }
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        surfaceTexture.setOnFrameAvailableListener(surfaceTexture1 -> {
            if (playerCustomGLSurfaceView.get() != null) {
                playerCustomGLSurfaceView.get().requestRender();
            }
        });
        cameraManager.setPreviewTexture(surfaceTexture);
        cameraManager.startPreview();
        return true;
    }

    /*
     * 返回一个纹理句柄，拿到这个纹理句柄后，就可以对它进行操作
     * */
    public static int createTextureID(boolean isOes) {
        int target= GLES20.GL_TEXTURE_2D;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            target = isOes? GLES11Ext.GL_TEXTURE_EXTERNAL_OES: GLES20.GL_TEXTURE_2D;
        }
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(target, texture[0]);
        GLES20.glTexParameterf(target,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(target,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    public void setBrightLevel(float brightLevel) {
        wrapRenderer.setBrightLevel(brightLevel);
    }

    public void setBeautyLevel(float beautyLevel) {
        wrapRenderer.setBeautyLevel(beautyLevel);
    }

    public void setToneLevel(float toneLevel) {
        wrapRenderer.setToneLevel(toneLevel);
    }

    public void setTexelOffset(float texelOffset) {
        wrapRenderer.setTexelOffset(texelOffset);
    }
}
