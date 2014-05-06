/**
 * 
 */

package edu.isi.backpack.services;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;
import edu.isi.backpack.sync.Connector;
import edu.isi.backpack.sync.MessageHandler;
import edu.isi.backpack.sync.SyncConnectorTransactor;
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

    @Override
    public void onCreate() {

        Log.i(TAG, "Starting wifi connection service");

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

        final NsdServiceInfo device = (NsdServiceInfo) intent
                .getParcelableExtra(ExtraConstants.DEVICE);

        Thread t = new Thread(new Runnable() {

            private Socket mmSocket;

            private InputStream mmInStream;

            private OutputStream mmOutStream;

            private Connector conn;

            private MessageHandler mHandler;

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
                        mHandler = new MessageHandler(conn, WifiConnectionService.this, metaFile);
                    } catch (IOException e) {
                        Log.e(TAG, "unable to get in/out put streams", e);
                        sendBroadcast(new Intent(WifiConstants.CONNECTION_CLOSED_ACTION));
                        BackpackUtils.broadcastMessage(WifiConnectionService.this,
                                "Error in initiating connection");
                        return;
                    }

                    // run transactions
                    SyncConnectorTransactor sc = new SyncConnectorTransactor(
                            WifiConnectionService.this, conn, mHandler, path,
                            device.getServiceName());
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
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to disconnect socket", e);
                    }

                    Intent i = new Intent(WifiConstants.CONNECTION_CLOSED_ACTION);
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

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
