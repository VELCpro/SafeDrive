package com.example.fabio.safedrive;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import BACtrackAPI.API.BACtrackAPI;
import BACtrackAPI.API.BACtrackAPICallbacks;
import BACtrackAPI.Constants.BACTrackDeviceType;
import BACtrackAPI.Exceptions.LocationServicesNotEnabledException;
import BACtrackAPI.Mobile.Constants.Errors;
import BACtrackAPI.Exceptions.BluetoothLENotSupportedException;
import BACtrackAPI.Exceptions.BluetoothNotEnabledException;



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
    private String uploadUrlForResult = "http://safedrive.altervista.org/"; // indirizzo web di updateinfo
    private Bitmap bitmap;
    private ArrayList<String> imageList = new ArrayList<>();
    private HashMap<String,String> imageMap = new HashMap<>();
    Button buttonUploadTest;

    //Per BACTrack
    private String QR_CODE_CONTENT;
    private static final int NO_RESULT = 10000;
    private float BAC_RESULT = NO_RESULT;
    private boolean UPLOAD_FINISH = false;
    public static final String apiKey = "f2c64398fb774a5eb2e4adcfe9309f";

    private static final byte PERMISSIONS_FOR_SCAN = 100;

    private static String TAG = "MainActivity";

    private TextView statusMessageTextView;
    private TextView batteryLevelTextView;

    private BACtrackAPI mAPI;
    private String currentFirmware;
    private Button serialNumberButton;
    private Button useCountButton;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        this.statusMessageTextView = (TextView)this.findViewById(R.id.status_message_text_view_id);
        textView = (TextView) findViewById(R.id.textView);

        createBACTrackProcess();
        connectNearest();


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
                    QR_CODE_CONTENT = qrCodes.valueAt(0).displayValue;
              //    if (qrCodes.valueAt(0).displayValue.equals("ciao")) {
                    if (qrCodes.valueAt(0).displayValue.equals("www.abbonationline.it/elle18")) { // cancellare poi l' IF
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
    } // salvo nella imageMap<String,String> (imagePath, imageFileName)

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
        startTimer();


       //mAPI.bacTrackAPICallbacks.BACtrackCountdown(2);

        startBlowProcess();
        //faccio in modo che i due processi si aspettino
      /**  while(BAC_RESULT == NO_RESULT || UPLOAD_FINISH == false){
            System.out.println("Aspetto un risultato");
        }**/

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

                                UPLOAD_FINISH = true; // dico che è finito l'upload perchè ho ricevuto una risposta

                                if(BAC_RESULT != NO_RESULT){
                                    uploadDati();
                                }
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

   /**
    *
    *
    *
    *
    *
    *
    *
    *
    **/

    // metodi BACTrack

    //  Copyright (c) 2018 KHN Solutions LLC. All rights reserved.








    /**   @Override
    protected void onCreate(Bundle savedInstanceState) {
            //super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);






        }**/


    public void createBACTrackProcess(){
        try {
            mAPI = new BACtrackAPI(this, mCallbacks, apiKey);
            mContext = this;
        } catch (BluetoothLENotSupportedException e) {
            e.printStackTrace();
            this.setStatus(R.string.TEXT_ERR_BLE_NOT_SUPPORTED);
        } catch (BluetoothNotEnabledException e) {
            e.printStackTrace();
            this.setStatus(R.string.TEXT_ERR_BT_NOT_ENABLED);
        } catch (LocationServicesNotEnabledException e) {
            e.printStackTrace();
            this.setStatus(R.string.TEXT_ERR_LOCATIONS_NOT_ENABLED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_FOR_SCAN: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /**
                     * Only start scan if permissions granted.
                     */
                    mAPI.connectToNearestBreathalyzer();
                }
            }
        }
    }

    public void connectNearest() {
        if (mAPI != null) {
            setStatus(R.string.TEXT_CONNECTING);
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_FOR_SCAN);
            } else {
                /**
                 * Permission already granted, start scan.
                 */
                mAPI.connectToNearestBreathalyzer();
            }
        }
    }

    public void disconnectClicked(View v) {
        if (mAPI != null) {
            mAPI.disconnect();
        }
    }

    public void getFirmwareVersionClicked(View v) {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.getFirmwareVersion();
        }
        if (!result)
            Log.e(TAG, "mAPI.getFirmwareVersion() failed");
        else
            Log.d(TAG, "Firmware version requested");
    }

    public void getSerialNumberClicked(View view) {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.getSerialNumber();
        }
        if (!result)
            Log.e(TAG, "mAPI.getSerialNumber() failed");
        else
            Log.d(TAG, "Serial Number requested");
    }

    public void requestUseCountClicked(View view) {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.getUseCount();
        }
        if (!result)
            Log.e(TAG, "mAPI.requestUseCount() failed");
        else
            Log.d(TAG, "Use count requested");
    }

    public void requestBatteryVoltageClicked(View view) {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.getBreathalyzerBatteryVoltage();
        }
        if (!result)
            Log.e(TAG, "mAPI.getBreathalyzerBatteryVoltage() failed");
        else
            Log.d(TAG, "Battery voltage requested");
    }

    // prendi
    public void startBlowProcess() {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.startCountdown();
        }
        if (!result)
            Log.e(TAG, "mAPI.startCountdown() failed");
        else
            Log.d(TAG, "Blow process start requested");
    }

    private void setStatus(int resourceId) {
        this.setStatus(this.getResources().getString(resourceId));
    }

    private void setStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, message);
                statusMessageTextView.setText(String.format("Status:\n%s", message));
            }
        });
    }

    private void setBatteryStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, message);
                batteryLevelTextView.setText(String.format("\n%s", message));
            }
        });
    }

    private class APIKeyVerificationAlert extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return urls[0];
        }

        @Override
        protected void onPostExecute(String result) {
            AlertDialog.Builder apiApprovalAlert = new AlertDialog.Builder(mContext);
            apiApprovalAlert.setTitle("API Approval Failed");
            apiApprovalAlert.setMessage(result);
            apiApprovalAlert.setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            mAPI.disconnect();
                            setStatus(R.string.TEXT_DISCONNECTED);
                            dialog.cancel();
                        }
                    });

            apiApprovalAlert.create();
            apiApprovalAlert.show();
        }
    }

    private final BACtrackAPICallbacks mCallbacks = new BACtrackAPICallbacks() {

        @Override
        public void BACtrackAPIKeyDeclined(String errorMessage) {
            APIKeyVerificationAlert verify = new APIKeyVerificationAlert();
            verify.execute(errorMessage);
        }

        @Override
        public void BACtrackAPIKeyAuthorized() {

        }

        @Override
        public void BACtrackConnected(BACTrackDeviceType bacTrackDeviceType) {
            setStatus(R.string.TEXT_CONNECTED);
        }

        @Override
        public void BACtrackDidConnect(String s) {
            setStatus(R.string.TEXT_DISCOVERING_SERVICES);
        }

        @Override
        public void BACtrackDisconnected() {
            setStatus(R.string.TEXT_DISCONNECTED);
            setBatteryStatus("");
            setCurrentFirmware(null);
        }
        @Override
        public void BACtrackConnectionTimeout() {

        }

        @Override
        public void BACtrackFoundBreathalyzer(BACtrackAPI.b BACtrackDevice) {
            Log.d(TAG, "Found breathalyzer : " + BACtrackDevice.toString());
        }

        @Override
        public void BACtrackCountdown(int currentCountdownCount) {
            setStatus(getString(R.string.TEXT_COUNTDOWN) + " " + currentCountdownCount);
        }

        @Override
        public void BACtrackStart() {
            setStatus(R.string.TEXT_BLOW_NOW);
        }

        @Override
        public void BACtrackBlow() {
            setStatus(R.string.TEXT_KEEP_BLOWING);
        }

        @Override
        public void BACtrackAnalyzing() {
            setStatus(R.string.TEXT_ANALYZING);
        }

        @Override
        public void BACtrackResults(float measuredBac) {
            setStatus(getString(R.string.TEXT_FINISHED) + " " + measuredBac);
            //qua salvare il risultato dell'analisi del BAC
            BAC_RESULT = measuredBac;
            if(UPLOAD_FINISH == true){
                uploadDati();
            }
        }

        @Override
        public void BACtrackFirmwareVersion(String version) {
            setCurrentFirmware(version);
            setStatus(getString(R.string.TEXT_FIRMWARE_VERSION) + " " + version);
        }

        @Override
        public void BACtrackSerial(String serialHex) {
            setStatus(getString(R.string.TEXT_SERIAL_NUMBER) + " " + serialHex);
        }

        @Override
        public void BACtrackUseCount(int useCount) {
            Log.d(TAG, "UseCount: " + useCount);
            // C6/C8 bug in hardware does not allow getting use count
            if (useCount == 4096)
            {
                setStatus("Cannot retrieve use count for C6/C8 devices");
            }
            else
            {
                setStatus(getString(R.string.TEXT_USE_COUNT) + " " + useCount);
            }
        }

        @Override
        public void BACtrackBatteryVoltage(float voltage) {

        }

        @Override
        public void BACtrackBatteryLevel(int level) {
            setBatteryStatus(getString(R.string.TEXT_BATTERY_LEVEL) + " " + level);

        }

        @Override
        public void BACtrackError(int errorCode) {
            if (errorCode == Errors.ERROR_BLOW_ERROR)
                setStatus(R.string.TEXT_ERR_BLOW_ERROR);
        }
    };


    public void setCurrentFirmware(@Nullable String currentFirmware) {
            this.currentFirmware = currentFirmware;

            String[] firmwareSplit = new String[0];
            if (currentFirmware != null) {
                firmwareSplit = currentFirmware.split("\\s+");
            }
            if (firmwareSplit.length >= 1 && firmwareSplit[0].contains("_") || firmwareSplit.length >= 1 && firmwareSplit[0].contains("-")){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (serialNumberButton != null) {
                            serialNumberButton.setVisibility(View.VISIBLE);
                        }
                        if (useCountButton != null) {
                            useCountButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
                return;
            }
            else if (firmwareSplit.length >= 1
                    && Long.valueOf(firmwareSplit[0]) >= Long.valueOf("201510150003")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (serialNumberButton != null) {
                            serialNumberButton.setVisibility(View.VISIBLE);
                        }
                        if (useCountButton != null) {
                            useCountButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (serialNumberButton != null) {
                            serialNumberButton.setVisibility(View.GONE);
                        }
                        if (useCountButton != null) {
                            useCountButton.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

    public void uploadDati(){
        final MyCommand myCommand = new MyCommand(getApplicationContext());

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, uploadUrlForResult,
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
                String formato = ".jpg"; // vedere se serve o no
                Map<String,String> params = new HashMap<String,String>();
                params.put("QRCode",QR_CODE_CONTENT);
                params.put("foto1",imageMap.get(0) + formato);
                params.put("foto2",imageMap.get(1) + formato);
                params.put("foto3",imageMap.get(2) + formato);
                params.put("BACResult",Float.toString(BAC_RESULT));

                return params;
            }
        };

        myCommand.add(stringRequest);
        //MySingleton.getmInstance(MainActivity.this).addToRequestQue(stringRequest);

        myCommand.execute();
        Toast.makeText(MainActivity.this,"Upload dati per confronto",Toast.LENGTH_SHORT).show();
    }
}
