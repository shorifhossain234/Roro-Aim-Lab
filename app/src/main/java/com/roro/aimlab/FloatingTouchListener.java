package com.roro.aimlab;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingTouchListener implements View.OnTouchListener {

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final View view;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    public FloatingTouchListener(WindowManager.LayoutParams params, WindowManager windowManager, View view) {
        this.params = params;
        this.windowManager = windowManager;
        this.view = view;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(view, params);
                return true;
        }
        return false;
    }
}
