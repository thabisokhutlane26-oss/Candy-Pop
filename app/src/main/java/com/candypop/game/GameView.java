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

    /*
     * Objective types:
     *
     * 0 = POP CANDIES
     * 1 = MAKE CONNECTIONS
     * 2 = LONG CONNECTIONS
     * 3 = COMBO
     * 4 = MIXED SCORE + LONG CONNECTION
     */
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

                animationHandler.postDelayed(
                        this,
                        16
                );

                return;
            }

            for (int[] cell : poppingCells) {

                if (cell[0] >= 0
                        && cell[0] < ROWS
                        && cell[1] >= 0
                        && cell[1] < COLS) {

                    board[cell[0]][cell[1]] = -1;
                }
            }

            refillBoard();

            poppingCells.clear();

            animating = false;

            invalidate();

            if (levelObjectiveComplete()) {

                levelComplete();
            }
        }
    };

    public GameView(Context context) {

        super(context);

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );

        paint.setAntiAlias(true);

        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);

        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        lineGlowPaint.setAntiAlias(true);
        lineGlowPaint.setStyle(Paint.Style.STROKE);
        lineGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        lineGlowPaint.setStrokeJoin(Paint.Join.ROUND);

        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Paint.Style.STROKE);

        hudPaint.setAntiAlias(true);
        effectPaint.setAntiAlias(true);

        gameSound = new GameSound(context);

        createBoard();

        setupLevel();

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );
    }

    private int[] getCandyColors(int type) {

        switch (type) {

            case 0:
                return new int[]{
                        Color.rgb(240, 75, 85),
                        Color.rgb(255, 125, 135)
                };

            case 1:
                return new int[]{
                        Color.rgb(235, 45, 65),
                        Color.rgb(255, 120, 135)
                };

            case 2:
                return new int[]{
                        Color.rgb(125, 65, 190),
                        Color.rgb(190, 125, 235)
                };

            case 3:
                return new int[]{
                        Color.rgb(65, 160, 240),
                        Color.rgb(150, 215, 255)
                };

            case 4:
                return new int[]{
                        Color.rgb(105, 55, 30),
                        Color.rgb(175, 105, 65)
                };

            case 5:
                return new int[]{
                        Color.rgb(245, 75, 160),
                        Color.rgb(255, 165, 215)
                };

            case 6:
                return new int[]{
                        Color.rgb(245, 145, 55),
                        Color.rgb(255, 215, 125)
                };

            case 7:
                return new int[]{
                        Color.rgb(220, 45, 50),
                        Color.rgb(255, 125, 125)
                };

            case 8:
                return new int[]{
                        Color.rgb(195, 35, 55),
                        Color.rgb(245, 100, 115)
                };

            default:
                return new int[]{
                        Color.rgb(90, 190, 75),
                        Color.rgb(170, 235, 120)
                };
        }
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

    /*
     * REAL 100-LEVEL PROGRESSION
     *
     * The game deliberately varies difficulty.
     * It does NOT simply become harder every level.
     */
    private void setupLevel() {

        /*
         * Reset level statistics.
         */
        score = 0;
        combo = 0;
        bestCombo = 0;

        candiesPoppedThisLevel = 0;
        connectionsThisLevel = 0;
        longConnections = 0;

        /*
         * LEVEL PATTERN
         *
         * 1  Easy
         * 2  Hard
         * 3  Easy
         * 4  Hard
         * 5  Easiest
         * 6  Hardest
         *
         * Then the pattern repeats with increasing
         * objectives across the 100 levels.
         */

        int cycle = (level - 1) % 6;

        int stage = (level - 1) / 6;

        if (cycle == 0) {

            // EASY
            timeLimit = 110;

            minimumConnection = 3;

            objectiveType =
                    OBJECTIVE_CANDIES;

            objectiveTarget =
                    12 + stage * 2;

        } else if (cycle == 1) {

            // HARD
            timeLimit = 70;

            minimumConnection = 4;

            objectiveType =
                    OBJECTIVE_CONNECTIONS;

            objectiveTarget =
                    7 + stage;

        } else if (cycle == 2) {

            // EASY
            timeLimit = 105;

            minimumConnection = 3;

            objectiveType =
                    OBJECTIVE_CONNECTIONS;

            objectiveTarget =
                    5 + stage;

        } else if (cycle == 3) {

            // HARD
            timeLimit = 75;

            minimumConnection = 4;

            objectiveType =
                    OBJECTIVE_LONG;

            objectiveTarget =
                    2 + stage / 2;

            longConnectionGoal =
                    objectiveTarget;

        } else if (cycle == 4) {

            // EASIEST / BONUS STYLE
            timeLimit = 130;

            minimumConnection = 3;

            objectiveType =
                    OBJECTIVE_CANDIES;

            objectiveTarget =
                    10 + stage;

        } else {

            // HARDEST
            timeLimit = 60;

            minimumConnection = 4;

            objectiveType =
                    OBJECTIVE_MIXED;

            objectiveTarget =
                    70 + stage * 10;

            longConnectionGoal =
                    3 + stage / 2;
        }

        /*
         * Special milestone levels.
         */
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

        /*
         * Never allow an invalid long-connection goal.
         */
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

        timerHandler.removeCallbacks(
                timerRunnable
        );

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );

        invalidate();
    }

    private boolean levelObjectiveComplete() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:

                return candiesPoppedThisLevel
                        >= objectiveTarget;

            case OBJECTIVE_CONNECTIONS:

                return connectionsThisLevel
                        >= objectiveTarget;

            case OBJECTIVE_LONG:

                return longConnections
                        >= longConnectionGoal;

            case OBJECTIVE_COMBO:

                return bestCombo
                        >= objectiveTarget;

            case OBJECTIVE_MIXED:

                return score >= objectiveTarget
                        && longConnections
                        >= longConnectionGoal;

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
                        objectiveTarget
                                - candiesPoppedThisLevel
                )
                        + " CANDIES";

            case OBJECTIVE_CONNECTIONS:

                return "MAKE "
                        + Math.max(
                        0,
                        objectiveTarget
                                - connectionsThisLevel
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

    private void createBoard() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                board[row][col] =
                        random.nextInt(
                                CANDY_TYPES
                        );
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawColor(
                Color.rgb(
                        255,
                        242,
                        248
                )
        );

        drawHud(canvas);

        float availableWidth =
                getWidth() - 24f;

        float availableHeight =
                getHeight() - 180f;

        cellSize =
                Math.min(
                        availableWidth / COLS,
                        availableHeight / ROWS
                );

        if (cellSize < 1f) {
            return;
        }

        float boardWidth =
                cellSize * COLS;

        float boardHeight =
                cellSize * ROWS;

        boardLeft =
                (getWidth() - boardWidth) / 2f;

        boardTop = 155f;

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

        if (selected.size() > 0) {
            drawSelection(canvas);
        }

        if (drawing
                && selected.size() > 0) {

            drawFingerPreview(canvas);
        }

        drawCombo(canvas);
    }

    private void drawHud(Canvas canvas) {

        hudPaint.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD
        );

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTextSize(23f);

        hudPaint.setColor(
                Color.rgb(
                        75,
                        40,
                        65
                )
        );

        canvas.drawText(
                "CANDY POP",
                getWidth() / 2f,
                34f,
                hudPaint
        );

        hudPaint.setTextSize(15f);

        canvas.drawText(
                "Level " + level,
                getWidth() / 2f,
                58f,
                hudPaint
        );

        drawHudBox(
                canvas,
                12f,
                72f,
                108f,
                122f,
                "SCORE",
                String.valueOf(score)
        );

        drawHudBox(
                canvas,
                128f,
                72f,
                236f,
                122f,
                "OBJECTIVE",
                getObjectiveShortValue()
        );

        drawHudBox(
                canvas,
                getWidth() - 120f,
                72f,
                getWidth() - 12f,
                122f,
                "TIME",
                String.valueOf(secondsLeft)
        );

        hudPaint.setTextSize(11f);

        hudPaint.setColor(
                Color.rgb(
                        120,
                        80,
                        105
                )
        );

        canvas.drawText(
                getObjectiveText(),
                getWidth() / 2f,
                143f,
                hudPaint
        );
    }

    private String getObjectiveShortValue() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:
                return candiesPoppedThisLevel
                        + "/"
                        + objectiveTarget;

            case OBJECTIVE_CONNECTIONS:
                return connectionsThisLevel
                        + "/"
                        + objectiveTarget;

            case OBJECTIVE_LONG:
                return longConnections
                        + "/"
                        + longConnectionGoal;

            case OBJECTIVE_COMBO:
                return bestCombo
                        + "/"
                        + objectiveTarget;

            case OBJECTIVE_MIXED:
                return longConnections
                        + "/"
                        + longConnectionGoal;

            default:
                return "0";
        }
    }

    private void drawHudBox(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            String title,
            String value
    ) {

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
                18f,
                18f,
                paint
        );

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(2f);

        paint.setColor(
                Color.rgb(
                        245,
                        210,
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
                18f,
                18f,
                paint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTextSize(10f);

        hudPaint.setColor(
                Color.rgb(
                        145,
                        110,
                        130
                )
        );

        canvas.drawText(
                title,
                (left + right) / 2f,
                top + 18f,
                hudPaint
        );

        hudPaint.setTextSize(18f);

        hudPaint.setColor(
                Color.rgb(
                        75,
                        40,
                        65
                )
        );

        canvas.drawText(
                value,
                (left + right) / 2f,
                top + 42f,
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
                        35,
                        90,
                        50,
                        80
                )
        );

        canvas.drawRoundRect(
                new RectF(
                        left + 4f,
                        top + 6f,
                        left + width + 4f,
                        top + height + 6f
                ),
                25f,
                25f,
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
                        left + width,
                        top + height
                ),
                25f,
                25f,
                paint
        );
    }

    private void drawBoard(Canvas canvas) {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

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
                cellSize * 0.34f;

        boolean popping =
                isPopping(row, col);

        float scale = 1f;

        if (popping) {

            long elapsed =
                    System.currentTimeMillis()
                            - popStartTime;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 260f
                    );

            scale =
                    1f - progress * 0.9f;
        }

        size *= scale;

        shadowPaint.setColor(
                Color.argb(
                        45,
                        80,
                        50,
                        70
                )
        );

        canvas.drawCircle(
                cx + 2f,
                cy + 4f,
                size,
                shadowPaint
        );

        paint.setStyle(
                Paint.Style.FILL
        );

        outlinePaint.setStyle(
                Paint.Style.STROKE
        );

        outlinePaint.setStrokeWidth(2f);

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
                    size * 0.82f,
                    paint
            );

            paint.setColor(Color.BLACK);

            canvas.drawOval(
                    new RectF(
                            cx - size * 0.20f,
                            cy - size * 0.35f,
                            cx - size * 0.10f,
                            cy - size * 0.12f
                    ),
                    paint
            );

            canvas.drawOval(
                    new RectF(
                            cx + size * 0.10f,
                            cy - size * 0.20f,
                            cx + size * 0.20f,
                            cy + size * 0.03f
                    ),
                    paint
            );

            canvas.drawOval(
                    new RectF(
                            cx - size * 0.05f,
                            cy + size * 0.12f,
                            cx + size * 0.05f,
                            cy + size * 0.35f
                    ),
                    paint
            );

        } else if (type == 1) {

            Path strawberry = new Path();

            strawberry.moveTo(
                    cx,
                    cy + size
            );

            strawberry.cubicTo(
                    cx - size * 1.05f,
                    cy + size * 0.25f,
                    cx - size * 0.75f,
                    cy - size * 0.85f,
                    cx,
                    cy - size * 0.55f
            );

            strawberry.cubicTo(
                    cx + size * 0.75f,
                    cy - size * 0.85f,
                    cx + size * 1.05f,
                    cy + size * 0.25f,
                    cx,
                    cy + size
            );

            paint.setColor(
                    Color.rgb(
                            235,
                            45,
                            65
                    )
            );

            canvas.drawPath(
                    strawberry,
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
                    cy - size * 0.55f
            );

            leaves.lineTo(
                    cx - size * 0.55f,
                    cy - size * 0.90f
            );

            leaves.lineTo(
                    cx - size * 0.15f,
                    cy - size * 0.30f
            );

            leaves.lineTo(
                    cx,
                    cy - size * 0.95f
            );

            leaves.lineTo(
                    cx + size * 0.18f,
                    cy - size * 0.30f
            );

            leaves.lineTo(
                    cx + size * 0.60f,
                    cy - size * 0.85f
            );

            leaves.close();

            canvas.drawPath(
                    leaves,
                    paint
            );

            paint.setColor(Color.WHITE);

            for (int i = -1; i <= 1; i++) {

                canvas.drawOval(
                        new RectF(
                                cx + i * size * 0.28f - 2f,
                                cy - size * 0.10f,
                                cx + i * size * 0.28f + 2f,
                                cy + size * 0.10f
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

            float r = size * 0.34f;

            canvas.drawCircle(
                    cx,
                    cy - size * 0.50f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * 0.35f,
                    cy - size * 0.18f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * 0.35f,
                    cy - size * 0.18f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * 0.50f,
                    cy + size * 0.20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx,
                    cy + size * 0.20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * 0.50f,
                    cy + size * 0.20f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx - size * 0.25f,
                    cy + size * 0.55f,
                    r,
                    paint
            );

            canvas.drawCircle(
                    cx + size * 0.25f,
                    cy + size * 0.55f,
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
                            cx - size * 0.20f,
                            cy - size * 0.95f,
                            cx + size * 0.35f,
                            cy - size * 0.55f
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
                            cx - size * 0.65f,
                            cy - size * 0.42f,
                            cx + size * 0.65f,
                            cy + size * 0.42f
                    ),
                    size * 0.18f,
                    size * 0.18f,
                    paint
            );

            Path leftWrap = new Path();

            leftWrap.moveTo(
                    cx - size * 0.60f,
                    cy - size * 0.35f
            );

            leftWrap.lineTo(
                    cx - size * 1.05f,
                    cy - size * 0.70f
            );

            leftWrap.lineTo(
                    cx - size * 0.90f,
                    cy
            );

            leftWrap.lineTo(
                    cx - size * 1.05f,
                    cy + size * 0.70f
            );

            leftWrap.lineTo(
                    cx - size * 0.60f,
                    cy + size * 0.35f
            );

            leftWrap.close();

            canvas.drawPath(
                    leftWrap,
                    paint
            );

            Path rightWrap = new Path();

            rightWrap.moveTo(
                    cx + size * 0.60f,
                    cy - size * 0.35f
            );

            rightWrap.lineTo(
                    cx + size * 1.05f,
                    cy - size * 0.70f
            );

            rightWrap.lineTo(
                    cx + size * 0.90f,
                    cy
            );

            rightWrap.lineTo(
                    cx + size * 1.05f,
                    cy + size * 0.70f
            );

            rightWrap.lineTo(
                    cx + size * 0.60f,
                    cy + size * 0.35f
            );

            rightWrap.close();

            canvas.drawPath(
                    rightWrap,
                    paint
            );

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    cx - size * 0.25f,
                    cy - size * 0.18f,
                    size * 0.10f,
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
                            cx - size * 0.85f,
                            cy - size * 0.65f,
                            cx + size * 0.85f,
                            cy + size * 0.65f
                    ),
                    size * 0.15f,
                    size * 0.15f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            160,
                            90,
                            50
                    )
            );

            float piece = size * 0.27f;

            for (int r = -1; r <= 1; r++) {

                for (int c = -1; c <= 1; c++) {

                    canvas.drawRoundRect(
                            new RectF(
                                    cx + c * piece * 1.8f - piece,
                                    cy + r * piece * 1.7f - piece,
                                    cx + c * piece * 1.8f + piece,
                                    cy + r * piece * 1.7f + piece
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
                    size * 0.14f
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
                    cy - size * 0.15f,
                    size * 0.78f,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    size * 0.12f
            );

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    cx,
                    cy - size * 0.15f,
                    size * 0.50f,
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
                    size * 0.10f
            );

            canvas.drawLine(
                    cx,
                    cy - size * 0.95f,
                    cx,
                    cy + size * 0.95f,
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
                    cy - size * 0.55f,
                    size * 0.34f,
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
                    size * 0.34f,
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
                    cy + size * 0.55f,
                    size * 0.34f,
                    paint
            );

        } else if (type == 7) {

            paint.setColor(
                    Color.rgb(
                            220,
                            45,
                            50
                    )
            );

            Path apple = new Path();

            apple.moveTo(
                    cx,
                    cy + size * 0.95f
            );

            apple.cubicTo(
                    cx - size * 1.05f,
                    cy + size * 0.25f,
                    cx - size * 0.70f,
                    cy - size * 0.75f,
                    cx,
                    cy - size * 0.35f
            );

            apple.cubicTo(
                    cx + size * 0.70f,
                    cy - size * 0.75f,
                    cx + size * 1.05f,
                    cy + size * 0.25f,
                    cx,
                    cy + size * 0.95f
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
                    cy - size * 0.72f,
                    cx + 3f,
                    cy - size * 0.35f,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            55,
                            160,
                            70
                    )
            );

            canvas.drawOval(
                    new RectF(
                            cx,
                            cy - size * 0.85f,
                            cx + size * 0.55f,
                            cy - size * 0.50f
                    ),
                    paint
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
                    size * 0.08f
            );

            canvas.drawLine(
                    cx - size * 0.20f,
                    cy - size * 0.05f,
                    cx - size * 0.45f,
                    cy - size * 0.85f,
                    paint
            );

            canvas.drawLine(
                    cx + size * 0.20f,
                    cy - size * 0.05f,
                    cx + size * 0.45f,
                    cy - size * 0.85f,
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
                    cx - size * 0.35f,
                    cy + size * 0.35f,
                    size * 0.48f,
                    paint
            );

            canvas.drawCircle(
                    cx + size * 0.35f,
                    cy + size * 0.35f,
                    size * 0.48f,
                    paint
            );

        } else {

            paint.setColor(
                    Color.rgb(
                            90,
                            190,
                            75
                    )
            );

            Path apple = new Path();

            apple.moveTo(
                    cx,
                    cy + size * 0.95f
            );

            apple.cubicTo(
                    cx - size * 1.05f,
                    cy + size * 0.25f,
                    cx - size * 0.70f,
                    cy - size * 0.75f,
                    cx,
                    cy - size * 0.35f
            );

            apple.cubicTo(
                    cx + size * 0.70f,
                    cy - size * 0.75f,
                    cx + size * 1.05f,
                    cy + size * 0.25f,
                    cx,
                    cy + size * 0.95f
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
                    cy - size * 0.72f,
                    cx + 3f,
                    cy - size * 0.35f,
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
                            cx,
                            cy - size * 0.85f,
                            cx + size * 0.55f,
                            cy - size * 0.50f
                    ),
                    paint
            );
        }

        paint.setColor(
                Color.argb(
                        120,
                        255,
                        255,
                        255
                )
        );

        canvas.drawCircle(
                cx - size * 0.28f,
                cy - size * 0.30f,
                size * 0.13f,
                paint
        );

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

    private void drawConnectionPath(Canvas canvas) {

        if (selected.size() < 2) {
            return;
        }

        connectionPath.reset();

        int[] first = selected.get(0);

        connectionPath.moveTo(
                getCellCenterX(first[1]),
                getCellCenterY(first[0])
        );

        for (int i = 1;
             i < selected.size();
             i++) {

            int[] cell = selected.get(i);

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
                        75,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                )
        );

        lineGlowPaint.setStrokeWidth(
                Math.max(
                        18f,
                        cellSize * 0.30f
                )
        );

        canvas.drawPath(
                connectionPath,
                lineGlowPaint
        );

        linePaint.setColor(color);

        linePaint.setStrokeWidth(
                Math.max(
                        8f,
                        cellSize * 0.16f
                )
        );

        canvas.drawPath(
                connectionPath,
                linePaint
        );
    }

    private void drawFingerPreview(Canvas canvas) {

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
                        110,
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)
                )
        );

        linePaint.setStrokeWidth(
                Math.max(
                        5f,
                        cellSize * 0.10f
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

    private void drawSelection(Canvas canvas) {

        for (int i = 0;
             i < selected.size();
             i++) {

            int[] cell =
                    selected.get(i);

            float cx =
                    getCellCenterX(cell[1]);

            float cy =
                    getCellCenterY(cell[0]);

            float radius =
                    cellSize * 0.40f;

            int color =
                    getCandyColor(
                            board[cell[0]][cell[1]]
                    );

            effectPaint.setStyle(
                    Paint.Style.STROKE
            );

            effectPaint.setStrokeWidth(4f);

            effectPaint.setColor(
                    Color.argb(
                            150,
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

    private void drawCombo(Canvas canvas) {

        if (combo <= 1) {
            return;
        }

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        hudPaint.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD
        );

        hudPaint.setTextSize(16f);

        hudPaint.setColor(
                Color.rgb(
                        225,
                        80,
                        130
                )
        );

        canvas.drawText(
                "COMBO x" + combo,
                getWidth() / 2f,
                getHeight() - 22f,
                hudPaint
        );
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (gameFinished
                || animating) {

            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                int[] startCell =
                        getCell(x, y);

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

        float dx = toX - fromX;
        float dy = toY - fromY;

        float distance =
                (float) Math.sqrt(
                        dx * dx + dy * dy
                );

        float step =
                Math.max(
                        2f,
                        cellSize * 0.06f
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
                    fromX + dx * fraction;

            float sampleY =
                    fromY + dy * fraction;

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

    private void tryAddCell(int[] cell) {

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

        if (a == null
                || b == null) {

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
                && (rowDifference != 0
                || colDifference != 0);
    }

    private boolean alreadySelected(
            int[] cell
    ) {

        for (int[] selectedCell : selected) {

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
                        ((x - boardLeft)
                                / cellSize);

        int row =
                (int)
                        ((y - boardTop)
                                / cellSize);

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

    private float getCellCenterX(int col) {

        return boardLeft
                + col * cellSize
                + cellSize / 2f;
    }

    private float getCellCenterY(int row) {

        return boardTop
                + row * cellSize
                + cellSize / 2f;
    }

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

        /*
         * SCORE IS UNLIMITED.
         *
         * There is no score ceiling.
         */
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

        /*
         * Progress counters are separate from score.
         */
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
    }

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
                    "Level " + level
                            + " complete!\n\n"
                            + "Score: " + score
                            + "\n"
                            + "Best combo: x"
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

        if (gameFinished
                || animating) {

            return;
        }

        gameFinished = true;

        timerHandler.removeCallbacks(
                timerRunnable
        );

        new AlertDialog.Builder(
                getContext()
        )
                .setTitle(
                        "⏰ TIME'S UP!"
                )
                .setMessage(
                        "Your score: "
                                + score
                                + "\n\n"
                                + "Objective:\n"
                                + getObjectiveText()
                )
                .setCancelable(false)
                .setPositiveButton(
                        "RETRY",
                        (dialog, which) ->
                                restartLevel()
                )
                .setNegativeButton(
                        "HOME",
                        (dialog, which) ->
                                goHome()
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