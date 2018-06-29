package com.example.fabio.safedrive;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;

import java.util.ArrayList;
import java.util.Timer;

public class MyCommand<T> {

    ArrayList<Request<T>> requestList = new ArrayList<>();

    private Context context;

    public MyCommand(Context context){
        this.context = context;
    }

    public void add(Request<T> request){
        requestList.add(request);
    }

    public void remove(Request<T> request){
        requestList.remove(request);
    }

    public void execute(){
        int i = 0;

        for(Request<T> request : requestList){
            // bisogna toglere il limite di timeout della connessione e successivo riprova ad uplodare il file
            request.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT

            ));
            MySingleton.getInstance(context).addToRequestQueue(request);
            Toast.makeText(this.context,"foto"+i,Toast.LENGTH_SHORT).show();
            System.out.println("foto"+i); // nel secondo caso invia la richiesta anche se non è una foto ma le 3 foto codice qr e bac result
            i++;

        }
    }
}
