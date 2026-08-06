package com.roro.aimlab;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, initializationStatus -> {});

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

        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-3071340264379141/1648765767");
        layout.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());

        setContentView(layout);
    }
}
