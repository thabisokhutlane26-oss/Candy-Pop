package com.candypop.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private LinearLayout root;

    private final int PINK = Color.rgb(255, 72, 145);
    private final int PURPLE = Color.rgb(126, 87, 255);
    private final int BLUE = Color.rgb(66, 165, 245);
    private final int CYAN = Color.rgb(38, 198, 218);
    private final int YELLOW = Color.rgb(255, 193, 7);
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

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        scrollView.setBackground(
                createGradient(
                        Color.rgb(255, 214, 239),
                        Color.rgb(224, 213, 255)
                )
        );

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 22, 28, 28);

        scrollView.addView(root);

        setContentView(scrollView);

        TextView candyDecoration = new TextView(this);
        candyDecoration.setText("🍬   🍭   🍓   🍬   🍭");
        candyDecoration.setTextSize(25);
        candyDecoration.setGravity(Gravity.CENTER);

        root.addView(
                candyDecoration,
                new LinearLayout.LayoutParams(
                        -1,
                        55
                )
        );

        TextView title = new TextView(this);
        title.setText("CANDYJOLT");
        title.setTextSize(38);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(PURPLE);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(
                7,
                0,
                4,
                Color.argb(80, 80, 40, 120)
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        65
                )
        );

        TextView subtitle = new TextView(this);
        subtitle.setText("MATCH  •  CONNECT  •  POP!");
        subtitle.setTextSize(16);
        subtitle.setTypeface(Typeface.DEFAULT_BOLD);
        subtitle.setTextColor(DARK);
        subtitle.setGravity(Gravity.CENTER);

        root.addView(
                subtitle,
                new LinearLayout.LayoutParams(
                        -1,
                        40
                )
        );

        addSpace(12);

        LinearLayout progressCard = createCard();

        TextView progressTitle = createLabel(
                "🏆  YOUR CANDY JOURNEY",
                17,
                PURPLE
        );

        progressCard.addView(
                progressTitle,
                new LinearLayout.LayoutParams(
                        -1,
                        40
                )
        );

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER);

        statsRow.addView(
                createStatBox(
                        "LEVEL",
                        "1",
                        PINK
                ),
                new LinearLayout.LayoutParams(
                        0,
                        78,
                        1
                )
        );

        addRowSpace(statsRow, 6);

        statsRow.addView(
                createStatBox(
                        "BEST SCORE",
                        "0",
                        BLUE
                ),
                new LinearLayout.LayoutParams(
                        0,
                        78,
                        1
                )
        );

        addRowSpace(statsRow, 6);

        statsRow.addView(
                createStatBox(
                        "STARS",
                        "0",
                        YELLOW
                ),
                new LinearLayout.LayoutParams(
                        0,
                        78,
                        1
                )
        );

        progressCard.addView(statsRow);

        root.addView(
                progressCard,
                new LinearLayout.LayoutParams(
                        -1,
                        135
                )
        );

        addSpace(18);

        Button playButton = createBigButton(
                "🍭   PLAY NOW   🍭",
                PINK
        );

        playButton.setOnClickListener(v -> startGame());

        root.addView(
                playButton,
                new LinearLayout.LayoutParams(
                        -1,
                        82
                )
        );

        addSpace(15);

        LinearLayout firstButtonRow = new LinearLayout(this);
        firstButtonRow.setOrientation(LinearLayout.HORIZONTAL);
        firstButtonRow.setGravity(Gravity.CENTER);

        Button levelsButton = createSmallButton(
                "⭐\nLEVELS",
                PURPLE
        );

        levelsButton.setOnClickListener(v ->
                showMessage(
                        "Levels",
                        "More levels are coming soon!"
                )
        );

        firstButtonRow.addView(
                levelsButton,
                new LinearLayout.LayoutParams(
                        0,
                        78,
                        1
                )
        );

        addRowSpace(firstButtonRow, 10);

        Button scoresButton = createSmallButton(
                "🏆\nSCORES",
                BLUE
        );

        scoresButton.setOnClickListener(v ->
                showMessage(
                        "Scores",
                        "Your high scores will appear here."
                )
        );

        firstButtonRow.addView(
                scoresButton,
                new LinearLayout.LayoutParams(
                        0,
                        78,
                        1
                )
        );

        root.addView(
                firstButtonRow,
                new LinearLayout.LayoutParams(
                        -1,
                        78
                )
        );

        addSpace(12);

        Button settingsButton = createSmallButton(
                "⚙   SETTINGS",
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
                        65
                )
        );

        addSpace(15);

        LinearLayout featureCard = createCard();

        TextView featureTitle = createLabel(
                "🍬 CANDY POP FEATURES",
                16,
                CYAN
        );

        featureCard.addView(
                featureTitle,
                new LinearLayout.LayoutParams(
                        -1,
                        35
                )
        );

        TextView features = new TextView(this);

        features.setText(
                "✨ 100 levels\n" +
                "🔥 Long combos & connections\n" +
                "⏱ Time-based challenges\n" +
                "🎵 Candy game music\n" +
                "⭐ Stars & rewards coming soon"
        );

        features.setTextSize(14);
        features.setTextColor(DARK);
        features.setTypeface(Typeface.DEFAULT_BOLD);
        features.setGravity(Gravity.CENTER_VERTICAL);

        featureCard.addView(
                features,
                new LinearLayout.LayoutParams(
                        -1,
                        110
                )
        );

        root.addView(
                featureCard,
                new LinearLayout.LayoutParams(
                        -1,
                        155
                )
        );

        addSpace(18);

        TextView bottomCandy = new TextView(this);

        bottomCandy.setText(
                "🍓  🍬  🍭  🍫  🍒  🍏\n\n" +
                "Have fun and make a JOLT! ⚡"
        );

        bottomCandy.setTextSize(15);
        bottomCandy.setTypeface(Typeface.DEFAULT_BOLD);
        bottomCandy.setTextColor(DARK);
        bottomCandy.setGravity(Gravity.CENTER);

        root.addView(
                bottomCandy,
                new LinearLayout.LayoutParams(
                        -1,
                        85
                )
        );
    }

    private LinearLayout createCard() {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(18, 10, 18, 10);

        card.setBackground(
                createRoundedBackground(
                        Color.argb(235, 255, 255, 255),
                        Color.argb(70, 126, 87, 255),
                        3,
                        24
                )
        );

        return card;
    }

    private TextView createLabel(
            String text,
            float size,
            int color
    ) {

        TextView label = new TextView(this);

        label.setText(text);
        label.setTextSize(size);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER);

        return label;
    }

    private LinearLayout createStatBox(
            String title,
            String value,
            int color
    ) {

        LinearLayout box = new LinearLayout(this);

        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(4, 4, 4, 4);

        box.setBackground(
                createRoundedBackground(
                        makeTransparent(color, 40),
                        makeTransparent(color, 70),
                        2,
                        18
                )
        );

        TextView valueText = new TextView(this);

        valueText.setText(value);
        valueText.setTextSize(23);
        valueText.setTypeface(Typeface.DEFAULT_BOLD);
        valueText.setTextColor(color);
        valueText.setGravity(Gravity.CENTER);

        box.addView(
                valueText,
                new LinearLayout.LayoutParams(
                        -1,
                        40
                )
        );

        TextView titleText = new TextView(this);

        titleText.setText(title);
        titleText.setTextSize(10);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(DARK);
        titleText.setGravity(Gravity.CENTER);

        box.addView(
                titleText,
                new LinearLayout.LayoutParams(
                        -1,
                        25
                )
        );

        return box;
    }

    private Button createBigButton(
            String text,
            int color
    ) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(21);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(WHITE);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setPadding(10, 0, 10, 0);

        button.setBackground(
                createRoundedBackground(
                        color,
                        makeTransparent(Color.BLACK, 90),
                        3,
                        28
                )
        );

        return button;
    }

    private Button createSmallButton(
            String text,
            int color
    ) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(WHITE);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);

        button.setBackground(
                createRoundedBackground(
                        color,
                        makeTransparent(Color.BLACK, 60),
                        2,
                        22
                )
        );

        return button;
    }

    private int makeTransparent(
            int color,
            int alpha
    ) {

        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private GradientDrawable createGradient(
            int startColor,
            int endColor
    ) {

        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        startColor,
                        endColor
                }
        );
    }

    private GradientDrawable createRoundedBackground(
            int fillColor,
            int strokeColor,
            int strokeWidth,
            int radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(
                strokeWidth,
                strokeColor
        );

        return drawable;
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

    private void addRowSpace(
            LinearLayout row,
            int width
    ) {

        View space = new View(this);

        row.addView(
                space,
                new LinearLayout.LayoutParams(
                        width,
                        1
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