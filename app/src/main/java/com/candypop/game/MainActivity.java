
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

    private static final int BG = Color.rgb(250, 246, 255);
    private static final int DARK = Color.rgb(48, 35, 75);
    private static final int MUTED = Color.rgb(126, 112, 145);
    private static final int PINK = Color.rgb(239, 65, 133);
    private static final int PURPLE = Color.rgb(119, 78, 218);
    private static final int BLUE = Color.rgb(65, 145, 225);
    private static final int GREEN = Color.rgb(42, 180, 112);
    private static final int ORANGE = Color.rgb(245, 145, 55);
    private static final int WHITE = Color.WHITE;

    private SharedPreferences scores;
    private SharedPreferences progress;
    private SharedPreferences settings;

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        getWindow().setNavigationBarColor(BG);
        getWindow().setStatusBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        scores = getSharedPreferences(
                "CandyPopScores", MODE_PRIVATE
        );

        progress = getSharedPreferences(
                "CandyJolt", MODE_PRIVATE
        );

        settings = getSharedPreferences(
                "CandyPopSettings", MODE_PRIVATE
        );

        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (root != null) {
            showHome();
        }
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(15), dp(20), dp(25));

        scroll.addView(root);
        setContentView(scroll);

        addHeader();
        addHero();
        addProgress();
        addPlayButton();
        addMenu();
        addFooter();
    }

    private void addHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);

        TextView name = text(
                "CANDYJOLT", 22, DARK, true
        );
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

        TextView subtitle = text(
                "THE SWEET MATCH ADVENTURE",
                9, MUTED, true
        );
        subtitle.setGravity(Gravity.LEFT);

        brand.addView(name);
        brand.addView(subtitle);

        header.addView(
                brand,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        TextView levelBadge = text(
                "★  100 LEVELS", 12, PURPLE, true
        );
        levelBadge.setPadding(
                dp(13), dp(9), dp(13), dp(9)
        );
        levelBadge.setBackground(
                rounded(Color.rgb(239, 230, 255), 30)
        );

        header.addView(levelBadge);

        root.addView(header);
    }

    private void addHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(
                dp(18), dp(25), dp(18), dp(23)
        );

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(255, 183, 213),
                        Color.rgb(220, 191, 255),
                        Color.rgb(184, 221, 255)
                }
        );

        gradient.setCornerRadius(dp(28));
        hero.setBackground(gradient);
        hero.setElevation(dp(3));

        TextView decoration = text(
                "🍓   🍊   🍇   🍋",
                29, WHITE, true
        );

        TextView title = text(
                "LET'S PLAY!", 31, DARK, true
        );

        TextView description = text(
                "Match delicious treats and complete\n"
                        + "your way through 100 exciting levels.",
                13, DARK, false
        );

        description.setLineSpacing(dp(3), 1f);

        hero.addView(decoration);
        addSpace(hero, 9);
        hero.addView(title);
        addSpace(hero, 7);
        hero.addView(description);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(22);
        root.addView(hero, params);
    }

    private void addProgress() {
        int highestLevel = Math.max(
                1,
                Math.min(100,
                        progress.getInt("highest_level", 1))
        );

        int highScore = scores.getInt("high_score", 0);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackground(rounded(WHITE, 22));
        card.setElevation(dp(2));

        TextView heading = text(
                "YOUR PROGRESS", 12, MUTED, true
        );
        heading.setGravity(Gravity.LEFT);

        card.addView(heading);
        addSpace(card, 12);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        addStat(stats, "LEVEL", String.valueOf(highestLevel), PURPLE);
        addStat(stats, "HIGH SCORE", String.valueOf(highScore), PINK);
        addStat(stats, "TOTAL", "100", BLUE);

        card.addView(stats);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.topMargin = dp(17);
        root.addView(card, params);
    }

    private void addStat(
            LinearLayout parent,
            String label,
            String value,
            int color
    ) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);

        TextView number = text(value, 24, color, true);
        TextView caption = text(label, 9, MUTED, true);

        item.addView(number);
        addSpace(item, 3);
        item.addView(caption);

        parent.addView(
                item,
                new LinearLayout.LayoutParams(0, -2, 1)
        );
    }

    private void addPlayButton() {
        Button play = button(
                "▶     CONTINUE PLAYING", GREEN, 18
        );

        play.setElevation(dp(4));

        int highestLevel = Math.max(
                1,
                Math.min(100,
                        progress.getInt("highest_level", 1))
        );

        play.setOnClickListener(v -> startGame(highestLevel));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(62));

        params.topMargin = dp(20);
        root.addView(play, params);
    }

    private void addMenu() {
        TextView heading = text(
                "EXPLORE THE GAME", 12, MUTED, true
        );
        heading.setGravity(Gravity.LEFT);

        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(-1, dp(30));

        headingParams.topMargin = dp(21);
        root.addView(heading, headingParams);

        LinearLayout rowOne = new LinearLayout(this);
        rowOne.setOrientation(LinearLayout.HORIZONTAL);

        Button levels = button("★  LEVELS", PURPLE, 14);
        levels.setOnClickListener(v -> showLevels());

        Button scoresButton = button("🏆  SCORES", ORANGE, 14);
        scoresButton.setOnClickListener(v -> showScores());

        addMenuItem(rowOne, levels);
        addMenuItem(rowOne, scoresButton);
        root.addView(rowOne);

        LinearLayout rowTwo = new LinearLayout(this);
        rowTwo.setOrientation(LinearLayout.HORIZONTAL);

        Button settingsButton = button(
                "⚙  SETTINGS", BLUE, 14
        );
        settingsButton.setOnClickListener(v -> showSettings());

        Button aboutButton = button(
                "ⓘ  ABOUT", PINK, 14
        );
        aboutButton.setOnClickListener(v -> showAbout());

        addMenuItem(rowTwo, settingsButton);
        addMenuItem(rowTwo, aboutButton);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, dp(52));

        rowParams.topMargin = dp(10);
        root.addView(rowTwo, rowParams);
    }

    private void addMenuItem(
            LinearLayout row,
            Button button
    ) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(52), 1);

        params.setMargins(dp(4), 0, dp(4), 0);
        row.addView(button, params);
    }

    private void addFooter() {
        TextView footer = text(
                "MADE FOR SWEET MOMENTS  •  CANDYJOLT",
                9, MUTED, true
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(40));

        params.topMargin = dp(14);
        root.addView(footer, params);
    }

    private TextView text(
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
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private Button button(
            String label,
            int color,
            int size
    ) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(WHITE);
        b.setTextSize(size);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(5), 0, dp(5), 0);
        b.setBackground(rounded(color, 17));

        return b;
    }

    private GradientDrawable rounded(
            int color,
            int radius
    ) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private void addSpace(LinearLayout parent, int height) {
        TextView space = new TextView(this);
        parent.addView(
                space,
                new LinearLayout.LayoutParams(1, dp(height))
        );
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

        for (int i = 0; i < 100; i++) {
            levels[i] = "Level " + (i + 1);
        }

        new AlertDialog.Builder(this)
                .setTitle("CHOOSE A LEVEL")
                .setItems(levels, (dialog, which) ->
                        startGame(which + 1))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showScores() {
        int highScore = scores.getInt("high_score", 0);
        int highestLevel = progress.getInt("highest_level", 1);

        new AlertDialog.Builder(this)
                .setTitle("YOUR GAME PROGRESS")
                .setMessage(
                        "Highest level: " + highestLevel
                                + "\nHigh score: " + highScore
                )
                .setPositiveButton("PLAY", (dialog, which) ->
                        startGame(highestLevel))
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void showSettings() {
        String[] options = {
                "Background Music",
                "Sound Effects"
        };

        boolean[] checked = {
                settings.getBoolean("music_enabled", true),
                settings.getBoolean("sound_enabled", true)
        };

        new AlertDialog.Builder(this)
                .setTitle("SOUND SETTINGS")
                .setMultiChoiceItems(
                        options,
                        checked,
                        (dialog, which, enabled) -> {
                            String key = which == 0
                                    ? "music_enabled"
                                    : "sound_enabled";

                            settings.edit()
                                    .putBoolean(key, enabled)
                                    .apply();
                        }
                )
                .setPositiveButton("DONE", null)
                .setNeutralButton("ABOUT", (dialog, which) ->
                        showAbout())
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("ABOUT CANDYJOLT")
                .setMessage(
                        "Welcome to CandyJolt!\n\n"
                                + "Match fruits and sweets, "
                                + "create bigger combos and "
                                + "complete 100 exciting levels.\n\n"
                                + "Enjoy the adventure!"
                )
                .setPositiveButton("LET'S PLAY!", null)
                .show();
    }
}
