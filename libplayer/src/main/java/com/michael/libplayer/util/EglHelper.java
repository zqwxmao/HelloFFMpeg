package com.michael.libplayer.util;

import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/17
 * @Description: This is
 */
public class EglHelper {

    private EGL10 mEgl;
    //默认的显示设备
    private EGLDisplay mEglDisplay;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    public void init(Surface surface, EGLContext eglContext) {
        //得到EGL显示实例
        mEgl = (EGL10) EGLContext.getEGL();
        //得到默认的显示设备 就是窗口
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throwException("eglGetDisplay failed");
        }

        //初始化默认显示设备
        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throwException("eglInitialize failed");
        }

        //设备显示的属性
        int[] attributes = new int[] {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 8,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
        };

        int[] numConfig = new int[1];
        if (!mEgl.eglChooseConfig(mEglDisplay, attributes, null, 1, numConfig)) {
            throwException("eglChooseConfig failed");
        }

        //从系统中获取对应属性的配置
        int numConfigs = numConfig[0];
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!mEgl.eglChooseConfig(mEglDisplay, attributes, configs, numConfigs, numConfig)) {
            throwException("eglChooseConfig#2 failed");
        }

        //创建EGLContext
        if (eglContext != null) {
            mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], eglContext, numConfig);
        } else {
            mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, null);
        }

        //创建渲染的Surface
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, configs[0], surface, null);

        //绑定EglContext和Surface到设备中
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throwException("eglMakeCurrent failed");
        }
    }

    public boolean swapBuffers() {
        if (mEgl != null) {
            return mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
        } else {
            throw new RuntimeException("egl is null");
        }
    }

    public EGLContext getEglContext() {
        return mEglContext;
    }

    public void destroyEgl() {
        if (mEgl != null) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = null;

            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = null;

            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
            mEgl = null;
        }
    }

    private void throwException(String str) {
        throw new RuntimeException(str);
    }
}
