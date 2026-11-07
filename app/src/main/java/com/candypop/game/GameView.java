
package com.candypop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import java.util.Locale;
import java.util.Random;

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 6;
    private static final int MAX_LEVEL = 100;

    private static final int OBJECTIVE_CANDIES = 0;
    private static final int OBJECTIVE_CONNECTIONS = 1;
    private static final int OBJECTIVE_LONG = 2;
    private static final int OBJECTIVE_MIXED = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    private boolean drawing;
    private boolean animating;
    private boolean gameFinished;

    private int connectionCandyType = -1;

    private int level = 1;
    private int score = 0;

    private int timeLimit = 100;
    private int secondsLeft = 100;
    private int minimumConnection = 3;

    private int objectiveType = OBJECTIVE_CANDIES;
    private int objectiveTarget = 12;
    private int longConnectionGoal = 0;

    private int candiesPoppedThisLevel = 0;
    private int connectionsThisLevel = 0;
    private int longConnections = 0;

    private int combo = 0;
    private int bestCombo = 0;

    private long popStartTime;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (gameFinished) {
                return;
            }

            if (secondsLeft > 0) {
                secondsLeft--;
                invalidate();
                timerHandler.postDelayed(this, 1000);
            } else if (animating) {
                timerHandler.postDelayed(this, 100);
            } else {
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

            long elapsed =
                    System.currentTimeMillis() - popStartTime;

            if (elapsed < 260) {
                invalidate();
                animationHandler.postDelayed(this, 16);
                return;
            }

            for (int[] cell : poppingCells) {
                board[cell[0]][cell[1]] = -1;
            }

            refillBoard();

            poppingCells.clear();
            animating = false;

            invalidate();

            if (secondsLeft <= 0) {
                showTimeUpDialog();
            }
        }
    };

    public GameView(Context context) {
        super(context);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        gameSound = new GameSound(context);

        setupLevel();
    }

    public void setLevel(int selectedLevel) {
        level = Math.max(1, Math.min(MAX_LEVEL, selectedLevel));
        setupLevel();
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void setupLevel() {
        score = 0;
        combo = 0;
        bestCombo = 0;

        candiesPoppedThisLevel = 0;
        connectionsThisLevel = 0;
        longConnections = 0;

        longConnectionGoal = 0;

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
            objectiveTarget = 10 + stage * 2;

        } else {
            timeLimit = 60;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 70 + stage * 10;
            longConnectionGoal = 3 + stage / 2;
        }

        if (level == 10 || level == 25 || level == 50
                || level == 75 || level == 100) {
            timeLimit = 90;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 150 + level * 5;
            longConnectionGoal = 3 + level / 25;
        }

        secondsLeft = timeLimit;
        gameFinished = false;
        drawing = false;
        animating = false;

        selected.clear();
        poppingCells.clear();
        connectionCandyType = -1;

        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        createBoard();

        timerHandler.postDelayed(timerRunnable, 1000);

        invalidate();
    }

    private void createBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = random.nextInt(CANDY_TYPES);
            }
        }

        ensurePlayableBoard();
    }

    private void ensurePlayableBoard() {
        if (hasValidConnection()) {
            return;
        }

        int type = random.nextInt(CANDY_TYPES);
        int length = Math.max(3, minimumConnection);

        if (objectiveType == OBJECTIVE_LONG
                || objectiveType == OBJECTIVE_MIXED) {
            length = Math.max(length, 5);
        }

        int row = random.nextInt(ROWS);
        int startCol = random.nextInt(COLS - length + 1);

        for (int i = 0; i < length; i++) {
            board[row][startCol + i] = type;
        }
    }

    private boolean hasValidConnection() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int type = board[row][col];

                if (type < 0) {
                    continue;
                }

                int horizontal = 1;
                for (int c = col + 1; c < COLS; c++) {
                    if (board[row][c] == type) {
                        horizontal++;
                    } else {
                        break;
                    }
                }

                int vertical = 1;
                for (int r = row + 1; r < ROWS; r++) {
                    if (board[r][col] == type) {
                        vertical++;
                    } else {
                        break;
                    }
                }

                if (horizontal >= minimumConnection
                        || vertical >= minimumConnection) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isAdjacent(
            int row1, int col1,
            int row2, int col2) {

        int dr = Math.abs(row1 - row2);
        int dc = Math.abs(col1 - col2);

        return dr <= 1 && dc <= 1 && (dr + dc > 0);
    }

    private boolean isCellSelected(int row, int col) {
        for (int[] cell : selected) {
            if (cell[0] == row && cell[1] == col) {
                return true;
            }
        }
        return false;
    }

    private void clearSelection() {
        selected.clear();
        connectionCandyType = -1;
        drawing = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        canvas.drawColor(Color.rgb(12, 17, 29));

        calculateBoardDimensions();

        drawHeader(canvas);
        drawLevelInfo(canvas);
        drawObjective(canvas);
        drawTimer(canvas);
        drawBoard(canvas);
        drawConnection(canvas);
        drawBottomInfo(canvas);
        drawEffects(canvas);
    }

    private void calculateBoardDimensions() {
        float width = getWidth();
        float height = getHeight();

        float horizontalPadding = dp(14);
        float availableWidth = width - horizontalPadding * 2;

        float availableHeight = height - dp(285);

        cellSize = Math.min(
                availableWidth / COLS,
                availableHeight / ROWS
        );

        cellSize = Math.max(dp(24), cellSize);

        boardLeft = (width - cellSize * COLS) / 2f;
        boardTop = dp(245);

        float maxBottom = height - dp(55);

        if (boardTop + cellSize * ROWS > maxBottom) {
            boardTop = Math.max(
                    dp(205),
                    maxBottom - cellSize * ROWS
            );
        }
    }

    private void drawHeader(Canvas canvas) {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(25));
        textPaint.setFakeBoldText(true);

        canvas.drawText(
                "CANDYJOLT",
                getWidth() / 2f,
                dp(38),
                textPaint
        );

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(Color.rgb(145, 165, 190));

        canvas.drawText(
                "CONNECT • POP • WIN",
                getWidth() / 2f,
                dp(57),
                textPaint
        );
    }

    private void drawLevelInfo(Canvas canvas) {
        float left = dp(14);
        float right = getWidth() - dp(14);
        float top = dp(72);

        drawPanel(
                canvas,
                left, top, right, top + dp(55),
                Color.rgb(24, 36, 55),
                dp(14)
        );

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(dp(10));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.rgb(145, 165, 190));

        canvas.drawText("LEVEL", left + dp(14),
                top + dp(19), textPaint);

        textPaint.setTextSize(dp(22));
        textPaint.setColor(Color.WHITE);

        canvas.drawText(String.valueOf(level),
                left + dp(14), top + dp(43), textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(Color.rgb(145, 165, 190));

        canvas.drawText("SCORE", right - dp(14),
                top + dp(19), textPaint);

        textPaint.setTextSize(dp(22));
        textPaint.setColor(Color.rgb(255, 210, 95));

        canvas.drawText(String.valueOf(score),
                right - dp(14), top + dp(43), textPaint);

        textPaint.setFakeBoldText(false);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawObjective(Canvas canvas) {
        float left = dp(14);
        float right = getWidth() - dp(14);
        float top = dp(138);

        drawPanel(
                canvas,
                left, top, right, top + dp(45),
                Color.rgb(24, 36, 55),
                dp(12)
        );

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(dp(10));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(Color.rgb(145, 165, 190));

        canvas.drawText("LEVEL OBJECTIVE",
                left + dp(12), top + dp(16), textPaint);

        textPaint.setTextSize(dp(13));
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(false);

        canvas.drawText(getObjectiveText(),
                left + dp(12), top + dp(35), textPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private String getObjectiveText() {
        switch (objectiveType) {
            case OBJECTIVE_CANDIES:
                return "Pop " + objectiveTarget + " candies ("
                        + candiesPoppedThisLevel + "/"
                        + objectiveTarget + ")";

            case OBJECTIVE_CONNECTIONS:
                return "Make " + objectiveTarget + " connections ("
                        + connectionsThisLevel + "/"
                        + objectiveTarget + ")";

            case OBJECTIVE_LONG:
                return "Make " + longConnectionGoal
                        + " long connections ("
                        + longConnections + "/"
                        + longConnectionGoal + ")";

            case OBJECTIVE_MIXED:
                return "Score " + objectiveTarget
                        + " and make " + longConnectionGoal
                        + " long connections";

            default:
                return "Connect matching candies";
        }
    }

    private void drawTimer(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(23));
        textPaint.setFakeBoldText(true);

        textPaint.setColor(secondsLeft <= 10
                ? Color.rgb(255, 75, 75)
                : Color.rgb(80, 225, 145));

        String value = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                secondsLeft / 60,
                secondsLeft % 60
        );

        canvas.drawText(
                value,
                getWidth() / 2f,
                dp(215),
                textPaint
        );

        textPaint.setFakeBoldText(false);
    }

    private void drawPanel(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            int color,
            float radius) {

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        canvas.drawRoundRect(
                left, top, right, bottom,
                radius, radius, paint
        );
    }

    private void drawBoard(Canvas canvas) {
        float right = boardLeft + cellSize * COLS;
        float bottom = boardTop + cellSize * ROWS;

        drawPanel(
                canvas,
                boardLeft - dp(5),
                boardTop - dp(5),
                right + dp(5),
                bottom + dp(5),
                Color.rgb(25, 36, 54),
                dp(17)
        );

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float left = boardLeft + col * cellSize;
                float top = boardTop + row * cellSize;
                float inset = cellSize * 0.07f;

                RectF rect = new RectF(
                        left + inset,
                        top + inset,
                        left + cellSize - inset,
                        top + cellSize - inset
                );

                drawPanel(
                        canvas,
                        rect.left, rect.top,
                        rect.right, rect.bottom,
                        Color.rgb(35, 49, 69),
                        dp(9)
                );

                int type = board[row][col];

                if (type >= 0) {
                    drawCandy(canvas, row, col, type);
                }

                if (isCellSelected(row, col)) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(3));
                    paint.setColor(Color.WHITE);

                    canvas.drawRoundRect(
                            rect,
                            dp(9), dp(9), paint
                    );

                    paint.setStyle(Paint.Style.FILL);
                }
            }
        }
    }

    private void drawCandy(
            Canvas canvas,
            int row,
            int col,
            int type) {

        float x = boardLeft + col * cellSize + cellSize / 2f;
        float y = boardTop + row * cellSize + cellSize / 2f;
        float radius = cellSize * 0.34f;

        int[] colors = getCandyColors(type);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colors[0]);

        switch (type) {
            case 0:
                // Red apple
                canvas.drawCircle(x, y + radius * 0.05f,
                        radius, paint);
                paint.setColor(Color.rgb(110, 65, 35));
                paint.setStrokeWidth(dp(3));
                canvas.drawLine(x, y - radius * 0.7f,
                        x + dp(2), y - radius, paint);
                drawLeaf(canvas, x + radius * 0.3f,
                        y - radius * 0.75f);
                break;

            case 1:
                // Orange
                canvas.drawCircle(x, y, radius, paint);
                paint.setColor(Color.rgb(255, 205, 95));
                canvas.drawCircle(x - radius * 0.35f,
                        y - radius * 0.35f,
                        radius * 0.12f, paint);
                paint.setColor(Color.rgb(80, 175, 85));
                canvas.drawCircle(x, y - radius * 0.9f,
                        radius * 0.16f, paint);
                break;

            case 2:
                // Grapes
                float grape = radius * 0.42f;
                canvas.drawCircle(x - grape, y - grape * 0.4f,
                        grape, paint);
                canvas.drawCircle(x + grape, y - grape * 0.4f,
                        grape, paint);
                canvas.drawCircle(x, y + grape * 0.45f,
                        grape, paint);
                canvas.drawCircle(x - grape * 0.5f,
                        y + grape * 1.15f, grape, paint);
                canvas.drawCircle(x + grape * 0.5f,
                        y + grape * 1.15f, grape, paint);
                break;

            case 3:
                // Blueberry
                canvas.drawCircle(x, y, radius, paint);
                paint.setColor(Color.rgb(190, 220, 255));
                canvas.drawCircle(x - radius * 0.35f,
                        y - radius * 0.35f,
                        radius * 0.18f, paint);
                paint.setColor(Color.rgb(40, 75, 145));
                canvas.drawCircle(x, y, radius * 0.18f, paint);
                break;

            case 4:
                // Chocolate square
                RectF chocolate = new RectF(
                        x - radius, y - radius,
                        x + radius, y + radius
                );

                canvas.drawRoundRect(
                        chocolate, dp(7), dp(7), paint
                );

                paint.setColor(Color.rgb(190, 125, 80));
                paint.setStrokeWidth(dp(2));

                canvas.drawLine(
                        x, y - radius * 0.75f,
                        x, y + radius * 0.75f, paint
                );

                canvas.drawLine(
                        x - radius * 0.75f, y,
                        x + radius * 0.75f, y, paint
                );
                break;

            case 5:
                // Pink strawberry
                Path berry = new Path();
                berry.moveTo(x, y + radius);
                berry.cubicTo(
                        x - radius * 1.25f,
                        y + radius * 0.1f,
                        x - radius * 0.8f,
                        y - radius,
                        x, y - radius * 0.55f
                );
                berry.cubicTo(
                        x + radius * 0.8f,
                        y - radius,
                        x + radius * 1.25f,
                        y + radius * 0.1f,
                        x, y + radius
                );

                paint.setColor(colors[0]);
                canvas.drawPath(berry, paint);

                paint.setColor(Color.WHITE);
                for (int i = 0; i < 5; i++) {
                    float sx = x + (i - 2) * radius * 0.28f;
                    float sy = y + ((i % 2) - 0.3f) * radius * 0.55f;
                    canvas.drawCircle(sx, sy, dp(1.5f), paint);
                }

                drawLeaf(canvas, x, y - radius * 0.75f);
                break;
        }

        // Glossy highlight
        paint.setColor(Color.argb(110, 255, 255, 255));
        canvas.drawCircle(
                x - radius * 0.35f,
                y - radius * 0.38f,
                radius * 0.16f,
                paint
        );
    }

    private int[] getCandyColors(int type) {
        switch (type) {
            case 0:
                return new int[]{
                        Color.rgb(220, 45, 60),
                        Color.rgb(255, 125, 135)
                };
            case 1:
                return new int[]{
                        Color.rgb(245, 135, 35),
                        Color.rgb(255, 195, 90)
                };
            case 2:
                return new int[]{
                        Color.rgb(125, 65, 190),
                        Color.rgb(190, 125, 235)
                };
            case 3:
                return new int[]{
                        Color.rgb(55, 125, 225),
                        Color.rgb(150, 215, 255)
                };
            case 4:
                return new int[]{
                        Color.rgb(105, 55, 30),
                        Color.rgb(175, 105, 65)
                };
            default:
                return new int[]{
                        Color.rgb(245, 75, 160),
                        Color.rgb(255, 165, 215)
                };
        }
    }

    private void drawLeaf(Canvas canvas, float x, float y) {
        paint.setColor(Color.rgb(70, 190, 95));

        RectF leaf = new RectF(
                x - dp(5), y - dp(3),
                x + dp(5), y + dp(3)
        );

        canvas.drawOval(leaf, paint);
    }

    private void drawConnection(Canvas canvas) {
        if (selected.size() < 2) {
            return;
        }

        connectionPath.reset();

        for (int i = 0; i < selected.size(); i++) {
            int[] cell = selected.get(i);

            float x = boardLeft
                    + cell[1] * cellSize
                    + cellSize / 2f;

            float y = boardTop
                    + cell[0] * cellSize
                    + cellSize / 2f;

            if (i == 0) {
                connectionPath.moveTo(x, y);
            } else {
                connectionPath.lineTo(x, y);
            }
        }

        linePaint.setColor(Color.argb(100, 255, 255, 255));
        linePaint.setStrokeWidth(dp(10));
        canvas.drawPath(connectionPath, linePaint);

        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(dp(3));
        canvas.drawPath(connectionPath, linePaint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);

        for (int[] cell : selected) {
            float x = boardLeft
                    + cell[1] * cellSize
                    + cellSize / 2f;

            float y = boardTop
                    + cell[0] * cellSize
                    + cellSize / 2f;

            canvas.drawCircle(x, y, dp(4), paint);
        }
    }

    private void drawBottomInfo(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(11));
        textPaint.setColor(Color.rgb(155, 175, 195));

        canvas.drawText(
                "Drag across matching fruits to connect them",
                getWidth() / 2f,
                getHeight() - dp(20),
                textPaint
        );
    }

    private void drawEffects(Canvas canvas) {
        if (!animating || poppingCells.isEmpty()) {
            return;
        }

        float progress = Math.min(
                1f,
                (System.currentTimeMillis() - popStartTime) / 260f
        );

        float radius = cellSize * (0.35f + progress * 0.3f);

        effectPaint.setStyle(Paint.Style.STROKE);
        effectPaint.setStrokeWidth(dp(3));
        effectPaint.setColor(
                Color.argb(
                        (int) (255 * (1f - progress)),
                        255,
                        230,
                        120
                )
        );

        for (int[] cell : poppingCells) {
            float x = boardLeft
                    + cell[1] * cellSize
                    + cellSize / 2f;

            float y = boardTop
                    + cell[0] * cellSize
                    + cellSize / 2f;

            canvas.drawCircle(x, y, radius, effectPaint);
        }

        effectPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameFinished || animating) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int[] start = getCellFromPosition(x, y);

                if (start == null) {
                    return true;
                }

                int type = board[start[0]][start[1]];

                if (type < 0) {
                    return true;
                }

                selected.clear();
                selected.add(start);

                connectionCandyType = type;
                drawing = true;

                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!drawing) {
                    return true;
                }

                int[] next = getCellFromPosition(x, y);

                if (next != null) {
                    addSelectedCell(next[0], next[1]);
                }

                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (drawing) {
                    drawing = false;

                    int[] last = getCellFromPosition(x, y);

                    if (last != null) {
                        addSelectedCell(last[0], last[1]);
                    }

                    if (selected.size() >= minimumConnection) {
                        removeSelected();
                    } else {
                        clearSelection();
                    }
                }

                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                clearSelection();
                return true;
        }

        return true;
    }

    private int[] getCellFromPosition(float x, float y) {
        if (cellSize <= 0) {
            return null;
        }

        int col = (int) ((x - boardLeft) / cellSize);
        int row = (int) ((y - boardTop) / cellSize);

        if (row < 0 || row >= ROWS
                || col < 0 || col >= COLS) {
            return null;
        }

        return new int[]{row, col};
    }

    private void addSelectedCell(int row, int col) {
        if (row < 0 || row >= ROWS
                || col < 0 || col >= COLS) {
            return;
        }

        if (board[row][col] != connectionCandyType) {
            return;
        }

        if (isCellSelected(row, col)) {
            return;
        }

        if (selected.isEmpty()) {
            return;
        }

        int[] previous = selected.get(selected.size() - 1);

        if (!isAdjacent(
                previous[0],
                previous[1],
                row,
                col)) {
            return;
        }

        selected.add(new int[]{row, col});

        gameSound.playSelect();
    }

    private void removeSelected() {
        if (selected.size() < minimumConnection) {
            clearSelection();
            return;
        }

        int chainLength = selected.size();

        poppingCells.clear();

        for (int[] cell : selected) {
            poppingCells.add(
                    new int[]{cell[0], cell[1]}
            );
        }

        // Scores are unlimited. Longer chains earn more points.
        int points = chainLength * chainLength * 10;

        if (chainLength >= 5) {
            points += 50;
        }

        if (chainLength >= 7) {
            points += 100;
        }

        score += points;
        saveHighScore();

        candiesPoppedThisLevel += chainLength;
        connectionsThisLevel++;

        if (chainLength >= 5) {
            longConnections++;
        }

        combo++;

        if (combo > bestCombo) {
            bestCombo = combo;
        }

        selected.clear();
        connectionCandyType = -1;

        animating = true;
        popStartTime = System.currentTimeMillis();

        gameSound.playPop();

        animationHandler.removeCallbacks(animationRunnable);
        animationHandler.post(animationRunnable);

        invalidate();
    }

    private void saveHighScore() {
        SharedPreferences preferences =
                getContext().getSharedPreferences(
                        "CandyPopScores",
                        Context.MODE_PRIVATE
                );

        int savedHighScore = preferences.getInt(
                "high_score",
                0
        );

        if (score > savedHighScore) {
            preferences.edit()
                    .putInt("high_score", score)
                    .apply();
        }
    }

    private boolean levelObjectiveComplete() {
        switch (objectiveType) {
            case OBJECTIVE_CANDIES:
                return candiesPoppedThisLevel >= objectiveTarget;

            case OBJECTIVE_CONNECTIONS:
                return connectionsThisLevel >= objectiveTarget;

            case OBJECTIVE_LONG:
                return longConnections >= longConnectionGoal;

            case OBJECTIVE_MIXED:
                return score >= objectiveTarget
                        && longConnections >= longConnectionGoal;

            default:
                return false;
        }
    }

    private void refillBoard() {
        // Drop remaining candies down each column.
        for (int col = 0; col < COLS; col++) {
            int writeRow = ROWS - 1;

            for (int row = ROWS - 1; row >= 0; row--) {
                if (board[row][col] >= 0) {
                    board[writeRow][col] = board[row][col];

                    if (writeRow != row) {
                        board[row][col] = -1;
                    }

                    writeRow--;
                }
            }

            // Fill empty spaces with new candies.
            while (writeRow >= 0) {
                board[writeRow][col] =
                        random.nextInt(CANDY_TYPES);

                writeRow--;
            }
        }

        ensurePlayableBoard();
    }

    private void showTimeUpDialog() {
        if (gameFinished || animating) {
            return;
        }

        gameFinished = true;

        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        if (levelObjectiveComplete()) {
            levelComplete();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("TIME'S UP!")
                .setMessage(
                        "You did not complete the objective.\n\n"
                                + getObjectiveText()
                                + "\n\nScore: " + score
                )
                .setCancelable(false)
                .setPositiveButton("RETRY", (dialog, which) -> {
                    restartLevel();
                })
                .setNegativeButton("HOME", (dialog, which) -> {
                    goHome();
                })
                .show();
    }

    private void levelComplete() {
        gameFinished = true;

        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        SharedPreferences preferences =
                getContext().getSharedPreferences(
                        "CandyJolt",
                        Context.MODE_PRIVATE
                );

        int highestUnlocked = preferences.getInt(
                "highest_level",
                1
        );

        if (level < MAX_LEVEL) {
            highestUnlocked = Math.max(
                    highestUnlocked,
                    level + 1
            );
        }

        preferences.edit()
                .putInt("highest_level", highestUnlocked)
                .apply();

        if (level >= MAX_LEVEL) {
            new AlertDialog.Builder(getContext())
                    .setTitle("YOU DID IT!")
                    .setMessage(
                            "Congratulations!\n\n"
                                    + "You completed all "
                                    + MAX_LEVEL
                                    + " levels!\n\n"
                                    + "Final score: " + score
                    )
                    .setCancelable(false)
                    .setPositiveButton("HOME", (dialog, which) -> {
                        goHome();
                    })
                    .show();

            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("LEVEL COMPLETE!")
                .setMessage(
                        "Excellent work!\n\n"
                                + "Level: " + level
                                + "\nScore: " + score
                                + "\nBest combo: " + bestCombo
                                + "\n\nNext level unlocked!"
                )
                .setCancelable(false)
                .setPositiveButton("NEXT LEVEL", (dialog, which) -> {
                    level++;
                    setupLevel();
                })
                .setNegativeButton("HOME", (dialog, which) -> {
                    goHome();
                })
                .show();
    }

    private void restartLevel() {
        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        setupLevel();
    }

    private void goHome() {
        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        gameFinished = true;

        if (getContext() instanceof Activity) {
            ((Activity) getContext()).finish();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        timerHandler.removeCallbacks(timerRunnable);
        animationHandler.removeCallbacks(animationRunnable);

        super.onDetachedFromWindow();
    }
}
