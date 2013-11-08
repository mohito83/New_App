/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android.util.Log;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * This class defines utility methods for socket programming. Rename the
 * function name if you don't like them
 * 
 * @author mohit aggarwl
 * 
 */
public class SocketUtils {

	private static final String TAG = "SocketUtils";
	private static byte buffer[] = new byte[8 * 1024];

	public static void writeToSocket(OutputStream os, byte[] buffer) {
		try {
			DataOutputStream dos = new DataOutputStream(os);
			dos.write(buffer);
		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}

	}

	public static void readFromSocket(File sdir, InputStream is) {
		try {
			// TODO need to handle multiple file transfers
			// TODO append .tmp to the file when the down load starts and rename
			// once file transfer is completed.
			DataInputStream dis = new DataInputStream(is);
			// reading number of file to be received
			int no_of_files = dis.readUnsignedShort();
			for (int i = 0; i < no_of_files; i++) {
				String fName = dis.readUTF();
				String tmpFName = fName + ".tmp";
				Log.i(TAG, "File name: " + fName);
				long fLen = dis.readLong();
				Log.i(TAG, "File size: " + fLen);
				// create a file
				Log.i(TAG, "Creating local file");
				// append .tmp to the file name before the file transfer starts
				File f = new File(sdir, tmpFName);
				f.createNewFile();
				FileOutputStream fos = new FileOutputStream(f);
				// byte buffer[] = new byte[(int) fLen];
				int bytesRead = 0;

				while (bytesRead < fLen) { // read exactly fLen
					/*
					 * int read = dis .read(buffer, 0, ((int) fLen - bytesRead)
					 * > buffer.length ? buffer.length : ((int) fLen -
					 * bytesRead)); fos.write(buffer,0,((int) fLen - bytesRead)
					 * > buffer.length ? buffer.length : ((int) fLen -
					 * bytesRead));
					 */
					int read = dis.read(buffer, 0,
							Math.min((int) fLen - bytesRead, buffer.length));
					fos.write(buffer, 0, read);
					bytesRead += read;
				}

				// fos.write(buffer);
				fos.close();

				// TODO Before renaming the file check the sanity of the
				// transferred file using some checksum
				
				// after file transfer is complete then rename the file to drop
				// .tmp
				File finalFile = new File(sdir, fName);
				// f.createNewFile();
				boolean isSuccess = f.renameTo(finalFile);
				Log.i(TAG, "File tranfer:" + isSuccess);
			}

		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}
	}

	public static void sendVideoPackage(File root, OutputStream os,
			List<Video> sendTo) {
		DataOutputStream dos = new DataOutputStream(os);

		try {
			// setting number of files to be sent
			dos.writeShort(sendTo.size());
			for (Video v : sendTo) {
				File f = new File(root, v.getFilepath());

				// TODO Please check this code thoroughly
				int len = 0;
				FileInputStream fin = new FileInputStream(f);
				dos.writeUTF(f.getName());
				dos.writeLong(f.length());
				// byte buffer[] = new byte[(int) f.length()];
				int totalBytesSent = 0;
				while ((len = fin.read(buffer)) != -1) {
					/* writeToSocket(os, buffer,, len); */
					dos.write(buffer, 0, len);
					totalBytesSent += len;
				}
				// dos.write(buffer);
				fin.close();
				Log.i(TAG, "Sent video " + totalBytesSent + " bytes");
			}
		} catch (IOException e) {
			Log.e(TAG, "Exception while sending videos package", e);
		}
	}

	/**
	 * This method will send the web pages content across the socket
	 * 
	 * @param os
	 * @param sendTo
	 */
	public static void sendWebPackage(OutputStream os, List<Articles> sendTo) {
		// TODO implement this function, once the the video transfer is done.
		// Should be similar to that
	}

}
