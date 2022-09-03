package com.example.researchar.helper;


import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BRISK;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ObjectDetector {
    private static final String TAG = "Object Detector";
    // this is used to load model and predict
    private Interpreter interpreter;
    // store all label in array
    private List<String> labelList;
    // use to initialize GPU in app
    private GpuDelegate gpuDelegate;

    private int INPUT_SIZE;
    private int PIXEL_SIZE = 3; // For RGB
    private int IMAGE_MEAN = 0;
    private float IMAGE_STD = 255.0f;
    private int height=0;
    private int width=0;
    private float THRESHOLD = 0.65f;

    private BRISK mBr;
    private int frameCounter = 0;
    private boolean firstObject = true;
    private ArrayList<Objects> detectedObjects;
    private ObjectTracker tracker;

    public ObjectDetector(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException{
        INPUT_SIZE = inputSize;
        // set GPU for the interpreter
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        // add gpuDelegate to option
        options.addDelegate(gpuDelegate);
        // now set number of threads to option
        options.setNumThreads(4); // set this according to your phone
        // this will load model weights to interpreter
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);
        // load label map
        labelList = loadLabelList(assetManager, labelPath);
        // if model is loaded, print
        Log.d(TAG, "Model is loaded");
        mBr = BRISK.create(60, 4);
        tracker = new ObjectTracker();
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        // to store label
        List<String> labelList = new ArrayList<>();
        // create a new reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;

        // loop through each line and store it to label List

        while((line=reader.readLine())!= null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    // use to load model
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // this will give description of file
        // use to get description of the file
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        // create a input stream to read file
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public ArrayList<ObjectIdentifier> getObjectIdentifiers(Mat mat_image){

//        MatOfKeyPoint objectKeypoints = new MatOfKeyPoint();
//        Mat objectDescriptors = new Mat();

        // Call the BRISK implementation detect keypoints and
        // calculate the descriptors, based on the grayscale image.
//        mBr.detectAndCompute(mGrey, new Mat(), objectKeypoints, objectDescriptors);

        // More interesting to get the circle around keypoints with with keypoint size and orientation
        // but in this case we will see grey ouput image.
        // Draw calculated keypoints into the original image
//        Features2d.drawKeypoints(mGrey, objectKeypoints, mRgba, new Scalar( 255, 0, 0 ), DrawMatchesFlags_DRAW_OVER_OUTIMG);

        // Another quick solution how to draw keypoints
        // KeyPoint[] kpArr = objectKeypoints.toArray();
        // for (KeyPoint i : kpArr)
        //    Imgproc.drawMarker(mRgba, i.pt, new Scalar( 255, 0, 0 ));

//        objectDescriptors.release();
//        objectKeypoints.release();

        // Rotate Original image by 90 degree to get portrait frame
        Mat rotated_mat_image = new Mat();
        Core.flip(mat_image.t(), rotated_mat_image, 1);

        Log.d(TAG, "" + mat_image.size());

        // convert rgba image to bitmap

        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image, bitmap);

        // define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        // convert scaled bitmap to byte buffer as model input should be in it
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // we are not going to use this method of output
        // instead we create tree mp of three array (boxes, score, classes)
        Map<Integer, Object> output_map = new TreeMap<>();
        // define output array
        // 10: top 10 object detected
        // 4: their coordinate in image
        float[][][] boxes = new float[1][10][4];

        // stores scores of 10 detected objects
        float[][] scores = new float[1][10];

        // stores classes of detected object
        float[][] classes = new float[1][10];

        // add  arrays to object map
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);

        Object value = output_map.get(0);
        Object object_class = output_map.get(1);
        Object score = output_map.get(2);

        ArrayList<ObjectIdentifier> objects = new ArrayList<ObjectIdentifier> ();

        // loop trhough each objects
        for(int i = 0; i < 10; i++){
            MatOfKeyPoint objectKeypoints = new MatOfKeyPoint();
            Mat objectDescriptors = new Mat();

            float class_value = (float) Array.get(Array.get(object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            if (score_value > THRESHOLD){
                Object box1 = Array.get(Array.get(value, 0), i);
                float top = (float)  Array.get(box1, 0)*height;
                float left = (float)  Array.get(box1, 1)*width;
                float bottom = (float)  Array.get(box1, 2)*height;
                float right = (float)  Array.get(box1, 3)*width;
                if(top > 0 && top < height && bottom > 0 && bottom < height && left > 0 && left < width && right > 0 && right < width){
                    Rect rectCrop = new Rect(new Point(left, top), new Point(right, bottom));
                    Log.d(TAG, "" + top + "," + left + "," + right + "," + bottom);
                    Mat imageROI = new Mat(rotated_mat_image, rectCrop);
                    Imgproc.cvtColor(imageROI, imageROI, Imgproc.COLOR_RGB2GRAY);
                    mBr.detectAndCompute(imageROI, new Mat(), objectKeypoints, objectDescriptors);
                    ObjectIdentifier obj = new ObjectIdentifier(labelList.get((int) class_value), objectKeypoints, objectDescriptors, new Mat(rotated_mat_image, rectCrop), rectCrop);
                    objects.add(obj);
                    imageROI.release();
                }
            }
            objectKeypoints.release();
            objectDescriptors.release();

        }
        return  objects;
    }


    public Mat matchObject(Mat mRgb, ObjectIdentifier obj){
        // Rotate Original image by 90 degree to get portrait frame
        Mat rotated_mat_image = new Mat();
        Core.flip(mRgb.t(), rotated_mat_image, 1);

        // makes a copy of the source image
        Mat temp = new Mat();
        rotated_mat_image.copyTo(temp);

        // matrix to save result of matching
        int result_cols = temp.cols() - obj.getmRbg().cols() + 1;
        int result_rows = temp.rows() - obj.getmRbg().rows() + 1;
        Mat result = new Mat();
        result.create(result_rows, result_cols, CvType.CV_32FC1);

        // Perform the template matching operation. The arguments are naturally the input image I, the template T, the result R and the match_method (given by the Trackbar),
        Imgproc.matchTemplate(temp, obj.getmRbg(), result, Imgproc.TM_CCORR_NORMED);

        // normalize the results:
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // localize the minimum and maximum values in the result matrix
        Point matchLoc;
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        matchLoc = mmr.maxLoc;
        Log.d(TAG, "" + matchLoc);
        Imgproc.rectangle(rotated_mat_image, matchLoc, new Point(matchLoc.x + obj.getmRbg().cols(), matchLoc.y + obj.getmRbg().rows()), new Scalar(255,155,155), 2);
        // Before returning, rotate image back -90 degree
        Core.flip(rotated_mat_image.t(), mRgb, 0);
        return  mRgb;
    };

    public ArrayList<Objects> detectObjects(Mat mat_image){
        ArrayList<Objects> objects = new ArrayList<>();
        // Rotate Original image by 90 degree to get portrait frame
        Mat rotated_mat_image = new Mat();
        Core.flip(mat_image.t(), rotated_mat_image, 1);

        // convert rgba image to bitmap

        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image, bitmap);

        // define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        // convert scaled bitmap to byte buffer as model input should be in it
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // we are not going to use this method of output
        // instead we create tree mp of three array (boxes, score, classes)
        Map<Integer, Object> output_map = new TreeMap<>();
        // define output array
        // 10: top 10 object detected
        // 4: their coordinate in image
        float[][][] boxes = new float[1][10][4];

        // stores scores of 10 detected objects
        float[][] scores = new float[1][10];

        // stores classes of detected object
        float[][] classes = new float[1][10];

        // add  arrays to object map
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);

        Object value = output_map.get(0);
        Object object_class = output_map.get(1);
        Object score = output_map.get(2);

        // loop trhough each objects
        for(int i = 0; i < 10; i++){
            float class_value = (float) Array.get(Array.get(object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            if (score_value > THRESHOLD){
                Object box1 = Array.get(Array.get(value, 0), i);
                float top = (float)  Array.get(box1, 0)*height;
                float left = (float)  Array.get(box1, 1)*width;
                float bottom = (float)  Array.get(box1, 2)*height;
                float right = (float)  Array.get(box1, 3)*width;
                if(top > 0 && top < height && bottom > 0 && bottom < height && left > 0 && left < width && right > 0 && right < width){
                    objects.add(new Objects(new Rect(new Point(left, top), new Point(right, bottom)), labelList.get((int) class_value)));
                }

            }

        }
        return objects;
    }

    public Mat recognizeAndTrackObjects(Mat mat_image){
        if(firstObject){
            detectedObjects = detectObjects(mat_image);
            if(detectedObjects.size() > 0){
                firstObject =  false;
                Objects obj = detectedObjects.get(0);
                Log.d(TAG, "" + obj.getBoundingBox());
                Log.d(TAG, "" + obj.getObjectName());
                tracker.addTrackingObject(mat_image, obj);
            }
        } else {
            tracker.updateBox(mat_image);
            mat_image = tracker.drawObjects(mat_image);
        }

        frameCounter++;
        return  mat_image;
    }

    public Mat recognizeImage(Mat mat_image){
        // Rotate Original image by 90 degree to get portrait frame
        Mat rotated_mat_image = new Mat();
        Core.flip(mat_image.t(), rotated_mat_image, 1);

        // convert rgba image to bitmap

        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image, bitmap);

        // define height and width
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        
        // convert scaled bitmap to byte buffer as model input should be in it
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        // we are not going to use this method of output
        // instead we create tree mp of three array (boxes, score, classes)
        Map<Integer, Object> output_map = new TreeMap<>();
        // define output array
        // 10: top 10 object detected
        // 4: their coordinate in image
        float[][][] boxes = new float[1][10][4];

        // stores scores of 10 detected objects
        float[][] scores = new float[1][10];

        // stores classes of detected object
        float[][] classes = new float[1][10];

        // add  arrays to object map
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);

        Object value = output_map.get(0);
        Object object_class = output_map.get(1);
        Object score = output_map.get(2);

        // loop trhough each objects
        for(int i = 0; i < 10; i++){
            float class_value = (float) Array.get(Array.get(object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            if (score_value > THRESHOLD){
                Object box1 = Array.get(Array.get(value, 0), i);
                float top = (float)  Array.get(box1, 0)*height;
                float left = (float)  Array.get(box1, 1)*width;
                float bottom = (float)  Array.get(box1, 2)*height;
                float right = (float)  Array.get(box1, 3)*width;
                if(top > 0 && top < height && bottom > 0 && bottom < height && left > 0 && left < width && right > 0 && right < width){
                    // Draw rectangle original image, starting point, end point, color, thickness
                    Imgproc.rectangle(rotated_mat_image, new Point(left, top), new Point(right, bottom), new Scalar(255,155,155), 2);
                    // Write label original image, class, starting point, font style, scale, color, thickenss
                    Imgproc.putText(rotated_mat_image, labelList.get((int) class_value), new Point(left, top), 3, 1, new Scalar(100, 100, 100),2);
                    Rect rectCrop = new Rect(new Point(left, top), new Point(right, bottom));
                    Log.d(TAG, "" + top + "," + left + "," + right + "," + bottom);
                    Mat imageROI = new Mat(rotated_mat_image, rectCrop);
                    Imgproc.GaussianBlur(imageROI, imageROI, new Size(55, 55), 55);
                    rotated_mat_image.copyTo(imageROI);
                }

            }

        }
        // Before returning, rotate image back -90 degree
        Core.flip(rotated_mat_image.t(), mat_image, 0);
        return  mat_image;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int quant = 0;
        int size_image = INPUT_SIZE;// 300
        if (quant == 0 ){
            byteBuffer = ByteBuffer.allocateDirect(size_image * size_image * 3);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        }
        // 4 is multiplied for float input
        // 3 is multiplied for rgb


        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_image * size_image];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < size_image; ++i) {
            for (int j = 0; j < size_image; ++j) {
                final int val = intValues[pixel++];
                if (quant==0){
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else{
                    // now put float value to bytebuffer
                    // scale image to convert image from 0-255 to 0-1
                    byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat(((val & 0xFF)) / 255.0f);
                }
            }
        }
        return byteBuffer;
        // check one more time it is important else you will get error
    }

}
