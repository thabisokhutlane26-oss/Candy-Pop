package com.candypop.game;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private MediaPlayer musicPlayer;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        settings = getSharedPreferences(
                "CandyPopSettings",
                MODE_PRIVATE
        );

        setContentView(new GameView(this));

        updateMusic();
    }

    private void updateMusic() {
        boolean musicEnabled = settings.getBoolean(
                "music_enabled",
                true
        );

        if (!musicEnabled) {
            if (musicPlayer != null && musicPlayer.isPlaying()) {
                musicPlayer.pause();
            }
            return;
        }

        if (musicPlayer == null) {
            musicPlayer = MediaPlayer.create(
                    this,
                    R.raw.candy_music
            );

            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(0.45f, 0.45f);
            }
        }

        if (musicPlayer != null && !musicPlayer.isPlaying()) {
            musicPlayer.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (settings != null) {
            updateMusic();
        }
    }

    @Override
    protected void onPause() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (musicPlayer != null) {
            if (musicPlayer.isPlaying()) {
                musicPlayer.stop();
            }

            musicPlayer.release();
            musicPlayer = null;
        }

        super.onDestroy();
    }
}