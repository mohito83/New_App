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
	private static final String FTP_SERVICE = "CustomFTPService";
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

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
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
		}
		// TODO code for web content
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		BluetoothDevice device = intent.getExtras().getParcelable("Device");
		/*
		 * Toast.makeText(this, device.getLabel() + " " + device.getAddress(),
		 * Toast.LENGTH_LONG).show();
		 */

		try {
			// get the socket from the device
			mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			mmSocket.connect();
		} catch (IOException e) {
			try {
				mmSocket.close();
			} catch (IOException e2) {
				Log.e(TAG,
						"unable to close() socket during connection failure",
						e2);
			}
		}

		if (mmSocket != null) {
			// Start point for data synchronnization
			try {
				mmInStream = mmSocket.getInputStream();
				mmOutStream = mmSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "unable to get in/out put streams", e);
			}

			boolean terminate = false;
			while (!terminate) {

				try {
					DataInputStream din = new DataInputStream(mmInStream);
					if (isExtDrMounted) {
						switch (transcState) {
						case Constants.NO_DATA_META:
							// read meta data and send video package
							// TODO along with each file send its metadata
							// information
							// Read from the InputStream
							// get delta from reading meta data file from the
							// slave
							FileUtils.getDelta(din, metaFile, sendTo, recvFrom);
							SocketUtils.sendVideoPackage(mmOutStream, sendTo);

							transcState = Constants.META_DATA_RECEIVED;
							break;

						case Constants.META_DATA_RECEIVED:
							// send list of files required by the master to
							// slave
							// TODO check this design: Whether we need to send
							// the file requests in one go or 1 by 1. I didn't
							// have the actual meta data and content set up when
							// I was doing the POC, and my dummy data 
							// didn't mimmick the actual data closely.
							// If we decide to go for 1 by 1 file request and
							// receive approach then we need to modify this
							// case as well as the one following it.
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							ObjectOutput out = null;
							try {
								out = new ObjectOutputStream(bos);
								out.writeObject(recvFrom);
								byte[] yourBytes = bos.toByteArray();
								SocketUtils.writeToSocket(mmOutStream,
										yourBytes);
							} finally {
								out.close();
								bos.close();
							}
							transcState = Constants.META_DATA_TO_SLAVE;
							break;

						case Constants.META_DATA_TO_SLAVE:
							// receive data from the slave and write it to
							// the file system
							File xferDir = new File(path, Constants.xferDirName
									+ "/" + device.getName());
							SocketUtils.readFromSocket(xferDir, din);
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
		/*
		 * START_STICKY runs the service till we explicitly stop the service
		 */
		return START_NOT_STICKY;
	}

}