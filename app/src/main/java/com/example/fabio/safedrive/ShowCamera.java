package com.example.fabio.safedrive;

import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback{


    Camera camera;
    SurfaceHolder holder;


    public ShowCamera(Context context,Camera camera) {
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);
    }

   @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = camera.getParameters();

        //fix orientation of the camera

        params.set("orientation","portrait");
        camera.setDisplayOrientation(90); // voglio che il display sia verticale
        params.setRotation(270); // Foto con la FRONT_FACING_CAMERA non sottosopra

        camera.setParameters(params);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();



    }
}
