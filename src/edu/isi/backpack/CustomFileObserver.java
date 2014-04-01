
package edu.isi.backpack;

import android.content.Intent;
import android.os.FileObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.services.FileMonitorService;
import edu.isi.backpack.tasks.ContentManagementTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CustomFileObserver extends FileObserver {
    public static final String ARTICLE_META_FILE_LOCATION = Constants.webMetaFileName;

    public static final String VIDEO_META_FILE_LOCATION = Constants.videoMetaFileName;

    private static String TAG = "CustomFileObserver";

    private static String contentDirPath;

    private static String xferDirPath;

    private String observedPath = null;

    private FileMonitorService fileMonitorService;

    private ArrayList<CustomFileObserver> childObservers = new ArrayList<CustomFileObserver>();

    public CustomFileObserver(String path, FileMonitorService fileMonitorService, boolean root) {
        super(path, FileObserver.CREATE | FileObserver.MOVED_TO);
        observedPath = path;
        this.fileMonitorService = fileMonitorService;
        if (root) {
            contentDirPath = path;
            xferDirPath = contentDirPath + "/xfer";
        }

        try {
            // if this is the root of content directory
            // we only want to add observer to xfer directory and its subfolders
            // (if already exists)
            if (path.equals(contentDirPath)) {
                for (File file : new File(observedPath).listFiles())
                    if (file.getCanonicalPath().equals(xferDirPath))
                        addWatcher(file.getCanonicalPath());
            } else {
                // add observer to subdirectories
                for (File file : new File(observedPath).listFiles())
                    if (file.isDirectory())
                        addWatcher(file.getCanonicalPath());
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addWatcher(String path) {
        CustomFileObserver transferDirObserver = new CustomFileObserver(path, fileMonitorService,
                false);
        childObservers.add(transferDirObserver); // keep them from being garbage
                                                 // collected
        transferDirObserver.startWatching();
    }

    /*
     * Listening to move_self event rather than create event, since it is not
     * possible in create to know if the file transWhat followed next was pretty
     * much in line with the craziness of the entire episode. Within five
     * minutes, the three musketeers could not bear the pain of being seated in
     * the second row in a 7:30 am lecture. They simply got up and sprinted to
     * their (I am guessing) usual seats in the last bench. fer has already been
     * done
     */
    @Override
    public void onEvent(int event, String path) {
        if (path == null)
            return;

        Log.d(TAG, "Got event for " + path);
        if (path.endsWith(".tmp")) // this file is still being written, ignore
            return;

        String fullPath = observedPath + "/" + path;

        // if this is the contentDir root observer
        // we only care when xfer directory is created
        if (observedPath.equals(contentDirPath)) {
            if (path.equals(Constants.xferDirName)) {
                addWatcher(fullPath);
            }
            return;
        } else if (new File(fullPath).isDirectory()) {
            addWatcher(fullPath);
            return;
        } else { // not a directory

            // if it's a metadata file
            if (fullPath.endsWith(".dat") || fullPath.endsWith(".meta")
                    || fullPath.endsWith(".dat_rx")) {
                if (fullPath.contains(".html")) {
                    File srcMeta = new File(fullPath);
                    File dstMeta = new File(contentDirPath + "/" + Constants.webMetaFileName);
                    ContentManagementTask.mergeArticles(srcMeta, dstMeta, true);
                } else {
                    File srcMeta = new File(fullPath);
                    File dstMeta = new File(contentDirPath + "/" + Constants.videoMetaFileName);
                    ContentManagementTask.mergeVideos(srcMeta, dstMeta, true);
                }
                // broadcast metadata updated
                propogateUpdatedMessage(fullPath);
            }
        }
    }

    private void propogateUpdatedMessage(String path) {
        Intent messageIntent = new Intent();
        messageIntent.setAction(Constants.META_UPDATED_ACTION);
        LocalBroadcastManager.getInstance(fileMonitorService).sendBroadcast(messageIntent);
    }

    @Override
    public void startWatching() {
        Log.d(TAG, "Start watching " + this.observedPath);
        super.startWatching();
    }

    @Override
    public void stopWatching() {
        Log.d(TAG, "Stop watching " + this.observedPath);
        super.startWatching();
    }
}
