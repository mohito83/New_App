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

import android.util.Log;
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
	private static byte buffer[] = new byte[8 * 1024];


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
		return buffSize;
	}

	/**
	 * Find the delta after comparing local meta data file with remote. TODO
	 * this is only for videos only need to extend this functionality for web
	 * content also
	 * 
	 * @param din
	 * @param metaFile
	 * @param sendTo
	 * @param recvFrom
	 * @throws IOException
	 */
	public static void getDelta(byte[] bytes, File metaFile,
			List<Video> sendTo, List<String> recvFrom) throws IOException {
		// TODO This code runs a risk of running into exception if the meta data
		// file size is too big. Needs to fix this issue later on

		FileInputStream fin = new FileInputStream(metaFile);
		
		//create copies of actual list
		for(Video vid : Videos.parseFrom(fin).getVideoList()){
			sendTo.add(vid);
		}
		fin.close();
		
		List<Video> recv = new ArrayList<Video>();
		for(Video vid : Videos.parseFrom(bytes).getVideoList()){
			recv.add(vid);
		}
		
		fin = new FileInputStream(metaFile);
		Iterator<Video> local = Videos.parseFrom(fin).getVideoList().iterator();
		fin.close();

		// for each local entry
		while (local.hasNext()) {
			Iterator<Video> remote = Videos.parseFrom(bytes).getVideoList().iterator();
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

		Iterator<Video> iter = recv.iterator();
		while (iter.hasNext()) {
			recvFrom.add(iter.next().getFilepath());
		}

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
	public static void getMasterRequestList(DataInputStream din, File metafile,
			List<Video> vid) {
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
				while(local.hasNext()){
					Video v = local.next();
					String filePath = v.getFilepath();
					// add if this file is being requested
					if(paths.contains(filePath)){
						vid.add(v);
					}
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Ecxception while creating opening the file", e);
			} catch (IOException e) {
				Log.e(TAG, "Exception while parsing the meta data file", e);
			}
		}
		
		//TODO implement similarly for web contents
	}
}
