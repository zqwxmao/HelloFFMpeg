package com.michael.libplayer.activity;

import android.Manifest;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;
import com.michael.libplayer.opengl.renderer.PlayerGLSurfaceViewRenderer;
import com.michael.libplayer.opengl.view.PlayerCustomGLSurfaceView;
import com.michael.libplayer.util.CameraManager;
import com.mj.permission.DynamicPermissionEmitter;
import com.mj.permission.DynamicPermissionEntity;

import java.util.Map;

public class PlayerGLSurfaceViewPreviewActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private PlayerCustomGLSurfaceView glSurfaceView;
    private PlayerGLSurfaceViewRenderer glSurfaceViewRenderer;
    private CameraManager cameraManager;

    private RadioButton rb0;
    private RadioButton rb1;
    private RadioButton rb2;
    private RadioButton rb3;
    private RadioButton rb4;
    private LinearLayout llSb;
    private SeekBar sbStep;
    private SeekBar sbTone;
    private SeekBar sbBeauty;
    private SeekBar sbBright;
    private SeekBar sbOpacity;

    private boolean modelChoosed;
    private static float minstepoffset= -10;
    private static float maxstepoffset= 10;
    private static float minToneValue= -5;
    private static float maxToneValue= 5;
    private static float minbeautyValue= 0;
    private static float maxbeautyValue= 2.5f;
    private static float minbrightValue= 0;
    private static float maxbrightValue= 1;
    private static float minOpacityValue= 0.5f;
    private static float maxOpacityValue= 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_glsurface_view_preview);

        rb0 = findViewById(R.id.rb0);
        rb1 = findViewById(R.id.rb1);
        rb2 = findViewById(R.id.rb2);
        rb3 = findViewById(R.id.rb3);
        rb0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inflateSurfaceView(-1);
                modelChoosed = true;
            }
        });
        rb4 = findViewById(R.id.rb4);
        rb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inflateSurfaceView(0);
                modelChoosed = true;
            }
        });
        rb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inflateSurfaceView(1);
                modelChoosed = true;
            }
        });
        rb3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inflateSurfaceView(2);
                modelChoosed = true;
            }
        });
        rb4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                inflateSurfaceView(3);
                modelChoosed = true;
            }
        });

        llSb = findViewById(R.id.ll_sb);
        llSb.setVisibility(View.GONE);
    }

    @Override
    protected String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
    }

    private void inflateSurfaceView(int index) {
        if (modelChoosed) {
            return;
        }
        glSurfaceView = new PlayerCustomGLSurfaceView(this);
        cameraManager = new CameraManager();

        glSurfaceViewRenderer = new PlayerGLSurfaceViewRenderer(index);
        glSurfaceView.init(glSurfaceViewRenderer, cameraManager, false);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        FrameLayout frameLayout = findViewById(R.id.fl_glsurface);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.player_dimen_30);
        layoutParams.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.player_dimen_80);
        frameLayout.addView(glSurfaceView, 0, layoutParams);

        cameraManager.setActivityWeakReference(this);
        boolean openCamera = cameraManager.openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        Log.e("zqwx", "- "+openCamera);
        if (openCamera) {
        }
        if (index == 1
        || index == 2
        || index == 3) {
            llSb.setVisibility(View.VISIBLE);
            sbStep = llSb.findViewById(R.id.sbStep);
            sbTone = llSb.findViewById(R.id.sbTone);
            sbBeauty = llSb.findViewById(R.id.sbBeauty);
            sbBright = llSb.findViewById(R.id.sbBright);
            sbOpacity = llSb.findViewById(R.id.sbOpacity);

            sbStep.setOnSeekBarChangeListener(this);
            sbTone.setOnSeekBarChangeListener(this);
            sbBeauty.setOnSeekBarChangeListener(this);
            sbBright.setOnSeekBarChangeListener(this);
            sbOpacity.setOnSeekBarChangeListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) {
            cameraManager.stopPreview();
            cameraManager.releaseCamera();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int id = seekBar.getId();
        if (id == R.id.sbStep) {
            glSurfaceViewRenderer.setTexelOffset(range(progress, minstepoffset, maxstepoffset));
        } else if (id == R.id.sbTone) {
            glSurfaceViewRenderer.setToneLevel(range(progress, minToneValue, maxToneValue));
        } else if (id == R.id.sbBeauty) {
            glSurfaceViewRenderer.setBeautyLevel(range(progress, minbeautyValue, maxbeautyValue));
        } else if (id == R.id.sbBright) {
            glSurfaceViewRenderer.setBrightLevel(range(progress, minbrightValue, maxbrightValue));
        } else if (id == R.id.sbOpacity) {
            glSurfaceViewRenderer.setOpacity(range(progress, minOpacityValue, maxOpacityValue));
        }
    }

    protected float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 100.0f + start;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
