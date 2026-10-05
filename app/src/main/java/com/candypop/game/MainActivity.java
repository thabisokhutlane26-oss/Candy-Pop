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

        private int combo = 0;
        private long lastTapTime = 0;

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
                        combo = 0;
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
            combo = 0;
            lastTapTime = 0;
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

        private void updateCombo() {

            long now = System.currentTimeMillis();

            if (lastTapTime == 0 ||
                    now - lastTapTime <= 2000) {

                combo++;

            } else {

                combo = 1;
            }

            lastTapTime = now;
        }

        private int getPointsForTap() {

            if (combo >= 10) {
                return 50;
            }

            if (combo >= 7) {
                return 40;
            }

            if (combo >= 5) {
                return 30;
            }

            if (combo >= 3) {
                return 20;
            }

            return 10;
        }

        private Candy createCandy() {

            Candy candy = new Candy();

            candy.radius = 34 + random.nextInt(8);

            float safeWidth = Math.max(
                    1,
                    getWidth() - 120
            );

            float safeHeight = Math.max(
                    1,
                    getHeight() - 310
            );

            candy.x =
                    60 +
                    random.nextFloat() *
                    safeWidth;

            candy.y =
                    185 +
                    random.nextFloat() *
                    safeHeight;

            candy.color =
                    candyColors[
                            random.nextInt(
                                    candyColors.length
                            )
                    ];

            candy.style = random.nextInt(3);

            candy.isBomb = random.nextInt(10) == 0;

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

            paint.setColor(
                    Color.rgb(40, 16, 70)
            );

            canvas.drawCircle(
                    getWidth() * 0.55f,
                    getHeight() * 0.88f,
                    120,
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

            paint.setTextSize(17);

            paint.setColor(
                    Color.rgb(255, 220, 80)
            );

            canvas.drawText(
                    "SCORE: " + score,
                    getWidth() * 0.17f,
                    105,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 100, 170)
            );

            canvas.drawText(
                    "COMBO x" + combo,
                    getWidth() * 0.50f,
                    105,
                    paint
            );

            paint.setColor(
                    Color.rgb(100, 230, 255)
            );

            canvas.drawText(
                    "TIME: " + timeLeft,
                    getWidth() * 0.83f,
                    105,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 120, 210)
            );

            canvas.drawText(
                    "LEVEL: " + level,
                    getWidth() / 2f,
                    135,
                    paint
            );

            paint.setColor(Color.LTGRAY);
            paint.setTextSize(16);

            canvas.drawText(
                    "Tap the candies!",
                    getWidth() / 2f,
                    160,
                    paint
            );
        }

        private void drawCandies(Canvas canvas) {

            for (Candy candy : candies) {

                if (candy.isBomb) {
                    drawBomb(canvas, candy);
                } else {

                    if (candy.style == 0) {
                        drawStripedCandy(canvas, candy);
                    } else if (candy.style == 1) {
                        drawWrappedCandy(canvas, candy);
                    } else {
                        drawRoundCandy(canvas, candy);
                    }
                }
            }
        }

        private void drawRoundCandy(
                Canvas canvas,
                Candy candy
        ) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    darken(candy.color)
            );

            canvas.drawCircle(
                    candy.x + 2,
                    candy.y + 4,
                    candy.radius + 2,
                    paint
            );

            paint.setColor(candy.color);

            canvas.drawCircle(
                    candy.x,
                    candy.y,
                    candy.radius,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            210,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    candy.x -
                            candy.radius * 0.32f,
                    candy.y -
                            candy.radius * 0.35f,
                    candy.radius * 0.20f,
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
                    candy.x +
                            candy.radius * 0.25f,
                    candy.y +
                            candy.radius * 0.25f,
                    candy.radius * 0.42f,
                    paint
            );
        }

        private void drawStripedCandy(
                Canvas canvas,
                Candy candy
        ) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    darken(candy.color)
            );

            canvas.drawRoundRect(
                    candy.x - candy.radius,
                    candy.y - candy.radius * 0.78f,
                    candy.x + candy.radius,
                    candy.y + candy.radius * 0.78f,
                    candy.radius,
                    candy.radius,
                    paint
            );

            paint.setColor(candy.color);

            canvas.drawRoundRect(
                    candy.x - candy.radius,
                    candy.y - candy.radius * 0.85f,
                    candy.x + candy.radius,
                    candy.y + candy.radius * 0.85f,
                    candy.radius,
                    candy.radius,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            120,
                            255,
                            255,
                            255
                    )
            );

            paint.setStrokeWidth(
                    candy.radius * 0.22f
            );

            canvas.drawLine(
                    candy.x - candy.radius * 0.70f,
                    candy.y + candy.radius * 0.60f,
                    candy.x + candy.radius * 0.70f,
                    candy.y - candy.radius * 0.60f,
                    paint
            );

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    candy.x -
                            candy.radius * 0.35f,
                    candy.y -
                            candy.radius * 0.35f,
                    candy.radius * 0.15f,
                    paint
            );
        }

        private void drawWrappedCandy(
                Canvas canvas,
                Candy candy
        ) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    darken(candy.color)
            );

            canvas.drawCircle(
                    candy.x,
                    candy.y,
                    candy.radius,
                    paint
            );

            paint.setColor(candy.color);

            canvas.drawCircle(
                    candy.x,
                    candy.y,
                    candy.radius * 0.80f,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            210,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    candy.x -
                            candy.radius * 0.30f,
                    candy.y -
                            candy.radius * 0.30f,
                    candy.radius * 0.17f,
                    paint
            );

            paint.setColor(
                    Color.argb(
                            170,
                            255,
                            255,
                            255
                    )
            );

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            canvas.drawCircle(
                    candy.x,
                    candy.y,
                    candy.radius * 0.55f,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);
        }

        private int darken(int color) {

            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);

            r = (int) (r * 0.72f);
            g = (int) (g * 0.72f);
            b = (int) (b * 0.72f);

            return Color.rgb(r, g, b);
        }

        private void drawBomb(
                Canvas canvas,
                Candy bomb
        ) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    Color.rgb(18, 18, 25)
            );

            canvas.drawCircle(
                    bomb.x + 2,
                    bomb.y + 4,
                    bomb.radius + 3,
                    paint
            );

            paint.setColor(
                    Color.rgb(45, 45, 58)
            );

            canvas.drawCircle(
                    bomb.x,
                    bomb.y,
                    bomb.radius,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 70, 110)
            );

            canvas.drawCircle(
                    bomb.x,
                    bomb.y,
                    bomb.radius * 0.43f,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(
                    bomb.radius * 0.55f
            );

            canvas.drawText(
                    "!",
                    bomb.x,
                    bomb.y +
                            bomb.radius * 0.19f,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 193, 7)
            );

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);

            canvas.drawLine(
                    bomb.x,
                    bomb.y - bomb.radius,
                    bomb.x + bomb.radius * 0.38f,
                    bomb.y - bomb.radius * 1.32f,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    Color.rgb(255, 235, 59)
            );

            canvas.drawCircle(
                    bomb.x + bomb.radius * 0.43f,
                    bomb.y - bomb.radius * 1.38f,
                    6,
                    paint
            );
        }

        private void explodeBomb(Candy bomb) {

            float explosionRadius =
                    bomb.radius * 3.5f;

            ArrayList<Candy> toRemove =
                    new ArrayList<>();

            for (Candy candy : candies) {

                if (candy == bomb) {
                    continue;
                }

                float dx =
                        candy.x - bomb.x;

                float dy =
                        candy.y - bomb.y;

                float distance =
                        (float) Math.sqrt(
                                dx * dx +
                                dy * dy
                        );

                if (distance <= explosionRadius) {
                    toRemove.add(candy);
                }
            }

            for (Candy candy : toRemove) {
                candies.remove(candy);
            }

            score += 50;

            combo++;

            while (candies.size() < 15) {
                candies.add(createCandy());
            }

            updateLevel();
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

                    if (candy.isBomb) {

                        explodeBomb(candy);

                    } else {

                        updateCombo();

                        score += getPointsForTap();

                        candies.remove(i);

                        candies.add(
                                createCandy()
                        );

                        updateLevel();
                    }

                    saveHighScore();
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
            int style;
            boolean isBomb;
        }
    }
}