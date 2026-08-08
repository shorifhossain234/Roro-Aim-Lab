package com.roro.aimlab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private PowerManager.WakeLock wakeLock;
    private int screenWidth, screenHeight, screenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoroAimLab:CaptureLock");
        wakeLock.acquire(10*60*1000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startForeground(1, buildNotification());

            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data = intent.getParcelableExtra("data", Intent.class);
            } else {
                data = intent.getParcelableExtra("data");
            }

            if (resultCode == -1 || data == null) {
                Log.e(TAG, "Invalid resultCode or data");
                stopSelf();
                return START_NOT_STICKY;
            }

            MediaProjectionManager mgr = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mgr.getMediaProjection(resultCode, data);

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null");
                stopSelf();
                return START_NOT_STICKY;
            }

            imageReader = ImageReader.newInstance(
                    screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "RoroAimLabCapture",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);

            imageReader.setOnImageAvailableListener(reader -> {
                android.media.Image image = reader.acquireLatestImage();
                if (image != null) {
                    processFrame(image);
                    image.close();
                }
            }, new Handler(Looper.getMainLooper()));

            Log.d(TAG, "Screen capture started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting capture: " + e.getMessage());
            stopSelf();
        }

        return START_STICKY;
    }

    private void processFrame(android.media.Image image) {
        // পরের ধাপে OpenCV দিয়ে গুটি খুঁজব
    }

    private Notification buildNotification() {
        String channelId = "roro_capture_channel";
        NotificationChannel channel = new NotificationChannel(
                channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);

        return new Notification.Builder(this, channelId)
                .setContentTitle("Roro Aim Lab")
                .setContentText("Aim assist active ✅")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
