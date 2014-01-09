/**
 * 
 */
package edu.isi.usaid.pifi.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;
import edu.isi.usaid.pifi.ContentListActivity;
import edu.isi.usaid.pifi.util.FileUtil;

/**
 * @author jenniferchen
 *
 */
public class DownloadTask extends AsyncTask<Void, Integer, String> {
	
	public static final String TAG = "DownloadTask";
	
	private ContentListActivity context;
	
	private ProgressDialog progressDialog;
	
	private String url;
	
	private File contentDir;
	
	private File localFile;
	
	protected boolean extractOnly = false;
	
	/**
	 * 
	 * @param c
	 * @param pd
	 * @param localFile - where to save the file, task does not delete this file
	 * @param contentDir - directory where content will be extracted
	 */
	public DownloadTask(
			ContentListActivity c, 
			String url, 
			ProgressDialog pd, 
			File localFile, 
			File contentDir){
		context = c;
		progressDialog = pd;
		this.url = url;
		this.localFile = localFile;
		this.contentDir = contentDir;
	}
	
	public static DownloadTask createExtractOnlyTask(ContentListActivity c, ProgressDialog pd, File localFile, File contentDir){
		DownloadTask task = new DownloadTask(c, null, pd, localFile, contentDir);
		task.extractOnly = true;
		return task;
		
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 * 
	 * returns the downloaded file
	 * 
	 */
	@Override
	protected String doInBackground(Void... v) {
		
		if (!extractOnly){
			
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
	        	URL u = new URL(url);
	            connection = (HttpURLConnection) u.openConnection();
	            connection.connect();
	            	
	            // unable to connect
	            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
	            	wl.release();
	            	return "Unable to connect, abort";
	            }
	
	            int fileLength = connection.getContentLength();  
	            if (fileLength < 0)
	            	publishProgress(-1);
	        	input = connection.getInputStream();
	        	output = new FileOutputStream(localFile);
		
	        	byte data[] = new byte[4096];
	        	int total = 0;
	        	int count;
	        	while ((count = input.read(data)) != -1) {
	        		// allow canceling with back button
	        		if (isCancelled()){
	        			input.close();
	        			output.close();
	        			localFile.delete();
	        			wl.release();
	        			return "download aborted";
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
				return "Download failed";
			}
        }
            
        // extract file
		publishProgress(1000);
    	String res = FileUtil.unzip(contentDir, localFile);
    	if (res != null){ // unzip failed
    		localFile.delete();
    		return res;
    	}
            
            
        
		return "Content downloaded";
	}
	
	@Override
	protected void onPreExecute() {
	    super.onPreExecute();
	    progressDialog.show();
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
	    super.onProgressUpdate(progress);
	    
	    if (progress[0] > 100) // finished
	    	progressDialog.setMessage("Extracting Content");
	    else if (progress[0] == -1)
	    	progressDialog.setMessage("Downloading Content (file size unknown)");
	    else
		    progressDialog.setMessage("Downloading Content (" + progress[0] + "%)");
	}
	
	@Override
	protected void onPostExecute(String result) {
	    progressDialog.dismiss();
	    Toast.makeText(context, result, Toast.LENGTH_LONG).show();
	    context.reload(false);
	}
	
	@Override
	protected void onCancelled(String result){
		Toast.makeText(context, result, Toast.LENGTH_LONG).show();
	}

}
