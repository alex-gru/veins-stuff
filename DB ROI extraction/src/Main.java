import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    //    private static final String inFilesDir = "E:\\Bak\\processing folder ROI\\";
    private static final String inFilesDir = "E:\\Bak\\processing folder ROI\\auflicht old\\";
    private static final String houghCirclesResultDir = inFilesDir + "houghcircles/";
    private static final String roiResultDir = houghCirclesResultDir + "roi/";

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        testHoughCircleDetectionOnDir();
    }

    public static void testHoughCircleDetectionOnDir() {
        File imageDir = new File(inFilesDir);

        FilenameFilter fileNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };

        Mat inImageGray = null;
        Mat inImage = null;
        Mat inImagePaintable = null;
        Mat circles;
        Mat roi = null;
        Point posCircle;
        Point centerOfFocusArea;
        File circlesFile;
        File roiRectangleFile;
        File roiFile;
        List<Point> circlePoints;
        double[] currentCircleCoords;
        String circlesFileName;
        Point start;
        Point end;
        String currFileInName;
        String roiRectangleFileName;
        String roiFileName;

        for (final File inFile : imageDir.listFiles(fileNameFilter)) {
            currFileInName = inFile.getName();
            System.out.println(currFileInName);

            inImageGray = Highgui.imread(inFile.toString(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
            inImage = Highgui.imread(inFile.toString());

            inImagePaintable = new Mat(inImage.rows(), inImage.cols(), inImage.type());
            inImage.copyTo(inImagePaintable);

            System.out.println("inImageGray size: " + inImageGray.size());

            circles = new Mat(inImageGray.rows(), inImageGray.cols(), inImageGray.type());

            //auflicht settings
            double canny1 = 10;
            double canny2 = 10;
            double dp = 1;
            double minDistance = 300;
            int minRadius = 20;
            int maxRadius = 60;

            //special
//            double canny1 = 15;
//            double canny2 = 10;
//            double dp = 1;
//            double minDistance = 150;
//            int minRadius = 20;
//            int maxRadius = 50;
//            Imgproc.Canny(inImageGray, inImageGray, canny1, canny2);
//            Imgproc.GaussianBlur(inImageGray,inImageGray,new Size(3,3),3);
            //end special


            Imgproc.HoughCircles(inImageGray, circles, Imgproc.CV_HOUGH_GRADIENT, dp, minDistance, canny1, canny2, minRadius, maxRadius);
            System.out.println("#circles: " + circles.cols());
            System.out.println("size: " + circles.size());

            circlePoints = new ArrayList<Point>();
            if (circles.cols() > 0) {
                for (int i = 0; i < circles.cols(); i++) {
                    currentCircleCoords = circles.get(0, i);
                    if (currentCircleCoords == null) {
                        continue;
                    }
                    posCircle = new Point(currentCircleCoords[0], currentCircleCoords[1]);

                    int centerOfFocusArea_X = 2155;
                    int centerOfFocusArea_Y = 1100;
                    centerOfFocusArea = new Point(centerOfFocusArea_X, centerOfFocusArea_Y);

                    final int MAX_DIFF_X = 400;
                    final int MAX_DIFF_Y = 100;
                    if (posCircle.x < centerOfFocusArea.x + MAX_DIFF_X
                            && posCircle.x > centerOfFocusArea.x - MAX_DIFF_X
                            && posCircle.y < centerOfFocusArea.y + MAX_DIFF_Y
                            && posCircle.y > centerOfFocusArea.y - MAX_DIFF_Y
//                            && posCircle.x > centerOfFocusArea_X - 300
                            ) {
                        circlePoints.add(posCircle);
                        Core.circle(inImagePaintable, posCircle, (int) currentCircleCoords[2], new Scalar(0, 255, 0), 20);
                    }
                }

                circlesFileName = currFileInName.replace(".", "_houghCircles.");

                if (circlePoints.size() != 2) {
                    System.err.println("Did not find the expected number of circles: " + circlePoints.size());
                    circlesFile = new File(houghCirclesResultDir + "failed/" + circlesFileName);
                    Highgui.imwrite(circlesFile.toString(), inImagePaintable);
                } else {
                    circlesFile = new File(houghCirclesResultDir + circlesFileName);
                    Highgui.imwrite(circlesFile.toString(), inImagePaintable);

                    roiRectangleFileName = currFileInName.replace(".", "_roiRectangle.");
                    roiRectangleFile = new File(roiResultDir + roiRectangleFileName);
                    roiFileName = currFileInName.replace(".", "_roi.");
                    roiFile = new File(roiResultDir + roiFileName);

                    if (circlePoints.get(0).x < circlePoints.get(1).x) {
                        start = circlePoints.get(0);
                    } else {
                        start = circlePoints.get(1);
                    }
                    start.x -= 100;
                    start.y += 300;
                    end = new Point(start.x + 700, start.y + 700);
                    Core.rectangle(inImagePaintable, start, end, new Scalar(0, 0, 255), 10);
                    Highgui.imwrite(roiRectangleFile.toString(), inImagePaintable);
                    roi = inImage.submat(new Rect(start, end));
                    Highgui.imwrite(roiFile.toString(), roi);
                }
            }

            try {
                inImageGray.release();
                inImage.release();
                inImagePaintable.release();
                circles.release();
                roi.release();
            } catch (NullPointerException e) {
            }
        }
    }
}
