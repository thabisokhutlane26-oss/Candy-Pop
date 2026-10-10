package com.candypop.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class GameSound {

    private final ToneGenerator toneGenerator;
    private final SharedPreferences settings;

    public GameSound(Context context) {
        settings = context.getSharedPreferences(
                "CandyPopSettings",
                Context.MODE_PRIVATE
        );

        toneGenerator = new ToneGenerator(
                AudioManager.STREAM_MUSIC,
                80
        );
    }

    private boolean isSoundEnabled() {
        return settings.getBoolean("sound_enabled", true);
    }

    public void playPop() {
        if (isSoundEnabled()) {
            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    70
            );
        }
    }

    public void playCombo() {
        if (isSoundEnabled()) {
            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_ACK,
                    120
            );
        }
    }

    public void playComplete() {
        if (isSoundEnabled()) {
            toneGenerator.startTone(
                    ToneGenerator.TONE_PROP_PROMPT,
                    180
            );
        }
    }

    public void release() {
        toneGenerator.release();
    }
}