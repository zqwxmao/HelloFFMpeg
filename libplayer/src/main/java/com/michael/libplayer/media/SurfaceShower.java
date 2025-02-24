package com.michael.libplayer.media;

import android.opengl.EGLSurface;
import android.opengl.GLES20;

import com.michael.libplayer.opengl.base.BaseFilter;
import com.michael.libplayer.opengl.core.IObserver;
import com.michael.libplayer.opengl.core.RenderBean;
import com.michael.libplayer.opengl.filter.LazyFilter;
import com.michael.libplayer.opengl.utils.MatrixUtils;

/**
 * 展示
 */
public class SurfaceShower implements IObserver<RenderBean> {

    private EGLSurface mShowSurface;
    private boolean isShow = false;
    private BaseFilter mFilter;
    private Object mSurface;
    private int mWidth;
    private int mHeight;
    private int mMatrixType = MatrixUtils.TYPE_CENTERCROP;
    private OnDrawEndListener mListener;

    public void setOutputSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    /**
     * 设置输出的Surface
     *
     * @param surface {@link android.view.Surface}、{@link android.graphics.SurfaceTexture}或{@link android.view.TextureView}
     */
    public void setSurface(Object surface) {
        this.mSurface = surface;
    }

    /**
     * 设置矩阵变换类型
     *
     * @param type 变换类型，{@link MatrixUtils#TYPE_FITXY},{@link MatrixUtils#TYPE_FITSTART},{@link MatrixUtils#TYPE_CENTERCROP},{@link MatrixUtils#TYPE_CENTERINSIDE}或{@link MatrixUtils#TYPE_FITEND}
     */
    public void setMatrixType(int type) {
        this.mMatrixType = type;
    }

    public void open() {
        isShow = true;
    }

    public void close() {
        isShow = false;
    }

    @Override
    public void onCall(RenderBean rb) {
        if (rb.endFlag && mShowSurface != null) {
            rb.egl.destroySurface(mShowSurface);
            mShowSurface = null;
        } else if (isShow && mSurface != null) {
            if (mShowSurface == null) {
                mShowSurface = rb.egl.createWindowSurface(mSurface);
                mFilter = new LazyFilter();
                mFilter.create();
                mFilter.sizeChanged(rb.sourceWidth, rb.sourceHeight);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(), mMatrixType, rb.sourceWidth, rb.sourceHeight,
                        mWidth, mHeight);
                MatrixUtils.flip(mFilter.getVertexMatrix(), false, true);
            }
            rb.egl.makeCurrent(mShowSurface);
            GLES20.glViewport(0, 0, mWidth, mHeight);
            mFilter.draw(rb.textureId);
            if (mListener != null) {
                mListener.onDrawEnd(mShowSurface, rb);
            }
            rb.egl.swapBuffers(mShowSurface);
        }
    }

    /**
     * 设置单帧渲染完成监听器
     *
     * @param listener 监听器
     */
    public void setOnDrawEndListener(OnDrawEndListener listener) {
        this.mListener = listener;
    }

    public interface OnDrawEndListener {
        /**
         * 渲染完成通知
         *
         * @param surface 渲染的目标EGLSurface
         * @param bean    渲染用的资源
         */
        void onDrawEnd(EGLSurface surface, RenderBean bean);
    }

}
