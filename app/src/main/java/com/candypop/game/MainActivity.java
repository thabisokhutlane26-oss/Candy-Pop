package com.candypop.game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new CandyJoltView(this));
    }

    static class CandyJoltView extends View {

        static final int HOME = 0;
        static final int LEVELS = 1;
        static final int GAME = 2;
        static final int COMPLETE = 3;
        static final int GAMEOVER = 4;
        static final int SETTINGS = 5;
        static final int SCORES = 6;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Random random = new Random();
        final Handler handler = new Handler();

        SharedPreferences prefs;

        int screen = HOME;
        int level = 1;
        int score = 0;
        int bestScore = 0;
        int timeLeft = 60;
        int target = 20;
        int collected = 0;
        int combo = 0;

        boolean paused = false;
        boolean musicOn = true;
        boolean soundOn = true;
        boolean vibrationOn = true;

        float downX;
        float downY;

        static final int ROWS = 7;
        static final int COLS = 7;

        int[][] board = new int[ROWS][COLS];

        int[] candyColors = {
                Color.rgb(255, 80, 110),
                Color.rgb(80, 170, 255),
                Color.rgb(255, 205, 60),
                Color.rgb(90, 210, 120),
                Color.rgb(180, 100, 245),
                Color.rgb(255, 140, 55)
        };

        Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (screen == GAME && !paused) {
                    timeLeft--;

                    if (timeLeft <= 0) {
                        timeLeft = 0;

                        if (collected >= target) {
                            completeLevel();
                        } else {
                            screen = GAMEOVER;
                            handler.removeCallbacks(this);
                        }
                    } else {
                        handler.postDelayed(this, 1000);
                    }

                    invalidate();
                }
            }
        };

        CandyJoltView(Context context) {
            super(context);

            prefs = context.getSharedPreferences(
                    "CandyJoltSettings",
                    Context.MODE_PRIVATE
            );

            bestScore = prefs.getInt("bestScore", 0);
            level = prefs.getInt("level", 1);

            musicOn = prefs.getBoolean("music", true);
            soundOn = prefs.getBoolean("sound", true);
            vibrationOn = prefs.getBoolean("vibration", true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawColor(Color.rgb(255, 244, 250));

            switch (screen) {
                case HOME:
                    drawHome(canvas);
                    break;

                case LEVELS:
                    drawLevels(canvas);
                    break;

                case GAME:
                    drawGame(canvas);
                    break;

                case COMPLETE:
                    drawComplete(canvas);
                    break;

                case GAMEOVER:
                    drawGameOver(canvas);
                    break;

                case SETTINGS:
                    drawSettings(canvas);
                    break;

                case SCORES:
                    drawScores(canvas);
                    break;
            }
        }

        void drawText(Canvas canvas, String value,
                      float x, float y, float size,
                      int color, Paint.Align align) {

            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextSize(size);
            p.setTextAlign(align);
            p.setTypeface(Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
            ));

            canvas.drawText(value, x, y, p);
        }

        void roundRect(Canvas canvas,
                       float left, float top,
                       float right, float bottom,
                       float radius, int color) {

            p.setStyle(Paint.Style.FILL);
            p.setColor(color);

            canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    radius,
                    radius,
                    p
            );
        }

        void drawHeader(Canvas canvas, String title) {

            p.setColor(Color.rgb(255, 92, 135));
            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    90,
                    p
            );

            drawText(
                    canvas,
                    "CANDYJOLT",
                    25,
                    43,
                    25,
                    Color.WHITE,
                    Paint.Align.LEFT
            );

            drawText(
                    canvas,
                    title,
                    getWidth() - 25,
                    43,
                    18,
                    Color.WHITE,
                    Paint.Align.RIGHT
            );
        }

        void button(Canvas canvas,
                    String label,
                    float left,
                    float top,
                    float right,
                    float bottom,
                    int color) {

            roundRect(
                    canvas,
                    left,
                    top,
                    right,
                    bottom,
                    25,
                    color
            );

            drawText(
                    canvas,
                    label,
                    (left + right) / 2f,
                    (top + bottom) / 2f + 8,
                    20,
                    Color.WHITE,
                    Paint.Align.CENTER
            );
        }

        void drawHome(Canvas canvas) {

            drawText(
                    canvas,
                    "🍬",
                    getWidth() / 2f - 60,
                    115,
                    50,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "🍭",
                    getWidth() / 2f,
                    115,
                    50,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "🍬",
                    getWidth() / 2f + 60,
                    115,
                    50,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "CandyJolt",
                    getWidth() / 2f,
                    175,
                    42,
                    Color.rgb(245, 60, 105),
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "Small Game • Big Smiles",
                    getWidth() / 2f,
                    205,
                    18,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            button(
                    canvas,
                    "PLAY",
                    80,
                    245,
                    getWidth() - 80,
                    315,
                    Color.rgb(70, 200, 105)
            );

            button(
                    canvas,
                    "LEVELS",
                    80,
                    330,
                    getWidth() - 80,
                    390,
                    Color.rgb(80, 160, 245)
            );

            button(
                    canvas,
                    "HIGH SCORES",
                    80,
                    405,
                    getWidth() - 80,
                    465,
                    Color.rgb(175, 100, 235)
            );

            button(
                    canvas,
                    "SETTINGS",
                    80,
                    480,
                    getWidth() - 80,
                    540,
                    Color.rgb(255, 165, 55)
            );

            drawText(
                    canvas,
                    musicOn ? "🔊" : "🔇",
                    getWidth() - 40,
                    590,
                    28,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );
        }

        void drawLevels(Canvas canvas) {

            drawHeader(canvas, "LEVELS");

            drawText(
                    canvas,
                    "Choose your level",
                    getWidth() / 2f,
                    125,
                    23,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            int size = 75;
            int gap = 12;
            int startY = 155;

            for (int i = 1; i <= 30; i++) {

                int column = (i - 1) % 3;
                int row = (i - 1) / 3;

                float left = 25 + column * (size + gap);
                float top = startY + row * (size + gap);

                boolean unlocked = i <= level;

                roundRect(
                        canvas,
                        left,
                        top,
                        left + size,
                        top + size,
                        18,
                        unlocked
                                ? Color.rgb(255, 185, 70)
                                : Color.rgb(205, 205, 210)
                );

                drawText(
                        canvas,
                        unlocked ? String.valueOf(i) : "🔒",
                        left + size / 2f,
                        top + size / 2f + 8,
                        unlocked ? 25 : 20,
                        Color.WHITE,
                        Paint.Align.CENTER
                );
            }

            drawText(
                    canvas,
                    "Complete levels to unlock more fun!",
                    getWidth() / 2f,
                    getHeight() - 25,
                    15,
                    Color.GRAY,
                    Paint.Align.CENTER
            );
        }

        void startGame(int selectedLevel) {

            level = Math.max(
                    1,
                    Math.min(30, selectedLevel)
            );

            score = 0;
            collected = 0;
            combo = 0;

            target = 18 + level * 3;

            timeLeft = Math.max(
                    35,
                    65 - level
            );

            paused = false;

            createBoard();

            screen = GAME;

            handler.removeCallbacks(timer);
            handler.postDelayed(timer, 1000);

            invalidate();
        }

        void createBoard() {

            for (int row = 0; row < ROWS; row++) {

                for (int column = 0;
                     column < COLS;
                     column++) {

                    board[row][column] =
                            createSafeCandy(row, column);
                }
            }

            if (!hasPossibleMoves()) {
                shuffleBoard();
            }
        }

        int createSafeCandy(int row, int column) {

            int value;

            do {
                value =
                        random.nextInt(
                                candyColors.length
                        );

            } while (
                    column >= 2
                            && board[row][column - 1] == value
                            && board[row][column - 2] == value

                    ||

                    row >= 2
                            && board[row - 1][column] == value
                            && board[row - 2][column] == value
            );

            return value;
        }

        void drawGame(Canvas canvas) {

            drawHeader(
                    canvas,
                    "LEVEL " + level
            );

            drawText(
                    canvas,
                    "TIME: " + timeLeft,
                    20,
                    125,
                    18,
                    Color.DKGRAY,
                    Paint.Align.LEFT
            );

            drawText(
                    canvas,
                    "SCORE: " + score,
                    getWidth() / 2f,
                    125,
                    18,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "Ⅱ",
                    getWidth() - 35,
                    125,
                    24,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "TARGET " + collected + "/" + target,
                    getWidth() / 2f,
                    155,
                    17,
                    Color.rgb(245, 80, 110),
                    Paint.Align.CENTER
            );

            float boardSize =
                    Math.min(
                            getWidth() - 24,
                            getHeight() - 260
                    );

            float cell =
                    boardSize / COLS;

            float left =
                    (getWidth() - boardSize) / 2f;

            float top = 175;

            roundRect(
                    canvas,
                    left - 5,
                    top - 5,
                    left + boardSize + 5,
                    top + boardSize + 5,
                    20,
                    Color.rgb(255, 225, 240)
            );

            for (int row = 0; row < ROWS; row++) {

                for (int column = 0;
                     column < COLS;
                     column++) {

                    float centerX =
                            left
                                    + column * cell
                                    + cell / 2f;

                    float centerY =
                            top
                                    + row * cell
                                    + cell / 2f;

                    drawCandy(
                            canvas,
                            centerX,
                            centerY,
                            cell * 0.38f,
                            board[row][column]
                    );
                }
            }

            drawText(
                    canvas,
                    "SWIPE • MATCH 3 OR MORE",
                    getWidth() / 2f,
                    getHeight() - 65,
                    15,
                    Color.GRAY,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "COMBO x" + Math.max(1, combo),
                    getWidth() / 2f,
                    getHeight() - 35,
                    18,
                    Color.rgb(245, 110, 40),
                    Paint.Align.CENTER
            );

            if (paused) {
                drawPause(canvas);
            }
        }

        void drawCandy(Canvas canvas,
                       float x,
                       float y,
                       float radius,
                       int type) {

            int color = candyColors[type];

            p.setStyle(Paint.Style.FILL);
            p.setColor(color);

            if (type == 0) {

                canvas.drawCircle(
                        x,
                        y,
                        radius,
                        p
                );

                canvas.drawCircle(
                        x - radius * .45f,
                        y - radius * .65f,
                        radius * .45f,
                        p
                );

                canvas.drawCircle(
                        x + radius * .45f,
                        y - radius * .65f,
                        radius * .45f,
                        p
                );

            } else if (type == 1) {

                canvas.drawCircle(
                        x,
                        y,
                        radius,
                        p
                );

            } else if (type == 2) {

                Path star = new Path();

                for (int i = 0; i < 10; i++) {

                    double angle =
                            -Math.PI / 2
                                    + i * Math.PI / 5;

                    float r =
                            i % 2 == 0
                                    ? radius
                                    : radius * .45f;

                    float px =
                            x
                                    + (float)
                                    Math.cos(angle) * r;

                    float py =
                            y
                                    + (float)
                                    Math.sin(angle) * r;

                    if (i == 0) {
                        star.moveTo(px, py);
                    } else {
                        star.lineTo(px, py);
                    }
                }

                star.close();

                canvas.drawPath(star, p);

            } else if (type == 3) {

                canvas.drawRoundRect(
                        x - radius,
                        y - radius,
                        x + radius,
                        y + radius,
                        radius * .3f,
                        radius * .3f,
                        p
                );

            } else if (type == 4) {

                Path diamond = new Path();

                diamond.moveTo(
                        x,
                        y - radius
                );

                diamond.lineTo(
                        x + radius,
                        y
                );

                diamond.lineTo(
                        x,
                        y + radius
                );

                diamond.lineTo(
                        x - radius,
                        y
                );

                diamond.close();

                canvas.drawPath(
                        diamond,
                        p
                );

            } else {

                canvas.drawCircle(
                        x,
                        y,
                        radius,
                        p
                );

                // FIXED: set the Paint color before drawCircle().
                p.setColor(lighten(color));

                canvas.drawCircle(
                        x,
                        y,
                        radius * .55f,
                        p
                );
            }

            p.setColor(Color.WHITE);

            canvas.drawCircle(
                    x - radius * .3f,
                    y - radius * .35f,
                    radius * .13f,
                    p
            );
        }

        int lighten(int color) {

            int red =
                    Math.min(
                            255,
                            Color.red(color) + 40
                    );

            int green =
                    Math.min(
                            255,
                            Color.green(color) + 40
                    );

            int blue =
                    Math.min(
                            255,
                            Color.blue(color) + 40
                    );

            return Color.rgb(
                    red,
                    green,
                    blue
            );
        }

        void drawPause(Canvas canvas) {

            p.setColor(
                    Color.argb(
                            190,
                            0,
                            0,
                            0
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            drawText(
                    canvas,
                    "PAUSED",
                    getWidth() / 2f,
                    230,
                    35,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            button(
                    canvas,
                    "RESUME",
                    70,
                    275,
                    getWidth() - 70,
                    335,
                    Color.rgb(70, 200, 105)
            );

            button(
                    canvas,
                    "RESTART",
                    70,
                    350,
                    getWidth() - 70,
                    410,
                    Color.rgb(80, 160, 245)
            );

            button(
                    canvas,
                    "MAIN MENU",
                    70,
                    425,
                    getWidth() - 70,
                    485,
                    Color.rgb(245, 90, 120)
            );
        }

        void drawComplete(Canvas canvas) {

            drawText(
                    canvas,
                    "🎉",
                    getWidth() / 2f,
                    125,
                    55,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "LEVEL " + level + " COMPLETE!",
                    getWidth() / 2f,
                    190,
                    28,
                    Color.rgb(245, 75, 110),
                    Paint.Align.CENTER
            );

            int stars = calculateStars();

            for (int i = 0; i < 3; i++) {

                drawText(
                        canvas,
                        i < stars ? "★" : "☆",
                        getWidth() / 2f
                                - 60
                                + i * 60,
                        255,
                        45,
                        Color.rgb(255, 190, 40),
                        Paint.Align.CENTER
                );
            }

            drawText(
                    canvas,
                    "Score: " + score,
                    getWidth() / 2f,
                    305,
                    21,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            button(
                    canvas,
                    "NEXT LEVEL",
                    60,
                    355,
                    getWidth() - 60,
                    415,
                    Color.rgb(70, 200, 105)
            );

            button(
                    canvas,
                    "REPLAY",
                    60,
                    430,
                    getWidth() - 60,
                    490,
                    Color.rgb(80, 160, 245)
            );

            button(
                    canvas,
                    "HOME",
                    60,
                    505,
                    getWidth() - 60,
                    565,
                    Color.rgb(245, 90, 120)
            );
        }

        int calculateStars() {

            if (score >= target * 70) {
                return 3;
            }

            if (score >= target * 40) {
                return 2;
            }

            return 1;
        }

        void completeLevel() {

            handler.removeCallbacks(timer);

            if (score > bestScore) {
                bestScore = score;

                prefs.edit()
                        .putInt("bestScore", bestScore)
                        .apply();
            }

            int unlocked =
                    prefs.getInt("level", 1);

            if (level >= unlocked
                    && level < 30) {

                prefs.edit()
                        .putInt("level", level + 1)
                        .apply();
            }

            screen = COMPLETE;

            invalidate();
        }

        void drawGameOver(Canvas canvas) {

            drawText(
                    canvas,
                    "GAME OVER",
                    getWidth() / 2f,
                    180,
                    36,
                    Color.rgb(245, 70, 100),
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "Score: " + score,
                    getWidth() / 2f,
                    245,
                    22,
                    Color.DKGRAY,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "Best: " + bestScore,
                    getWidth() / 2f,
                    285,
                    19,
                    Color.GRAY,
                    Paint.Align.CENTER
            );

            button(
                    canvas,
                    "PLAY AGAIN",
                    60,
                    350,
                    getWidth() - 60,
                    415,
                    Color.rgb(70, 200, 105)
            );

            button(
                    canvas,
                    "MAIN MENU",
                    60,
                    430,
                    getWidth() - 60,
                    495,
                    Color.rgb(245, 90, 120)
            );
        }

        void drawSettings(Canvas canvas) {

            drawHeader(
                    canvas,
                    "SETTINGS"
            );

            setting(
                    canvas,
                    "Music",
                    musicOn,
                    130
            );

            setting(
                    canvas,
                    "Sound Effects",
                    soundOn,
                    205
            );

            setting(
                    canvas,
                    "Vibration",
                    vibrationOn,
                    280
            );

            button(
                    canvas,
                    "BACK",
                    70,
                    380,
                    getWidth() - 70,
                    440,
                    Color.rgb(80, 160, 245)
            );

            drawText(
                    canvas,
                    "CandyJolt",
                    getWidth() / 2f,
                    520,
                    25,
                    Color.rgb(245, 80, 110),
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "Keep Playing • Keep Smiling!",
                    getWidth() / 2f,
                    550,
                    15,
                    Color.GRAY,
                    Paint.Align.CENTER
            );
        }

        void setting(Canvas canvas,
                     String name,
                     boolean enabled,
                     int y) {

            drawText(
                    canvas,
                    name,
                    40,
                    y,
                    20,
                    Color.DKGRAY,
                    Paint.Align.LEFT
            );

            button(
                    canvas,
                    enabled ? "ON" : "OFF",
                    getWidth() - 120,
                    y - 30,
                    getWidth() - 35,
                    y + 10,
                    enabled
                            ? Color.rgb(70, 200, 105)
                            : Color.rgb(170, 170, 175)
            );
        }

        void drawScores(Canvas canvas) {

            drawHeader(
                    canvas,
                    "HIGH SCORES"
            );

            drawText(
                    canvas,
                    "🏆",
                    getWidth() / 2f,
                    145,
                    50,
                    Color.WHITE,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "BEST SCORE",
                    getWidth() / 2f,
                    205,
                    20,
                    Color.GRAY,
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    String.valueOf(bestScore),
                    getWidth() / 2f,
                    255,
                    38,
                    Color.rgb(245, 90, 110),
                    Paint.Align.CENTER
            );

            drawText(
                    canvas,
                    "1. You",
                    45,
                    330,
                    21,
                    Color.DKGRAY,
                    Paint.Align.LEFT
            );

            drawText(
                    canvas,
                    String.valueOf(bestScore),
                    getWidth() - 45,
                    330,
                    21,
                    Color.DKGRAY,
                    Paint.Align.RIGHT
            );

            drawText(
                    canvas,
                    "2. Candy Master",
                    45,
                    385,
                    19,
                    Color.GRAY,
                    Paint.Align.LEFT
            );

            drawText(
                    canvas,
                    "850",
                    getWidth() - 45,
                    385,
                    19,
                    Color.GRAY,
                    Paint.Align.RIGHT
            );

            drawText(
                    canvas,
                    "3. Sweet Player",
                    45,
                    440,
                    19,
                    Color.GRAY,
                    Paint.Align.LEFT
            );

            drawText(
                    canvas,
                    "720",
                    getWidth() - 45,
                    440,
                    19,
                    Color.GRAY,
                    Paint.Align.RIGHT
            );

            button(
                    canvas,
                    "BACK",
                    70,
                    500,
                    getWidth() - 70,
                    560,
                    Color.rgb(80, 160, 245)
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            float x = event.getX();
            float y = event.getY();

            if (event.getAction()
                    == MotionEvent.ACTION_DOWN) {

                downX = x;
                downY = y;

                return true;
            }

            if (event.getAction()
                    == MotionEvent.ACTION_UP) {

                float dx = x - downX;
                float dy = y - downY;

                if (screen == GAME
                        && !paused
                        && Math.max(
                        Math.abs(dx),
                        Math.abs(dy)
                ) > 35) {

                    handleSwipe(dx, dy);

                } else {

                    handleTap(x, y);
                }

                return true;
            }

            return true;
        }

        void handleTap(float x, float y) {

            if (screen == HOME) {

                if (y >= 245 && y <= 315) {

                    startGame(level);

                } else if (y >= 330 && y <= 390) {

                    screen = LEVELS;
                    invalidate();

                } else if (y >= 405 && y <= 465) {

                    screen = SCORES;
                    invalidate();

                } else if (y >= 480 && y <= 540) {

                    screen = SETTINGS;
                    invalidate();

                } else if (y > 550) {

                    musicOn = !musicOn;

                    prefs.edit()
                            .putBoolean(
                                    "music",
                                    musicOn
                            )
                            .apply();

                    invalidate();
                }

            } else if (screen == LEVELS) {

                if (y < 90) {

                    screen = HOME;
                    invalidate();
                    return;
                }

                int size = 75;
                int gap = 12;
                int startY = 155;

                for (int i = 1; i <= 30; i++) {

                    int column =
                            (i - 1) % 3;

                    int row =
                            (i - 1) / 3;

                    float left =
                            25 + column
                                    * (size + gap);

                    float top =
                            startY + row
                                    * (size + gap);

                    if (x >= left
                            && x <= left + size
                            && y >= top
                            && y <= top + size
                            && i <= level) {

                        startGame(i);
                        return;
                    }
                }

            } else if (screen == GAME) {

                if (paused) {

                    if (y >= 275
                            && y <= 335) {

                        paused = false;
                        invalidate();

                    } else if (y >= 350
                            && y <= 410) {

                        startGame(level);

                    } else if (y >= 425
                            && y <= 485) {

                        paused = false;
                        screen = HOME;

                        handler.removeCallbacks(timer);

                        invalidate();
                    }

                } else if (y < 150
                        && x > getWidth() - 80) {

                    paused = true;
                    invalidate();
                }

            } else if (screen == COMPLETE) {

                if (y >= 355
                        && y <= 415) {

                    startGame(
                            Math.min(
                                    30,
                                    level + 1
                            )
                    );

                } else if (y >= 430
                        && y <= 490) {

                    startGame(level);

                } else if (y >= 505
                        && y <= 565) {

                    screen = HOME;
                    invalidate();
                }

            } else if (screen == GAMEOVER) {

                if (y >= 350
                        && y <= 415) {

                    startGame(level);

                } else if (y >= 430
                        && y <= 495) {

                    screen = HOME;
                    invalidate();
                }

            } else if (screen == SETTINGS) {

                if (y >= 100
                        && y <= 170) {

                    musicOn = !musicOn;

                } else if (y >= 175
                        && y <= 245) {

                    soundOn = !soundOn;

                } else if (y >= 250
                        && y <= 320) {

                    vibrationOn = !vibrationOn;

                } else if (y >= 360
                        && y <= 450) {

                    screen = HOME;
                }

                prefs.edit()
                        .putBoolean(
                                "music",
                                musicOn
                        )
                        .putBoolean(
                                "sound",
                                soundOn
                        )
                        .putBoolean(
                                "vibration",
                                vibrationOn
                        )
                        .apply();

                invalidate();

            } else if (screen == SCORES) {

                if (y >= 490
                        && y <= 580) {

                    screen = HOME;
                    invalidate();
                }
            }
        }

        void handleSwipe(float dx, float dy) {

            float boardSize =
                    Math.min(
                            getWidth() - 24,
                            getHeight() - 260
                    );

            float cell =
                    boardSize / COLS;

            float left =
                    (getWidth() - boardSize) / 2f;

            float top = 175;

            int column =
                    (int) ((downX - left) / cell);

            int row =
                    (int) ((downY - top) / cell);

            if (row < 0
                    || row >= ROWS
                    || column < 0
                    || column >= COLS) {
                return;
            }

            int newRow = row;
            int newColumn = column;

            if (Math.abs(dx)
                    > Math.abs(dy)) {

                newColumn +=
                        dx > 0 ? 1 : -1;

            } else {

                newRow +=
                        dy > 0 ? 1 : -1;
            }

            if (newRow < 0
                    || newRow >= ROWS
                    || newColumn < 0
                    || newColumn >= COLS) {
                return;
            }

            swap(
                    row,
                    column,
                    newRow,
                    newColumn
            );

            if (findMatches().isEmpty()) {

                swap(
                        row,
                        column,
                        newRow,
                        newColumn
                );

            } else {

                combo = 0;
                resolveBoard();
            }

            invalidate();
        }

        void swap(
                int row1,
                int column1,
                int row2,
                int column2) {

            int temp =
                    board[row1][column1];

            board[row1][column1] =
                    board[row2][column2];

            board[row2][column2] =
                    temp;
        }

        Set<String> findMatches() {

            Set<String> matches =
                    new HashSet<>();

            // Horizontal matches
            for (int row = 0;
                 row < ROWS;
                 row++) {

                int start = 0;

                for (int column = 1;
                     column <= COLS;
                     column++) {

                    boolean same =
                            column < COLS
                                    && board[row][column]
                                    == board[row][start];

                    if (!same) {

                        int length =
                                column - start;

                        if (length >= 3) {

                            for (int c = start;
                                 c < column;
                                 c++) {

                                matches.add(
                                        row + "," + c
                                );
                            }
                        }

                        start = column;
                    }
                }
            }

            // Vertical matches
            for (int column = 0;
                 column < COLS;
                 column++) {

                int start = 0;

                for (int row = 1;
                     row <= ROWS;
                     row++) {

                    boolean same =
                            row < ROWS
                                    && board[row][column]
                                    == board[start][column];

                    if (!same) {

                        int length =
                                row - start;

                        if (length >= 3) {

                            for (int r = start;
                                 r < row;
                                 r++) {

                                matches.add(
                                        r + "," + column
                                );
                            }
                        }

                        start = row;
                    }
                }
            }

            return matches;
        }

        void resolveBoard() {

            Set<String> matches;

            while (!(matches =
                    findMatches()).isEmpty()) {

                combo++;

                int amount =
                        matches.size();

                score +=
                        amount * 20 * combo;

                collected += amount;

                if (vibrationOn) {

                    try {

                        Vibrator vibrator =
                                (Vibrator)
                                        getContext()
                                                .getSystemService(
                                                        Context.VIBRATOR_SERVICE
                                                );

                        if (vibrator != null) {
                            vibrator.vibrate(35);
                        }

                    } catch (Exception ignored) {
                    }
                }

                for (String key : matches) {

                    String[] parts =
                            key.split(",");

                    int row =
                            Integer.parseInt(
                                    parts[0]
                            );

                    int column =
                            Integer.parseInt(
                                    parts[1]
                            );

                    board[row][column] = -1;
                }

                dropCandies();

                if (collected >= target) {
                    completeLevel();
                    return;
                }
            }

            if (!hasPossibleMoves()) {
                shuffleBoard();
            }
        }

        void dropCandies() {

            for (int column = 0;
                 column < COLS;
                 column++) {

                int write =
                        ROWS - 1;

                for (int row = ROWS - 1;
                     row >= 0;
                     row--) {

                    if (board[row][column] != -1) {

                        board[write][column] =
                                board[row][column];

                        write--;
                    }
                }

                while (write >= 0) {

                    board[write][column] =
                            random.nextInt(
                                    candyColors.length
                            );

                    write--;
                }
            }
        }

        boolean hasPossibleMoves() {

            for (int row = 0;
                 row < ROWS;
                 row++) {

                for (int column = 0;
                     column < COLS;
                     column++) {

                    if (column + 1 < COLS) {

                        swap(
                                row,
                                column,
                                row,
                                column + 1
                        );

                        boolean possible =
                                !findMatches()
                                        .isEmpty();

                        swap(
                                row,
                                column,
                                row,
                                column + 1
                        );

                        if (possible) {
                            return true;
                        }
                    }

                    if (row + 1 < ROWS) {

                        swap(
                                row,
                                column,
                                row + 1,
                                column
                        );

                        boolean possible =
                                !findMatches()
                                        .isEmpty();

                        swap(
                                row,
                                column,
                                row + 1,
                                column
                        );

                        if (possible) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        void shuffleBoard() {

            ArrayList<Integer> values =
                    new ArrayList<>();

            for (int row = 0;
                 row < ROWS;
                 row++) {

                for (int column = 0;
                     column < COLS;
                     column++) {

                    values.add(
                            board[row][column]
                    );
                }
            }

            do {

                Collections.shuffle(
                        values,
                        random
                );

                int index = 0;

                for (int row = 0;
                     row < ROWS;
                     row++) {

                    for (int column = 0;
                         column < COLS;
                         column++) {

                        board[row][column] =
                                values.get(index++);
                    }
                }

            } while (
                    !findMatches().isEmpty()
                            || !hasPossibleMoves()
            );
        }

        @Override
        protected void onDetachedFromWindow() {

            handler.removeCallbacks(timer);

            super.onDetachedFromWindow();
        }
    }
}