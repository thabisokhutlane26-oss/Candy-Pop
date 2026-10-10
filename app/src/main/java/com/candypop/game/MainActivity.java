package com.candypop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PINK = Color.rgb(255, 75, 139);
    private static final int PURPLE = Color.rgb(111, 67, 218);
    private static final int BLUE = Color.rgb(54, 139, 225);
    private static final int GREEN = Color.rgb(57, 183, 105);
    private static final int ORANGE = Color.rgb(255, 148, 57);
    private static final int DARK = Color.rgb(59, 38, 83);
    private static final int BACKGROUND = Color.rgb(255, 242, 250);
    private static final int WHITE = Color.WHITE;

    private LinearLayout root;
    private SharedPreferences preferences;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().setNavigationBarColor(BACKGROUND);

        preferences = getSharedPreferences(
                "CandyPopScores",
                MODE_PRIVATE
        );

        settings = getSharedPreferences(
                "CandyPopSettings",
                MODE_PRIVATE
        );

        showHome();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(14), dp(22), dp(22));

        scroll.addView(root);
        setContentView(scroll);

        addTopBar();
        addLogo();
        addCandyRow();
        addTagline();
        addPlayButton();
        addMenuButtons();
        addFooter();
    }

    private void addTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView brand = makeText(
                "CANDY POP", 19, DARK, true
        );

        bar.addView(
                brand,
                new LinearLayout.LayoutParams(0, dp(44), 1)
        );

        TextView levels = makeText(
                "★  100 LEVELS", 12, PURPLE, true
        );

        levels.setPadding(dp(12), 0, dp(12), 0);
        levels.setBackground(rounded(
                Color.rgb(235, 224, 255), 24
        ));

        bar.addView(
                levels,
                new LinearLayout.LayoutParams(-2, dp(34))
        );

        root.addView(
                bar,
                new LinearLayout.LayoutParams(-1, dp(45))
        );
    }

    private void addLogo() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(18), dp(12), dp(18));

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(255, 207, 233),
                        Color.rgb(232, 216, 255),
                        Color.rgb(210, 236, 255)
                }
        );

        gradient.setCornerRadius(dp(28));
        card.setBackground(gradient);

        card.addView(
                makeText("WELCOME TO", 13, PURPLE, true),
                new LinearLayout.LayoutParams(-1, dp(27))
        );

        card.addView(
                makeText("CANDY", 45, PINK, true),
                new LinearLayout.LayoutParams(-1, dp(59))
        );

        card.addView(
                makeText("P O P !", 32, PURPLE, true),
                new LinearLayout.LayoutParams(-1, dp(45))
        );

        card.addView(
                makeText(
                        "A SWEET WORLD OF FUN",
                        12, DARK, true
                ),
                new LinearLayout.LayoutParams(-1, dp(28))
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(10);
        root.addView(card, params);
    }

    private void addCandyRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setOrientation(LinearLayout.HORIZONTAL);

        addCandy(row, "●", Color.rgb(255, 77, 120));
        addCandy(row, "◆", Color.rgb(255, 166, 55));
        addCandy(row, "●", Color.rgb(79, 188, 105));
        addCandy(row, "◆", Color.rgb(82, 132, 255));
        addCandy(row, "●", Color.rgb(191, 101, 225));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(52));

        params.topMargin = dp(5);
        root.addView(row, params);
    }

    private void addCandy(
            LinearLayout row,
            String symbol,
            int color
    ) {
        TextView candy = makeText(symbol, 30, color, true);

        row.addView(
                candy,
                new LinearLayout.LayoutParams(0, -1, 1)
        );
    }

    private void addTagline() {
        root.addView(
                makeText(
                        "Small Game • Big Smiles",
                        20, DARK, true
                ),
                new LinearLayout.LayoutParams(-1, dp(42))
        );

        root.addView(
                makeText(
                        "Connect candies. Make combos. Beat levels!",
                        13, Color.rgb(126, 104, 145), false
                ),
                new LinearLayout.LayoutParams(-1, dp(30))
        );
    }

    private void addPlayButton() {
        Button play = makeButton(
                "▶     PLAY NOW", GREEN, 22
        );

        play.setElevation(dp(4));
        play.setOnClickListener(v -> startGame(1));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(72));

        params.topMargin = dp(12);
        root.addView(play, params);
    }

    private void addMenuButtons() {
        Button levels = makeButton("★     LEVELS", PURPLE, 17);
        levels.setOnClickListener(v -> showLevels());
        addMenuButton(levels, 13);

        Button scores = makeButton(
                "🏆     HIGH SCORES", ORANGE, 17
        );
        scores.setOnClickListener(v -> showScores());
        addMenuButton(scores, 10);

        Button settingsButton = makeButton(
                "⚙     SETTINGS", BLUE, 17
        );
        settingsButton.setOnClickListener(v -> showSettings());
        addMenuButton(settingsButton, 10);
    }

    private void addMenuButton(Button button, int margin) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(56));

        params.topMargin = dp(margin);
        root.addView(button, params);
    }

    private void addFooter() {
        TextView footer = makeText(
                "MADE FOR SWEET MOMENTS",
                11, Color.rgb(145, 120, 154), true
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(42));

        params.topMargin = dp(10);
        root.addView(footer, params);
    }

    private TextView makeText(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);

        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private Button makeButton(
            String label,
            int color,
            int size
    ) {
        Button button = new Button(this);

        button.setText(label);
        button.setTextColor(WHITE);
        button.setTextSize(size);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(color, 19));

        return button;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    private void startGame(int level) {
        Intent intent = new Intent(
                MainActivity.this,
                GameActivity.class
        );

        intent.putExtra("level", level);
        startActivity(intent);
    }

    private void showLevels() {
        String[] levels = new String[100];

        for (int i = 0; i < levels.length; i++) {
            levels[i] = "Level " + (i + 1);
        }

        new AlertDialog.Builder(this)
                .setTitle("★ CHOOSE A LEVEL")
                .setItems(levels, (dialog, which) ->
                        startGame(which + 1))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showScores() {
        int highScore = preferences.getInt("high_score", 0);
        int highestLevel = preferences.getInt("highest_level", 1);

        new AlertDialog.Builder(this)
                .setTitle("🏆 YOUR PROGRESS")
                .setMessage(
                        "High Score: " + highScore
                                + "\nHighest Level: " + highestLevel
                )
                .setPositiveButton("PLAY", (dialog, which) ->
                        startGame(1))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showSettings() {
        String[] options = {
                "🎵  Background Music",
                "🔊  Sound Effects"
        };

        boolean[] checked = {
                settings.getBoolean("music_enabled", true),
                settings.getBoolean("sound_enabled", true)
        };

        new AlertDialog.Builder(this)
                .setTitle("⚙  SOUND SETTINGS")
                .setMultiChoiceItems(
                        options,
                        checked,
                        (dialog, which, isChecked) -> {
                            if (which == 0) {
                                settings.edit()
                                        .putBoolean(
                                                "music_enabled",
                                                isChecked
                                        )
                                        .apply();
                            } else {
                                settings.edit()
                                        .putBoolean(
                                                "sound_enabled",
                                                isChecked
                                        )
                                        .apply();
                            }
                        }
                )
                .setPositiveButton("ABOUT", (dialog, which) ->
                        showAbout())
                .setNegativeButton("DONE", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("🍬 ABOUT CANDY POP")
                .setMessage(
                        "Welcome to Candy Pop!\n\n"
                                + "Connect candies, create combos, "
                                + "beat challenges and enjoy 100 levels "
                                + "of colourful fun.\n\n"
                                + "Made for kids and adults."
                )
                .setPositiveButton("LET'S PLAY!", null)
                .show();
    }
}