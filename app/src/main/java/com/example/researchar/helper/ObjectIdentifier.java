package com.example.researchar.helper;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;

public class ObjectIdentifier {
    private String objectType;
    private MatOfKeyPoint objectKeypoints;
    private Mat objectDescriptors;
    private Mat mRbg;
    private Rect bBox;
    public ObjectIdentifier(String type,  MatOfKeyPoint keyPoint, Mat descriptor, Mat object, Rect boundingBox){
        objectType = type;
        objectKeypoints = keyPoint;
        objectDescriptors = descriptor;
        mRbg = object;
        bBox = boundingBox;
    }

    public String getObjectType(){
        return this.objectType;
    }
    public MatOfKeyPoint getObjectKeypoints(){
        return this.objectKeypoints;
    }
    public Mat getObjectDescriptors(){
        return this.objectDescriptors;
    }
    public Rect getbBox() {return this.bBox;}
    public Mat getmRbg(){
        return this.mRbg;
    }
}
