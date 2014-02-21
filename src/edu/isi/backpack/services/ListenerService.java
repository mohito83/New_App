/**
 * 
 */
package edu.isi.backpack.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import edu.isi.backpack.bluetooth.BluetoothDisconnectedException;
import edu.isi.backpack.bluetooth.Connector;
import edu.isi.backpack.bluetooth.MessageHandler;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.util.BackpackUtils;

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

	/**
	 * Consturctor for the class.
	 * 
	 * 
	 */
	public void onCreate() {
//		Debug.waitForDebugger();

		File sdr = Environment.getExternalStorageDirectory();
		path = new File(sdr, Constants.contentDirName);
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
			Log.e(TAG,
					"Unable to create a empty meta data file" + e.getMessage());
		}
	}

	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.i(TAG, "listener service started");

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {

				Looper.prepare();
				// start the server socket for listening the incoming
				// connections
				mAdapter = BluetoothAdapter.getDefaultAdapter();
				if (mAdapter != null && mAdapter.isEnabled()) {

					while (true) {

						// create server socket if not created
						if (mmServerSocket == null) {
							mmServerSocket = createServerSocket();
						}
						BluetoothSocket socket = null;
						if (mmServerSocket != null) {
							Log.i(TAG, "Server Socket created");
							while (socket == null) {
								try {
									// This is a blocking call and will only
									// return on a
									// successful connection or an exception
									Log.i(TAG,
											"Listening for incoming connection requests");
									socket = mmServerSocket.accept();
								} catch (IOException e) { // timed out
									socket = null;
									Log.e(TAG,
											"Incoming Connection failed"
													+ e.getMessage());
								}
							}

						}

						// connection established
						Log.i(TAG, "Connection established");
						sendBroadcast(new Intent(Constants.BT_CONNECTED_ACTION));

						if (socket != null) {
							Thread commThread = new Thread(
									new CommunicationSocket(socket));
							commThread.start();
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
			tmp = mAdapter.listenUsingRfcommWithServiceRecord(FTP_SERVICE,
					MY_UUID);
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
	 * 
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
				conn = new Connector(mmInStream, mmOutStream);
				mHanlder = new MessageHandler(conn, ListenerService.this,
						metaFile, webMetaFile);
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

				BackpackUtils.broadcastMessage(ListenerService.this,
						"Successfully connected to "
								+ commSock.getRemoteDevice().getName());

				boolean terminate = false;
				boolean disconnected = false;
				
				while (!terminate) {
					switch (transcState) {
					case Constants.META_DATA_EXCHANGE:
						try {
							Log.i(TAG, "Sending videos meta data");
							mHanlder.sendFullMetaData(
									Constants.VIDEO_META_DATA_FULL, metaFile);
							Log.i(TAG, "Receiving videos meta data");
							mHanlder.receiveFullMetaData(path);
	
							Log.i(TAG, "Sending web meta data");
							mHanlder.sendFullMetaData(Constants.WEB_META_DATA_FULL,
									webMetaFile);
							Log.i(TAG, "Receiving web meta data");
							mHanlder.receiveFullMetaData(path);
	
							transcState = Constants.FILE_DATA_EXCHANGE;
							break;
						} catch (BluetoothDisconnectedException e){
							terminate = true;
							disconnected = true;
							break;
						}

					case Constants.FILE_DATA_EXCHANGE:
						try {
							File xferDir = new File(path, Constants.xferDirName
									+ "/" + commSock.getRemoteDevice().getName());
							xferDir.mkdirs();
							Log.i(TAG, "Start receiving videos");
							mHanlder.receiveFiles(xferDir);
							Log.i(TAG, "Finished receiving videos");
	
							Log.i(TAG, "Start sending videos");
							mHanlder.sendVideos(path);
							Log.i(TAG, "Finished sending videos");
	
							Log.i(TAG, "Start receiving web contents");
							mHanlder.receiveFiles(xferDir);
							Log.i(TAG, "Finished receiving web contents");
	
							Log.i(TAG, "Start sending web contents");
							mHanlder.sendWebContent(path);
							Log.i(TAG, "Finished sending web contents");
	
							transcState = Constants.SYNC_COMPLETE;
							terminate = true;
							break;
						} catch (BluetoothDisconnectedException e){
							terminate = true;
							disconnected = true;
							break;
						}
					}
				}

				// close the socket
				if (terminate) {
					String message;
					if (disconnected)  // got disconnected in the middle of transfer
						message = "File sync incomplete.";
					else 
						message = "File sync is successful. Closing the session";
					
					BackpackUtils.broadcastMessage(ListenerService.this,
							message);
					
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
		Log.i(TAG, "listener service destroyed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
