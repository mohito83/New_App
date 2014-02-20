package edu.isi.backpack.services;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.isi.backpack.CustomFileObserver;
import edu.isi.backpack.constants.Constants;

/**
 * Using service to create FileObserver so that it will not be garbage collected
 *  
 * @author jenniferchen
 *
 */
public class FileMonitorService extends Service
{
	private static final String TAG = "FileMonitorTaskTag";
	
	private CustomFileObserver customFileObserver; // global keeps it from being garbage collected
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Printing the start up message for FileMonitorService");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() 
	{
		super.onCreate();
		File contentDir = new File(Environment.getExternalStorageDirectory(), Constants.contentDirName);
		customFileObserver
			= new CustomFileObserver(contentDir.getAbsolutePath(), this);
		customFileObserver.startWatching();
	}

	@Override
	public void onDestroy() 
	{
		Log.d(TAG, "Called inside FileMonitorTaskFileUpdate For onDestory event");
		super.onDestroy();
	}

	@Override
	public void onRebind(Intent intent) 
	{
		Log.d(TAG, "Called inside FileMonitorTaskFileUpdate For rebind event");
		super.onRebind(intent);
	}

	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) 
	{
		super.onStart(intent, startId);
	}

}