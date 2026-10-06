package com.candypop.game;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private MediaPlayer musicPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        musicPlayer = MediaPlayer.create(
                this,
                R.raw.candy_music
        );

        if (musicPlayer != null) {
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(0.45f, 0.45f);
            musicPlayer.start();
        }

        setContentView(new GameView(this));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (musicPlayer != null) {
            musicPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }

        super.onDestroy();
    }
}