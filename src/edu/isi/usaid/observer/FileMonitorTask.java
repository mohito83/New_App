package edu.isi.usaid.observer;

import edu.isi.usaid.pifi.Constants;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class FileMonitorTask extends Service
{
	private static final String TAG = "FileMonitorTaskTag";
	
	private final String path = Environment.getExternalStorageDirectory() + "/" + Constants.contentDirName;
	
	private final CustomFileObserver customFileObserver = new CustomFileObserver(path);
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Printing the start up message for FileMonitorTask");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() 
	{
		Log.d(TAG, "Called inside FileMonitorTaskFileUpdate For oncreateEvent");
		Log.d(TAG, "Manually Starting the fileObserver");
		customFileObserver.startWatching();
		super.onCreate();
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
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

}
