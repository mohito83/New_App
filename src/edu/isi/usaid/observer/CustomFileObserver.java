package edu.isi.usaid.observer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos.Builder;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class CustomFileObserver extends FileObserver
{
	public static final String VIDEO_META_FILE_LOCATION = Constants.metaFileName;

	private static String tagName = "CustomFileObserver";
	
	private final String baseDirPath = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;
	
	public CustomFileObserver(String path) 
	{
		super(path);
	}
	
	public boolean isTransferDirectoryContent(String path)
	{
		return path != null && path.contains("transfer");
	}
	@Override
	
	public void onEvent(int event, String path) 
	{
		Log.d(tagName, "Got event for file with path: " + path);
		if(event == FileObserver.CREATE)
		{
			if(isTransferDirectoryContent(path)) 
			{
				if(isMetaDataFile(path))
				{
					appendContentToMainMetaFile(path);					
				}
				else
				{
					copyFileToBaseDirectory(path);								
				}
			}			
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
			Log.e(tagName, "Exception in merging metadatafile. No File exists at: "   
									  + fileName, e.getCause());
			return new ArrayList<Video>();
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "Exception in merging metadatafile. Exception while reading file at: "   
					  				   + fileName, e.getCause());
			return new ArrayList<Video>();
		}
	}
	
	private void appendToVideosMetaFile(String sourceMetaFilePath) 
	{
		List<Video> newMetaFileVideos = getVideosFromFile(sourceMetaFilePath);
		List<Video> existingMetaFileVideos = getVideosFromFile(VIDEO_META_FILE_LOCATION);
		if(newMetaFileVideos.isEmpty() || existingMetaFileVideos.isEmpty()) 
			return;
		existingMetaFileVideos.addAll(newMetaFileVideos);
		writeToOutputFile(existingMetaFileVideos);
	}

	private void writeToOutputFile(List<Video> videoList) 
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

