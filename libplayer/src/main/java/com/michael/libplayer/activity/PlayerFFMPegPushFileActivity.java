package com.michael.libplayer.activity;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.michael.libplayer.R;
import com.michael.libplayer.ffmpeg.FFMpegHandle;
import com.michael.libplayer.util.FileUtils;
import com.michael.libplayer.util.pool.ThreadPoolManager;

import java.io.File;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class PlayerFFMPegPushFileActivity extends AppCompatActivity {

    private ProgressBar pb;
    private ListView lv;

    private List<String> fileNames = new LinkedList<>();
    private List<String> filePaths = new LinkedList<>();
    private final String rtmpURL = "rtmp://118.24.120.128/myapp/test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_player_ffmpeg_push_file);
        initView();
        initData();
    }

    private void initView() {
        this.pb = findViewById(R.id.player_pb);
        this.lv = findViewById(R.id.player_lv);
    }

    private void initData() {
        ThreadPoolManager.getInstance().start(() -> {

            String rootDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
            File rootFile = new File(rootDirectory);
            Stack<File> fileStack = new Stack<>();
            fileStack.push(rootFile);
            File tmpFile;
            File[] tmpFiles;
            int tmpLen;
            while (!fileStack.isEmpty()) {
                tmpFile = fileStack.pop();
                if (tmpFile != null) {
                    if (tmpFile.isDirectory()) {
//                        Log.e("zqwx", "fileDirectory : "+tmpFile.getName()+", path : "+tmpFile.getAbsolutePath());
                        tmpFiles = tmpFile.listFiles();
                        if (tmpFiles != null && tmpFiles.length > 0) {
                            tmpLen = tmpFiles.length;
                            for (int i = 0; i < tmpLen; i++) {
                                fileStack.push(tmpFiles[i]);
                            }
                        }
                    } else {
                        if (!TextUtils.isEmpty(tmpFile.getName())) {
                            try {
                                if (FileUtils.isVideo(tmpFile.getName())) {
                                    Log.i("zqwx", "fileName : " + tmpFile.getName() + ", path : " + tmpFile.getAbsolutePath());
                                    fileNames.add(tmpFile.getName());
                                    filePaths.add(tmpFile.getAbsolutePath());
                                }
                            } catch (StringIndexOutOfBoundsException e) {
                            }
                        }
                    }
                }
            }

            runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                lv.setVisibility(View.VISIBLE);
                lv.setAdapter(new ArrayAdapter<>(PlayerFFMPegPushFileActivity.this, android.R.layout.simple_list_item_1, fileNames));
                lv.setOnItemClickListener((parent, view, position, id) -> {
                    pb.setVisibility(View.VISIBLE);
                    ThreadPoolManager.getInstance().start(() -> {
                        FFMpegHandle.getInstance().pushFFMpegFile(rtmpURL, filePaths.get(position));
                        runOnUiThread(() -> {
                            pb.setVisibility(View.GONE);
                        });
                    });
                });
            });
        });

    }
}
