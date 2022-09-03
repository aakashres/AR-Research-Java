package com.example.researchar.helper;

import android.content.Context;
import android.util.Log;


import com.example.researchar.R;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceDetector {

    private static final String TAG = "Face_Detection";

    private CascadeClassifier cascadeClassifier;

    // call this om  activity
    public FaceDetector(Context context) throws IOException {
        // load the model
        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE); // creating a folder
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml"); // creating file on that folder
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int byteRead;
            // writing that file from raw folder
            while ((byteRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            is.close();
            os.close();
            // loading file from cascade folder created above
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (IOException e) {
            Log.i(TAG, "Cascade file not found");
        }

    }

    public Mat detectFaces(Mat mRgba) {
        // original frame is -90degree so we have to rotate 90 degree to get proper face detection
        Core.flip(mRgba.t(), mRgba, 1);

        // Convert it into RGB
        Mat mRgb = new Mat();
        Imgproc.cvtColor(mRgba, mRgb, Imgproc.COLOR_RGBA2RGB);
        int height = mRgb.height();
        // minimum size of face in frame
        int absoluteFaceSize = (int) (height * 0.1);
        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            // input, output, scale factor, min neighbors, flag, minimum size of output
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // loop through all faces
        Rect[] faceArray = faces.toArray();
        for (int i = 0; i < faceArray.length; i++) {
            // draw face on original frame mRgba
            Imgproc.rectangle(mRgba, faceArray[i].tl(), faceArray[i].br(), new Scalar(0, 255, 0, 255), 2);
            Rect rectCrop = new Rect(faceArray[i].x, faceArray[i].y, faceArray[i].width, faceArray[i].height);
            Mat imageROI = new Mat(mRgba, rectCrop);
            Imgproc.GaussianBlur(imageROI, imageROI, new Size(55, 55), 55);
            mRgb.copyTo(imageROI);
        }
        // rotate back original frame back to -90 degrees
        Core.flip(mRgba.t(), mRgba, 0);
        return mRgba;
    }

    public Mat drawLinesOnFrame(Mat mRgba) {
        // to detect lines first detect edges
        Mat edges = new Mat();
        Imgproc.Canny(mRgba, edges, 80, 200);

        // then detect lines in frame
        // define variable first
        // store lines
        Mat lines = new Mat();
        // Starting and ending point of lines
        Point p1 = new Point();
        Point p2 = new Point();
        double a, b;
        double x0, y0;

        Imgproc.HoughLines(edges, lines, 1.0, Math.PI / 180.0, 140);

        // then loop through each line
        for (int i = 0; i < lines.rows(); i++) {
            // for each line
            double[] vec = lines.get(i, 0);
            double rho = vec[0];
            double theta = vec[1];
            //
            a = Math.cos(theta);
            b = Math.sin(theta);
            x0 = a * rho;
            y0 = b * rho;

            // starting point and end point
            p1.x = Math.round(x0 + 1000 * (-b));
            p1.y = Math.round(y0 + 1000 * a);
            p2.x = Math.round(x0 - 1000 * (-b));
            p2.y = Math.round(y0 - 1000 * a);

            //draw line on original frame
            // draw on, start, end, color of line, thickness of line
            Imgproc.line(mRgba, p1, p2, new Scalar(255.0, 255.0, 255.0), 1, Imgproc.LINE_AA, 0);
        }
        return mRgba;
    }

}
