package com.example.researchar.helper;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.util.ArrayList;

public class PersistObject {
    private final String TAG = "Persist Object";
    public PersistObject(){
        Log.i(TAG, "Instantiated new "+ this.getClass());
    }

    public void saveObject(String type, MatOfKeyPoint keyPoint, Mat descriptor){
        Log.d(TAG, "Saving Object Identifier");
        Log.d(TAG, "Object Type: " + type);
        Log.d(TAG, "Object Keypoint: " + keyPoint);
        Log.d(TAG, "Object Descriptor: " + descriptor);
    }

    public ArrayList<ObjectIdentifier> getObjects(){
        return new ArrayList<ObjectIdentifier>();
    }
}
