package com.example.daniel.butterbot;

import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by Daniel on 11/22/2016.
 */

public class JoyStick {
    public static final int STICK_NONE = 0;
    public static final int STICK_UP = 1;
    public static final int STICK_UPRIGHT = 2;
    public static final int STICK_RIGHT = 3;
    public static final int STICK_DOWNRIGHT = 4;
    public static final int STICK_DOWN = 5;
    public static final int STICK_DOWNLEFT = 6;
    public static final int STICK_LEFT = 7;
    public static final int STICK_UPLEFT = 8;

    private ImageView pad;
    private ImageView stick;
    private ViewGroup.MarginLayoutParams pad_margins;
    private ViewGroup.MarginLayoutParams stick_margins;
    private Vibrator vib;
    private int box_width, box_height;
    private int default_pad_margins, default_stick_margins;
    private int center_x = 0, center_y = 0;
    private float magnitude = 0, angle = 0;
    private int inc = 22;

    private boolean touch_state = false;

    public JoyStick(Context context, ViewGroup boundingBox, ImageView pad, ImageView stick) {
        this.pad = pad;
        this.stick = stick;
        vib = (Vibrator)context.getSystemService(VIBRATOR_SERVICE);


        box_width = boundingBox.getLayoutParams().width;
        box_height = boundingBox.getLayoutParams().height;
        pad_margins = (ViewGroup.MarginLayoutParams)pad.getLayoutParams();
        stick_margins = (ViewGroup.MarginLayoutParams)stick.getLayoutParams();
        default_pad_margins = pad_margins.leftMargin;
        default_stick_margins = stick_margins.leftMargin;

    }

    public void draw(MotionEvent arg1) {

        //first touch
        if(arg1.getAction() == MotionEvent.ACTION_DOWN) {
            int touch_x = (int) (arg1.getX() - (box_width / 2));
            int touch_y = (int) (arg1.getY() - (box_height / 2));

            //Check we are close to center of joystick
            if(Math.abs(touch_x) < pad_margins.leftMargin && Math.abs(touch_y)<pad_margins.topMargin){
                vib.vibrate(20);
                touch_state = true;
                //Center on initial touch
                center_x = touch_x;
                center_y = touch_y;
                //adjust margins of pad to touch location
                pad_margins.leftMargin += center_x;
                pad_margins.rightMargin-= center_x;
                pad_margins.topMargin += center_y;
                pad_margins.bottomMargin -= center_y;

                //adjust margins of stick to touch location
                stick_margins.leftMargin += center_x - inc;
                stick_margins.rightMargin-= center_x - inc;
                stick_margins.topMargin += center_y - inc;
                stick_margins.bottomMargin -= center_y - inc;

                //write to screen
                pad.setLayoutParams(pad_margins);
                stick.setLayoutParams(stick_margins);
            }
        } //moving the stick
        else if(arg1.getAction() == MotionEvent.ACTION_MOVE && touch_state) {
            float stick_x = arg1.getX() - (box_width / 2) - center_x;
            float stick_y = arg1.getY() - (box_height/ 2) - center_y;
            float dist = Math.min((float) Math.sqrt(Math.pow(stick_x, 2) + Math.pow(stick_y, 2)),box_width/2-default_pad_margins);
            angle = (float) cal_angle(stick_x, stick_y);
            magnitude =  dist /(box_width/2-default_pad_margins);

            //limit to the bounds of the pad
            int limit_x = (int) (Math.cos(Math.toRadians(angle)) * dist);
            int limit_y = (int) (Math.sin(Math.toRadians(angle)) * dist);


            //adjust margins of stick to touch location
            stick_margins.leftMargin = default_stick_margins + limit_x + center_x - inc;
            stick_margins.rightMargin = default_stick_margins - limit_x - center_x - inc;
            stick_margins.topMargin  = default_stick_margins + limit_y + center_y - inc;
            stick_margins.bottomMargin = default_stick_margins - limit_y - center_y - inc;

            stick.setLayoutParams(stick_margins);

        } //releasing the stick
        else if(arg1.getAction() == MotionEvent.ACTION_UP) {
            touch_state = false;

            //Recenter pad
            pad_margins.leftMargin = default_pad_margins;
            pad_margins.rightMargin = default_pad_margins;
            pad_margins.topMargin = default_pad_margins;
            pad_margins.bottomMargin = default_pad_margins;

            stick_margins.leftMargin = default_stick_margins;
            stick_margins.rightMargin =default_stick_margins;
            stick_margins.topMargin = default_stick_margins;
            stick_margins.bottomMargin = default_stick_margins;
            //write to screen
            pad.setLayoutParams(pad_margins);
            stick.setLayoutParams(stick_margins);

        }
    }

    public float[] getPosition() {
        return new float[] { getX(), getY() };
    }

    public float getX() {
        float mag_x = 0;
        if(touch_state) {
            mag_x = (float)(Math.cos(Math.toRadians(angle)) * magnitude);
        }
        return mag_x;
    }

    public float getY() {
        float mag_y = 0;
        if(touch_state) {
            mag_y = (float)(Math.sin(Math.toRadians(angle)) * magnitude);
        }
        return mag_y;
    }

    public float getAngle() {
        if(touch_state) {
            return angle;
        }
        return 0;
    }

    public float getMagnitude() {
        if(touch_state) {
            return magnitude;
        }
        return 0;
    }


    public int get8Direction() {
        if(touch_state) {
            if(angle >= 247.5 && angle < 292.5 ) {
                return STICK_UP;
            } else if(angle >= 292.5 && angle < 337.5 ) {
                return STICK_UPRIGHT;
            } else if(angle >= 337.5 || angle < 22.5 ) {
                return STICK_RIGHT;
            } else if(angle >= 22.5 && angle < 67.5 ) {
                return STICK_DOWNRIGHT;
            } else if(angle >= 67.5 && angle < 112.5 ) {
                return STICK_DOWN;
            } else if(angle >= 112.5 && angle < 157.5 ) {
                return STICK_DOWNLEFT;
            } else if(angle >= 157.5 && angle < 202.5 ) {
                return STICK_LEFT;
            } else if(angle >= 202.5 && angle < 247.5 ) {
                return STICK_UPLEFT;
            }
        }
        return STICK_NONE;
    }

    public int get4Direction() {
        if(touch_state) {
            if(angle >= 225 && angle < 315 ) {
                return STICK_UP;
            } else if(angle >= 315 || angle < 45 ) {
                return STICK_RIGHT;
            } else if(angle >= 45 && angle < 135 ) {
                return STICK_DOWN;
            } else if(angle >= 135 && angle < 225 ) {
                return STICK_LEFT;
            }
        }
        return STICK_NONE;
    }


    private double cal_angle(float x, float y) {
        if(x >= 0 && y >= 0)
            return Math.toDegrees(Math.atan(y / x));
        else if(x < 0 && y >= 0)
            return Math.toDegrees(Math.atan(y / x)) + 180;
        else if(x < 0 && y < 0)
            return Math.toDegrees(Math.atan(y / x)) + 180;
        else if(x >= 0 && y < 0)
            return Math.toDegrees(Math.atan(y / x)) + 360;
        return 0;
    }
}
