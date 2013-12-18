/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
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
import android.os.Looper;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
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
			/*.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");*/
			.fromString("00001101-0000-1000-8000-00805F9B34FB");// this is the correct UUID for SPP

	private BluetoothAdapter mAdapter;
	private BluetoothServerSocket mmServerSocket;

	private File path, metaFile, webMetaFile;

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
		// Debug.waitForDebugger();

		File sdr = Environment.getExternalStorageDirectory();
		path = new File(sdr, Constants.contentDirName);
		if (!path.exists()) {
			path.mkdir();
		}
		metaFile = new File(path, Constants.metaFileName);
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

				FileUtils.broadcastMessage(ListenerService.this,
						"Successfully connected to "
								+ commSock.getRemoteDevice().getName());

				DataInputStream din = new DataInputStream(mmInStream);
				DataOutputStream dos = new DataOutputStream(mmOutStream);
				boolean transfer = false;

				while (!transfer) {
					switch (transcState) {
					case Constants.NO_DATA_META:

						// send meta data file to the connecting
						// device(master)
						Log.i(TAG, "Sending meta data for videos");
						SocketUtils.sendMetaData(metaFile, mmOutStream);
						Log.i(TAG, "Sending meta data for web articles");
						SocketUtils.sendMetaData(webMetaFile, mmOutStream);

						transcState = Constants.META_TO_MASTER;
						break;

					case Constants.META_TO_MASTER:

						Log.i(TAG, "Receiving video files ");

						// receive package from the master.
						File xferDir = new File(path, Constants.xferDirName
								+ "/" + commSock.getRemoteDevice().getName());
						xferDir.mkdirs();
						Log.i(TAG, "Receiving Videos");
						int noOfFiles = SocketUtils.readVideoFromSocket(
								xferDir, din);
						FileUtils.broadcastMessage(ListenerService.this,
								"Received " + noOfFiles + " video files from: "
										+ commSock.getRemoteDevice().getName());
						Log.i(TAG, "Successfully received Videos");

						// send back some message to waiting client to send the
						// web content
						try {
							Log.i(TAG, "Replying back with status message");
							dos.writeShort(Constants.OK_RESPONSE);
						} catch (IOException e1) {
							Log.e(TAG,
									"Tansaction state: "
											+ transcState
											+ ": Error replying back with status message");
							Log.e(TAG, e1.getMessage());
						}

						Log.i(TAG, "Receiving Web articles");
						noOfFiles = SocketUtils.readArticlesFromSocket(xferDir,
								din);
						Log.i(TAG, "Successfully received Web articles");
						FileUtils.broadcastMessage(ListenerService.this,
								"Received " + noOfFiles
										+ " web article files from: "
										+ commSock.getRemoteDevice().getName());

						// once the data is received send some dummy message
						// back to the sender so that it can come out of the
						// read block and send us the list of files it wants
						// from us.
						try {
							Log.i(TAG, "Replying back with status message");
							dos.writeShort(Constants.OK_RESPONSE);
						} catch (IOException e) {
							Log.e(TAG,
									"Tansaction state: "
											+ transcState
											+ ": Error replying back with status message");
							Log.e(TAG, e.getMessage());
						}

						transcState = Constants.DATA_FROM_MASTER;
						break;

					case Constants.DATA_FROM_MASTER:
						// read the requested file from the master
						// and send the packages data back to master.

						List<Video> videoPaths = new ArrayList<Video>();
						List<Article> webPaths = new ArrayList<Article>();

						try {
							byte[] buf = SocketUtils.receiveMetadataFile(
									metaFile, mmInStream);
							Log.i(TAG, "Received videos meta data file");
							// get delta entries to send/request
							Log.i(TAG, "Calculating delta for videos");
							FileUtils.getDeltaforVideos(buf, metaFile,
									videoPaths);

							buf = SocketUtils.receiveMetadataFile(metaFile,
									mmInStream);
							Log.i(TAG, "Received web articles meta data file");
							Log.i(TAG, "Calculating delta for web articles");
							FileUtils
									.getDeltaforWeb(buf, webMetaFile, webPaths);
						} catch (IOException e1) {
							Log.e(TAG, "Tansaction state: " + transcState
									+ ": Error receiving status message");
							Log.e(TAG, e1.getMessage());
						}

						if (videoPaths != null) {
							FileUtils.broadcastMessage(ListenerService.this,
									"Sending "
											+ videoPaths.size()
											+ " videos to: "
											+ commSock.getRemoteDevice()
													.getName());

							Log.i(TAG, "Sending videos");
							SocketUtils.sendVideoPackage(path, mmOutStream,
									videoPaths);
							Log.i(TAG, "Videos Sent");

							// wait for the reply before proceeding
							try {
								int resp = din.readShort();
								Log.i(TAG, "Listener responded:" + resp);
							} catch (IOException e) {
								Log.e(TAG, "Tansaction state: " + transcState
										+ ": Error receiving status message");
								Log.e(TAG, e.getMessage());
							}
						}

						if (webPaths != null) {
							FileUtils.broadcastMessage(ListenerService.this,
									"Sending "
											+ webPaths.size()
											+ " web articles to: "
											+ commSock.getRemoteDevice()
													.getName());

							Log.i(TAG, "Sending web articles");
							SocketUtils.sendWebPackage(path, dos, webPaths);
							Log.i(TAG, "Web Acticles Sent");

							// wait for the reply before proceeding
							try {
								short resp = din.readShort();
								Log.i(TAG, "Listener responded: " + resp);
							} catch (IOException e) {
								Log.e(TAG, "Tansaction state: " + transcState
										+ ": Error receiving status message");
								Log.e(TAG, e.getMessage());
							}
						}

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
					FileUtils.broadcastMessage(ListenerService.this,
							"File sync is successful. Closing the session");
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
		return null;
	}

}
