package com.example.researchar.helper;


import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Tracker;

import java.util.ArrayList;

public class ObjectTracker {
    private ArrayList<Objects> trackedObjects;
    private Tracker tracker;
    private Boolean retVal;

    public ObjectTracker() {
        this.trackedObjects = new ArrayList<>();
    }

    public void addTrackingObject(Mat mrgb, Objects obj){
        trackedObjects.add(obj);
        tracker.init(mrgb,obj.getBoundingBox());
    }

    public void updateBox(Mat mrgb){
        for(Objects obj: trackedObjects){
            Rect box = obj.getBoundingBox();
            retVal = tracker.update(mrgb, box);
            obj.setBoundingBox(box);
        }
    }

    public Mat drawObjects(Mat mrgb){
        for(Objects obj: trackedObjects){
            Imgproc.rectangle(mrgb, obj.getBoundingBox(), new Scalar(255,155,155), 2);
            Imgproc.putText(mrgb, obj.getObjectName(), obj.getBoundingBox().tl(), 3, 1, new Scalar(100, 100, 100),2);
        }
        return mrgb;
    }




}
