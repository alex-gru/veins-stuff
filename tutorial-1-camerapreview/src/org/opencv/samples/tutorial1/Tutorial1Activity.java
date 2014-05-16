package org.opencv.samples.tutorial1;

import android.view.*;
import android.widget.FrameLayout;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Tutorial1Activity extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private FrameLayout root;

    private boolean edgeDetection = false;
    private boolean gauss = false;
    private boolean contours = true;


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

    public Tutorial1Activity() {
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

        setContentView(R.layout.tutorial1_surface_view);
        root = (FrameLayout) findViewById(R.id.container);

        if (mIsJavaCamera)
            mOpenCvCameraView = (JavaCameraView) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (NativeCameraView) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (JavaCameraView) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (NativeCameraView) findViewById(R.id.tutorial1_activity_native_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
        filtered = new Mat(height, width, CvType.CV_8UC4);
        overlay = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        frame.release();
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
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            frame = inputFrame.gray();
//            Imgproc.Canny(frame, filtered, 30, 30);
//            Imgproc.findContours(filtered, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2RGBA, 4);

            /*
            *   Start PROBABILISTIC HOUGH TRANSFORM
            */
//            Mat lines = new Mat();
//            int threshold = 50;
//            int minLineSize = 20;
//            int lineGap = 20;
//
//            Imgproc.HoughLinesP(filtered, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
//
//            for (int x = 0; x < lines.rows(); x++)
//            {
//                double[] vec = lines.get(0, x);
//                double x1 = vec[0],
//                        y1 = vec[1],
//                        x2 = vec[2],
//                        y2 = vec[3];
//                Point start = new Point(x1, y1);
//                Point end = new Point(x2, y2);
//
//                Core.line(frame, start, end, new Scalar(255,0,0), 3);
//            }
            /*
            *   End PROBABILISTIC HOUGH TRANSFORM
            */

            Imgproc.HoughCircles(frame, filtered, Imgproc.CV_HOUGH_GRADIENT, 1, 10, 200,100, 0, 50);
            for (int i = 0; i < filtered.rows(); i++) {
                Point center = new Point();
            }
            frame = inputFrame.rgba();

//            Imgproc.drawContours(frame, contours, -1, new Scalar(0, 255, 0), 5);
            return frame;
        }
        return inputFrame.rgba();
    }

    /*
    Android Kitkat(4.4)'s new IMMERSIVE MODE, this is the sticky variant, so the action bar automatically disappears
    after a short delay.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            root.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "Touch!");

        return false;
    }
}
