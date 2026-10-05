package com.candypop.game;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    private CandyGameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(22, 11, 43));
        getWindow().setNavigationBarColor(Color.rgb(22, 11, 43));

        gameView = new CandyGameView();
        setContentView(gameView);
    }

    private class CandyGameView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final Handler handler = new Handler();

        private final ArrayList<Candy> candies = new ArrayList<>();

        private final SharedPreferences preferences;

        private int score = 0;
        private int highScore = 0;
        private int timeLeft = 60;
        private int level = 1;

        private boolean gameOver = false;

        private final int[] candyColors = {
                Color.rgb(255, 72, 110),
                Color.rgb(255, 193, 7),
                Color.rgb(0, 188, 212),
                Color.rgb(156, 39, 176),
                Color.rgb(76, 175, 80),
                Color.rgb(255, 112, 67)
        };

        private final Runnable timer = new Runnable() {
            @Override
            public void run() {

                if (!gameOver) {

                    timeLeft--;

                    if (timeLeft <= 0) {
                        timeLeft = 0;
                        gameOver = true;

                        saveHighScore();
                    }

                    invalidate();

                    if (!gameOver) {
                        handler.postDelayed(this, 1000);
                    }
                }
            }
        };

        CandyGameView() {
            super(MainActivity.this);

            preferences = getSharedPreferences(
                    "CandyJoltSettings",
                    MODE_PRIVATE
            );

            highScore = preferences.getInt(
                    "highScore",
                    0
            );

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            startGame();
        }

        private void saveHighScore() {

            if (score > highScore) {

                highScore = score;

                preferences.edit()
                        .putInt("highScore", highScore)
                        .apply();
            }
        }

        private void startGame() {

            score = 0;
            timeLeft = 60;
            level = 1;
            gameOver = false;

            candies.clear();

            for (int i = 0; i < 15; i++) {
                candies.add(createCandy());
            }

            handler.removeCallbacks(timer);
            handler.postDelayed(timer, 1000);

            invalidate();
        }

        private void updateLevel() {

            int newLevel = (score / 100) + 1;

            if (newLevel != level) {

                level = newLevel;

                int targetCandies =
                        Math.min(
                                10 + level * 5,
                                35
                        );

                while (candies.size() < targetCandies) {
                    candies.add(createCandy());
                }
            }
        }

        private Candy createCandy() {

            Candy candy = new Candy();

            candy.radius = 32 + random.nextInt(9);

            float safeWidth = Math.max(
                    1,
                    getWidth() - 100
            );

            float safeHeight = Math.max(
                    1,
                    getHeight() - 300
            );

            candy.x =
                    50 +
                    candy.radius +
                    random.nextFloat() *
                    safeWidth;

            candy.y =
                    175 +
                    random.nextFloat() *
                    safeHeight;

            candy.color =
                    candyColors[
                            random.nextInt(
                                    candyColors.length
                            )
                    ];

            return candy;
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            drawBackground(canvas);
            drawHeader(canvas);

            if (!gameOver) {
                drawCandies(canvas);
            } else {
                drawGameOver(canvas);
            }
        }

        private void drawBackground(Canvas canvas) {

            canvas.drawColor(
                    Color.rgb(25, 12, 52)
            );

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    Color.rgb(45, 18, 82)
            );

            canvas.drawCircle(
                    getWidth() * 0.15f,
                    getHeight() * 0.25f,
                    130,
                    paint
            );

            paint.setColor(
                    Color.rgb(60, 22, 95)
            );

            canvas.drawCircle(
                    getWidth() * 0.85f,
                    getHeight() * 0.65f,
                    160,
                    paint
            );
        }

        private void drawHeader(Canvas canvas) {

            paint.setTextAlign(Paint.Align.CENTER);

            paint.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(34);

            canvas.drawText(
                    "CANDYJOLT",
                    getWidth() / 2f,
                    55,
                    paint
            );

            paint.setTextSize(18);

            paint.setColor(
                    Color.rgb(255, 220, 80)
            );

            canvas.drawText(
                    "SCORE: " + score,
                    getWidth() * 0.20f,
                    105,
                    paint
            );

            paint.setColor(
                    Color.rgb(100, 230, 255)
            );

            canvas.drawText(
                    "TIME: " + timeLeft,
                    getWidth() * 0.50f,
                    105,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 120, 210)
            );

            canvas.drawText(
                    "LEVEL: " + level,
                    getWidth() * 0.80f,
                    105,
                    paint
            );

            paint.setColor(Color.LTGRAY);
            paint.setTextSize(16);

            canvas.drawText(
                    "Tap the candies!",
                    getWidth() / 2f,
                    140,
                    paint
            );
        }

        private void drawCandies(Canvas canvas) {

            for (Candy candy : candies) {

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(candy.color);

                canvas.drawCircle(
                        candy.x,
                        candy.y,
                        candy.radius,
                        paint
                );

                paint.setColor(
                        Color.argb(
                                220,
                                255,
                                255,
                                255
                        )
                );

                canvas.drawCircle(
                        candy.x -
                                candy.radius * 0.32f,
                        candy.y -
                                candy.radius * 0.32f,
                        candy.radius * 0.18f,
                        paint
                );

                paint.setColor(
                        Color.argb(
                                100,
                                255,
                                255,
                                255
                        )
                );

                paint.setStrokeWidth(5);

                canvas.drawLine(
                        candy.x -
                                candy.radius * 0.65f,
                        candy.y +
                                candy.radius * 0.35f,
                        candy.x +
                                candy.radius * 0.45f,
                        candy.y -
                                candy.radius * 0.45f,
                        paint
                );
            }
        }

        private void drawGameOver(Canvas canvas) {

            paint.setColor(
                    Color.argb(
                            225,
                            10,
                            5,
                            25
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    paint
            );

            paint.setTextAlign(Paint.Align.CENTER);

            paint.setColor(Color.WHITE);
            paint.setTextSize(42);

            canvas.drawText(
                    "GAME OVER",
                    getWidth() / 2f,
                    getHeight() * 0.30f,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 220, 80)
            );

            paint.setTextSize(28);

            canvas.drawText(
                    "Score: " + score,
                    getWidth() / 2f,
                    getHeight() * 0.39f,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 120, 210)
            );

            paint.setTextSize(24);

            canvas.drawText(
                    "Level: " + level,
                    getWidth() / 2f,
                    getHeight() * 0.45f,
                    paint
            );

            paint.setColor(Color.LTGRAY);
            paint.setTextSize(20);

            canvas.drawText(
                    "High Score: " + highScore,
                    getWidth() / 2f,
                    getHeight() * 0.51f,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 64, 129)
            );

            float left =
                    getWidth() * 0.25f;

            float right =
                    getWidth() * 0.75f;

            float top =
                    getHeight() * 0.57f;

            float bottom =
                    getHeight() * 0.67f;

            canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    30,
                    30,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(22);

            canvas.drawText(
                    "PLAY AGAIN",
                    getWidth() / 2f,
                    getHeight() * 0.635f,
                    paint
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() !=
                    MotionEvent.ACTION_DOWN) {

                return true;
            }

            float touchX = event.getX();
            float touchY = event.getY();

            if (gameOver) {

                float left =
                        getWidth() * 0.25f;

                float right =
                        getWidth() * 0.75f;

                float top =
                        getHeight() * 0.57f;

                float bottom =
                        getHeight() * 0.67f;

                if (touchX >= left &&
                        touchX <= right &&
                        touchY >= top &&
                        touchY <= bottom) {

                    startGame();
                }

                return true;
            }

            for (int i =
                    candies.size() - 1;
                    i >= 0;
                    i--) {

                Candy candy =
                        candies.get(i);

                float dx =
                        touchX - candy.x;

                float dy =
                        touchY - candy.y;

                float distance =
                        (float) Math.sqrt(
                                dx * dx +
                                dy * dy
                        );

                if (distance <=
                        candy.radius + 20) {

                    score += 10;

                    candies.remove(i);

                    candies.add(
                            createCandy()
                    );

                    updateLevel();

                    invalidate();

                    break;
                }
            }

            return true;
        }

        @Override
        protected void onDetachedFromWindow() {

            super.onDetachedFromWindow();

            handler.removeCallbacks(timer);
        }

        private class Candy {

            float x;
            float y;
            float radius;

            int color;
        }
    }
}