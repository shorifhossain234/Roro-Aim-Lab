package com.roro.aimlab;

import android.app.Service;
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
    private View overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayView = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(5f);
                paint.setStyle(Paint.Style.STROKE);
                // Test: একটা লাল X আঁকো স্ক্রিনের মাঝখানে
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                canvas.drawLine(cx - 50, cy - 50, cx + 50, cy + 50, paint);
                canvas.drawLine(cx + 50, cy - 50, cx - 50, cy + 50, paint);
                canvas.drawCircle(cx, cy, 30, paint);
            }
        };

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);
        overlayView.invalidate();
    }

    public static void start(android.content.Context context) {
        context.startService(new Intent(context, AimOverlayService.class));
    }

    public static void stop(android.content.Context context) {
        context.stopService(new Intent(context, AimOverlayService.class));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) windowManager.removeView(overlayView);
    }
}
