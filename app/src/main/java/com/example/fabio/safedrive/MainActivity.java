package com.example.fabio.safedrive;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // Variabili Camera
    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;

    // Variabili CameraSource per scan QR
    SurfaceView surfaceView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;
    TextView textView;

    // Costanti
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;

    public static final int[] FULL_HD_1080p = {1920, 1080};
    public static final int[] HD_720p = {1280, 720};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);


        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(HD_720p[0], HD_720p[1]).setAutoFocusEnabled(true)
                .setFacing(CAMERA_FACING_FRONT).setRequestedFps(15).build();


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            return;
                    }
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.release();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if(qrCodes.size() != 0){
                    System.out.println(qrCodes);
                    System.out.println(qrCodes.valueAt(0).displayValue);

                    if (qrCodes.valueAt(0).displayValue.equals("ciao")) {
                        System.out.println(qrCodes.valueAt(0).displayValue);
                        cameraSource.takePicture(null,mPictureSourceCallback);
                    }
                }
            }
        });

        //Open the camera

      /*  camera = Camera.open(CAMERA_FACING_FRONT);
        showCamera = new ShowCamera(this,camera);
        frameLayout.addView(showCamera);
      */


    }

    CameraSource.PictureCallback mPictureSourceCallback = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data) {
            File picture_file = getOutputMediaFile();

            if(picture_file == null){
                return;
            }else{
                try {
                    FileOutputStream fos = new FileOutputStream(picture_file);
                    fos.write(data);
                    fos.close();
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[] { picture_file.getPath() }, new String[] { "image/jpeg" }, null);

                 //   cameraSource.release(); // Chiudo la cameraSource
                    surfaceView.setVisibility(View.GONE); // Cosi distruggo la Surface e di consegienza po invoco camera.release()
                                                          // non so se è meglio fare prima una cosa o l'altra.
                    camera = Camera.open(CAMERA_FACING_FRONT); // Apro la camera normale
                    showCamera = new ShowCamera(getApplicationContext(),camera);
                    frameLayout.addView(showCamera);
                  //camera.startPreview();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File picture_file = getOutputMediaFile();

            if(picture_file == null){
                return;
            }else{
                try {
                    FileOutputStream fos = new FileOutputStream(picture_file);
                    fos.write(data);
                    fos.close();
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[] { picture_file.getPath() }, new String[] { "image/jpeg" }, null);

                    camera.startPreview();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    private File getOutputMediaFile() {
     // Creo un nome unico per ogni foto associandogli un TimeStamp
        String formato = "jpg";
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".";
        String state = Environment.getExternalStorageState();
        if(!state.equals(Environment.MEDIA_MOUNTED)){
            return null;
        }
        else{
            File folder_gui = new File(Environment.getExternalStorageDirectory() + File.separator + "GUI");

            if(!folder_gui.exists()){
                folder_gui.mkdirs();
            }

            File outputFile = new File(folder_gui,imageFileName + formato);
            return outputFile;
        }
    }

    public void captureImage(View v){
        if(camera != null){
            camera.takePicture(null,null,mPictureCallback);
        }
    }
}
