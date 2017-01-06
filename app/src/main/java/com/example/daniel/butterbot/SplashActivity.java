package com.example.daniel.butterbot;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VerticalSeekBar;

import com.android.debug.hv.ViewServer;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.Iterator;
import java.util.List;


/**
 * TODO
 * Accelerometer past vertical starts to decrease
 * preprogrammed actions
 * OPENMV support
 * Hidden access token/ dialog prompt on startup
 * ADDITIONS
 * * Connect to the same wifi network upon receiving connectioninfo
 * * Voice control
 * * Custom popup menu ui
 * * Custom AlertDialog ui
 * * Proper splash screen
 */

public class SplashActivity extends AppCompatActivity {

    private RelativeLayout root;
    private RelativeLayout head;
    private TextView warning;
    private ImageView eye;
    private LinearLayout status_container;
    private TextView author;
    private TextView title;
    private TextView status;
    private RelativeLayout joystick;
    private ImageView pad;
    private ImageView stick;
    private ImageButton mic;
    private ImageButton tilt;
    private VerticalSeekBar slider;
    private ImageView battery;
    private AVLoadingIndicatorView dots;

    private Vibrator vib;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private DisplayMetrics metrics;

    private JoyStick js;
    private TCPClient client;
    private ParticleStream cloud;
    private Command bb;
    private OpenMV camera;

    private float[] magnetic = null;
    private float[] gravity = null;

    private boolean transitioned = false;
    private boolean neck_enabled = false;
    private boolean neck_moving = false;
    private int neck_resting_pos = 1500; //%50 percent
    private float phone_tilt, start_phone_tilt;
    private int neck_pos = neck_resting_pos;
    private int slider_value = 500; //%50

    private static final int MAX_CMD_RATE = 30; //max rate(hz) to send commands over tcp
    private static final int TCP_TIMEOUT = 1500; // tcp heartbeat timeout(milliseconds)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        //Get screen elements
        root = (RelativeLayout) findViewById(R.id.activity_splash);
        head = (RelativeLayout) findViewById(R.id.head);
        warning = (TextView) findViewById(R.id.warning);
        eye = (ImageView) findViewById(R.id.eye);
        status_container = (LinearLayout) findViewById(R.id.status_container);
        status = (TextView) findViewById(R.id.status);
        dots = (AVLoadingIndicatorView) findViewById(R.id.dots);
        author = (TextView) findViewById(R.id.author);
        title = (TextView) findViewById(R.id.title);
        joystick = (RelativeLayout) findViewById(R.id.joystick);
        pad = (ImageView) findViewById(R.id.pad);
        stick = (ImageView) findViewById(R.id.stick);
        mic = (ImageButton) findViewById(R.id.mic);
        tilt = (ImageButton) findViewById(R.id.tilt);
        slider = (VerticalSeekBar) findViewById(R.id.slider);
        battery = (ImageView) findViewById(R.id.battery);
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);


        //set fonts
        Typeface harlow_font = Typeface.createFromAsset(getAssets(), "HARLOWSI.TTF");
        Typeface msy_font = Typeface.createFromAsset(getAssets(), "msyhl.ttc");
        title.setTypeface(harlow_font);
        status.setTypeface(harlow_font);
        warning.setTypeface(harlow_font);
        author.setTypeface(msy_font);

        //get services
        vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        //Initialize objects
        client = new TCPClient();
        cloud = new ParticleStream(getApplicationContext(), "https://api.particle.io/v1/devices/events?access_token=f7d322194fbf5eed19b6c47b35a12765ab87fda6");
        bb = new Command(client,MAX_CMD_RATE);
        js = new JoyStick(getApplicationContext(), joystick, pad, stick);
        camera = new OpenMV(bb,100);



        //configure TCP
        client.setTCPUpdateListener(new TCPClient.OnTCPUpdate() {

            @Override
            public void connected() {
                if(!transitioned){
                    transitionLayout();
                }
                warning.setVisibility(View.INVISIBLE);
                Toast.makeText(SplashActivity.this, "Connected!", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void disconnected() {
                warning.setVisibility(View.VISIBLE);
                warning.setText("Disconnected");
                warning.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Butterbot_red));
                neck_enabled = false;
            }
            @Override
            public void messageReceived(byte cmd, int arg1, int arg2) {
                if(cmd == Command.BATT_LVL){
                    handleBattery(arg1,arg2);
                }
                if(cmd == Command.OPENMV_POS){
                    Log.d("OPENMV_POS", String.format("x: %d, y: %d",arg1,arg2));
                    camera.consume(arg1,arg2);
                }else{
                    Log.e("Command","Invalid cmd_id: " + String.format("%x",cmd));
                }
            }

            @Override
            public void error(String error) {
                //splash screen
                if(transitioned) {
                    warning.setVisibility(View.VISIBLE);
                    warning.setText(error);
                    warning.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Butterbot_red));
                }
                //main screen
                else{
                    status.setText(error);
                    status.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Butterbot_red));
                    dots.setVisibility(View.INVISIBLE);
                }
            }
        });

        //Config cloud connection
        cloud.setConnectionInfoListener(new ParticleStream.OnConnectionInfo() {

            @Override
            public void connectionInfo(String ssid, String ip, int port) {
                Log.d("main", "Starting tcp " + ip);
                client.setConnection(ip, port,TCP_TIMEOUT);
                client.execute();
            }

            @Override
            public void connectionError(String error) {
                status.setText("Cloud Error!");
                status.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Butterbot_red));
                dots.setVisibility(View.INVISIBLE);
            }
        });
        //connect to cloud
        cloud.execute();


        //configure camera
        camera.setTarget(100,100);
        camera.config_x(0.5,-50,50); //base scalar and limits
        camera.config_y(2.0,1100,1900); //neck scalar and limits
        camera.setOpenMVUpdateListener(new OpenMV.OnOpenMVUpdate(){
            @Override
            public void target_lock() {
                //TODO
            }

            @Override
            public void target_lost() {
                //TODO
            }
        });
        camera.execute();


        ///////////////////////////////Configure UI Element Callbacks///////////////////////////////
        //Override button callback
        eye.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!transitioned){
                    Toast.makeText(SplashActivity.this, "Demo mode. Not connected", Toast.LENGTH_SHORT).show();
                    transitionLayout();
                }
                return true;
            }
        });

        //toggle tracking callback
        eye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(transitioned){
                    Toast.makeText(SplashActivity.this, "Toggle tracking", Toast.LENGTH_SHORT).show();
                    if(camera.isTracking_enabled()) {
                        camera.disable_tracking();
                    }else{
                        camera.enable_tracking();
                    }
                }
            }
        });


        //Tilt callback for neck control
        tilt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    tilt.setImageResource(R.drawable.tiltdown);
                    vib.vibrate(20);

                    if(!neck_enabled) {
                        AlertDialog alertDialog = new AlertDialog.Builder(SplashActivity.this).create();
                        alertDialog.setTitle("Caution");
                        alertDialog.setMessage("About to enable neck. Is the head tilted down?");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        neck_enabled = true;

                                        bb.forceSend(bb.ATTACH_NECK, 1, 0);

                                        neck_resting_pos = 1500;
                                        bb.forceSend(bb.CTRL_NECK,neck_resting_pos,0);
                                        bb.forceSend(bb.CTRL_ARMS,1500,1500);
                                    }
                                });
                        alertDialog.show();
                    }
                    else{
                        neck_pos = neck_resting_pos;
                        start_phone_tilt = phone_tilt;
                        neck_moving = true;
                    }



                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    neck_moving = false;
                    neck_resting_pos = neck_pos;
                    tilt.setImageResource(R.drawable.tiltup);
                }
                return true;
            }
        });

        //Mic call back for audio control
        mic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //ui response
                    mic.setImageResource(R.drawable.micdown);
                    vib.vibrate(20);
                    handleMenu();

                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mic.setImageResource(R.drawable.micup);
                }
                return true;
            }
        });


        //Joystick call back for base control
        joystick.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                //update ui
                js.draw(arg1);


                if(arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int max_spd = 100;
                    int left,right;
                    int x = (int)(js.getX() * max_spd);
                    int y = (int)(js.getY() * -max_spd);
                    int mag = (int)(js.getMagnitude() * max_spd);
                    float angle = js.getAngle() + 90;
                    if(angle > 180){
                        angle -= 360;
                    }
                    //Forward
                    if(angle < 80  && angle > -80){
                        //veer right
                        if(angle > 0){
                            left = mag;
                            right = mag - x;
                        }
                        //veer left
                        else{
                            left = mag - x;
                            right = mag;
                        }
                    }
                    //reverse
                    else if(angle > 110 || angle < -110){
                        //veer right
                        if(angle > 0){
                            left = -mag;
                            right = -mag + x;
                        }
                        //veer left
                        else{
                            left = -mag + x;
                            right = -mag;
                        }
                    }
                    //center turn right
                    else if(angle > 0){
                        left = mag;
                        right = -mag;
                    }
                    //center turn left
                    else{
                        left = -mag;
                        right = mag;
                    }
                    bb.send(Command.CTRL_MOTORS,left+255,right+255);
                } else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    bb.forceSend(Command.CTRL_MOTORS,255,255);
                }
                return true;
            }
        });


        //Slider callback for arm control
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                bb.forceSend(Command.CTRL_ARMS,2000-slider_value,slider_value+1000);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                slider_value = progress;
                bb.send(Command.CTRL_ARMS,2000 -slider_value, slider_value + 1000); //range 1000 -2000 microseconds
            }
        });



        ///////////////////////////////Set up accelerometer//////////////////////////////////
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    magnetic = event.values;
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    gravity = event.values;
                if ((gravity == null) || (magnetic == null))
                    return;
                float[] fR = new float[9];
                float[] fI = new float[9];
                if (!SensorManager.getRotationMatrix(fR, fI, gravity, magnetic))
                    return;
                float[] orientation = new float[3];
                SensorManager.getOrientation(fR, orientation);
                phone_tilt = (float)Math.toDegrees(orientation[1]);

                if(neck_moving){
                    neck_pos = (int)Math.min(Math.max(neck_resting_pos - ((phone_tilt - start_phone_tilt) * 8.8),1100),1900);
                    bb.send(Command.CTRL_NECK,neck_pos,0);
                    //Log.d("Tilt",Integer.toString(neck_pos));
                }

            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, sensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);

        ViewServer.get(this).addWindow(this);
    }

    public void onDestroy() {
        super.onDestroy();
        //bb.forceSend(Command.CTRL_MOTORS,255,255); //255 offset aka 255 is 0
        //bb.forceSend(Command.CTRL_ARMS, 2000,1000);
        //bb.forceSend(Command.ATTACH_NECK,0,0);
        client.close();
        cloud.close();
        sensorManager.unregisterListener(sensorEventListener);
        ViewServer.get(this).removeWindow(this);
    }

    public void onResume() {
        super.onResume();
        ViewServer.get(this).setFocusedWindow(this);
    }

    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }


    public void transitionLayout() {

        transitioned = true;

        //constants
        int anim_duration = 220;
        int head_vert_margin = 100;
        int head_height_start = metrics.heightPixels;
        int head_height_end = (int) ((metrics.heightPixels / 2.0) - (2 * head_vert_margin));
        int head_width = head_height_end;
        int eye_margin = head_width / 6;
        int eye_margin_shift = -20;

        //Extract element attributes
        final ViewGroup.LayoutParams head_params = head.getLayoutParams();

        ValueAnimator head_height_anim = ValueAnimator.ofInt(head_height_start, head_height_end);
        head_height_anim.setInterpolator(new LinearInterpolator());
        head_height_anim.setDuration(anim_duration);
        head_height_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                head_params.height = (int) animation.getAnimatedValue();
                head.setLayoutParams(head_params);
            }
        });

        //remove splash screen elements
        root.setBackground(getDrawable(R.color.Butterbot_background_dark));
        root.removeView(status_container);
        root.removeView(author);

        //resize head width
        head_params.width = head_width;
        head.setLayoutParams(head_params);
        //set head vertical margins
        setMargins(head, 0, head_vert_margin, 0, head_vert_margin);
        //set eye size by modifying margins
        setMargins(eye, eye_margin, eye_margin + eye_margin_shift, eye_margin, eye_margin + eye_margin_shift);
        //animate head height resize
        head_height_anim.start();

    }


    void handleBattery(int level, int low){
        int full_batt = 1103;
        int empty_batt = 831;
        double percent = 1.0 * (level - empty_batt)/(full_batt-empty_batt);
        int icon;

        //Set the battery level
        if(percent > 0.9){ //  90 < p
            icon = 1;
        }
        else if(percent > 0.75) {// 75 < p < 90
            icon = 2;
        }
        else if(percent > 0.55) {// 55 < p < 75
            icon = 3;
        }
        else if(percent > 0.35) {// 35 < p < 55
            icon = 4;
        }
        else if(percent > 0.15) {// 15 < p < 35
            icon = 5;
        }
        else if(percent > 0.05) {// 5 < p < 15
            icon = 6;
        }
        else{                    //    p < 5
            icon = 7;
        }

        Context c = getApplicationContext();
        Resources resources = c.getResources();
        String filename = "batt" + Integer.toString(icon);
        int resourceId = resources.getIdentifier(filename, "drawable", c.getPackageName());
        battery.setImageDrawable(ContextCompat.getDrawable(c, resourceId));

        //set low battery message
        if(low > 0){
            warning.setVisibility(View.VISIBLE);
            warning.setText("Low Battery");
            warning.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.Butterbot_red));
        }
        else if(warning.getText().equals("Low Battery")){
            warning.setVisibility(View.INVISIBLE);
            warning.setText("");
        }
    }

    void handleMenu(){
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(SplashActivity.this, mic);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.omg:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_OMG,0);
                        break;
                    case R.id.purpose:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_PURPOSE,0);
                        break;
                    case R.id.friendship:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_FRIENDSHIP,0);
                        break;
                    case R.id.beep:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_BEEP,0);
                        break;
                    case R.id.boop:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_BOOP,0);
                        break;
                    case R.id.action_omg:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_OMG,0);
                        break;
                    case R.id.action_purpose:
                        bb.send(Command.CTRL_AUDIO,Command.AUDIO_PURPOSE,0);
                        break;

                }
                return true;
            }
        });
        popup.show();//showing popup menu
    }


    //doesn't work
    boolean enableNework(String ssid, Context cxt) {
        boolean state = false;
        WifiManager wm = (WifiManager) cxt.getSystemService(Context.WIFI_SERVICE);
        if (wm.setWifiEnabled(true)) {
            List<WifiConfiguration> networks = wm.getConfiguredNetworks();
            Iterator<WifiConfiguration> iterator = networks.iterator();
            while (iterator.hasNext()) {
                WifiConfiguration wifiConfig = iterator.next();
                if (wifiConfig.SSID.equals(ssid))
                    state = wm.enableNetwork(wifiConfig.networkId, true);
                else
                    wm.disableNetwork(wifiConfig.networkId);
            }
            wm.reconnect();
        }
        return state;
    }

}
