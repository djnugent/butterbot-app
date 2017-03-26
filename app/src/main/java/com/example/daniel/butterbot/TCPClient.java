package com.example.daniel.butterbot;

/**
 * Created by Daniel on 11/21/2016.
 */


import android.os.AsyncTask;
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPClient extends AsyncTask<Void, Object, Void> {


    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = 1;
    public static final int MESSAGE_RECV = 2;
    public static final int ERROR = 3;

    private boolean running = true;
    private boolean alive = false;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String server = "0.0.0.0";
    private int port = 0;

    private byte[] serverMessage;
    private OnTCPUpdate listener = null;
    private long last_heartbeat = 0;
    private long last_recv = 0;
    private int timeout = 250;



    public  void setConnection(String server, int port,int timeout){
        this.server = new String(server);
        this.port = port;
        this.timeout = timeout;
    }


    public void setTCPUpdateListener(OnTCPUpdate listener) {
        this.listener = listener;
    }

    public boolean isAlive(){
        return socket != null && alive && !socket.isClosed() && socket.isConnected();
    }
    public void close(){
        running = false;
        alive = false;
    }

    public boolean sendPacket(byte cmd_id, int arg1, int arg2){
        if (out != null && alive && !socket.isClosed()) {
            ByteBuffer buff = ByteBuffer.allocate(6);
            buff.put(0, cmd_id);
            buff.putShort(1,(short)arg1);
            buff.putShort(3,(short)arg2);
            buff.put(5,(byte)'\n');

            byte[] cmd = buff.array();
            try {
                out.write(cmd);
                out.flush();
                return true;
            }catch (Exception e){
                Log.e("TCP", "Write Exception", e);
            }
        }
        else{
            Log.e("TCP", "Write Error");
        }
        return false;
    }

    @Override
    protected Void doInBackground(Void... Params) {

        Log.d("TCP", "Starting Task");

        while(running){
            //(re)connect to server
            if(!alive) {
                try {
                    //Close socket if it is open
                    if(socket != null){
                        try{
                        Log.d("TCP", "Cleaning socket");
                        socket.close();} catch (Exception e){}
                    }

                    //connect to server
                    Log.d("TCP", "Connecting to " + server + ":" + Integer.toString(port));
                    InetAddress serverAddr = InetAddress.getByName(server);
                    socket = new Socket(serverAddr, port);
                    socket.setTcpNoDelay(true);

                    //Setup I/O buffers
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());

                    //connection successful
                    Log.d("TCP", "Connected");
                    last_recv = System.currentTimeMillis();
                    alive = socket.isConnected();
                    publishProgress(CONNECTED);

                } catch (Exception e) {
                    Log.e("TCP", "Connection failed", e);
                    alive = false;

                    //refused connection
                    if(e.getMessage().contains("ECONNREFUSED")){
                        publishProgress(ERROR, "Connection Refused");
                    }
                    //No connection found
                    else if(e.getMessage().contains("timed out")){
                        publishProgress(ERROR, "Connection timeout");
                    }
                    else if(e.getMessage().contains("EHOSTUNREACH")){
                        publishProgress(ERROR, "Host not reachable");
                    }
                    else {
                        publishProgress(DISCONNECTED);
                    }

                    try {
                        socket.close();
                        Thread.sleep(400);
                    }
                    catch (Exception b){}
                }
            }

            //process incoming messages
            if(alive && !socket.isClosed()){
                try{
                    //Let the server know we are alive
                    if (System.currentTimeMillis() - last_heartbeat > 250) {
                        sendPacket((byte) 0x07, 0, 0);
                        last_heartbeat = System.currentTimeMillis();
                    }
                    //wait for full packet
                    if (in.available() >= 6){
                        last_recv = System.currentTimeMillis();
                        serverMessage = new byte[6];
                        in.readFully(serverMessage);
                    }
                    //parse packet
                    if (serverMessage != null && listener != null) {
                        ByteBuffer msg = ByteBuffer.wrap(serverMessage);
                        //call the method messageReceived from MyActivity class
                        byte cmd = msg.get(0);
                        short arg1 = msg.getShort(1);
                        short arg2 = msg.getShort(3);
                        publishProgress(MESSAGE_RECV,cmd,(int)arg1,(int)arg2);
                    }
                    serverMessage = null;

                    //update connection status
                    if(System.currentTimeMillis() - last_recv > timeout){
                        publishProgress(DISCONNECTED);
                        alive = false;
                        Log.e("TCP","Heartbeat Timeout");
                    }

                }
                catch (Exception e){
                    Log.e("TCP","Socket error", e);
                }

            }
            else{
                //connected has been closed
                if(alive){
                    publishProgress(DISCONNECTED);
                    alive = false;
                }
            }
        }

        //close connection
        try{
            Log.d("TCP Client", "Task finished: Closing socket");
            socket.close();
            publishProgress(DISCONNECTED);
        }catch (Exception e){
            Log.e("TCP Client", "Close Connection failed", e);
            alive = false;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {}

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Object... values) {
        if(running) {
            int type = (int) values[0];
            switch (type) {
                case CONNECTED:
                    listener.connected();
                    break;
                case DISCONNECTED:
                    listener.disconnected();
                    break;
                case MESSAGE_RECV:
                    byte cmd = (byte) values[1];
                    int arg1 = (int) values[2];
                    int arg2 = (int) values[3];
                    listener.messageReceived(cmd, arg1, arg2);
                    break;
                case ERROR:
                    listener.error((String)values[1]);
                    break;
            }
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnTCPUpdate {
        public void connected();
        public void disconnected();
        public void error(String error);
        public void messageReceived(byte cmd_id, int arg1,int arg2);
    }
}