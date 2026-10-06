package com.candypop.game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
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

    public static class CandyGameView extends View {

        private static final int SCREEN_HOME = 0;
        private static final int SCREEN_LEVELS = 1;
        private static final int SCREEN_GAMEPLAY = 2;
        private static final int SCREEN_COMPLETE = 3;
        private static final int SCREEN_GAMEOVER = 4;
        private static final int SCREEN_SETTINGS = 5;
        private static final int SCREEN_SCORES = 6;

        private static final int MAX_LEVEL = 30;

        private static final int ROWS = 7;
        private static final int COLUMNS = 6;

        private int screen = SCREEN_HOME;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final Handler handler = new Handler();
        private final SharedPreferences prefs;

        private final Candy[][] board =
                new Candy[ROWS][COLUMNS];

        private int currentLevel = 1;
        private int score = 0;
        private int highScore = 0;
        private int timeLeft = 60;
        private int combo = 0;

        private int target = 20;
        private int collected = 0;

        private boolean paused = false;

        private boolean musicOn = true;
        private boolean soundOn = true;
        private boolean vibrationOn = true;

        private float touchDownX;
        private float touchDownY;

        private boolean moving = false;

        private final int[] candyColors = {
                Color.rgb(245, 72, 105),   // pink
                Color.rgb(255, 190, 45),   // yellow
                Color.rgb(80, 190, 105),   // green
                Color.rgb(70, 145, 235),   // blue
                Color.rgb(180, 90, 220),   // purple
                Color.rgb(255, 125, 55)    // orange
        };

        private final String[] candyNames = {
                "Heart",
                "Lemon",
                "Mint",
                "Blueberry",
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

            highScore =
                    prefs.getInt("highScore", 0);

            currentLevel =
                    prefs.getInt("currentLevel", 1);

            if (currentLevel < 1) {
                currentLevel = 1;
            }

            if (currentLevel > MAX_LEVEL) {
                currentLevel = MAX_LEVEL;
            }

            musicOn =
                    prefs.getBoolean("music", true);

            soundOn =
                    prefs.getBoolean("sound", true);

            vibrationOn =
                    prefs.getBoolean("vibration", true);

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            setFocusable(true);
        }

        // =========================================================
        // TIMER
        // =========================================================

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

        // =========================================================
        // DRAW
        // =========================================================

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            canvas.drawColor(
                    Color.rgb(250, 244, 255)
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

        // =========================================================
        // HOME
        // =========================================================

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

            float cx =
                    getWidth() / 2f;

            float cy = 245;

            drawRealCandy(
                    canvas,
                    cx - 70,
                    cy,
                    34,
                    0,
                    0
            );

            drawRealCandy(
                    canvas,
                    cx,
                    cy - 20,
                    34,
                    1,
                    1
            );

            drawRealCandy(
                    canvas,
                    cx + 70,
                    cy,
                    34,
                    2,
                    2
            );
        }

        // =========================================================
        // LEVELS
        // =========================================================

        private void drawLevels(Canvas canvas) {

            drawBackground(canvas);

            drawHeader(
                    canvas,
                    "Level Select",
                    true,
                    true
            );

            int cardW = 105;
            int cardH = 82;

            int startY = 120;
            int gapX = 15;
            int gapY = 15;

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
                        col * (cardW + gapX);

                float y =
                        startY +
                        row * (cardH + gapY);

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
                    getHeight() - 25,
                    14,
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
                            ? Color.rgb(150, 110, 220)
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
                    y + 34,
                    25,
                    unlocked
                            ? Color.rgb(70, 70, 80)
                            : Color.GRAY,
                    true
            );

            if (unlocked) {

                int stars =
                        getStars(level);

                drawText(
                        canvas,
                        getStarString(stars),
                        x + w / 2,
                        y + 66,
                        19,
                        Color.rgb(255, 190, 45),
                        true
                );

            } else {

                drawText(
                        canvas,
                        "LOCKED",
                        x + w / 2,
                        y + 65,
                        12,
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

        // =========================================================
        // GAMEPLAY
        // =========================================================

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
                            10,
                            12,
                            getWidth() - 10,
                            90
                    ),
                    22,
                    22,
                    paint
            );

            drawText(
                    canvas,
                    "TIME",
                    50,
                    40,
                    12,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    timeLeft + "s",
                    50,
                    69,
                    23,
                    timeLeft <= 10
                            ? Color.RED
                            : Color.rgb(65, 65, 75),
                    true
            );

            drawText(
                    canvas,
                    "SCORE",
                    getWidth() / 2f,
                    40,
                    12,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    String.valueOf(score),
                    getWidth() / 2f,
                    69,
                    23,
                    Color.rgb(70, 70, 80),
                    true
            );

            drawButton(
                    canvas,
                    "PAUSE",
                    getWidth() - 58,
                    51,
                    78,
                    42,
                    Color.rgb(245, 105, 115)
            );
        }

        private void drawBoard(Canvas canvas) {

            float boardTop = 105;

            float boardBottom =
                    getHeight() - 145;

            paint.setColor(
                    Color.rgb(238, 226, 247)
            );

            canvas.drawRoundRect(
                    new RectF(
                            10,
                            boardTop,
                            getWidth() - 10,
                            boardBottom
                    ),
                    25,
                    25,
                    paint
            );

            float boardW =
                    getWidth() - 30;

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
                            15 +
                            c * cellW +
                            cellW / 2f;

                    float cy =
                            boardTop +
                            10 +
                            r * cellH +
                            cellH / 2f;

                    float size =
                            Math.min(
                                    cellW,
                                    cellH
                            ) * 0.34f;

                    drawCandyTile(
                            canvas,
                            cx,
                            cy,
                            size,
                            candy
                    );
                }
            }
        }

        // =========================================================
        // REAL CANDY DRAWING
        // =========================================================

        private void drawCandyTile(
                Canvas canvas,
                float cx,
                float cy,
                float size,
                Candy candy
        ) {

            drawRealCandy(
                    canvas,
                    cx,
                    cy,
                    size,
                    candy.colorIndex,
                    candy.shape
            );

            if (candy.selected) {

                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(5);

                paint.setColor(Color.WHITE);

                canvas.drawCircle(
                        cx,
                        cy,
                        size + 7,
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );
            }
        }

        private void drawRealCandy(
                Canvas canvas,
                float cx,
                float cy,
                float size,
                int colorIndex,
                int shape
        ) {

            int color =
                    candyColors[
                            colorIndex %
                            candyColors.length
                    ];

            paint.setColor(color);

            // soft shadow
            paint.setColor(
                    Color.argb(
                            45,
                            40,
                            30,
                            50
                    )
            );

            canvas.drawCircle(
                    cx + 2,
                    cy + 4,
                    size,
                    paint
            );

            paint.setColor(color);

            if (shape == 0) {

                // HEART CANDY

                Path heart =
                        new Path();

                heart.moveTo(
                        cx,
                        cy + size
                );

                heart.cubicTo(
                        cx - size * 1.45f,
                        cy - size * .05f,
                        cx - size * .75f,
                        cy - size * 1.25f,
                        cx,
                        cy - size * .35f
                );

                heart.cubicTo(
                        cx + size * .75f,
                        cy - size * 1.25f,
                        cx + size * 1.45f,
                        cy - size * .05f,
                        cx,
                        cy + size
                );

                heart.close();

                canvas.drawPath(
                        heart,
                        paint
                );

            } else if (shape == 1) {

                // ROUND LOLLIPOP CANDY

                canvas.drawCircle(
                        cx,
                        cy,
                        size,
                        paint
                );

                paint.setColor(Color.WHITE);

                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(3);

                canvas.drawCircle(
                        cx,
                        cy,
                        size * .68f,
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );

            } else if (shape == 2) {

                // WRAPPED CANDY

                Path left =
                        new Path();

                left.moveTo(
                        cx - size,
                        cy - size * .55f
                );

                left.lineTo(
                        cx - size * 1.55f,
                        cy - size
                );

                left.lineTo(
                        cx - size * 1.55f,
                        cy + size
                );

                left.lineTo(
                        cx - size,
                        cy + size * .55f
                );

                left.close();

                canvas.drawPath(
                        left,
                        paint
                );

                Path right =
                        new Path();

                right.moveTo(
                        cx + size,
                        cy - size * .55f
                );

                right.lineTo(
                        cx + size * 1.55f,
                        cy - size
                );

                right.lineTo(
                        cx + size * 1.55f,
                        cy + size
                );

                right.lineTo(
                        cx + size,
                        cy + size * .55f
                );

                right.close();

                canvas.drawPath(
                        right,
                        paint
                );

                canvas.drawRoundRect(
                        new RectF(
                                cx - size,
                                cy - size * .7f,
                                cx + size,
                                cy + size * .7f
                        ),
                        size * .25f,
                        size * .25f,
                        paint
                );

                paint.setColor(Color.WHITE);

                paint.setStrokeWidth(4);

                canvas.drawLine(
                        cx - size * .55f,
                        cy - size * .45f,
                        cx + size * .55f,
                        cy + size * .45f,
                        paint
                );

            } else if (shape == 3) {

                // STAR CANDY

                Path star =
                        new Path();

                for (int i = 0; i < 10; i++) {

                    double angle =
                            -Math.PI / 2
                            + i * Math.PI / 5;

                    float radius =
                            i % 2 == 0
                                    ? size
                                    : size * .45f;

                    float px =
                            cx +
                            (float) Math.cos(angle)
                                    * radius;

                    float py =
                            cy +
                            (float) Math.sin(angle)
                                    * radius;

                    if (i == 0) {
                        star.moveTo(px, py);
                    } else {
                        star.lineTo(px, py);
                    }
                }

                star.close();

                canvas.drawPath(
                        star,
                        paint
                );
            } else if (shape == 4) {

                // GEM CANDY

                Path gem =
                        new Path();

                gem.moveTo(
                        cx,
                        cy - size
                );

                gem.lineTo(
                        cx + size,
                        cy - size * .25f
                );

                gem.lineTo(
                        cx + size * .65f,
                        cy + size
                );

                gem.lineTo(
                        cx - size * .65f,
                        cy + size
                );

                gem.lineTo(
                        cx - size,
                        cy - size * .25f
                );

                gem.close();

                canvas.drawPath(
                        gem,
                        paint
                );
            } else {

                // ROUND CANDY

                canvas.drawCircle(
                        cx,
                        cy,
                        size,
                        paint
                );

                paint.setColor(Color.WHITE);

                canvas.drawCircle(
                        cx - size * .3f,
                        cy - size * .3f,
                        size * .16f,
                        paint
                );
            }

            // candy shine

            paint.setColor(
                    Color.argb(
                            150,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    cx - size * .30f,
                    cy - size * .30f,
                    size * .12f,
                    paint
            );
        }

        private void drawGameBottom(Canvas canvas) {

            float y =
                    getHeight() - 120;

            paint.setColor(Color.WHITE);

            canvas.drawRoundRect(
                    new RectF(
                            12,
                            y,
                            getWidth() - 12,
                            getHeight() - 10
                    ),
                    22,
                    22,
                    paint
            );

            drawText(
                    canvas,
                    "TARGET",
                    85,
                    y + 28,
                    11,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    collected +
                            "/" +
                            target,
                    85,
                    y + 66,
                    22,
                    Color.rgb(245, 75, 100),
                    true
            );

            drawText(
                    canvas,
                    "COMBO",
                    getWidth() - 90,
                    y + 28,
                    11,
                    Color.GRAY,
                    true
            );

            drawText(
                    canvas,
                    combo + "x",
                    getWidth() - 90,
                    y + 66,
                    25,
                    Color.rgb(180, 100, 220),
                    true
            );
        }

        // =========================================================
        // BOARD CREATION
        // =========================================================

        private void createBoard() {

            for (int r = 0;
                 r < ROWS;
                 r++) {

                for (int c = 0;
                     c < COLUMNS;
                     c++) {

                    board[r][c] =
                            createSafeCandy(
                                    r,
                                    c
                            );
                }
            }
        }

        private Candy createSafeCandy(
                int row,
                int col
        ) {

            for (int attempt = 0;
                 attempt < 50;
                 attempt++) {

                int color =
                        random.nextInt(
                                candyColors.length
                        );

                if (col >= 2 &&
                        board[row][col - 1] != null &&
                        board[row][col - 2] != null &&
                        board[row][col - 1].colorIndex == color &&
                        board[row][col - 2].colorIndex == color) {
                    continue;
                }

                if (row >= 2 &&
                        board[row - 1][col] != null &&
                        board[row - 2][col] != null &&
                        board[row - 1][col].colorIndex == color &&
                        board[row - 2][col].colorIndex == color) {
                    continue;
                }

                return new Candy(
                        row,
                        col,
                        color,
                        random.nextInt(6)
                );
            }

            return new Candy(
                    row,
                    col,
                    random.nextInt(
                            candyColors.length
                    ),
                    random.nextInt(6)
            );
        }

        // =========================================================
        // SWIPE MATCH SYSTEM
        // =========================================================

        private void handleSwipe(
                float startX,
                float startY,
                float endX,
                float endY
        ) {

            if (moving || paused) {
                return;
            }

            float dx =
                    endX - startX;

            float dy =
                    endY - startY;

            float distance =
                    (float) Math.sqrt(
                            dx * dx +
                            dy * dy
                    );

            if (distance < 35) {
                return;
            }

            Cell start =
                    getCell(
                            startX,
                            startY
                    );

            if (start == null) {
                return;
            }

            int targetRow =
                    start.row;

            int targetCol =
                    start.col;

            if (Math.abs(dx) >
                    Math.abs(dy)) {

                if (dx > 0) {
                    targetCol++;
                } else {
                    targetCol--;
                }

            } else {

                if (dy > 0) {
                    targetRow++;
                } else {
                    targetRow--;
                }
            }

            if (targetRow < 0 ||
                    targetRow >= ROWS ||
                    targetCol < 0 ||
                    targetCol >= COLUMNS) {
                return;
            }

            swapCandies(
                    start.row,
                    start.col,
                    targetRow,
                    targetCol
            );

            Set<String> matches =
                    findMatches();

            if (matches.isEmpty()) {

                // illegal move:
                // swap back

                swapCandies(
                        start.row,
                        start.col,
                        targetRow,
                        targetCol
                );

                combo = 0;

                invalidate();

                return;
            }

            moving = true;

            resolveMatches();
        }

        private Cell getCell(
                float x,
                float y
        ) {

            float boardTop = 105;

            float boardBottom =
                    getHeight() - 145;

            if (y < boardTop ||
                    y > boardBottom) {
                return null;
            }

            float boardW =
                    getWidth() - 30;

            float cellW =
                    boardW / COLUMNS;

            float cellH =
                    (boardBottom -
                            boardTop -
                            20) / ROWS;

            int col =
                    (int) ((x - 15) / cellW);

            int row =
                    (int) (
                            (y -
                                    boardTop -
                                    10)
                                    / cellH
                    );

            if (row < 0 ||
                    row >= ROWS ||
                    col < 0 ||
                    col >= COLUMNS) {

                return null;
            }

            return new Cell(
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

            if (board[r1][c1] != null) {
                board[r1][c1].row = r1;
                board[r1][c1].col = c1;
            }

            if (board[r2][c2] != null) {
                board[r2][c2].row = r2;
                board[r2][c2].col = c2;
            }
        }

        private Set<String> findMatches() {

            Set<String> matches =
                    new HashSet<>();

            // horizontal

            for (int r = 0;
                 r < ROWS;
                 r++) {

                int start = 0;

                while (start < COLUMNS) {

                    Candy first =
                            board[r][start];

                    if (first == null) {
                        start++;
                        continue;
                    }

                    int end =
                            start + 1;

                    while (end < COLUMNS &&
                            board[r][end] != null &&
                            board[r][end].colorIndex ==
                                    first.colorIndex) {

                        end++;
                    }

                    if (end - start >= 3) {

                        for (int c = start;
                             c < end;
                             c++) {

                            matches.add(
                                    key(r, c)
                            );
                        }
                    }

                    start = end;
                }
            }

            // vertical

            for (int c = 0;
                 c < COLUMNS;
                 c++) {

                int start = 0;

                while (start < ROWS) {

                    Candy first =
                            board[start][c];

                    if (first == null) {
                        start++;
                        continue;
                    }

                    int end =
                            start + 1;

                    while (end < ROWS &&
                            board[end][c] != null &&
                            board[end][c].colorIndex ==
                                    first.colorIndex) {

                        end++;
                    }

                    if (end - start >= 3) {

                        for (int r = start;
                             r < end;
                             r++) {

                            matches.add(
                                    key(r, c)
                            );
                        }
                    }

                    start = end;
                }
            }

            return matches;
        }

        private void resolveMatches() {

            Set<String> matches =
                    findMatches();

            if (matches.isEmpty()) {

                moving = false;

                checkWinCondition();

                invalidate();

                return;
            }

            combo++;

            int gained =
                    matches.size()
                            * 20
                            * Math.max(
                                    1,
                                    combo
                            );

            score += gained;

            for (String position :
                    matches) {

                String[] parts =
                        position.split(",");

                int r =
                        Integer.parseInt(
                                parts[0]
                        );

                int c =
                        Integer.parseInt(
                                parts[1]
                        );

                if (board[r][c] != null) {

                    if (board[r][c].colorIndex == 0) {
                        collected++;
                    }

                    board[r][c] = null;
                }
            }

            // small pause creates a
            // satisfying disappearance effect

            invalidate();

            handler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {

                            collapseBoard();

                            refillBoard();

                            invalidate();

                            handler.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {

                                            Set<String>
                                                    next =
                                                    findMatches();

                                            if (!next.isEmpty()) {

                                                resolveMatches();

                                            } else {

                                                moving = false;

                                                checkWinCondition();

                                                invalidate();
                                            }
                                        }
                                    },
                                    120
                            );
                        }
                    },
                    100
            );
        }

        private void collapseBoard() {

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

                        candy.row =
                                writeRow;

                        candy.col =
                                c;

                        writeRow--;
                    }
                }

                while (writeRow >= 0) {

                    board[writeRow][c] =
                            null;

                    writeRow--;
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
                                        r,
                                        c,
                                        random.nextInt(
                                                candyColors.length
                                        ),
                                        random.nextInt(6)
                                );
                    }
                }
            }
        }

        private String key(
                int row,
                int col
        ) {

            return row + "," + col;
        }

        // =========================================================
        // WIN / GAME OVER
        // =========================================================

        private void checkWinCondition() {

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
            }
        }

        private void startGame(
                int level
        ) {

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
                    18 +
                    currentLevel * 3;

            timeLeft =
                    Math.max(
                            35,
                            65 -
                            currentLevel
                    );

            paused = false;

            moving = false;

            createBoard();

            screen =
                    SCREEN_GAMEPLAY;

            startTimer();

            invalidate();
        }

        private void finishGame(
                boolean won
        ) {

            stopTimer();

            moving = false;

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
                                Math.max(
                                        stars,
                                        getStars(
                                                currentLevel
                                        )
                                )
                        )
                        .apply();

                if (currentLevel < MAX_LEVEL) {

                    int unlocked =
                            currentLevel + 1;

                    int saved =
                            prefs.getInt(
                                    "currentLevel",
                                    1
                            );

                    if (unlocked > saved) {

                        prefs.edit()
                                .putInt(
                                        "currentLevel",
                                        unlocked
                                )
                                .apply();
                    }
                }

                screen =
                        SCREEN_COMPLETE;

            } else {

                screen =
                        SCREEN_GAMEOVER;
            }

            invalidate();
        }

        private int calculateStars() {

            if (score >= target * 40) {
                return 3;
            }

            if (score >= target * 27) {
                return 2;
            }

            return 1;
        }

        private String getStarString(
                int stars
        ) {

            String result = "";

            for (int i = 0; i < 3; i++) {

                result +=
                        i < stars
                                ? "★"
                                : "☆";
            }

            return result;
        }

        // =========================================================
        // TOUCH
        // =========================================================

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            float x =
                    event.getX();

            float y =
                    event.getY();

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                touchDownX = x;
                touchDownY = y;

                return true;
            }

            if (event.getAction() ==
                    MotionEvent.ACTION_UP) {

                switch (screen) {

                    case SCREEN_HOME:
                        handleHomeTouch(
                                x,
                                y
                        );
                        break;

                    case SCREEN_LEVELS:
                        handleLevelsTouch(
                                x,
                                y
                        );
                        break;

                    case SCREEN_GAMEPLAY:

                        if (paused) {

                            handlePauseTouch(
                                    x,
                                    y
                            );

                        } else if (
                                touchDownY < 100 &&
                                touchDownX >
                                        getWidth() - 115) {

                            paused = true;

                            stopTimer();

                            invalidate();

                        } else {

                            handleSwipe(
                                    touchDownX,
                                    touchDownY,
                                    x,
                                    y
                            );
                        }

                        break;

                    case SCREEN_COMPLETE:
                        handleCompleteTouch(
                                x,
                                y
                        );
                        break;

                    case SCREEN_GAMEOVER:
                        handleGameOverTouch(
                                x,
                                y
                        );
                        break;

                    case SCREEN_SETTINGS:
                        handleSettingsTouch(
                                x,
                                y
                        );
                        break;

                    case SCREEN_SCORES:
                        handleScoresTouch(
                                x,
                                y
                        );
                        break;
                }

                return true;
            }

            return true;
        }

        // =========================================================
        // HOME TOUCH
        // =========================================================

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

            if (x > getWidth() - 80 &&
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

        // =========================================================
        // LEVEL TOUCH
        // =========================================================

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
                    x > getWidth() - 80) {

                screen =
                        SCREEN_HOME;

                invalidate();

                return;
            }

            int cardW = 105;
            int cardH = 82;
            int gapX = 15;
            int gapY = 15;

            int startY = 120;

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

        // =========================================================
        // PAUSE
        // =========================================================

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

                startGame(
                        currentLevel
                );

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

        // =========================================================
        // COMPLETE
        // =========================================================

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

        // =========================================================
        // GAME OVER
        // =========================================================

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

        // =========================================================
        // SETTINGS
        // =========================================================

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

        // =========================================================
        // SCORES
        // =========================================================

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

        // =========================================================
        // SETTINGS SCREEN DRAW
        // =========================================================

        private void drawSettings(
                Canvas canvas
        ) {

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

        // =========================================================
        // HIGH SCORES
        // =========================================================

        private void drawHighScores(
                Canvas canvas
        ) {

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

                paint.setColor(
                        Color.WHITE
                );

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
                        155,
                        y + 42,
                        15,
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

        private String getFlag(
                int index
        ) {

            String[] flags = {
                    "🇱🇸",
                    "🇿🇦",
                    "🇧🇼",
                    "🇳🇦",
                    "🇿🇼"
            };

            return flags[index];
        }

        // =========================================================
        // COMPLETE SCREEN
        // =========================================================

        private void drawComplete(
                Canvas canvas
        ) {

            drawBackground(canvas);

            drawTitle(
                    canvas,
                    "LEVEL " +
                            currentLevel +
                            " COMPLETE!",
                    getWidth() / 2f,
                    110,
                    28
            );

            int stars =
                    getStars(
                            currentLevel
                    );

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
                    "Amazing!",
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
                    currentLevel < MAX_LEVEL
                            ? "NEXT LEVEL"
                            : "PLAY AGAIN",
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

        // =========================================================
        // GAME OVER
        // =========================================================

        private void drawGameOver(
                Canvas canvas
        ) {

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
                    String.valueOf(
                            highScore
                    ),
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

        // =========================================================
        // PAUSE
        // =========================================================

        private void drawPauseOverlay(
                Canvas canvas
        ) {

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

        // =========================================================
        // HEADER
        // =========================================================

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

        private void drawMuteButton(
                Canvas canvas
        ) {

            drawButton(
                    canvas,
                    musicOn
                            ? "🔊"
                            : "🔇",
                    getWidth() - 45,
                    45,
                    55,
                    45,
                    Color.WHITE
            );
        }

        // =========================================================
        // HELPERS
        // =========================================================

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

        // =========================================================
        // CLEANUP
        // =========================================================

        @Override
        protected void onDetachedFromWindow() {

            stopTimer();

            super.onDetachedFromWindow();
        }
    }

    // =============================================================
    // CANDY
    // =============================================================

    public static class Candy {

        int row;
        int col;

        int colorIndex;
        int shape;

        boolean selected = false;

        Candy(
                int row,
                int col,
                int colorIndex,
                int shape
        ) {

            this.row = row;
            this.col = col;

            this.colorIndex =
                    colorIndex;

            this.shape = shape;
        }
    }

    // =============================================================
    // CELL
    // =============================================================

    public static class Cell {

        int row;
        int col;

        Cell(
                int row,
                int col
        ) {

            this.row = row;
            this.col = col;
        }
    }
}