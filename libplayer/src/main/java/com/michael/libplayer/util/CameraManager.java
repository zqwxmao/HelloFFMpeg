package com.michael.libplayer.util;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @Author: zhangqiaowenxiang
 * @Time: 2019/12/17
 * @Description: This is
 */
public class CameraManager {

    private WeakReference<Activity> activityWeakReference;
    private Camera camera;
    private int cameraId;
    private Camera.Size cameraSize;

    public void setActivityWeakReference(Activity activityWeakReference) {
        this.activityWeakReference = new WeakReference<>(activityWeakReference);
    }

    public boolean openCamera(int cameraId) {
        try {
            this.cameraId = cameraId;
            this.camera = Camera.open(cameraId);
//            this.camera.setDisplayOrientation(90);
            setCameraDisplayOrientation(activityWeakReference.get(), cameraId, this.camera);
            setPreviewCameraSize();
        } catch (Exception e) {
            Log.e("zqwx", "openCamera - "+e.getMessage());
            return false;
        }
        return true;
    }

    private void setCameraDisplayOrientation(Activity activity,
              int cameraId, android.hardware.Camera camera) {
          android.hardware.Camera.CameraInfo info =
                  new android.hardware.Camera.CameraInfo();
          android.hardware.Camera.getCameraInfo(cameraId, info);
          int rotation = activity.getWindowManager().getDefaultDisplay()
                             .getRotation();
          int degrees = 0;
          switch (rotation) {
              case Surface.ROTATION_0: degrees = 0; break;
              case Surface.ROTATION_90: degrees = 90; break;
              case Surface.ROTATION_180: degrees = 180; break;
              case Surface.ROTATION_270: degrees = 270; break;
          }

          int result;
          if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
              result = (info.orientation + degrees) % 360;
              result = (360 - result) % 360;  // compensate the mirror
          } else {  // back-facing
              result = (info.orientation - degrees + 360) % 360;
          }
          camera.setDisplayOrientation(result);
      }

    /*
     * 开启预览，是在GLSurfaceView创建成功后调用
     * */
    public void startPreview() {
        if (camera != null) {
            camera.startPreview();
        }
    }

    /*
     * 屏幕失去焦点后，停止预览，避免资源浪费
     * */
    public void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    /*
     * 退出应用时，释放Camera
     * */
    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void setPreviewCameraSize() {
        Camera.Parameters parameters = camera.getParameters();

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        int length = previewSizes.size();
        int last = 0, tmp;
        for (int i = 0; i < length; i++) {
            tmp = previewSizes.get(i).width * previewSizes.get(i).height;
            if (last < tmp) {
                last = tmp;
                this.cameraSize = previewSizes.get(i);
            }
        }
        Log.e("zqwx", this.cameraSize.width +" - "+this.cameraSize.height);
        parameters.setPreviewSize(this.cameraSize.width, this.cameraSize.height);
        parameters.setPreviewFormat(ImageFormat.NV21);
        this.camera.setParameters(parameters);
    }

    /*
     * 将SurfaceTexture与Camera绑定
     * 这样Camera的输出数据，就可以显示在SurfaceTexture上面
     * 而STexture是通过GLSurfaceView创建的，这样GLSView就可以操控STexture的数据了
     * 通过对STexture上数据的处理，可以实现滤镜功能，当然也可以实现我们需要的方形预览
     *
     *
     * */
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (camera != null) {
            try {
                camera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setPreviewSurfaceHolder(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback cb) {
        if (camera != null) {
            camera.setPreviewCallback(cb);
        }
    }

}
