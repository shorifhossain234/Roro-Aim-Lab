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
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private int screenWidth, screenHeight, screenDensity;
    private Handler mainHandler;
    private boolean opencvLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        notificationManager = getSystemService(NotificationManager.class);
        mainHandler = new Handler(Looper.getMainLooper());
        opencvLoaded = OpenCVLoader.initLocal();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoroAimLab:CaptureLock");
        wakeLock.acquire(10*60*1000L);

        // Pocket positions (4 কোণ, percentage-based)
        AimOverlayService.pocketPositions = new float[][] {
            {screenWidth * 0.05f, screenHeight * 0.15f},
            {screenWidth * 0.95f, screenHeight * 0.15f},
            {screenWidth * 0.05f, screenHeight * 0.85f},
            {screenWidth * 0.95f, screenHeight * 0.85f}
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("Starting..."));

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
                if (opencvLoaded) {
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
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);

            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            bitmap.recycle();

            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY);

            // Board area: মাঝের 70% অংশ
            int boardTop = (int)(screenHeight * 0.15);
            int boardBottom = (int)(screenHeight * 0.85);
            int boardLeft = (int)(screenWidth * 0.05);
            int boardRight = (int)(screenWidth * 0.95);

            Mat boardGray = gray.submat(boardTop, boardBottom, boardLeft, boardRight);

            // Circle detection
            Mat circles = new Mat();
            Imgproc.HoughCircles(boardGray, circles, Imgproc.HOUGH_GRADIENT,
                1.0, 30, 50, 25, 15, 40);

            List<float[]> gutis = new ArrayList<>();
            float[] striker = null;
            float maxRadius = 0;

            if (circles.cols() > 0) {
                for (int i = 0; i < circles.cols(); i++) {
                    double[] c = circles.get(0, i);
                    float x = (float)(c[0] + boardLeft);
                    float y = (float)(c[1] + boardTop);
                    float r = (float)c[2];

                    // সবচেয়ে বড় circle = striker
                    if (r > maxRadius) {
                        maxRadius = r;
                        striker = new float[]{x, y, r};
                    } else {
                        gutis.add(new float[]{x, y, r});
                    }
                }
            }

            if (striker != null) {
                AimOverlayService.strikerPos = striker;
                AimOverlayService.gutiBalls = gutis.isEmpty() ? null :
                    gutis.toArray(new float[0][]);
                AimOverlayService.updateAndDraw(this);
                updateNotification("Detected: striker + " + gutis.size() + " gutis");
            } else {
                updateNotification("Searching for pieces...");
            }

            mat.release();
            gray.release();
            boardGray.release();
            circles.release();

        } catch (Exception e) {
            updateNotification("Error: " + e.getMessage());
        }
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
