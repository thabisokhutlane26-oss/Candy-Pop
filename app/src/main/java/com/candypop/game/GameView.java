package com.candypop.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 6;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[][] board = new int[ROWS][COLS];
    private final List<int[]> selected = new ArrayList<>();
    private final Path connectionPath = new Path();

    private float cellSize;
    private float boardLeft;
    private float boardTop;

    private boolean drawing = false;

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

        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(Color.argb(55, 0, 0, 0));

        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(20f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.WHITE);

        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(3f);

        createBoard();

        setFocusable(true);
    }

    private void createBoard() {

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] =
                        (int) (Math.random() * CANDY_TYPES);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(255, 238, 249));

        cellSize = Math.min(
                getWidth() / (float) COLS,
                getHeight() / (float) ROWS
        );

        float boardWidth = cellSize * COLS;
        float boardHeight = cellSize * ROWS;

        boardLeft =
                (getWidth() - boardWidth) / 2f;

        boardTop =
                (getHeight() - boardHeight) / 2f;

        drawBoardBackground(canvas);
        drawBoard(canvas);

        if (drawing && selected.size() >= 2) {
            canvas.drawPath(
                    connectionPath,
                    linePaint
            );
        }

        drawSelection(canvas);
    }

    private void drawBoardBackground(Canvas canvas) {

        paint.setColor(Color.argb(40, 124, 77, 255));

        RectF boardRect = new RectF(
                boardLeft - 10,
                boardTop - 10,
                boardLeft + cellSize * COLS + 10,
                boardTop + cellSize * ROWS + 10
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

                float centerX =
                        getCellCenterX(col);

                float centerY =
                        getCellCenterY(row);

                float radius =
                        cellSize * 0.32f;

                drawCandy(
                        canvas,
                        centerX,
                        centerY,
                        radius,
                        board[row][col]
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

        int color = candyColors[type];

        float bodyWidth = radius * 1.55f;
        float bodyHeight = radius * 1.65f;

        // Shadow
        canvas.drawOval(
                new RectF(
                        x - bodyWidth / 2f + 3,
                        y - bodyHeight / 2f + 7,
                        x + bodyWidth / 2f + 3,
                        y + bodyHeight / 2f + 7
                ),
                shadowPaint
        );

        // Wrapper ends
        paint.setColor(darken(color, 0.78f));

        Path leftWrapper = new Path();

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

        Path rightWrapper = new Path();

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

        // Candy body
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

        // Dark outline
        outlinePaint.setColor(
                darken(color, 0.65f)
        );

        canvas.drawRoundRect(
                body,
                radius * 0.45f,
                radius * 0.45f,
                outlinePaint
        );

        // Candy stripes
        paint.setColor(
                Color.argb(75, 255, 255, 255)
        );

        for (int i = -1; i <= 1; i++) {

            canvas.drawRoundRect(
                    new RectF(
                            x + i * radius * 0.42f - 3,
                            y - bodyHeight * 0.34f,
                            x + i * radius * 0.42f + 3,
                            y + bodyHeight * 0.34f
                    ),
                    4,
                    4,
                    paint
            );
        }

        // Large shine
        paint.setColor(
                Color.argb(185, 255, 255, 255)
        );

        canvas.drawOval(
                new RectF(
                        x - bodyWidth * 0.28f,
                        y - bodyHeight * 0.31f,
                        x + bodyWidth * 0.02f,
                        y - bodyHeight * 0.05f
                ),
                paint
        );

        // Small shine
        paint.setColor(
                Color.argb(120, 255, 255, 255)
        );

        canvas.drawCircle(
                x + bodyWidth * 0.23f,
                y + bodyHeight * 0.25f,
                radius * 0.09f,
                paint
        );
    }

    private void drawSelection(Canvas canvas) {

        if (!drawing) {
            return;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(
                Color.argb(180, 255, 255, 255)
        );

        for (int[] cell : selected) {

            float x =
                    getCellCenterX(cell[1]);

            float y =
                    getCellCenterY(cell[0]);

            canvas.drawCircle(
                    x,
                    y,
                    cellSize * 0.39f,
                    paint
            );
        }

        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                selected.clear();
                connectionPath.reset();

                int[] first =
                        getCell(x, y);

                if (first != null) {

                    selected.add(first);

                    connectionPath.moveTo(
                            getCellCenterX(first[1]),
                            getCellCenterY(first[0])
                    );

                    drawing = true;

                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!drawing || selected.isEmpty()) {
                    return true;
                }

                int[] current =
                        getCell(x, y);

                if (current != null) {

                    int[] last =
                            selected.get(
                                    selected.size() - 1
                            );

                    if (current[0] != last[0]
                            || current[1] != last[1]) {

                        if (isAdjacent(last, current)
                                && board[current[0]][current[1]]
                                == board[last[0]][last[1]]) {

                            if (!alreadySelected(current)) {

                                selected.add(current);

                                connectionPath.lineTo(
                                        getCellCenterX(
                                                current[1]
                                        ),
                                        getCellCenterY(
                                                current[0]
                                        )
                                );

                                invalidate();
                            }
                        }
                    }
                }

                return true;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                if (drawing) {

                    if (selected.size() >= 3) {
                        removeSelected();
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

    private int[] getCell(
            float x,
            float y
    ) {

        if (cellSize <= 0) {
            return null;
        }

        int col =
                (int) ((x - boardLeft) / cellSize);

        int row =
                (int) ((y - boardTop) / cellSize);

        if (row < 0 || row >= ROWS
                || col < 0 || col >= COLS) {

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
                Math.abs(a[0] - b[0]);

        int colDistance =
                Math.abs(a[1] - b[1]);

        return rowDistance + colDistance == 1;
    }

    private boolean alreadySelected(
            int[] cell
    ) {

        for (int[] selectedCell : selected) {

            if (selectedCell[0] == cell[0]
                    && selectedCell[1] == cell[1]) {

                return true;
            }
        }

        return false;
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

        for (int[] cell : selected) {

            board[cell[0]][cell[1]] = -1;
        }

        // Drop candies downward.
        for (int col = 0; col < COLS; col++) {

            int writeRow = ROWS - 1;

            for (
                    int row = ROWS - 1;
                    row >= 0;
                    row--
            ) {

                if (board[row][col] != -1) {

                    board[writeRow][col] =
                            board[row][col];

                    writeRow--;
                }
            }

            // Spawn new candies.
            while (writeRow >= 0) {

                board[writeRow][col] =
                        (int) (
                                Math.random()
                                        * CANDY_TYPES
                        );

                writeRow--;
            }
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

        return Color.rgb(r, g, b);
    }
}