package com.candypop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 6;
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

            if (gameFinished) {
                return;
            }

            if (secondsLeft > 0) {
                secondsLeft--;

                invalidate();

                // The countdown must continue even while
                // candies are popping or the board is refilling.
                timerHandler.postDelayed(this, 1000);

            } else {

                // If a candy animation is finishing, wait briefly.
                // The animation will call showTimeUpDialog() when finished.
                if (animating) {
                    timerHandler.postDelayed(this, 100);
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
    }

    public void setLevel(int selectedLevel) {
        if (selectedLevel < 1) {
            selectedLevel = 1;
        }

        if (selectedLevel > MAX_LEVEL) {
            selectedLevel = MAX_LEVEL;
        }

        level = selectedLevel;
        setupLevel();
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

        } else if (cycle == 4) {            timeLimit = 130;
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

        // Special challenge levels
        if (level == 10 || level == 25 || level == 50
                || level == 75 || level == 100) {

            timeLimit = 90;
            minimumConnection = 4;
            objectiveType = OBJECTIVE_MIXED;
            objectiveTarget = 150 + level * 5;
            longConnectionGoal = 3 + level / 25;
        }

        // Keep the mixed objective's long-chain goal separate.
        if (objectiveType != OBJECTIVE_LONG
                && objectiveType != OBJECTIVE_MIXED) {
            longConnectionGoal = 0;
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

        createGuaranteedPath();
    }

    private void createGuaranteedPath() {

        int type = random.nextInt(CANDY_TYPES);

        int row = random.nextInt(ROWS);
        int col = random.nextInt(COLS);

        int length = Math.max(3, minimumConnection);

        if (objectiveType == OBJECTIVE_LONG
                || objectiveType == OBJECTIVE_MIXED) {
            length = Math.max(length, 5);
        }

        length = Math.min(length, ROWS * COLS);

        List<int[]> path = createSnakePath(row, col, length);

        for (int[] cell : path) {
            board[cell[0]][cell[1]] = type;
        }
    }

    private List<int[]> createSnakePath(
            int startRow,
            int startCol,
            int length) {

        List<int[]> path = new ArrayList<>();

        boolean[][] visited = new boolean[ROWS][COLS];

        int row = startRow;
        int col = startCol;

        path.add(new int[]{row, col});
        visited[row][col] = true;

        while (path.size() < length) {

            List<int[]> choices = new ArrayList<>();

            int[][] directions = {
                    {0, 1},
                    {0, -1},
                    {1, 0},
                    {-1, 0},
                    {1, 1},
                    {-1, -1},
                    {1, -1},
                    {-1, 1}
            };

            for (int[] direction : directions) {

                int nextRow = row + direction[0];
                int nextCol = col + direction[1];

                if (nextRow >= 0 && nextRow < ROWS
                        && nextCol >= 0 && nextCol < COLS
                        && !visited[nextRow][nextCol]) {

                    choices.add(new int[]{
                            nextRow,
                            nextCol
                    });
                }
            }

            if (choices.isEmpty()) {
                break;
            }

            int[] next = choices.get(
                    random.nextInt(choices.size())
            );

            row = next[0];
            col = next[1];

            visited[row][col] = true;
            path.add(new int[]{row, col});
        }

        return path;
    }

    private boolean hasValidConnection() {

        boolean[][] visited = new boolean[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {

                int type = board[row][col];

                if (type < 0) {
                    continue;
                }

                if (searchConnection(
                        row,
                        col,
                        type,
                        visited,
                        1)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean searchConnection(
            int row,
            int col,
            int type,
            boolean[][] visited,
            int count) {

        if (count >= minimumConnection) {
            return true;
        }

        visited[row][col] = true;

        int[][] directions = {
                {0, 1},
                {0, -1},
                {1, 0},
                {-1, 0},
                {1, 1},
                {-1, -1},
                {1, -1},
                {-1, 1}
        };

        for (int[] direction : directions) {

            int nextRow = row + direction[0];
            int nextCol = col + direction[1];

            if (nextRow < 0 || nextRow >= ROWS
                    || nextCol < 0 || nextCol >= COLS) {
                continue;
            }

            if (visited[nextRow][nextCol]) {
                continue;
            }

            if (board[nextRow][nextCol] != type) {
                continue;
            }

            if (searchConnection(
                    nextRow,
                    nextCol,
                    type,
                    visited,
                    count + 1)) {

                visited[row][col] = false;
                return true;
            }
        }

        visited[row][col] = false;
        return false;
    }

    private boolean isAdjacent(
            int row1,
            int col1,
            int row2,
            int col2) {

        int rowDifference = Math.abs(row1 - row2);
        int colDifference = Math.abs(col1 - col2);

        return rowDifference <= 1
                && colDifference <= 1
                && (rowDifference + colDifference > 0);
    }

    private boolean isSameCell(
            int row1,
            int col1,
            int row2,
            int col2) {

        return row1 == row2 && col1 == col2;
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

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        canvas.drawColor(Color.rgb(12, 17, 29));

        drawBackground(canvas);
        calculateBoardDimensions(width, height);
        drawHeader(canvas);
        drawLevelInfo(canvas);
        drawObjective(canvas);
        drawTimer(canvas);
        drawBoard(canvas);
        drawConnection(canvas);
        drawBottomInfo(canvas);
        drawEffects(canvas);
    }

    private void drawBackground(Canvas canvas) {

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(12, 17, 29));

        canvas.drawRect(
                0,
                0,
                getWidth(),
                getHeight(),
                paint
        );

        paint.setColor(Color.rgb(19, 28, 45));

        canvas.drawCircle(
                getWidth() * 0.1f,
                getHeight() * 0.25f,
                getWidth() * 0.35f,
                paint
        );

        paint.setColor(Color.rgb(17, 35, 43));

        canvas.drawCircle(
                getWidth() * 0.9f,
                getHeight() * 0.7f,
                getWidth() * 0.3f,
                paint
        );
    }

    private void calculateBoardDimensions(
            int width,
            int height) {

        float horizontalPadding = dp(14);
        float availableWidth = width - horizontalPadding * 2;

        cellSize = availableWidth / COLS;

        float boardHeight = cellSize * ROWS;

        float topArea = dp(250);
        float bottomArea = dp(100);

        if (height - topArea - bottomArea < boardHeight) {
            float availableHeight =
                    height - topArea - bottomArea;

            cellSize = Math.min(
                    cellSize,
                    availableHeight / ROWS
            );
        }

        boardLeft = (width - cellSize * COLS) / 2f;

        boardTop = Math.max(
                dp(210),
                (height - cellSize * ROWS) / 2f
        );

        if (boardTop + cellSize * ROWS > height - dp(80)) {
            boardTop = height - dp(80) - cellSize * ROWS;
        }

        boardTop = Math.max(dp(190), boardTop);
    }

    private void drawHeader(Canvas canvas) {

        float center = getWidth() / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(25));
        paint.setFakeBoldText(true);

        canvas.drawText(
                "CANDYJOLT",
                center,
                dp(38),
                paint
        );

        paint.setTextSize(dp(10));
        paint.setColor(Color.rgb(145, 165, 190));
        paint.setFakeBoldText(false);

        canvas.drawText(
                "CONNECT • POP • WIN",
                center,
                dp(57),
                paint
        );
    }

    private void drawLevelInfo(Canvas canvas) {

        float top = dp(72);
        float left = dp(14);
        float right = getWidth() - dp(14);

        drawRoundedPanel(
                canvas,
                left,
                top,
                right,
                top + dp(55),
                Color.rgb(24, 36, 55),
                dp(14)
        );

        paint.setColor(Color.rgb(145, 165, 190));
        paint.setTextSize(dp(10));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "LEVEL",
                left + dp(14),
                top + dp(19),
                paint
        );

        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(22));

        canvas.drawText(
                String.valueOf(level),
                left + dp(14),
                top + dp(43),
                paint
        );

        paint.setColor(Color.rgb(145, 165, 190));
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(dp(10));

        canvas.drawText(
                "SCORE",
                right - dp(14),
                top + dp(19),
                paint
        );

        paint.setColor(Color.rgb(255, 210, 95));
        paint.setTextSize(dp(22));

        canvas.drawText(
                String.valueOf(score),
                right - dp(14),
                top + dp(43),
                paint
        );
    }

    private void drawObjective(Canvas canvas) {

        float top = dp(138);
        float left = dp(14);
        float right = getWidth() - dp(14);

        drawRoundedPanel(
                canvas,
                left,
                top,
                right,
                top + dp(45),
                Color.rgb(24, 36, 55),
                dp(12)
        );

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(11));
        paint.setFakeBoldText(true);
        paint.setColor(Color.rgb(145, 165, 190));

        canvas.drawText(
                "LEVEL OBJECTIVE",
                left + dp(12),
                top + dp(17),
                paint
        );

        paint.setTextSize(dp(13));
        paint.setColor(Color.WHITE);

        String objective = getObjectiveText();

        canvas.drawText(
                objective,
                left + dp(12),
                top + dp(35),
                paint
        );

        paint.setFakeBoldText(false);
    }

    private String getObjectiveText() {

        switch (objectiveType) {

            case OBJECTIVE_CANDIES:
                return "Pop " + objectiveTarget
                        + " candies  ("
                        + candiesPoppedThisLevel
                        + "/" + objectiveTarget + ")";

            case OBJECTIVE_CONNECTIONS:
                return "Make " + objectiveTarget
                        + " connections  ("
                        + connectionsThisLevel
                        + "/" + objectiveTarget + ")";

            case OBJECTIVE_LONG:
                return "Make " + longConnectionGoal
                        + " long connections  ("
                        + longConnections
                        + "/" + longConnectionGoal + ")";

            case OBJECTIVE_COMBO:
                return "Reach combo " + objectiveTarget
                        + "  (" + bestCombo
                        + "/" + objectiveTarget + ")";

            case OBJECTIVE_MIXED:
                return "Score " + objectiveTarget
                        + " + " + longConnectionGoal
                        + " long connections";

            default:
                return "Connect matching candies";
        }
    }

    private void drawTimer(Canvas canvas) {

        float centerX = getWidth() / 2f;
        float y = dp(198);

        int timerColor = secondsLeft <= 10
                ? Color.rgb(255, 75, 75)
                : Color.rgb(80, 225, 145);

        paint.setColor(timerColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(23));
        paint.setFakeBoldText(true);

        String timerText = String.format(
                java.util.Locale.getDefault(),
                "%02d:%02d",
                secondsLeft / 60,
                secondsLeft % 60
        );

        canvas.drawText(
                timerText,
                centerX,
                y,
                paint
        );

        paint.setFakeBoldText(false);
    }

    private void drawBoard(Canvas canvas) {

        float right = boardLeft + cellSize * COLS;
        float bottom = boardTop + cellSize * ROWS;

        drawRoundedPanel(
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

                RectF cellRect = new RectF(
                        left + inset,
                        top + inset,
                        left + cellSize - inset,
                        top + cellSize - inset
                );

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(35, 49, 69));

                canvas.drawRoundRect(
                        cellRect,
                        dp(9),
                        dp(9),
                        paint
                );

                int type = board[row][col];

                if (type >= 0) {
                    drawCandy(
                            canvas,
                            row,
                            col,
                            type
                    );
                }

                if (isCellSelected(row, col)) {
                    drawSelectedCell(
                            canvas,
                            cellRect
                    );
                }
            }
        }
    }

    private void drawSelectedCell(
            Canvas canvas,
            RectF rect) {

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(Color.WHITE);

        canvas.drawRoundRect(
                rect,
                dp(9),
                dp(9),
                paint
        );

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawRoundedPanel(
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
                left,
                top,
                right,
                bottom,
                radius,
                radius,
                paint
        );
    }
Width(dp(1));
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawLine(
                x,
                y - radius * 0.35f,
                x,
                y + radius * 0.35f,
                paint
        );

        canvas.drawLine(
                x - radius * 0.35f,
                y,
                x + radius * 0.35f,
                y,
                paint
        );

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPinkCandy(
            Canvas canvas,
            float x,
            float y,
            float radius) {

        paint.setColor(Color.WHITE);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(radius * 0.85f);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "★",
                x,
                y + radius * 0.3f,
                paint
        );

        paint.setFakeBoldText(false);
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

        lineGlowPaint.setColor(
                Color.argb(90, 255, 255, 255)
        );
        lineGlowPaint.setStrokeWidth(dp(11));

        canvas.drawPath(connectionPath, lineGlowPaint);

        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(dp(4));

        canvas.drawPath(connectionPath, linePaint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        for (int[] cell : selected) {

            float x = boardLeft
                    + cell[1] * cellSize
                    + cellSize / 2f;

            float y = boardTop
                    + cell[0] * cellSize
                    + cellSize / 2f;

            canvas.drawCircle(
                    x,
                    y,
                    dp(4),
                    paint
            );
        }
    }

    private void drawBottomInfo(Canvas canvas) {

        float y = getHeight() - dp(35);

        paint.setColor(Color.rgb(155, 175, 195));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(11));

        canvas.drawText(
                "Drag across matching fruits to connect them",
                getWidth() / 2f,
                y,
                paint
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

            canvas.drawCircle(
                    x,
                    y,
                    radius,
                    effectPaint
            );
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

                lastTouchX = x;
                lastTouchY = y;

                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawing) {
                    return true;
                }

                currentFingerX = x;
                currentFingerY = y;

                int[] next = getCellFromPosition(x, y);

                if (next != null) {
                    addSelectedCell(next[0], next[1]);
                }

                invalidate();
                return true;

            case MotionEvent.ACTION_UP:

                if (drawing) {
                    drawing = false;

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

        // Score increases with chain length.
        // There is deliberately no score cap.
        int points = chainLength * chainLength * 10;

        if (chainLength >= 5) {
            points += 50;
        }

        if (chainLength >= 7) {
            points += 100;
        }

        score += points;

        candiesPoppedThisLevel += chainLength;
        connectionsThisLevel++;

        if (chainLength >= 5) {
            longConnections++;
        }

        combo++;

        if (combo > bestCombo) {
            bestCombo = combo;
        }

        if (objectiveType == OBJECTIVE_COMBO
                && bestCombo >= objectiveTarget) {
            // Objective is recorded as complete.
            // The player continues until the timer expires.
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

    private void updateComboAfterMove() {

        if (selected.size() < minimumConnection) {
            combo = 0;
        }
    }
    private void refillBoard() {

        // Move remaining candies down each column.
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

        if (gameFinished) {
            return;
        }

        if (animating) {
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