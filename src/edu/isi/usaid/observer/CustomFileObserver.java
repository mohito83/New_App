package edu.isi.usaid.observer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos.Builder;

import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CustomFileObserver extends FileObserver
{
	private static final String TRANSFER_DIRECTORY_NAME = "xfer";

	public static final String ARTICLE_META_FILE_LOCATION = Constants.webMetaFileName;

	public static final String VIDEO_META_FILE_LOCATION = Constants.metaFileName;

	private static String tagName = "CustomFileObserver";
	
	private final String baseDirPath = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;

	private FileMonitorTask fileMonitorTask;
	
	private Map<String, FileObserver> fileObserverMap = new HashMap<String, FileObserver>() ; 
	
	public CustomFileObserver(String path, FileMonitorTask fileMonitorTask) 
	{
		super(path);
		this.fileMonitorTask = fileMonitorTask;
	}
	
	public boolean isTransferDirectoryContent(String path)
	{
		return path != null && path.contains(TRANSFER_DIRECTORY_NAME);
	}


  /*
   * Listening to move_self event rather than create event, since it is not 
   * possible in create to know if the file transfer has already been done
   */  
	@Override
	public void onEvent(int event, String path) 
	{
		Log.d(tagName, "Got event for file with path: " + path);
		if(path== null || path.equals("null")) return; 
		String fullPath = baseDirPath + "/" + path; 
		if(isDirectory(fullPath) && !fileObserverMap.containsKey(fullPath))
		{
			fileObserverMap.put(fullPath, new CustomFileObserver(fullPath, fileMonitorTask));
			return;
		}
		Log.d(tagName, "Event Id: " + event);
		if(event == FileObserver.MOVE_SELF && isTransferDirectoryContent(path)) 
		{
			if(isMetaDataFile(path))
			{
				appendContentToMainMetaFile(path);
				propogateUpdatedMessage(path);
			}
			else
			{
				copyFileToBaseDirectory(path);								
			}
			cleanupFile(path);
		}			
	}

	private boolean isDirectory(String path) 
	{
		File file = new File(path); 
		return file.isDirectory();
	}

	private void propogateUpdatedMessage(String path) 
	{
		Intent messageIntent = new Intent();
		messageIntent.setAction(Constants.META_UPDATED_ACTION);
		LocalBroadcastManager.getInstance(fileMonitorTask).sendBroadcast(messageIntent);
	}

	private void cleanupFile(String path) 
	{
		File file = new File(path);
		if(file.isFile())
		{
			file.delete();
		}
	}

	private void appendContentToMainMetaFile(String sourceMetaFilePath) 
	{
		if(sourceMetaFilePath.contains("news"))
		{
			appendToNewsMetaFile(sourceMetaFilePath);
		}
		else if(sourceMetaFilePath.contains("video"))
		{			
			appendToVideosMetaFile(sourceMetaFilePath);
		}
	}

	private List<Video> getVideosFromFile(String fileName)
	{
		try 
		{
			Videos videos = Videos.parseFrom(new FileInputStream(fileName));
			return videos.getVideoList();
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(tagName, "No File exists at: " + fileName, e.getCause());
			return new ArrayList<Video>();
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "Exception in reading metadatafile. Exception while reading file at: "   
					  				   + fileName, e.getCause());
			return new ArrayList<Video>();
		}
	}
	
	private List<Article> getArticlesFromMetaFile(String fileName)
	{
		try 
		{
			Articles articles = Articles.parseFrom(new FileInputStream(fileName));
			return articles.getArticleList();
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(tagName, "No File exists at: " + fileName, e.getCause());
			return new ArrayList<Article>();
		} 
		catch (IOException e) {
			Log.e(tagName, "Exception in reading metadatafile. Exception while reading file at: "   
	  				   + fileName, e.getCause());
			return new ArrayList<Article>();
		}
	}
	
	private void appendToVideosMetaFile(String sourceMetaFilePath) 
	{
		List<Video> newMetaFileVideos = getVideosFromFile(sourceMetaFilePath);
		List<Video> existingMetaFileVideos = getVideosFromFile(VIDEO_META_FILE_LOCATION);
		if(newMetaFileVideos.isEmpty()) 
			return;
		existingMetaFileVideos.addAll(newMetaFileVideos);
		writeUpdatedVideos(existingMetaFileVideos);
	}

	private void writeUpdatedVideos(List<Video> videoList) 
	{
		Builder videoBuilder = Videos.newBuilder();
		videoBuilder.addAllVideo(videoList);
		Videos appendedVideos = videoBuilder.build();
		try 
		{
			appendedVideos.writeTo(new FileOutputStream(VIDEO_META_FILE_LOCATION));
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(tagName, "FileNotFoundException occured for the video meta data file "
						   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "IOException occured when writing video meta data file "
					   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		}
		
	}

	private void appendToNewsMetaFile(String sourceMetaFilePath) 
	{
		List<Article> newArticles = getArticlesFromMetaFile(sourceMetaFilePath);
		List<Article> existingArticles = getArticlesFromMetaFile(ARTICLE_META_FILE_LOCATION);
		if(newArticles.isEmpty())
			return;
		existingArticles.addAll(newArticles);
		writeUpdatedArticles(existingArticles);
	}

	private void writeUpdatedArticles(List<Article> existingArticles) 
	{
		edu.isi.usaid.pifi.metadata.ArticleProtos.Articles.Builder articleBuilder = Articles.newBuilder();
		articleBuilder.addAllArticle(existingArticles);
		Articles appendedArticles = articleBuilder.build();
		try 
		{
			appendedArticles.writeTo(new FileOutputStream(ARTICLE_META_FILE_LOCATION));
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(tagName, "FileNotFoundException occured for the article meta data file "
						   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "IOException occured when writing article meta data file "
					   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		}
	}

	private boolean isMetaDataFile(String path) 
	{
		return path.endsWith(".meta") || path.endsWith(".dat");
	}

	private void copyFileToBaseDirectory(String sourceFilePath) 
	{
		File srcFile = new File(sourceFilePath);
		File destDir = new File(baseDirPath);
		try 
		{
			FileUtils.copyFileToDirectory(srcFile, destDir);
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "Error occured when trying to copy a file from " + 
							sourceFilePath + " to " + baseDirPath, 
							e.getCause());
		}
	}
}

