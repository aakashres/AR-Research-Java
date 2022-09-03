package com.example.researchar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.researchar.helper.ExpressionRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class EmotionDetection extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG="EmotionDetection";

    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;

    // call java class
    private ExpressionRecognizer facialExpressionRecognition;


    final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG, "OpenCv is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:{
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public EmotionDetection(){
        Log.i(TAG, "Instantiated new "+ this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int MY_PERMISSION_REQUEST_CAMERA = 0;
        if (ContextCompat.checkSelfPermission(EmotionDetection.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(EmotionDetection.this, new String [] {Manifest.permission.CAMERA}, MY_PERMISSION_REQUEST_CAMERA);
        }
        setContentView(R.layout.activity_emotion_detection);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);

        // THis will load cascade classifier and model
        /// this only happen one time when you start CameraActivity
        try{
            // input size of model is 48x48
            facialExpressionRecognition = new ExpressionRecognizer(getAssets(), EmotionDetection.this, "model.tflite", 48);
            Log.d(TAG, "Model is loaded successfully");
        } catch (IOException e){
            Log.d(TAG, "Getting error while loading model");
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.d(TAG, "Opencv is not loaded. Try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        // output
        mRgba = facialExpressionRecognition.recognizeImage(mRgba);
        return mRgba;
    }
}