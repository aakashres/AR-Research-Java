package com.example.researchar.helper;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class FrameBuffer {
    private int buffer_length;
    private ArrayList<Mat> frame_buffer = new ArrayList<Mat>();

    public ArrayList<Mat> getFrame_buffer() {
        return frame_buffer;
    }

    public void setFrame_buffer(ArrayList<Mat> frame_buffer) {
        this.frame_buffer = frame_buffer;
    }

    public FrameBuffer(int buffer_length) {
        this.buffer_length = buffer_length;
    }

    public int getBuffer_length() {
        return buffer_length;
    }

    public void setBuffer_length(int buffer_length) {
        this.buffer_length = buffer_length;
    }

    public boolean isBufferFull(){
        return  frame_buffer.size() == this.buffer_length;
    }

    public boolean isBufferEmpty(){
        return  frame_buffer.size() == 0;
    }

    public boolean addFrames(Mat frame){
        if (! this.isBufferFull()){
            this.frame_buffer.add(frame);
            return true;
        }
        return false;
    }

    public Mat getFrames(){
        if(!this.isBufferEmpty() & this.isBufferFull()){
            return this.frame_buffer.get(0);
        }
        return new Mat(0,0, CvType.CV_8UC4);
    }

    public void updateFrameBufferLength(int type, int length){
        this.buffer_length += type*length;
    }
}
