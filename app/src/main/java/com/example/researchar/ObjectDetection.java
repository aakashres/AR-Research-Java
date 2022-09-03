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

import com.example.researchar.helper.FrameBuffer;
import com.example.researchar.helper.ObjectDetector;
import com.example.researchar.helper.ObjectIdentifier;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;


public class ObjectDetection extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG="FaceDetection";

    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;

    private ObjectDetector objectDetector;
    private boolean first_time = false;
    private ArrayList<ObjectIdentifier> objects;

    private long lastTime;
    private double fps;

    private FrameBuffer frameBuffer;
    private Mat retFrame;



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

    public ObjectDetection(){
        Log.i(TAG, "Instantiated new "+ this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int MY_PERMISSION_REQUEST_CAMERA = 0;
        if (ContextCompat.checkSelfPermission(ObjectDetection.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(ObjectDetection.this, new String [] {Manifest.permission.CAMERA}, MY_PERMISSION_REQUEST_CAMERA);
        }
        setContentView(R.layout.activity_emotion_detection);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setCvCameraViewListener(this);

        try{
            // input size of model is  300x300;
            objectDetector = new ObjectDetector(getAssets(), "ssd_mobilenet_v1_1_metadata_1.tflite", "objectlabelmap.txt", 300);
            Log.d(TAG, "Model is loaded successfully");
        } catch (IOException e){
            Log.d(TAG, "Getting error while loading model");
            e.printStackTrace();
        }

        frameBuffer = new FrameBuffer(10);
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

    private long displayFps(long lastTime){
        fps = 1000000000.0 / (System.nanoTime() - lastTime); //one second(nano) divided by amount of time it takes for one frame to finish
        Log.d(TAG, "FPS: " + fps);
        return System.nanoTime();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        lastTime = System.nanoTime();
        mRgba = inputFrame.rgba();
        mRgba = objectDetector.recognizeImage(mRgba);
//        mRgba= objectDetector.recognizeAndTrackObjects(mRgba);
//        mRgba = objectDetector.recognizeImage(mRgba);
//        frameBuffer.addFrames(mRgba);
//        retFrame  = frameBuffer.getFrames();
//
        lastTime = displayFps(lastTime);
        return mRgba;
//        if(retFrame.cols()>0 | retFrame.rows()>0){
//            return  retFrame;
//        } else{
//            return new Mat(mRgba.rows(), mRgba.cols(),  CvType.CV_8UC4);
//        }

//        mRgba = objectDetector.recognizeImage(mRgba);


//        if (first_time == false) {
//            objects = objectDetector.getObjectIdentifiers(mRgba);
//        } else {
//            Log.d(TAG, "length " + objects.size());
//            if (objects.size() > 0) {
//                first_time = false;
//                for (ObjectIdentifier obj : objects) {
//                    mRgba = objectDetector.matchObject(mRgba, obj);
//                    return mRgba;
//                }
//            }
//        }
    }
}