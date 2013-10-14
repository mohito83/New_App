package edu.isi.usaid.observer;

import android.os.FileObserver;
import android.util.Log;

public class CustomFileObserver extends FileObserver
{
	private static String tagName = "CustomFileObserver";
	
	public CustomFileObserver(String path) 
	{
		super(path);
	}

	@Override
	public void onEvent(int event, String path) 
	{
		Log.d(tagName, "Got event for file with path: " + path); 
	}
}

