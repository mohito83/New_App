/**
 * 
 */

package edu.isi.backpack.services;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import edu.isi.backpack.R;
import edu.isi.backpack.bluetooth.BluetoothDisconnectedException;
import edu.isi.backpack.bluetooth.Connector;
import edu.isi.backpack.bluetooth.MessageHandler;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;
import edu.isi.backpack.util.BackpackUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author jenniferchen
 */
public class WifiConnectionService extends Service {

    public static final String TAG = "WifiConnectionService";

    private File path;

    private File metaFile;

    private File webMetaFile;

    @Override
    public void onCreate() {
        
        Log.i(TAG, "Starting wifi connection service");
        
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
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        final NsdServiceInfo device = (NsdServiceInfo) intent
                .getParcelableExtra(ExtraConstants.DEVICE);

        Thread t = new Thread(new Runnable() {

            private Socket mmSocket;

            private InputStream mmInStream;

            private OutputStream mmOutStream;

            private Connector conn;

            private MessageHandler mHanlder;

            private int transcState = Constants.META_DATA_EXCHANGE;

            @Override
            public void run() {
                Log.i(TAG, "connecting");
                try {
                    mmSocket = new Socket(device.getHost(), device.getPort());
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed to " + device);
                    Log.e(TAG, "Closing socket");
                    try {
                        mmSocket.close();
                    } catch (IOException e1) {
                        Log.e(TAG, "Closing socket failed");
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }

                // connection established
                Log.i(TAG, "Connection established");
                sendBroadcast(new Intent(WifiConstants.CONNECTION_ESTABLISHED_ACTION));
                BackpackUtils.broadcastMessage(WifiConnectionService.this,
                        "Successfully connected to " + device.getServiceName());

                if (mmSocket != null) {
                    // Start point for data synchronization
                    try {
                        mmInStream = mmSocket.getInputStream();
                        mmOutStream = mmSocket.getOutputStream();
                        conn = new Connector(mmInStream, mmOutStream, getApplicationContext());
                        mHanlder = new MessageHandler(conn, WifiConnectionService.this, metaFile,
                                webMetaFile);
                    } catch (IOException e) {
                        Log.e(TAG, "unable to get in/out put streams", e);
                        sendBroadcast(new Intent(WifiConstants.CONNECTION_CLOSED_ACTION));
                        BackpackUtils.broadcastMessage(WifiConnectionService.this,
                                "Error in initiating connection");
                        return;
                    }

                    // start transactions
                    boolean terminate = false;
                    boolean disconnected = false;

                    while (!terminate) {

                        // try {
                        switch (transcState) {
                            case Constants.META_DATA_EXCHANGE:
                                try {
                                    Log.i(TAG, "Receiving videos meta data");
                                    mHanlder.receiveFullMetaData(path);

                                    Log.i(TAG, "Sending videos meta data");
                                    mHanlder.sendFullMetaData(Constants.VIDEO_META_DATA_FULL,
                                            metaFile);

                                    Log.i(TAG, "Receiving web meta data");
                                    mHanlder.receiveFullMetaData(path);

                                    Log.i(TAG, "Sending web meta data");
                                    mHanlder.sendFullMetaData(Constants.WEB_META_DATA_FULL,
                                            webMetaFile);

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
                                            + device.getServiceName());
                                    xferDir.mkdirs();
                                    Log.i(TAG, "Start sending web contents");
                                    mHanlder.sendWebContent(path);
                                    Log.i(TAG, "Finished sending web contents");

                                    Log.i(TAG, "Start receiving web contents");
                                    mHanlder.receiveFiles(xferDir);
                                    Log.i(TAG, "Finished receiving web contents");

                                    Log.i(TAG, "Start sending videos");
                                    mHanlder.sendVideos(path);
                                    Log.i(TAG, "Finished sending videos");

                                    Log.i(TAG, "Start receiving videos");
                                    mHanlder.receiveFiles(xferDir);
                                    Log.i(TAG, "Finished receiving videos");

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

                    if (terminate) {
                        String message;
                        if (disconnected) // got disconnected in the middle of
                                          // transfer
                            message = getString(R.string.file_sync_incomplete);
                        else
                            message = getString(R.string.file_sync_successful);
                        conn.cancelNotification();
                        BackpackUtils.broadcastMessage(WifiConnectionService.this, message);

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
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to disconnect socket", e);
                        }

                        Intent i = new Intent(WifiConstants.CONNECTION_CLOSED_ACTION);
                        i.putExtra(ExtraConstants.STATUS, message);
                        sendBroadcast(i);
                    }
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

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
