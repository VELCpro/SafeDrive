package com.example.fabio.safedrive;

import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

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
        camera.stopPreview();
        camera.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = camera.getParameters();

        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size mSize = null;

        int width = 0;
        int height = 0;

        for(Camera.Size size : sizes){
            if(size.width > width && size.height > height){
                width = size.width;
                height = size.height;
                mSize = size;
            }
         }

        //fix orientation of the camera

        params.set("orientation","portrait");
        camera.setDisplayOrientation(90); // voglio che il display sia verticale
        params.setRotation(270); // Foto con la FRONT_FACING_CAMERA non sottosopra

        params.setJpegQuality(100); // Max JPEG Quality 100, Min 0
        params.setPictureSize(mSize.width,mSize.height); // metti la risoluzione piu grande sempre in width
        camera.setParameters(params);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();



    }
}
