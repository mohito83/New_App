/**
 * 
 */

package edu.isi.backpack.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.util.FileUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.balatarin.android.R;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author jenniferchen 1. Download or use a local copy of the content package
 *         file. 2.1 If overwrite current content 2.1.1 Delete current content
 *         2.1.2 Extract package to content directory 2.2 If merging with
 *         current content 2.2.1 Extract package to tmp directory 2.2.2 Merge
 *         tmp to content directory 2.2.3 Delete tmp directory
 */
public class ContentManagementTask extends AsyncTask<Void, Integer, String> {

    public static final String TAG = "ContentManagementTask";

    public static final int TASK_OVERWRITE = 0;

    public static final int TASK_MERGE = 1;

    public static final String ERROR_ABORTED = "Download Canceled";

    public static final String ERROR_CONNECT = "File not available";

    public static final String ERROR_DOWNLOAD = "Download Failed";

    public static final String ERROR_EXTRACT = "Extraction Failed";

    public static final String SUCCESSFUL = "Successful";

    public static final int PROGRESS_UNKNOWN_SIZE = -1;

    public static final int PROGRESS_EXTRACT = 100;

    public static final int PROGRESS_MERGE = 200;

    public static final int PROGRESS_CLEANUP = 300;

    private ContentListActivity context;

    private ProgressDialog progressDialog;

    /** where to download zipped content package **/
    private String packageUrl = null;

    /** downloaded zipped package file **/
    private File packageFile;

    /** backpack content package directory **/
    private File contentDir;

    boolean overwrite;

    /**
     * Download url and save as packageFile
     * 
     * @param c
     * @param packageUrl - where to download package
     * @param pd - progress dialog
     * @param packageFile - where to save downloaded package, set null will not
     *            save the file
     * @param contentDir - backpack content directory
     * @param overrite - overwrite or merge content
     */
    public ContentManagementTask(ContentListActivity c, String packageUrl, ProgressDialog pd,
            File packageFile, File contentDir, boolean overwrite) {
        context = c;
        progressDialog = pd;
        this.packageUrl = packageUrl;
        this.packageFile = packageFile;
        this.contentDir = contentDir;
        this.overwrite = overwrite;
    }

    /**
     * Already has local copy of packageFile
     * 
     * @param c
     * @param pd - progress dialog
     * @param packageFile - where to find the content package
     * @param contentDir - backpack content directory
     * @param overrite - overwrite or merge content
     */
    public ContentManagementTask(ContentListActivity c, ProgressDialog pd, File packageFile,
            File contentDir, boolean overwrite) {
        context = c;
        progressDialog = pd;
        this.packageFile = packageFile;
        this.contentDir = contentDir;
        this.overwrite = overwrite;
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected String doInBackground(Void... params) {

        boolean deleteLocalFile = false;
        if (packageFile == null) {
            deleteLocalFile = true;
            packageFile = new File(context.getExternalCacheDir(), "backpackPkgTmp.zip");
        }

        // if need to download package file
        if (packageUrl != null) {

            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass()
                    .getName());

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            // download the file
            try {
                wl.acquire();
                URL u = new URL(packageUrl);
                connection = (HttpURLConnection) u.openConnection();
                connection.connect();

                // unable to connect
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return ERROR_CONNECT;
                }

                int fileLength = connection.getContentLength();
                if (fileLength < 0)
                    publishProgress(PROGRESS_UNKNOWN_SIZE);
                input = connection.getInputStream();
                output = new FileOutputStream(packageFile);

                byte data[] = new byte[4096];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        output.close();
                        packageFile.delete();
                        return ERROR_ABORTED;
                    }

                    output.write(data, 0, count);

                    total += count;
                    long percent = (long) total * 100l / (long) fileLength;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) percent);
                    else
                        publishProgress(-1);

                }

                if (output != null)
                    output.close();
                if (input != null)
                    input.close();

                if (connection != null)
                    connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                return ERROR_DOWNLOAD;
            } finally {
                wl.release();
            }
        } // if packageUrl != null

        if (isCancelled()) {
            packageFile.delete();
            return ERROR_ABORTED;
        }

        // extract file to a temporary folder
        publishProgress(PROGRESS_EXTRACT);
        File tmpDir = new File(context.getExternalCacheDir(), "backpackTmp");
        if (tmpDir.exists())
            try {
                FileUtils.cleanDirectory(tmpDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        else
            tmpDir.mkdir();
        String res = FileUtil.unzip(tmpDir, packageFile);
        if (res != null) {
            packageFile.delete();
            return ERROR_EXTRACT;
        }

        if (isCancelled()) {
            packageFile.delete();
            try {
                FileUtils.cleanDirectory(tmpDir);
                tmpDir.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ERROR_ABORTED;
        }

        // delete old content and bookmarks if overwrite is true
        if (overwrite) {
            try {
                FileUtils.cleanDirectory(contentDir);
                context.removeAllBookmarks(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // merge
        publishProgress(PROGRESS_MERGE);

        // traverse the extracted folder look for .dat files
        // the level with the .dat files is a content directory
        // there could be several content directories in a package
        // need to merge each content directory with the app's content dir
        ArrayList<File> contentDirs = findContentDirectories(tmpDir);
        for (File dir : contentDirs) {
            // merge new dir to content dir
            mergeContent(new File(dir, Constants.metaFileName), new File(contentDir, Constants.metaFileName), false);
        }

        // delete temp directory
        publishProgress(PROGRESS_CLEANUP);
        try {
            FileUtils.cleanDirectory(tmpDir);
            tmpDir.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // delete local file
        if (deleteLocalFile)
            packageFile.delete();

        return SUCCESSFUL;

    }

    protected ArrayList<File> findContentDirectories(File directory) {

        ArrayList<File> contentDirs = new ArrayList<File>();
        traverse(directory, contentDirs);
        return contentDirs;

    }

    /**
     * traverse until found the directory with .dat
     * 
     * @param directory
     * @return
     */
    private void traverse(File directory, ArrayList<File> contentDirs) {

        // if contains any .dat file, start merging
        // if not, traverse until .dat file is found
        IOFileFilter datFilter = FileFilterUtils.suffixFileFilter(".db");
        IOFileFilter datFileFilter = FileFilterUtils.and(FileFileFilter.FILE, datFilter);
        IOFileFilter dirFilter = FileFilterUtils.directoryFileFilter();
        Collection<File> dats = FileUtils.listFiles(directory, datFileFilter, null);

        // no .dat files, travers child directories
        if (dats.isEmpty()) {
            File[] dirs = directory.listFiles((FileFilter) dirFilter);
            for (File childDir : dirs) {
                if (childDir.isDirectory())
                    traverse(childDir, contentDirs);
            }
        } else { // found .dat
            contentDirs.add(directory);
        }
    }

    /**
     * Merge two content packages
     * 
     * @param srcFile - source metadata
     * @param dstFile - destination metadata TODO be
     *            careful of other concurrent content modifications or reading
     *            while modifying content
     */
    public static void mergeContent(File srcFile, File dstFile, boolean deleteSource) {

        try {
            
            File srcDir = srcFile.getParentFile();
            File dstDir = dstFile.getParentFile();

            // create a new meta
            Media.Builder mediaBuilder = Media.newBuilder();

            // merge if both meta exist
            Media srcDat;
            Media dstDat = null;
            ArrayList<String> oldItemNames = new ArrayList<String>();
            if (srcFile.exists() && dstFile.exists()) {
                FileInputStream is = new FileInputStream(dstFile);
                dstDat = Media.parseFrom(is);
                is.close();
                srcDat = Media.parseFrom(new FileInputStream(srcFile));

                // get a list of old article file names
                for (Media.Item oldItem : dstDat.getItemsList())
                    oldItemNames.add(oldItem.getFilename()); // assume
                                                             // unique
                                                             // filename,
                                                             // add new
                                                             // only, no
                                                             // update

            } else if (srcFile.exists()) {
                srcDat = Media.parseFrom(new FileInputStream(srcFile));
            } else
                return;

            // look for new items to add
            for (Media.Item newItem : srcDat.getItemsList()) {

                File contentFile = new File(srcDir, newItem.getFilename());
                String imageDirName = FilenameUtils.getBaseName(newItem.getFilename());
                File imageDir = new File(contentFile.getParent(), imageDirName);
                File srcThumbFile = new File(srcDir, newItem.getThumbnail());

                if (!oldItemNames.contains(newItem.getFilename())) {
                    // add metadata entry
                    mediaBuilder.addItems(newItem);
                    // copy assets, keeping relative path
                    File destContentFile = new File(dstDir, newItem.getFilename());
                    FileUtils.copyFile(contentFile, destContentFile);
                    // image files
                    if (imageDir.exists()) {
                        for (File image : imageDir.listFiles()) {
                            File destImageFile = new File(dstDir, imageDirName + "/"
                                    + image.getName());
                            FileUtils.copyFile(image, destImageFile);
                        }
                        if (deleteSource)
                            FileUtils.deleteDirectory(imageDir);
                    }
                    // thumbnail
                    File dstThumbFile = new File(dstDir, newItem.getThumbnail());
                    if (!dstThumbFile.exists()) {
                        FileUtils.copyFile(srcThumbFile, dstThumbFile);
                    }
                }

                if (deleteSource) {
                    contentFile.delete();
                    if (imageDir.exists())
                        FileUtils.deleteDirectory(imageDir);
                    srcThumbFile.delete();
                }
            }

            // merge with old entries (so new entries on top)
            mediaBuilder.mergeFrom(dstDat);

            // write out merged metadata
            FileOutputStream out = new FileOutputStream(dstFile);
            mediaBuilder.build().writeTo(out);
            out.close();

            if (deleteSource)
                srcFile.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // if user canceled
        progressDialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
                Toast.makeText(context, context.getString(R.string.download_cancel),
                        Toast.LENGTH_SHORT).show();
            }

        });
        progressDialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);

        if (progress[0] == PROGRESS_UNKNOWN_SIZE)
            progressDialog.setMessage(context.getString(R.string.progress_unknown_size));
        else if (progress[0] == PROGRESS_EXTRACT)
            progressDialog.setMessage(context.getString(R.string.progress_extracting));
        else if (progress[0] == PROGRESS_MERGE)
            progressDialog.setMessage(context.getString(R.string.progress_merging));
        else if (progress[0] == PROGRESS_CLEANUP)
            progressDialog.setMessage(context.getString(R.string.progress_cleanup));
        else
            progressDialog.setMessage(context.getString(R.string.progress_downloading) + " "
                    + progress[0] + "%");
    }

    @Override
    protected void onPostExecute(String res) {
        progressDialog.dismiss();
        Toast.makeText(context, res, Toast.LENGTH_LONG).show();
        context.reload(false);
    }

}
