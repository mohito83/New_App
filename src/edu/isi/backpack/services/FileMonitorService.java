
package edu.isi.backpack.services;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import edu.isi.backpack.CustomFileObserver;
import edu.isi.backpack.constants.Constants;

import java.io.File;

/**
 * Using service to create FileObserver so that it will not be garbage collected
 * 
 * @author jenniferchen
 */
public class FileMonitorService extends Service {

    private CustomFileObserver customFileObserver; // global keeps it from being
                                                   // garbage collected

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        File contentDir = new File(Environment.getExternalStorageDirectory(),
                Constants.contentDirName);
        customFileObserver = new CustomFileObserver(contentDir.getAbsolutePath(), this);
        customFileObserver.startWatching();
    }

}
