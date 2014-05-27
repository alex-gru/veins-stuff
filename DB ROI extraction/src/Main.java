import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String inFilesDir = "images/Tag der offenen Tuer - Backup/";
    private static final String houghCirclesResultDir = inFilesDir + "houghcircles/";
    private static final String roiResultDir = inFilesDir + "roi/";

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
        for (final File inFile : imageDir.listFiles(fileNameFilter)) {
            String currFileInName = inFile.getName();
            System.out.println(currFileInName);

            Mat inImageGray = Highgui.imread(inFile.toString(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
            Mat inImage = Highgui.imread(inFile.toString());

            Mat inImagePaintable = new Mat(inImage.rows(), inImage.cols(), inImage.type());
            inImage.copyTo(inImagePaintable);

            System.out.println("inImageGray size: " + inImageGray.size());

            Mat circles = new Mat(inImageGray.rows(), inImageGray.cols(), inImageGray.type());
            Imgproc.HoughCircles(inImageGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 50, 30, 30, 30, 50);
            System.out.println("#circles: " + circles.cols());
            System.out.println("size: " + circles.size());

            List<Point> circlePoints = new ArrayList<Point>();
            if (circles.cols() > 0) {
                for (int i = 0; i < circles.cols(); i++) {
                    double[] current = circles.get(0, i);
                    if (current == null) {
                        continue;
                    }
                    Point posCircle = new Point(current[0], current[1]);
                    Point centerInImage = new Point(inImageGray.cols() / 2, inImageGray.rows() / 2);
                    final int MAX_DIFF = 200;
                    if (posCircle.x < centerInImage.x + MAX_DIFF
                            && posCircle.x > centerInImage.x - MAX_DIFF
                            && posCircle.y < centerInImage.y + MAX_DIFF
                            && posCircle.y > centerInImage.y - MAX_DIFF) {
                        circlePoints.add(posCircle);
                        Core.circle(inImagePaintable, posCircle, (int) current[2], new Scalar(0, 255, 0), 20);
                    }
                }

                String circlesFileName = currFileInName.replace(".", "_houghCircles.");
                File circlesFile = new File(houghCirclesResultDir + circlesFileName);
                Highgui.imwrite(circlesFile.toString(), inImagePaintable);

                if (circlePoints.size() != 2) {
                    System.err.println("Did not find the expected number of circles: " + circlePoints.size());
                } else {
                    String roiRectangleFileName = currFileInName.replace(".", "_roiRectangle.");
                    File roiRectangleFile = new File(roiResultDir + roiRectangleFileName);
                    String roiFileName = currFileInName.replace(".", "_roi.");
                    File roiFile = new File(roiResultDir + roiFileName);

                    Point start;
                    if (circlePoints.get(0).x < circlePoints.get(1).x) {
                        start = circlePoints.get(0);
                    } else {
                        start = circlePoints.get(1);
                    }
                    start.x -= 100;
                    start.y += 160;
                    Point end = new Point(start.x + 500, start.y + 500);
                    Core.rectangle(inImagePaintable, start, end, new Scalar(0, 0, 255), 10);
                    Highgui.imwrite(roiRectangleFile.toString(), inImagePaintable);
                    Mat roi = inImage.submat(new Rect(start, end));
                    Highgui.imwrite(roiFile.toString(), roi);
                }
            }
        }
    }
}
