package com.michael.libplayer.activity;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.michael.libplayer.R;

public class PlayerVideoViewActivity extends AppCompatActivity {

    private VideoView vvPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_player_video_view);
        initView();
    }

    private void initView() {
        this.vvPlay = findViewById(R.id.vv_play);
        vvPlay.setVideoPath("rtmp://118.24.120.128/myapp/test");
        vvPlay.start();
        MediaController mediaController = new MediaController(this);
        vvPlay.setMediaController(mediaController);
        mediaController.setMediaPlayer(vvPlay);
    }
}
