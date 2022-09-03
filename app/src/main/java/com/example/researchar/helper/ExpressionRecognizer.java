package com.example.researchar.helper;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import com.example.researchar.R;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ExpressionRecognizer {
    // define interpreter
    // Before this implement tensorflow to build.gradle
    private static final String TAG = "Facial_Expression";
    private final Interpreter interpreter;

    //define input size
    private int INPUT_SIZE;

    // define height and width of original frame
    private int height = 0;
    private int width = 0;

    // now define Gpudelegate
    // it is used to implement gpu in interpreter
    private GpuDelegate gpuDelegate = null;

    // now define cascadeClassifier for face detection
    private CascadeClassifier cascadeClassifier;

    // Mat to hold emoji

    Mat surprise = null;
    Mat fear = null;
    Mat angry = null;
    Mat neutral = null;
    Mat sad = null;
    Mat disgust = null;
    Mat happy = null;

    // call this om camera activity
    public ExpressionRecognizer(AssetManager assetManager, Context context, String modelPath, int inputSize) throws IOException {
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
        // if model is loaded, print
        Log.d(TAG, "Model is loaded");

        // now we load face detection model
        loadFaceDetectionModel(context);

        // load all emoji images to show
        loadEmojis(context);
    }

    private void loadFaceDetectionModel(Context context) {
        try {
            // define input stream to read classifier
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            // create a folder
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE); // creating a folder
            // now create file in that folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml"); // creating file on that folder
            // now define output stream to transfer data to file we created
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            // now create buffer to store bytes
            byte[] buffer = new byte[4096];
            int byteRead;
            // Read byte in while loop
            // when it read -1 that means no data to read
            while ((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            // close input and output stream
            is.close();
            os.close();

            // returning cascade classifier with face detection
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.d(TAG, "Cascade classifier is loaded");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // use to load model
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // this will give description of file
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);
        // create a input stream to read file
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

    }

    public void loadEmojis(Context context){
        surprise = new Mat();
        fear = new Mat();
        angry = new Mat();
        neutral = new Mat();
        sad = new Mat();
        disgust = new Mat();
        happy = new Mat();

//        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.surprise), surprise, false);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.fear), fear);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.angry), angry);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.neutral), neutral);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.sad), sad);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.disgust), disgust);
        Utils.bitmapToMat(BitmapFactory.decodeResource(context.getResources(), R.raw.happy), happy);

        try {
            surprise = Utils.loadResource(context, R.raw.surprise, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Imgproc.resize(surprise, surprise, new Size(70, 70));
        Imgproc.resize(fear, fear, new Size(70, 70));
        Imgproc.resize(angry, angry, new Size(70, 70));
        Imgproc.resize(neutral, neutral, new Size(70, 70));
        Imgproc.resize(sad, sad, new Size(70, 70));
        Imgproc.resize(disgust, disgust, new Size(70, 70));
        Imgproc.resize(happy, happy, new Size(70, 70));
    }

    // input and output are in Mat format
    // call this in onCameraFrame of CameraActivity
    public Mat recognizeImage(Mat mat_image) {
        // before predicting
        // our image is not properly aligned
        // we have to rotate it b9 90 degree for proper prediction
        Core.flip(mat_image.t(), mat_image, 1);

        // Convert mat_image to gray scale image
        Mat grayscaleImage = new Mat();
        Imgproc.cvtColor(mat_image, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        // set height and width
        width = grayscaleImage.width();
        height = grayscaleImage.height();

        // define minimum height of face in original image
        // below this size, no of face in original image will show
        int absoluteFaceSize = (int) (height * 0.1);

        // now create MatofRect to store faces
        MatOfRect faces = new MatOfRect();
        // check if cascadeClassifier is loaded or not
        if (cascadeClassifier != null) {
            // detect face in frame
            // input, output, scale factor, min neighbors, flag, minimum size of output
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // now convert it into array
        Rect[] faceArray = faces.toArray();
        // Loop through each face
        for (int i = 0; i < faceArray.length; i++) {
            // if you want to draw rectangle around face
            // input, face top left (starting point), face bottom right (ending point), color R G B A, thickness
            Imgproc.rectangle(mat_image, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);

            // now crop face from original frame and grayscaleImage
            // Starting x coordinate, Starting y coordinate, width, height
            Rect roi = new Rect(
                    (int) faceArray[i].tl().x, (int) faceArray[i].tl().y,
                    ((int) faceArray[i].br().x) - ((int) faceArray[i].tl().x),
                    ((int) faceArray[i].br().y) - ((int) faceArray[i].tl().y)
            );
            // its very important to check one more time
            Mat cropped_rgba = new Mat(mat_image, roi); //
            // now convert cropped_rgba to bitmap
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(cropped_rgba.cols(), cropped_rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba, bitmap);
            // resize bitmap to (48, 48)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, false);
            // now convert scaledBitmap to byte buffer
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);
            // now create an object to hold output
            float[][]  emotion= new float[1][1];

            // now predict with bytebuffer as input and emotion as an output
            interpreter.run(byteBuffer, emotion);

            // if emotion is recognize print value of it
            Log.d(TAG, "Output: " + Array.get(Array.get(emotion, 0), 0));

            // create a function that return text emotion
            float emotion_v = (float) Array.get(Array.get(emotion, 0), 0);
            String emotion_text = get_emotion_text(emotion_v);
            Imgproc.putText(mat_image, emotion_text, new Point((int) faceArray[i].tl().x, (int) faceArray[i].tl().y), 1, 1.5, new Scalar(0,0,255,150), 2);
            Mat submat= mat_image.submat(0,70,0,70);
            Mat emoji = getEmoji(emotion_text);
            emoji.copyTo(submat);
        }

        // after predicting
        // rotate image back to original
        // rotate mat_image by -90 degree
        Core.flip(mat_image.t(), mat_image, 0);
        return mat_image;
    }

    private Mat getEmoji(String emotion) {
        Mat retval;
        switch (emotion){
            case "Surprise":
                retval = surprise;
                break;
            case "Fear":
                retval = fear;
                break;
            case "Angry":
                retval = angry;
                break;
            case "Neutral":
                retval = neutral;
                break;
            case "Sad":
                retval = sad;
                break;
            case "Disgust":
                retval = disgust;
                break;
            case "Happy":
                retval = happy;
                break;
            default:
                retval = null;
        }
        return retval;
    }

    private String get_emotion_text(float emotion_v) {
        // create an empty string
        String val = "";
        // use if statement to determine val
        // You can change starting value and ending value to get better result
        // Like
        if (emotion_v >= 0 & emotion_v < 0.5) {
            val = "Surprise";
        } else if (emotion_v >= 0.5 & emotion_v < 1.5) {
            val = "Fear";
        } else if (emotion_v >= 1.5 & emotion_v < 2.5) {
            val = "Angry";
        } else if (emotion_v >= 2.5 & emotion_v < 3.5) {
            val = "Neutral";
        } else if (emotion_v >= 3.5 & emotion_v < 4.5) {
            val = "Sad";
        } else if (emotion_v >= 4.5 & emotion_v < 5.5) {
            val = "Disgust";
        } else {
            val = "Happy";
        }
        return val;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap scaledBitmap) {
        ByteBuffer byteBuffer;
        int size_image = INPUT_SIZE;//48

        byteBuffer = ByteBuffer.allocateDirect(4 * size_image * size_image * 3);
        // 4 is multiplied for float input
        // 3 is multiplied for rgb
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_image * size_image];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < size_image; ++i) {
            for (int j = 0; j < size_image; ++j) {
                final int val = intValues[pixel++];
                // now put float value to bytebuffer
                // scale image to convert image from 0-255 to 0-1
                byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                byteBuffer.putFloat(((val & 0xFF)) / 255.0f);

            }
        }
        return byteBuffer;
        // check one more time it is important else you will get error
    }
}
