package com.michael.libplayer.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.michael.libplayer.R;

public class PlayerMainActivity extends AppCompatActivity {

    private ListView listView;
    private final String[] titles = {
      "1.使用FFMPeg推流视频文件到流媒体服务器",
      "2.使用VideoView拉流视频流文件播放",
      "3.使用MediaCodec硬编码/FFMpeg推流",
      "4.使用OpenGLES预览相机",
      "5.使用GLSurfaceView预览相机",
      "",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_main);
        initView();
    }

    private void initView() {
        listView = findViewById(R.id.player_lv);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    startActivity(PlayerFFMPegPushFileActivity.class);
                    break;
                case 1:
                    startActivity(PlayerIjkVideoViewActivity.class);
                    break;
                case 2:
                    startActivity(PlayerCameraRecordMuxerActivity.class);
                    break;
                case 3:
                    startActivity(PlayerOpenGLPreviewActivity.class);
                    break;
                case 4:
                    startActivity(PlayerGLSurfaceViewPreviewActivity.class);
                    break;
            }
        });
    }

    private void startActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }
}
