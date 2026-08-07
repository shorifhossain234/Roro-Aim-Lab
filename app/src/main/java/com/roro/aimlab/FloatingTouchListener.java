package com.roro.aimlab;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingTouchListener implements View.OnTouchListener {

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final View view;
    private final Context context;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private long touchStartTime;

    private static final int CLICK_DRAG_TOLERANCE = 10;
    private static final long CLICK_MAX_DURATION = 200;

    public FloatingTouchListener(WindowManager.LayoutParams params, WindowManager windowManager, View view, Context context) {
        this.params = params;
        this.windowManager = windowManager;
        this.view = view;
        this.context = context;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                touchStartTime = System.currentTimeMillis();
                return true;

            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(view, params);
                return true;

            case MotionEvent.ACTION_UP:
                float dx = Math.abs(event.getRawX() - initialTouchX);
                float dy = Math.abs(event.getRawY() - initialTouchY);
                long duration = System.currentTimeMillis() - touchStartTime;

                if (dx < CLICK_DRAG_TOLERANCE && dy < CLICK_DRAG_TOLERANCE && duration < CLICK_MAX_DURATION) {
                    Intent intent = new Intent(context, CaptureRequestActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                return true;
        }
        return false;
    }
}
