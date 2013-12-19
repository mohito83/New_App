/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContextWrapper;
import android.content.Intent;
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
	 * Find the delta after comparing local meta data file with remote.
	 * 
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

	}

	public static void getDeltaforWeb(byte[] bytes, File metaFile,
			List<Article> sendTo) throws IOException {

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