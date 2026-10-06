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

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 6;
    private static final int MAX_LEVEL = 100;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint effectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

    /*
     * These remember the previous finger position.
     * They help us detect fast swipes and turns.
     */
    private float lastTouchX;
    private float lastTouchY;

    private int level = 1;
    private int score = 0;

    private int target;
    private int collected = 0;

    private int timeLeft;
    private int levelTime;

    private int minimumMatch;
    private int requiredLongMatches;
    private int longMatches;

    private String objectiveText = "";

    private int comboCount = 0;
    private int comboDisplay = 0;
    private long comboUntil = 0;

    private float popProgress = 0f;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            if (gameFinished) {
                return;
            }

            if (timeLeft > 0) {

                timeLeft--;

                invalidate();

                timerHandler.postDelayed(
                        this,
                        1000
                );

            } else {

                gameFinished = true;

                invalidate();

                showTimeUpDialog();
            }
        }
    };

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {

            if (!animating) {
                return;
            }

            popProgress += 0.14f;

            if (popProgress >= 1f) {

                popProgress = 1f;

                for (int[] cell : poppingCells) {
                    board[cell[0]][cell[1]] = -1;
                }

                animating = false;

                refillBoard();

                poppingCells.clear();

                invalidate();

                if (collected >= target
                        && longMatches >= requiredLongMatches) {

                    levelComplete();
                }

                return;
            }

            invalidate();

            animationHandler.postDelayed(
                    this,
                    25
            );
        }
    };

    private final int[] candyColors = {
            Color.rgb(255, 70, 85),
            Color.rgb(255, 185, 25),
            Color.rgb(60, 190, 95),
            Color.rgb(55, 145, 240),
            Color.rgb(175, 70, 205),
            Color.rgb(255, 105, 55)
    };

    public GameView(Context context) {
        super(context);

        gameSound = new GameSound(context);

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(
                Color.argb(55, 0, 0, 0)
        );

        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(20f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(
                Color.WHITE
        );

        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);

        hudPaint.setAntiAlias(true);
        hudPaint.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD
        );

        effectPaint.setAntiAlias(true);
        effectPaint.setTextAlign(
                Paint.Align.CENTER
        );
        effectPaint.setTypeface(
                android.graphics.Typeface.DEFAULT_BOLD
        );

        createBoard();
        setupLevel();

        setFocusable(true);

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );
    }

    private void setupLevel() {

        level = Math.max(
                1,
                Math.min(
                        MAX_LEVEL,
                        level
                )
        );

        collected = 0;
        longMatches = 0;
        comboCount = 0;
        comboDisplay = 0;

        if (level <= 10) {

            target = 18 + level * 2;
            levelTime = 100;
            minimumMatch = 3;
            requiredLongMatches = 0;
            objectiveText = "Easy start";

        } else if (level <= 20) {

            target = 35 + (level - 10) * 2;
            levelTime = 95;
            minimumMatch = 3;
            requiredLongMatches = 0;
            objectiveText = "Pop more candies";

        } else if (level <= 40) {

            target = 55 + (level - 20) * 3;
            levelTime = 90;
            minimumMatch = 3;
            requiredLongMatches = 2;
            objectiveText = "Make 4+ candy matches";

        } else if (level <= 60) {

            target = 80 + (level - 40) * 3;
            levelTime = 80;
            minimumMatch = 3;
            requiredLongMatches = 4;
            objectiveText = "Build long matches";

        } else if (level <= 80) {

            target = 120 + (level - 60) * 3;
            levelTime = 75;
            minimumMatch = 4;
            requiredLongMatches = 5;
            objectiveText = "Hard mode";

        } else if (level <= 99) {

            target = 180 + (level - 80) * 4;
            levelTime = 70;
            minimumMatch = 4;
            requiredLongMatches = 7;
            objectiveText = "Expert challenge";

        } else {

            target = 300;
            levelTime = 120;
            minimumMatch = 4;
            requiredLongMatches = 10;
            objectiveText = "CANDYJOLT FINAL CHALLENGE";
        }

        timeLeft = levelTime;
    }

    private void createBoard() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                board[row][col] =
                        (int) (
                                Math.random()
                                        * CANDY_TYPES
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
                        238,
                        249
                )
        );

        drawHud(canvas);

        float topSpace =
                Math.min(
                        150f,
                        getHeight() * 0.18f
                );

        float availableHeight =
                getHeight() - topSpace;

        cellSize = Math.min(
                getWidth() / (float) COLS,
                availableHeight / (float) ROWS
        );

        float boardWidth =
                cellSize * COLS;

        float boardHeight =
                cellSize * ROWS;

        boardLeft =
                (getWidth() - boardWidth)
                        / 2f;

        boardTop =
                topSpace
                        + (
                        availableHeight
                                - boardHeight
                ) / 2f;

        drawBoardBackground(canvas);
        drawBoard(canvas);

        /*
         * Make the connection line large and obvious.
         * It follows the exact candy-to-candy path.
         */
        linePaint.setStrokeWidth(
                Math.max(
                        14f,
                        cellSize * 0.18f
                )
        );

        if (drawing
                && selected.size() >= 2) {

            canvas.drawPath(
                    connectionPath,
                    linePaint
            );
        }

        drawSelection(canvas);
        drawCombo(canvas);
    }

    private void drawHud(Canvas canvas) {

        hudPaint.setTextAlign(
                Paint.Align.CENTER
        );

        float width = getWidth();

        float boxWidth =
                width / 4f;

        float boxHeight = 85f;

        drawHudBox(
                canvas,
                boxWidth * 0.5f,
                50,
                boxWidth,
                boxHeight,
                "LEVEL",
                String.valueOf(level),
                Color.rgb(
                        124,
                        77,
                        255
                )
        );

        drawHudBox(
                canvas,
                boxWidth * 1.5f,
                50,
                boxWidth,
                boxHeight,
                "SCORE",
                String.valueOf(score),
                Color.rgb(
                        255,
                        105,
                        180
                )
        );

        drawHudBox(
                canvas,
                boxWidth * 2.5f,
                50,
                boxWidth,
                boxHeight,
                "TARGET",
                String.valueOf(target),
                Color.rgb(
                        66,
                        165,
                        245
                )
        );

        int timerColor =
                timeLeft <= 10
                        ? Color.rgb(
                        230,
                        40,
                        50
                )
                        : Color.rgb(
                        60,
                        190,
                        95
                );

        drawHudBox(
                canvas,
                boxWidth * 3.5f,
                50,
                boxWidth,
                boxHeight,
                "TIME",
                String.valueOf(timeLeft),
                timerColor
        );
    }

    private void drawHudBox(
            Canvas canvas,
            float centerX,
            float centerY,
            float width,
            float height,
            String label,
            String value,
            int color
    ) {

        paint.setColor(Color.WHITE);

        RectF box = new RectF(
                centerX - width / 2f,
                centerY - height / 2f,
                centerX + width / 2f,
                centerY + height / 2f
        );

        canvas.drawRoundRect(
                box,
                20,
                20,
                paint
        );

        paint.setColor(color);

        canvas.drawRoundRect(
                new RectF(
                        box.left,
                        box.top,
                        box.right,
                        box.top + 9
                ),
                20,
                20,
                paint
        );

        hudPaint.setColor(
                Color.rgb(
                        90,
                        70,
                        100
                )
        );

        hudPaint.setTextSize(13);

        canvas.drawText(
                label,
                centerX,
                centerY - 9,
                hudPaint
        );

        hudPaint.setColor(color);

        hudPaint.setTextSize(25);

        canvas.drawText(
                value,
                centerX,
                centerY + 20,
                hudPaint
        );
    }

    private void drawBoardBackground(
            Canvas canvas
    ) {

        paint.setColor(
                Color.argb(
                        40,
                        124,
                        77,
                        255
                )
        );

        RectF boardRect = new RectF(
                boardLeft - 10,
                boardTop - 10,
                boardLeft
                        + cellSize * COLS
                        + 10,
                boardTop
                        + cellSize * ROWS
                        + 10
        );

        canvas.drawRoundRect(
                boardRect,
                28,
                28,
                paint
        );
    }

    private void drawBoard(Canvas canvas) {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                int type = board[row][col];

                if (type < 0) {
                    continue;
                }

                float centerX =
                        getCellCenterX(col);

                float centerY =
                        getCellCenterY(row);

                float radius =
                        cellSize * 0.32f;

                float scale = 1f;

                if (isPopping(row, col)) {

                    scale =
                            1f
                                    - 0.75f
                                    * popProgress;

                    if (scale < 0.08f) {
                        scale = 0.08f;
                    }
                }

                drawCandy(
                        canvas,
                        centerX,
                        centerY,
                        radius * scale,
                        type
                );
            }
        }
    }

    private void drawCandy(
            Canvas canvas,
            float x,
            float y,
            float radius,
            int type
    ) {

        int color =
                candyColors[type];

        float bodyWidth =
                radius * 1.55f;

        float bodyHeight =
                radius * 1.65f;

        canvas.drawOval(
                new RectF(
                        x - bodyWidth / 2f + 3,
                        y - bodyHeight / 2f + 7,
                        x + bodyWidth / 2f + 3,
                        y + bodyHeight / 2f + 7
                ),
                shadowPaint
        );

        paint.setColor(
                darken(
                        color,
                        0.78f
                )
        );

        Path leftWrapper =
                new Path();

        leftWrapper.moveTo(
                x - bodyWidth / 2f,
                y - radius * 0.55f
        );

        leftWrapper.lineTo(
                x - radius * 1.15f,
                y - radius * 0.30f
        );

        leftWrapper.lineTo(
                x - radius * 1.15f,
                y + radius * 0.30f
        );

        leftWrapper.lineTo(
                x - bodyWidth / 2f,
                y + radius * 0.55f
        );

        leftWrapper.close();

        canvas.drawPath(
                leftWrapper,
                paint
        );

        Path rightWrapper =
                new Path();

        rightWrapper.moveTo(
                x + bodyWidth / 2f,
                y - radius * 0.55f
        );

        rightWrapper.lineTo(
                x + radius * 1.15f,
                y - radius * 0.30f
        );

        rightWrapper.lineTo(
                x + radius * 1.15f,
                y + radius * 0.30f
        );

        rightWrapper.lineTo(
                x + bodyWidth / 2f,
                y + radius * 0.55f
        );

        rightWrapper.close();

        canvas.drawPath(
                rightWrapper,
                paint
        );

        paint.setColor(color);

        RectF body = new RectF(
                x - bodyWidth / 2f,
                y - bodyHeight / 2f,
                x + bodyWidth / 2f,
                y + bodyHeight / 2f
        );

        canvas.drawRoundRect(
                body,
                radius * 0.45f,
                radius * 0.45f,
                paint
        );

        outlinePaint.setColor(
                darken(
                        color,
                        0.65f
                )
        );

        canvas.drawRoundRect(
                body,
                radius * 0.45f,
                radius * 0.45f,
                outlinePaint
        );

        paint.setColor(
                Color.argb(
                        75,
                        255,
                        255,
                        255
                )
        );

        for (int i = -1; i <= 1; i++) {

            canvas.drawRoundRect(
                    new RectF(
                            x
                                    + i
                                    * radius
                                    * 0.42f
                                    - 3,
                            y - bodyHeight
                                    * 0.34f,
                            x
                                    + i
                                    * radius
                                    * 0.42f
                                    + 3,
                            y + bodyHeight
                                    * 0.34f
                    ),
                    4,
                    4,
                    paint
            );
        }

        paint.setColor(
                Color.argb(
                        185,
                        255,
                        255,
                        255
                )
        );

        canvas.drawOval(
                new RectF(
                        x - bodyWidth
                                * 0.28f,
                        y - bodyHeight
                                * 0.31f,
                        x + bodyWidth
                                * 0.02f,
                        y - bodyHeight
                                * 0.05f
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

    private void drawSelection(
            Canvas canvas
    ) {

        if (!drawing) {
            return;
        }

        paint.setStyle(
                Paint.Style.STROKE
        );

        paint.setStrokeWidth(
                Math.max(
                        4f,
                        cellSize * 0.045f
                )
        );

        paint.setColor(
                Color.argb(
                        210,
                        255,
                        255,
                        255
                )
        );

        for (int[] cell : selected) {

            canvas.drawCircle(
                    getCellCenterX(cell[1]),
                    getCellCenterY(cell[0]),
                    cellSize * 0.39f,
                    paint
            );
        }

        paint.setStyle(
                Paint.Style.FILL
        );
    }

    private void drawCombo(
            Canvas canvas
    ) {

        if (comboDisplay <= 0
                || System.currentTimeMillis()
                > comboUntil) {

            return;
        }

        effectPaint.setTextSize(
                Math.max(
                        30f,
                        cellSize * 0.48f
                )
        );

        effectPaint.setColor(
                Color.rgb(
                        124,
                        77,
                        255
                )
        );

        float alpha =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                (comboUntil
                                        - System.currentTimeMillis())
                                        / 900f
                        )
                );

        effectPaint.setAlpha(
                (int) (
                        alpha * 255
                )
        );

        String text =
                comboDisplay == 2
                        ? "COMBO x2!"
                        : comboDisplay == 3
                        ? "COMBO x3!"
                        : comboDisplay >= 4
                        ? "COMBO x"
                        + comboDisplay
                        + "!"
                        : "NICE!";

        canvas.drawText(
                text,
                getWidth() / 2f,
                boardTop - 20,
                effectPaint
        );

        effectPaint.setAlpha(255);

        postInvalidateDelayed(40);
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

                selected.clear();

                connectionPath.reset();

                lastTouchX = x;
                lastTouchY = y;

                int[] first =
                        getCell(x, y);

                if (first != null
                        && board[first[0]][first[1]] >= 0) {

                    selected.add(first);

                    connectionPath.moveTo(
                            getCellCenterX(
                                    first[1]
                            ),
                            getCellCenterY(
                                    first[0]
                            )
                    );

                    drawing = true;

                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawing
                        || selected.isEmpty()) {

                    return true;
                }

                /*
                 * Process every cell crossed by the finger.
                 *
                 * This is the important part that makes
                 * fast swipes and changing directions work.
                 */
                processTouchMovement(
                        x,
                        y
                );

                lastTouchX = x;
                lastTouchY = y;

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                if (drawing) {

                    if (selected.size()
                            >= minimumMatch) {

                        removeSelected();

                    } else {

                        comboCount = 0;
                        comboDisplay = 0;
                    }

                    selected.clear();

                    connectionPath.reset();

                    drawing = false;

                    invalidate();
                }

                return true;
        }

        return true;
    }

    /*
     * Converts finger movement into a sequence of
     * horizontal/vertical candy-to-candy steps.
     *
     * This allows:
     *
     *  → → →
     *  ↓
     *  ← ←
     *
     * and many other shapes.
     */
    private void processTouchMovement(
            float x,
            float y
    ) {

        int[] current =
                getCell(x, y);

        if (current == null) {
            return;
        }

        int[] last =
                selected.get(
                        selected.size() - 1
                );

        if (current[0] == last[0]
                && current[1] == last[1]) {

            return;
        }

        /*
         * If the finger entered a neighboring cell,
         * add it immediately.
         */
        if (isAdjacent(last, current)) {

            tryAddCell(current);

            return;
        }

        /*
         * The touch event may have skipped one or more
         * cells because the finger moved quickly.
         *
         * We walk through the grid one cell at a time.
         */
        int rowDifference =
                current[0] - last[0];

        int colDifference =
                current[1] - last[1];

        int rowSteps =
                Math.abs(rowDifference);

        int colSteps =
                Math.abs(colDifference);

        int rowDirection =
                Integer.signum(rowDifference);

        int colDirection =
                Integer.signum(colDifference);

        /*
         * When both row and column changed, choose the
         * direction that best matches the actual finger
         * movement.
         */
        float pixelDX =
                x - lastTouchX;

        float pixelDY =
                y - lastTouchY;

        if (rowSteps > 0
                && colSteps > 0) {

            if (Math.abs(pixelDX)
                    >= Math.abs(pixelDY)) {

                /*
                 * Move horizontally first.
                 */
                for (int i = 0;
                        i < colSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0],
                                    last[1]
                                            + colDirection
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }

                /*
                 * Then move vertically.
                 */
                for (int i = 0;
                        i < rowSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0]
                                            + rowDirection,
                                    last[1]
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }

            } else {

                /*
                 * Move vertically first.
                 */
                for (int i = 0;
                        i < rowSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0]
                                            + rowDirection,
                                    last[1]
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }

                /*
                 * Then move horizontally.
                 */
                for (int i = 0;
                        i < colSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0],
                                    last[1]
                                            + colDirection
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }
            }

        } else {

            /*
             * Only one direction changed.
             */
            if (rowSteps > 0) {

                for (int i = 0;
                        i < rowSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0]
                                            + rowDirection,
                                    last[1]
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }

            } else {

                for (int i = 0;
                        i < colSteps;
                        i++) {

                    int[] next =
                            new int[]{
                                    last[0],
                                    last[1]
                                            + colDirection
                            };

                    if (!tryAddCell(next)) {
                        return;
                    }

                    last =
                            selected.get(
                                    selected.size() - 1
                            );
                }
            }
        }
    }

    /*
     * Adds one candy to the path only when:
     *
     * 1. It is inside the board.
     * 2. It is directly adjacent.
     * 3. It is the same candy type.
     * 4. It has not already been selected.
     */
    private boolean tryAddCell(
            int[] cell
    ) {

        if (cell == null) {
            return false;
        }

        int row = cell[0];
        int col = cell[1];

        if (row < 0
                || row >= ROWS
                || col < 0
                || col >= COLS) {

            return false;
        }

        int[] last =
                selected.get(
                        selected.size() - 1
                );

        if (!isAdjacent(last, cell)) {
            return false;
        }

        if (board[row][col] < 0) {
            return false;
        }

        /*
         * Same candy type only.
         */
        if (board[row][col]
                != board[last[0]][last[1]]) {

            return false;
        }

        /*
         * Do not allow the path to jump through
         * an already selected candy.
         */
        if (alreadySelected(cell)) {
            return false;
        }

        selected.add(
                new int[]{
                        row,
                        col
                }
        );

        connectionPath.lineTo(
                getCellCenterX(col),
                getCellCenterY(row)
        );

        return true;
    }

    private int[] getCell(
            float x,
            float y
    ) {

        if (cellSize <= 0) {
            return null;
        }

        int col =
                (int) (
                        (x - boardLeft)
                                / cellSize
                );

        int row =
                (int) (
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

    private boolean isAdjacent(
            int[] a,
            int[] b
    ) {

        int rowDistance =
                Math.abs(
                        a[0] - b[0]
                );

        int colDistance =
                Math.abs(
                        a[1] - b[1]
                );

        return rowDistance
                + colDistance == 1;
    }

    private boolean alreadySelected(
            int[] cell
    ) {

        for (int[] selectedCell
                : selected) {

            if (selectedCell[0]
                    == cell[0]
                    && selectedCell[1]
                    == cell[1]) {

                return true;
            }
        }

        return false;
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

    private void removeSelected() {

        if (animating) {
            return;
        }

        int matched =
                selected.size();

        if (matched >= 4) {
            longMatches++;
        }

        comboCount++;

        comboDisplay =
                Math.max(
                        1,
                        comboCount
                );

        comboUntil =
                System.currentTimeMillis()
                        + 1200;

        if (comboCount >= 2) {
            gameSound.playCombo();
        } else {
            gameSound.playPop();
        }

        int bonus = 0;

        if (matched >= 5) {
            bonus = 50;
        } else if (matched == 4) {
            bonus = 25;
        }

        int comboBonus =
                Math.max(
                        0,
                        comboCount - 1
                ) * 15;

        score +=
                matched * 10
                        + bonus
                        + comboBonus;

        collected += matched;

        poppingCells.clear();

        for (int[] cell : selected) {

            poppingCells.add(
                    new int[]{
                            cell[0],
                            cell[1]
                    }
            );
        }

        popProgress = 0f;
        animating = true;

        animationHandler.removeCallbacks(
                animationRunnable
        );

        animationHandler.post(
                animationRunnable
        );
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

                if (board[row][col]
                        != -1) {

                    board[writeRow][col] =
                            board[row][col];

                    writeRow--;
                }
            }

            while (writeRow >= 0) {

                board[writeRow][col] =
                        (int) (
                                Math.random()
                                        * CANDY_TYPES
                        );

                writeRow--;
            }
        }

        invalidate();
    }

    private void levelComplete() {

        if (gameFinished) {
            return;
        }

        gameFinished = true;

        timerHandler.removeCallbacks(
                timerRunnable
        );

        gameSound.playComplete();

        int stars;

        if (timeLeft >= levelTime * 0.60f) {
            stars = 3;
        } else if (timeLeft >= levelTime * 0.25f) {
            stars = 2;
        } else {
            stars = 1;
        }

        score += stars * 100;

        invalidate();

        String starText;

        if (stars == 3) {
            starText = "⭐⭐⭐";
        } else if (stars == 2) {
            starText = "⭐⭐";
        } else {
            starText = "⭐";
        }

        String message =
                starText
                        + "\n\n"
                        + "Level "
                        + level
                        + " completed!\n\n"
                        + "Objective: "
                        + objectiveText
                        + "\n\n"
                        + "Score: "
                        + score;

        new AlertDialog.Builder(
                getContext()
        )
                .setTitle(
                        "🎉 LEVEL COMPLETE!"
                )
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        level >= MAX_LEVEL
                                ? "FINISH"
                                : "NEXT LEVEL",
                        (dialog, which) -> {

                            if (level >= MAX_LEVEL) {

                                goHome();

                            } else {

                                level++;

                                setupLevel();

                                createBoard();

                                gameFinished = false;

                                timerHandler.postDelayed(
                                        timerRunnable,
                                        1000
                                );

                                invalidate();
                            }
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

        int remaining =
                Math.max(
                        0,
                        target - collected
                );

        String extraText = "";

        if (requiredLongMatches > 0
                && longMatches
                < requiredLongMatches) {

            extraText =
                    "\n\nLong matches: "
                            + longMatches
                            + " / "
                            + requiredLongMatches;
        }

        new AlertDialog.Builder(
                getContext()
        )
                .setTitle(
                        "⏰ TIME'S UP!"
                )
                .setMessage(
                        "You needed "
                                + remaining
                                + " more candies.\n\n"
                                + "Score: "
                                + score
                                + extraText
                )
                .setCancelable(false)
                .setPositiveButton(
                        "TRY AGAIN",
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

        timerHandler.removeCallbacks(
                timerRunnable
        );

        animationHandler.removeCallbacks(
                animationRunnable
        );

        score = 0;
        collected = 0;
        longMatches = 0;
        comboCount = 0;
        comboDisplay = 0;

        animating = false;

        poppingCells.clear();

        gameFinished = false;

        createBoard();

        setupLevel();

        timerHandler.postDelayed(
                timerRunnable,
                1000
        );

        invalidate();
    }

    private void goHome() {

        timerHandler.removeCallbacks(
                timerRunnable
        );

        animationHandler.removeCallbacks(
                animationRunnable
        );

        Context context = getContext();

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        context.startActivity(intent);

        if (context instanceof Activity) {

            ((Activity) context).finish();
        }
    }

    private int darken(
            int color,
            float factor
    ) {

        int r =
                (int) (
                        Color.red(color)
                                * factor
                );

        int g =
                (int) (
                        Color.green(color)
                                * factor
                );

        int b =
                (int) (
                        Color.blue(color)
                                * factor
                );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    @Override
    protected void onDetachedFromWindow() {

        timerHandler.removeCallbacks(
                timerRunnable
        );

        animationHandler.removeCallbacks(
                animationRunnable
        );

        gameSound.release();

        super.onDetachedFromWindow();
    }
}