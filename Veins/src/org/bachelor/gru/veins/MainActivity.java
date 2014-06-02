package org.bachelor.gru.veins;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    public final static String TAG = "Gru-APP";

    private CameraView mOpenCvCameraView;
    private FrameLayout root;
    private Point touchedPoint = null;
    double[] pointColorValue;
    private boolean newPointSet = false;
    private boolean edgeDetection = false;
    private boolean gauss = false;
    private boolean contours = false;
    private boolean contoursWithHough = false;
    private boolean segmentation = false;
    private boolean templateMatching = false;
    private boolean orbMatching = false;
    private boolean touchBackgroundRemove = false;
    private boolean bottomColorDetectAndRemove = false;
    private boolean centerColorMeanDetectAndRemove = false;
    private boolean passOriginFrameWithCanny = true;

    private static Mat currentHandCutout = null;
    private static Mat currentOriginFrame = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private Mat frame;
    private Mat filtered;
    private Mat overlay;
    private boolean touched = false;
    ImageView templateImageView;

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + ((Object) this).getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);
        root = (FrameLayout) findViewById(R.id.container);

        mOpenCvCameraView = (CameraView) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

//        templateImageView = (ImageView) findViewById(R.id.template);
        root.setOnTouchListener(new RootTouchListener());

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
        filtered = new Mat(height, width, CvType.CV_8UC4);
        overlay = new Mat(height, width, CvType.CV_8UC4);
//        showTemplateAndKeypoints();
    }

    public void onCameraViewStopped() {
        frame.release();
        currentHandCutout.release();
        currentOriginFrame.release();
        filtered.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        if (edgeDetection) {
            frame = inputFrame.rgba();
            Imgproc.Canny(inputFrame.gray(), filtered, 80, 100);
            Imgproc.cvtColor(filtered, frame, Imgproc.COLOR_GRAY2RGBA, 4);

            return frame;
        } else if (gauss) {
            frame = inputFrame.rgba();
            Imgproc.GaussianBlur(frame, filtered, new Size(5, 5), 3);
            return filtered;
        } else if (contours) {
            if (touched) {
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                frame = inputFrame.gray();
                Imgproc.Canny(frame, filtered, 50, 50);
                Imgproc.findContours(filtered, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2RGBA, 4);

                double max = 0;
                int max_contour_idx = 0;
                Rect bounding_rect = null;

                for (int i = 0; i < contours.size(); i++) {
                    double area = Imgproc.contourArea(contours.get(i));
                    if (area > max) {
                        max = area;
                        max_contour_idx = i;
                        bounding_rect = Imgproc.boundingRect(contours.get(i));
                    }
                }


                frame = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);
//                Mat maskedFrame = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);
                Imgproc.drawContours(frame, contours, max_contour_idx, new Scalar(0, 255, 0), 5);
                return frame;
//                return maskedFrame;
            }
        } else if (contoursWithHough) {
            if (touched) {
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                frame = inputFrame.gray();
                Imgproc.Canny(frame, filtered, 50, 200);
                Imgproc.findContours(filtered, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2RGBA, 4);

            /*
            *   Start PROBABILISTIC HOUGH TRANSFORM
            */
                Mat lines = new Mat();
                int threshold = 50;
                int minLineSize = 30;
                int lineGap = 30;

                Imgproc.HoughLinesP(filtered, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);
//                Log.d(TAG, "Number of lines found: " + lines.cols());
                for (int x = 0; x < lines.cols(); x++) {
                    double[] vec = lines.get(0, x);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];
                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);

//                    Log.d(TAG, "Line: Start: " + start + ", end: " + end);
                    Core.line(frame, start, end, new Scalar(255, 0, 0), 3);
                }
            /*
            *   End PROBABILISTIC HOUGH TRANSFORM
            */


//                frame = inputFrame.rgba();
//                frame = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);

//                Imgproc.drawContours(frame, contours, -1, new Scalar(0, 255, 0), 5);

                double max = 0;
                int max_contour_idx = 0;
                Rect bounding_rect = null;

                for (int i = 0; i < contours.size(); i++) {
                    double area = Imgproc.contourArea(contours.get(i));
                    if (area > max) {
                        max = area;
                        max_contour_idx = i;
                        bounding_rect = Imgproc.boundingRect(contours.get(i));
                    }
                }

                if (bounding_rect != null) {
                    Point start = new Point(bounding_rect.x, bounding_rect.y);
                    Point end = new Point(start.x + bounding_rect.width, start.y + bounding_rect.height);
                    Core.rectangle(frame, start, end, new Scalar(0, 0, 255));
                }
                return frame;
            }
        } else if (segmentation) {
            if (touched) {
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                frame = inputFrame.gray();
                Imgproc.threshold(frame, frame, 120, 255, Imgproc.THRESH_BINARY);
                Imgproc.Canny(frame, frame, 70, 70);

                Imgproc.findContours(frame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                frame = inputFrame.rgba();
                Imgproc.drawContours(frame, contours, -1, new Scalar(0, 255, 0), 5);

                return frame;
            }
        } else if (templateMatching) {
            if (touched) {
                frame = inputFrame.rgba();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.hand_template_black, options);
                Mat template = new Mat(bMap.getHeight(), bMap.getWidth(), CvType.CV_8UC1);
                Utils.bitmapToMat(bMap, template);
                int match_method = Imgproc.TM_CCOEFF_NORMED;


                // / Create the result matrix
                int result_cols = frame.cols() - template.cols() + 1;
                int result_rows = frame.rows() - template.rows() + 1;
                Log.d(TAG, "cols: " + result_cols + ", rows: " + result_rows);
                Log.d(TAG, "template: cols: " + template.cols() + ", rows: " + template.rows());
                Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

                // / Do the Matching and Normalize
                Imgproc.matchTemplate(frame, template, result, match_method);
                Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

                // / Localizing the best match with minMaxLoc
                Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

                Point matchLoc;
                if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                    matchLoc = mmr.minLoc;
                } else {
                    matchLoc = mmr.maxLoc;
                }

                // / Show me what you got
                Point matchLoc_tx = new Point(matchLoc.x, matchLoc.y);
                Point matchLoc_ty = new Point(matchLoc.x + template.cols(),
                        matchLoc.y + template.rows());
                Core.circle(frame, matchLoc_tx, 10, new Scalar(255, 0, 0), 2);
//                Core.rectangle(frame, matchLoc_tx, matchLoc_ty, new Scalar(0, 255, 0));
//                Core.rectangle(frame, matchLoc, new Point(matchLoc.x + template.cols(),
//                        matchLoc.y + template.rows()), new Scalar(0, 255, 0));


                return frame;
            }
        } else if (orbMatching) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String filename;
            File file;

            if (touched) {
                FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
                DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

                //first image
                filename = "hand_template_samesize.png";
                file = new File(path, filename);
                filename = file.toString();
                Mat img1 = Highgui.imread(filename);
                Mat descriptors1 = new Mat();
                MatOfKeyPoint keypoints1 = new MatOfKeyPoint();

                detector.detect(img1, keypoints1);
                descriptor.compute(img1, keypoints1, descriptors1);

                //second image
                filename = "testinput.png";
                file = new File(path, filename);
                filename = file.toString();
                Mat img2 = Highgui.imread(filename);
                Mat descriptors2 = new Mat();
                MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

                detector.detect(img2, keypoints2);
                descriptor.compute(img2, keypoints2, descriptors2);

                //matcher should include 2 different image's descriptors
                MatOfDMatch matches = new MatOfDMatch();
                matcher.match(descriptors1, descriptors2, matches);
                //feature and connection colors
                Scalar RED = new Scalar(255, 0, 0);
                Scalar GREEN = new Scalar(0, 255, 0);
                //output image
                Mat outputImg = new Mat();
                MatOfByte drawnMatches = new MatOfByte();
                //this will draw all matches, works fine
                Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches,
                        outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
                filename = "matches.png";
                file = new File(path, filename);
                filename = file.toString();
                Log.d(TAG, "Save matches: " + Highgui.imwrite(filename, outputImg));

                touched = false;
            }
        } else if (touchBackgroundRemove) {
            frame = inputFrame.rgba();
            Mat filtered = new Mat(frame.rows(), frame.cols(), frame.type());
//            frame = inputFrame.gray();
            if (touchedPoint != null) {
                Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
                if (newPointSet) {
                    pointColorValue = frame.get((int) touchedPoint.y, (int) touchedPoint.x);
                    Log.d(TAG, "pointColorValue: " + pointColorValue[0] + " " + pointColorValue[1] + ", " + pointColorValue[2]);
                    newPointSet = false;
                }
                int maxDiff = 70;
                Scalar lowerThreshold = new Scalar(pointColorValue[0] - maxDiff, pointColorValue[1] - maxDiff, pointColorValue[2] - maxDiff);
                Scalar upperThreshold = new Scalar(pointColorValue[0] + maxDiff, pointColorValue[1] + maxDiff, pointColorValue[2] + maxDiff);
                Core.inRange(frame, lowerThreshold, upperThreshold, filtered);

                Imgproc.GaussianBlur(filtered, filtered, new Size(5, 5), 3);
                Imgproc.Canny(filtered, filtered, 110, 110);
//                Imgproc.dilate(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
//                Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
                frame = filtered;
            }
            return frame;
        } else if (bottomColorDetectAndRemove) {
            frame = inputFrame.rgba();
            Mat filtered = new Mat(frame.rows(), frame.cols(), frame.type());
//            frame = inputFrame.gray();
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
            Point wristAtBottomPosition = new Point(frame.cols() - 1, frame.rows() / 2);
            Log.d(TAG, "wristAtBottomPosition : " + wristAtBottomPosition);
            pointColorValue = frame.get((int) wristAtBottomPosition.y, (int) wristAtBottomPosition.x);

            Log.d(TAG, "pointColorValue: " + pointColorValue[0] + " " + pointColorValue[1] + ", " + pointColorValue[2]);
            int maxDiff = 10;
            Scalar lowerThreshold = new Scalar(pointColorValue[0] - maxDiff, pointColorValue[1] - maxDiff, pointColorValue[2] - maxDiff);
            Scalar upperThreshold = new Scalar(pointColorValue[0] + maxDiff, pointColorValue[1] + maxDiff, pointColorValue[2] + maxDiff);
            Core.inRange(frame, lowerThreshold, upperThreshold, filtered);
            frame = inputFrame.gray();
            Log.d(TAG, "frame " + frame.size() + ", filtered: " + filtered.size());
            Log.d(TAG, "[channels] frame " + frame.channels() + ", filtered: " + filtered.channels());
            inputFrame.rgba().copyTo(filtered, filtered);
            double[] val = filtered.get(500, 500);
            Log.d(TAG, "" + val[0]);

            return filtered;
        } else if (centerColorMeanDetectAndRemove) {
            frame = inputFrame.rgba();
            Mat filtered = new Mat(frame.rows(), frame.cols(), frame.type());
//            frame = inputFrame.gray();
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
            Point centerPosition = new Point(frame.cols() / 2, frame.rows() / 2);
//            Log.d(TAG, "centerPosition : " + centerPosition);
            pointColorValue = frame.get((int) centerPosition.y, (int) centerPosition.x);

            int maxDiff = 80;
            Scalar lowerThreshold = new Scalar(pointColorValue[0] - maxDiff, pointColorValue[1] - maxDiff, pointColorValue[2] - maxDiff);
            Scalar upperThreshold = new Scalar(pointColorValue[0] + maxDiff, pointColorValue[1] + maxDiff, pointColorValue[2] + maxDiff);
            Core.inRange(frame, lowerThreshold, upperThreshold, filtered);

            Size kernel = new Size(5, 5);
            Imgproc.erode(filtered, filtered, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernel));
            Imgproc.dilate(filtered, filtered, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernel));

//            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat edges = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);
            Imgproc.Canny(filtered, edges, 110, 110);
//            Imgproc.findContours(edges, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            frame = inputFrame.gray();
//            Log.d(TAG, "frame " + frame.size() + ", filtered: " + filtered.size());
//            Log.d(TAG, "[channels] frame " + frame.channels() + ", filtered: " + filtered.channels());
            inputFrame.rgba().copyTo(filtered, filtered);
//            Imgproc.drawContours(filtered, contours, -1, new Scalar(0, 255, 0), 10);
            currentHandCutout = new Mat(filtered.rows(), filtered.cols(), filtered.type());
            edges.copyTo(currentHandCutout);
            currentOriginFrame = new Mat(inputFrame.rgba().rows(), inputFrame.rgba().cols(), inputFrame.rgba().type());
            inputFrame.rgba().copyTo(currentOriginFrame);
//            filtered.copyTo(currentHandCutout);
            Core.circle(edges, centerPosition, 150, new Scalar(255, 0, 0), 5);

//            return edges;
            return filtered;
        } else if (passOriginFrameWithCanny) {
            Mat edges = new Mat(frame.rows(), frame.cols(), CvType.CV_8UC4);
            Imgproc.Canny(inputFrame.gray(), edges, 110, 110);

            currentHandCutout = new Mat(edges.rows(), edges.cols(), edges.type());
            edges.copyTo(currentHandCutout);
            currentOriginFrame = new Mat(inputFrame.rgba().rows(), inputFrame.rgba().cols(), inputFrame.rgba().type());
            inputFrame.rgba().copyTo(currentOriginFrame);

            Point centerPosition = new Point(frame.cols() / 2, frame.rows() / 2);
//            Core.circle(edges, centerPosition, 300, new Scalar(255, 0, 0), 5);
            Point startPoint = new Point(centerPosition.x - 200, centerPosition.y - 200);
            Point endPoint = new Point(centerPosition.x + 200, centerPosition.y + 200);
            Core.rectangle(edges, startPoint, endPoint, new Scalar(255, 0, 0), 10);

            return edges;
//            return inputFrame.gray();
        }

        return inputFrame.rgba();
    }

    /*
    Android Kitkat(4.4)'s new IMMERSIVE MODE, this is the sticky variant, so the action bar automatically disappears
    after a short delay.
     */
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            root.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
////                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
////                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
//    }

    public static void calcDistanceToCenter(View view) throws Exception {
        FileOutputStream fos;
        Mat currentFrame = currentHandCutout;
        fos = new FileOutputStream("/sdcard/distance.txt", false);

        FileWriter fWriter;
        fWriter = new FileWriter(fos.getFD());
        fWriter.write("");

        currentFrame = currentHandCutout;
        double maxDistance = 800;
        double minDistance = 50;
        List<Point> distancePoints = new ArrayList<Point>();
        List<Double> distancePointsDistances = new ArrayList<Double>();

        Point centerPosition = new Point(currentFrame.cols() / 2, currentFrame.rows() / 2);

        Log.d(TAG, "Size frame: " + currentFrame.size());
        for (int i = 0; i < currentFrame.rows(); i++) {
            for (int j = 0; j < centerPosition.x; j++) {
                double[] val = currentFrame.get(i, j);

                if (val[0] != 0) {
                    double dx = centerPosition.x - j;
                    double dy = centerPosition.y - i;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > minDistance && dist < maxDistance) {
                        distancePoints.add(new Point(j, i));
                        distancePointsDistances.add(dist);
                    }
                    fWriter.append(dist + "\n");
                } else {
                }
            }
        }


        fWriter.close();
        Log.d(TAG, "minDistancePoint: " + distancePoints);

        Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_GRAY2BGR);
        Core.circle(currentFrame, centerPosition, 300, new Scalar(255, 0, 0), 5);

        List<Double> filteredPointsDistances = new ArrayList<Double>();
        double minimum = 1000;
        for (int i = 0; i < distancePointsDistances.size(); i++) {
            double current = distancePointsDistances.get(i);
            if (current < minimum) {
                minimum = current;
            }
        }


        // filter points with certain maxDiff threshold
        double maxDiff = 150;
        final double MAX_VAL = 999999;
        for (int i = 0; i < distancePoints.size(); i++) {
            if (distancePointsDistances.get(i) < minimum + maxDiff
                    ) {
                filteredPointsDistances.add(distancePointsDistances.get(i));
            } else {
                filteredPointsDistances.add(Double.valueOf(MAX_VAL));
            }

        }


        // calculate local minima
        int countConsecutiveMaxVals = 0;
        final int COUNT_MAX_VALS_THRESHOLD_NEW_LOCAL_SET = 200;
        List<Double> localMinima = new ArrayList<Double>();
        List<Point> localMinimaPoints = new ArrayList<Point>();
        double localMinimum = MAX_VAL;
        int idxLocalMinimum = -1;
        Point localMinimumPoint = null;

        for (int i = 0; i < filteredPointsDistances.size(); i++) {
            double dist = filteredPointsDistances.get(i);
            if (dist == MAX_VAL) {
                countConsecutiveMaxVals++;
            } else if (dist < localMinimum) {
                localMinimum = dist;
                idxLocalMinimum = i;
                localMinimumPoint = distancePoints.get(idxLocalMinimum);
            }

            if (countConsecutiveMaxVals > COUNT_MAX_VALS_THRESHOLD_NEW_LOCAL_SET) {
                if (idxLocalMinimum != -1) {
                    // check if distance to previous local minimum is large enough
                    double distanceToPreviousLocalMinimum = 0;
                    if (localMinimaPoints.isEmpty()) {
                        distanceToPreviousLocalMinimum = 1000;
                    } else {
                        Point minimumCandidate = distancePoints.get(i);
                        Point previousMinimum = localMinimaPoints.get(localMinimaPoints.size() - 1);
                        double dx = minimumCandidate.x - previousMinimum.x;
                        double dy = minimumCandidate.y - previousMinimum.y;
                        distanceToPreviousLocalMinimum = Math.sqrt(dx * dx + dy * dy);
                    }
                    if (distanceToPreviousLocalMinimum > 400) {
                        localMinima.add(localMinimum);
                        localMinimaPoints.add(distancePoints.get(idxLocalMinimum));
                        countConsecutiveMaxVals = 0;
                        idxLocalMinimum = -1;
                    }
                } else {
                    localMinimum = MAX_VAL;
                }
            }
        }

        Mat minimaMat = new Mat(currentFrame.rows(), currentFrame.cols(), currentFrame.type());
        currentFrame.copyTo(minimaMat);
        for (int i = 0; i < localMinimaPoints.size(); i++) {
            Core.circle(minimaMat, localMinimaPoints.get(i), 5, new Scalar(0, 255, 0), 5);
        }


        Point startPoint;
        Point endPoint;
        int startPointIndex = -1;
        int endPointIndex = -1;

        // find reference 1
        startPointIndex = 0;

        startPoint = localMinimaPoints.get(startPointIndex);
//        startPoint.x = startPoint.x;
//        startPoint.y = startPoint.y + 100;
        startPoint = new Point(centerPosition.x - 200, centerPosition.y - 200);


        // find reference 2
        endPointIndex = localMinimaPoints.size() - 1;

//        endPoint = localMinimaPoints.get(endPointIndex);
//        endPoint.x = startPoint.x + 400;
//        endPoint.y = startPoint.y + 400;
        endPoint = new Point(centerPosition.x + 200, centerPosition.y + 200);

        Core.rectangle(currentFrame, startPoint, endPoint, new Scalar(0, 0, 255), 10);
        Core.rectangle(minimaMat, startPoint, endPoint, new Scalar(0, 0, 255), 10);
        Highgui.imwrite("/sdcard/distance_localMinima.png", minimaMat);

        double angle = Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)
                * 180.0 / Math.PI;
        Log.d(TAG, "angle between start and end point: " + angle);

        FileOutputStream fos_filtered;
        fos_filtered = new FileOutputStream("/sdcard/distance_filtered.txt", false);

        fWriter = new FileWriter(fos_filtered.getFD());
        fWriter.write("");

        Log.d(TAG, "currentOriginFrame: " + currentOriginFrame.size());
        Log.d(TAG, "startPoint: " + startPoint + " endPoint: " + endPoint);

        Imgproc.cvtColor(currentOriginFrame, currentOriginFrame, Imgproc.COLOR_BGR2RGB);
        Rect rectRoi = new Rect(startPoint, endPoint);
        Mat roi = currentOriginFrame.submat(rectRoi);
        Highgui.imwrite("/sdcard/distance_roi.png", roi);


        for (int i = 0; i < distancePoints.size(); i++) {
            fWriter.append(filteredPointsDistances.get(i) + "\n");
            if (filteredPointsDistances.get(i) < minimum + maxDiff) {
                Core.circle(currentFrame, distancePoints.get(i), 5, new Scalar(0, 255, 0), 1);
            }
        }
        fWriter.close();
        Highgui.imwrite("/sdcard/distance.png", currentFrame);
        Log.d(TAG, "return: " + rectRoi.size());
    }

    private class RootTouchListener implements View.OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                touchedPoint = new Point(x, y);
                Log.d(TAG, "new touchedPoint: " + touchedPoint);
                newPointSet = true;

                mOpenCvCameraView.takePicture();

//                try {
//                    calcDistanceToCenter(v);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
            return true;
        }
    }
}
