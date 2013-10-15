/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 * 
 * @author mohit aggarwal
 * 
 */
public class ListenerService extends Service {

	private static final String TAG = "ListenerService";

	// Name for the SDP record when creating server socket
	private static final String FTP_SERVICE = "CustomFTPService";
	private static final UUID MY_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// private Context context;
	private BluetoothAdapter mAdapter;
	private InputStream mmInStream;
	private OutputStream mmOutStream;
	private BluetoothServerSocket mmServerSocket;
	private boolean isExtDrMounted;

	private byte buffer[] = new byte[8 * 1024]; // optimal buffer size for
												// transferring data using
												// Android APIs

	private File path, metaFile;

	/**
	 * For control messages during data communication over sockets
	 */
	private int transcState = Constants.NO_DATA_META;

	/**
	 * Consturctor for the class.
	 * 
	 * 
	 */
	public void onCreate() {
		// context = bluetoothFileTransferActivity;
		isExtDrMounted = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
		File sdr = Environment.getExternalStorageDirectory();
		path = new File(sdr, Constants.contentDirName);
		if (!path.exists()) {
			path.mkdir();
		}
		metaFile = new File(path, Constants.metaFileName);
		if (!metaFile.exists()) {
			// TODO something to handle this case. Jennifer can you try pluging
			// your fix here and seeif it works.
		}
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		// start the server socket for listening the incoming connections
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mAdapter != null && mAdapter.isEnabled()) {
			// TODO we need to keep listening for bluetooth events, when it is
			// turned on and off. based on that information we need to start or
			// stop our BluetoothServerSocket. Other use cases like connection
			// state/data persistent caused by this
			// functionality can be taken care after the demo.
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(FTP_SERVICE,
						MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "AcceptThread: Socket listen() failed", e);
			}
			mmServerSocket = tmp;

			while (true) { // TODO && bluetooth radio is running
				BluetoothSocket socket = null;
				if (mmServerSocket != null) {
					try {
						// This is a blocking call and will only return on a
						// successful connection or an exception
						socket = mmServerSocket.accept();
						mmInStream = socket.getInputStream();
						mmOutStream = socket.getOutputStream();
					} catch (IOException e) {
						Log.e(TAG,
								"Exception while opening child socket for data transfer",
								e);
						break;
					}
				}

				if (socket != null) {
					DataInputStream din = new DataInputStream(mmInStream);
					boolean transfer = false;

					while (!transfer) {
						switch (transcState) {
						case Constants.NO_DATA_META:
							// send meta data file to the connecting
							// device(master)
							// TODO this done only for videos content, web
							// content implementation is similar. Once video
							// content functionality is tested properly add
							// similar code for web content
							FileInputStream fin = null;
							try {
								fin = new FileInputStream(metaFile);
								while (fin.read(buffer) != -1) {
									SocketUtils.writeToSocket(mmOutStream,
											buffer);
								}
							} catch (FileNotFoundException e) {
								Log.e(TAG, "unable to open file input stream",
										e);
							} catch (IOException e) {
								Log.e(TAG, "unable to perform i/o on file: "
										+ metaFile.getName(), e);
							} finally {
								try {
									fin.close();
								} catch (IOException e) {
									Log.e(TAG,
											"Exception while cloding the file input stream",
											e);
								}
							}

							transcState = Constants.META_TO_MASTER;
							break;

						case Constants.META_TO_MASTER:
							// receive package from the master.
							// TODO first write to a .tmp file and once while is
							// completely transferred rename it to remove .tmp
							File xferDir = new File(path, Constants.xferDirName
									+ "/" + socket.getRemoteDevice().getName());
							SocketUtils.readFromSocket(xferDir, din);
							// TODO just identified a bug.. this flow of control
							// will only work this flow will only with the
							// packages having one file. will fix this issue
							// some time on monday 10/14.

							transcState = Constants.DATA_FROM_MASTER;
							break;

						case Constants.DATA_FROM_MASTER:
							// read the requested file from the master and send
							// the packages data back to master.
							List<Video> paths = new ArrayList<Video>();

							FileUtils
									.getMasterRequestList(din, metaFile, paths);

							if (paths != null) {
								SocketUtils
										.sendVideoPackage(mmOutStream, paths);
							}

							transcState = Constants.DATA_TO_MASTER;
							break;

						case Constants.DATA_TO_MASTER:
							// file sync is complete tear down the connnection.
							transfer = true;
							transcState = Constants.NO_DATA_META;
							break;
						}
					}

					// close the socket
					try {
						mmInStream.close();
						mmOutStream.close();
						socket.close();
					} catch (IOException e) {
						Log.e(TAG,
								"Exception while closing child socket after file sync is completed",
								e);
					}
				}// end if(socket!=null)
			}
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
