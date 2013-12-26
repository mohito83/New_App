/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;

/**
 * This class is a service which initiates the connection to the remote server
 * 
 * @author mohit aggarwl
 * 
 */
public class ConnectionService extends Service {
	private static final String TAG = "BackPackConnectionService";

	// Name for the SDP record when creating server socket
	private static final UUID MY_UUID = UUID
	/* .fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); */
	.fromString("00001101-0000-1000-8000-00805F9B34FB"); // this is the correct
															// UUID for SPP

	private BluetoothAdapter mAdapter;
	private boolean isExtDrMounted;

	private File path;

	private File metaFile;
	private File webMetaFile;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onCreate() {
		 Debug.waitForDebugger();
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
			private BluetoothSocket mmSocket;

			private InputStream mmInStream;
			private OutputStream mmOutStream;

			private Connector conn;
			private MessageHandler mHanlder;

			private int transcState = Constants.META_DATA_EXCHANGE;

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

				if (mmSocket != null) {
					// Start point for data synchronization
					try {
						mmInStream = mmSocket.getInputStream();
						mmOutStream = mmSocket.getOutputStream();
						conn = new Connector(mmInStream, mmOutStream);
						mHanlder = new MessageHandler(conn,
								ConnectionService.this, metaFile, webMetaFile);
					} catch (IOException e) {
						Log.e(TAG, "unable to get in/out put streams", e);
						FileUtils.broadcastMessage(ConnectionService.this,
								"Error in initiating connection");
						return;
					}

					// start transactions
					boolean terminate = false;
					int isVidtoSend = 0, isVidToRecv = 0;
					int isWebtoSend = 0, isWebToRecv = 0;

					while (!terminate) {

						// try {
						if (isExtDrMounted) {
							switch (transcState) {
							case Constants.META_DATA_EXCHANGE:
								Log.i(TAG, "Receiving videos meta data");
								isVidtoSend = mHanlder
										.receiveFullMetaData(path);

								Log.i(TAG, "Sending videos meta data");
								isVidToRecv = mHanlder.sendFullMetaData(
										Constants.VIDEO_META_DATA_FULL,
										metaFile);

								Log.i(TAG, "Receiving web meta data");
								isWebtoSend = mHanlder
										.receiveFullMetaData(path);

								Log.i(TAG, "Sending web meta data");
								isWebToRecv = mHanlder.sendFullMetaData(
										Constants.WEB_META_DATA_FULL,
										webMetaFile);

								transcState = Constants.FILE_DATA_EXCHANGE;
								break;

							case Constants.FILE_DATA_EXCHANGE:
								File xferDir = new File(path,
										Constants.xferDirName + "/"
												+ device.getName());
								xferDir.mkdirs();
								if (isVidtoSend > 0) {
									Log.i(TAG, "Start sending videos");
									mHanlder.sendVideos(path);
									Log.i(TAG, "Finished sending videos");
								}

								if (isVidToRecv > 0) {
									Log.i(TAG, "Start receiving videos");
									mHanlder.receiveVideos(xferDir);
									Log.i(TAG, "Finished receiving videos");
								}

								if (isWebtoSend > 0) {
									Log.i(TAG, "Start sending web contents");
									mHanlder.sendWebContent(path);
									Log.i(TAG, "Finished sending web contents");
								}

								if (isWebToRecv > 0) {
									Log.i(TAG, "Start receiving web contents");
									mHanlder.receiveWebContent(xferDir);
									Log.i(TAG,
											"Finished receiving web contents");
								}

								transcState = Constants.SYNC_COMPLETE;
								terminate = true;
								break;
							/*
							 * byte[] buf = SocketUtils
							 * .receiveMetadataFile(metaFile, mmInStream);
							 * Log.i(TAG, "Received videos meta data file"); //
							 * get delta entries to send/request Log.i(TAG,
							 * "Calculating delta for videos");
							 * FileUtils.getDeltaforVideos(buf, metaFile,
							 * sendToVideos);
							 * 
							 * buf = SocketUtils.receiveMetadataFile( metaFile,
							 * mmInStream); Log.i(TAG,
							 * "Received web articles meta data file"); // get
							 * delta entries to send/request Log.i(TAG,
							 * "Calculating delta for web articles");
							 * FileUtils.getDeltaforWeb(buf, webMetaFile,
							 * sendToWeb);
							 * 
							 * FileUtils.broadcastMessage(
							 * ConnectionService.this, "Sending " +
							 * sendToVideos.size() + " videos to: " +
							 * device.getName());
							 * 
							 * Log.i(TAG, "Sending videos");
							 * SocketUtils.sendVideoPackage(path, mmOutStream,
							 * sendToVideos); Log.i(TAG, "Videos Sent");
							 * 
							 * // Wait here till you receive a message from //
							 * the Listener. Starts sending web content // only
							 * after that. short resp = dis.readShort();
							 * Log.i(TAG, "Listener responded:" + resp);
							 * 
							 * // sending web content
							 * FileUtils.broadcastMessage(
							 * ConnectionService.this, "Sending " +
							 * sendToWeb.size() + " entries to: " +
							 * device.getName());
							 * 
							 * Log.i(TAG, "Sending web articles");
							 * SocketUtils.sendWebPackage(path, mmOutStream,
							 * sendToWeb); Log.i(TAG, "Web Acticles Sent");
							 * 
							 * // wait until listener responds // (listener has
							 * finished receiving) resp = dis.readShort();
							 * Log.i(TAG, "Listener responded: " + resp);
							 * 
							 * transcState = Constants.META_DATA_RECEIVED;
							 * break;
							 * 
							 * case Constants.META_DATA_RECEIVED: // send list
							 * of files required by the master // to slave
							 * 
							 * Log.i(TAG, "Sending meta data for videos");
							 * 
							 * SocketUtils.sendMetaData(metaFile, mmOutStream);
							 * Log.i(TAG, "Sending meta data for web articles");
							 * SocketUtils.sendMetaData(webMetaFile,
							 * mmOutStream);
							 * 
							 * transcState = Constants.META_DATA_TO_SLAVE;
							 * break;
							 * 
							 * case Constants.META_DATA_TO_SLAVE: // receive
							 * data from the slave and write it // to the file
							 * system
							 * 
							 * File xferDir = new File(path,
							 * Constants.xferDirName + "/" + device.getName());
							 * xferDir.mkdirs(); Log.i(TAG, "Receiving Videos");
							 * int noOfFile = SocketUtils
							 * .readVideoFromSocket(xferDir, dis);
							 * FileUtils.broadcastMessage(
							 * ConnectionService.this, "Received " + noOfFile +
							 * " video files from: " + device.getName());
							 * Log.i(TAG, "Successfully received Videos");
							 * 
							 * // send some beacon message to the sender //
							 * waiting to send web content
							 * dos.writeShort(Constants.OK_RESPONSE);
							 * 
							 * // start receiving web content files Log.i(TAG,
							 * "Receiving Web articles"); noOfFile = SocketUtils
							 * .readArticlesFromSocket(xferDir, dis); Log.i(TAG,
							 * "Successfully received Web articles"); FileUtils
							 * .broadcastMessage( ConnectionService.this,
							 * "Received " + noOfFile +
							 * " web content files from: " + device.getName());
							 * 
							 * Log.i(TAG, "Replying back with status message");
							 * dos.writeShort(Constants.OK_RESPONSE);
							 * 
							 * transcState = Constants.DATA_FROM_SLAVE; break;
							 * 
							 * case Constants.DATA_FROM_SLAVE: // close the
							 * connection socket terminate = true; transcState =
							 * Constants.META_DATA_EXCHANGE; break;
							 */
							}
						}

						/*
						 * } catch (IOException e) {
						 * FileUtils.broadcastMessage(ConnectionService.this,
						 * "Connection lost with " + device.getName());
						 * Log.e(TAG,"Connection lost with " +
						 * device.getName()); Log.e(TAG, "due to error:"+
						 * e.getMessage()); break; }
						 */

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