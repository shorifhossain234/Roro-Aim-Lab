package com.roro.aimlab;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

public class AimOverlayService extends Service {
    private WindowManager windowManager;
    private AimView aimView;

    public static float[] strikerPos = null;
    public static float[][] pocketPositions = null;
    public static float[][] gutiBalls = null;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        aimView = new AimView(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(aimView, params);
    }

    public static void updateAndDraw(Context context) {
        if (instance != null && instance.aimView != null) {
            instance.aimView.postInvalidate();
        }
    }

    private static AimOverlayService instance;

    public static void start(Context context) {
        context.startService(new Intent(context, AimOverlayService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, AimOverlayService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (aimView != null) windowManager.removeView(aimView);
    }

    static class AimView extends View {
        Paint linePaint, circlePaint, strikerPaint;

        public AimView(Context context) {
            super(context);
            linePaint = new Paint();
            linePaint.setColor(Color.YELLOW);
            linePaint.setStrokeWidth(4f);
            linePaint.setAntiAlias(true);

            circlePaint = new Paint();
            circlePaint.setColor(Color.GREEN);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(3f);

            strikerPaint = new Paint();
            strikerPaint.setColor(Color.RED);
            strikerPaint.setStyle(Paint.Style.STROKE);
            strikerPaint.setStrokeWidth(4f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (strikerPos == null || gutiBalls == null || pocketPositions == null) return;

            // Striker আঁকো
            canvas.drawCircle(strikerPos[0], strikerPos[1], 30, strikerPaint);

            // প্রতিটা গুটির জন্য সবচেয়ে কাছের pocket-এ লাইন আঁকো
            for (float[] guti : gutiBalls) {
                float bestDist = Float.MAX_VALUE;
                float[] bestPocket = null;

                for (float[] pocket : pocketPositions) {
                    float dist = (float) Math.sqrt(
                        Math.pow(guti[0] - pocket[0], 2) +
                        Math.pow(guti[1] - pocket[1], 2));
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestPocket = pocket;
                    }
                }

                if (bestPocket != null) {
                    // গুটি থেকে pocket-এ লাইন
                    canvas.drawLine(guti[0], guti[1], bestPocket[0], bestPocket[1], linePaint);
                    // striker থেকে গুটিতে লাইন
                    canvas.drawLine(strikerPos[0], strikerPos[1], guti[0], guti[1], linePaint);
                    // গুটির চারদিকে circle
                    canvas.drawCircle(guti[0], guti[1], 20, circlePaint);
                }
            }
        }
    }
}
