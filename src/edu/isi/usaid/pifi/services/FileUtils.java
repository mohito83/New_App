/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.ExtraConstants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;

/**
 * This class defines file utilities for reading data and saving data to file
 * 
 * @author mohit aggarwl
 * 
 */
public class FileUtils {

	private static final String TAG = "FileUtils";

	// private static byte buffer[] = new byte[8 * 1024];

	/**
	 * 
	 * @param buffer2
	 * @param f
	 * @param l
	 * @return
	 * @throws IOException
	 */
	public static long readDataFromFile(byte[] buffer2, File f, long l)
			throws IOException {
		FileInputStream fin = new FileInputStream(f);
		long buffSize = fin.read(buffer2);
		fin.close();
		return buffSize;
	}

	/**
	 * Find the delta after comparing local meta data file with remote. TODO
	 * this is only for videos only need to extend this functionality for web
	 * content also
	 * @param metaFile
	 * @param sendTo
	 * @param din
	 * 
	 * @throws IOException
	 */
	public static void getDeltaforVideos(byte[] bytes, File metaFile,
			List<Video> sendTo) throws IOException {
		// TODO This code runs a risk of running into exception if the meta data
		// file size is too big. Needs to fix this issue later on

		FileInputStream fin = new FileInputStream(metaFile);

		// create copies of actual list
		for (Video vid : Videos.parseFrom(fin).getVideoList()) {
			sendTo.add(vid);
		}
		fin.close();

		List<Video> recv = new ArrayList<Video>();
		for (Video vid : Videos.parseFrom(bytes).getVideoList()) {
			recv.add(vid);
		}

		fin = new FileInputStream(metaFile);
		Iterator<Video> local = Videos.parseFrom(fin).getVideoList().iterator();
		fin.close();

		// for each local entry
		while (local.hasNext()) {
			Iterator<Video> remote = Videos.parseFrom(bytes).getVideoList()
					.iterator();
			Video v = local.next();
			// for each remote entry
			while (remote.hasNext()) {
				Video rem = remote.next();
				// if local entry matches remote entry
				if (v.getFilename().equals(rem.getFilename())) {
					sendTo.remove(v);
					recv.remove(rem);
				}
			}
		}

		/*Iterator<Video> iter = recv.iterator();
		while (iter.hasNext()) {
			recvFrom.add(iter.next().getFilepath());
		}*/

	}

	/**
	 * This method will read the file request from the master and prepare a list
	 * of paths local to the device to help it send requested data back to the
	 * master
	 * 
	 * @param din
	 * @param paths
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void receiveMasterVideoList(DataInputStream din,
			File metafile, List<Video> vid) {
		byte[] buff = new byte[Short.MAX_VALUE];
		List<String> paths = new ArrayList<String>();
		// read data in to buffer. should be stored in a buffer of 32Kbytes
		try {
			din.read(buff);
			ByteArrayInputStream bin = new ByteArrayInputStream(buff);
			ObjectInputStream ois = new ObjectInputStream(bin);
			paths = (ArrayList<String>) ois.readObject();
			ois.close();
		} catch (StreamCorruptedException e) {
			Log.e(TAG, "object stream is corrupted", e);
		} catch (OptionalDataException e) {
			Log.e(TAG, "exception while serializing the object", e);
		} catch (IOException e) {
			Log.e(TAG, "Exception during read", e);
		} catch (ClassNotFoundException e) {
			Log.e(TAG,
					"Exception while typecasting object returned from readObject()",
					e);
		}

		if (paths != null) {
			try {
				FileInputStream fin = new FileInputStream(metafile);
				// get a list of local videos
				List<Video> vidTmp = Videos.parseFrom(fin).getVideoList();
				Iterator<Video> local = vidTmp.iterator();
				fin.close();
				while (local.hasNext()) {
					Video v = local.next();
					String filePath = v.getFilepath();
					// add if this file is being requested
					if (paths.contains(filePath)) {
						vid.add(v);
					}
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Ecxception while creating opening the file", e);
			} catch (IOException e) {
				Log.e(TAG, "Exception while parsing the meta data file", e);
			}

		}

	}

	public static void getDeltaforWeb(byte[] bytes, File metaFile,
			List<Article> sendTo) throws IOException {

		// TODO This code runs a risk of running into exception if the meta data
		// file size is too big. Needs to fix this issue later on

		FileInputStream fin = new FileInputStream(metaFile);

		// create copies of actual list
		for (Article vid : Articles.parseFrom(fin).getArticleList()) {
			sendTo.add(vid);
		}
		fin.close();

		List<Article> recv = new ArrayList<Article>();
		for (Article vid : Articles.parseFrom(bytes).getArticleList()) {
			recv.add(vid);
		}

		fin = new FileInputStream(metaFile);
		Iterator<Article> local = Articles.parseFrom(fin).getArticleList()
				.iterator();
		fin.close();

		// for each local entry
		while (local.hasNext()) {
			Iterator<Article> remote = Articles.parseFrom(bytes)
					.getArticleList().iterator();
			Article v = local.next();
			// for each remote entry
			while (remote.hasNext()) {
				Article rem = remote.next();
				// if local entry matches remote entry
				if (v.getFilename().equals(rem.getFilename())) {
					sendTo.remove(v);
					recv.remove(rem);
				}
			}
		}

/*		Iterator<Article> iter = recv.iterator();
		while (iter.hasNext()) {
			recvFrom.add(iter.next().getFilename());
		}
*/
	}

	/**
	 * This method will receive and parses the master web list
	 * 
	 * @param din
	 * @param metaFile
	 * @param webPaths
	 */
	public static void receiveMasterWebList(DataInputStream din, File metaFile,
			List<Article> webPaths) {
		byte[] buff = new byte[Short.MAX_VALUE];
		List<String> paths = new ArrayList<String>();
		// read data in to buffer. should be stored in a buffer of 32Kbytes
		try {
			din.read(buff);
			ByteArrayInputStream bin = new ByteArrayInputStream(buff);
			ObjectInputStream ois = new ObjectInputStream(bin);
			paths = (ArrayList<String>) ois.readObject();
			ois.close();
		} catch (StreamCorruptedException e) {
			Log.e(TAG, "object stream is corrupted", e);
		} catch (OptionalDataException e) {
			Log.e(TAG, "exception while serializing the object", e);
		} catch (IOException e) {
			Log.e(TAG, "Exception during read", e);
		} catch (ClassNotFoundException e) {
			Log.e(TAG,
					"Exception while typecasting object returned from readObject()",
					e);
		}

		if (paths != null) {
			try {
				FileInputStream fin = new FileInputStream(metaFile);
				// get a list of local videos
				List<Article> vidTmp = Articles.parseFrom(fin).getArticleList();
				Iterator<Article> local = vidTmp.iterator();
				fin.close();
				while (local.hasNext()) {
					Article v = local.next();
					String filePath = v.getFilename();
					// add if this file is being requested
					if (paths.contains(filePath)) {
						webPaths.add(v);
					}
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Ecxception while creating opening the file", e);
			} catch (IOException e) {
				Log.e(TAG, "Exception while parsing the meta data file", e);
			}

		}

	}

	/**
	 * This method broadcast the messages to the
	 * 
	 * @param message
	 */
	public static void broadcastMessage(ContextWrapper wrapper, String message) {
		Intent i = new Intent();
		i.setAction(Constants.BT_STATUS_ACTION);
		i.putExtra(ExtraConstants.STATUS, message);
		wrapper.sendBroadcast(i);
	}
}
