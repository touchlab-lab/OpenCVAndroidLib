package org.opencv.samples.colorblobdetect;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Switch;

import org.florescu.android.rangeseekbar.RangeSeekBar;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class BaseCameraActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private Mat mRgba;
    Mat mHsvMat;
    Mat mMask;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            switch(status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(BaseCameraActivity.this);
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private RangeSeekBar rsbHue;
    private RangeSeekBar rsbSat;
    private RangeSeekBar rsbVal;
    private Switch showFilter;

    public BaseCameraActivity()
    {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.hsv_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(
                R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        showFilter = (Switch) findViewById(R.id.showFilter);
        rsbHue = (RangeSeekBar) findViewById(R.id.rsbHue);
        rsbSat = (RangeSeekBar) findViewById(R.id.rsbSat);
        rsbVal = (RangeSeekBar) findViewById(R.id.rsbVal);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(! OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;



        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if(showFilter.isChecked())
        {
            Scalar mLowerBound = new Scalar(0);
            Scalar mUpperBound = new Scalar(0);

            mLowerBound.val[0] = rsbHue.getSelectedMinValue().doubleValue();
            mUpperBound.val[0] = rsbHue.getSelectedMaxValue().doubleValue();
            mLowerBound.val[1] = rsbSat.getSelectedMinValue().doubleValue();
            mUpperBound.val[1] = rsbSat.getSelectedMaxValue().doubleValue();
            mLowerBound.val[2] = rsbVal.getSelectedMinValue().doubleValue();
            mUpperBound.val[2] = rsbVal.getSelectedMaxValue().doubleValue();

            mLowerBound.val[3] = 0;
            mUpperBound.val[3] = 255;

            if(mHsvMat == null) mHsvMat = new Mat();
            if(mMask == null) mMask = new Mat();

            Imgproc.cvtColor(mRgba, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

            Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);

            Imgproc.cvtColor(mMask, mRgba, Imgproc.COLOR_GRAY2RGBA);
        }

        /*if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }*/

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
