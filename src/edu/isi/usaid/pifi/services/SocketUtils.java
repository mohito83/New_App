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

	public static void writeToSocket(OutputStream os, byte[] buffer2) {
		try {
			DataOutputStream dos = new DataOutputStream(os);
			dos.write(buffer2);
		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}

	}

	public static void readFromSocket(File sdir, InputStream is) {
		try {
			// TODO append .tmp to the file when the down load starts and rename
			// once file transfer is completed.
			DataInputStream dis = new DataInputStream(is);
			String fName = dis.readUTF();
			long fLen = dis.readLong();
			// create a file
			File f = new File(sdir, fName);
			FileOutputStream fos = new FileOutputStream(f);
			while (dis.read(buffer) != -1) {
				fos.write(buffer);
			}
			fos.close();
		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}
	}

	public static void sendVideoPackage(OutputStream os, List<Video> sendTo) {
		for (Video v : sendTo) {
			File f = new File(v.getFilepath());
			// TODO Please check this code thoroughly
			long len = 0L;
			try {
				FileInputStream fin = new FileInputStream(f);
				DataOutputStream dos = new DataOutputStream(os);
				dos.writeUTF(f.getName());
				dos.writeLong(f.length());
				while ((len = fin.read(buffer)) != -1) {
					/* writeToSocket(os, buffer,, len); */
					dos.write(buffer);
				}
				fin.close();
			} catch (IOException e) {
				Log.e(TAG, "Exception while sending videos package", e);
			}
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
