package net.majorkernelpanic.example1;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by Vijen on 14/09/2017.
 */

public class BroadcastIP {
    private String LOG_TAG = "BroadcastIP";
    private Boolean BROADCAST = true;
    private int BROADCAST_PORT = 50005;
    private int BROADCAST_INTERVAL = 1000;
    MainActivity mainActivity = new MainActivity();

    long start;
    long end;


    public BroadcastIP(String name, InetAddress broadcastIp) {

         start = System.currentTimeMillis();
         end = start + 60*1000; // 30 seconds * 1000 ms/sec
        BroadcastIPaddress(name,broadcastIp);
    }

    public void BroadcastIPaddress(final String name, final InetAddress broadcastIp) {

        // Broadcasts the name of the device at a regular interval
        Log.i(LOG_TAG, "Broadcasting started!");
        final Thread broadcastThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String request = "ADD:"+name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket1 = new DatagramSocket();
                    socket1.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIp, BROADCAST_PORT);
                    while(System.currentTimeMillis() < end) {
                       socket1.send(packet);
                       Log.i(LOG_TAG, "Broadcast packet sent: "
                               + packet.getAddress().toString()+" "
                               +packet.getPort() + " "
                               +packet.getData().toString() + " "
                               );
                       Thread.sleep(BROADCAST_INTERVAL);
                    }
                    Log.i(LOG_TAG, "Broadcaster ending!");
                    socket1.disconnect();
                    socket1.close();
                    return;
                }
                catch(SocketException e) {
                    Log.e(LOG_TAG, "SocketExceltion in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                    return;
                }
                catch(IOException e) {
                    Log.e(LOG_TAG, "IOException in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                    return;
                }
                catch(InterruptedException e) {
                    Log.e(LOG_TAG, "InterruptedException in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                    return;
                }
            }
        });
        broadcastThread.start();
    }
}

