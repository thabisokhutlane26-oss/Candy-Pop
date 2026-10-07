package com.candypop.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private LinearLayout root;

    private final int PINK = Color.rgb(255, 105, 180);
    private final int PURPLE = Color.rgb(124, 77, 255);
    private final int BLUE = Color.rgb(66, 165, 245);
    private final int WHITE = Color.WHITE;
    private final int DARK = Color.rgb(55, 35, 70);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        showHome();
    }

    private void showHome() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(35, 35, 35, 35);
        root.setBackgroundColor(Color.rgb(255, 235, 248));

        setContentView(root);

        TextView title = new TextView(this);
        title.setText("🍬 CANDYJOLT ⚡");
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(PURPLE);
        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1.2f
                )
        );

        TextView subtitle = new TextView(this);
        subtitle.setText("Match • Connect • Pop!");
        subtitle.setTextSize(20);
        subtitle.setTypeface(Typeface.DEFAULT_BOLD);
        subtitle.setTextColor(DARK);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        0.7f
                )
        );

        Button playButton = createButton(
                "🍭  PLAY",
                PINK
        );

        playButton.setOnClickListener(v -> startGame());

        root.addView(
                playButton,
                new LinearLayout.LayoutParams(
                        -1,
                        90
                )
        );

        addSpace(18);

        Button levelsButton = createButton(
                "⭐  LEVELS",
                PURPLE
        );

        levelsButton.setOnClickListener(v ->
                showMessage("Levels", "More levels are coming soon!")
        );

        root.addView(
                levelsButton,
                new LinearLayout.LayoutParams(
                        -1,
                        75
                )
        );

        addSpace(12);

        Button scoresButton = createButton(
                "🏆  SCORES",
                BLUE
        );

        scoresButton.setOnClickListener(v ->
                showMessage(
                        "Scores",
                        "Your high scores will appear here."
                )
        );

        root.addView(
                scoresButton,
                new LinearLayout.LayoutParams(
                        -1,
                        75
                )
        );

        addSpace(12);

        Button settingsButton = createButton(
                "⚙  SETTINGS",
                DARK
        );

        settingsButton.setOnClickListener(v ->
                showMessage(
                        "Settings",
                        "Sound, music and vibration will be added here."
                )
        );

        root.addView(
                settingsButton,
                new LinearLayout.LayoutParams(
                        -1,
                        75
                )
        );

        TextView footer = new TextView(this);
        footer.setText("🍬 Have fun and make a JOLT!");
        footer.setTextSize(15);
        footer.setTextColor(DARK);
        footer.setGravity(Gravity.CENTER);

        root.addView(
                footer,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        0.8f
                )
        );
    }

    private Button createButton(String text, int color) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(20);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(WHITE);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);

        button.setBackgroundColor(color);

        return button;
    }

    private void addSpace(int height) {

        View space = new View(this);

        root.addView(
                space,
                new LinearLayout.LayoutParams(
                        1,
                        height
                )
        );
    }

    private void startGame() {

        Intent intent = new Intent(
                MainActivity.this,
                GameActivity.class
        );

        intent.putExtra("level", 1);

        startActivity(intent);
    }

    private void showMessage(
            String title,
            String message
    ) {

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }
}