/**
 * 
 */
package edu.isi.backpack.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContextWrapper;
import android.content.Intent;
import edu.isi.backpack.bluetooth.AckPayload;
import edu.isi.backpack.bluetooth.InfoMessage;
import edu.isi.backpack.bluetooth.InfoPayload;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.ArticleProtos.Articles;
import edu.isi.backpack.metadata.VideoProtos.Video;
import edu.isi.backpack.metadata.VideoProtos.Videos;

/**
 * This class defines the utility methods
 * 
 * @author mohit aggarwl
 * 
 */
public class BackpackUtils {

	/**
	 * This method calculates the delta for the video meta data file
	 * 
	 * @param videoMetaFile
	 * @param newMetaFile
	 * @return
	 * @throws IOException
	 */
	public static List<Video> getVideoDeltaList(File videoMetaFile,
			File newMetaFile) throws IOException {
		List<Video> videoList = new ArrayList<Video>();
		FileInputStream finorg = new FileInputStream(videoMetaFile);
		FileInputStream finnew = new FileInputStream(newMetaFile);

		Map<String, Video> newMap = new HashMap<String, Video>();

		if (videoMetaFile.length() > 0 && newMetaFile.length() > 0) {
			for (Video v : Videos.parseFrom(finnew).getVideoList()) {
				newMap.put(v.getFilename(), v);
			}

			for (Video nVid : Videos.parseFrom(finorg).getVideoList()) {
				if (!newMap.containsKey(nVid.getFilename())) {
					videoList.add(nVid);
				}
			}
		} else {
			if (videoMetaFile.length() > 0 && newMetaFile.length() == 0) {
				videoList.addAll(Videos.parseFrom(finorg).getVideoList());
			}
		}

		finorg.close();
		finnew.close();

		return videoList;
	}

	/**
	 * This method calculates the delta for web meta data file
	 * 
	 * @param webMetaFile
	 * @param newMetaFile
	 * @return
	 * @throws IOException
	 */
	public static List<Article> getWebDeltaList(File webMetaFile,
			File newMetaFile) throws IOException {
		List<Article> webList = new ArrayList<Article>();
		FileInputStream finorg = new FileInputStream(webMetaFile);
		FileInputStream finnew = new FileInputStream(newMetaFile);

		Map<String, Article> newMap = new HashMap<String, Article>();

		if (webMetaFile.length() > 0 && newMetaFile.length() > 0) {
			for (Article v : Articles.parseFrom(finnew).getArticleList()) {
				newMap.put(v.getFilename(), v);
			}

			for (Article nVid : Articles.parseFrom(finorg).getArticleList()) {
				if (!newMap.containsKey(nVid.getFilename())) {
					webList.add(nVid);
				}
			}
		} else {
			if (webMetaFile.length() > 0 && newMetaFile.length() == 0) {
				webList.addAll(Articles.parseFrom(finorg).getArticleList());
			}
		}

		finorg.close();
		finnew.close();

		return webList;
	}

	/**
	 * 
	 * @param path
	 * @param artilce
	 * @return
	 */
	public static List<File> getWebArticleImages(File path, Article artilce) {
		List<File> webImagesPath = new ArrayList<File>();
		String filenName = artilce.getFilename();
		String webImageFolderName = filenName.substring(0,
				filenName.indexOf(".html"));
		File imgFolder = new File(path, webImageFolderName);
		if (imgFolder.isDirectory()) {
			webImagesPath = Arrays.asList(imgFolder.listFiles());
		}

		return webImagesPath;
	}

	/**
	 * This method creates the info message
	 * 
	 * @param type
	 * @param f
	 * @return
	 */
	public static InfoMessage createInfoMessage(short type, File f) {
		InfoMessage infomsg = new InfoMessage();
		infomsg.setType(type);
		InfoPayload payload = new InfoPayload();
		if (f != null) {
			payload.setFileName(f.getName());
			payload.setLength(f.length());
		}
		infomsg.setPayload(payload);
		return infomsg;
	}

	/**
	 * This method creates the info message with ack payload
	 * 
	 * @param type
	 * @param orgType
	 * @param ackVal
	 * @return
	 */
	public static InfoMessage createInfoMessage(short type, short orgType,
			short ackVal) {
		InfoMessage infomsg = new InfoMessage();
		infomsg.setType(type);
		AckPayload ackPayload = new AckPayload();
		ackPayload.setAck(ackVal);
		ackPayload.setOrgMsgType(orgType);
		infomsg.setPayload(ackPayload);
		return infomsg;
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
