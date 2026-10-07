package com.candypop.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.graphics.Typeface;

public class MainActivity extends Activity {

    private HomeView homeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        hideSystemBars();

        homeView = new HomeView(this);
        setContentView(homeView);
    }

    private void hideSystemBars() {

        if (android.os.Build.VERSION.SDK_INT >= 30) {

            getWindow().setDecorFitsSystemWindows(false);

            getWindow().getInsetsController().hide(
                    WindowInsets.Type.statusBars()
                            | WindowInsets.Type.navigationBars()
            );

            getWindow().getInsetsController().setSystemBarsBehavior(
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

        } else {

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void startGame() {

        Intent intent = new Intent(
                MainActivity.this,
                GameActivity.class
        );

        intent.putExtra("level", 1);

        startActivity(intent);
    }

    private void showMessage(
            String title,
            String message
    ) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private class HomeView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        private final int PINK = Color.rgb(255, 72, 145);
        private final int DARK_PINK = Color.rgb(218, 43, 116);
        private final int PURPLE = Color.rgb(119, 72, 235);
        private final int BLUE = Color.rgb(54, 157, 239);
        private final int CYAN = Color.rgb(34, 190, 207);
        private final int YELLOW = Color.rgb(255, 183, 35);
        private final int DARK = Color.rgb(61, 35, 75);
        private final int WHITE = Color.WHITE;

        private float scale = 1f;

        private RectF playRect = new RectF();
        private RectF levelsRect = new RectF();
        private RectF scoresRect = new RectF();
        private RectF settingsRect = new RectF();

        private float pressedX;
        private float pressedY;

        public HomeView(Context context) {
            super(context);

            setFocusable(true);

            textPaint.setTypeface(Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            ));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            scale = Math.min(width / 360f, height / 720f);

            if (scale < 0.75f) {
                scale = 0.75f;
            }

            drawBackground(canvas, width, height);
            drawFloatingCandies(canvas, width, height);
            drawHeader(canvas, width);
            drawProgressCard(canvas, width);
            drawPlayButton(canvas, width);
            drawMenuButtons(canvas, width);
            drawFeatureStrip(canvas, width, height);

            postInvalidateDelayed(40);
        }

        private void drawBackground(
                Canvas canvas,
                float width,
                float height
        ) {

            LinearGradient gradient = new LinearGradient(
                    0,
                    0,
                    width,
                    height,
                    Color.rgb(255, 213, 238),
                    Color.rgb(210, 203, 255),
                    Shader.TileMode.CLAMP
            );

            paint.setShader(gradient);
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);

            // Soft light circles.
            paint.setColor(Color.argb(35, 255, 255, 255));

            canvas.drawCircle(
                    width * 0.08f,
                    height * 0.18f,
                    width * 0.18f,
                    paint
            );

            canvas.drawCircle(
                    width * 0.92f,
                    height * 0.28f,
                    width * 0.22f,
                    paint
            );

            canvas.drawCircle(
                    width * 0.50f,
                    height * 0.82f,
                    width * 0.30f,
                    paint
            );
        }

        private void drawFloatingCandies(
                Canvas canvas,
                float width,
                float height
        ) {

            float t = (System.currentTimeMillis() % 5000) / 5000f;

            float movement =
                    (float) Math.sin(t * Math.PI * 2) * 4f;

            drawStrawberry(
                    canvas,
                    width * 0.10f,
                    72 + movement,
                    width * 0.045f
            );

            drawWrappedCandy(
                    canvas,
                    width * 0.89f,
                    105 - movement,
                    width * 0.045f
            );

            drawLollipop(
                    canvas,
                    width * 0.87f,
                    height * 0.47f + movement,
                    width * 0.055f
            );

            drawApple(
                    canvas,
                    width * 0.10f,
                    height * 0.58f - movement,
                    width * 0.052f
            );

            drawGrapes(
                    canvas,
                    width * 0.88f,
                    height * 0.73f + movement,
                    width * 0.045f
            );
        }

        private void drawHeader(
                Canvas canvas,
                float width
        ) {

            float centerX = width / 2f;

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            ));

            textPaint.setTextSize(38 * scale);
            textPaint.setColor(PURPLE);
            textPaint.setShadowLayer(
                    9 * scale,
                    0,
                    5 * scale,
                    Color.argb(75, 70, 30, 100)
            );

            canvas.drawText(
                    "CANDYJOLT",
                    centerX,
                    74 * scale,
                    textPaint
            );

            textPaint.clearShadowLayer();

            textPaint.setTextSize(13 * scale);
            textPaint.setColor(DARK);

            canvas.drawText(
                    "MATCH  •  CONNECT  •  POP!",
                    centerX,
                    97 * scale,
                    textPaint
            );

            // Decorative candy row.
            drawWatermelon(
                    canvas,
                    centerX - 105 * scale,
                    47 * scale,
                    16 * scale
            );

            drawWrappedCandy(
                    canvas,
                    centerX + 105 * scale,
                    48 * scale,
                    15 * scale
            );
        }

        private void drawProgressCard(
                Canvas canvas,
                float width
        ) {

            float left = 20 * scale;
            float right = width - 20 * scale;
            float top = 120 * scale;
            float bottom = 236 * scale;

            drawCard(
                    canvas,
                    left,
                    top,
                    right,
                    bottom,
                    25 * scale
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(15 * scale);
            textPaint.setColor(PURPLE);

            canvas.drawText(
                    "YOUR CANDY JOURNEY",
                    width / 2f,
                    top + 27 * scale,
                    textPaint
            );

            float sectionWidth =
                    (right - left - 24 * scale) / 3f;

            drawStat(
                    canvas,
                    left + 12 * scale,
                    top + 42 * scale,
                    sectionWidth,
                    "LEVEL",
                    "1",
                    PINK
            );

            drawStat(
                    canvas,
                    left + 12 * scale + sectionWidth,
                    top + 42 * scale,
                    sectionWidth,
                    "BEST SCORE",
                    "0",
                    BLUE
            );

            drawStat(
                    canvas,
                    left + 12 * scale + sectionWidth * 2,
                    top + 42 * scale,
                    sectionWidth,
                    "STARS",
                    "0",
                    YELLOW
            );
        }

        private void drawStat(
                Canvas canvas,
                float left,
                float top,
                float width,
                String label,
                String value,
                int color
        ) {

            RectF rect = new RectF(
                    left + 4 * scale,
                    top,
                    left + width - 4 * scale,
                    top + 64 * scale
            );

            paint.setColor(Color.argb(
                    42,
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
            ));

            canvas.drawRoundRect(
                    rect,
                    17 * scale,
                    17 * scale,
                    paint
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            textPaint.setTextSize(22 * scale);
            textPaint.setColor(color);

            canvas.drawText(
                    value,
                    rect.centerX(),
                    rect.top + 29 * scale,
                    textPaint
            );

            textPaint.setTextSize(9 * scale);
            textPaint.setColor(DARK);

            canvas.drawText(
                    label,
                    rect.centerX(),
                    rect.top + 49 * scale,
                    textPaint
            );
        }

        private void drawPlayButton(
                Canvas canvas,
                float width
        ) {

            float left = 25 * scale;
            float right = width - 25 * scale;
            float top = 253 * scale;
            float bottom = 330 * scale;

            playRect.set(
                    left,
                    top,
                    right,
                    bottom
            );

            drawButtonShadow(
                    canvas,
                    left,
                    top,
                    right,
                    bottom,
                    28 * scale
            );

            LinearGradient gradient = new LinearGradient(
                    left,
                    top,
                    right,
                    bottom,
                    Color.rgb(255, 91, 160),
                    DARK_PINK,
                    Shader.TileMode.CLAMP
            );

            paint.setShader(gradient);

            canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    28 * scale,
                    28 * scale,
                    paint
            );

            paint.setShader(null);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(23 * scale);
            textPaint.setColor(WHITE);

            canvas.drawText(
                    "PLAY NOW",
                    width / 2f,
                    top + 47 * scale,
                    textPaint
            );

            // Small candy highlights.
            drawMiniCandy(
                    canvas,
                    left + 43 * scale,
                    top + 38 * scale,
                    9 * scale,
                    YELLOW
            );

            drawMiniCandy(
                    canvas,
                    right - 43 * scale,
                    top + 38 * scale,
                    9 * scale,
                    CYAN
            );
        }

        private void drawMenuButtons(
                Canvas canvas,
                float width
        ) {

            float gap = 10 * scale;
            float left = 20 * scale;
            float right = width - 20 * scale;

            float totalWidth = right - left;
            float buttonWidth = (totalWidth - gap) / 2f;

            float top = 347 * scale;
            float bottom = 411 * scale;

            levelsRect.set(
                    left,
                    top,
                    left + buttonWidth,
                    bottom
            );

            scoresRect.set(
                    left + buttonWidth + gap,
                    top,
                    right,
                    bottom
            );

            drawSmallMenuButton(
                    canvas,
                    levelsRect,
                    "LEVELS",
                    PURPLE,
                    "100"
            );

            drawSmallMenuButton(
                    canvas,
                    scoresRect,
                    "SCORES",
                    BLUE,
                    "BEST"
            );

            settingsRect.set(
                    left,
                    422 * scale,
                    right,
                    477 * scale
            );

            drawWideMenuButton(
                    canvas,
                    settingsRect,
                    "SETTINGS",
                    DARK
            );
        }

        private void drawSmallMenuButton(
                Canvas canvas,
                RectF rect,
                String title,
                int color,
                String smallText
        ) {

            drawButtonShadow(
                    canvas,
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    20 * scale
            );

            paint.setColor(color);

            canvas.drawRoundRect(
                    rect,
                    20 * scale,
                    20 * scale,
                    paint
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setColor(WHITE);

            textPaint.setTextSize(15 * scale);

            canvas.drawText(
                    title,
                    rect.centerX(),
                    rect.top + 27 * scale,
                    textPaint
            );

            textPaint.setTextSize(8 * scale);

            canvas.drawText(
                    smallText,
                    rect.centerX(),
                    rect.top + 45 * scale,
                    textPaint
            );
        }

        private void drawWideMenuButton(
                Canvas canvas,
                RectF rect,
                String title,
                int color
        ) {

            drawButtonShadow(
                    canvas,
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    19 * scale
            );

            paint.setColor(color);

            canvas.drawRoundRect(
                    rect,
                    19 * scale,
                    19 * scale,
                    paint
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(16 * scale);
            textPaint.setColor(WHITE);

            canvas.drawText(
                    "⚙",
                    rect.centerX() - 45 * scale,
                    rect.centerY() + 6 * scale,
                    textPaint
            );

            canvas.drawText(
                    title,
                    rect.centerX() + 15 * scale,
                    rect.centerY() + 6 * scale,
                    textPaint
            );
        }

        private void drawFeatureStrip(
                Canvas canvas,
                float width,
                float height
        ) {

            float left = 20 * scale;
            float right = width - 20 * scale;

            float top = 493 * scale;

            // On smaller phones, keep the feature card inside the display.
            if (top > height - 150) {
                top = height - 150;
            }

            float bottom = Math.min(
                    height - 28 * scale,
                    top + 125 * scale
            );

            drawCard(
                    canvas,
                    left,
                    top,
                    right,
                    bottom,
                    23 * scale
            );

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            textPaint.setTextSize(13 * scale);
            textPaint.setColor(CYAN);

            canvas.drawText(
                    "CANDY POP FEATURES",
                    width / 2f,
                    top + 24 * scale,
                    textPaint
            );

            textPaint.setTextSize(10 * scale);
            textPaint.setColor(DARK);

            canvas.drawText(
                    "100 LEVELS   •   LONG COMBOS",
                    width / 2f,
                    top + 49 * scale,
                    textPaint
            );

            canvas.drawText(
                    "TIME CHALLENGES   •   SPECIAL CANDIES",
                    width / 2f,
                    top + 69 * scale,
                    textPaint
            );

            textPaint.setColor(PINK);

            canvas.drawText(
                    "MORE CANDY ADVENTURES COMING!",
                    width / 2f,
                    top + 98 * scale,
                    textPaint
            );
        }

        private void drawCard(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                float radius
        ) {

            // Shadow.
            paint.setColor(Color.argb(
                    35,
                    70,
                    35,
                    100
            ));

            canvas.drawRoundRect(
                    left,
                    top + 5 * scale,
                    right,
                    bottom + 5 * scale,
                    radius,
                    radius,
                    paint
            );

            LinearGradient gradient = new LinearGradient(
                    left,
                    top,
                    right,
                    bottom,
                    Color.argb(245, 255, 255, 255),
                    Color.argb(220, 250, 239, 255),
                    Shader.TileMode.CLAMP
            );

            paint.setShader(gradient);

            canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    bottom,
                    radius,
                    radius,
                    paint
            );

            paint.setShader(null);
        }

        private void drawButtonShadow(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                float radius
        ) {

            paint.setColor(Color.argb(
                    55,
                    75,
                    30,
                    100
            ));

            canvas.drawRoundRect(
                    left,
                    top + 5 * scale,
                    right,
                    bottom + 5 * scale,
                    radius,
                    radius,
                    paint
            );
        }

        // ---------------------------------------------------------
        // REALISTIC CANDY ARTWORK
        // ---------------------------------------------------------

        private void drawMiniCandy(
                Canvas canvas,
                float x,
                float y,
                float r,
                int color
        ) {

            paint.setShader(
                    new RadialGradient(
                            x - r * 0.35f,
                            y - r * 0.35f,
                            r,
                            Color.WHITE,
                            color,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawCircle(
                    x,
                    y,
                    r,
                    paint
            );

            paint.setShader(null);
        }

        private void drawStrawberry(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            paint.setColor(Color.rgb(225, 42, 72));

            path.reset();

            path.moveTo(x, y - size);
            path.cubicTo(
                    x - size * 1.25f,
                    y - size * 0.75f,
                    x - size * 1.15f,
                    y + size * 0.9f,
                    x,
                    y + size * 1.15f
            );

            path.cubicTo(
                    x + size * 1.15f,
                    y + size * 0.9f,
                    x + size * 1.25f,
                    y - size * 0.75f,
                    x,
                    y - size
            );

            canvas.drawPath(path, paint);

            paint.setColor(Color.rgb(48, 176, 75));

            path.reset();
            path.moveTo(x, y - size);
            path.lineTo(x - size * 0.65f, y - size * 1.45f);
            path.lineTo(x - size * 0.12f, y - size * 1.25f);
            path.lineTo(x, y - size * 1.55f);
            path.lineTo(x + size * 0.25f, y - size * 1.25f);
            path.lineTo(x + size * 0.75f, y - size * 1.42f);
            path.close();

            canvas.drawPath(path, paint);

            paint.setColor(Color.argb(
                    210,
                    255,
                    235,
                    235
            ));

            canvas.drawCircle(
                    x - size * 0.38f,
                    y - size * 0.25f,
                    size * 0.11f,
                    paint
            );

            canvas.drawCircle(
                    x - size * 0.55f,
                    y + size * 0.18f,
                    size * 0.07f,
                    paint
            );
        }

        private void drawWrappedCandy(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            float w = size * 2.3f;
            float h = size * 1.25f;

            paint.setColor(PINK);

            path.reset();

            path.moveTo(x - w, y);
            path.lineTo(x - w * 0.65f, y - h * 0.65f);
            path.lineTo(x - w * 0.48f, y - h * 0.25f);
            path.lineTo(x - w * 0.35f, y + h * 0.25f);
            path.lineTo(x - w * 0.65f, y + h * 0.65f);
            path.close();

            canvas.drawPath(path, paint);

            path.reset();

            path.moveTo(x + w, y);
            path.lineTo(x + w * 0.65f, y - h * 0.65f);
            path.lineTo(x + w * 0.48f, y - h * 0.25f);
            path.lineTo(x + w * 0.35f, y + h * 0.25f);
            path.lineTo(x + w * 0.65f, y + h * 0.65f);
            path.close();

            canvas.drawPath(path, paint);

            paint.setShader(
                    new LinearGradient(
                            x - size,
                            y - h,
                            x + size,
                            y + h,
                            Color.rgb(255, 102, 174),
                            Color.rgb(185, 42, 120),
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawRoundRect(
                    x - size,
                    y - h,
                    x + size,
                    y + h,
                    size * 0.35f,
                    size * 0.35f,
                    paint
            );

            paint.setShader(null);

            paint.setColor(Color.argb(
                    200,
                    255,
                    255,
                    255
            ));

            canvas.drawRoundRect(
                    x - size * 0.55f,
                    y - h * 0.55f,
                    x - size * 0.15f,
                    y - h * 0.32f,
                    size * 0.1f,
                    size * 0.1f,
                    paint
            );
        }

        private void drawLollipop(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            paint.setStrokeWidth(size * 0.22f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.WHITE);

            canvas.drawLine(
                    x,
                    y + size * 0.75f,
                    x,
                    y + size * 2.2f,
                    paint
            );

            paint.setShader(
                    new RadialGradient(
                            x - size * 0.35f,
                            y - size * 0.35f,
                            size,
                            Color.WHITE,
                            PINK,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawCircle(
                    x,
                    y,
                    size,
                    paint
            );

            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.16f);
            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    x,
                    y,
                    size * 0.58f,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);
        }

        private void drawApple(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            paint.setShader(
                    new RadialGradient(
                            x - size * 0.35f,
                            y - size * 0.45f,
                            size * 1.3f,
                            Color.rgb(255, 125, 125),
                            Color.rgb(205, 32, 55),
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawCircle(
                    x - size * 0.38f,
                    y,
                    size * 0.72f,
                    paint
            );

            canvas.drawCircle(
                    x + size * 0.38f,
                    y,
                    size * 0.72f,
                    paint
            );

            paint.setShader(null);

            paint.setColor(Color.rgb(80, 48, 30));

            canvas.drawRect(
                    x - size * 0.08f,
                    y - size * 0.95f,
                    x + size * 0.08f,
                    y - size * 0.45f,
                    paint
            );

            paint.setColor(Color.rgb(52, 175, 73));

            path.reset();
            path.moveTo(x, y - size * 0.8f);
            path.quadTo(
                    x + size * 0.9f,
                    y - size * 1.15f,
                    x + size * 0.65f,
                    y - size * 0.35f
            );

            path.quadTo(
                    x + size * 0.3f,
                    y - size * 0.55f,
                    x,
                    y - size * 0.8f
            );

            canvas.drawPath(path, paint);
        }

        private void drawGrapes(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            int grapeColor = Color.rgb(112, 65, 190);

            for (int row = 0; row < 4; row++) {

                int count = row + 1;

                for (int col = 0; col < count; col++) {

                    float gx =
                            x + (col - row / 2f) * size * 0.75f;

                    float gy =
                            y + row * size * 0.65f;

                    paint.setShader(
                            new RadialGradient(
                                    gx - size * 0.2f,
                                    gy - size * 0.25f,
                                    size * 0.65f,
                                    Color.rgb(190, 135, 245),
                                    grapeColor,
                                    Shader.TileMode.CLAMP
                            )
                    );

                    canvas.drawCircle(
                            gx,
                            gy,
                            size * 0.42f,
                            paint
                    );

                    paint.setShader(null);
                }
            }

            paint.setColor(Color.rgb(50, 170, 75));

            canvas.drawOval(
                    x - size * 0.55f,
                    y - size * 0.7f,
                    x + size * 0.15f,
                    y - size * 0.15f,
                    paint
            );
        }

        private void drawWatermelon(
                Canvas canvas,
                float x,
                float y,
                float size
        ) {

            paint.setColor(Color.rgb(56, 181, 83));

            canvas.drawCircle(
                    x,
                    y,
                    size,
                    paint
            );

            paint.setColor(Color.rgb(255, 92, 117));

            canvas.drawCircle(
                    x,
                    y,
                    size * 0.72f,
                    paint
            );

            paint.setColor(Color.argb(
                    220,
                    255,
                    255,
                    255
            ));

            canvas.drawCircle(
                    x - size * 0.28f,
                    y - size * 0.25f,
                    size * 0.12f,
                    paint
            );

            paint.setColor(Color.rgb(45, 45, 45));

            canvas.drawCircle(
                    x - size * 0.25f,
                    y,
                    size * 0.07f,
                    paint
            );

            canvas.drawCircle(
                    x + size * 0.20f,
                    y - size * 0.15f,
                    size * 0.07f,
                    paint
            );

            canvas.drawCircle(
                    x + size * 0.18f,
                    y + size * 0.25f,
                    size * 0.07f,
                    paint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    pressedX = x;
                    pressedY = y;

                    return true;

                case MotionEvent.ACTION_UP:

                    if (playRect.contains(x, y)) {

                        startGame();
                        return true;
                    }

                    if (levelsRect.contains(x, y)) {

                        showMessage(
                                "Levels",
                                "More levels are coming soon!"
                        );

                        return true;
                    }

                    if (scoresRect.contains(x, y)) {

                        showMessage(
                                "Scores",
                                "Your high scores will appear here."
                        );

                        return true;
                    }

                    if (settingsRect.contains(x, y)) {

                        showMessage(
                                "Settings",
                                "Sound, music and vibration will be added here."
                        );

                        return true;
                    }

                    return true;
            }

            return true;
        }
    }
}