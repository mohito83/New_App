/**
 * 
 */

package edu.isi.backpack.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import edu.isi.backpack.R;
import edu.isi.backpack.bluetooth.BluetoothDisconnectedException;
import edu.isi.backpack.bluetooth.Connector;
import edu.isi.backpack.bluetooth.MessageHandler;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.util.BackpackUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 * 
 * @author mohit aggarwal
 */
public class ListenerService extends Service {

    private static final String TAG = "BackPackListenerService";

    // Name for the SDP record when creating server socket
    private static final String FTP_SERVICE = "CustomFTPService";

    private static final UUID MY_UUID = UUID
    /* .fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); */
    .fromString("00001101-0000-1000-8000-00805F9B34FB");// this is the correct
                                                        // UUID for SPP

    private BluetoothAdapter mAdapter;

    private BluetoothServerSocket mmServerSocket;

    private File path, metaFile, webMetaFile;

    private boolean btOn = false;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        btOn = false;
                        if (mmServerSocket != null)
                            try {
                                mmServerSocket.close(); // this cancels accept()
                                                        // call
                            } catch (IOException e) {
                            }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        btOn = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        mAdapter = BluetoothAdapter.getDefaultAdapter();
                        mmServerSocket = null;
                        if (mAdapter != null && mAdapter.isEnabled()) {
                            Log.i(TAG, "Bluetooth turned on");
                            btOn = true;
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    /**
     * Consturctor for the class.
     */
    public void onCreate() {
        // Debug.waitForDebugger();

        // register bluetooth listener
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        File appDir = getExternalFilesDir(null);
        path = new File(appDir, Constants.contentDirName);
        if (!path.exists()) {
            path.mkdir();
        }
        metaFile = new File(path, Constants.videoMetaFileName);
        webMetaFile = new File(path, Constants.webMetaFileName);
        try {
            if (!webMetaFile.exists()) {
                webMetaFile.createNewFile();
            }

            if (!metaFile.exists()) {
                metaFile.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to create a empty meta data file" + e.getMessage());
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "starting listener service");

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter != null && mAdapter.isEnabled())
            btOn = true;
        else
            btOn = false;

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                Looper.prepare(); // @mohit Do we need this?

                // start the server socket for listening the incoming
                // connections
                while (true) {
                    if (btOn) {

                        Log.i(TAG, "Bluetooth is on");

                        // create server socket if not created
                        if (mmServerSocket == null) {
                            mmServerSocket = createServerSocket();
                        }
                        BluetoothSocket socket = null;
                        if (mmServerSocket != null) {
                            Log.i(TAG, "Server Socket created");
                            try {
                                // This is a blocking call and will only
                                // return on a
                                // successful connection or an exception
                                Log.i(TAG, "Listening for incoming connection requests");
                                socket = mmServerSocket.accept();
                            } catch (IOException e) { // bluetooth turned off
                                                      // while waiting for
                                                      // connection
                                socket = null;
                                Log.e(TAG, "Bluetooth turned off while waiting for connection");
                                continue;
                            }
                        }

                        // if socket is null, accept was canceled because
                        // bluetooth was turned off
                        if (socket != null) {

                            // connection established
                            Log.i(TAG, "Connection established");
                            sendBroadcast(new Intent(Constants.BT_CONNECTED_ACTION));

                            Thread commThread = new Thread(new CommunicationSocket(socket));
                            commThread.start();

                            // wait for commThread to finish
                            // since we only need to keep one connection at a
                            // time
                            try {
                                commThread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.i(TAG, "Sync Complete");
                        }
                    }
                }
            }
        });
        t.start();

        return START_STICKY;

    }

    private BluetoothServerSocket createServerSocket() {
        BluetoothServerSocket tmp = null;
        try {
            tmp = mAdapter.listenUsingRfcommWithServiceRecord(FTP_SERVICE, MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "AcceptThread: Socket listen() failed", e);
        }
        return tmp;
    }

    /**
     * This class handles the communication on the child socket on a different
     * thread
     * 
     * @author mohit aggarwl
     */
    private class CommunicationSocket implements Runnable {
        private BluetoothSocket commSock = null;

        private InputStream mmInStream;

        private OutputStream mmOutStream;

        private Connector conn;

        private MessageHandler mHanlder;

        private int transcState;

        /**
         * Constructor for the class
         * 
         * @param socket
         */
        public CommunicationSocket(BluetoothSocket socket) {
            commSock = socket;
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
                conn = new Connector(mmInStream, mmOutStream, getApplicationContext());
                mHanlder = new MessageHandler(conn, ListenerService.this, metaFile, webMetaFile);
                transcState = Constants.META_DATA_EXCHANGE;
            } catch (IOException e) {
                socket = null;
                Log.e(TAG,
                        "Trying to get socket IOStream while socket is not connected"
                                + e.getMessage());
                sendBroadcast(new Intent(Constants.BT_DISCONNECTED_ACTION));
            }
        }

        @Override
        public void run() {
            if (commSock != null) {

                BackpackUtils.broadcastMessage(ListenerService.this, commSock.getRemoteDevice()
                        .getName() + " " + getString(R.string.connection_successful));

                boolean terminate = false;
                boolean disconnected = false;

                while (!terminate) {
                    switch (transcState) {
                        case Constants.META_DATA_EXCHANGE:
                            try {
                                Log.i(TAG, "Sending videos meta data");
                                mHanlder.sendFullMetaData(Constants.VIDEO_META_DATA_FULL, metaFile);
                                Log.i(TAG, "Receiving videos meta data");
                                mHanlder.receiveFullMetaData(path);

                                Log.i(TAG, "Sending web meta data");
                                mHanlder.sendFullMetaData(Constants.WEB_META_DATA_FULL, webMetaFile);
                                Log.i(TAG, "Receiving web meta data");
                                mHanlder.receiveFullMetaData(path);

                                transcState = Constants.FILE_DATA_EXCHANGE;
                                break;
                            } catch (BluetoothDisconnectedException e) {
                                terminate = true;
                                disconnected = true;
                                break;
                            }

                        case Constants.FILE_DATA_EXCHANGE:
                            try {
                                File xferDir = new File(path, Constants.xferDirName + "/"
                                        + commSock.getRemoteDevice().getName());
                                xferDir.mkdirs();
                                Log.i(TAG, "Start receiving web contents");
                                mHanlder.receiveFiles(xferDir);
                                Log.i(TAG, "Finished receiving web contents");

                                Log.i(TAG, "Start sending web contents");
                                mHanlder.sendWebContent(path);
                                Log.i(TAG, "Finished sending web contents");

                                Log.i(TAG, "Start receiving videos");
                                mHanlder.receiveFiles(xferDir);
                                Log.i(TAG, "Finished receiving videos");

                                Log.i(TAG, "Start sending videos");
                                mHanlder.sendVideos(path);
                                Log.i(TAG, "Finished sending videos");

                                transcState = Constants.SYNC_COMPLETE;
                                terminate = true;
                                break;
                            } catch (BluetoothDisconnectedException e) {
                                terminate = true;
                                disconnected = true;
                                break;
                            }
                    }
                }

                // close the socket
                if (terminate) {
                    String message;
                    if (disconnected) // got disconnected in the middle of
                                      // transfer
                        message = getString(R.string.file_sync_incomplete);
                    else
                        message = getString(R.string.file_sync_successful);
                    conn.cancelNotification();
                    BackpackUtils.broadcastMessage(ListenerService.this, message);

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
                        Log.e(TAG,
                                "Exception while closing child socket after file sync is completed",
                                e);
                    }

                    Intent i = new Intent(Constants.BT_DISCONNECTED_ACTION);
                    i.putExtra(ExtraConstants.STATUS, message);
                    sendBroadcast(i);
                }

            }// end if(socket!=null)
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.i(TAG, "listener service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
