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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos.Builder;

public class CustomFileObserver extends FileObserver
{
	public static final String ARTICLE_META_FILE_LOCATION = Constants.webMetaFileName;

	public static final String VIDEO_META_FILE_LOCATION = Constants.metaFileName;

	private static String tagName = "CustomFileObserver";
	
	private final String baseDirPath = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;
	
	private String toAppendPath = null;

	private FileMonitorTask fileMonitorTask;
	
	private Map<String, FileObserver> fileObserverMap = new HashMap<String, FileObserver>() ; 
	
	private Map<String, String> transferredVideoFiles = new ConcurrentHashMap<String, String>();
	
	public CustomFileObserver(String path, FileMonitorTask fileMonitorTask, int[] eventTypes) 
	{		
		super(path, FileObserver.CREATE | FileObserver.MOVED_TO);
		toAppendPath = path;
		this.fileMonitorTask = fileMonitorTask;
		if(toAppendPath.equals(baseDirPath))
		{
			startWatchingForExistingDirectories();
		}
	}
	
	private void startWatchingForExistingDirectories() 
	{
		File baseDir = new File(baseDirPath);
		File[] listFiles = baseDir.listFiles();
		if(containsTransferFolder(listFiles))
		{
			String transferDirectoryPath = baseDirPath + "/xfer" ; 
			addWatcherForFile(new File(transferDirectoryPath));
			for(File file : new File(baseDirPath).listFiles())
			{
				addWatcherForFile(file);
			}
			
		}
	}

	private boolean containsTransferFolder(File[] listFiles) 
	{
		for(File eachFile : listFiles)
		{
			try {
				if(eachFile.getCanonicalPath().contains("xfer"))
					return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private void addWatcherForFile(File file) 
	{
		Log.d(tagName, "In addWatcherForFile: Adding observer for existing path: " +file.getAbsolutePath());
		FileObserver transferDirObserver = new CustomFileObserver(file.getAbsolutePath(), fileMonitorTask, new int[]{FileObserver.MOVED_FROM});
		fileObserverMap.put(file.getAbsolutePath(), transferDirObserver);
		transferDirObserver.startWatching();

	}

	public boolean isTransferDirectoryContent(String path)
	{
		return path != null ;
	}


  /*
   * Listening to move_self event rather than create event, since it is not 
   * possible in create to know if the file transWhat followed next was pretty much in line with the craziness of the entire episode. Within five minutes, the three musketeers could not bear the pain of being seated in the second row in a 7:30 am lecture. They simply got up and sprinted to their (I am guessing) usual seats in the last bench. fer has already been done
   */  
	@Override
	public void onEvent(int event, String path) 
	{
		if(path.endsWith(".tmp"))
			path = path.replaceAll(".tmp", "");
		Log.d(tagName, "Got event for file with path: " + path);
		Log.d(tagName, Environment.getRootDirectory().getAbsolutePath());
		Log.d(tagName, Environment.getExternalStorageDirectory().getAbsolutePath());
		String fullPath = toAppendPath + "/" + path;; 
		Log.d(tagName, "checking for directory " + " at path :" + fullPath + " with result : "    + isDirectory(fullPath)); 
		if(path== null || path.equals("null")) return; 
		if(toAppendPath.equals(baseDirPath) && !isDirectory(fullPath))
			return;
		if(isDirectory(fullPath) && !fileObserverMap.containsKey(fullPath))
		{
			FileObserver transferDirObserver = new CustomFileObserver(fullPath, fileMonitorTask, new int[]{FileObserver.MOVED_FROM});
			Log.d(tagName, "Creating a new observer for the new transfer folder path: " + fullPath);			
			fileObserverMap.put(fullPath, transferDirObserver);
			transferDirObserver.startWatching();
			return;
		}
		Log.d(tagName, "Event Id: " + event);
		Log.d(tagName, "Move self event: " + FileObserver.MOVE_SELF);
		Log.d(tagName, "Create event: " + FileObserver.CREATE);

		fullPath = toAppendPath + "/" + path;
		
		Log.d(tagName, "Looking for a file at path " + fullPath);

		if(isTransferDirectoryContent(fullPath) && !isDirectory(fullPath)) 
		{
			Log.d(tagName, "Checking for isMetaDataFile for the path : " + fullPath + " with result : " + isMetaDataFile(fullPath));
			if(isMetaDataFile(fullPath) )
			{
				appendContentToMainMetaFile(fullPath);
				propogateUpdatedMessage(fullPath);				
			}
			else
			{
				copyFileToBaseDirectory(fullPath);								
			}
			cleanupFile(fullPath);
		}			
	}

	private boolean isDirectory(String path) 
	{
		File file = new File(path); 
		try {
			return file.getCanonicalFile().isDirectory();
		} catch (IOException e) {
			Log.e(tagName, e.getMessage());
		}
		return false;
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
		if(sourceMetaFilePath.contains("html"))
		{
			Log.d(tagName, "Trying to append file with path: " + sourceMetaFilePath + " to the actual news meta file"); 
			appendToNewsMetaFile(sourceMetaFilePath);
		}
		else 
		{			
			Log.d(tagName, "Trying to append file with path: " + sourceMetaFilePath + " to the actual video meta file"); 
			if(!transferredVideoFiles.containsKey(sourceMetaFilePath))
			{
				transferredVideoFiles.put(sourceMetaFilePath, "1");
				appendToVideosMetaFile(sourceMetaFilePath);
			}
		}
	}

	private List<Video> getVideosFromFile(String fileName)
	{
		try 
		{
			Videos videos = Videos.parseFrom(new FileInputStream(fileName));
			return new ArrayList<Video>(videos.getVideoList());
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
			return  new ArrayList<Article> (articles.getArticleList());
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
		List<Video> existingMetaFileVideos = getVideosFromFile(baseDirPath + "/" + VIDEO_META_FILE_LOCATION);
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
			appendedVideos.writeTo(new FileOutputStream(baseDirPath + "/" + VIDEO_META_FILE_LOCATION));
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
		Log.d(tagName, "Trying to write file at location sourceMetaFilePath to main new file"); 
		List<Article> newArticles = getArticlesFromMetaFile(sourceMetaFilePath);
		List<Article> existingArticles = getArticlesFromMetaFile(baseDirPath + "/" + ARTICLE_META_FILE_LOCATION);
		if(newArticles.isEmpty())
		{
			Log.d(tagName, " Returning since the newArtciles are empty.");
			return;
		}
		existingArticles.addAll(newArticles);
		Log.d(tagName, "Writing updated articles into main meta file after merge") ; 
		writeUpdatedArticles(existingArticles);
	}

	private void writeUpdatedArticles(List<Article> existingArticles) 
	{
		edu.isi.usaid.pifi.metadata.ArticleProtos.Articles.Builder articleBuilder = Articles.newBuilder();
		articleBuilder.addAllArticle(existingArticles);
		Articles appendedArticles = articleBuilder.build();
		try 
		{
			appendedArticles.writeTo(new FileOutputStream(baseDirPath + "/" + ARTICLE_META_FILE_LOCATION));
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(tagName, "FileNotFoundException occured for the article meta data file "
						   + " at location: " + ARTICLE_META_FILE_LOCATION, e.getCause());
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "IOException occured when writing article meta data file "
					   + " at location: " + ARTICLE_META_FILE_LOCATION, e.getCause());
		}
	}

	private boolean isMetaDataFile(String path) 
	{
		return path.endsWith(".meta") || path.endsWith(".dat");
	}

	private void copyFileToBaseDirectory(String sourceFilePath) 
	{
		try {
			String toCopyDir = baseDirPath;
			if(isImageCopied(sourceFilePath))
			{
				String dirName = extractDirectoryName(toAppendPath); 
				Log.d(tagName, "DirName " + dirName + " extracted from the path : " + toAppendPath);
				createDirectoryIfNotExists(dirName, baseDirPath);
				toCopyDir = toCopyDir + "/" + dirName; 
				Log.d(tagName, "toCopydir location: " + toCopyDir );
			}
		Log.d(tagName, "Actual sourceFilePath: " + sourceFilePath); 
		byte[] pathBytes = sourceFilePath.getBytes(); 
		String encodedPath = new String(pathBytes, "UTF-8");
		Log.d(tagName, "Encoded path: " + encodedPath);
		File srcFile = new File(encodedPath).getCanonicalFile();
		File destDir = new File(toCopyDir);
		try 
		{
			FileUtils.copyFileToDirectory(srcFile, destDir);
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "Error occured when trying to copy a file from " + 
							sourceFilePath + " to " + toCopyDir, 
							e.getCause());
			String str = Log.getStackTraceString(e.getCause());
			Log.e(tagName, "Stacktrace string for exception: " + str + " message " + e.getMessage()); 
		}}
		catch(Exception e)
		{
			Log.d(tagName, e.getMessage() );
		}
	}

	private String extractDirectoryName(String toAppendPath2) {
		String[] splittedPaths = toAppendPath.split("/");
		int i=0;
		for(;i<splittedPaths.length;++i)
		{
			if(splittedPaths[i].equals("xfer"))
			{
				i += 2 ; break; 
			}
		}
		 return splittedPaths[i]; 		
	}

	private void createDirectoryIfNotExists(String dirName, String baseDirPath2) 
	{
		String totalDirName = baseDirPath2 + "/" + dirName; 
		File file = new File(totalDirName); 
		if(!file.exists())
		{
			Log.d(tagName, "Directory with path : " + totalDirName + " does not exists."); 
			Log.d(tagName, "Creating directory at location " + totalDirName);
			boolean mkdir = file.mkdir();
			if(mkdir) 
			{
				Log.d(tagName, "Directory creation successful at location " + totalDirName); 
			}
		}
	}

	private boolean isImageCopied(String sourceFilePath) 
	{
		String[] splittedPaths = toAppendPath.split("/");
		int i=0;
		for(;i<splittedPaths.length;++i)
		{
			if(splittedPaths[i].equals("xfer"))
			{
				i += 2 ; break; 
			}
		}
		if(i < splittedPaths.length) return true; 
		else return false;	
	}
}

