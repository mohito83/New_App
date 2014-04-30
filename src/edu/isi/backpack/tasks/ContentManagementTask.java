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

import edu.isi.backpack.R;
import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.ArticleProtos.Articles;
import edu.isi.backpack.metadata.VideoProtos.Video;
import edu.isi.backpack.metadata.VideoProtos.Videos;
import edu.isi.backpack.util.FileUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

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
            mergeContent(dir, contentDir, false);
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
        IOFileFilter datFilter = FileFilterUtils.suffixFileFilter(".dat");
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
     * @param sourceDir - directory of new content
     * @param contentDir - newDir will merge to content of this directory TODO
     *            be careful of other concurrent content modifications or
     *            reading while modifying content
     */
    public static void mergeContent(File sourceDir, File contentDir, boolean deleteSource) {

        // find metadata files
        File srcVideoFile = new File(sourceDir, Constants.videoMetaFileName);
        File dstVideoFile = new File(contentDir, Constants.videoMetaFileName);
        mergeVideos(srcVideoFile, dstVideoFile, deleteSource);

        File srcArticleFile = new File(sourceDir, Constants.webMetaFileName);
        File dstArticleFile = new File(contentDir, Constants.webMetaFileName);
        mergeArticles(srcArticleFile, dstArticleFile, deleteSource);
    }

    /**
     * merge one video metadata with another and copy any files to destination
     * folder
     * 
     * @param sourceVideoMeta
     * @param destVideoMeta
     */
    public static void mergeVideos(File srcVideoMeta, File dstVideoMeta, boolean deleteSource) {

        try {

            File sourceDir = srcVideoMeta.getParentFile();
            File dstDir = dstVideoMeta.getParentFile();

            // create a new video meta
            Videos.Builder videos = Videos.newBuilder();

            // merge if both meta exist
            Videos srcVideoDat;
            Videos dstVideoDat = null;
            ArrayList<String> oldVideoNames = new ArrayList<String>();
            if (srcVideoMeta.exists() && dstVideoMeta.exists()) {
                FileInputStream is = new FileInputStream(dstVideoMeta);
                dstVideoDat = Videos.parseFrom(is);
                srcVideoDat = Videos.parseFrom(new FileInputStream(srcVideoMeta));
                is.close();

                // get a list of old video file names
                for (Video oldVideo : dstVideoDat.getVideoList())
                    oldVideoNames.add(oldVideo.getFilename()); // assume unique
                                                               // filename, add
                                                               // new only, no
                                                               // update

                // // create new metadata from the old one
                // videos.mergeFrom(dstVideoDat);
            } else if (srcVideoMeta.exists()) {
                srcVideoDat = Videos.parseFrom(new FileInputStream(srcVideoMeta));
            } else
                return;

            // look for new videos to add
            for (Video newVideo : srcVideoDat.getVideoList()) {

                File videoFile = new File(sourceDir, newVideo.getFilepath());
                File thumbFile = new File(videoFile.getParent(), newVideo.getId() + "_default.jpg");

                if (!oldVideoNames.contains(newVideo.getFilename())) {
                    // add metadata entry
                    videos.addVideo(newVideo);
                    // copy video and thumbnail file, keeping relative path
                    File destVideoFile = new File(dstDir, newVideo.getFilepath());
                    File destThumbFile = new File(destVideoFile.getParent(), newVideo.getId()
                            + "_default.jpg");
                    FileUtils.copyFile(videoFile, destVideoFile);
                    FileUtils.copyFile(thumbFile, destThumbFile);
                }

                if (deleteSource) {
                    videoFile.delete();
                    thumbFile.delete();
                }
            }

            // merge with old one (so new entries are first)
            if (dstVideoDat != null)
                videos.mergeFrom(dstVideoDat);

            // write out merged metadata
            FileOutputStream out = new FileOutputStream(dstVideoMeta);
            videos.build().writeTo(out);
            out.close();

            if (deleteSource)
                srcVideoMeta.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mergeArticles(File srcArticleMeta, File dstArticleMeta, boolean deleteSource) {

        try {

            File sourceDir = srcArticleMeta.getParentFile();
            File dstDir = dstArticleMeta.getParentFile();

            // create a new Article meta
            Articles.Builder articles = Articles.newBuilder();

            // merge if both meta exist
            Articles srcArticleDat;
            Articles dstArticleDat = null;
            ArrayList<String> oldArticleNames = new ArrayList<String>();
            if (srcArticleMeta.exists() && dstArticleMeta.exists()) {
                FileInputStream is = new FileInputStream(dstArticleMeta);
                dstArticleDat = Articles.parseFrom(is);
                is.close();
                srcArticleDat = Articles.parseFrom(new FileInputStream(srcArticleMeta));

                // get a list of old article file names
                for (Article oldArticle : dstArticleDat.getArticleList())
                    oldArticleNames.add(oldArticle.getFilename()); // assume
                                                                   // unique
                                                                   // filename,
                                                                   // add new
                                                                   // only, no
                                                                   // update

            } else if (srcArticleMeta.exists()) {
                srcArticleDat = Articles.parseFrom(new FileInputStream(srcArticleMeta));
            } else
                return;

            // look for new articles to add
            for (Article newArticle : srcArticleDat.getArticleList()) {

                File articleFile = new File(sourceDir, newArticle.getFilename());
                String imageDirName = FilenameUtils.getBaseName(newArticle.getFilename());
                File imageDir = new File(articleFile.getParent(), imageDirName);

                if (!oldArticleNames.contains(newArticle.getFilename())) {
                    // add metadata entry
                    articles.addArticle(newArticle);
                    // copy article and image files, keeping relative path
                    File destArticleFile = new File(dstDir, newArticle.getFilename());
                    FileUtils.copyFile(articleFile, destArticleFile);
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
                }

                if (deleteSource) {
                    articleFile.delete();
                    if (imageDir.exists())
                        FileUtils.deleteDirectory(imageDir);
                }
            }

            // merge with old entries (so new entries on top)
            articles.mergeFrom(dstArticleDat);

            // write out merged metadata
            FileOutputStream out = new FileOutputStream(dstArticleMeta);
            articles.build().writeTo(out);
            out.close();

            if (deleteSource)
                srcArticleMeta.delete();

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
