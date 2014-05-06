/**
 * 
 */

package edu.isi.backpack.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;
import edu.isi.backpack.sync.Connector;
import edu.isi.backpack.sync.MessageHandler;
import edu.isi.backpack.sync.SyncListenerTransactor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jenniferchen The listener starts at boot time registers the device on
 *         wifi. Then waits for incoming wifi connection requests.
 */
public class WifiListenerService extends Service {

    public static final String TAG = "WifiListenerService";

    public static final int MSG_REQUEST_SERVICE_NAME = 0;

    private NsdManager mNsdManager;

    private String registeredName = null;

    private boolean registered = false;

    private AtomicBoolean wifiOn = new AtomicBoolean(false);

    private ServerSocket serverSocket = null;

    private File path, metaFile;

    private String deviceName;

    /**
     * Handler of incoming messages from clients.
     */
    @SuppressLint("HandlerLeak")
    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REQUEST_SERVICE_NAME:
                    Log.i(TAG, "service name request received");
                    while (!registered) {
                    } // wait until registration is complete (either suceeded or
                      // failed)
                    Log.i(TAG, "sending back service name: " + registeredName);
                    Message m = Message.obtain(null, WifiListenerService.MSG_REQUEST_SERVICE_NAME);
                    Bundle b = new Bundle();
                    b.putString("service", registeredName);
                    m.setData(b);
                    try {
                        msg.replyTo.send(m);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private RegistrationListener mRegistrationListener = new RegistrationListener() {

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "Wifi registration failed");
            registered = true;
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Wifi registered: " + serviceInfo.getServiceName());
            registeredName = serviceInfo.getServiceName();
            registered = true;
            Log.i(TAG, "Wifi registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Wifi service unregistered: " + serviceInfo.getServiceName());
            registeredName = null;
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "Wifi unregistration failed");
        }

    };

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                synchronized (wifiOn) {
                    Log.i(TAG, "Wifi turned on");
                    wifiOn.set(true);
                    wifiOn.notifyAll();
                }
            else {
                synchronized (wifiOn) {
                    Log.i(TAG, "Wifi turned off");
                    wifiOn.set(false);
                    wifiOn.notifyAll();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        registered = false;

        // register for wifi state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        // intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        // intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, intentFilter);

        // register wifi
        registerService();

        File appDir = getExternalFilesDir(null);
        path = new File(appDir, Constants.contentDirName);
        if (!path.exists()) {
            path.mkdir();
        }
        metaFile = new File(path, Constants.metaFileName);
        try {
            if (!metaFile.exists()) {
                metaFile.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to create a empty meta data file" + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Starting WifiListenerService");

        // check wifi status
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            wifiOn.set(true);
        }

        // start listening for incoming connections
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                while (true) {
                    if (wifiOn.get()) {

                        Log.i(TAG, "Wifi is on");

                        // create server socket
                        try {
                            if (serverSocket == null)
                                serverSocket = new ServerSocket(WifiConstants.SERVICE_PORT);
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to create server socket");
                            continue;
                        }

                        Socket socket = null;
                        if (serverSocket != null) {
                            Log.i(TAG, "Wifi Server socket created");
                            try {
                                // blocks until successful connection or
                                // exception
                                socket = serverSocket.accept();
                            } catch (IOException e) {
                                socket = null;
                                Log.e(TAG, "Error occured while waiting for wifi connection");
                                continue;
                            }
                        }

                        // if socket is null, accept was canceled because wifi
                        // has been turned off
                        if (socket != null) {
                            Log.i(TAG, "Connection established");
                            sendBroadcast(new Intent(WifiConstants.CONNECTION_ESTABLISHED_ACTION));

                            Thread commThread = new Thread(new CommunicationSocket(socket));
                            commThread.start();

                            // wait for commThread to finish
                            // we are keeping one connection at a time
                            try {
                                commThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, "Sync complete");
                        }
                    } else { // wifi is off, wait until it's turned on
                        synchronized (wifiOn) {
                            try {
                                wifiOn.wait();
                            } catch (InterruptedException e) {
                                Log.w(TAG, "Interrupted while waiting for wifi to turn On");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        });
        t.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // unregister wifi
        unregisterService();
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mMessenger.getBinder(); // return messenger to send message
    }

    public void registerService() {

        if (registeredName == null) { // if haven't registered

            // use bluetooth device name to register for wifi service name
            deviceName = BluetoothAdapter.getDefaultAdapter().getName();
            deviceName = deviceName.replaceAll(" ", "_");

            NsdServiceInfo serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceInfo.setServiceName(Constants.WIFI_SERVICE_HEADER + deviceName);
            serviceInfo.setServiceType(WifiConstants.SERVICE_TYPE);
            serviceInfo.setPort(WifiConstants.SERVICE_PORT);

            mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                    mRegistrationListener);

        } else {
            Log.i(TAG, "Wifi already registered");
        }
    }

    public void unregisterService() {
        Log.i(TAG, "Unregistering Wifi service");
        mNsdManager.unregisterService(mRegistrationListener);
    }

    /**
     * This class handles the communication on the child socket on a different
     * thread
     * 
     * @author mohit aggarwl
     */
    private class CommunicationSocket implements Runnable {
        private Socket commSock = null;

        private InputStream mmInStream;

        private OutputStream mmOutStream;

        private Connector conn;

        private MessageHandler mHandler;

        /**
         * Constructor for the class
         * 
         * @param socket
         */
        public CommunicationSocket(Socket socket) {
            commSock = socket;
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
                conn = new Connector(mmInStream, mmOutStream, getApplicationContext());
                mHandler = new MessageHandler(conn, WifiListenerService.this, metaFile);
            } catch (IOException e) {
                socket = null;
                Log.e(TAG,
                        "Trying to get socket IOStream while socket is not connected"
                                + e.getMessage());
                sendBroadcast(new Intent(WifiConstants.CONNECTION_CLOSED_ACTION));
            }
        }

        @Override
        public void run() {
            if (commSock != null) {

                // start transaction
                SyncListenerTransactor trans = new SyncListenerTransactor(WifiListenerService.this,
                        conn, mHandler, path, deviceName);
                String result = trans.run();

                Log.i(TAG, "Close socket");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                try {
                    mmInStream.close();
                    mmOutStream.close();
                    commSock.close();

                } catch (IOException e) {
                    Log.e(TAG, "Exception while closing child socket after file sync is completed",
                            e);
                }

                Intent i = new Intent(WifiConstants.CONNECTION_CLOSED_ACTION);
                i.putExtra(ExtraConstants.STATUS, result);
                sendBroadcast(i);

            }// end if(socket!=null)
        }
    }

}
