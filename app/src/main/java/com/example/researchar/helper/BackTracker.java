package com.example.researchar.helper;

import org.opencv.core.Mat;

import java.util.ArrayList;

public class BackTracker extends ObjectTracker{
    public BackTracker(ArrayList<Objects> trackedObjects) {
        super();
    }

    public void backTrackObjects(FrameBuffer buffer){
        ArrayList<Mat> frames = buffer.getFrame_buffer();
        for(int i = frames.size()-1 ;  i >= 0 ; i--){
            this.updateBox(frames.get(i));
        }
    }
}
