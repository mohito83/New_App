/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.ExtraConstants;
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
		Debug.waitForDebugger();

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

		Log.i(TAG, "listener service started");

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				/*
				 * Notification notification = new Notification.Builder(
				 * ListenerService.this).setContentTitle("Pifi Sync")
				 * .setContentText("Pifi Sync Service")
				 * .setSmallIcon(R.drawable.ic_action_sync) .getNotification();
				 * startForeground(7456, notification);
				 */

				Looper.prepare();
				// start the server socket for listening the incoming
				// connections
				mAdapter = BluetoothAdapter.getDefaultAdapter();
				if (mAdapter != null && mAdapter.isEnabled()) {
					// TODO we need to keep listening for bluetooth events, when
					// it is
					// turned on and off. based on that information we need to
					// start or
					// stop our BluetoothServerSocket. Other use cases like
					// connection
					// state/data persistent caused by this
					// functionality can be taken care after the demo.

					while (true) { // TODO && bluetooth radio is running

						// create server socket if not created
						if (mmServerSocket == null) {
							mmServerSocket = createServerSocket();
							Log.i(TAG, "Socket opened");
						}
						BluetoothSocket socket = null;
						if (mmServerSocket != null) {
							while (socket == null) {
								try {
									// This is a blocking call and will only
									// return on a
									// successful connection or an exception
									Log.i("ListenerService",
											"Listening for connection");
									socket = mmServerSocket.accept();
								} catch (IOException e) { // timed out
									socket = null;
									Log.i(TAG, "Connection failed");
								}
							}

						}

						// connection established
						Log.i(TAG, "Connection established");

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
			} catch (IOException e) {
				socket = null;
				Log.i(TAG,
						"Trying to get socket IOStream while socket is not connected");
			}
		}

		@Override
		public void run() {
			if (commSock != null) {

				Intent i = new Intent();
				i.setAction(Constants.BT_STATUS_ACTION);
				i.putExtra(ExtraConstants.STATUS, "Connected");
				sendBroadcast(i);

				DataInputStream din = new DataInputStream(mmInStream);
				DataOutputStream dos = new DataOutputStream(mmOutStream);
				boolean transfer = false;

				while (!transfer) {
					switch (transcState) {
					case Constants.NO_DATA_META:

						// send meta data file to the connecting
						// device(master)
						// TODO this done only for videos content,
						// web
						// content implementation is similar. Once
						// video
						// content functionality is tested properly
						// add
						// similar code for web content

						FileInputStream fin = null;
						try {

							Log.i(TAG, "Sending meta file size");

							// 1. send metadata size
							long byteCount = metaFile.length();
							dos.writeLong(byteCount);

							Log.i(TAG, "Sending meta file");

							// 2. send metadata file
							fin = new FileInputStream(metaFile);
							int tb = 0;
							int b = 0;
							while (b < byteCount) {
								b = fin.read(buffer, 0, Math.min((int) byteCount
										- tb,buffer.length));
								dos.write(buffer,0,b);
								tb += b;
							}
							dos.flush();
							i = new Intent();
							i.setAction(Constants.BT_STATUS_ACTION);
							i.putExtra(ExtraConstants.STATUS, "Sent metadata: "
									+ tb + " bytes");
							sendBroadcast(i);

							Log.i(TAG, "Sent metadata " + tb + " bytes");

						} catch (FileNotFoundException e) {
							Log.e(TAG, "unable to open file input stream", e);
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

						Log.i(TAG, "Receiving video files ");

						// receive package from the master.
						// TODO first write to a .tmp file and once
						// while is
						// completely transferred rename it to
						// remove .tmp
						File xferDir = new File(path, Constants.xferDirName
								+ "/" + commSock.getRemoteDevice().getName());
						xferDir.mkdirs();
						SocketUtils.readFromSocket(xferDir, din);

						// once the data is received send some dummy message
						// back to the sender so that it can come out of the
						// read block and send us the list of files it wants
						// from us.
						//dos = new DataOutputStream(mmOutStream);
						String str = "Success!!";
						try {
							mmOutStream.write(str.getBytes());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//SocketUtils.writeToSocket(dos, str.getBytes());
						// TODO just identified a bug.. this flow of
						// control
						// will only work this flow will only with
						// the
						// packages having one file. will fix this
						// issue
						// some time on monday 10/14.

						Log.i(TAG, "Video files received");

						transcState = Constants.DATA_FROM_MASTER;
						break;

					case Constants.DATA_FROM_MASTER:
						// read the requested file from the master
						// and send
						// the packages data back to master.

						Log.i(TAG, "Sending local package");

						List<Video> paths = new ArrayList<Video>();

						FileUtils.getMasterRequestList(din, metaFile, paths);

						if (paths != null) {
							SocketUtils.sendVideoPackage(path, mmOutStream,
									paths);
						}

						Log.i(TAG, "Local package sent");
						
						// wait until connector responds
						// (connector has finished receiving)
						byte[] buff = new byte[20];
						try {
							mmInStream.read(buff);
						} catch (IOException e) {
							e.printStackTrace();
						}
						Log.i(TAG, "Connector responded: " + new String(buff));

						transcState = Constants.DATA_TO_MASTER;
						break;

					case Constants.DATA_TO_MASTER:

						Log.i(TAG, "Transaction complete");

						// file sync is complete tear down the
						// connnection.
						transfer = true;
						transcState = Constants.NO_DATA_META;
						break;
					}
				}

				// close the socket
				try {
					Log.i(TAG, "Close socket");
					mmInStream.close();
					mmOutStream.close();
					commSock.close();
				} catch (IOException e) {
					Log.e(TAG,
							"Exception while closing child socket after file sync is completed",
							e);
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
		// TODO Auto-generated method stub
		return null;
	}

}
