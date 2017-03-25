package com.example.daniel.butterbot;

import java.io.*;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.sql.Connection;


/**
 * Created by Daniel on 11/21/2016.
 */

public class ParticleCloud extends AsyncTask<Void, Void, String[]> {

    boolean running = true;
    String access_token;
    Context context;
    private OnConnectionInfo listener = null;

    public ParticleCloud(Context context){
        this.context = context;
    }

    public void setConnectionInfoListener(OnConnectionInfo listener) {
        this.listener = listener;
    }

    public void set_access_token(String access_token){this.access_token = access_token;}

    public void close(){
        running = false;
    }

    @Override
    protected String[] doInBackground(Void... params) {
        try{
            String url = "https://api.particle.io/v1/devices/events?access_token=" + access_token;
            URL particle = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(particle.openStream()));

            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
                inputLine = "{" + inputLine + "}";
                try {
                    JSONObject jsonObject = new JSONObject(inputLine);
                    JSONObject data = jsonObject.getJSONObject("data");
                    String raw = data.getString("data");
                    Log.d("Particle", "recv: " + raw);

                    String[] connection_info = raw.split(":");
                    if(connection_info.length >= 3){
                        return connection_info;
                    }
                }
                catch (JSONException e){}
            }
            in.close();
        }
        catch(Exception e){
            Log.e("Particle","error",e);
            return new String[0];
        }
        return null;
    }

    @Override
    protected void onPostExecute(String[] result){
        if(result.length >= 3){
            listener.connectionInfo(result[0],result[1],Integer.parseInt(result[2]));
        }
        //error
        else{
            listener.connectionError("");
        }
    }

    //Allow us to pass back info main Activity
    public interface OnConnectionInfo {
        public void connectionInfo(String ssid, String ip, int port);
        public void connectionError(String error);
    }
}