package com.roro.aimlab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 60, 30, 30);

        TextView tv = new TextView(this);
        tv.setTextSize(16);

        try {
            int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
            if (result == ConnectionResult.SUCCESS) {
                tv.setText("Google Play Services: AVAILABLE ✅\n\nAdMob should work fine.");
            } else {
                tv.setText("Google Play Services: NOT AVAILABLE ❌\n\nError code: " + result + "\n\nThis is why AdMob crashes.");
            }
        } catch (Throwable t) {
            tv.setText("Error checking Play Services:\n\n" + t.toString());
        }

        layout.addView(tv);
        setContentView(layout);
    }
}
