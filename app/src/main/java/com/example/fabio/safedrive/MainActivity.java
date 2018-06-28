package com.example.fabio.safedrive;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class MainActivity extends AppCompatActivity {

    public static boolean FIRST_TIME_OPEN = true;
    // Variabili Camera
    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    CountDownTimer countDownTimer;

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

    //Per upload immagine
    private String uploadUrl = "http://safedrive.altervista.org/updateinfo.php"; // indirizzo web di updateinfo
    private Bitmap bitmap;
    private ArrayList<String> imageList = new ArrayList<>();
    private HashMap<String,String> imageMap = new HashMap<>();
    Button buttonUploadTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        textView = (TextView) findViewById(R.id.textView);

        /** try the button Upload
        buttonUploadTest = (Button) findViewById(R.id.buttonUploadTest);



        buttonUploadTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bitmap = getImageFromGalleries(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + File.separator + "snap1.jpg")));
                uploadImage(bitmap);
            }
        });**/



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

              //    if (qrCodes.valueAt(0).displayValue.equals("ciao")) {
                    if (qrCodes.valueAt(0).displayValue.equals("www.abbonationline.it/elle18")) {
                        System.out.println(qrCodes.valueAt(0).displayValue);
                        cameraSource.takePicture(null,mPictureSourceCallback);
                     //   startTimerCameraSource();
                    //    uploadImages(imageList);
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

                 // cameraSource.release(); // Chiudo la cameraSource
                    surfaceView.setVisibility(View.GONE); // Cosi distruggo la Surface e di consegienza po invoco camera.release() // non so se è meglio fare prima una cosa o l'altra.
                    textView.setVisibility(View.GONE);


                    startCamera();
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
        String formato = ".jpg";
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
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
            imageList.add(outputFile.getPath());
            imageMap.put(outputFile.getPath(), imageFileName);
            return outputFile;
        }
    }

    public void captureImage(View v){
        if(camera != null){
            camera.takePicture(null,null,mPictureCallback);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (camera != null){
            //              mCamera.setPreviewCallback(null);
            showCamera.getHolder().removeCallback(showCamera);
            camera.release();        // release the camera for other applications
            camera = null;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (camera == null) {
            if(FIRST_TIME_OPEN){
                FIRST_TIME_OPEN = false;
                return;
            }else
                startCamera();

        }
    }

    public void startCamera(){
        camera = Camera.open(CAMERA_FACING_FRONT); // Apro la camera normale
        showCamera = new ShowCamera(getApplicationContext(),camera);
        frameLayout.addView(showCamera);


        int nPhoto = 5;

       // for(int i = 0; i < nPhoto ; i++){ // sto lavorando sul timer per le foto
            startTimer();

            //stoptimer();

       // }

           // Toast.makeText(this,"Foto numero "+i+" scattata", Toast.LENGTH_LONG).show();


    }

    public void startTimer(){

        final int numeroFotoDaScattare = 2;
        final int countDownInterval = 3000;
        final int millisInFuture = (numeroFotoDaScattare+1) * countDownInterval;

        countDownTimer =
                new CountDownTimer(millisInFuture,countDownInterval){

                    @Override
                    public void onFinish() {
                        System.out.println(numeroFotoDaScattare +" foto  sono state scattate correttamente");
                        Toast.makeText(MainActivity.this, numeroFotoDaScattare +" foto  sono state scattate correttamente", Toast.LENGTH_SHORT).show();
                        uploadImages(imageMap);
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        System.out.println(millisUntilFinished);
                        camera.takePicture(null, null, mPictureCallback);
                        int i = ((millisInFuture-(int)millisUntilFinished)/(countDownInterval))+1;
                        System.out.println("Foto numero n° " + i +" scattata ");
                        Toast.makeText(MainActivity.this, "Foto numero n° " + i +" scattata ", Toast.LENGTH_SHORT).show();
                    }

                }.start();
    }

    public void startTimerCameraSource(){

        final int numeroFotoDaScattare = 4;
        final int countDownInterval = 3000;
        final int millisInFuture = (numeroFotoDaScattare+1) * countDownInterval;

        countDownTimer =
                new CountDownTimer(millisInFuture,countDownInterval){

                    @Override
                    public void onFinish() {
                        System.out.println(numeroFotoDaScattare +" foto  sono state scattate correttamente");
                        Toast.makeText(MainActivity.this, numeroFotoDaScattare +" foto  sono state scattate correttamente", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        System.out.println(millisUntilFinished);
                        cameraSource.takePicture(null,mPictureSourceCallback);
                        int i = ((millisInFuture-(int)millisUntilFinished)/(countDownInterval))+1;
                        System.out.println("Foto numero n° " + i +" scattata ");
                        Toast.makeText(MainActivity.this, "Foto numero n° " + i +" scattata ", Toast.LENGTH_SHORT).show();
                    }

                }.start();
    }

    public void stoptimer(){
        countDownTimer.cancel();
    }

    public Bitmap getImageFromGalleries(Uri path){
        try {
            return MediaStore.Images.Media.getBitmap(getContentResolver(),path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void uploadImageOLd(final Bitmap bitmap){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, uploadUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String Response = jsonObject.getString("response");
                            Toast.makeText(MainActivity.this,Response,Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String,String>();
                params.put("name","snap1"); //aggiungere poi nome dinamico
                params.put("image",imageToString(bitmap));
                return params;
            }
        };

        MySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
    }

    private void uploadImages(final ArrayList<String> imageList){
        final MyCommand myCommand = new MyCommand(getApplicationContext());

        for(final String imagePath : imageList){
            final StringRequest stringRequest = new StringRequest(Request.Method.POST, uploadUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                String Response = jsonObject.getString("response");
                                Toast.makeText(MainActivity.this,Response,Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            })
            {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    Map<String,String> params = new HashMap<String,String>();
                    params.put("name","foto" ); //aggiungere poi nome dinamico

                    params.put("image",imageToString(getImageFromGalleries(Uri.fromFile(new File(imagePath)))));
                    return params;
                }
            };

            myCommand.add(stringRequest);
            Toast.makeText(MainActivity.this,imagePath +" Aggiunta",Toast.LENGTH_SHORT).show();


        }
        myCommand.execute();
        Toast.makeText(MainActivity.this," Upload riuscito",Toast.LENGTH_SHORT).show();

    }

    private void uploadImages(final HashMap<String,String> imageMap){
        final MyCommand myCommand = new MyCommand(getApplicationContext());

        for(final String imagePath : imageMap.keySet()){
            final StringRequest stringRequest = new StringRequest(Request.Method.POST, uploadUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                String Response = jsonObject.getString("response");
                                Toast.makeText(MainActivity.this,Response,Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            })
            {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {

                    Map<String,String> params = new HashMap<String,String>();
                    params.put("name",imageMap.get(imagePath)); //aggiungere poi nome dinamico

                    params.put("image",imageToString(getImageFromGalleries(Uri.fromFile(new File(imagePath)))));
                    return params;
                }
            };

            myCommand.add(stringRequest);
            Toast.makeText(MainActivity.this,imagePath +" Aggiunta",Toast.LENGTH_SHORT).show();
            //MySingleton.getmInstance(MainActivity.this).addToRequestQue(stringRequest);

        }
        myCommand.execute();
        Toast.makeText(MainActivity.this," Upload iniziato",Toast.LENGTH_SHORT).show();

    }


    private String imageToString(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream); // forse posso evitarlo dato che ho gia il JPEG
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes,Base64.DEFAULT);
    }
}
