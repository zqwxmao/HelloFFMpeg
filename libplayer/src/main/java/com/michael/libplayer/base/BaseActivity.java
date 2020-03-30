package com.michael.libplayer.base;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.michael.libplayer.R;
import com.michael.libplayer.util.SystemBarTintManager;
import com.michael.libplayer.util.Utils;
import com.mj.permission.DynamicPermissionEmitter;
import com.mj.permission.DynamicPermissionEntity;

import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    protected ActionBarType mActionBarType = ActionBarType.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setTranslucentStatus();

        //设置标题栏
        mActionBarType = getDefaultActionBarType();
        setupActionBar();
    }

    private void checkPermission() {
        final String[] permissions = getPermissions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isEmpty(permissions)) {
            DynamicPermissionEmitter permissionEmitter = new DynamicPermissionEmitter(this);
            permissionEmitter.emitterPermission(new DynamicPermissionEmitter.ApplyPermissionsCallback() {

                @Override
                public void applyPermissionResult(Map<String, DynamicPermissionEntity> permissionEntityMap) {
                    DynamicPermissionEntity permissionEntity = permissionEntityMap.get(permissions);
                    if (permissionEntity != null) {
                        if (permissionEntity.isGranted()) {
                            //权限允许，可以搞事情了
                        } else if (permissionEntity.shouldShowRequestPermissionRationable()) {
                            //勾选不在提示，且点击了拒绝，在这里给用户提示权限的重要性，给一个友好的提示
                        } else {
                            //拒绝了权限，不能乱搞
                        }
                    }
                }
            }, permissions);
        }
    }

    protected String[] getPermissions() {
        return null;
    }

    /**
     * 获取标题栏类型
     * @return
     */
    protected ActionBarType getDefaultActionBarType() {
        return ActionBarType.NONE;
    }

    /**
     * 对ActionBar进行初始化
     */
    private void setupActionBar() {
        switch (mActionBarType) {
            case NONE: {
                //不需要时就不进行设置
                break;
            }
            case DEFAULT: {
                break;
            }
        }
    }

    public enum ActionBarType {
        NONE,
        DEFAULT,
    }

    /**
     * 设置状态栏为半透明黑色
     **/
    @TargetApi(21)
    protected void setTranslucentStatus() {
        if (!Utils.isBelowStatusBar()) {
            return;
        } else {     //设置5.0以上的系统为全透明
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(getResources().getColor(getStatusBarColor()));
        }

        SystemBarTintManager tineManager = new SystemBarTintManager(this);      //让状态栏显示透明
        tineManager.setStatusBarTintEnabled(true);
        tineManager.setStatusBarTintColor(Color.TRANSPARENT);
    }

    protected int getStatusBarColor(){
        return  R.color.player_background_half_black;
    }

    protected void startActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

    public static void startActivity(Context context, Class clazz, Intent extIntent) {
        Intent intent;
        if (extIntent != null) {
            intent = extIntent.cloneFilter();
            intent.setComponent(new ComponentName(context, clazz));
        } else {
            intent = new Intent(context, clazz);
        }
        context.startActivity(intent);
    }

    public static void startActivity(Context context, Class clazz) {
        Intent intent = new Intent(context, clazz);
        context.startActivity(intent);
    }
}
