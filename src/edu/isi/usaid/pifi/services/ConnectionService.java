/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 * 
 * @author mohit aggarwal
 * 
 */
public class ConnectionService extends Service {

	private static final String TAG = "ConnectionService";

	// Name for the SDP record when creating server socket
	private static final String FTP_SERVICE = "CustomFTPService";
	private static final UUID MY_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// private Context context;
	private BluetoothAdapter mAdapter;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	private boolean isExtDrMounted;

	private byte buffer[] = new byte[Short.MAX_VALUE];

	private File path, metaFile;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	/**
	 * For control messages during data communication over sockets
	 */
	private int dataState = Constants.NO_DATA_META;

	/**
	 * Consturctor for the class.
	 * 
	 * @param bluetoothFileTransferActivity
	 * 
	 * @param adapter
	 */
	public void onCreate() {
		// context = bluetoothFileTransferActivity;
		mAdapter = BluetoothAdapter.getDefaultAdapter();

		// TODO make sure that bluetooth is turned on before coming to service
		// apis
		// TODO receive device to connect to from the main activity of the
		// PiFiMobile app.

		mState = STATE_NONE;
		isExtDrMounted = Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
		File sdr = Environment.getExternalStorageDirectory();
		path = new File(sdr, Constants.contentDirName);
		if (!path.exists()) {
			path.mkdir();
		}
		metaFile = new File(path, Constants.metaFileName);
		if (!metaFile.exists()) {
			PrintWriter writer;
			try {
				writer = new PrintWriter("the-file-name.txt", "UTF-8");
				writer.println("Hello World!!");
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		// start the server socket for listening the incoming connections
		if (mAdapter != null && mAdapter.isEnabled()) {
			start();
			// if there is device to be connected.. then connect to it.
			// TODO get this information from the intent passed to the service
			// by main activity
			Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
			if (pairedDevices != null && !pairedDevices.isEmpty()) {
				BluetoothDevice device = pairedDevices.iterator().next();

				if (device != null) {
					connect(device);
				}
			}
			// connect(null);
		}
		return START_STICKY;
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(FTP_SERVICE,
						MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "AcceptThread: Socket listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			Log.d(TAG, "BEGIN mAcceptThread" + this);

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (true/* mState != STATE_CONNECTED */) {
				if (mmServerSocket != null) {
					try {
						// This is a blocking call and will only return on a
						// successful connection or an exception
						socket = mmServerSocket.accept();
					} catch (IOException e) {
						Log.e(TAG, "AcceptThread: accept() failed", e);
						break;
					}
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (ConnectionService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice(), true);
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG,
										"AcceptThread: Could not close unwanted socket",
										e);
							}
							break;
						}
					}
				}
			}

			Log.i(TAG, "END mAcceptThread");

		}

		public void cancel() {
			Log.d(TAG, "AcceptThread: socket cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "AcceptThread: close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "ConnectThread: create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "ConnectThread: BEGIN mConnectThread ");

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (ConnectionService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, false);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "ConnectThread: close() socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private final boolean isServer;
		private final String devName;
		private List<Video> sendTo = new ArrayList<Video>();
		private List<Video> recvFrom = new ArrayList<Video>();

		public ConnectedThread(BluetoothSocket socket, BluetoothDevice device,
				boolean isServer) {
			Log.d(TAG, "create ConnectedThread: ");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			this.isServer = isServer;
			devName = (device.getName() != null && device.getName().trim()
					.length() > 0) ? device.getName() : device.getAddress();

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			File xferFolder = new File(path,Constants.xferDirName+devName);
			xferFolder.mkdir();

			// Keep listening to the InputStream while connected
			while (true) {
				try {

					if (isExtDrMounted) {
						// Read from the InputStream
						DataInputStream din = new DataInputStream(mmInStream);
						String fileName = din.readUTF();
						long fileLen = din.readLong();
						short state = din.readShort();
						
						switch(state){
						case Constants.META_DATA_SENT:
							getDelta(din,sendTo,recvFrom);
							sendPackage(sendTo,Constants.DATA_FROM_MASTER);
							break;
							
						case Constants.DATA_FROM_MASTER:
							saveToDevice(din,fileName,fileLen);
						}

					}

				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}

		/**
		 * This method saves the content on the file
		 * @param din 
		 * @param fileName
		 * @param fileLen
		 */
		private void saveToDevice(DataInputStream din, String fileName, long fileLen){
			try {
				
				OutputStream os = new FileOutputStream(metaFile);
				while (fileLen > 0) {
					fileLen -= din.read(buffer);
					os.write(buffer);
				}
				os.close();
				} catch (IOException e) {
				// Unable to create file, likely because external
				// storage is
				// not currently mounted.
				Log.e("ExternalStorage", "Error writing "
						+ metaFile, e);
			}
		}
		/**
		 * This method sends the package to the receiver
		 * @param sendTo2
		 */
		private void sendPackage(List<Video> sendTo2, short state) {
			for(Video v : sendTo2){
				File f = new File(v.getFilepath());
				write(buffer, v.getFilename(), f.length(), state);
			}
		}

		/**
		 * This method parses the meta data file and populates the delta lists
		 * @param din data input stream to parse meta data into protobuf object
		 * @param sendTo arraylist for storing data to be sent to server
		 * @param recvFrom arraylist for list of files to be received form the slave
		 * @throws FileNotFoundException 
		 */
		private void getDelta(DataInputStream din, List<Video> sendTo,
				List<Video> recvFrom) throws FileNotFoundException,IOException {
			FileInputStream fin = new FileInputStream(metaFile);
			sendTo = Videos.parseFrom(fin).getVideoList();
			Iterator<Video> local = sendTo.iterator();
			fin.close();
			// TODO Auto-generated method stub
			recvFrom = Videos.parseFrom(din).getVideoList();
			
			while(local.hasNext()){
				Iterator<Video> remote = recvFrom.iterator();
				Video v = local.next();
				while(remote.hasNext()){
					Video rem = remote.next();
					if(v.getFilename().equals(rem.getFilename())){
						local.remove();
						remote.remove();
					}
				}
			}
		}
		
		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 * @param nfName
		 * @param len
		 * @param state
		 */
		public void write(byte[] buffer, String nfName, long len, int state) {
			try {
				DataOutputStream dos = new DataOutputStream(mmOutStream);
				dos.writeUTF(nfName); // for writing filename
				dos.writeLong(len); // for sending number of bytes
				dos.writeShort(state); // for sending data state to the receiver
										// so
										// that it can take appropriate actions
				dos.write(buffer); // for sending actual content

			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		Log.d(TAG, "start");

		setState(STATE_LISTEN);

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 * @param secure
	 *            Socket Security type - Secure (true) , Insecure (false)
	 */
	private void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);
		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 * @param isServer
	 *            TODO
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device, boolean isServer) {
		Log.d(TAG, "connected");

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket, device, isServer);
		mConnectedThread.start();

		setState(STATE_CONNECTED);
		if (isServer) {
			write();
		}
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public synchronized void write() {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		// check for external storage device
		if (isExtDrMounted) {
			// check the data state and based on that take actions
			switch (dataState) {
			case Constants.NO_DATA_META:
				// send meta data file to the requesting connection.
				sendDataOverSocket(r, Constants.metaFileName,
						Constants.NO_DATA_META);
				dataState = Constants.META_DATA_SENT;
				break;
			
			}

		}
		/* r.write(buffer, file.getName(), file.length()); */
	}

	private void sendDataOverSocket(ConnectedThread r, String fileName,
			short state) {
		// We can read and write the media
		File file = new File(path, fileName);
		long len = file.length();
		long conLen = -1;

		try {
			path.mkdirs();

			InputStream is = new FileInputStream(file);
			while (len > 0) {
				conLen = is.read(buffer);
				r.write(buffer, fileName, conLen, state);
				len -= conLen;
			}
			is.close();

		} catch (IOException e) {
			// Unable to create file, likely because external storage is
			// not currently mounted.
			Log.e("ExternalStorage", "Error writing " + file, e);
		}
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {

		// Start the service over to restart listening mode
		ConnectionService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {

		// Start the service over to restart listening mode
		ConnectionService.this.start();
	}

	/**
	 * @return the dataState
	 */
	public synchronized int getDataState() {
		return dataState;
	}

	/**
	 * @param dataState
	 *            the dataState to set
	 */
	public synchronized void setDataState(int dataState) {
		this.dataState = dataState;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onDestroy() {
		if (mAdapter != null) {
			mAdapter.cancelDiscovery();
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		super.onDestroy();
	}

}
