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

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        Random random = new Random();
        Handler handler = new Handler();
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

        float downX, downY;
        boolean moving;

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

        String[] candyNames = {
                "Strawberry",
                "Blueberry",
                "Lemon",
                "Apple",
                "Grape",
                "Orange"
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
                        }
                    }

                    invalidate();
                    handler.postDelayed(this, 1000);
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

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(4);
        }

        void text(Canvas c, String s, float x, float y, float size, int color,
                  Paint.Align align) {
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(size);
            p.setColor(color);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextAlign(align);
            c.drawText(s, x, y, p);
        }

        void roundRect(Canvas c, float l, float t, float r, float b,
                       float radius, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(l, t, r, b, radius, radius, p);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            c.drawColor(Color.rgb(255, 244, 250));

            switch (screen) {
                case HOME:
                    drawHome(c);
                    break;
                case LEVELS:
                    drawLevels(c);
                    break;
                case GAME:
                    drawGame(c);
                    break;
                case COMPLETE:
                    drawComplete(c);
                    break;
                case GAMEOVER:
                    drawGameOver(c);
                    break;
                case SETTINGS:
                    drawSettings(c);
                    break;
                case SCORES:
                    drawScores(c);
                    break;
            }
        }

        void drawHeader(Canvas c, String title) {
            roundRect(c, 0, 0, getWidth(), 90, 0,
                    Color.rgb(255, 92, 135));

            text(c, "CANDYJOLT", 25, 43, 25,
                    Color.WHITE, Paint.Align.LEFT);

            text(c, title, getWidth() - 25, 43, 18,
                    Color.WHITE, Paint.Align.RIGHT);
        }

        void drawHome(Canvas c) {
            text(c, "🍬", getWidth() / 2f - 60, 115, 55,
                    Color.WHITE, Paint.Align.CENTER);
            text(c, "🍭", getWidth() / 2f, 115, 55,
                    Color.WHITE, Paint.Align.CENTER);
            text(c, "🍬", getWidth() / 2f + 60, 115, 55,
                    Color.WHITE, Paint.Align.CENTER);

            text(c, "CandyJolt", getWidth() / 2f, 175, 42,
                    Color.rgb(245, 60, 105), Paint.Align.CENTER);

            text(c, "Small Game • Big Smiles", getWidth() / 2f,
                    205, 18, Color.DKGRAY, Paint.Align.CENTER);

            button(c, "PLAY", 80, 245, getWidth() - 80, 315,
                    Color.rgb(70, 200, 105));

            button(c, "LEVELS", 80, 330, getWidth() - 80, 390,
                    Color.rgb(80, 160, 245));

            button(c, "HIGH SCORES", 80, 405, getWidth() - 80, 465,
                    Color.rgb(175, 100, 235));

            button(c, "SETTINGS", 80, 480, getWidth() - 80, 540,
                    Color.rgb(255, 165, 55));

            text(c, musicOn ? "🔊" : "🔇",
                    getWidth() - 40, 590, 28,
                    Color.DKGRAY, Paint.Align.CENTER);
        }

        void button(Canvas c, String label, float l, float t,
                    float r, float b, int color) {
            roundRect(c, l, t, r, b, 25, color);
            text(c, label, (l + r) / 2f,
                    (t + b) / 2f + 8,
                    21, Color.WHITE, Paint.Align.CENTER);
        }

        void drawLevels(Canvas c) {
            drawHeader(c, "LEVELS");

            text(c, "Choose your level", getWidth() / 2f,
                    125, 23, Color.DKGRAY, Paint.Align.CENTER);

            int startY = 155;
            int size = 75;
            int gap = 12;

            for (int i = 1; i <= 30; i++) {
                int col = (i - 1) % 3;
                int row = (i - 1) / 3;

                float l = 25 + col * (size + gap);
                float t = startY + row * (size + gap);
                float r = l + size;
                float b = t + size;

                boolean unlocked = i <= level;

                roundRect(c, l, t, r, b, 18,
                        unlocked
                                ? Color.rgb(255, 185, 70)
                                : Color.rgb(205, 205, 210));

                text(c, unlocked ? "" + i : "🔒",
                        (l + r) / 2f,
                        (t + b) / 2f + 8,
                        unlocked ? 25 : 20,
                        Color.WHITE,
                        Paint.Align.CENTER);
            }

            text(c, "Complete levels to unlock more fun!",
                    getWidth() / 2f, getHeight() - 25,
                    15, Color.GRAY, Paint.Align.CENTER);
        }

        void startGame(int chosenLevel) {
            level = Math.max(1, Math.min(30, chosenLevel));
            score = 0;
            collected = 0;
            combo = 0;
            target = 18 + level * 3;
            timeLeft = Math.max(35, 65 - level);
            paused = false;

            createBoard();

            screen = GAME;
            handler.removeCallbacks(timer);
            handler.postDelayed(timer, 1000);

            invalidate();
        }

        void createBoard() {
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    board[r][col] = createSafeCandy(r, col);
                }
            }

            if (!hasPossibleMoves()) {
                shuffleBoard();
            }
        }

        int createSafeCandy(int r, int col) {
            int value;

            do {
                value = random.nextInt(candyColors.length);
            } while (
                    (col >= 2 &&
                            board[r][col - 1] == value &&
                            board[r][col - 2] == value)
                    ||
                    (r >= 2 &&
                            board[r - 1][col] == value &&
                            board[r - 2][col] == value)
            );

            return value;
        }

        void drawGame(Canvas c) {
            drawHeader(c, "LEVEL " + level);

            text(c, "TIME: " + timeLeft,
                    20, 125, 18, Color.DKGRAY, Paint.Align.LEFT);

            text(c, "SCORE: " + score,
                    getWidth() / 2f, 125,
                    18, Color.DKGRAY, Paint.Align.CENTER);

            text(c, "Ⅱ",
                    getWidth() - 35, 125,
                    24, Color.DKGRAY, Paint.Align.CENTER);

            text(c, "TARGET " + collected + "/" + target,
                    getWidth() / 2f, 155,
                    17, Color.rgb(245, 80, 110),
                    Paint.Align.CENTER);

            float boardSize = Math.min(
                    getWidth() - 24,
                    getHeight() - 260
            );

            float cell = boardSize / COLS;
            float left = (getWidth() - boardSize) / 2f;
            float top = 175;

            roundRect(c, left - 5, top - 5,
                    left + boardSize + 5,
                    top + boardSize + 5,
                    20,
                    Color.rgb(255, 225, 240));

            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {

                    float cx = left + col * cell + cell / 2f;
                    float cy = top + r * cell + cell / 2f;

                    drawCandy(c, cx, cy,
                            cell * 0.38f,
                            board[r][col]);
                }
            }

            text(c, "SWIPE • MATCH 3 OR MORE",
                    getWidth() / 2f,
                    getHeight() - 65,
                    15, Color.GRAY,
                    Paint.Align.CENTER);

            text(c, "COMBO x" + Math.max(1, combo),
                    getWidth() / 2f,
                    getHeight() - 35,
                    18,
                    Color.rgb(245, 110, 40),
                    Paint.Align.CENTER);

            if (paused) {
                drawPause(c);
            }
        }

        void drawCandy(Canvas c, float x, float y, float radius, int type) {
            int color = candyColors[type];

            p.setStyle(Paint.Style.FILL);
            p.setColor(color);

            switch (type) {
                case 0:
                    c.drawCircle(x, y, radius, p);
                    c.drawCircle(x - radius * .45f,
                            y - radius * .65f,
                            radius * .45f, p);
                    c.drawCircle(x + radius * .45f,
                            y - radius * .65f,
                            radius * .45f, p);
                    break;

                case 1:
                    c.drawCircle(x, y, radius, p);
                    break;

                case 2:
                    Path star = new Path();
                    for (int i = 0; i < 10; i++) {
                        double a = -Math.PI / 2 + i * Math.PI / 5;
                        float rr = (i % 2 == 0)
                                ? radius
                                : radius * .45f;
                        float px = x + (float) Math.cos(a) * rr;
                        float py = y + (float) Math.sin(a) * rr;

                        if (i == 0) star.moveTo(px, py);
                        else star.lineTo(px, py);
                    }
                    star.close();
                    c.drawPath(star, p);
                    break;

                case 3:
                    c.drawRoundRect(
                            x - radius, y - radius,
                            x + radius, y + radius,
                            radius * .3f, radius * .3f, p);
                    break;

                case 4:
                    Path diamond = new Path();
                    diamond.moveTo(x, y - radius);
                    diamond.lineTo(x + radius, y);
                    diamond.lineTo(x, y + radius);
                    diamond.lineTo(x - radius, y);
                    diamond.close();
                    c.drawPath(diamond, p);
                    break;

                default:
                    c.drawCircle(x, y, radius, p);
                    c.drawCircle(x, y,
                            radius * .55f,
                            lighten(color),
                            p);
                    break;
            }

            p.setColor(Color.WHITE);
            c.drawCircle(x - radius * .3f,
                    y - radius * .35f,
                    radius * .13f, p);
        }

        int lighten(int color) {
            int r = Math.min(255, Color.red(color) + 40);
            int g = Math.min(255, Color.green(color) + 40);
            int b = Math.min(255, Color.blue(color) + 40);
            return Color.rgb(r, g, b);
        }

        void drawPause(Canvas c) {
            p.setColor(Color.argb(190, 0, 0, 0));
            c.drawRect(0, 0, getWidth(), getHeight(), p);

            text(c, "PAUSED", getWidth() / 2f,
                    230, 35, Color.WHITE,
                    Paint.Align.CENTER);

            button(c, "RESUME", 70, 275,
                    getWidth() - 70, 335,
                    Color.rgb(70, 200, 105));

            button(c, "RESTART", 70, 350,
                    getWidth() - 70, 410,
                    Color.rgb(80, 160, 245));

            button(c, "MAIN MENU", 70, 425,
                    getWidth() - 70, 485,
                    Color.rgb(245, 90, 120));
        }

        void drawComplete(Canvas c) {
            text(c, "🎉", getWidth() / 2f,
                    125, 55, Color.WHITE,
                    Paint.Align.CENTER);

            text(c, "LEVEL " + level + " COMPLETE!",
                    getWidth() / 2f, 190,
                    28, Color.rgb(245, 75, 110),
                    Paint.Align.CENTER);

            int stars = calculateStars();

            text(c,
                    stars >= 1 ? "★" : "☆",
                    getWidth() / 2f - 60,
                    255, 45,
                    Color.rgb(255, 190, 40),
                    Paint.Align.CENTER);

            text(c,
                    stars >= 2 ? "★" : "☆",
                    getWidth() / 2f,
                    255, 45,
                    Color.rgb(255, 190, 40),
                    Paint.Align.CENTER);

            text(c,
                    stars >= 3 ? "★" : "☆",
                    getWidth() / 2f + 60,
                    255, 45,
                    Color.rgb(255, 190, 40),
                    Paint.Align.CENTER);

            text(c, "Score: " + score,
                    getWidth() / 2f, 305,
                    21, Color.DKGRAY,
                    Paint.Align.CENTER);

            button(c, "NEXT LEVEL", 60, 355,
                    getWidth() - 60, 415,
                    Color.rgb(70, 200, 105));

            button(c, "REPLAY", 60, 430,
                    getWidth() - 60, 490,
                    Color.rgb(80, 160, 245));

            button(c, "HOME", 60, 505,
                    getWidth() - 60, 565,
                    Color.rgb(245, 90, 120));
        }

        int calculateStars() {
            if (score >= target * 70) return 3;
            if (score >= target * 40) return 2;
            return 1;
        }

        void completeLevel() {
            handler.removeCallbacks(timer);

            if (score > bestScore) {
                bestScore = score;
                prefs.edit().putInt("bestScore", bestScore).apply();
            }

            if (level < 30 && level >= prefs.getInt("level", 1)) {
                prefs.edit().putInt("level", level + 1).apply();
            }

            screen = COMPLETE;
            invalidate();
        }

        void drawGameOver(Canvas c) {
            text(c, "GAME OVER",
                    getWidth() / 2f, 180,
                    36, Color.rgb(245, 70, 100),
                    Paint.Align.CENTER);

            text(c, "Score: " + score,
                    getWidth() / 2f, 245,
                    22, Color.DKGRAY,
                    Paint.Align.CENTER);

            text(c, "Best: " + bestScore,
                    getWidth() / 2f, 285,
                    19, Color.GRAY,
                    Paint.Align.CENTER);

            button(c, "PLAY AGAIN", 60, 350,
                    getWidth() - 60, 415,
                    Color.rgb(70, 200, 105));

            button(c, "MAIN MENU", 60, 430,
                    getWidth() - 60, 495,
                    Color.rgb(245, 90, 120));
        }

        void drawSettings(Canvas c) {
            drawHeader(c, "SETTINGS");

            setting(c, "Music", musicOn, 130);
            setting(c, "Sound Effects", soundOn, 205);
            setting(c, "Vibration", vibrationOn, 280);

            button(c, "BACK", 70, 380,
                    getWidth() - 70, 440,
                    Color.rgb(80, 160, 245));

            text(c, "CandyJolt",
                    getWidth() / 2f, 520,
                    25, Color.rgb(245, 80, 110),
                    Paint.Align.CENTER);

            text(c, "Keep Playing • Keep Smiling!",
                    getWidth() / 2f, 550,
                    15, Color.GRAY,
                    Paint.Align.CENTER);
        }

        void setting(Canvas c, String name, boolean value, int y) {
            text(c, name, 40, y,
                    20, Color.DKGRAY,
                    Paint.Align.LEFT);

            button(c, value ? "ON" : "OFF",
                    getWidth() - 120, y - 30,
                    getWidth() - 35, y + 10,
                    value
                            ? Color.rgb(70, 200, 105)
                            : Color.rgb(170, 170, 175));
        }

        void drawScores(Canvas c) {
            drawHeader(c, "HIGH SCORES");

            text(c, "🏆",
                    getWidth() / 2f, 145,
                    50, Color.WHITE,
                    Paint.Align.CENTER);

            text(c, "BEST SCORE",
                    getWidth() / 2f, 205,
                    20, Color.GRAY,
                    Paint.Align.CENTER);

            text(c, "" + bestScore,
                    getWidth() / 2f, 255,
                    38, Color.rgb(245, 90, 110),
                    Paint.Align.CENTER);

            text(c, "1. You", 45, 330,
                    21, Color.DKGRAY,
                    Paint.Align.LEFT);

            text(c, "" + bestScore,
                    getWidth() - 45, 330,
                    21, Color.DKGRAY,
                    Paint.Align.RIGHT);

            text(c, "2. Candy Master", 45, 385,
                    19, Color.GRAY,
                    Paint.Align.LEFT);

            text(c, "850", getWidth() - 45, 385,
                    19, Color.GRAY,
                    Paint.Align.RIGHT);

            text(c, "3. Sweet Player", 45, 440,
                    19, Color.GRAY,
                    Paint.Align.LEFT);

            text(c, "720", getWidth() - 45, 440,
                    19, Color.GRAY,
                    Paint.Align.RIGHT);

            button(c, "BACK", 70, 500,
                    getWidth() - 70, 560,
                    Color.rgb(80, 160, 245));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            float x = event.getX();
            float y = event.getY();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX = x;
                downY = y;
                moving = false;
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {

                float dx = x - downX;
                float dy = y - downY;

                if (screen == GAME && !paused &&
                        Math.max(Math.abs(dx), Math.abs(dy)) > 35) {
                    handleSwipe(dx, dy);
                    return true;
                }

                handleTap(x, y);
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
                    prefs.edit().putBoolean("music", musicOn).apply();
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

                    int col = (i - 1) % 3;
                    int row = (i - 1) / 3;

                    float l = 25 + col * (size + gap);
                    float t = startY + row * (size + gap);
                    float r = l + size;
                    float b = t + size;

                    if (x >= l && x <= r &&
                            y >= t && y <= b &&
                            i <= level) {
                        startGame(i);
                        return;
                    }
                }

            } else if (screen == GAME) {

                if (paused) {

                    if (y >= 275 && y <= 335) {
                        paused = false;
                        invalidate();
                    } else if (y >= 350 && y <= 410) {
                        startGame(level);
                    } else if (y >= 425 && y <= 485) {
                        paused = false;
                        screen = HOME;
                        handler.removeCallbacks(timer);
                        invalidate();
                    }

                } else if (y < 150 && x > getWidth() - 80) {
                    paused = true;
                    invalidate();
                }

            } else if (screen == COMPLETE) {

                if (y >= 355 && y <= 415) {
                    startGame(Math.min(30, level + 1));
                } else if (y >= 430 && y <= 490) {
                    startGame(level);
                } else if (y >= 505 && y <= 565) {
                    screen = HOME;
                    invalidate();
                }

            } else if (screen == GAMEOVER) {

                if (y >= 350 && y <= 415) {
                    startGame(level);
                } else if (y >= 430 && y <= 495) {
                    screen = HOME;
                    invalidate();
                }

            } else if (screen == SETTINGS) {

                if (y >= 100 && y <= 170) {
                    musicOn = !musicOn;
                } else if (y >= 175 && y <= 245) {
                    soundOn = !soundOn;
                } else if (y >= 250 && y <= 320) {
                    vibrationOn = !vibrationOn;
                } else if (y >= 360 && y <= 450) {
                    screen = HOME;
                }

                prefs.edit()
                        .putBoolean("music", musicOn)
                        .putBoolean("sound", soundOn)
                        .putBoolean("vibration", vibrationOn)
                        .apply();

                invalidate();

            } else if (screen == SCORES) {

                if (y >= 490 && y <= 580) {
                    screen = HOME;
                    invalidate();
                }
            }
        }

        void handleSwipe(float dx, float dy) {

            float boardSize = Math.min(
                    getWidth() - 24,
                    getHeight() - 260
            );

            float cell = boardSize / COLS;
            float left = (getWidth() - boardSize) / 2f;
            float top = 175;

            int col = (int) ((downX - left) / cell);
            int row = (int) ((downY - top) / cell);

            if (row < 0 || row >= ROWS ||
                    col < 0 || col >= COLS) {
                return;
            }

            int newRow = row;
            int newCol = col;

            if (Math.abs(dx) > Math.abs(dy)) {
                newCol += dx > 0 ? 1 : -1;
            } else {
                newRow += dy > 0 ? 1 : -1;
            }

            if (newRow < 0 || newRow >= ROWS ||
                    newCol < 0 || newCol >= COLS) {
                return;
            }

            swap(row, col, newRow, newCol);

            if (findMatches().isEmpty()) {
                swap(row, col, newRow, newCol);
            } else {
                combo = 0;
                resolveBoard();
            }

            invalidate();
        }

        void swap(int r1, int c1, int r2, int c2) {
            int temp = board[r1][c1];
            board[r1][c1] = board[r2][c2];
            board[r2][c2] = temp;
        }

        Set<String> findMatches() {

            Set<String> matches = new HashSet<>();

            for (int r = 0; r < ROWS; r++) {

                int start = 0;

                for (int c = 1; c <= COLS; c++) {

                    boolean same = c < COLS &&
                            board[r][c] == board[r][start];

                    if (!same) {

                        int length = c - start;

                        if (length >= 3) {
                            for (int x = start; x < c; x++) {
                                matches.add(r + "," + x);
                            }
                        }

                        start = c;
                    }
                }
            }

            for (int c = 0; c < COLS; c++) {

                int start = 0;

                for (int r = 1; r <= ROWS; r++) {

                    boolean same = r < ROWS &&
                            board[r][c] == board[start][c];

                    if (!same) {

                        int length = r - start;

                        if (length >= 3) {
                            for (int x = start; x < r; x++) {
                                matches.add(x + "," + c);
                            }
                        }

                        start = r;
                    }
                }
            }

            return matches;
        }

        void resolveBoard() {

            Set<String> matches;

            while (!(matches = findMatches()).isEmpty()) {

                combo++;

                int amount = matches.size();

                score += amount * 20 * combo;

                collected += amount;

                if (vibrationOn) {
                    try {
                        Vibrator vibrator =
                                (Vibrator) getContext()
                                        .getSystemService(Context.VIBRATOR_SERVICE);

                        if (vibrator != null) {
                            vibrator.vibrate(35);
                        }
                    } catch (Exception ignored) {
                    }
                }

                for (String key : matches) {
                    String[] parts = key.split(",");
                    int r = Integer.parseInt(parts[0]);
                    int c = Integer.parseInt(parts[1]);
                    board[r][c] = -1;
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

            for (int c = 0; c < COLS; c++) {

                int write = ROWS - 1;

                for (int r = ROWS - 1; r >= 0; r--) {

                    if (board[r][c] != -1) {
                        board[write][c] = board[r][c];
                        write--;
                    }
                }

                while (write >= 0) {
                    board[write][c] =
                            random.nextInt(candyColors.length);
                    write--;
                }
            }
        }

        boolean hasPossibleMoves() {

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {

                    if (c + 1 < COLS) {
                        swap(r, c, r, c + 1);

                        boolean possible =
                                !findMatches().isEmpty();

                        swap(r, c, r, c + 1);

                        if (possible) return true;
                    }

                    if (r + 1 < ROWS) {
                        swap(r, c, r + 1, c);

                        boolean possible =
                                !findMatches().isEmpty();

                        swap(r, c, r + 1, c);

                        if (possible) return true;
                    }
                }
            }

            return false;
        }

        void shuffleBoard() {

            ArrayList<Integer> values = new ArrayList<>();

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    values.add(board[r][c]);
                }
            }

            do {
                java.util.Collections.shuffle(values, random);

                int i = 0;

                for (int r = 0; r < ROWS; r++) {
                    for (int c = 0; c < COLS; c++) {
                        board[r][c] = values.get(i++);
                    }
                }

            } while (!findMatches().isEmpty() ||
                    !hasPossibleMoves());
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacks(timer);
            super.onDetachedFromWindow();
        }
    }
}