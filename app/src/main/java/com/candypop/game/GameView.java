package com.candypop.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GameView extends View {

    private static final int ROWS = 7;
    private static final int COLS = 7;
    private static final int CANDY_TYPES = 6;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[][] board = new int[ROWS][COLS];
    private final List<int[]> selected = new ArrayList<>();
    private final Path connectionPath = new Path();

    private float cellSize;
    private float boardLeft;
    private float boardTop;

    private boolean drawing = false;

    private final int[] candyColors = {
            Color.rgb(255, 82, 82),
            Color.rgb(255, 193, 7),
            Color.rgb(76, 175, 80),
            Color.rgb(66, 165, 245),
            Color.rgb(171, 71, 188),
            Color.rgb(255, 112, 67)
    };

    public GameView(Context context) {
        super(context);

        paint.setStyle(Paint.Style.FILL);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(18f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.WHITE);

        createBoard();

        setFocusable(true);
    }

    private void createBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = (int) (Math.random() * CANDY_TYPES);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(255, 244, 250));

        cellSize = Math.min(
                getWidth() / (float) COLS,
                getHeight() / (float) ROWS
        );

        float boardWidth = cellSize * COLS;
        float boardHeight = cellSize * ROWS;

        boardLeft = (getWidth() - boardWidth) / 2f;
        boardTop = (getHeight() - boardHeight) / 2f;

        drawBoard(canvas);

        if (drawing && selected.size() >= 2) {
            canvas.drawPath(connectionPath, linePaint);
        }
    }

    private void drawBoard(Canvas canvas) {

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {

                float centerX = boardLeft + col * cellSize + cellSize / 2f;
                float centerY = boardTop + row * cellSize + cellSize / 2f;

                float radius = cellSize * 0.34f;

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

        // Candy body
        paint.setColor(color);
        canvas.drawCircle(x, y, radius, paint);

        // Highlight
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(
                x - radius * 0.30f,
                y - radius * 0.30f,
                radius * 0.22f,
                paint
        );

        // Shine
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawCircle(
                x + radius * 0.25f,
                y + radius * 0.25f,
                radius * 0.12f,
                paint
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                selected.clear();
                connectionPath.reset();

                int[] first = getCell(x, y);

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

                int[] current = getCell(x, y);

                if (current != null) {

                    int[] last = selected.get(selected.size() - 1);

                    if (current[0] != last[0] ||
                            current[1] != last[1]) {

                        if (isAdjacent(last, current)
                                && board[current[0]][current[1]]
                                == board[last[0]][last[1]]) {

                            if (!alreadySelected(current)) {

                                selected.add(current);

                                connectionPath.lineTo(
                                        getCellCenterX(current[1]),
                                        getCellCenterY(current[0])
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

    private int[] getCell(float x, float y) {

        if (cellSize <= 0) {
            return null;
        }

        int col = (int) ((x - boardLeft) / cellSize);
        int row = (int) ((y - boardTop) / cellSize);

        if (row < 0 || row >= ROWS ||
                col < 0 || col >= COLS) {
            return null;
        }

        return new int[]{row, col};
    }

    private boolean isAdjacent(int[] a, int[] b) {

        int rowDistance = Math.abs(a[0] - b[0]);
        int colDistance = Math.abs(a[1] - b[1]);

        return rowDistance + colDistance == 1;
    }

    private boolean alreadySelected(int[] cell) {

        for (int[] selectedCell : selected) {

            if (selectedCell[0] == cell[0]
                    && selectedCell[1] == cell[1]) {
                return true;
            }
        }

        return false;
    }

    private float getCellCenterX(int col) {
        return boardLeft + col * cellSize + cellSize / 2f;
    }

    private float getCellCenterY(int row) {
        return boardTop + row * cellSize + cellSize / 2f;
    }

    private void removeSelected() {

        for (int[] cell : selected) {
            board[cell[0]][cell[1]] = -1;
        }

        // Drop candies downward.
        for (int col = 0; col < COLS; col++) {

            int writeRow = ROWS - 1;

            for (int row = ROWS - 1; row >= 0; row--) {

                if (board[row][col] != -1) {

                    board[writeRow][col] = board[row][col];
                    writeRow--;
                }
            }

            // Create new candies at the top.
            while (writeRow >= 0) {

                board[writeRow][col] =
                        (int) (Math.random() * CANDY_TYPES);

                writeRow--;
            }
        }
    }
}