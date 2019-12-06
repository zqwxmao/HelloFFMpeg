package com.michael.libplayer.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.michael.libplayer.R;

public class PlayerMainActivity extends AppCompatActivity {

    private ListView listView;
    private final String[] titles = {
      "1.使用FFMPeg推流视频文件到流媒体服务器",
      "2.使用VideoView拉流视频流文件播放",
      "",
      "",
      "",
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
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(PlayerFFMPegPushFileActivity.class);
                        break;
                    case 1:
                        startActivity(PlayerVideoViewActivity.class);
                        break;
                }
            }
        });
    }

    private void startActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }
}
