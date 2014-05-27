import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;

/**
 * Created by alexgru-mobile on 27.05.14.
 */
public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String inFileName = "images/IMG_0036_AUFLICHT_ID44.JPG";
        File inFile = new File(inFileName);
        Mat inImageGray = Highgui.imread(inFile.toString(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat inImage = Highgui.imread(inFile.toString());

        System.out.println(inImageGray.size());

        Mat circles = new Mat(inImageGray.rows(), inImageGray.cols(), inImageGray.type());
        Imgproc.HoughCircles(inImageGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 100, 50, 50, 0, 100);
        System.out.println("#circles: " + circles.rows());
        System.out.println("size: " + circles.size());

        for (int i = 0; i < circles.cols(); i++) {
            double[] current = circles.get(0, i);
            Point center = new Point(current[0], current[1]);
            Core.circle(inImage, center, (int) current[2], new Scalar(0, 255, 0), 20);
        }

        String circlesFileName = inFile.toString().replace(".", "_houghCircles.");
        File circlesFile = new File(circlesFileName);
        Highgui.imwrite(circlesFile.toString(), inImage);
    }
}
