package com.example.researchar.helper;

import org.opencv.core.Rect;

public class Objects {
    private Rect boundingBox;
    private String objectName;

    public Objects(Rect boundingBox, String objectName) {
        this.boundingBox = boundingBox;
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Rect getBoundingBox() {
        return this.boundingBox;
    }

    public void setBoundingBox(Rect boundingBox) {
        this.boundingBox = boundingBox;
    }
}
