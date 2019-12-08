package com.michael.libplayer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.michael.libplayer.R;
import com.michael.libplayer.base.BaseActivity;

public class PlayerWelcomeActivity extends BaseActivity implements View.OnClickListener {

    private Button btnPlayerWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity_player_welcome);
        initView();
    }

    private void initView() {
        this.btnPlayerWelcome = findViewById(R.id.btn_enter_preview);

        this.btnPlayerWelcome.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_enter_preview) {
            startActivity(this, PlayerPreviewActivity.class);
        }
    }
}
