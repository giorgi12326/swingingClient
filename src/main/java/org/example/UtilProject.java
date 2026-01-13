package org.example;

import static org.example.SimpleMove.*;

public class UtilProject {
    public static Pair<Float>projectTo2D(float x, float y, float z) {
        x -= cameraCoords.x;
        y -= cameraCoords.y;
        z -= cameraCoords.z;

        float cosY = (float)Math.cos(-cameraRotation.y);
        float sinY = (float)Math.sin(-cameraRotation.y);
        float xr = x * cosY + z * sinY;
        float zr2 = -x * sinY + z * cosY;
        x = xr;
        z = zr2;

        float cosX = (float)Math.cos(cameraRotation.x);
        float sinX = (float)Math.sin(cameraRotation.x);
        float yr = y * cosX - z * sinX;
        float zr = y * sinX + z * cosX;
        y = yr;
        z = zr;

        if(z < 0) return null;

        float d = FOV * x/z;
        float t = FOV * y/z;

//        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null; // only sohw visible nodes , wihtout it things in front of camera z is drawn off screen

        return new Pair<>(SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f));
    }
    public static  Pair<Float>projectTo2DWithoutRotatingAgainstCamera(float x, float y, float z) {
        x -= cameraCoords.x;
        y -= cameraCoords.y;
        z -= cameraCoords.z;

        float d = x/z;
        float t = y/z;

//        if(Math.abs(y) > Math.abs(z) || Math.abs(x) > Math.abs(z)) return null; // only sohw visible nodes , wihtout it things in front of camera z is drawn off screen

        return new Pair<>(SCREEN_WIDTH * d/2f + SCREEN_WIDTH/2f, ((SCREEN_HEIGHT*t)/2f) + (SCREEN_HEIGHT/2f));
    }
}
