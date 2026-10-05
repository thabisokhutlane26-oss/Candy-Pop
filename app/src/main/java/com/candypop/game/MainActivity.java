package com.candypop.game;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MainActivity extends Activity {

    private CandyGameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameView = new CandyGameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (gameView != null) {
            gameView.stopTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gameView != null) {
            gameView.resumeTimerIfNeeded();
        }
    }

    // ============================================================
    // MAIN GAME VIEW
    // ============================================================

    public static class CandyGameView extends View {

        private static final int SCREEN_HOME = 0;
        private static final int SCREEN_LEVELS = 1;
        private static final int SCREEN_GAMEPLAY = 2;
        private static final int SCREEN_COMPLETE = 3;
        private static final int SCREEN_GAMEOVER = 4;
        private static final int SCREEN_SETTINGS = 5;
        private static final int SCREEN_SCORES = 6;

        private int screen = SCREEN_HOME;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final Handler handler = new Handler();

        private final SharedPreferences prefs;

        private int currentLevel = 1;
        private int score = 0;
        private int highScore = 0;
        private int timeLeft = 45;
        private int combo = 0;
        private int target = 12;
        private int collected = 0;

        private boolean paused = false;
        private boolean musicOn = true;
        private boolean soundOn = true;
        private boolean vibrationOn = true;

        private long lastTapTime = 0;

        private final ArrayList<Candy> candies = new ArrayList<>();

        private final int[] candyColors = {
                Color.rgb(245, 75, 100),
                Color.rgb(255, 205, 55),
                Color.rgb(80, 190, 110),
                Color.rgb(80, 145, 235),
                Color.rgb(180, 95, 220),
                Color.rgb(255, 145, 55)
        };

        private final String[] playerNames = {
                "SweetPlayer",
                "CandyMaster",
                "JoltKing",
                "SugarRush",
                "CandyStar"
        };

        private final int[] sampleScores = {
                2480,
                2150,
                1890,
                1540,
                1210
        };

        private final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (screen == SCREEN_GAMEPLAY && !paused) {

                    if (timeLeft > 0) {
                        timeLeft--;
                    }

                    if (timeLeft <= 0) {
                        finishGame(false);
                        return;
                    }

                    invalidate();
                    handler.postDelayed(this, 1000);
                }
            }
        };

        public CandyGameView(Context context) {
            super(context);

            prefs = context.getSharedPreferences(
                    "CandyJoltSettings",
                    Context.MODE_PRIVATE
            );

            highScore = prefs.getInt("highScore", 0);
            musicOn = prefs.getBoolean("music", true);
            soundOn = prefs.getBoolean("sound", true);
            vibrationOn = prefs.getBoolean("vibration", true);

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            setFocusable(true);
        }

        // ========================================================
        // TIMER
        // ========================================================

        private void startTimer() {
            handler.removeCallbacks(timerRunnable);
            handler.postDelayed(timerRunnable, 1000);
        }

        public void stopTimer() {
            handler.removeCallbacks(timerRunnable);
        }

        public void resumeTimerIfNeeded() {
            if (screen == SCREEN_GAMEPLAY && !paused) {
                handler.removeCallbacks(timerRunnable);
                handler.postDelayed(timerRunnable, 1000);
            }
        }

        // ========================================================
        // DRAW
        // ========================================================

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            canvas.drawColor(Color.rgb(250, 244, 255));

            switch (screen) {
                case SCREEN_HOME:
                    drawHome(canvas);
                    break;

                case SCREEN_LEVELS:
                    drawLevels(canvas);
                    break;

                case SCREEN_GAMEPLAY:
                    drawGameplay(canvas);
                    break;

                case SCREEN_COMPLETE:
                    drawComplete(canvas);
                    break;

                case SCREEN_GAMEOVER:
                    drawGameOver(canvas);
                    break;

                case SCREEN_SETTINGS:
                    drawSettings(canvas);
                    break;

                case SCREEN_SCORES:
                    drawHighScores(canvas);
                    break;
            }
        }

        // ========================================================
        // HOME
        // ========================================================

        private void drawHome(Canvas canvas) {

            drawBackground(canvas);

            drawTitle(
                    canvas,
                    "CandyJolt",
                    getWidth() / 2f,
                    105,
                    42
            );

            drawText(
                    canvas,
                    "Small Game • Big Smiles",
                    getWidth() / 2f,
                    145,
                    18,
                    Color.DKGRAY,
                    true
            );

            drawCandyLogo(canvas);

            float center = getWidth() / 2f;

            drawButton(
                    canvas,
                    "PLAY",
                    center,
                    390,
                    250,
                    65,
                    Color.rgb(85, 190, 110)
            );

            drawButton(
                    canvas,
                    "LEVELS",
                    center,
                    475,
                    250,
                    65,
                    Color.rgb(90, 145, 235)
            );

            drawButton(
                    canvas,
                    "HIGH SCORES",
                    center,
                    560,
                    250,
                    65,
                    Color.rgb(180, 105, 220)
            );

            drawButton(
                    canvas,
                    "SETTINGS",
                    center,
                    645,
                    250,
                    65,
                    Color.rgb(255, 165, 65)
            );

            drawMuteButton(canvas);
        }

        private void drawCandyLogo(Canvas canvas) {

            float cx = getWidth() / 2f;
            float cy = 245;

            paint.setColor(Color.rgb(245, 75, 100));
            canvas.drawCircle(cx - 65, cy, 34, paint);

            paint.setColor(Color.rgb(255, 205, 55));
            canvas.drawCircle(cx, cy - 20, 34, paint);

            paint.setColor(Color.rgb(80, 190, 110));
            canvas.drawCircle(cx + 65, cy, 34, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText("♥", cx - 65, cy + 9, paint);
            canvas.drawText("★", cx, cy - 11, paint);
            canvas.drawText("●", cx + 65, cy + 8, paint);
        }

        // ========================================================
        // LEVEL SELECT
        // ========================================================

        private void drawLevels(Canvas canvas) {

            drawBackground(canvas);

            drawHeader(canvas, "Level Select", true, true);

            int cardW = 105;
            int cardH = 92;

            int startY = 145;
            int gapX = 15;
            int gapY = 18;

            int totalW = cardW * 3 + gapX * 2;
            int startX = (getWidth() - totalW) / 2;

            for (int i = 1; i <= 12; i++) {

                int row = (i - 1) / 3;
                int col = (i - 1) % 3;

                float x = startX + col * (cardW + gapX);
                float y = startY + row * (cardH + gapY);

                boolean unlocked = i <= Math.min(currentLevel + 2, 12);

                drawLevelCard(
                        canvas,
                        i,
                        x,
                        y,
                        cardW,
                        cardH,
                        unlocked
                );
            }

            drawText(
                    canvas,
                    "Complete levels to unlock more fun!",
                    getWidth() / 2f,
                    getHeight() - 35,
                    15,
                    Color.DKGRAY,
                    true
            );
        }

        private void drawLevelCard(
                Canvas canvas,
                int level,
                float x,
                float y,
                float w,
                float h,
                boolean unlocked
        ) {

            paint.setColor(
                    unlocked
                            ? Color.WHITE
                            : Color.rgb(220, 220, 225)
            );

            canvas.drawRoundRect(
                    new RectF(x, y, x + w, y + h),
                    18,
                    18,
                    paint
            );

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(
                    unlocked
                            ? Color.rgb(150, 110, 220)
                            : Color.GRAY
            );

            canvas.drawRoundRect(
                    new RectF(x, y, x + w, y + h),
                    18,
                    18,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);

            drawText(
                    canvas,
                    String.valueOf(level),
                    x + w / 2,
                    y + 38,
                    27,
                    unlocked ? Color.rgb(70, 70, 80) : Color.GRAY,
                    true
            );

            if (unlocked) {

                int stars = getStars(level);

                String starText = "";

                for (int s = 0; s < 3; s++) {
                    starText += s < stars ? "★" : "☆";
                }

                drawText(
                        canvas,
                        starText,
                        x + w / 2,
                        y + 70,
                        20,
                        Color.rgb(255, 190, 45),
                        true
                );

            } else {

                drawText(
                        canvas,
                        "🔒",
                        x + w / 2,
                        y + 70,
                        18,
                        Color.GRAY,
                        true
                );
            }
        }

        private int getStars(int level) {

            if (level < currentLevel) {

                int saved =
                        prefs.getInt("stars_" + level, 0);

                if (saved > 0) {
                    return saved;
                }

                return 1;
            }

            return 0;
        }

        // ========================================================
        // GAMEPLAY
        // ========================================================

        private void drawGameplay(Canvas canvas) {

            drawBackground(canvas);

            drawGameHud(canvas);

            drawBoard(canvas);

            drawGameBottom(canvas);

            if (paused) {
                drawPauseOverlay(canvas);
            }
        }

        private void drawGameHud(Canvas canvas) {

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            12,
                            15,
                            getWidth() - 12,
                            92
                    ),
                    22,
                    22,
                    paint
            );

            drawText(
                    canvas,
                    "TIME",
                    55,
                    43,
                    13,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(timeLeft) + "s",
                    55,
                    72,
                    24,
                    timeLeft <= 10
                            ? Color.RED
                            : Color.rgb(65, 65, 75),
                    true
            );

            drawText(
                    canvas,
                    "SCORE",
                    getWidth() / 2f,
                    43,
                    13,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(score),
                    getWidth() / 2f,
                    72,
                    24,
                    Color.rgb(70, 70, 80),
                    true
            );

            drawButton(
                    canvas,
                    "PAUSE",
                    getWidth() - 62,
                    54,
                    82,
                    45,
                    Color.rgb(245, 105, 115)
            );
        }

        private void drawBoard(Canvas canvas) {

            float boardTop = 120;
            float boardBottom = getHeight() - 155;

            paint.setColor(Color.rgb(238, 226, 247));

            canvas.drawRoundRect(
                    new RectF(
                            15,
                            boardTop,
                            getWidth() - 15,
                            boardBottom
                    ),
                    25,
                    25,
                    paint
            );

            int columns = 5;
            int rows = 7;

            float boardW = getWidth() - 40;
            float cellW = boardW / columns;
            float cellH = (boardBottom - boardTop - 20) / rows;

            for (Candy candy : candies) {

                float cx = 20 + candy.col * cellW + cellW / 2;
                float cy = boardTop + 10
                        + candy.row * cellH
                        + cellH / 2;

                drawCandyTile(
                        canvas,
                        cx,
                        cy,
                        Math.min(cellW, cellH) * 0.36f,
                        candy
                );
            }
        }

        private void drawCandyTile(
                Canvas canvas,
                float cx,
                float cy,
                float size,
                Candy candy
        ) {

            paint.setColor(candy.color);

            switch (candy.type) {

                case 0:
                    canvas.drawCircle(
                            cx,
                            cy,
                            size,
                            paint
                    );

                    paint.setColor(Color.WHITE);
                    paint.setTextSize(size * 0.9f);
                    paint.setTextAlign(Paint.Align.CENTER);

                    canvas.drawText(
                            "♥",
                            cx,
                            cy + size * 0.32f,
                            paint
                    );
                    break;

                case 1:
                    PathUtil.drawDrop(
                            canvas,
                            cx,
                            cy,
                            size,
                            paint
                    );
                    break;

                case 2:
                    canvas.drawRoundRect(
                            new RectF(
                                    cx - size,
                                    cy - size * 0.75f,
                                    cx + size,
                                    cy + size * 0.75f
                            ),
                            size * 0.25f,
                            size * 0.25f,
                            paint
                    );

                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(5);
                    canvas.drawLine(
                            cx - size * .7f,
                            cy,
                            cx + size * .7f,
                            cy,
                            paint
                    );
                    break;

                case 3:
                    canvas.drawCircle(
                            cx,
                            cy,
                            size,
                            paint
                    );

                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(4);

                    canvas.drawLine(
                            cx - size,
                            cy,
                            cx + size,
                            cy,
                            paint
                    );

                    canvas.drawLine(
                            cx,
                            cy - size,
                            cx,
                            cy + size,
                            paint
                    );
                    break;
            }
        }

        private void drawGameBottom(Canvas canvas) {

            float y = getHeight() - 125;

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            15,
                            y,
                            getWidth() - 15,
                            getHeight() - 15
                    ),
                    22,
                    22,
                    paint
            );

            drawText(
                    canvas,
                    "TARGET",
                    90,
                    y + 30,
                    12,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    "♥  " + collected + "/" + target,
                    90,
                    y + 68,
                    22,
                    Color.rgb(245, 75, 100),
                    true
            );

            drawText(
                    canvas,
                    "COMBO",
                    getWidth() - 100,
                    y + 30,
                    12,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    combo + "x",
                    getWidth() - 100,
                    y + 68,
                    25,
                    Color.rgb(180, 100, 220),
                    true
            );
        }

        // ========================================================
        // COMPLETE
        // ========================================================

        private void drawComplete(Canvas canvas) {

            drawBackground(canvas);

            drawTitle(
                    canvas,
                    "LEVEL " + currentLevel + " COMPLETE!",
                    getWidth() / 2f,
                    110,
                    30
            );

            int stars = calculateStars();

            drawText(
                    canvas,
                    getStarString(stars),
                    getWidth() / 2f,
                    190,
                    45,
                    Color.rgb(255, 190, 45),
                    true
            );

            drawText(
                    canvas,
                    "Great job!",
                    getWidth() / 2f,
                    245,
                    23,
                    Color.DKGRAY,
                    true
            );

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            45,
                            285,
                            getWidth() - 45,
                            400
                    ),
                    24,
                    24,
                    paint
            );

            drawText(
                    canvas,
                    "FINAL SCORE",
                    getWidth() / 2f,
                    325,
                    14,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(score),
                    getWidth() / 2f,
                    370,
                    32,
                    Color.rgb(70, 70, 80),
                    true
            );

            drawButton(
                    canvas,
                    "NEXT LEVEL",
                    getWidth() / 2f,
                    470,
                    250,
                    60,
                    Color.rgb(85, 190, 110)
            );

            drawButton(
                    canvas,
                    "REPLAY",
                    getWidth() / 2f,
                    545,
                    250,
                    60,
                    Color.rgb(90, 145, 235)
            );

            drawButton(
                    canvas,
                    "HOME",
                    getWidth() / 2f,
                    620,
                    250,
                    60,
                    Color.rgb(180, 105, 220)
            );
        }

        // ========================================================
        // GAME OVER
        // ========================================================

        private void drawGameOver(Canvas canvas) {

            drawBackground(canvas);

            drawTitle(
                    canvas,
                    "Game Over",
                    getWidth() / 2f,
                    115,
                    38
            );

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            45,
                            180,
                            getWidth() - 45,
                            360
                    ),
                    24,
                    24,
                    paint
            );

            drawText(
                    canvas,
                    "YOUR SCORE",
                    getWidth() / 2f,
                    225,
                    14,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(score),
                    getWidth() / 2f,
                    270,
                    34,
                    Color.rgb(70, 70, 80),
                    true
            );

            drawText(
                    canvas,
                    "BEST SCORE",
                    getWidth() / 2f,
                    315,
                    14,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(highScore),
                    getWidth() / 2f,
                    345,
                    25,
                    Color.rgb(245, 155, 45),
                    true
            );

            drawButton(
                    canvas,
                    "PLAY AGAIN",
                    getWidth() / 2f,
                    450,
                    250,
                    65,
                    Color.rgb(85, 190, 110)
            );

            drawButton(
                    canvas,
                    "MAIN MENU",
                    getWidth() / 2f,
                    535,
                    250,
                    65,
                    Color.rgb(180, 105, 220)
            );
        }

        // ========================================================
        // SETTINGS
        // ========================================================

        private void drawSettings(Canvas canvas) {

            drawBackground(canvas);

            drawHeader(
                    canvas,
                    "Settings",
                    true,
                    false
            );

            float y = 145;

            drawSettingRow(
                    canvas,
                    "Music",
                    musicOn,
                    y
            );

            drawSettingRow(
                    canvas,
                    "Sound Effects",
                    soundOn,
                    y + 70
            );

            drawSettingRow(
                    canvas,
                    "Vibration",
                    vibrationOn,
                    y + 140
            );

            drawSettingTextRow(
                    canvas,
                    "Language",
                    "English",
                    y + 235
            );

            drawSettingTextRow(
                    canvas,
                    "Difficulty",
                    "Normal",
                    y + 305
            );

            drawText(
                    canvas,
                    "ABOUT",
                    35,
                    y + 410,
                    14,
                    Color.GRAY,
                    true
            );

            drawButton(
                    canvas,
                    "RATE APP",
                    getWidth() / 2f,
                    y + 465,
                    250,
                    52,
                    Color.rgb(255, 165, 65)
            );

            drawButton(
                    canvas,
                    "PRIVACY POLICY",
                    getWidth() / 2f,
                    y + 530,
                    250,
                    52,
                    Color.rgb(90, 145, 235)
            );

            drawButton(
                    canvas,
                    "MORE GAMES",
                    getWidth() / 2f,
                    y + 595,
                    250,
                    52,
                    Color.rgb(180, 105, 220)
            );
        }

        private void drawSettingRow(
                Canvas canvas,
                String name,
                boolean enabled,
                float y
        ) {

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            25,
                            y,
                            getWidth() - 25,
                            y + 55
                    ),
                    18,
                    18,
                    paint
            );

            drawText(
                    canvas,
                    name,
                    50,
                    y + 35,
                    17,
                    Color.DKGRAY,
                    true
            );

            drawText(
                    canvas,
                    enabled ? "ON" : "OFF",
                    getWidth() - 70,
                    y + 35,
                    15,
                    enabled
                            ? Color.rgb(70, 180, 100)
                            : Color.GRAY,
                    true
            );
        }

        private void drawSettingTextRow(
                Canvas canvas,
                String name,
                String value,
                float y
        ) {

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            25,
                            y,
                            getWidth() - 25,
                            y + 55
                    ),
                    18,
                    18,
                    paint
            );

            drawText(
                    canvas,
                    name,
                    50,
                    y + 35,
                    17,
                    Color.DKGRAY,
                    true
            );

            drawText(
                    canvas,
                    value,
                    getWidth() - 75,
                    y + 35,
                    15,
                    Color.rgb(100, 100, 110),
                    true
            );
        }

        // ========================================================
        // HIGH SCORES
        // ========================================================

        private void drawHighScores(Canvas canvas) {

            drawBackground(canvas);

            drawHeader(
                    canvas,
                    "High Scores",
                    true,
                    false
            );

            drawText(
                    canvas,
                    "♛",
                    getWidth() - 40,
                    48,
                    28,
                    Color.rgb(255, 190, 45),
                    true
            );

            drawButton(
                    canvas,
                    "GLOBAL",
                    getWidth() / 2f - 80,
                    115,
                    135,
                    48,
                    Color.rgb(90, 145, 235)
            );

            drawButton(
                    canvas,
                    "FRIENDS",
                    getWidth() / 2f + 80,
                    115,
                    135,
                    48,
                    Color.rgb(190, 190, 200)
            );

            float y = 190;

            for (int i = 0; i < 5; i++) {

                paint.setColor(Color.WHITE);

                canvas.drawRoundRect(
                        new RectF(
                                25,
                                y,
                                getWidth() - 25,
                                y + 68
                        ),
                        18,
                        18,
                        paint
                );

                drawText(
                        canvas,
                        "#" + (i + 1),
                        55,
                        y + 42,
                        19,
                        Color.rgb(80, 80, 90),
                        true
                );

                drawText(
                        canvas,
                        getFlag(i),
                        100,
                        y + 42,
                        20,
                        Color.DKGRAY,
                        true
                );

                drawText(
                        canvas,
                        playerNames[i],
                        145,
                        y + 42,
                        16,
                        Color.DKGRAY,
                        true
                );

                drawText(
                        canvas,
                        String.valueOf(sampleScores[i]),
                        getWidth() - 70,
                        y + 42,
                        17,
                        Color.rgb(180, 100, 220),
                        true
                );

                y += 82;
            }
        }

        private String getFlag(int index) {

            String[] flags = {
                    "🇱🇸",
                    "🇿🇦",
                    "🇧🇼",
                    "🇳🇦",
                    "🇿🇼"
            };

            return flags[index];
        }

        // ========================================================
        // PAUSE
        // ========================================================

        private void drawPauseOverlay(Canvas canvas) {

            paint.setColor(Color.argb(190, 20, 15, 35));

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    paint
            );

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            35,
                            120,
                            getWidth() - 35,
                            getHeight() - 100
                    ),
                    28,
                    28,
                    paint
            );

            drawText(
                    canvas,
                    "CandyJolt",
                    getWidth() / 2f,
                    175,
                    32,
                    Color.rgb(180, 105, 220),
                    true
            );

            drawText(
                    canvas,
                    "✕",
                    getWidth() - 65,
                    165,
                    27,
                    Color.DKGRAY,
                    true
            );

            drawButton(
                    canvas,
                    "RESUME",
                    getWidth() / 2f,
                    260,
                    230,
                    58,
                    Color.rgb(85, 190, 110)
            );

            drawButton(
                    canvas,
                    "RESTART",
                    getWidth() / 2f,
                    335,
                    230,
                    58,
                    Color.rgb(90, 145, 235)
            );

            drawButton(
                    canvas,
                    "SETTINGS",
                    getWidth() / 2f,
                    410,
                    230,
                    58,
                    Color.rgb(255, 165, 65)
            );

            drawButton(
                    canvas,
                    "MAIN MENU",
                    getWidth() / 2f,
                    485,
                    230,
                    58,
                    Color.rgb(180, 105, 220)
            );

            drawText(
                    canvas,
                    "Keep Playing Keep Smiling!",
                    getWidth() / 2f,
                    getHeight() - 135,
                    16,
                    Color.DKGRAY,
                    true
            );
        }

        // ========================================================
        // NAVIGATION
        // ========================================================

        private void drawHeader(
                Canvas canvas,
                String title,
                boolean back,
                boolean home
        ) {

            paint.setColor(Color.WHITE);

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    82,
                    paint
            );

            if (back) {

                drawText(
                        canvas,
                        "‹",
                        30,
                        55,
                        42,
                        Color.DKGRAY,
                        true
                );
            }

            drawText(
                    canvas,
                    title,
                    getWidth() / 2f,
                    52,
                    24,
                    Color.rgb(75, 65, 85),
                    true
            );

            if (home) {

                drawText(
                        canvas,
                        "⌂",
                        getWidth() - 32,
                        54,
                        28,
                        Color.DKGRAY,
                        true
                );
            }
        }

        private void drawMuteButton(Canvas canvas) {

            drawButton(
                    canvas,
                    musicOn ? "🔊" : "🔇",
                    getWidth() - 45,
                    45,
                    55,
                    45,
                    Color.WHITE
            );
        }

        // ========================================================
        // GAME START
        // ========================================================

        private void startGame(int level) {

            currentLevel = level;

            score = 0;
            combo = 0;
            collected = 0;

            target = 10 + currentLevel * 2;

            timeLeft = Math.max(
                    30,
                    55 - currentLevel
            );

            paused = false;

            candies.clear();

            createBoard();

            screen = SCREEN_GAMEPLAY;

            startTimer();

            invalidate();
        }

        private void createBoard() {

            int rows = 7;
            int columns = 5;

            for (int r = 0; r < rows; r++) {

                for (int c = 0; c < columns; c++) {

                    int color =
                            candyColors[
                                    random.nextInt(
                                            candyColors.length
                                    )
                            ];

                    int type =
                            random.nextInt(4);

                    candies.add(
                            new Candy(
                                    r,
                                    c,
                                    color,
                                    type
                            )
                    );
                }
            }
        }

        // ========================================================
        // GAME MECHANICS
        // ========================================================

        private void tapCandy(float x, float y) {

            if (paused) {
                return;
            }

            float boardTop = 120;

            int columns = 5;
            int rows = 7;

            float boardBottom = getHeight() - 155;

            float boardW = getWidth() - 40;

            float cellW = boardW / columns;

            float cellH =
                    (boardBottom - boardTop - 20)
                            / rows;

            int col =
                    (int) ((x - 20) / cellW);

            int row =
                    (int) ((y - boardTop - 10) / cellH);

            if (row < 0 ||
                    row >= rows ||
                    col < 0 ||
                    col >= columns) {
                return;
            }

            Candy selected = null;

            for (Candy candy : candies) {

                if (candy.row == row &&
                        candy.col == col) {

                    selected = candy;
                    break;
                }
            }

            if (selected == null) {
                return;
            }

            long now = System.currentTimeMillis();

            if (now - lastTapTime <= 1600) {
                combo++;
            } else {
                combo = 1;
            }

            lastTapTime = now;

            int points =
                    10 + Math.min(combo, 5) * 5;

            score += points;

            if (selected.type == 0) {
                collected++;
            }

            candies.remove(selected);

            refillBoard();

            if (score > highScore) {
                highScore = score;

                prefs.edit()
                        .putInt(
                                "highScore",
                                highScore
                        )
                        .apply();
            }

            if (collected >= target) {
                finishGame(true);
                return;
            }

            invalidate();
        }

        private void refillBoard() {

            int rows = 7;
            int columns = 5;

            boolean[][] occupied =
                    new boolean[rows][columns];

            for (Candy candy : candies) {
                occupied[candy.row][candy.col] = true;
            }

            for (int r = 0; r < rows; r++) {

                for (int c = 0; c < columns; c++) {

                    if (!occupied[r][c]) {

                        int color =
                                candyColors[
                                        random.nextInt(
                                                candyColors.length
                                        )
                                ];

                        int type =
                                random.nextInt(4);

                        candies.add(
                                new Candy(
                                        r,
                                        c,
                                        color,
                                        type
                                )
                        );
                    }
                }
            }
        }

        private void finishGame(boolean won) {

            stopTimer();

            if (score > highScore) {

                highScore = score;

                prefs.edit()
                        .putInt(
                                "highScore",
                                highScore
                        )
                        .apply();
            }

            if (won) {

                int stars = calculateStars();

                prefs.edit()
                        .putInt(
                                "stars_" + currentLevel,
                                stars
                        )
                        .apply();

                if (currentLevel < 12) {
                    currentLevel =
                            Math.max(
                                    currentLevel,
                                    currentLevel + 1
                            );
                }

                screen = SCREEN_COMPLETE;

            } else {

                screen = SCREEN_GAMEOVER;
            }

            invalidate();
        }

        private int calculateStars() {

            if (score >= target * 35) {
                return 3;
            }

            if (score >= target * 22) {
                return 2;
            }

            return 1;
        }

        private String getStarString(int stars) {

            String result = "";

            for (int i = 0; i < 3; i++) {
                result +=
                        i < stars
                                ? "★"
                                : "☆";
            }

            return result;
        }

        // ========================================================
        // TOUCH
        // ========================================================

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }

            float x = event.getX();
            float y = event.getY();

            switch (screen) {

                case SCREEN_HOME:
                    handleHomeTouch(x, y);
                    break;

                case SCREEN_LEVELS:
                    handleLevelsTouch(x, y);
                    break;

                case SCREEN_GAMEPLAY:
                    handleGameplayTouch(x, y);
                    break;

                case SCREEN_COMPLETE:
                    handleCompleteTouch(x, y);
                    break;

                case SCREEN_GAMEOVER:
                    handleGameOverTouch(x, y);
                    break;

                case SCREEN_SETTINGS:
                    handleSettingsTouch(x, y);
                    break;

                case SCREEN_SCORES:
                    handleScoresTouch(x, y);
                    break;
            }

            return true;
        }

        // ========================================================
        // HOME TOUCH
        // ========================================================

        private void handleHomeTouch(float x, float y) {

            float center = getWidth() / 2f;

            if (inside(
                    x,
                    y,
                    center,
                    390,
                    250,
                    65
            )) {

                startGame(currentLevel);
                return;
            }

            if (inside(
                    x,
                    y,
                    center,
                    475,
                    250,
                    65
            )) {

                screen = SCREEN_LEVELS;
                invalidate();
                return;
            }

            if (inside(
                    x,
                    y,
                    center,
                    560,
                    250,
                    65
            )) {

                screen = SCREEN_SCORES;
                invalidate();
                return;
            }

            if (inside(
                    x,
                    y,
                    center,
                    645,
                    250,
                    65
            )) {

                screen = SCREEN_SETTINGS;
                invalidate();
                return;
            }

            if (x > getWidth() - 80 &&
                    y < 90) {

                musicOn = !musicOn;

                prefs.edit()
                        .putBoolean(
                                "music",
                                musicOn
                        )
                        .apply();

                invalidate();
            }
        }

        // ========================================================
        // LEVEL TOUCH
        // ========================================================

        private void handleLevelsTouch(float x, float y) {

            if (y < 90 && x < 80) {

                screen = SCREEN_HOME;
                invalidate();
                return;
            }

            if (y < 90 &&
                    x > getWidth() - 80) {

                screen = SCREEN_HOME;
                invalidate();
                return;
            }

            int cardW = 105;
            int cardH = 92;
            int gapX = 15;
            int gapY = 18;

            int startY = 145;

            int totalW =
                    cardW * 3
                            + gapX * 2;

            int startX =
                    (getWidth() - totalW) / 2;

            for (int i = 1; i <= 12; i++) {

                int row =
                        (i - 1) / 3;

                int col =
                        (i - 1) % 3;

                float cx =
                        startX
                                + col * (cardW + gapX)
                                + cardW / 2f;

                float cy =
                        startY
                                + row * (cardH + gapY)
                                + cardH / 2f;

                if (inside(
                        x,
                        y,
                        cx,
                        cy,
                        cardW,
                        cardH
                )) {

                    boolean unlocked =
                            i <= Math.min(
                                    currentLevel + 2,
                                    12
                            );

                    if (unlocked) {
                        startGame(i);
                    }

                    return;
                }
            }
        }

        // ========================================================
        // GAMEPLAY TOUCH
        // ========================================================

        private void handleGameplayTouch(
                float x,
                float y
        ) {

            if (paused) {

                handlePauseTouch(x, y);
                return;
            }

            if (y < 100 &&
                    x > getWidth() - 115) {

                paused = true;
                stopTimer();
                invalidate();
                return;
            }

            tapCandy(x, y);
        }

        private void handlePauseTouch(
                float x,
                float y
        ) {

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    260,
                    230,
                    58
            )) {

                paused = false;
                startTimer();
                invalidate();
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    335,
                    230,
                    58
            )) {

                startGame(currentLevel);
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    410,
                    230,
                    58
            )) {

                paused = false;
                screen = SCREEN_SETTINGS;
                invalidate();
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    485,
                    230,
                    58
            )) {

                paused = false;
                screen = SCREEN_HOME;
                invalidate();
            }

            if (x > getWidth() - 100 &&
                    y < 210) {

                paused = false;
                startTimer();
                invalidate();
            }
        }

        // ========================================================
        // COMPLETE TOUCH
        // ========================================================

        private void handleCompleteTouch(
                float x,
                float y
        ) {

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    470,
                    250,
                    60
            )) {

                int next =
                        Math.min(
                                currentLevel + 1,
                                12
                        );

                startGame(next);
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    545,
                    250,
                    60
            )) {

                startGame(currentLevel);
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    620,
                    250,
                    60
            )) {

                screen = SCREEN_HOME;
                invalidate();
            }
        }

        // ========================================================
        // GAME OVER TOUCH
        // ========================================================

        private void handleGameOverTouch(
                float x,
                float y
        ) {

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    450,
                    250,
                    65
            )) {

                startGame(currentLevel);
                return;
            }

            if (inside(
                    x,
                    y,
                    getWidth() / 2f,
                    535,
                    250,
                    65
            )) {

                screen = SCREEN_HOME;
                invalidate();
            }
        }

        // ========================================================
        // SETTINGS TOUCH
        // ========================================================

        private void handleSettingsTouch(
                float x,
                float y
        ) {

            if (y < 90 && x < 80) {

                screen = SCREEN_HOME;
                invalidate();
                return;
            }

            float base = 145;

            if (y >= base &&
                    y <= base + 55) {

                musicOn = !musicOn;

                prefs.edit()
                        .putBoolean(
                                "music",
                                musicOn
                        )
                        .apply();

                invalidate();
                return;
            }

            if (y >= base + 70 &&
                    y <= base + 125) {

                soundOn = !soundOn;

                prefs.edit()
                        .putBoolean(
                                "sound",
                                soundOn
                        )
                        .apply();

                invalidate();
                return;
            }

            if (y >= base + 140 &&
                    y <= base + 195) {

                vibrationOn = !vibrationOn;

                prefs.edit()
                        .putBoolean(
                                "vibration",
                                vibrationOn
                        )
                        .apply();

                invalidate();
            }
        }

        // ========================================================
        // SCORES TOUCH
        // ========================================================

        private void handleScoresTouch(
                float x,
                float y
        ) {

            if (y < 90 && x < 80) {

                screen = SCREEN_HOME;
                invalidate();
            }
        }

        // ========================================================
        // UI HELPERS
        // ========================================================

        private boolean inside(
                float x,
                float y,
                float cx,
                float cy,
                float w,
                float h
        ) {

            return x >= cx - w / 2
                    && x <= cx + w / 2
                    && y >= cy - h / 2
                    && y <= cy + h / 2;
        }

        private void drawBackground(Canvas canvas) {

            paint.setColor(
                    Color.rgb(
                            250,
                            244,
                            255
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    paint
            );

            paint.setColor(
                    Color.argb(
                            45,
                            245,
                            75,
                            100
                    )
            );

            canvas.drawCircle(
                    30,
                    100,
                    70,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            40,
                            80,
                            190,
                            110
                    )
            );

            canvas.drawCircle(
                    getWidth() - 20,
                    getHeight() - 80,
                    90,
                    paint
            );
        }

        private void drawTitle(
                Canvas canvas,
                String text,
                float x,
                float y,
                float size
        ) {

            drawText(
                    canvas,
                    text,
                    x,
                    y,
                    size,
                    Color.rgb(
                            180,
                            90,
                            210
                    ),
                    true
            );
        }

        private void drawText(
                Canvas canvas,
                String text,
                float x,
                float y,
                float size,
                int color,
                boolean bold
        ) {

            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            bold
                                    ? Typeface.BOLD
                                    : Typeface.NORMAL
                    )
            );

            canvas.drawText(
                    text,
                    x,
                    y,
                    paint
            );
        }

        private void drawButton(
                Canvas canvas,
                String text,
                float cx,
                float cy,
                float w,
                float h,
                int color
        ) {

            paint.setColor(color);

            canvas.drawRoundRect(
                    new RectF(
                            cx - w / 2,
                            cy - h / 2,
                            cx + w / 2,
                            cy + h / 2
                    ),
                    20,
                    20,
                    paint
            );

            drawText(
                    canvas,
                    text,
                    cx,
                    cy + 7,
                    17,
                    color == Color.WHITE
                            ? Color.DKGRAY
                            : Color.WHITE,
                    true
            );
        }

        // ========================================================
        // CLEANUP
        // ========================================================

        @Override
        protected void onDetachedFromWindow() {

            stopTimer();

            super.onDetachedFromWindow();
        }
    }

    // ============================================================
    // CANDY MODEL
    // ============================================================

    public static class Candy {

        int row;
        int col;
        int color;
        int type;

        Candy(
                int row,
                int col,
                int color,
                int type
        ) {

            this.row = row;
            this.col = col;
            this.color = color;
            this.type = type;
        }
    }

    // ============================================================
    // SIMPLE DROP SHAPE HELPER
    // ============================================================

    public static class PathUtil {

        public static void drawDrop(
                Canvas canvas,
                float cx,
                float cy,
                float size,
                Paint paint
        ) {

            android.graphics.Path path =
                    new android.graphics.Path();

            path.moveTo(
                    cx,
                    cy - size
            );

            path.cubicTo(
                    cx - size * .85f,
                    cy - size * .15f,
                    cx - size * .8f,
                    cy + size * .65f,
                    cx,
                    cy + size
            );

            path.cubicTo(
                    cx + size * .8f,
                    cy + size * .65f,
                    cx + size * .85f,
                    cy - size * .15f,
                    cx,
                    cy - size
            );

            path.close();

            canvas.drawPath(
                    path,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(size * .65f);
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(
                    "★",
                    cx,
                    cy + size * .25f,
                    paint
            );
        }
    }
}