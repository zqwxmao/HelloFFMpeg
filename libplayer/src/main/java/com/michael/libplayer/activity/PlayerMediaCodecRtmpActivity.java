package com.michael.libplayer.activity;

import android.Manifest;
import android.os.Bundle;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;

public class PlayerMediaCodecRtmpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_player_media_codec_rtmp);
    }

    @Override
    protected String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
        };
    }
}
