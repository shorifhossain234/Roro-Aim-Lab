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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private Handler mainHandler;
    private boolean opencvLoaded = false;
    private long lastProcessTime = 0;
    private int imgW, imgH;

    // বোর্ড এলাকা (720x1600 ফোনে)
    // উপরে ৩৭% খালি, নিচে ২৭% খালি
    private int boardTop, boardBottom, boardLeft, boardRight;

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imgW = metrics.widthPixels;
        imgH = metrics.heightPixels;

        // বোর্ড এলাকা calculate
        boardTop = (int)(imgH * 0.37);
        boardBottom = (int)(imgH * 0.73);
        boardLeft = (int)(imgW * 0.04);
        boardRight = (int)(imgW * 0.96);

        notificationManager = getSystemService(NotificationManager.class);
        mainHandler = new Handler(Looper.getMainLooper());
        opencvLoaded = OpenCVLoader.initLocal();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoroAimLab:Lock");
        wakeLock.acquire(10*60*1000L);

        // 4 pocket (বোর্ডের ৪ কোণায়)
        AimOverlayService.pocketPositions = new float[][]{
            {boardLeft  + imgW * 0.02f, boardTop    + imgH * 0.02f},
            {boardRight - imgW * 0.02f, boardTop    + imgH * 0.02f},
            {boardLeft  + imgW * 0.02f, boardBottom - imgH * 0.02f},
            {boardRight - imgW * 0.02f, boardBottom - imgH * 0.02f}
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("🔍 Scanning board..."));

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

        imageReader = ImageReader.newInstance(imgW, imgH, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("RoroCapture",
                imgW, imgH, getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        imageReader.setOnImageAvailableListener(reader -> {
            android.media.Image image = reader.acquireLatestImage();
            if (image != null) {
                long now = System.currentTimeMillis();
                if (opencvLoaded && now - lastProcessTime > 300) {
                    lastProcessTime = now;
                    processFrame(image);
                }
                image.close();
            }
        }, mainHandler);

        return START_STICKY;
    }

    private void processFrame(android.media.Image image) {
        try {
            android.media.Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int rowStride = planes[0].getRowStride();
            int pixelStride = planes[0].getPixelStride();
            int rowPadding = rowStride - pixelStride * imgW;

            Bitmap bmp = Bitmap.createBitmap(
                imgW + rowPadding/pixelStride, imgH, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buffer);
            bmp = Bitmap.createBitmap(bmp, 0, 0, imgW, imgH);

            Mat rgba = new Mat();
            Utils.bitmapToMat(bmp, rgba);
            bmp.recycle();

            // HSV convert
            Mat hsv = new Mat();
            Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2RGB);
            Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

            // বোর্ড এলাকা crop করো
            Mat board = hsv.submat(boardTop, boardBottom, boardLeft, boardRight);

            // সাদা গুটি (তোমার গুটি)
            Mat whiteMask = new Mat();
            Core.inRange(board, new Scalar(0, 0, 170), new Scalar(180, 50, 255), whiteMask);

            // হলুদ গুটি (প্রতিপক্ষ) — হলুদের ভেতর কালো চিহ্ন
            Mat yellowMask = new Mat();
            Core.inRange(board, new Scalar(15, 70, 70), new Scalar(38, 255, 255), yellowMask);

            // Striker area — নিচের ২০% (৭৩%-৯৩% এর মধ্যে)
            int strikerTop = (int)(imgH * 0.73);
            int strikerBottom = (int)(imgH * 0.93);
            Mat strikerArea = hsv.submat(strikerTop, strikerBottom, boardLeft, boardRight);

            // বেগুনি striker
            Mat strikerMask = new Mat();
            Core.inRange(strikerArea,
                new Scalar(125, 40, 40),
                new Scalar(165, 255, 255),
                strikerMask);

            // Circle detect
            Mat wC = new Mat(), yC = new Mat(), sC = new Mat();
            Imgproc.HoughCircles(whiteMask, wC, Imgproc.HOUGH_GRADIENT,
                1.2, 22, 40, 10, 8, 25);
            Imgproc.HoughCircles(yellowMask, yC, Imgproc.HOUGH_GRADIENT,
                1.2, 22, 40, 10, 8, 25);
            Imgproc.HoughCircles(strikerMask, sC, Imgproc.HOUGH_GRADIENT,
                1.2, 30, 40, 10, 12, 30);

            List<float[]> gutis = new ArrayList<>();
            float[] striker = null;

            // Striker position (offset যোগ করো)
            if (sC.cols() > 0) {
                double[] c = sC.get(0, 0);
                striker = new float[]{
                    (float)c[0] + boardLeft,
                    (float)c[1] + strikerTop,
                    (float)c[2]
                };
            }

            // সাদা গুটি
            for (int i = 0; i < wC.cols(); i++) {
                double[] c = wC.get(0, i);
                gutis.add(new float[]{
                    (float)c[0] + boardLeft,
                    (float)c[1] + boardTop,
                    (float)c[2]
                });
            }

            // হলুদ গুটি
            for (int i = 0; i < yC.cols(); i++) {
                double[] c = yC.get(0, i);
                gutis.add(new float[]{
                    (float)c[0] + boardLeft,
                    (float)c[1] + boardTop,
                    (float)c[2]
                });
            }

            // Striker না পেলে fallback
            if (striker == null) {
                striker = new float[]{imgW / 2f, imgH * 0.83f, 18f};
            }

            String status;
            if (!gutis.isEmpty()) {
                AimOverlayService.strikerPos = striker;
                AimOverlayService.gutiBalls = gutis.toArray(new float[0][]);
                AimOverlayService.updateAndDraw(this);
                status = "✅ W:" + wC.cols() + " Y:" + yC.cols() +
                         " S:" + (sC.cols() > 0 ? "✓" : "x");
            } else {
                status = "❌ W:" + wC.cols() + " Y:" + yC.cols() +
                         " S:" + sC.cols() + " (adjusting...)";
            }

            updateNotification(status);

            rgba.release(); hsv.release(); board.release();
            whiteMask.release(); yellowMask.release();
            strikerArea.release(); strikerMask.release();
            wC.release(); yC.release(); sC.release();

        } catch (Exception e) {
            updateNotification("Err: " + e.getMessage());
        }
    }

    private void updateNotification(String text) {
        notificationManager.notify(1, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        String ch = "roro_ch";
        notificationManager.createNotificationChannel(
            new NotificationChannel(ch, "Roro", NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, ch)
            .setContentTitle("Roro Aim Lab")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true).build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
