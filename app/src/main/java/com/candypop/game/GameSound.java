package com.candypop.game;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class GameSound {

    private final ToneGenerator toneGenerator;

    public GameSound(Context context) {
        toneGenerator = new ToneGenerator(
                AudioManager.STREAM_MUSIC,
                80
        );
    }

    public void playPop() {
        toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                70
        );
    }

    public void playCombo() {
        toneGenerator.startTone(
                ToneGenerator.TONE_PROP_ACK,
                120
        );
    }

    public void playComplete() {
        toneGenerator.startTone(
                ToneGenerator.TONE_PROP_PROMPT,
                180
        );
    }

    public void release() {
        toneGenerator.release();
    }
}