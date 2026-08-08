package com.roro.aimlab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(this);
        tv.setText("Hello Roro + AdMob (safe)");
        tv.setTextSize(24);
        layout.addView(tv);

        try {
            com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});
            com.google.android.gms.ads.AdView adView = new com.google.android.gms.ads.AdView(this);
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
            adView.setAdUnitId("ca-app-pub-3071340264379141/1648765767");
            layout.addView(adView);
            adView.loadAd(new com.google.android.gms.ads.AdRequest.Builder().build());
        } catch (Throwable t) {
            TextView errorTv = new TextView(this);
            errorTv.setText("Ad failed: " + t.getMessage());
            errorTv.setTextSize(12);
            layout.addView(errorTv);
        }

        setContentView(layout);
    }
}
