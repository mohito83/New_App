
package edu.isi.backpack.services;

import android.app.Service;
import android.content.Intent;
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
        File contentDir = new File(getExternalFilesDir(null), Constants.contentDirName);
        customFileObserver = new CustomFileObserver(contentDir.getAbsolutePath(), this, true);
        customFileObserver.startWatching();
    }
}
