/**
 * 
 */

package edu.isi.backpack.sync;

import android.content.ContextWrapper;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.util.BackpackUtils;

import org.toosheh.android.R;

import java.io.File;

/**
 * @author jenniferchen use this class once connection (wifi or bluetooth) from
 *         another device has been establish. This class doesn't know anything
 *         about bluetooth or wifi, it only takes care of the transaction after
 *         connection and before disconnection. The listener service class
 *         should take care of connection and disconnection.
 */
public class SyncListenerTransactor {

    public static final String TAG = "SyncListenerTransactor";

    private ContextWrapper context;

    private Connector conn;

    private MessageHandler mHandler;

    private File contentDir;

    private String remoteDevice;

    public SyncListenerTransactor(ContextWrapper context, Connector conn, MessageHandler mHandler,
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

        while (!terminate) {
            switch (transcState) {
                case Constants.META_DATA_EXCHANGE:
                    try {
                        mHandler.sendFullMetaData(Constants.META_DATA_FULL, metaFile);

                        mHandler.receiveFullMetaData(contentDir);

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
                        mHandler.receiveFiles(xferDir);

                        mHandler.sendContents(contentDir);

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

        // transaction is over
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
