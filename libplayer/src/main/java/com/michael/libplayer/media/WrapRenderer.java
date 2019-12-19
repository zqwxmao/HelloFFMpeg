package com.michael.libplayer.media;


import android.opengl.GLES20;

import com.michael.libplayer.opengl.core.Renderer;
import com.michael.libplayer.opengl.filter.OesFilter;
import com.michael.libplayer.opengl.utils.MatrixUtils;

/**
 * WrapRenderer 用于包装其他Filter渲染OES纹理
 */
public class WrapRenderer implements Renderer {

    private Renderer mRenderer;
    private OesFilter mFilter;

    public static final int TYPE_MOVE = 0;
    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_ORIGINAL = 2;

    public WrapRenderer(Renderer renderer) {
        this.mRenderer = renderer;
        setFlag(TYPE_MOVE);
    }

    public WrapRenderer(Renderer renderer, int typeIndex) {
        this.mRenderer = renderer;
        mFilter = new OesFilter(typeIndex);
        setFlag(TYPE_MOVE);
    }

    public void setFlag(int flag) {
        if (flag == TYPE_MOVE) {
            mFilter.setVertexCo(new float[]{
                    -1.0f, 1.0f,
                    -1.0f, -1.0f,
                    1.0f, 1.0f,
                    1.0f, -1.0f,
            });
        } else if (flag == TYPE_CAMERA) {
            mFilter.setVertexCo(new float[]{
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, 1.0f,
            });
        } else if (flag == TYPE_ORIGINAL) {
            mFilter.setVertexCo(MatrixUtils.getOriginalVertexCo());
        }
    }

    public float[] getTextureMatrix() {
        return mFilter.getTextureMatrix();
    }

    public void setTextureMatrix(float[] matrix) {
        this.mFilter.setTextureMatrix(matrix);
    }

    @Override
    public void create() {
        mFilter.create();
        if (mRenderer != null) {
            mRenderer.create();
        }
    }

    @Override
    public void sizeChanged(int width, int height) {
        mFilter.sizeChanged(width, height);
        if (mRenderer != null) {
            mRenderer.sizeChanged(width, height);
        }
    }

    @Override
    public void draw(int texture) {
        if (mRenderer != null) {
            mRenderer.draw(mFilter.drawToTexture(texture));
        } else {
            mFilter.draw(texture);
        }
    }

    @Override
    public void destroy() {
        if (mRenderer != null) {
            mRenderer.destroy();
        }
        mFilter.destroy();
    }

    public void setBrightLevel(float brightLevel) {
        mFilter.setBrightLevel(brightLevel);
    }

    public void setBeautyLevel(float beautyLevel) {
        mFilter.setBeautyLevel(beautyLevel);
    }

    public void setToneLevel(float toneLevel) {
        mFilter.setToneLevel(toneLevel);
    }

    public void setTexelOffset(float texelOffset) {
        mFilter.setTexelOffset(texelOffset);
    }
}