package com.candypop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 10;
    private static final int MAX_LEVEL = 100;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random random = new Random();
    private final int[][] board = new int[ROWS][COLS];

    private final List<int[]> selected = new ArrayList<>();
    private final List<int[]> poppingCells = new ArrayList<>();
    private final Path connectionPath = new Path();

    private final Handler timerHandler = new Handler();
    private final Handler animationHandler = new Handler();

    private final GameSound gameSound;

    private float cellSize;
    private float boardLeft;
    private float boardTop;

    private boolean drawing = false;
    private boolean gameFinished = false;
    private boolean animating = false;

    private float lastTouchX;
    private float lastTouchY;
    private float currentFingerX;
    private float currentFingerY;

    private int connectionCandyType = -1;

    private int level = 1;
    private int score = 0;

    private int timeLimit = 100;
    private int secondsLeft = 100;
    private int minimumConnection = 3;

    private int objectiveType = 0;
    private int objectiveTarget = 10;

    private int candiesPoppedThisLevel = 0;
    private int connectionsThisLevel = 0;

    private int longConnectionGoal = 0;
    private int longConnections = 0;

    private int combo = 0;
    private int bestCombo = 0;

    private long popStartTime = 0L;

    private static final int OBJECTIVE_CANDIES = 0;
    private static final int OBJECTIVE_CONNECTIONS = 1;
    private static final int OBJECTIVE_LONG = 2;
    private static final int OBJECTIVE_COMBO = 3;
    private static final int OBJECTIVE_MIXED = 4;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!gameFinished && !animating) {
                if (secondsLeft > 0) {
                    secondsLeft--;
                    invalidate();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    showTimeUpDialog();
                }
            }
        }
    };

    private final Runnable animationRunnable = new Runnable() {
    @Override
    public void run() {

        if (!animating) {
            return;
        }

        long elapsed =
                System.currentTimeMillis() - popStartTime;

        if (elapsed < 260) {
            invalidate();
            animationHandler.postDelayed(this, 16);
            return;
        }

        for (int[] cell : poppingCells) {
            if (cell[0] >= 0 && cell[0] < ROWS
                    && cell[1] >= 0 && cell[1] < COLS) {
                board[cell[0]][cell[1]] = -1;
            }
        }

        refillBoard();

        poppingCells.clear();
        animating = false;

        invalidate();

        // Do not end the level when the objective is reached.
        // Let the countdown determine when the level ends.
        if (secondsLeft <= 0) {
            showTimeUpDialog();
        }
    }
};
 

    public GameView(Context context) {
        super(context);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        paint.setAntiAlias(true);
        shadowPaint.setAntiAlias(true);
        linePaint.setAntiAlias(true);
        lineGlowPaint.setAntiAlias(true);
        outlinePaint.setAntiAlias(true);
        hudPaint.setAntiAlias(true);
        effectPaint.setAntiAlias(true);

        shadowPaint.setStyle(Paint.Style.FILL);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        lineGlowPaint.setStyle(Paint.Style.STROKE);
        lineGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        lineGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        outlinePaint.setStyle(Paint.Style.STROKE);

        gameSound = new GameSound(context);

        createBoard();
        setupLevel();

        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private int getCandyColor(int type) {
        switch (type) {
            case 0:
                return Color.rgb(240, 75, 85);
            case 1:
                return Color.rgb(235, 45, 65);
            case 2:
                return Color.rgb(125, 65, 190);
            case 3:
                return Color.rgb(65, 160, 240);
            case 4:
                return Color.rgb(105, 55, 30);
            case 5:
                return Color.rgb(245, 75, 160);
            case 6:
                return Color.rgb(245, 145, 55);
            case 7:
                return Color.rgb(220, 45, 50);
            case 8:
                return Color.rgb(195, 35, 55);
            default:
                return Color.rgb(90, 190, 75);
        }
    }

    private int[] getCandyColors(int type) {
        int c = getCandyColor(type);

        switch (type) {
            case 0:
                return new int[]{c, Color.rgb(255, 125, 135)};
            case 1:
                return new int[]{c, Color.rgb(255, 120, 135)};
            case 2:
                return new int[]{c, Color.rgb(190, 125, 235)};
            case 3:
                return new int[]{c, Color.rgb(150, 215, 255)};
            case 4:
                return new int[]{c, Color.rgb(175, 105, 65)};
            case 5:
                return new int[]{c, Color.rgb(255, 165, 215)};
            case 6:
                return new int[]{c, Color.rgb(255, 215, 125)};
            case 7:
                return new int[]{c, Color.rgb(255, 125, 125)};
            case 8:
                return new int[]{c, Color.rgb(245, 100, 115)};
            default:
                return new int[]{c, Color.rgb(170, 235, 120)};
        }
    }

    private void setupLevel() {

        score = 0;
        combo = 0;
        bestCombo = 0;

        candiesPoppedThisLevel = 0;
        connectionsThisLevel = 0;
        longConnections = 0;

        int cycle = (level - 1) % 6;
        int stage = (level - 1) / 6;

        if (cycle == 0) {

            timeLimit = 110;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_CANDIES;
            objectiveTarget = 12 + stage * 2;

        } else if (cycle == 1) {

            timeLimit = 70;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_CONNECTIONS;
            objectiveTarget = 7 + stage;

        } else if (cycle == 2) {

            timeLimit = 105;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_CONNECTIONS;
            objectiveTarget = 5 + stage;

        } else if (cycle == 3) {

            timeLimit = 75;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_LONG;
            objectiveTarget = 2 + stage / 2;
            longConnectionGoal = objectiveTarget;

        } else if (cycle == 4) {

            timeLimit = 130;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_CANDIES;
            objectiveTarget = 10 + stage;

        } else {

            timeLimit = 60;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 70 + stage * 10;
            longConnectionGoal = 3 + stage / 2;
        }

        if (level == 10) {
            timeLimit = 120;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_COMBO;
            objectiveTarget = 4;

        } else if (level == 25) {
            timeLimit = 90;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_LONG;
            objectiveTarget = 5;
            longConnectionGoal = 5;

        } else if (level == 50) {
            timeLimit = 120;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_CANDIES;
            objectiveTarget = 80;

        } else if (level == 75) {
            timeLimit = 75;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 150;
            longConnectionGoal = 6;

        } else if (level == 100) {
            timeLimit = 180;
            minimumConnection = 3;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 500;
            longConnectionGoal = 10;
        }

        if (objectiveType != OBJECTIVE_LONG
                && objectiveType != OBJECTIVE_MIXED) {
            longConnectionGoal = 0;
        }

        secondsLeft = timeLimit;

        gameFinished = false;
        animating = false;

        selected.clear();
        poppingCells.clear();
        connectionPath.reset();
        connectionCandyType = -1;

        createBoard();

        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 1000);

        invalidate();
    }

    private boolean levelObjectiveComplete() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:
                return candiesPoppedThisLevel >= objectiveTarget;

            case OBJECTIVE_CONNECTIONS:
                return connectionsThisLevel >= objectiveTarget;

            case OBJECTIVE_LONG:
                return longConnections >= longConnectionGoal;

            case OBJECTIVE_COMBO:
                return bestCombo >= objectiveTarget;

            case OBJECTIVE_MIXED:
                return score >= objectiveTarget
                        && longConnections >= longConnectionGoal;

            default:
                return false;
        }
    }

    private String getObjectiveText() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:
                return "POP "
                        + Math.max(
                        0,
                        objectiveTarget - candiesPoppedThisLevel
                )
                        + " CANDIES";

            case OBJECTIVE_CONNECTIONS:
                return "MAKE "
                        + Math.max(
                        0,
                        objectiveTarget - connectionsThisLevel
                )
                        + " CONNECTIONS";

            case OBJECTIVE_LONG:
                return "LONG CHAINS "
                        + longConnections
                        + "/"
                        + longConnectionGoal;

            case OBJECTIVE_COMBO:
                return "COMBO "
                        + bestCombo
                        + "/"
                        + objectiveTarget;

            case OBJECTIVE_MIXED:
                return "SCORE "
                        + score
                        + "/"
                        + objectiveTarget
                        + " • LONG "
                        + longConnections
                        + "/"
                        + longConnectionGoal;

            default:
                return "";
        }
    }

    private String getObjectiveShortValue() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:
                return candiesPoppedThisLevel
                        + "/" + objectiveTarget;

            case OBJECTIVE_CONNECTIONS:
                return connectionsThisLevel
                        + "/" + objectiveTarget;

            case OBJECTIVE_LONG:
                return longConnections
                        + "/" + longConnectionGoal;

            case OBJECTIVE_COMBO:
                return bestCombo
                        + "/" + objectiveTarget;

            case OBJECTIVE_MIXED:
                return longConnections
                        + "/" + longConnectionGoal;

            default:
                return "0";
        }
    }

    /*
     * ============================================================
     * GUARANTEED PLAYABLE BOARD SYSTEM
     * ============================================================
     *
     * Every fresh board receives at least one guaranteed path.
     *
     * For LONG and MIXED objectives the guaranteed path is at least
     * five candies long.
     *
     * For normal connections it is at least minimumConnection long.
     *
     * This prevents the game from giving the player an objective
     * while the board has no valid connection available.
     */

    private void createBoard() {

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] =
                        random.nextInt(CANDY_TYPES);
            }
        }

        ensureGuaranteedPlayablePath();
    }

    private int getGuaranteedPathLength() {

        int required = minimumConnection;
        if (objectiveType == OBJECTIVE_CONNECTIONS) {
            required = Math.max(required, objectiveTarget);
        }

        if (objectiveType == OBJECTIVE_LONG
                || objectiveType == OBJECTIVE_MIXED) {

            required = Math.max(
                    required,
                    5
            );
        }

        /*
         * A 7x7 board can safely contain a long winding path.
         * Cap the guaranteed path so it remains achievable without
         * forcing the whole board to become one candy type.
         */
        required = Math.min(required, ROWS * COLS);

        return required;
    }

    private void ensureGuaranteedPlayablePath() {

        int required = getGuaranteedPathLength();

        /*
         * First check whether the randomly generated board already
         * contains a valid path.
         */
        if (hasValidConnection(required)) {
            return;
        }

        /*
         * Try several random path constructions.
         */
        for (int attempt = 0; attempt < 100; attempt++) {

            int candyType =
                    random.nextInt(CANDY_TYPES);

            List<int[]> path =
                    buildRandomPath(required);

            if (path.size() >= required) {

                for (int[] cell : path) {

                    board[cell[0]][cell[1]] =
                            candyType;
                }

                if (hasValidConnection(required)) {
                    return;
                }
            }
        }

        /*
         * Final deterministic fallback.
         */
        createGuaranteedSnake(required);
    }

    private List<int[]> buildRandomPath(int required) {

        List<int[]> path =
                new ArrayList<>();

        boolean[][] used =
                new boolean[ROWS][COLS];

        int startRow =
                random.nextInt(ROWS);

        int startCol =
                random.nextInt(COLS);

        path.add(
                new int[]{
                        startRow,
                        startCol
                }
        );

        used[startRow][startCol] = true;

        int[][] directions = {

                {-1, -1},
                {-1, 0},
                {-1, 1},

                {0, -1},
                {0, 1},

                {1, -1},
                {1, 0},
                {1, 1}
        };

        for (int step = 1;
             step < required;
             step++) {

            int[] last =
                    path.get(
                            path.size() - 1
                    );

            List<int[]> possible =
                    new ArrayList<>();

            for (int[] direction : directions) {

                int nr =
                        last[0]
                                + direction[0];

                int nc =
                        last[1]
                                + direction[1];

                if (nr < 0
                        || nr >= ROWS
                        || nc < 0
                        || nc >= COLS) {
                    continue;
                }

                if (used[nr][nc]) {
                    continue;
                }

                possible.add(
                        new int[]{
                                nr,
                                nc
                        }
                );
            }

            if (possible.isEmpty()) {
                break;
            }

            Collections.shuffle(
                    possible,
                    random
            );

            int[] next =
                    possible.get(0);

            path.add(next);

            used[next[0]][next[1]] = true;
        }

        return path;
    }

    private void createGuaranteedSnake(int required) {

        int candyType =
                random.nextInt(CANDY_TYPES);

        List<int[]> path =
                new ArrayList<>();

        /*
         * A winding staircase-style route.
         */
        for (int row = 0;
             row < ROWS && path.size() < required;
             row++) {

            if (row % 2 == 0) {

                for (int col = 0;
                     col < COLS
                             && path.size() < required;
                     col++) {

                    path.add(
                            new int[]{
                                    row,
                                    col
                            }
                    );
                }

            } else {

                for (int col = COLS - 1;
                     col >= 0
                             && path.size() < required;
                     col--) {

                    path.add(
                            new int[]{
                                    row,
                                    col
                            }
                    );
                }
            }
        }

        for (int[] cell : path) {

            board[cell[0]][cell[1]] =
                    candyType;
        }
    }

    private boolean hasValidConnection(int required) {

        if (required <= 1) {
            return true;
        }

        boolean[][] visited =
                new boolean[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                int type =
                        board[row][col];

                if (type < 0) {
                    continue;
                }

                for (int nextRow = 0;
                     nextRow < ROWS;
                     nextRow++) {

                    for (int nextCol = 0;
                         nextCol < COLS;
                         nextCol++) {

                        visited[nextRow][nextCol] =
                                false;
                    }
                }

                if (findPathLength(
                        row,
                        col,
                        type,
                        1,
                        required,
                        visited
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean findPathLength(
            int row,
            int col,
            int type,
            int length,
            int required,
            boolean[][] visited
    ) {

        if (length >= required) {
            return true;
        }

        visited[row][col] = true;

        int[][] directions = {

                {-1, -1},
                {-1, 0},
                {-1, 1},

                {0, -1},
                {0, 1},

                {1, -1},
                {1, 0},
                {1, 1}
        };

        for (int[] direction : directions) {

            int nr =
                    row + direction[0];

            int nc =
                    col + direction[1];

            if (nr < 0
                    || nr >= ROWS
                    || nc < 0
                    || nc >= COLS) {
                continue;
            }

            if (visited[nr][nc]) {
                continue;
            }

            if (board[nr][nc] != type) {
                continue;
            }

            if (findPathLength(
                    nr,
                    nc,
                    type,
                    length + 1,
                    required,
                    visited
            )) {
                return true;
            }
        }

        visited[row][col] = false;

        return false;
    }

    /*
     * ============================================================
     * DRAWING
     * ============================================================
     */

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float scale = width / 360f;

        if (scale < 0.85f) {
            scale = 0.85f;
        }

        drawGameBackground(canvas);

        drawHud(canvas, scale);

        float hudBottom =
                177f * scale;

        float objectiveHeight =
                66f * scale;

        float boardAreaTop =
                hudBottom
                        + 7f * scale;

        float boardAreaBottom =
                height
                        - objectiveHeight
                        - 8f * scale;

        float availableWidth =
                width
                        - 14f * scale;

        float availableHeight =
                boardAreaBottom
                        - boardAreaTop;

        cellSize =
                Math.min(
                        availableWidth / COLS,
                        availableHeight / ROWS
                );

        if (cellSize <= 0f) {
            return;
        }

        float boardWidth =
                cellSize * COLS;

        float boardHeight =
                cellSize * ROWS;

        boardLeft =
                (width - boardWidth)
                        / 2f;

        boardTop =
                boardAreaTop
                        + Math.max(
                        0f,
                        (availableHeight - boardHeight)
                                / 2f
                );

        drawBoardBackground(
                canvas,
                boardLeft,
                boardTop,
                boardWidth,
                boardHeight
        );

        if (selected.size() >= 2) {
            drawConnectionPath(canvas);
        }

        drawBoard(canvas);

        if (!selected.isEmpty()) {
            drawSelection(canvas);
        }

        if (drawing && !selected.isEmpty()) {
            drawFingerPreview(canvas);
        }

        drawCombo(canvas, scale);

        drawObjectivePanel(
                canvas,
                scale,
                height
                        - objectiveHeight
                        - 4f * scale,
                height - 4f * scale
        );
    }

    private void drawGameBackground(Canvas canvas) {

        float width = getWidth();
        float height = getHeight();

        paint.setStyle(Paint.Style.FILL);

        paint.setColor(
                Color.rgb(
                        255,
                        239,
                        248
                )
        );

        canvas.drawRect(
                0,
                0,
                width,
                height,
                paint
        );

        drawBackgroundBubble(
                canvas,
                width * .07f,
                height * .19f,
                32f,
                Color.argb(
                        35,
                        255,
                        80,
                        150
                )
        );

        drawBackgroundBubble(
                canvas,
                width * .93f,
                height * .20f,
                38f,
                Color.argb(
                        35,
                        120,
                        70,
                        220
                )
        );

        drawBackgroundBubble(
                canvas,
                width * .06f,
                height * .76f,
                36f,
                Color.argb(
                        32,
                        70,
                        170,
                        245
                )
        );

        drawBackgroundBubble(
                canvas,
                width * .94f,
                height * .77f,
                38f,
                Color.argb(
                        35,
                        255,
                        165,
                        70
                )
        );

        paint.setColor(
                Color.argb(
                        75,
                        255,
                        130,
                        180
                )
        );

        drawSparkle(
                canvas,
                width * .14f,
                height * .10f,
                6f
        );

        drawSparkle(
                canvas,
                width * .86f,
                height * .10f,
                7f
        );

        drawSparkle(
                canvas,
                width * .10f,
                height * .88f,
                6f
        );

        drawSparkle(
                canvas,
                width * .90f,
                height * .89f,
                7f
        );
    }

    private void drawBackgroundBubble(
            Canvas canvas,
            float cx,
            float cy,
            float radius,
            int color
    ) {

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        canvas.drawCircle(
                cx,
                cy,
                radius,
                paint
        );

        paint.setColor(
                Color.argb(
                        50,
                        255,
                        255,
                        255
                )
        );

        canvas.drawCircle(
                cx - radius * .3f,
                cy - radius * .3f,
                radius * .18f,
                paint
        );
    }

    private void drawSparkle(
            Canvas canvas,
            float cx,
            float cy,
            float size
    ) {

        Path p = new Path();

        p.moveTo(
                cx,
                cy - size
        );

        p.lineTo(
                cx + size * .25f,
                cy - size * .25f
        );

        p.lineTo(
                cx + size,
                cy
        );

        p.lineTo(
                cx + size * .25f,
                cy + size * .25f
        );

        p.lineTo(
                cx,
                cy + size
        );

        p.lineTo(
                cx - size * .25f,
                cy + size * .25f
        );

        p.lineTo(
                cx - size,
                cy
        );

        p.lineTo(
                cx - size * .25f,
                cy - size * .25f
        );

        p.close();

        canvas.drawPath(
                p,
                paint
        );
    }

    private void drawHud(
            Canvas canvas,
            float scale
    ) {

        float width = getWidth();

        hudPaint.setTypeface(
                android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                )
        );

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTextSize(
                25f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        125,
                        45,
                        95
                )
        );

        canvas.drawText(
                "CANDY POP",
                width / 2f,
                28f * scale,
                hudPaint
        );

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        canvas.drawRoundRect(
                new RectF(
                        width / 2f
                                - 55f * scale,
                        37f * scale,
                        width / 2f
                                + 55f * scale,
                        66f * scale
                ),
                15f * scale,
                15f * scale,
                paint
        );

        hudPaint.setTextSize(
                14f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        110,
                        55,
                        90
                )
        );

        canvas.drawText(
                "LEVEL " + level,
                width / 2f,
                57f * scale,
                hudPaint
        );

        float margin =
                8f * scale;

        float gap =
                6f * scale;

        float cardWidth =
                (width
                        - margin * 2f
                        - gap * 2f)
                        / 3f;

        float top =
                73f * scale;

        float bottom =
                161f * scale;

        drawHudBox(
                canvas,
                margin,
                top,
                margin + cardWidth,
                bottom,
                "SCORE",
                String.valueOf(score),
                scale
        );

        drawHudBox(
                canvas,
                margin + cardWidth + gap,
                top,
                margin
                        + cardWidth * 2f
                        + gap,
                bottom,
                "GOAL",
                getObjectiveShortValue(),
                scale
        );

        drawHudBox(
                canvas,
                margin
                        + cardWidth * 2f
                        + gap * 2f,
                top,
                width - margin,
                bottom,
                "TIME",
                String.valueOf(secondsLeft),
                scale
        );
    }

    private void drawHudBox(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            String title,
            String value,
            float scale
    ) {

        shadowPaint.setColor(
                Color.argb(
                        40,
                        80,
                        45,
                        80
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left + 2f * scale,
                        top + 4f * scale,
                        right + 2f * scale,
                        bottom + 4f * scale
                ),
                17f * scale,
                17f * scale,
                shadowPaint
        );

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        canvas.drawRoundRect(
                new RectF(
                        left,
                        top,
                        right,
                        bottom
                ),
                17f * scale,
                17f * scale,
                paint
        );

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(
                2f * scale
        );

        paint.setColor(
                Color.rgb(
                        246,
                        205,
                        225
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left,
                        top,
                        right,
                        bottom
                ),
                17f * scale,
                17f * scale,
                paint
        );

        paint.setStyle(Paint.Style.FILL);

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD
        );

        hudPaint.setTextSize(
                11f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        145,
                        105,
                        130
                )
        );

        canvas.drawText(
                title,
                (left + right) / 2f,
                top + 21f * scale,
                hudPaint
        );

        hudPaint.setTextSize(
                24f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        75,
                        35,
                        70
                )
        );

        canvas.drawText(
                value,
                (left + right) / 2f,
                top + 57f * scale,
                hudPaint
        );
    }

    private void drawBoardBackground(
            Canvas canvas,
            float left,
            float top,
            float width,
            float height
    ) {

        shadowPaint.setColor(
                Color.argb(
                        55,
                        75,
                        40,
                        70
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left + 4f,
                        top + 7f,
                        left + width + 4f,
                        top + height + 7f
                ),
                25f,
                25f,
                shadowPaint
        );

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(
                Color.rgb(
                        255,
                        255,
                        255
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left,
                        top,
                        left + width,
                        top + height
                ),
                25f,
                25f,
                paint
        );

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);

        paint.setColor(
                Color.rgb(
                        248,
                        225,
                        238
                )
        );

        for (int i = 1;
             i < COLS;
             i++) {

            float x =
                    left + i * cellSize;

            canvas.drawLine(
                    x,
                    top + 9f,
                    x,
                    top + height - 9f,
                    paint
            );
        }

        for (int i = 1;
             i < ROWS;
             i++) {

            float y =
                    top + i * cellSize;

            canvas.drawLine(
                    left + 9f,
                    y,
                    left + width - 9f,
                    y,
                    paint
            );
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawBoard(Canvas canvas) {

        for (int row = 0;
             row < ROWS;
             row++) {

            for (int col = 0;
                 col < COLS;
                 col++) {

                if (board[row][col] < 0) {
                    continue;
                }

                drawCandy(
                        canvas,
                        row,
                        col,
                        board[row][col]
                );
            }
        }
    }

    private void drawCandy(
            Canvas canvas,
            int row,
            int col,
            int type
    ) {

        float cx =
                getCellCenterX(col);

        float cy =
                getCellCenterY(row);

        float size =
                cellSize * .34f;

        float scale = 1f;

        if (isPopping(row, col)) {

            long elapsed =
                    System.currentTimeMillis()
                            - popStartTime;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 260f
                    );

            scale =
                    1f
                            - progress * .9f;
        }

        size *= scale;

        shadowPaint.setColor(
                Color.argb(
                        48,
                        70,
                        40,
                        65
                )
        );

        canvas.drawCircle(
                cx + 2f,
                cy + 4f,
                size,
                shadowPaint
        );

        paint.setStyle(Paint.Style.FILL);

        if (type == 0) {

            paint.setColor(
                    Color.rgb(
                            55,
                            175,
                            85
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    size,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            240,
                            75,
                            85
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    size * .82f,
                    paint
            );

            paint.setColor(Color.BLACK);

            canvas.drawOval(
                    new RectF(
                            cx - size * .20f,
                            cy - size * .35f,
                            cx - size * .10f,
                            cy - size * .12f
                    ),
                    paint
            );

            canvas.drawOval(
                    new RectF(
                            cx + size * .10f,
                            cy - size * .20f,
                            cx + size * .20f,
                            cy + size * .03f
                    ),
                    paint
            );

            canvas.drawOval(
                    new RectF(
                            cx - size * .05f,
                            cy + size * .12f,
                            cx + size * .05f,
                            cy + size * .35f
                    ),
                    paint
            );

        } else if (type == 1) {

            paint.setColor(
                    Color.rgb(
                            235,
                            45,
                            65
                    )
            );

            Path p = new Path();

            p.moveTo(
                    cx,
                    cy + size
            );

            p.cubicTo(
                    cx - size * 1.05f,
                    cy + size * .25f,
                    cx - size * .75f,
                    cy - size * .85f,
                    cx,
                    cy - size * .55f
            );

            p.cubicTo(
                    cx + size * .75f,
                    cy - size * .85f,
                    cx + size * 1.05f,
                    cy + size * .25f,
                    cx,
                    cy + size
            );

            canvas.drawPath(
                    p,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            45,
                            170,
                            70
                    )
            );

            Path leaves = new Path();

            leaves.moveTo(
                    cx,
                    cy - size * .55f
            );

            leaves.lineTo(
                    cx - size * .55f,
                    cy - size * .90f
            );

            leaves.lineTo(
                    cx - size * .15f,
                    cy - size * .30f
            );

            leaves.lineTo(
                    cx,
                    cy - size * .95f
            );

            leaves.lineTo(
                    cx + size * .18f,
                    cy - size * .30f
            );

            leaves.lineTo(
                    cx + size * .60f,
                    cy - size * .85f
            );

            leaves.close();

            canvas.drawPath(
                    leaves,
                    paint
            );

            paint.setColor(Color.WHITE);

            for (int i = -1;
                 i <= 1;
                 i++) {

                canvas.drawOval(
                        new RectF(
                                cx
                                        + i * size * .28f
                                        - 2f,
                                cy - size * .10f,
                                cx
                                        + i * size * .28f
                                        + 2f,
                                cy + size * .10f
                        ),
                        paint
                );
            }

        } else if (type == 2) {

            paint.setColor(
                    Color.rgb(
                            125,
                            65,
                            190
                    )
            );

            float r =
                    size * .34f;

            canvas.drawCircle(
                    cx,
                    cy - size * .50f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * .35f,
                    cy - size * .18f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .35f,
                    cy - size * .18f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * .50f,
                    cy + size * .20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx,
                    cy + size * .20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .50f,
                    cy + size * .20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * .25f,
                    cy + size * .55f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .25f,
                    cy + size * .55f,
                    r,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            55,
                            150,
                            65
                    )
            );

            canvas.drawOval(
                    new RectF(
                            cx - size * .20f,
                            cy - size * .95f,
                            cx + size * .35f,
                            cy - size * .55f
                    ),
                    paint
            );

        } else if (type == 3) {

            paint.setColor(
                    Color.rgb(
                            65,
                            160,
                            240
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            cx - size * .65f,
                            cy - size * .42f,
                            cx + size * .65f,
                            cy + size * .42f
                    ),
                    size * .18f,
                    size * .18f,
                    paint
            );

            Path left = new Path();

            left.moveTo(
                    cx - size * .60f,
                    cy - size * .35f
            );

            left.lineTo(
                    cx - size * 1.05f,
                    cy - size * .70f
            );

            left.lineTo(
                    cx - size * .90f,
                    cy
            );

            left.lineTo(
                    cx - size * 1.05f,
                    cy + size * .70f
            );

            left.lineTo(
                    cx - size * .60f,
                    cy + size * .35f
            );

            left.close();

            canvas.drawPath(
                    left,
                    paint
            );

            Path right = new Path();

            right.moveTo(
                    cx + size * .60f,
                    cy - size * .35f
            );

            right.lineTo(
                    cx + size * 1.05f,
                    cy - size * .70f
            );

            right.lineTo(
                    cx + size * .90f,
                    cy
            );

            right.lineTo(
                    cx + size * 1.05f,
                    cy + size * .70f
            );

            right.lineTo(
                    cx + size * .60f,
                    cy + size * .35f
            );

            right.close();

            canvas.drawPath(
                    right,
                    paint
            );

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    cx - size * .25f,
                    cy - size * .18f,
                    size * .10f,
                    paint
            );

        } else if (type == 4) {

            paint.setColor(
                    Color.rgb(
                            105,
                            55,
                            30
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            cx - size * .85f,
                            cy - size * .65f,
                            cx + size * .85f,
                            cy + size * .65f
                    ),
                    size * .15f,
                    size * .15f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            175,
                            105,
                            65
                    )
            );

            float piece =
                    size * .27f;

            for (int r = -1;
                 r <= 1;
                 r++) {

                for (int c = -1;
                     c <= 1;
                     c++) {

                    canvas.drawRoundRect(
                            new RectF(
                                    cx
                                            + c * piece * 1.8f
                                            - piece,
                                    cy
                                            + r * piece * 1.7f
                                            - piece,
                                    cx
                                            + c * piece * 1.8f
                                            + piece,
                                    cy
                                            + r * piece * 1.7f
                                            + piece
                            ),
                            4f,
                            4f,
                            paint
                    );
                }
            }

        } else if (type == 5) {

            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(
                    size * .14f
            );

            canvas.drawLine(
                    cx,
                    cy,
                    cx,
                    cy + size * 1.10f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            245,
                            75,
                            160
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy - size * .15f,
                    size * .78f,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    size * .12f
            );

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    cx,
                    cy - size * .15f,
                    size * .50f,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

        } else if (type == 6) {

            paint.setColor(
                    Color.rgb(
                            135,
                            80,
                            45
                    )
            );

            paint.setStrokeWidth(
                    size * .10f
            );

            canvas.drawLine(
                    cx,
                    cy - size * .95f,
                    cx,
                    cy + size * .95f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            125,
                            160
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy - size * .55f,
                    size * .34f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            255,
                            230,
                            185
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    size * .34f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            105,
                            190,
                            105
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy + size * .55f,
                    size * .34f,
                    paint
            );

        } else if (type == 7) {

            drawApple(
                    canvas,
                    cx,
                    cy,
                    size,
                    Color.rgb(
                            220,
                            45,
                            50
                    )
            );

        } else if (type == 8) {

            paint.setColor(
                    Color.rgb(
                            60,
                            145,
                            65
                    )
            );

            paint.setStrokeWidth(
                    size * .08f
            );

            canvas.drawLine(
                    cx - size * .20f,
                    cy - size * .05f,
                    cx - size * .45f,
                    cy - size * .85f,
                    paint
            );

            canvas.drawLine(
                    cx + size * .20f,
                    cy - size * .05f,
                    cx + size * .45f,
                    cy - size * .85f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            195,
                            35,
                            55
                    )
            );

            canvas.drawCircle(
                    cx - size * .35f,
                    cy + size * .35f,
                    size * .48f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * .35f,
                    cy + size * .35f,
                    size * .48f,
                    paint
            );

        } else {

            drawApple(
                    canvas,
                    cx,
                    cy,
                    size,
                    Color.rgb(
                            90,
                            190,
                            75
                    )
            );
        }

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(
                Color.argb(
                        125,
                        255,
                        255,
                        255
                )
        );

        canvas.drawCircle(
                cx - size * .28f,
                cy - size * .30f,
                size * .13f,
                paint
        );

        outlinePaint.setStyle(
                Paint.Style.STROKE
        );

        outlinePaint.setStrokeWidth(2f);

        outlinePaint.setColor(
                Color.argb(
                        100,
                        255,
                        255,
                        255
                )
        );

        canvas.drawCircle(
                cx,
                cy,
                size,
                outlinePaint
        );
    }

    private void drawApple(
            Canvas canvas,
            float cx,
            float cy,
            float size,
            int color
    ) {

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(color);

        Path apple = new Path();

        apple.moveTo(
                cx,
                cy + size * .95f
        );

        apple.cubicTo(
                cx - size * 1.05f,
                cy + size * .25f,
                cx - size * .70f,
                cy - size * .75f,
                cx,
                cy - size * .35f
        );

        apple.cubicTo(
                cx + size * .70f,
                cy - size * .75f,
                cx + size * 1.05f,
                cy + size * .25f,
                cx,
                cy + size * .95f
        );

        canvas.drawPath(
                apple,
                paint
        );

        paint.setColor(
                Color.rgb(
                        75,
                        125,
                        45
                )
        );

        canvas.drawRect(
                cx - 3f,
                cy - size * .72f,
                cx + 3f,
                cy - size * .35f,
                paint
        );

        paint.setColor(
                Color.rgb(
                        55,
                        155,
                        65
                )
        );

        canvas.drawOval(
                new RectF(
                        cx,
                        cy - size * .85f,
                        cx + size * .55f,
                        cy - size * .50f
                ),
                paint
        );
    }

    private boolean isPopping(
            int row,
            int col
    ) {

        for (int[] cell : poppingCells) {

            if (cell[0] == row
                    && cell[1] == col) {

                return true;
            }
        }

        return false;
    }

    private void drawConnectionPath(
            Canvas canvas
    ) {

        if (selected.size() < 2) {
            return;
        }

        connectionPath.reset();

        int[] first =
                selected.get(0);

        connectionPath.moveTo(
                getCellCenterX(first[1]),
                getCellCenterY(first[0])
        );

        for (int i = 1;
             i < selected.size();
             i++) {

            int[] cell =
                    selected.get(i);

            connectionPath.lineTo(
                    getCellCenterX(cell[1]),
                    getCellCenterY(cell[0])
            );
        }

        int color =
                getCandyColor(
                        connectionCandyType
                );

        lineGlowPaint.setColor(
                Color.argb(
                        90,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                )
        );

        lineGlowPaint.setStrokeWidth(
                Math.max(
                        20f,
                        cellSize * .32f
                )
        );

        canvas.drawPath(
                connectionPath,
                lineGlowPaint
        );

        linePaint.setColor(color);

        linePaint.setStrokeWidth(
                Math.max(
                        9f,
                        cellSize * .16f
                )
        );

        canvas.drawPath(
                connectionPath,
                linePaint
        );

        linePaint.setColor(
                Color.argb(
                        110,
                        255,
                        255,
                        255
                )
        );

        linePaint.setStrokeWidth(
                Math.max(
                        2f,
                        cellSize * .035f
                )
        );

        canvas.drawPath(
                connectionPath,
                linePaint
        );
    }

    private void drawFingerPreview(
            Canvas canvas
    ) {

        if (selected.isEmpty()) {
            return;
        }

        int[] last =
                selected.get(
                        selected.size() - 1
                );

        int[] fingerCell =
                getCell(
                        currentFingerX,
                        currentFingerY
                );

        if (fingerCell == null) {
            return;
        }

        if (!isAdjacent(
                last,
                fingerCell
        )) {
            return;
        }

        if (board[fingerCell[0]][fingerCell[1]]
                != connectionCandyType) {
            return;
        }

        int color =
                getCandyColor(
                        connectionCandyType
                );

        linePaint.setColor(
                Color.argb(
                        120,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                )
        );

        linePaint.setStrokeWidth(
                Math.max(
                        6f,
                        cellSize * .10f
                )
        );

        canvas.drawLine(
                getCellCenterX(last[1]),
                getCellCenterY(last[0]),
                currentFingerX,
                currentFingerY,
                linePaint
        );
    }

    private void drawSelection(
            Canvas canvas
    ) {

        for (int[] cell : selected) {

            float cx =
                    getCellCenterX(
                            cell[1]
                    );

            float cy =
                    getCellCenterY(
                            cell[0]
                    );

            float radius =
                    cellSize * .40f;

            int color =
                    getCandyColor(
                            board[cell[0]][cell[1]]
                    );

            effectPaint.setStyle(
                    Paint.Style.STROKE
            );

            effectPaint.setStrokeWidth(
                    Math.max(
                            4f,
                            cellSize * .045f
                    )
            );

            effectPaint.setColor(
                    Color.argb(
                            170,
                            Color.red(color),
                            Color.green(color),
                            Color.blue(color)
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    radius,
                    effectPaint
            );

            effectPaint.setStrokeWidth(2f);
            effectPaint.setColor(Color.WHITE);

            canvas.drawCircle(
                    cx,
                    cy,
                    radius - 4f,
                    effectPaint
            );
        }
    }

    private void drawCombo(
            Canvas canvas,
            float scale
    ) {

        if (combo <= 1) {
            return;
        }

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTypeface(
                android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                )
        );

        hudPaint.setTextSize(
                19f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        225,
                        70,
                        125
                )
        );

        canvas.drawText(
                "🔥 COMBO x" + combo,
                getWidth() / 2f,
                getHeight()
                        - 76f * scale,
                hudPaint
        );
    }

    private void drawObjectivePanel(
            Canvas canvas,
            float scale,
            float top,
            float bottom
    ) {

        float width =
                getWidth();

        float left =
                10f * scale;

        float right =
                width - 10f * scale;

        shadowPaint.setColor(
                Color.argb(
                        45,
                        80,
                        40,
                        75
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left + 2f * scale,
                        top + 3f * scale,
                        right + 2f * scale,
                        bottom + 3f * scale
                ),
                20f * scale,
                20f * scale,
                shadowPaint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        paint.setColor(Color.WHITE);

        canvas.drawRoundRect(
                new RectF(
                        left,
                        top,
                        right,
                        bottom
                ),
                20f * scale,
                20f * scale,
                paint
        );

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(
                2f * scale
        );

        paint.setColor(
                Color.rgb(
                        245,
                        205,
                        225
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left,
                        top,
                        right,
                        bottom
                ),
                20f * scale,
                20f * scale,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        hudPaint.setTypeface(
                android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                )
        );

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTextSize(
                11f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        155,
                        105,
                        135
                )
        );

        canvas.drawText(
                "OBJECTIVE",
                width / 2f,
                top + 18f * scale,
                hudPaint
        );

        hudPaint.setTextSize(
                16f * scale
        );

        hudPaint.setColor(
                Color.rgb(
                        90,
                        40,
                        80
                )
        );

        String objective =
                getObjectiveText();

        if (objective.length() > 40) {

            objective =
                    objective.substring(
                            0,
                            40
                    )
                            + "...";
        }

        canvas.drawText(
                objective,
                width / 2f,
                top + 43f * scale,
                hudPaint
        );
    }

    /*
     * ============================================================
     * TOUCH / CONNECTION SYSTEM
     * ============================================================
     */

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (gameFinished || animating) {
            return true;
        }

        float x =
                event.getX();

        float y =
                event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                int[] startCell =
                        getCell(
                                x,
                                y
                        );

                if (startCell == null) {
                    return true;
                }

                if (board[startCell[0]][startCell[1]]
                        < 0) {
                    return true;
                }

                drawing = true;

                lastTouchX = x;
                lastTouchY = y;

                currentFingerX = x;
                currentFingerY = y;

                selected.clear();
                connectionPath.reset();

                connectionCandyType =
                        board[
                                startCell[0]
                        ][
                                startCell[1]
                        ];

                selected.add(
                        new int[]{
                                startCell[0],
                                startCell[1]
                        }
                );

                invalidate();

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawing) {
                    return true;
                }

                currentFingerX = x;
                currentFingerY = y;

                processTouchMovement(
                        lastTouchX,
                        lastTouchY,
                        x,
                        y
                );

                lastTouchX = x;
                lastTouchY = y;

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:

                if (!drawing) {
                    return true;
                }

                drawing = false;

                currentFingerX = x;
                currentFingerY = y;

                if (selected.size()
                        >= minimumConnection) {

                    removeSelected();

                } else {

                    selected.clear();
                    connectionPath.reset();
                    connectionCandyType = -1;
                    combo = 0;

                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_CANCEL:

                drawing = false;

                selected.clear();
                connectionPath.reset();
                connectionCandyType = -1;

                invalidate();

                return true;
        }

        return true;
    }

    private void processTouchMovement(
            float fromX,
            float fromY,
            float toX,
            float toY
    ) {

        float dx =
                toX - fromX;

        float dy =
                toY - fromY;

        float distance =
                (float) Math.sqrt(
                        dx * dx
                                + dy * dy
                );

        float step =
                Math.max(
                        2f,
                        cellSize * .06f
                );

        int samples =
                Math.max(
                        1,
                        (int) Math.ceil(
                                distance / step
                        )
                );

        int[] previousSampleCell = null;

        for (int i = 1;
             i <= samples;
             i++) {

            float fraction =
                    i / (float) samples;

            float sampleX =
                    fromX
                            + dx * fraction;

            float sampleY =
                    fromY
                            + dy * fraction;

            int[] cell =
                    getCell(
                            sampleX,
                            sampleY
                    );

            if (cell == null) {
                continue;
            }

            if (previousSampleCell != null
                    && sameCell(
                    previousSampleCell,
                    cell
            )) {
                continue;
            }

            previousSampleCell = cell;

            handleTouchedCell(cell);
        }
    }

    private void handleTouchedCell(
            int[] cell
    ) {

        if (cell == null
                || selected.isEmpty()) {
            return;
        }

        int[] last =
                selected.get(
                        selected.size() - 1
                );

        if (sameCell(last, cell)) {
            return;
        }

        if (selected.size() >= 2) {

            int[] previous =
                    selected.get(
                            selected.size() - 2
                    );

            if (sameCell(
                    previous,
                    cell
            )) {

                backtrackOneCell();
                return;
            }
        }

        if (!isAdjacent(
                last,
                cell
        )) {
            return;
        }

        if (board[cell[0]][cell[1]]
                != connectionCandyType) {
            return;
        }

        if (alreadySelected(cell)) {
            return;
        }

        tryAddCell(cell);
    }

    private void tryAddCell(
            int[] cell
    ) {

        if (cell == null) {
            return;
        }

        if (cell[0] < 0
                || cell[0] >= ROWS
                || cell[1] < 0
                || cell[1] >= COLS) {
            return;
        }

        if (board[cell[0]][cell[1]]
                != connectionCandyType) {
            return;
        }

        if (alreadySelected(cell)) {
            return;
        }

        selected.add(
                new int[]{
                        cell[0],
                        cell[1]
                }
        );

        try {

            if (gameSound != null) {
                gameSound.playPop();
            }

        } catch (Exception ignored) {
        }

        rebuildConnectionPath();

        invalidate();
    }

    private void backtrackOneCell() {

        if (selected.size() <= 1) {
            return;
        }

        selected.remove(
                selected.size() - 1
        );

        rebuildConnectionPath();

        invalidate();
    }

    private void rebuildConnectionPath() {

        connectionPath.reset();

        if (selected.isEmpty()) {
            return;
        }

        int[] first =
                selected.get(0);

        connectionPath.moveTo(
                getCellCenterX(first[1]),
                getCellCenterY(first[0])
        );

        for (int i = 1;
             i < selected.size();
             i++) {

            int[] cell =
                    selected.get(i);

            connectionPath.lineTo(
                    getCellCenterX(cell[1]),
                    getCellCenterY(cell[0])
            );
        }
    }

    private boolean isAdjacent(
            int[] a,
            int[] b
    ) {

        if (a == null || b == null) {
            return false;
        }

        int rowDifference =
                Math.abs(
                        a[0] - b[0]
                );

        int colDifference =
                Math.abs(
                        a[1] - b[1]
                );

        return rowDifference <= 1
                && colDifference <= 1
                && (
                rowDifference != 0
                        || colDifference != 0
        );
    }

    private boolean alreadySelected(
            int[] cell
    ) {

        for (int[] selectedCell :
                selected) {

            if (sameCell(
                    selectedCell,
                    cell
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean sameCell(
            int[] a,
            int[] b
    ) {

        return a != null
                && b != null
                && a[0] == b[0]
                && a[1] == b[1];
    }

    private int[] getCell(
            float x,
            float y
    ) {

        if (cellSize <= 0f) {
            return null;
        }

        int col =
                (int)
                        (
                                (x - boardLeft)
                                        / cellSize
                        );

        int row =
                (int)
                        (
                                (y - boardTop)
                                        / cellSize
                        );

        if (row < 0
                || row >= ROWS
                || col < 0
                || col >= COLS) {
            return null;
        }

        return new int[]{
                row,
                col
        };
    }

    private float getCellCenterX(
            int col
    ) {

        return boardLeft
                + col * cellSize
                + cellSize / 2f;
    }

    private float getCellCenterY(
            int row
    ) {

        return boardTop
                + row * cellSize
                + cellSize / 2f;
    }

    /*
     * ============================================================
     * POP / SCORE / REFILL
     * ============================================================
     */

    private void removeSelected() {

        if (selected.size()
                < minimumConnection) {
            return;
        }

        poppingCells.clear();

        for (int[] cell : selected) {

            poppingCells.add(
                    new int[]{
                            cell[0],
                            cell[1]
                    }
            );
        }

        int connectionLength =
                selected.size();

        int points =
                connectionLength
                        * connectionLength;

        if (connectionLength >= 5) {
            points += 10;
        }

        if (connectionLength >= 6) {
            points += 15;
        }

        if (connectionLength >= 7) {
            points += 25;
        }

        score += points;

        candiesPoppedThisLevel +=
                connectionLength;

        connectionsThisLevel++;

        combo++;

        if (combo > bestCombo) {
            bestCombo = combo;
        }

        if (connectionLength >= 5) {
            longConnections++;
        }

        try {

            if (gameSound != null) {
                gameSound.playPop();
            }

        } catch (Exception ignored) {
        }

        popStartTime =
                System.currentTimeMillis();

        animating = true;

        selected.clear();

        connectionPath.reset();

        connectionCandyType = -1;

        animationHandler.removeCallbacks(
                animationRunnable
        );

        animationHandler.post(
                animationRunnable
        );

        invalidate();
    }

    private void refillBoard() {

        for (int col = 0;
             col < COLS;
             col++) {

            int writeRow =
                    ROWS - 1;

            for (int row = ROWS - 1;
                 row >= 0;
                 row--) {

                if (board[row][col] >= 0) {

                    board[writeRow][col] =
                            board[row][col];

                    writeRow--;
                }
            }

            while (writeRow >= 0) {

                board[writeRow][col] =
                        random.nextInt(
                                CANDY_TYPES
                        );

                writeRow--;
            }
        }

        /*
         * IMPORTANT:
         * After every refill we guarantee that the new board
         * contains a playable connection.
         */
        ensureGuaranteedPlayablePath();
    }

    /*
     * ============================================================
     * LEVEL COMPLETE / TIME UP
     * ============================================================
     */

    private void levelComplete() {

        if (gameFinished) {
            return;
        }

        gameFinished = true;

        timerHandler.removeCallbacks(
                timerRunnable
        );

        String message;

        if (level >= MAX_LEVEL) {

            message =
                    "Amazing! You completed all 100 levels!\n\n"
                            + "Final score: "
                            + score
                            + "\nBest combo: x"
                            + bestCombo;

        } else {

            message =
                    "Level "
                            + level
                            + " complete!\n\n"
                            + "Score: "
                            + score
                            + "\nBest combo: x"
                            + bestCombo;
        }

        new AlertDialog.Builder(
                getContext()
        )
                .setTitle(
                        level >= MAX_LEVEL
                                ? "🎉 GAME COMPLETE!"
                                : "🍬 LEVEL COMPLETE!"
                )
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        level >= MAX_LEVEL
                                ? "PLAY AGAIN"
                                : "NEXT LEVEL",
                        (dialog, which) -> {

                            if (level >= MAX_LEVEL) {
                                level = 1;
                            } else {
                                level++;
                            }

                            setupLevel();
                        }
                )
                .setNegativeButton(
                        "HOME",
                        (dialog, which) ->
                                goHome()
                )
                .show();
    }

    private void showTimeUpDialog() {

    if (gameFinished || animating) {
        return;
    }

    timerHandler.removeCallbacks(timerRunnable);

    // If the objective is complete, finish the level.
    if (levelObjectiveComplete()) {
        levelComplete();
        return;
    }

    // Otherwise, the objective was not completed in time.
    gameFinished = true;

    new AlertDialog.Builder(getContext())
            .setTitle("⏰ TIME'S UP!")
            .setMessage(
                    "Your score: " + score
                            + "\n\n"
                            + "Objective:\n"
                            + getObjectiveText()
            )
            .setCancelable(false)
            .setPositiveButton(
                    "RETRY",
                    (dialog, which) -> restartLevel()
            )
            .setNegativeButton(
                    "HOME",
                    (dialog, which) -> goHome()
            )
            .show();
}

    private void restartLevel() {

        gameFinished = false;

        score = 0;
        combo = 0;
        bestCombo = 0;

        candiesPoppedThisLevel = 0;
        connectionsThisLevel = 0;
        longConnections = 0;

        secondsLeft = timeLimit;

        selected.clear();
        poppingCells.clear();
        connectionPath.reset();

        connectionCandyType = -1;

        animating = false;

        createBoard();

        timerHandler.removeCallbacks(
                timerRunnable
        );

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );

        invalidate();
    }

    private void goHome() {

        try {

            Context context =
                    getContext();

            Intent intent =
                    new Intent(
                            context,
                            MainActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            context.startActivity(intent);

            if (context instanceof Activity) {
                ((Activity) context).finish();
            }

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDetachedFromWindow() {

        timerHandler.removeCallbacks(
                timerRunnable
        );

        animationHandler.removeCallbacks(
                animationRunnable
        );

        try {

            if (gameSound != null) {
                gameSound.release();
            }

        } catch (Exception ignored) {
        }

        super.onDetachedFromWindow();
    }
}
