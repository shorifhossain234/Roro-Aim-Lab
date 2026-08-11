package com.roro.aimlab;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class PieceDetector {

    private static final Scalar YELLOW_LOWER = new Scalar(20, 150, 80);
    private static final Scalar YELLOW_UPPER = new Scalar(35, 255, 255);

    private static final Scalar RED_LOWER_1 = new Scalar(0, 150, 130);
    private static final Scalar RED_UPPER_1 = new Scalar(8, 255, 255);
    private static final Scalar RED_LOWER_2 = new Scalar(172, 150, 130);
    private static final Scalar RED_UPPER_2 = new Scalar(179, 255, 255);

    private static final double MIN_PIECE_AREA = 80;
    private static final double MAX_PIECE_AREA = 4000;

    public static class Piece {
        public final Point center;
        public final double radius;
        public Piece(Point center, double radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    public List<Piece> detectOpponentPieces(Mat hsvFrame) {
        Mat mask = new Mat();
        Core.inRange(hsvFrame, YELLOW_LOWER, YELLOW_UPPER, mask);
        List<Piece> pieces = findCirclesInMask(mask);
        mask.release();
        return pieces;
    }

    public List<Piece> detectQueen(Mat hsvFrame) {
        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Mat mask = new Mat();
        Core.inRange(hsvFrame, RED_LOWER_1, RED_UPPER_1, mask1);
        Core.inRange(hsvFrame, RED_LOWER_2, RED_UPPER_2, mask2);
        Core.bitwise_or(mask1, mask2, mask);
        List<Piece> pieces = findCirclesInMask(mask);
        mask1.release();
        mask2.release();
        mask.release();
        return pieces;
    }

    public List<Piece> detectOwnPieces(Mat grayFrame, Mat hsvFrame) {
        List<Piece> allCircles = houghCircles(grayFrame);
        List<Piece> yellow = detectOpponentPieces(hsvFrame);
        List<Piece> red = detectQueen(hsvFrame);

        List<Piece> own = new ArrayList<>();
        for (Piece c : allCircles) {
            if (!overlapsAny(c, yellow) && !overlapsAny(c, red)) {
                own.add(c);
            }
        }
        return own;
    }

    public Piece detectStriker(Mat grayFrame, Rect strikerTrackRoi) {
        Mat roi = new Mat(grayFrame, strikerTrackRoi);
        List<Piece> circles = houghCircles(roi);
        roi.release();
        if (circles.isEmpty()) return null;

        Piece largest = circles.get(0);
        for (Piece p : circles) {
            if (p.radius > largest.radius) largest = p;
        }
        return new Piece(
                new Point(largest.center.x + strikerTrackRoi.x, largest.center.y + strikerTrackRoi.y),
                largest.radius
        );
    }

    private List<Piece> findCirclesInMask(Mat mask) {
        List<Piece> results = new ArrayList<>();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area < MIN_PIECE_AREA || area > MAX_PIECE_AREA) continue;

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            Point center = new Point();
            float[] radius = new float[1];
            Imgproc.minEnclosingCircle(contour2f, center, radius);
            contour2f.release();

            double circleArea = Math.PI * radius[0] * radius[0];
            if (circleArea == 0 || area / circleArea < 0.65) continue;

            results.add(new Piece(center, radius[0]));
        }
        hierarchy.release();
        for (MatOfPoint c : contours) c.release();
        return results;
    }

    private List<Piece> houghCircles(Mat grayFrame) {
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(grayFrame, blurred, new Size(5, 5), 2, 2);

        Mat circles = new Mat();
        Imgproc.HoughCircles(
                blurred, circles, Imgproc.HOUGH_GRADIENT,
                1.0, 20, 100, 25, 8, 30
        );
        blurred.release();

        List<Piece> results = new ArrayList<>();
        for (int i = 0; i < circles.cols(); i++) {
            double[] c = circles.get(0, i);
            if (c == null) continue;
            results.add(new Piece(new Point(c[0], c[1]), c[2]));
        }
        circles.release();
        return results;
    }

    private boolean overlapsAny(Piece p, List<Piece> others) {
        for (Piece o : others) {
            double dx = p.center.x - o.center.x;
            double dy = p.center.y - o.center.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < (p.radius + o.radius) * 0.6) return true;
        }
        return false;
    }
}
