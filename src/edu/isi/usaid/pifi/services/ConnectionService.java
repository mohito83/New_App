/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.ExtraConstants;
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
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	private BluetoothAdapter mAdapter;
	private BluetoothSocket mmSocket;

	private InputStream mmInStream;
	private OutputStream mmOutStream;

	private boolean isExtDrMounted;

	private File path;

	private File metaFile;
	private List<Video> sendTo = new ArrayList<Video>();
	private List<String> recvFrom = new ArrayList<String>();

	private int transcState = Constants.NO_DATA_META;
	BluetoothDevice item;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		//Debug.waitForDebugger();
		// context = bluetoothFileTransferActivity;
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		isExtDrMounted = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
		File sdr = Environment.getExternalStorageDirectory();
		path = new File(sdr, Constants.contentDirName);
		if (!path.exists()) {
			path.mkdir();
		}
		metaFile = new File(path, Constants.metaFileName);
		if (!metaFile.exists()) {
			// TODO some dummy data to handle this condition
			Toast.makeText(getApplicationContext(), "No meta data found!",
					Toast.LENGTH_SHORT).show();
			return;
		}
		// TODO code for web content
	}

	public int onStartCommand(Intent intent, int flags, int startId) {

//        Debug.waitForDebugger();
		final BluetoothDevice item = intent.getExtras().getParcelable("Device");

		// use a seaparate thread for connection and data transfer
		Thread t = new Thread(new Runnable() {

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
					// Method m =
					// device.getClass().getMethod("createRfcommSocket", new
					// Class[] {int.class});
					// mmSocket = (BluetoothSocket) m.invoke(device, 1);
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
						Intent i = new Intent();
						i.setAction(Constants.BT_STATUS_ACTION);
						i.putExtra(ExtraConstants.STATUS, "Connection Failed");
						sendBroadcast(i);

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
				Intent i = new Intent();
				i.setAction(Constants.BT_STATUS_ACTION);
				i.putExtra(ExtraConstants.STATUS, "Connected");
				sendBroadcast(i);

				DataInputStream dis;
				if (mmSocket != null) {
					// Start point for data synchronnization
					try {
						mmInStream = mmSocket.getInputStream();
						mmOutStream = mmSocket.getOutputStream();
						dis = new DataInputStream(mmInStream);
					} catch (IOException e) {
						Log.e(TAG, "unable to get in/out put streams", e);
						i = new Intent();
						i.setAction(Constants.BT_STATUS_ACTION);
						i.putExtra(ExtraConstants.STATUS,
								"Unable to get in/out streams");
						sendBroadcast(i);

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
									// TODO along with each file send its
									// metadata
									// information
									// Read from the InputStream
									// get delta from reading meta data file
									// from the
									// slave

									// wait until data arrives
									/*while (dis.available() == 0) {
									}
									;*/

									// get metadata file size
									long byteCount = dis.readLong();
									Log.i(TAG, "expecting " + byteCount
											+ " bytes");

									// read metadata
									byte[] buf = new byte[(int) byteCount];
									// while(dis.available() < byteCount){} //
									// wait until full file arrive
									int bytesRead = 0;
									while (bytesRead < byteCount) {
										bytesRead += mmInStream.read(buf,
												bytesRead, buf.length - bytesRead);
									}

									i = new Intent();
									i.setAction(Constants.BT_STATUS_ACTION);
									i.putExtra(ExtraConstants.STATUS,
											"metadata: " + bytesRead
													+ " bytes received");
									sendBroadcast(i);

									// get delta entries to send/request
									FileUtils.getDelta(buf, metaFile, sendTo,
											recvFrom);

									i = new Intent();
									i.setAction(Constants.BT_STATUS_ACTION);
									i.putExtra(
											ExtraConstants.STATUS,
											sendTo.size()
													+ " entries to send, request "
													+ recvFrom + " entries");
									sendBroadcast(i);

									Log.i(TAG, "Sending videos");
									SocketUtils.sendVideoPackage(path,
											mmOutStream, sendTo);

									Log.i(TAG, "Videos Sent");
									
									// wait until listener responds
									// (listener has finished receiving)
									byte[] buff = new byte[20];
									mmInStream.read(buff);
									Log.i(TAG, "Listener responded: " + new String(buff));
									
									transcState = Constants.META_DATA_RECEIVED;
									break;

								case Constants.META_DATA_RECEIVED:
									// send list of files required by the master
									// to
									// slave
									// TODO check this design: Whether we need
									// to send
									// the file requests in one go or 1 by 1. I
									// didn't
									// have the actual meta data and content set
									// up when
									// I was doing the POC, and my dummy data
									// didn't mimmick the actual data closely.
									// If we decide to go for 1 by 1 file
									// request and
									// receive approach then we need to modify
									// this
									// case as well as the one following it.
									
									Log.i(TAG, "Sending file requests");
									
									 ByteArrayOutputStream bos = new
									 ByteArrayOutputStream();
									 ObjectOutput out = null;
									 try {
										 out = new ObjectOutputStream(bos);
										 out.writeObject(recvFrom);
										 byte[] yourBytes = bos.toByteArray();
										 SocketUtils.writeToSocket(mmOutStream,
										 yourBytes);
									 } finally {
//										 out.close();
//										 bos.close();
									 }
									 
									Log.i(TAG, "Requests sent");
									transcState = Constants.META_DATA_TO_SLAVE;
									break;

								case Constants.META_DATA_TO_SLAVE:
									// receive data from the slave and write it
									// to
									// the file system
									
									Log.i(TAG, "Receiving requested files");
									File xferDir = new File(path,
											Constants.xferDirName + "/"
													+ device.getName());
									xferDir.mkdirs();
									SocketUtils.readFromSocket(xferDir, dis);
									Log.i(TAG, "Finished receiving requested files");
									
									// tell listener finished
									String str = "Success!!";
									try {
										mmOutStream.write(str.getBytes());
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
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
							Log.e(TAG, "disconnected", e);
							i = new Intent();
							i.setAction(Constants.BT_STATUS_ACTION);
							i.putExtra(ExtraConstants.STATUS, "connected lost");
							sendBroadcast(i);
							break;
						}

					}

					if (terminate) {
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