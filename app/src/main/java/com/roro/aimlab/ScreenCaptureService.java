package com.roro.aimlab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private int screenWidth, screenHeight, screenDensity;
    private int frameCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        notificationManager = getSystemService(NotificationManager.class);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoroAimLab:CaptureLock");
        wakeLock.acquire(10*60*1000L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startForeground(1, buildNotification("Starting capture..."));

            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data = intent.getParcelableExtra("data", Intent.class);
            } else {
                data = intent.getParcelableExtra("data");
            }

            MediaProjectionManager mgr = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mgr.getMediaProjection(resultCode, data);

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
                    frameCount++;
                    if (frameCount % 30 == 0) {
                        updateNotification("Capturing... frames: " + frameCount);
                    }
                    processFrame(image);
                    image.close();
                }
            }, new Handler(Looper.getMainLooper()));

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            stopSelf();
        }

        return START_STICKY;
    }

    private void processFrame(android.media.Image image) {
        // Phase 3: এখানে OpenCV দিয়ে গুটি খুঁজব
        // এখন শুধু frame count করছি
    }

    private void updateNotification(String text) {
        notificationManager.notify(1, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        String channelId = "roro_capture_channel";
        NotificationChannel channel = new NotificationChannel(
                channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);

        return new Notification.Builder(this, channelId)
                .setContentTitle("Roro Aim Lab ✅")
                .setContentText(text)
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
