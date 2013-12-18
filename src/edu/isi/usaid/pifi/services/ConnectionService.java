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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * This class is a service which initiates the connection to the remote server
 * 
 * @author mohit aggarwl
 * 
 */
public class ConnectionService extends Service {
	private static final String TAG = "ConnectionService";

	// Name for the SDP record when creating server socket
	private static final UUID MY_UUID = UUID
			/*.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");*/
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // this is the correct UUID for SPP

	private BluetoothAdapter mAdapter;
	private BluetoothSocket mmSocket;

	private InputStream mmInStream;
	private OutputStream mmOutStream;

	private boolean isExtDrMounted;

	private File path;

	private File metaFile;
	private File webMetaFile;

	private int transcState = Constants.NO_DATA_META;
	BluetoothDevice item;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onCreate() {
		// Debug.waitForDebugger();
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		isExtDrMounted = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
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

		// Debug.waitForDebugger();
		final BluetoothDevice item = intent.getExtras().getParcelable("Device");

		// use a seaparate thread for connection and data transfer
		Thread t = new Thread(new Runnable() {
			private List<Video> sendToVideos = new ArrayList<Video>();
			private List<Article> sendToWeb = new ArrayList<Article>();

			@Override
			public void run() {
				// get the slave bluetooth device

				BluetoothDevice device = mAdapter.getRemoteDevice(item
						.getAddress());

				// connect to slave
				try {
					// get the socket from the device
					Log.i(TAG, "Connecting");
					mmSocket = device
							.createRfcommSocketToServiceRecord(MY_UUID);
					mAdapter.cancelDiscovery();
					mmSocket.connect();
				} catch (IOException e) {

					// try reconnecting using reflection if that also fails then
					// show error message & exit.
					if (e.getMessage().equalsIgnoreCase(
							"Service discovery failed")) {
						Method m;
						try {
							m = device.getClass().getMethod(
									"createRfcommSocket",
									new Class[] { int.class });
							mmSocket = (BluetoothSocket) m.invoke(device, 1);
						} catch (NoSuchMethodException e1) {
							e1.printStackTrace();
						} catch (IllegalArgumentException e1) {
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							e1.printStackTrace();
						} catch (InvocationTargetException e1) {
							e1.printStackTrace();
						}
					} else {
						FileUtils.broadcastMessage(ConnectionService.this,
								"Connection with " + device.getName()
										+ "Failed");

						try {
							Log.e(TAG, "unable to connect, closing socket");
							Log.e(TAG, e.getMessage());
							mmSocket.close();
						} catch (IOException e2) {
							Log.e(TAG,
									"unable to close() socket during connection failure",
									e2);
						}

						return;
					}

				}

				// connection established
				Log.i(TAG, "Connection established");
				FileUtils.broadcastMessage(ConnectionService.this,
						"Successfully connected to " + device.getName());

				DataInputStream dis;
				DataOutputStream dos;
				if (mmSocket != null) {
					// Start point for data synchronnization
					try {
						mmInStream = mmSocket.getInputStream();
						mmOutStream = mmSocket.getOutputStream();
						dis = new DataInputStream(mmInStream);
						dos = new DataOutputStream(mmOutStream);
					} catch (IOException e) {
						Log.e(TAG, "unable to get in/out put streams", e);
						FileUtils.broadcastMessage(ConnectionService.this,
								"Error in initiating connection");
						return;
					}

					// start transactions
					boolean terminate = false;
					while (!terminate) {

						try {
							if (isExtDrMounted) {
								switch (transcState) {
								case Constants.NO_DATA_META:
									// read meta data and send video package
									// along with each file send its
									// metadata information Read from the
									// InputStream get delta from reading meta
									// data file from the slave

									byte[] buf = SocketUtils
											.receiveMetadataFile(metaFile,
													mmInStream);
									Log.i(TAG, "Received videos meta data file");
									// get delta entries to send/request
									Log.i(TAG, "Calculating delta for videos");
									FileUtils.getDeltaforVideos(buf, metaFile,
											sendToVideos);

									buf = SocketUtils.receiveMetadataFile(
											metaFile, mmInStream);
									Log.i(TAG,
											"Received web articles meta data file");
									// get delta entries to send/request
									Log.i(TAG,
											"Calculating delta for web articles");
									FileUtils.getDeltaforWeb(buf, webMetaFile,
											sendToWeb);

									FileUtils.broadcastMessage(
											ConnectionService.this,
											"Sending " + sendToVideos.size()
													+ " videos to: "
													+ device.getName());

									Log.i(TAG, "Sending videos");
									SocketUtils.sendVideoPackage(path,
											mmOutStream, sendToVideos);
									Log.i(TAG, "Videos Sent");

									// Wait here till you receive a message from
									// the Listener. Starts sending web content
									// only after that.
									short resp = dis.readShort();
									Log.i(TAG, "Listener responded:" + resp);

									// sending web content
									FileUtils.broadcastMessage(
											ConnectionService.this,
											"Sending " + sendToWeb.size()
													+ " entries to: "
													+ device.getName());

									Log.i(TAG, "Sending web articles");
									SocketUtils.sendWebPackage(path,
											mmOutStream, sendToWeb);
									Log.i(TAG, "Web Acticles Sent");

									// wait until listener responds
									// (listener has finished receiving)
									resp = dis.readShort();
									Log.i(TAG, "Listener responded: " + resp);

									transcState = Constants.META_DATA_RECEIVED;
									break;

								case Constants.META_DATA_RECEIVED:
									// send list of files required by the master
									// to slave

									Log.i(TAG, "Sending meta data for videos");

									SocketUtils.sendMetaData(metaFile,
											mmOutStream);
									Log.i(TAG,
											"Sending meta data for web articles");
									SocketUtils.sendMetaData(webMetaFile,
											mmOutStream);

									transcState = Constants.META_DATA_TO_SLAVE;
									break;

								case Constants.META_DATA_TO_SLAVE:
									// receive data from the slave and write it
									// to the file system

									File xferDir = new File(path,
											Constants.xferDirName + "/"
													+ device.getName());
									xferDir.mkdirs();
									Log.i(TAG, "Receiving Videos");
									int noOfFile = SocketUtils
											.readVideoFromSocket(xferDir, dis);
									FileUtils.broadcastMessage(
											ConnectionService.this, "Received "
													+ noOfFile
													+ " video files from: "
													+ device.getName());
									Log.i(TAG, "Successfully received Videos");

									// send some beacon message to the sender
									// waiting to send web content
									dos.writeShort(Constants.OK_RESPONSE);

									// start receiving web content files
									Log.i(TAG, "Receiving Web articles");
									noOfFile = SocketUtils
											.readArticlesFromSocket(xferDir,
													dis);
									Log.i(TAG,
											"Successfully received Web articles");
									FileUtils
											.broadcastMessage(
													ConnectionService.this,
													"Received "
															+ noOfFile
															+ " web content files from: "
															+ device.getName());

									Log.i(TAG,
											"Replying back with status message");
									dos.writeShort(Constants.OK_RESPONSE);

									transcState = Constants.DATA_FROM_SLAVE;
									break;

								case Constants.DATA_FROM_SLAVE:
									// close the connection socket
									terminate = true;
									transcState = Constants.NO_DATA_META;
									break;
								}
							}

						} catch (IOException e) {
							FileUtils.broadcastMessage(ConnectionService.this,
									"Connection lost with " + device.getName());
							break;
						}

					}

					if (terminate) {
						FileUtils.broadcastMessage(ConnectionService.this,
								"File sync is successful. Closing the session");
						try {
							mmInStream.close();
							mmOutStream.close();
							mmSocket.close();
						} catch (IOException e) {
							Log.e(TAG, "Unable to disconnect socket", e);
						}
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
}