package com.roro.aimlab;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File crashFile = new File(getFilesDir(), "last_crash.txt");
        if (crashFile.exists()) {
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(crashFile)));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                TextView errorView = new TextView(this);
                errorView.setText("CRASH LOG:\n\n" + sb.toString());
                errorView.setTextSize(12);
                errorView.setPadding(30, 60, 30, 30);
                errorView.setTextIsSelectable(true);

                ScrollView scrollView = new ScrollView(this);
                scrollView.addView(errorView);
                setContentView(scrollView);

                crashFile.delete();
                return;
            } catch (Exception e) {
                // fall through to normal UI
            }
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 100, 40, 40);

        TextView title = new TextView(this);
        title.setText("Roro Aim Lab");
        title.setTextSize(24);
        layout.addView(title);

        Button permissionBtn = new Button(this);
        permissionBtn.setText("Grant Overlay Permission");
        permissionBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        layout.addView(permissionBtn);

        Button startBtn = new Button(this);
        startBtn.setText("Start Floating Button");
        startBtn.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, FloatingButtonService.class);
            startService(serviceIntent);
        });
        layout.addView(startBtn);

        setContentView(layout);
    }
}
