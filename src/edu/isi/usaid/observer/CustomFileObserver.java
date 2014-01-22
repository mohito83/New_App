package edu.isi.usaid.observer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

	private static String TAG = "CustomFileObserver";
	
	private final String contentDirPath = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;
	
	private final String xferDirPath = contentDirPath + "/" + "xfer";
	
	private String observedPath = null;

	private FileMonitorTask fileMonitorTask;
	
	private Map<String, String> transferredVideoFiles = new ConcurrentHashMap<String, String>();
	
	private ArrayList<CustomFileObserver> childObservers = new ArrayList<CustomFileObserver>();
	
	public CustomFileObserver(String path, FileMonitorTask fileMonitorTask) 
	{		
		super(path, FileObserver.CREATE | FileObserver.MOVED_TO);
		observedPath = path;
		this.fileMonitorTask = fileMonitorTask;
		
		// if this is the root of content directory
		// we only want to add observer to xfer directory and its subfolders (if already exists)
		if (path.equals(contentDirPath)){
			for (File file : new File(observedPath).listFiles())
				if (file.getAbsolutePath().equals(xferDirPath))
					addWatcher(file.getAbsolutePath());
		}
		else { 
			// add observer to subdirectories
			for (File file : new File(observedPath).listFiles())
				if (file.isDirectory())
					addWatcher(file.getAbsolutePath());
		}
	}

	private void addWatcher(String path) 
	{
		CustomFileObserver transferDirObserver = new CustomFileObserver(path, fileMonitorTask);
		childObservers.add(transferDirObserver); // keep them from being garbage collected
		transferDirObserver.startWatching();
	}


  /*
   * Listening to move_self event rather than create event, since it is not 
   * possible in create to know if the file transWhat followed next was pretty much in line with the craziness of the entire episode. Within five minutes, the three musketeers could not bear the pain of being seated in the second row in a 7:30 am lecture. They simply got up and sprinted to their (I am guessing) usual seats in the last bench. fer has already been done
   */  
	@Override
	public void onEvent(int event, String path) 
	{
		if (path == null) 
			return; 

		Log.d(TAG, "Got event for " + path);
		if (path.endsWith(".tmp")) // this file is still being written, ignore
			return;

		String fullPath = observedPath + "/" + path;
		
		// if this is the contentDir root observer
		// we only care when xfer directory is created
		if (observedPath.equals(contentDirPath)){
			if (path.equals(Constants.xferDirName)){
				addWatcher(fullPath);
			}
			return;
		}
		else if (new File(fullPath).isDirectory()){
			addWatcher(fullPath);
			return;
		}
		else { // not a directory
			
			// if it's a metadata file
			if(fullPath.endsWith(".dat") || fullPath.endsWith(".meta") || fullPath.endsWith(".dat_rx")){
				appendContentToMainMetaFile(fullPath);
				propogateUpdatedMessage(fullPath);				
			}
			else{
				copyFileToBaseDirectory(fullPath);								
			}
			Log.d(TAG, "Delete " + fullPath);
			cleanupFile(fullPath);
		}			
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
			Log.d(TAG, "Trying to append file with path: " + sourceMetaFilePath + " to the actual news meta file"); 
			appendToNewsMetaFile(sourceMetaFilePath);
		}
		else 
		{			
			Log.d(TAG, "Trying to append file with path: " + sourceMetaFilePath + " to the actual video meta file"); 
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
			Log.e(TAG, "No File exists at: " + fileName, e.getCause());
			return new ArrayList<Video>();
		} 
		catch (IOException e) 
		{
			Log.e(TAG, "Exception in reading metadatafile. Exception while reading file at: "   
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
			Log.e(TAG, "No File exists at: " + fileName, e.getCause());
			return new ArrayList<Article>();
		} 
		catch (IOException e) {
			Log.e(TAG, "Exception in reading metadatafile. Exception while reading file at: "   
	  				   + fileName, e.getCause());
			return new ArrayList<Article>();
		}
	}
	
	private void appendToVideosMetaFile(String sourceMetaFilePath) 
	{
		List<Video> newMetaFileVideos = getVideosFromFile(sourceMetaFilePath);
		List<Video> existingMetaFileVideos = getVideosFromFile(contentDirPath + "/" + VIDEO_META_FILE_LOCATION);
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
			appendedVideos.writeTo(new FileOutputStream(contentDirPath + "/" + VIDEO_META_FILE_LOCATION));
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(TAG, "FileNotFoundException occured for the video meta data file "
						   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		} 
		catch (IOException e) 
		{
			Log.e(TAG, "IOException occured when writing video meta data file "
					   + " at location: " + VIDEO_META_FILE_LOCATION, e.getCause());
		}
		
	}

	private void appendToNewsMetaFile(String sourceMetaFilePath) 
	{
		Log.d(TAG, "Trying to write file at location sourceMetaFilePath to main new file"); 
		List<Article> newArticles = getArticlesFromMetaFile(sourceMetaFilePath);
		List<Article> existingArticles = getArticlesFromMetaFile(contentDirPath + "/" + ARTICLE_META_FILE_LOCATION);
		if(newArticles.isEmpty())
		{
			Log.d(TAG, " Returning since the newArtciles are empty.");
			return;
		}
		existingArticles.addAll(newArticles);
		Log.d(TAG, "Writing updated articles into main meta file after merge") ; 
		writeUpdatedArticles(existingArticles);
	}

	private void writeUpdatedArticles(List<Article> existingArticles) 
	{
		edu.isi.usaid.pifi.metadata.ArticleProtos.Articles.Builder articleBuilder = Articles.newBuilder();
		articleBuilder.addAllArticle(existingArticles);
		Articles appendedArticles = articleBuilder.build();
		try 
		{
			appendedArticles.writeTo(new FileOutputStream(contentDirPath + "/" + ARTICLE_META_FILE_LOCATION));
		} 
		catch (FileNotFoundException e) 
		{
			Log.e(TAG, "FileNotFoundException occured for the article meta data file "
						   + " at location: " + ARTICLE_META_FILE_LOCATION, e.getCause());
		} 
		catch (IOException e) 
		{
			Log.e(TAG, "IOException occured when writing article meta data file "
					   + " at location: " + ARTICLE_META_FILE_LOCATION, e.getCause());
		}
	}


	private void copyFileToBaseDirectory(String sourceFilePath) 
	{
		try {
			String toCopyDir = contentDirPath;
			if(isImageCopied(sourceFilePath))
			{
				String dirName = extractDirectoryName(observedPath); 
				Log.d(TAG, "DirName " + dirName + " extracted from the path : " + observedPath);
				createDirectoryIfNotExists(dirName, contentDirPath);
				toCopyDir = toCopyDir + "/" + dirName; 
				Log.d(TAG, "toCopydir location: " + toCopyDir );
			}
		Log.d(TAG, "Actual sourceFilePath: " + sourceFilePath); 
		byte[] pathBytes = sourceFilePath.getBytes(); 
		String encodedPath = new String(pathBytes, "UTF-8");
		Log.d(TAG, "Encoded path: " + encodedPath);
		File srcFile = new File(encodedPath).getCanonicalFile();
		File destDir = new File(toCopyDir);
		try 
		{
			if(sourceFilePath.contains("20VIRGINIA-articleLarge.jpg"))
			{
				Log.d(TAG, "Copying the file to " + destDir.getCanonicalPath() + " " + destDir.getAbsolutePath());
				Log.d(TAG, "isImageCopied: " + isImageCopied(sourceFilePath)); 
				Log.d(TAG, "toCopyDir :  " + toCopyDir +  " basedir:  " + contentDirPath ); 
			}
			FileUtils.copyFileToDirectory(srcFile, destDir);
		} 
		catch (IOException e) 
		{
			Log.e(TAG, "Exception stuff: " + e.getMessage());
		}}
		catch(Exception e)
		{
			Log.e(TAG, "Exception stuff: " + e.getMessage());
		}
	}

	private String extractDirectoryName(String toAppendPath2) {
		String[] splittedPaths = observedPath.split("/");
		int i=0;
		for(;i<splittedPaths.length;++i)
		{
			if(splittedPaths[i].equals("xfer"))
			{
				i += 2 ; break; 
			}
		}
		String res = "" ; 
		while ( i < splittedPaths.length) 
		{
			res = res + splittedPaths[i]; 
			res += "/";
			++i ; 
		}
		 return res; 		
	}

	private void createDirectoryIfNotExists(String dirName, String baseDirPath2) 
	{
		String totalDirName = baseDirPath2 + "/" + dirName; 
		File file = new File(totalDirName); 
		if(!file.exists())
		{
			Log.d(TAG, "Directory with path : " + totalDirName + " does not exists."); 
			Log.d(TAG, "Creating directory at location " + totalDirName);
			boolean mkdir = file.mkdir();
			if(mkdir) 
			{
				Log.d(TAG, "Directory creation successful at location " + totalDirName); 
			}
		}
	}

	private boolean isImageCopied(String sourceFilePath) 
	{
		String[] splittedPaths = observedPath.split("/");
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
	
	@Override
	public void startWatching(){
		Log.d(TAG, "Start watching " + this.observedPath);
		super.startWatching();
	}
	
	@Override
	public void stopWatching(){
		Log.d(TAG, "Stop watching " + this.observedPath);
		super.startWatching();
	}
}

