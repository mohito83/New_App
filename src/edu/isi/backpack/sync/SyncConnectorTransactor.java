/**
 * 
 */

package edu.isi.backpack.sync;

import android.content.ContextWrapper;
import android.util.Log;

import org.toosheh.android.R;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.util.BackpackUtils;

import java.io.File;

/**
 * @author jenniferchen use this class once connection (wifi or bluetooth) to
 *         another device has been establish. This class doesn't know anything
 *         about bluetooth or wifi, it only takes care of the transaction after
 *         connection and before disconnection. The connector service class
 *         should take care of connection and disconnection.
 */
public class SyncConnectorTransactor {

    public static final String TAG = "SyncConnectorTransactor";

    private ContextWrapper context;

    private Connector conn;

    private MessageHandler mHandler;

    private File contentDir;

    private String remoteDevice;

    public SyncConnectorTransactor(ContextWrapper context, Connector conn, MessageHandler mHandler,
            File contentDir, String remoteDevice) {

        this.context = context;
        this.conn = conn;
        this.mHandler = mHandler;
        this.contentDir = contentDir;
        this.remoteDevice = remoteDevice;
    }

    public String run() {

        File metaFile = new File(contentDir, Constants.metaFileName);

        boolean terminate = false;
        boolean disconnected = false;
        int transcState = Constants.META_DATA_EXCHANGE;

        // start transactions
        while (!terminate) {

            // try {
            switch (transcState) {
                case Constants.META_DATA_EXCHANGE:
                    try {
                        Log.i(TAG, "Receiving meta data");
                        mHandler.receiveFullMetaData(contentDir);

                        Log.i(TAG, "Sending meta data");
                        mHandler.sendFullMetaData(Constants.META_DATA_FULL, metaFile);

                        transcState = Constants.FILE_DATA_EXCHANGE;
                        break;
                    } catch (BluetoothDisconnectedException e) {
                        terminate = true;
                        disconnected = true;
                        break;
                    }

                case Constants.FILE_DATA_EXCHANGE:
                    try {
                        File xferDir = new File(contentDir, Constants.xferDirName + "/"
                                + remoteDevice);
                        xferDir.mkdirs();
                        Log.i(TAG, "Start sending contents");
                        mHandler.sendContents(contentDir);
                        Log.i(TAG, "Finished sending contents");

                        Log.i(TAG, "Start receiving contents");
                        mHandler.receiveFiles(xferDir);
                        Log.i(TAG, "Finished receiving contents");

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

        // state is 'terminate'
        String message;
        if (disconnected) // got disconnected in the middle of
                          // transfer
            message = context.getString(R.string.file_sync_incomplete) + ": " + remoteDevice;
        else
            message = context.getString(R.string.file_sync_successful) + ": " + remoteDevice;
        conn.cancelNotification();
        BackpackUtils.broadcastMessage(context, message);

        return message;
    }

}
