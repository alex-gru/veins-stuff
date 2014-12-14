import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    //    private static final String inFilesDir = "E:\\Bak\\processing folder ROI\\";
    private static final String inFilesDir = "E:\\Bak\\processing folder ROI\\";
    private static final String houghCirclesResultDir = inFilesDir + "houghcircles/";
    private static final String roiResultDir = houghCirclesResultDir + "roi/";

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        testHoughCircleDetectionOnDirNew();
    }

    public static void testHoughCircleDetectionOnDirNew() {
        File imageDir = new File(inFilesDir);

        FilenameFilter fileNameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };

        Mat inImageGray = null;
        Mat inImage = null;
        Mat inImagePaintable = null;
        Mat inImagePaintableALLCIRCLES = null;
        Mat circles;
        Mat roi = null;
        Point posCircle;
        Point centerOfFocusArea;
        File circlesFile;
        File circlesFile_ALL;
        File roiRectangleFile;
        File roiFile;
        List<Point> circlePoints;
        double[] currentCircleCoords;
        String circlesFileName;
        String circlesFileName_ALL;
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
            inImagePaintableALLCIRCLES = new Mat(inImage.rows(), inImage.cols(), inImage.type());
            inImage.copyTo(inImagePaintable);
            inImage.copyTo(inImagePaintableALLCIRCLES);

            System.out.println("inImageGray size: " + inImageGray.size());

            circles = new Mat(inImageGray.rows(), inImageGray.cols(), inImageGray.type());

            //auflicht settings
//            double canny1 = CannyParameters.AUFLICHT_CANNY1;
//            double canny2 = CannyParameters.AUFLICHT_CANNY2;
//            double dp = CannyParameters.AUFLICHT_DP;
//            double minDistance = CannyParameters.AUFLICHT_MIN_DISTANCE;
//            int minRadius = CannyParameters.AUFLICHT_MIN_RADIUS;
//            int maxRadius = CannyParameters.AUFLICHT_MAX_RADIUS;

            //durchlicht settings
            double canny1 = CannyParameters.DURCHLICHT_CANNY1;
            double canny2 = CannyParameters.DURCHLICHT_CANNY2;
            double dp = CannyParameters.DURCHLICHT_DP;
            double minDistance = CannyParameters.DURCHLICHT_MIN_DISTANCE;
            int minRadius = CannyParameters.DURCHLICHT_MIN_RADIUS;
            int maxRadius = CannyParameters.DURCHLICHT_MAX_RADIUS;

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

// this part is used to search for hough circles only around a certain point, i.e. center area
//                    int centerOfFocusArea_X = 1280;
//                    int centerOfFocusArea_Y = 980;

//                    int centerOfFocusArea_X = 1430;
//                    int centerOfFocusArea_Y = 960;

                    int centerOfFocusArea_X = 2050;
                    int centerOfFocusArea_Y = 1430;

//                    int centerOfFocusArea_X = 2155;
//                    int centerOfFocusArea_Y = 1100;

                    centerOfFocusArea = new Point(centerOfFocusArea_X, centerOfFocusArea_Y);

                    final int MAX_DIFF_X = 355;
                    final int MAX_DIFF_Y = 155;

// according to image dimensions
//                    final int MAX_DIFF_X = 200;
//                    final int MAX_DIFF_Y = 50;

                    if (posCircle.x < centerOfFocusArea.x + MAX_DIFF_X
                            && posCircle.x > centerOfFocusArea.x - MAX_DIFF_X
                            && posCircle.y < centerOfFocusArea.y + MAX_DIFF_Y
                            && posCircle.y > centerOfFocusArea.y - MAX_DIFF_Y
                            ) {
                        circlePoints.add(posCircle);
                        Core.circle(inImagePaintable, posCircle, (int) currentCircleCoords[2], new Scalar(0, 255, 0), 20);
                    }

                    Core.circle(inImagePaintableALLCIRCLES, posCircle, (int) currentCircleCoords[2], new Scalar(0, 255, 0), 20);

                }

                circlesFileName = currFileInName.replace(".", "_houghCircles.");
                circlesFileName_ALL = currFileInName.replace(".", "_houghCircles_ALL.");

                circlesFile_ALL = new File(houghCirclesResultDir + "failed/" + circlesFileName_ALL);
                Highgui.imwrite(circlesFile_ALL.toString(), inImagePaintableALLCIRCLES);

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

// according to image dimensions
//                    start.x -= 70;
//                    start.y += 200;

                    end = new Point(start.x + 700, start.y + 700);
//                    end = new Point(start.x + 500, start.y + 500);

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
