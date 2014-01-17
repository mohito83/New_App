/**
 * 
 */
package edu.isi.usaid.pifi.tasks;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.Toast;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.ContentListActivity;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import edu.isi.usaid.pifi.util.FileUtil;

/**
 * @author jenniferchen
 *
 * 1. Download or use a local copy of the content package file.
 * 2.1 If overwrite current content
 *   2.1.1 Delete current content
 *   2.1.2 Extract package to content directory
 * 2.2 If merging with current content
 * 	 2.2.1 Extract package to tmp directory
 *   2.2.2 Merge tmp to content directory
 *   2.2.3 Delete tmp directory
 *   
 */
public class ContentManagementTask extends AsyncTask<Void, Integer, String>{
	
	public static final String TAG = "ContentManagementTask";
	
	public static final int TASK_OVERWRITE = 0;
	
	public static final int TASK_MERGE = 1;
	
	public static final String ERROR_ABORTED = "Download Canceled";
	
	public static final String ERROR_CONNECT = "Connection Error";
	
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
	 * @param c
	 * @param packageUrl - where to download package
	 * @param pd - progress dialog
	 * @param packageFile - where to save downloaded package
	 * @param contentDir - backpack content directory
	 * @param overrite - overwrite or merge content
	 */
	public ContentManagementTask(
			ContentListActivity c, 
			String packageUrl, 
			ProgressDialog pd, 
			File packageFile, 
			File contentDir,
			boolean overwrite){
		context = c;
		progressDialog = pd;
		this.packageUrl = packageUrl;
		this.packageFile = packageFile;
		this.contentDir = contentDir;
		this.overwrite = overwrite;
	}
	
	/**
	 * Already has local copy of packageFile
	 * @param c
	 * @param pd - progress dialog
	 * @param packageFile - where to find the content package
	 * @param contentDir - backpack content directory
	 * @param overrite - overwrite or merge content
	 */
	public ContentManagementTask(
			ContentListActivity c, 
			ProgressDialog pd, 
			File packageFile, 
			File contentDir,
			boolean overwrite){
		context = c;
		progressDialog = pd;
		this.packageFile = packageFile;
		this.contentDir = contentDir;
		this.overwrite = overwrite;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected String doInBackground(Void... params) {
		
		// if need to download package file
		if (packageUrl != null){
			
			// take CPU lock to prevent CPU from going off if the user 
	        // presses the power button during download
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	             getClass().getName());
	        wl.acquire();
	        
	        InputStream input = null;
	        OutputStream output = null;
	        HttpURLConnection connection = null;
	
	        // download the file
	        try {
	        	URL u = new URL(packageUrl);
	            connection = (HttpURLConnection) u.openConnection();
	            connection.connect();
	            	
	            // unable to connect
	            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
	            	wl.release();
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
	        		if (isCancelled()){
	        			input.close();
	        			output.close();
	        			packageFile.delete();
	        			wl.release();
	        			return ERROR_ABORTED;
	                }
	        		
	        		output.write(data, 0, count);
	        		
	        		total += count;
	        		long percent = (long)total * 100l / (long)fileLength;
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
	            wl.release();
	            
	        } catch (Exception e) {
	    		wl.release();
				e.printStackTrace();
				return ERROR_DOWNLOAD;
			}
		} // if packageUrl != null
		
		
		// extract file to a temporary folder
		publishProgress(PROGRESS_EXTRACT);
		File tmpDir = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				"backpackTmp");
		if (tmpDir.exists())
			try {
				FileUtils.cleanDirectory(tmpDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else
			tmpDir.mkdir();
		String res = FileUtil.unzip(tmpDir, packageFile);
		if (res != null){
			packageFile.delete();
			return ERROR_EXTRACT;
		}
		
		// delete old content and bookmarks if overwrite is true
		if (overwrite){
			try {
				FileUtils.cleanDirectory(contentDir);
				context.removeAllBookmarks(false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		// merge
		publishProgress(PROGRESS_MERGE);
		
		// TODO traverse the extracted folder look for .dat files
		// the level with the .dat files is a content directory
		// there could be several content directories in a package
		// need to merge each content directory with the app's content dir
		ArrayList<File> contentDirs = findContentDirectories(tmpDir);
		for (File dir : contentDirs){
			// merge new dir to content dir
			mergeContent(dir, contentDir);
		}
		
		
		// delete temp directory
		publishProgress(PROGRESS_CLEANUP);
		try {
			FileUtils.cleanDirectory(tmpDir);
			tmpDir.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return SUCCESSFUL;
		
	}
	
	protected ArrayList<File> findContentDirectories(File directory){
		
		ArrayList<File> contentDirs = new ArrayList<File>();
		traverse(directory, contentDirs);
		return contentDirs;
			
	}
	
	/**
	 * traverse until found the directory with .dat
	 * @param directory
	 * @return
	 */
	private void traverse(File directory, ArrayList<File> contentDirs){
		
		// if contains any .dat file, start merging
		// if not, traverse until .dat file is found
		IOFileFilter datFilter = FileFilterUtils.suffixFileFilter(".dat");
		IOFileFilter datFileFilter = FileFilterUtils.and(FileFileFilter.FILE, datFilter);
		IOFileFilter dirFilter = FileFilterUtils.directoryFileFilter();
		Collection<File> dats = FileUtils.listFiles(directory, datFileFilter, null);
		
		// no .dat files, travers child directories
		if (dats.isEmpty()){
			File[] dirs = directory.listFiles((FileFilter)dirFilter);
			for (File childDir : dirs){
				if (childDir.isDirectory())
					traverse(childDir, contentDirs);
			}
		}
		else { // found .dat
			contentDirs.add(directory);
		}
	}
	
	/**
	 * Merge two content packages
	 * @param sourceDir - directory of new content
	 * @param contentDir - newDir will merge to content of this directory
	 * 
	 * TODO be careful of other concurrent content modifications or reading while modifying content
	 *
	 */
	public static void mergeContent(File sourceDir, File contentDir){
		
		try{
		
			// find metadata files
			File srcVideoFile = new File(sourceDir, Constants.videoMetaFileName);
			File srcArticleFile = new File(sourceDir, Constants.webMetaFileName);
			File dstVideoFile = new File(contentDir, Constants.videoMetaFileName);
			File dstArticleFile = new File(contentDir, Constants.webMetaFileName);
			
			// new video meta
			Videos.Builder videos = Videos.newBuilder();
			
			// merge if both meta exist
			if (srcVideoFile.exists() && dstVideoFile.exists()){
				FileInputStream is = new FileInputStream(dstVideoFile);
				Videos dstVideoDat = Videos.parseFrom(is);
				Videos srcVideoDat = Videos.parseFrom(new FileInputStream(srcVideoFile));
				
				// get a list of old video file names
				ArrayList<String> oldVideoNames = new ArrayList<String>();
				for (Video oldVideo : dstVideoDat.getVideoList())
					oldVideoNames.add(oldVideo.getFilename()); // assume unique filename, add new only, no update
				
				// create new metadata from the old one
				videos.mergeFrom(dstVideoDat);
				
				// look for new videos to add
				for (Video newVideo : srcVideoDat.getVideoList()){
					if (!oldVideoNames.contains(newVideo.getFilename()))
						videos.addVideo(newVideo);
				}
				
				// write out merged metadata
				is.close();
				FileOutputStream out = new FileOutputStream(dstVideoFile);
				videos.build().writeTo(out);
				out.close();
			} 
			else if (srcVideoFile.exists()){ 
				Videos srcVideoDat = Videos.parseFrom(new FileInputStream(srcVideoFile));
				videos.mergeFrom(srcVideoDat);
				
				FileOutputStream out = new FileOutputStream(dstVideoFile);
				videos.build().writeTo(out);
				out.close();
			}
			
			
			
			// new Article meta
			Articles.Builder articles = Articles.newBuilder();
			
			// merge if both meta exist
			if (srcArticleFile.exists() && dstArticleFile.exists()){
				FileInputStream is = new FileInputStream(dstArticleFile);
				Articles dstArticleDat = Articles.parseFrom(is);
				Articles srcArticleDat = Articles.parseFrom(new FileInputStream(srcArticleFile));
				
				// get a list of old article file names
				ArrayList<String> oldArticleNames = new ArrayList<String>();
				for (Article oldArticle : dstArticleDat.getArticleList())
					oldArticleNames.add(oldArticle.getFilename()); // assume unique filename, add new only, no update
				
				// create new metadata from the old one
				articles.mergeFrom(dstArticleDat);
				
				// look for new articles to add
				for (Article newArticle : srcArticleDat.getArticleList()){
					if (!oldArticleNames.contains(newArticle.getFilename()))
						articles.addArticle(newArticle);
				}
				
				// write out merged metadata
				is.close();
				FileOutputStream out = new FileOutputStream(dstArticleFile);
				articles.build().writeTo(out);
				out.close();
			} 
			else if (srcArticleFile.exists()){ 
				Articles srcArticleDat = Articles.parseFrom(new FileInputStream(srcArticleFile));
				articles.mergeFrom(srcArticleDat);
				
				FileOutputStream out = new FileOutputStream(dstArticleFile);
				articles.build().writeTo(out);
				out.close();
			}
			
			// copy content except metadata
			IOFileFilter dats = FileFilterUtils.suffixFileFilter(".dat");
			FileFilter filter = FileFilterUtils.notFileFilter(dats);
			FileUtils.copyDirectory(sourceDir, contentDir, filter);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected void onPreExecute() {
	    super.onPreExecute();
	    progressDialog.show();
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
	    
	    if (progress[0] == PROGRESS_UNKNOWN_SIZE)
	    	progressDialog.setMessage("Downloading Content (file size unknown)");
	    else if (progress[0] == PROGRESS_EXTRACT)
	    	progressDialog.setMessage("Extracting Package");
	    else if (progress[0] == PROGRESS_MERGE)
	    	progressDialog.setMessage("Merging content");
	    else if (progress[0] == PROGRESS_CLEANUP)
	    	progressDialog.setMessage("Clean Up");
	    else
		    progressDialog.setMessage("Downloading Content (" + progress[0] + "%)");
    }
	
	@Override
	protected void onPostExecute(String res) {
		progressDialog.dismiss();
		Toast.makeText(context, res, Toast.LENGTH_LONG).show();
		context.reload(false);
	}

}
