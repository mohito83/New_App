package edu.isi.usaid.observer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import edu.isi.usaid.pifi.Constants;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class CustomFileObserver extends FileObserver
{
	private static String tagName = "CustomFileObserver";
	
	private final String baseDirPath = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;
	
	public CustomFileObserver(String path) 
	{
		super(path);
	}

	public boolean isDirectory(String path)
	{
		File file = new File(path);
		return file.isDirectory();
	}
	
	@Override
	public void onEvent(int event, String path) 
	{
		Log.d(tagName, "Got event for file with path: " + path);
		if(event == FileObserver.CREATE)
		{
			/*
			 * Not Copying directories. Copying only the files that are created 
			 */
			if(!isDirectory(path)) 
			{
				copyFileToBaseDirectory(baseDirPath, path);
			}			
		}
	}

	private void copyFileToBaseDirectory(String basedir, String sourceFilePath) 
	{
		File srcFile = new File(sourceFilePath);
		File destDir = new File(basedir);
		try 
		{
			FileUtils.copyFileToDirectory(srcFile, destDir);
		} 
		catch (IOException e) 
		{
			Log.e(tagName, "Error occured when trying to copy a file from " + 
							sourceFilePath + " to " + basedir, 
							e.getCause());
		}
	}
}

