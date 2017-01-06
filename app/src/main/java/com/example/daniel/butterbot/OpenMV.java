package com.example.daniel.butterbot;

import android.os.AsyncTask;
import android.util.Log;


/**
 * Created by Daniel on 12/31/2016.
 */


public class OpenMV extends AsyncTask<Void, Object, Void> {

    public static final int TARGET_LOST = 0;
    public static final int TARGET_LOCK = 1;

    //running state
    private boolean running = true;
    private boolean tracking_enabled = false;
    private boolean target_lock = false;

    //connection to butterbot
    private Command conn;

    //butterbot state
    private int neck_pos, throttle;

    //timeout for lost lock
    private int track_timeout;

    //control settings
    int x_min,x_max,target_x, y_min,y_max, target_y;
    double x_scaler, y_scaler;

    //camera state
    private int curr_x, curr_y;
    private long last_packet = 0;
    private boolean new_data = false;

    //listener
    private OnOpenMVUpdate listener = null;


    public OpenMV(Command conn, int track_timeout){
        this.conn = conn;
        this.track_timeout = track_timeout;
    }

    public void setTarget(int target_x, int target_y){
        this.target_x = target_x;
        this.target_y = target_y;
    }

    public void config_x(double x_scaler, int x_min, int x_max){
        this.x_scaler = x_scaler;
        this.x_min = x_min;
        this.x_max = x_max;
    }

    public void config_y(double y_scaler, int y_min, int y_max){
        this.y_scaler = y_scaler;
        this.y_min = y_min;
        this.y_max = y_max;
    }

    public void setOpenMVUpdateListener(OnOpenMVUpdate listener) {
        this.listener = listener;
    }

    public void enable_tracking(){
        tracking_enabled = conn.forceSend(conn.OPENMV_ENABLE, 1, 0);
        neck_pos = 1700;
        conn.forceSend(conn.CTRL_NECK,neck_pos,0);
        new_data = false;
    }

    public void disable_tracking(){
        tracking_enabled = !conn.forceSend(conn.OPENMV_ENABLE, 0, 0);
        neck_pos = 1500;
        conn.forceSend(conn.CTRL_NECK,neck_pos,0);
    }

    public boolean isTracking_enabled(){return tracking_enabled;}

    public void consume(int x, int y){
        curr_x = x;
        curr_y = y;
        new_data = true;
        last_packet = System.currentTimeMillis();
    }

    public void stop(){running = false;}

    @Override
    protected Void doInBackground(Void... Params) {
        while(running){
            if(tracking_enabled){
                //update tracking state
                if(new_data && !target_lock){
                    target_lock = true;
                    publishProgress(TARGET_LOCK);
                }
                else if(System.currentTimeMillis() - last_packet > track_timeout) {
                    target_lock = false;
                    publishProgress(TARGET_LOST);
                }
                //track target
                if(target_lock) {
                    //update neck and base
                    if(new_data) {
                        int x_error = target_x - curr_x;
                        int y_error = target_y - curr_y;

                        neck_pos = Math.min(Math.max(neck_pos + (int)(y_error * y_scaler), y_min), y_max);
                        throttle = Math.min(Math.max((int)(x_error * x_scaler), x_min), x_max);

                        conn.forceSend(conn.CTRL_NECK, neck_pos,0);
                        conn.forceSend(conn.CTRL_MOTORS, 255 + throttle, 255 - throttle);
                    }
                    //decay throttle when we can't see target but haven't timed out
                    else{
                        throttle = throttle/2;
                    }
                    try {
                        Thread.sleep(1000/25); //25hz update rate
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        if (tracking_enabled) {
            int type = (int) values[0];
            switch (type) {
                case TARGET_LOCK:
                    listener.target_lock();
                    break;
                case TARGET_LOST:
                    listener.target_lost();
                    break;
            }
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnOpenMVUpdate {
        public void target_lock();
        public void target_lost();
    }
}
