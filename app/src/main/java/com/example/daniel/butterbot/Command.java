package com.example.daniel.butterbot;

/**
 * Created by Daniel on 11/26/2016.
 */

public class Command {
    public static final int NUM_COMMANDS = 9;
    public static final byte CMD_ERROR      = 0x00;        //Invalid command
    public static final byte CTRL_MOTORS    = 0x01;        //control treads        - {Left(-255,255),right(-255,255)}
    public static final byte CTRL_ARMS      = 0x02;        //control arms          - {Left(1000,2000).right(1000,2000)}
    public static final byte CTRL_NECK      = 0x03;        //control neck          - {(1000,2000)}
    public static final byte CTRL_LED       = 0x04;        //control LED           - {LED_MODE, On/Off or blink_ms}
    public static final byte CTRL_AUDIO     = 0x05;        //play sound track      - {AUDIO}
    public static final byte CTRL_LIST_MODE = 0x06;        //activate listen mode  - {}
    public static final byte BATT_LVL       = 0x07;        //Battery Level
    public static final byte ATTACH_NECK    = 0x08;        //Activate the neck

    public static final byte LED_MODE_TALK  = 0x00;        //Flash LED while talking
    public static final byte LED_MODE_BLINK = 0x01;        //Flash at a continous rate
    public static final byte LED_MODE_IO    = 0x02;        //LED either on or off
    public static final byte LED_MODE_CNCT  = 0x03;        //LED breathes while waiting for the app to connnect
    public static final byte LED_MODE_BATT  = 0x04;        //LED flashes fast when battery is low
    public static final int LED_TALK_RATE  = 170 ;        //Rate to flash the LED when talking. Half the period in ms
    public static final int LED_BATT_RATE  = 100 ;         //Rate to flash the LED when low on battery. Half the period in ms
    public static final int LED_CNCT_RATE  = 1000;          //Rate to breathe the LED when connecting. Half the period in ms

    public static final int AUDIO_OMG          = 3;
    public static final int AUDIO_PURPOSE      = 4;
    public static final int AUDIO_FRIENDSHIP   = 2;
    public static final int AUDIO_BEEP         = 0;
    public static final int AUDIO_BOOP         = 1;


    long[] last_send;
    int send_period;
    TCPClient client;


    public Command(TCPClient client,int send_rate){
        last_send = new long[NUM_COMMANDS];
        for(int i =0; i<NUM_COMMANDS; i++){
            last_send[i] = 0;
        }

        this.client = client;
        send_period = (int)1000.0/send_rate;
    }

    public void send(byte cmd, int arg1,int arg2){
        if(cmd < NUM_COMMANDS){
            if(System.currentTimeMillis() - last_send[(int)cmd] > send_period){
                client.sendPacket(cmd,arg1,arg2);
                last_send[(int)cmd] = System.currentTimeMillis();
            }
        }
    }
    public void forceSend(byte cmd, int arg1,int arg2){
        if(cmd < NUM_COMMANDS){
            client.sendPacket(cmd,arg1,arg2);
            last_send[(int)cmd] = System.currentTimeMillis();
        }
    }
}
