/**
 * 
 */

package edu.isi.backpack.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import edu.isi.backpack.R;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.sync.Connector;
import edu.isi.backpack.sync.MessageHandler;
import edu.isi.backpack.sync.SyncConnectorTransactor;
import edu.isi.backpack.util.BackpackUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * This class is a service which initiates the connection to the remote server
 * 
 * @author mohit aggarwl
 */
public class ConnectionService extends Service {
    private static final String TAG = "BackPackConnectionService";

    // Name for the SDP record when creating server socket
    private static final UUID MY_UUID = UUID
    /* .fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); */
    .fromString("00001101-0000-1000-8000-00805F9B34FB"); // this is the correct
                                                         // UUID for SPP

    private BluetoothAdapter mAdapter;

    private File path;

    private File metaFile;

    private File webMetaFile;

    private Intent mIntent;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() {
        // Debug.waitForDebugger();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
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

    CountDownTimer timer = new CountDownTimer(Long.MAX_VALUE, 15000) {
        // This is called every interval. (Every 15 seconds in this example)
        public void onTick(long millisUntilFinished) {

            Thread t = new Thread(new Runnable() {
                final BluetoothDevice item = mIntent.getExtras().getParcelable(
                        ExtraConstants.DEVICE);

                private BluetoothSocket mmSocket;

                @Override
                public void run() {
                    // get the slave bluetooth device

                    BluetoothDevice device = mAdapter.getRemoteDevice(item.getAddress());

                    // connect to slave
                    try {
                        // get the socket from the device
                        Log.i(TAG, "Connecting");
                        // mmSocket =
                        // device.createRfcommSocketToServiceRecord(MY_UUID);
                        mmSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                        mAdapter.cancelDiscovery();
                        mmSocket.connect();
                    } catch (IOException e) {
                        try {
                            mmSocket.close();
                        } catch (Exception e2) {
                            Log.e(TAG, "unable to close() socket during connection failure", e2);
                        }
                    }

                }
            });
            t.start();
        }

        public void onFinish() {
            start();
        }
    };

    public int onStartCommand(Intent intent, int flags, int startId) {

        // Debug.waitForDebugger();
        mIntent = intent;
        final BluetoothDevice item = intent.getExtras().getParcelable(ExtraConstants.DEVICE);

        // use a seaparate thread for connection and data transfer
        Thread t = new Thread(new Runnable() {
            private BluetoothSocket mmSocket;

            private InputStream mmInStream;

            private OutputStream mmOutStream;

            private Connector conn;

            private MessageHandler mHandler;

            @Override
            public void run() {
                // get the slave bluetooth device

                BluetoothDevice device = mAdapter.getRemoteDevice(item.getAddress());

                // connect to slave
                try {
                    // get the socket from the device
                    Log.i(TAG, "Connecting");
                    // mmSocket =
                    // device.createRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    mAdapter.cancelDiscovery();
                    mmSocket.connect();
                } catch (IOException e) {

                    // try reconnecting using reflection if that also fails then
                    // show error message & exit.
                    if (e.getMessage().equalsIgnoreCase("Service discovery failed")) {
                        Method m;
                        try {
                            /*
                             * m =
                             * device.getClass().getMethod("createRfcommSocket",
                             * new Class[] { int.class });
                             */
                            m = device.getClass().getMethod("createInsecureRfcommSocket",
                                    new Class[] {
                                        int.class
                                    });
                            mmSocket = (BluetoothSocket) m.invoke(device, 1);
                            mmSocket.connect();
                        } catch (NoSuchMethodException e1) {
                            e1.printStackTrace();
                        } catch (IllegalArgumentException e1) {
                            e1.printStackTrace();
                        } catch (IllegalAccessException e1) {
                            e1.printStackTrace();
                        } catch (InvocationTargetException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            BackpackUtils.broadcastMessage(ConnectionService.this, device.getName()
                                    + " " + getString(R.string.connection_failed));
                            return;
                        }
                    } else {
                        BackpackUtils.broadcastMessage(ConnectionService.this, device.getName()
                                + " " + getString(R.string.connection_failed));

                        try {
                            Log.e(TAG, "unable to connect, closing socket");
                            Log.e(TAG, e.getMessage());
                            mmSocket.close();
                        } catch (IOException e2) {
                            Log.e(TAG, "unable to close() socket during connection failure", e2);
                        }

                        return;
                    }

                }
                // periodically hit the socket to resume its task (when sync
                // freezed)
                timer.start();
                // connection established
                Log.i(TAG, "Connection established");
                sendBroadcast(new Intent(Constants.BT_CONNECTED_ACTION));
                BackpackUtils.broadcastMessage(ConnectionService.this, "Successfully connected to "
                        + device.getName());

                if (mmSocket != null) {
                    // Start point for data synchronization
                    try {
                        mmInStream = mmSocket.getInputStream();
                        mmOutStream = mmSocket.getOutputStream();
                        conn = new Connector(mmInStream, mmOutStream, getApplicationContext());
                        mHandler = new MessageHandler(conn, ConnectionService.this, metaFile,
                                webMetaFile);
                    } catch (IOException e) {
                        Log.e(TAG, "unable to get in/out put streams", e);
                        sendBroadcast(new Intent(Constants.BT_DISCONNECTED_ACTION));
                        BackpackUtils.broadcastMessage(ConnectionService.this,
                                "Error in initiating connection");
                        return;
                    }

                    // run transactions
                    SyncConnectorTransactor sc = new SyncConnectorTransactor(ConnectionService.this, conn, mHandler,
                            path, device.getName());
                    String result = sc.run();

                    // transaction ended
                    // disconnect
                    Log.i(TAG, "Close socket");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        mmInStream.close();
                        mmOutStream.close();
                        mmSocket.close();
                        timer.cancel();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to disconnect socket", e);
                    }

                    Intent i = new Intent(Constants.BT_DISCONNECTED_ACTION);
                    i.putExtra(ExtraConstants.STATUS, result);
                    sendBroadcast(i);
                }
                // To stop the service
                stopSelf();
            }
        });

        t.start();

        /*
         * START_STICKY runs the service till we explicitly stop the service
         */
        return START_NOT_STICKY;
    }
}
