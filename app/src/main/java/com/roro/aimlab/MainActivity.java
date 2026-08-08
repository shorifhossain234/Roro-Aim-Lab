package com.roro.aimlab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Roro Aim Lab - Ready!");
        tv.setTextSize(24);
        tv.setPadding(40, 100, 40, 40);
        setContentView(tv);
    }
}
