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
import android.graphics.Path;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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

        private static final int ROWS = 7;
        private static final int COLUMNS = 5;
        private static final int CANDY_TYPES = 6;
        private static final int MAX_LEVEL = 30;

        private int screen = SCREEN_HOME;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final Handler handler = new Handler();

        private final SharedPreferences prefs;

        private ToneGenerator toneGenerator;
        private Vibrator vibrator;

        private int currentLevel = 1;
        private int score = 0;
        private int highScore = 0;
        private int timeLeft = 60;
        private int combo = 0;

        private int target = 15;
        private int collected = 0;

        private boolean paused = false;

        private boolean musicOn = true;
        private boolean soundOn = true;
        private boolean vibrationOn = true;

        private long lastMatchTime = 0;

        private float touchDownX;
        private float touchDownY;

        private boolean touchingBoard = false;

        private Candy[][] board =
                new Candy[ROWS][COLUMNS];

        private final int[] candyColors = {
                Color.rgb(245, 75, 105),
                Color.rgb(255, 190, 45),
                Color.rgb(75, 190, 105),
                Color.rgb(70, 145, 235),
                Color.rgb(175, 90, 220),
                Color.rgb(255, 130, 55)
        };

        private final String[] candyNames = {
                "Berry",
                "Lemon",
                "Apple",
                "Blue",
                "Grape",
                "Orange"
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

                    handler.postDelayed(
                            this,
                            1000
                    );
                }
            }
        };

        public CandyGameView(Context context) {
            super(context);

            prefs = context.getSharedPreferences(
                    "CandyJoltSettings",
                    Context.MODE_PRIVATE
            );

            highScore =
                    prefs.getInt(
                            "highScore",
                            0
                    );

            musicOn =
                    prefs.getBoolean(
                            "music",
                            true
                    );

            soundOn =
                    prefs.getBoolean(
                            "sound",
                            true
                    );

            vibrationOn =
                    prefs.getBoolean(
                            "vibration",
                            true
                    );

            try {
                toneGenerator =
                        new ToneGenerator(
                                AudioManager.STREAM_MUSIC,
                                70
                        );
            } catch (Exception ignored) {
            }

            vibrator =
                    (Vibrator)
                            context.getSystemService(
                                    Context.VIBRATOR_SERVICE
                            );

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            setFocusable(true);
        }

        // ========================================================
        // TIMER
        // ========================================================

        private void startTimer() {

            handler.removeCallbacks(
                    timerRunnable
            );

            handler.postDelayed(
                    timerRunnable,
                    1000
            );
        }

        public void stopTimer() {

            handler.removeCallbacks(
                    timerRunnable
            );
        }

        public void resumeTimerIfNeeded() {

            if (screen == SCREEN_GAMEPLAY &&
                    !paused) {

                handler.removeCallbacks(
                        timerRunnable
                );

                handler.postDelayed(
                        timerRunnable,
                        1000
                );
            }
        }

        // ========================================================
        // DRAW
        // ========================================================

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            canvas.drawColor(
                    Color.rgb(
                            250,
                            244,
                            255
                    )
            );

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

            float center =
                    getWidth() / 2f;

            drawButton(
                    canvas,
                    "PLAY",
                    center,
                    390,
                    250,
                    65,
                    Color.rgb(
                            85,
                            190,
                            110
                    )
            );

            drawButton(
                    canvas,
                    "LEVELS",
                    center,
                    475,
                    250,
                    65,
                    Color.rgb(
                            90,
                            145,
                            235
                    )
            );

            drawButton(
                    canvas,
                    "HIGH SCORES",
                    center,
                    560,
                    250,
                    65,
                    Color.rgb(
                            180,
                            105,
                            220
                    )
            );

            drawButton(
                    canvas,
                    "SETTINGS",
                    center,
                    645,
                    250,
                    65,
                    Color.rgb(
                            255,
                            165,
                            65
                    )
            );

            drawMuteButton(canvas);
        }

        private void drawCandyLogo(Canvas canvas) {

            float cx =
                    getWidth() / 2f;

            float cy = 245;

            drawCandyShape(
                    canvas,
                    cx - 65,
                    cy,
                    34,
                    0
            );

            drawCandyShape(
                    canvas,
                    cx,
                    cy - 20,
                    34,
                    1
            );

            drawCandyShape(
                    canvas,
                    cx + 65,
                    cy,
                    34,
                    2
            );
        }

        // ========================================================
        // LEVEL SELECT
        // ========================================================

        private void drawLevels(Canvas canvas) {

            drawBackground(canvas);

            drawHeader(
                    canvas,
                    "Level Select",
                    true,
                    true
            );

            int cardW = 105;
            int cardH = 92;

            int startY = 145;
            int gapX = 15;
            int gapY = 18;

            int totalW =
                    cardW * 3 +
                            gapX * 2;

            int startX =
                    (getWidth() - totalW) / 2;

            for (int i = 1;
                 i <= MAX_LEVEL;
                 i++) {

                int row =
                        (i - 1) / 3;

                int col =
                        (i - 1) % 3;

                float x =
                        startX +
                                col *
                                        (cardW + gapX);

                float y =
                        startY +
                                row *
                                        (cardH + gapY);

                /*
                 * The level screen is scroll-free for now.
                 * The first 12 levels remain visible just like
                 * the original version.
                 */
                if (row >= 4) {
                    break;
                }

                boolean unlocked =
                        i <= Math.min(
                                currentLevel + 2,
                                MAX_LEVEL
                        );

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
                            : Color.rgb(
                                    220,
                                    220,
                                    225
                            )
            );

            canvas.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x + w,
                            y + h
                    ),
                    18,
                    18,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(3);

            paint.setColor(
                    unlocked
                            ? Color.rgb(
                                    150,
                                    110,
                                    220
                            )
                            : Color.GRAY
            );

            canvas.drawRoundRect(
                    new RectF(
                            x,
                            y,
                            x + w,
                            y + h
                    ),
                    18,
                    18,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

            drawText(
                    canvas,
                    String.valueOf(level),
                    x + w / 2,
                    y + 38,
                    27,
                    unlocked
                            ? Color.rgb(
                                    70,
                                    70,
                                    80
                            )
                            : Color.GRAY,
                    true
            );

            if (unlocked) {

                int stars =
                        getStars(level);

                String starText = "";

                for (int s = 0;
                     s < 3;
                     s++) {

                    starText +=
                            s < stars
                                    ? "★"
                                    : "☆";
                }

                drawText(
                        canvas,
                        starText,
                        x + w / 2,
                        y + 70,
                        20,
                        Color.rgb(
                                255,
                                190,
                                45
                        ),
                        true
                );

            } else {

                drawText(
                        canvas,
                        "LOCKED",
                        x + w / 2,
                        y + 70,
                        13,
                        Color.GRAY,
                        true
                );
            }
        }

        private int getStars(int level) {

            return prefs.getInt(
                    "stars_" + level,
                    0
            );
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
                    String.valueOf(
                            timeLeft
                    ) + "s",
                    55,
                    72,
                    24,
                    timeLeft <= 10
                            ? Color.RED
                            : Color.rgb(
                                    65,
                                    65,
                                    75
                            ),
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
                    Color.rgb(
                            70,
                            70,
                            80
                    ),
                    true
            );

            drawButton(
                    canvas,
                    "PAUSE",
                    getWidth() - 62,
                    54,
                    82,
                    45,
                    Color.rgb(
                            245,
                            105,
                            115
                    )
            );
        }

        private void drawBoard(Canvas canvas) {

            float boardTop = 120;

            float boardBottom =
                    getHeight() - 155;

            paint.setColor(
                    Color.rgb(
                            238,
                            226,
                            247
                    )
            );

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

            float boardW =
                    getWidth() - 40;

            float cellW =
                    boardW / COLUMNS;

            float cellH =
                    (boardBottom -
                            boardTop -
                            20) / ROWS;

            for (int r = 0;
                 r < ROWS;
                 r++) {

                for (int c = 0;
                     c < COLUMNS;
                     c++) {

                    Candy candy =
                            board[r][c];

                    if (candy == null) {
                        continue;
                    }

                    float cx =
                            20 +
                                    c * cellW +
                                    cellW / 2;

                    float cy =
                            boardTop +
                                    10 +
                                    r * cellH +
                                    cellH / 2;

                    drawCandyShape(
                            canvas,
                            cx,
                            cy,
                            Math.min(
                                    cellW,
                                    cellH
                            ) * 0.34f,
                            candy.type
                    );
                }
            }
        }

        // ========================================================
        // REAL CANDY GRAPHICS
        // ========================================================

        private void drawCandyShape(
                Canvas canvas,
                float cx,
                float cy,
                float size,
                int type
        ) {

            int color =
                    candyColors[
                            Math.max(
                                    0,
                                    Math.min(
                                            CANDY_TYPES - 1,
                                            type
                                    )
                            )
                    ];

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(color);

            switch (type) {

                case 0:
                    drawBerryCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;

                case 1:
                    drawLemonCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;

                case 2:
                    drawAppleCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;

                case 3:
                    drawBlueCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;

                case 4:
                    drawGrapeCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;

                case 5:
                    drawOrangeCandy(
                            canvas,
                            cx,
                            cy,
                            size
                    );
                    break;
            }

            // Candy shine
            paint.setColor(
                    Color.argb(
                            210,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    cx - size * .35f,
                    cy - size * .35f,
                    size * .13f,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            90,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    cx - size * .12f,
                    cy - size * .50f,
                    size * .07f,
                    paint
            );
        }

        private void drawBerryCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[0]
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    size,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            215,
                            45,
                            75
                    )
            );

            canvas.drawCircle(
                    cx - size * .42f,
                    cy,
                    size * .55f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .42f,
                    cy,
                    size * .55f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            70,
                            170,
                            75
                    )
            );

            Path leaf =
                    new Path();

            leaf.moveTo(
                    cx,
                    cy - size * .72f
            );

            leaf.lineTo(
                    cx - size * .35f,
                    cy - size * 1.05f
            );

            leaf.lineTo(
                    cx + size * .10f,
                    cy - size * .85f
            );

            leaf.close();

            canvas.drawPath(
                    leaf,
                    paint
            );
        }

        private void drawLemonCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[1]
            );

            Path path =
                    new Path();

            path.moveTo(
                    cx,
                    cy - size
            );

            path.cubicTo(
                    cx + size * .85f,
                    cy - size * .45f,
                    cx + size * .80f,
                    cy + size * .55f,
                    cx,
                    cy + size
            );

            path.cubicTo(
                    cx - size * .80f,
                    cy + size * .55f,
                    cx - size * .85f,
                    cy - size * .45f,
                    cx,
                    cy - size
            );

            path.close();

            canvas.drawPath(
                    path,
                    paint
            );
        }

        private void drawAppleCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[2]
            );

            canvas.drawCircle(
                    cx - size * .38f,
                    cy,
                    size * .65f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .38f,
                    cy,
                    size * .65f,
                    paint
            );

            canvas.drawCircle(
                    cx,
                    cy + size * .20f,
                    size * .72f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            90,
                            55,
                            35
                    )
            );

            paint.setStrokeWidth(
                    size * .12f
            );

            canvas.drawLine(
                    cx,
                    cy - size * .55f,
                    cx + size * .15f,
                    cy - size * .90f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            55,
                            160,
                            75
                    )
            );

            Path leaf =
                    new Path();

            leaf.moveTo(
                    cx + size * .05f,
                    cy - size * .65f
            );

            leaf.lineTo(
                    cx + size * .55f,
                    cy - size * .95f
            );

            leaf.lineTo(
                    cx + size * .38f,
                    cy - size * .45f
            );

            leaf.close();

            canvas.drawPath(
                    leaf,
                    paint
            );
        }

        private void drawBlueCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[3]
            );

            canvas.drawRoundRect(
                    new RectF(
                            cx - size,
                            cy - size * .78f,
                            cx + size,
                            cy + size * .78f
                    ),
                    size * .40f,
                    size * .40f,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            130,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    cx - size * .25f,
                    cy,
                    size * .30f,
                    paint
            );
        }

        private void drawGrapeCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[4]
            );

            canvas.drawCircle(
                    cx,
                    cy - size * .40f,
                    size * .48f,
                    paint
            );

            canvas.drawCircle(
                    cx - size * .48f,
                    cy,
                    size * .48f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .48f,
                    cy,
                    size * .48f,
                    paint
            );

            canvas.drawCircle(
                    cx - size * .25f,
                    cy + size * .48f,
                    size * .48f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .25f,
                    cy + size * .48f,
                    size * .48f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            65,
                            165,
                            75
                    )
            );

            Path leaf =
                    new Path();

            leaf.moveTo(
                    cx,
                    cy - size * .65f
            );

            leaf.lineTo(
                    cx + size * .65f,
                    cy - size * 1.05f
            );

            leaf.lineTo(
                    cx + size * .35f,
                    cy - size * .40f
            );

            leaf.close();

            canvas.drawPath(
                    leaf,
                    paint
            );
        }

        private void drawOrangeCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size
        ) {

            paint.setColor(
                    candyColors[5]
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    size,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            180,
                            75
                    )
            );

            paint.setStrokeWidth(
                    size * .08f
            );

            for (int i = 0;
                 i < 8;
                 i++) {

                double angle =
                        Math.PI * 2 *
                                i / 8.0;

                float x1 =
                        cx +
                                (float)
                                        Math.cos(angle)
                                        * size
                                        * .15f;

                float y1 =
                        cy +
                                (float)
                                        Math.sin(angle)
                                        * size
                                        * .15f;

                float x2 =
                        cx +
                                (float)
                                        Math.cos(angle)
                                        * size
                                        * .75f;

                float y2 =
                        cy +
                                (float)
                                        Math.sin(angle)
                                        * size
                                        * .75f;

                canvas.drawLine(
                        x1,
                        y1,
                        x2,
                        y2,
                        paint
                );
            }
        }

        // ========================================================
        // BOTTOM HUD
        // ========================================================

        private void drawGameBottom(Canvas canvas) {

            float y =
                    getHeight() - 125;

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
                    "🍬  " +
                            collected +
                            "/" +
                            target,
                    90,
                    y + 68,
                    22,
                    Color.rgb(
                            245,
                            75,
                            100
                    ),
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
                    Color.rgb(
                            180,
                            100,
                            220
                    ),
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
                    "LEVEL " +
                            currentLevel +
                            " COMPLETE!",
                    getWidth() / 2f,
                    110,
                    30
            );

            int stars =
                    calculateStars();

            drawText(
                    canvas,
                    getStarString(stars),
                    getWidth() / 2f,
                    190,
                    45,
                    Color.rgb(
                            255,
                            190,
                            45
                    ),
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
                    Color.rgb(
                            70,
                            70,
                            80
                    ),
                    true
            );

            drawButton(
                    canvas,
                    "NEXT LEVEL",
                    getWidth() / 2f,
                    470,
                    250,
                    60,
                    Color.rgb(
                            85,
                            190,
                            110
                    )
            );

            drawButton(
                    canvas,
                    "REPLAY",
                    getWidth() / 2f,
                    545,
                    250,
                    60,
                    Color.rgb(
                            90,
                            145,
                            235
                    )
            );

            drawButton(
                    canvas,
                    "HOME",
                    getWidth() / 2f,
                    620,
                    250,
                    60,
                    Color.rgb(
                            180,
                            105,
                            220
                    )
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
                    Color.rgb(
                            70,
                            70,
                            80
                    ),
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
                    Color.rgb(
                            245,
                            155,
                            45
                    ),
                    true
            );

            drawButton(
                    canvas,
                    "PLAY AGAIN",
                    getWidth() / 2f,
                    450,
                    250,
                    65,
                    Color.rgb(
                            85,
                            190,
                            110
                    )
            );

            drawButton(
                    canvas,
                    "MAIN MENU",
                    getWidth() / 2f,
                    535,
                    250,
                    65,
                    Color.rgb(
                            180,
                            105,
                            220
                    )
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
                    Color.rgb(
                            255,
                            165,
                            65
                    )
            );

            drawButton(
                    canvas,
                    "PRIVACY POLICY",
                    getWidth() / 2f,
                    y + 530,
                    250,
                    52,
                    Color.rgb(
                            90,
                            145,
                            235
                    )
            );

            drawButton(
                    canvas,
                    "MORE GAMES",
                    getWidth() / 2f,
                    y + 595,
                    250,
                    52,
                    Color.rgb(
                            180,
                            105,
                            220
                    )
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
                    enabled
                            ? "ON"
                            : "OFF",
                    getWidth() - 70,
                    y + 35,
                    15,
                    enabled
                            ? Color.rgb(
                                    70,
                                    180,
                                    100
                            )
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
                    Color.rgb(
                            100,
                            100,
                            110
                    ),
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
                    Color.rgb(
                            255,
                            190,
                            45
                    ),
                    true
            );

            drawButton(
                    canvas,
                    "GLOBAL",
                    getWidth() / 2f - 80,
                    115,
                    135,
                    48,
                    Color.rgb(
                            90,
                            145,
                            235
                    )
            );

            drawButton(
                    canvas,
                    "FRIENDS",
                    getWidth() / 2f + 80,
                    115,
                    135,
                    48,
                    Color.rgb(
                            190,
                            190,
                            200
                    )
            );

            float y = 190;

            for (int i = 0;
                 i < 5;
                 i++) {

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
                        Color.rgb(
                                80,
                                80,
                                90
                        ),
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
                        String.valueOf(
                                sampleScores[i]
                        ),
                        getWidth() - 70,
                        y + 42,
                        17,
                        Color.rgb(
                                180,
                                100,
                                220
                        ),
                        true
                );

                y += 82;
            }
        }

        private String getFlag(int index) {

            String[] flags = {
                    "LS",
                    "ZA",
                    "BW",
                    "NA",
                    "ZW"
            };

            return flags[index];
        }

        // ========================================================
        // PAUSE
        // ========================================================

        private void drawPauseOverlay(Canvas canvas) {

            paint.setColor(
                    Color.argb(
                            190,
                            20,
                            15,
                            35
                    )
            );

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
                    Color.rgb(
                            180,
                            105,
                            220
                    ),
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
                    Color.rgb(
                            85,
                            190,
                            110
                    )
            );

            drawButton(
                    canvas,
                    "RESTART",
                    getWidth() / 2f,
                    335,
                    230,
                    58,
                    Color.rgb(
                            90,
                            145,
                            235
                    )
            );

            drawButton(
                    canvas,
                    "SETTINGS",
                    getWidth() / 2f,
                    410,
                    230,
                    58,
                    Color.rgb(
                            255,
                            165,
                            65
                    )
            );

            drawButton(
                    canvas,
                    "MAIN MENU",
                    getWidth() / 2f,
                    485,
                    230,
                    58,
                    Color.rgb(
                            180,
                            105,
                            220
                    )
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
                    Color.rgb(
                            75,
                            65,
                            85
                    ),
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
                    musicOn
                            ? "SOUND"
                            : "MUTE",
                    getWidth() - 45,
                    45,
                    75,
                    45,
                    Color.WHITE
            );
        }

        // ========================================================
        // GAME START
        // ========================================================

        private void startGame(int level) {

            currentLevel =
                    Math.max(
                            1,
                            Math.min(
                                    level,
                                    MAX_LEVEL
                            )
                    );

            score = 0;
            combo = 0;
            collected = 0;

            target =
                    12 +
                            currentLevel * 3;

            timeLeft =
                    Math.max(
                            35,
                            65 -
                                    currentLevel
                                    * 2
                    );

            paused = false;

            createBoard();

            screen =
                    SCREEN_GAMEPLAY;

            startTimer();

            invalidate();
        }

        // ========================================================
        // CREATE CLEAN BOARD
        // ========================================================

        private void createBoard() {

            int attempts = 0;

            do {

                for (int r = 0;
                     r < ROWS;
                     r++) {

                    for (int c = 0;
                         c < COLUMNS;
                         c++) {

                        board[r][c] =
                                new Candy(
                                        randomCandyType()
                                );
                    }
                }

                attempts++;

            } while (
                    hasAnyMatch() &&
                    attempts < 100
            );
        }

        private int randomCandyType() {

            return random.nextInt(
                    CANDY_TYPES
            );
        }

        // ========================================================
        // REAL SWIPE MATCH-3
        // ========================================================

        private void handleBoardSwipe(
                float startX,
                float startY,
                float endX,
                float endY
        ) {

            if (paused) {
                return;
            }

            BoardPosition start =
                    getBoardPosition(
                            startX,
                            startY
                    );

            BoardPosition end =
                    getBoardPosition(
                            endX,
                            endY
                    );

            if (start == null ||
                    end == null) {
                return;
            }

            int rowDiff =
                    Math.abs(
                            start.row -
                                    end.row
                    );

            int colDiff =
                    Math.abs(
                            start.col -
                                    end.col
                    );

            // Only one adjacent movement is allowed.
            if (rowDiff + colDiff != 1) {
                return;
            }

            swapCandies(
                    start.row,
                    start.col,
                    end.row,
                    end.col
            );

            if (!hasMatchAt(
                    start.row,
                    start.col
            ) &&
                    !hasMatchAt(
                            end.row,
                            end.col
                    )) {

                // Invalid move: put them back.
                swapCandies(
                        start.row,
                        start.col,
                        end.row,
                        end.col
                );

                playInvalidSound();

                invalidate();

                return;
            }

            combo = 0;

            resolveMatches();

            invalidate();
        }

        private BoardPosition getBoardPosition(
                float x,
                float y
        ) {

            float boardTop = 120;

            float boardBottom =
                    getHeight() - 155;

            float boardW =
                    getWidth() - 40;

            float cellW =
                    boardW / COLUMNS;

            float cellH =
                    (boardBottom -
                            boardTop -
                            20) / ROWS;

            if (x < 20 ||
                    x > getWidth() - 20 ||
                    y < boardTop + 10 ||
                    y > boardBottom - 10) {

                return null;
            }

            int col =
                    (int)
                            ((x - 20) /
                                    cellW);

            int row =
                    (int)
                            ((y -
                                    boardTop -
                                    10) /
                                    cellH);

            if (row < 0 ||
                    row >= ROWS ||
                    col < 0 ||
                    col >= COLUMNS) {

                return null;
            }

            return new BoardPosition(
                    row,
                    col
            );
        }

        private void swapCandies(
                int r1,
                int c1,
                int r2,
                int c2
        ) {

            Candy temp =
                    board[r1][c1];

            board[r1][c1] =
                    board[r2][c2];

            board[r2][c2] =
                    temp;
        }

        // ========================================================
        // FIND MATCHES
        // ========================================================

        private Set<String> findMatches() {

            Set<String> matches =
                    new HashSet<>();

            // Horizontal
            for (int r = 0;
                 r < ROWS;
                 r++) {

                int runStart = 0;

                for (int c = 1;
                     c <= COLUMNS;
                     c++) {

                    boolean same =
                            c < COLUMNS &&
                                    board[r][c] != null &&
                                    board[r][runStart] != null &&
                                    board[r][c].type ==
                                            board[r][runStart].type;

                    if (!same) {

                        int runLength =
                                c - runStart;

                        if (runLength >= 3) {

                            for (int x = runStart;
                                 x < c;
                                 x++) {

                                matches.add(
                                        key(
                                                r,
                                                x
                                        )
                                );
                            }
                        }

                        runStart = c;
                    }
                }
            }

            // Vertical
            for (int c = 0;
                 c < COLUMNS;
                 c++) {

                int runStart = 0;

                for (int r = 1;
                     r <= ROWS;
                     r++) {

                    boolean same =
                            r < ROWS &&
                                    board[r][c] != null &&
                                    board[runStart][c] != null &&
                                    board[r][c].type ==
                                            board[runStart][c].type;

                    if (!same) {

                        int runLength =
                                r - runStart;

                        if (runLength >= 3) {

                            for (int y = runStart;
                                 y < r;
                                 y++) {

                                matches.add(
                                        key(
                                                y,
                                                c
                                        )
                                );
                            }
                        }

                        runStart = r;
                    }
                }
            }

            return matches;
        }

        private String key(
                int row,
                int col
        ) {

            return row + ":" + col;
        }

        private boolean hasAnyMatch() {

            return !findMatches().isEmpty();
        }

        private boolean hasMatchAt(
                int row,
                int col
        ) {

            if (board[row][col] == null) {
                return false;
            }

            int type =
                    board[row][col].type;

            int horizontal = 1;

            for (int c = col - 1;
                 c >= 0 &&
                         board[row][c] != null &&
                         board[row][c].type == type;
                 c--) {

                horizontal++;
            }

            for (int c = col + 1;
                 c < COLUMNS &&
                         board[row][c] != null &&
                         board[row][c].type == type;
                 c++) {

                horizontal++;
            }

            if (horizontal >= 3) {
                return true;
            }

            int vertical = 1;

            for (int r = row - 1;
                 r >= 0 &&
                         board[r][col] != null &&
                         board[r][col].type == type;
                 r--) {

                vertical++;
            }

            for (int r = row + 1;
                 r < ROWS &&
                         board[r][col] != null &&
                         board[r][col].type == type;
                 r++) {

                vertical++;
            }

            return vertical >= 3;
        }

        // ========================================================
        // REMOVE + DROP + REFILL
        // ========================================================

        private void resolveMatches() {

            Set<String> matches =
                    findMatches();

            if (matches.isEmpty()) {
                return;
            }

            combo++;

            if (combo > 1) {
                playComboSound();
            } else {
                playMatchSound();
            }

            vibrateForMatch();

            int amount =
                    matches.size();

            collected += amount;

            int points =
                    amount *
                            10 *
                            Math.max(
                                    1,
                                    combo
                            );

            score += points;

            if (score > highScore) {

                highScore = score;

                prefs.edit()
                        .putInt(
                                "highScore",
                                highScore
                        )
                        .apply();
            }

            // Remove matched candies.
            for (String position : matches) {

                String[] parts =
                        position.split(":");

                int row =
                        Integer.parseInt(
                                parts[0]
                        );

                int col =
                        Integer.parseInt(
                                parts[1]
                        );

                board[row][col] = null;
            }

            dropCandies();

            refillBoard();

            // Check cascades.
            if (hasAnyMatch()) {

                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (screen ==
                                        SCREEN_GAMEPLAY &&
                                        !paused) {

                                    resolveMatches();
                                    invalidate();
                                }
                            }
                        },
                        180
                );
            }

            if (collected >= target) {

                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (screen ==
                                        SCREEN_GAMEPLAY) {

                                    finishGame(true);
                                }
                            }
                        },
                        250
                );
            }
        }

        private void dropCandies() {

            for (int c = 0;
                 c < COLUMNS;
                 c++) {

                int writeRow =
                        ROWS - 1;

                for (int r = ROWS - 1;
                     r >= 0;
                     r--) {

                    if (board[r][c] != null) {

                        Candy candy =
                                board[r][c];

                        board[r][c] = null;

                        board[writeRow][c] =
                                candy;

                        writeRow--;
                    }
                }
            }
        }

        private void refillBoard() {

            for (int r = 0;
                 r < ROWS;
                 r++) {

                for (int c = 0;
                     c < COLUMNS;
                     c++) {

                    if (board[r][c] == null) {

                        board[r][c] =
                                new Candy(
                                        randomCandyType()
                                );
                    }
                }
            }
        }

        // ========================================================
        // AUDIO + VIBRATION
        // ========================================================

        private void playMatchSound() {

            if (!soundOn ||
                    toneGenerator == null) {
                return;
            }

            try {

                toneGenerator.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        90
                );

            } catch (Exception ignored) {
            }
        }

        private void playComboSound() {

            if (!soundOn ||
                    toneGenerator == null) {
                return;
            }

            try {

                toneGenerator.startTone(
                        ToneGenerator.TONE_PROP_ACK,
                        130
                );

            } catch (Exception ignored) {
            }
        }

        private void playInvalidSound() {

            if (!soundOn ||
                    toneGenerator == null) {
                return;
            }

            try {

                toneGenerator.startTone(
                        ToneGenerator.TONE_PROP_NACK,
                        80
                );

            } catch (Exception ignored) {
            }
        }

        private void vibrateForMatch() {

            if (!vibrationOn ||
                    vibrator == null) {
                return;
            }

            try {

                if (Build.VERSION.SDK_INT >= 26) {

                    vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                    45,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                            )
                    );

                } else {

                    vibrator.vibrate(45);
                }

            } catch (Exception ignored) {
            }
        }

        // ========================================================
        // FINISH GAME
        // ========================================================

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

                int stars =
                        calculateStars();

                prefs.edit()
                        .putInt(
                                "stars_" +
                                        currentLevel,
                                stars
                        )
                        .apply();

                screen =
                        SCREEN_COMPLETE;

                playComboSound();

            } else {

                screen =
                        SCREEN_GAMEOVER;

                playInvalidSound();
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

        private String getStarString(
                int stars
        ) {

            String result = "";

            for (int i = 0;
                 i < 3;
                 i++) {

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
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            float x = event.getX();
            float y = event.getY();

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                touchDownX = x;
                touchDownY = y;

                touchingBoard =
                        screen ==
                                SCREEN_GAMEPLAY &&
                                !paused;

                return true;
            }

            if (event.getAction() ==
                    MotionEvent.ACTION_UP) {

                float dx =
                        x - touchDownX;

                float dy =
                        y - touchDownY;

                if (screen ==
                        SCREEN_GAMEPLAY &&
                        touchingBoard &&
                        !paused) {

                    if (Math.abs(dx) > 35 ||
                            Math.abs(dy) > 35) {

                        float endX =
                                touchDownX;

                        float endY =
                                touchDownY;

                        if (Math.abs(dx) >
                                Math.abs(dy)) {

                            endX +=
                                    dx > 0
                                            ? 70
                                            : -70;

                        } else {

                            endY +=
                                    dy > 0
                                            ? 70
                                            : -70;
                        }

                        handleBoardSwipe(
                                touchDownX,
                                touchDownY,
                                endX,
                                endY
                        );

                    } else {

                        // Tapping the pause button.
                        if (touchDownY < 100 &&
                                touchDownX >
                                        getWidth() - 115) {

                            paused = true;
                            stopTimer();
                            invalidate();
                        }
                    }

                    touchingBoard = false;

                    return true;
                }

                switch (screen) {

                    case SCREEN_HOME:
                        handleHomeTouch(x, y);
                        break;

                    case SCREEN_LEVELS:
                        handleLevelsTouch(x, y);
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

            return true;
        }

        // ========================================================
        // HOME TOUCH
        // ========================================================

        private void handleHomeTouch(
                float x,
                float y
        ) {

            float center =
                    getWidth() / 2f;

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

                screen =
                        SCREEN_LEVELS;

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

                screen =
                        SCREEN_SCORES;

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

                screen =
                        SCREEN_SETTINGS;

                invalidate();

                return;
            }

            if (x >
                    getWidth() - 100 &&
                    y < 90) {

                musicOn =
                        !musicOn;

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

        private void handleLevelsTouch(
                float x,
                float y
        ) {

            if (y < 90 &&
                    x < 80) {

                screen =
                        SCREEN_HOME;

                invalidate();

                return;
            }

            if (y < 90 &&
                    x >
                            getWidth() - 80) {

                screen =
                        SCREEN_HOME;

                invalidate();

                return;
            }

            int cardW = 105;
            int cardH = 92;
            int gapX = 15;
            int gapY = 18;
            int startY = 145;

            int totalW =
                    cardW * 3 +
                            gapX * 2;

            int startX =
                    (getWidth() -
                            totalW) / 2;

            for (int i = 1;
                 i <= 12;
                 i++) {

                int row =
                        (i - 1) / 3;

                int col =
                        (i - 1) % 3;

                float cx =
                        startX +
                                col *
                                        (cardW + gapX) +
                                cardW / 2f;

                float cy =
                        startY +
                                row *
                                        (cardH + gapY) +
                                cardH / 2f;

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
                                    MAX_LEVEL
                            );

                    if (unlocked) {
                        startGame(i);
                    }

                    return;
                }
            }
        }

        // ========================================================
        // PAUSE TOUCH
        // ========================================================

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

                screen =
                        SCREEN_SETTINGS;

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

                screen =
                        SCREEN_HOME;

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
                                MAX_LEVEL
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

                startGame(
                        currentLevel
                );

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

                screen =
                        SCREEN_HOME;

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

                startGame(
                        currentLevel
                );

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

                screen =
                        SCREEN_HOME;

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

            if (y < 90 &&
                    x < 80) {

                screen =
                        SCREEN_HOME;

                invalidate();

                return;
            }

            float base = 145;

            if (y >= base &&
                    y <= base + 55) {

                musicOn =
                        !musicOn;

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

                soundOn =
                        !soundOn;

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

                vibrationOn =
                        !vibrationOn;

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

            if (y < 90 &&
                    x < 80) {

                screen =
                        SCREEN_HOME;

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

            return x >= cx - w / 2 &&
                    x <= cx + w / 2 &&
                    y >= cy - h / 2 &&
                    y <= cy + h / 2;
        }

        private void drawBackground(
                Canvas canvas
        ) {

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

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

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

            if (toneGenerator != null) {

                try {
                    toneGenerator.release();
                } catch (Exception ignored) {
                }
            }

            super.onDetachedFromWindow();
        }
    }

    // ============================================================
    // CANDY MODEL
    // ============================================================

    public static class Candy {

        int type;

        Candy(int type) {
            this.type = type;
        }
    }

    // ============================================================
    // BOARD POSITION
    // ============================================================

    public static class BoardPosition {

        int row;
        int col;

        BoardPosition(
                int row,
                int col
        ) {

            this.row = row;
            this.col = col;
        }
    }
}