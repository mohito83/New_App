/**
 * 
 */
package edu.isi.usaid.pifi.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import edu.isi.usaid.pifi.ContentListActivity;

/**
 * @author jenniferchen
 *
 */
public class DownloadTask extends AsyncTask<String, Integer, String> {
	
	public static final String TAG = "DownloadTask";
	
	private ContentListActivity context;
	
	private ProgressDialog progressDialog;
	
	public DownloadTask(ContentListActivity c, ProgressDialog pd){
		context = c;
		progressDialog = pd;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 * 
	 * returns the downloaded file
	 * 
	 */
	@Override
	protected String doInBackground(String... urls) {
		
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
        	URL url = new URL(urls[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            	
            // unable to connect
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
            	wl.release();
            	return "Unable to connect, abort";
            }

            int fileLength = connection.getContentLength();  
            if (fileLength < 0)
            	publishProgress(-1);
            Log.i(TAG, "file size " + fileLength + " bytes");
        	input = connection.getInputStream();
        	File sdDir = Environment.getExternalStorageDirectory();
        	File file = new File(sdDir, "BackpackContent.zip");
        	output = new FileOutputStream(file);
	
        	byte data[] = new byte[4096];
        	int total = 0;
        	int count;
        	while ((count = input.read(data)) != -1) {
        		// allow canceling with back button
        		if (isCancelled()){
        			input.close();
        			output.close();
        			file.delete();
        			wl.release();
        			return "download aborted";
                }
        		
        		output.write(data, 0, count);
        		
        		total += count;
        		// publishing the progress....
        		if (fileLength > 0) // only if total length is known
        			publishProgress((int) (total * 100 / fileLength));
        		else 
        			publishProgress(-1);
        			
            }
        	
        	publishProgress(1000);
        	
        	if (output != null)
        		output.close();
			if (input != null)
				input.close();
			
			if (connection != null)
				connection.disconnect();
            wl.release();
            
            // extract file
        	String res = unzip(sdDir, file);
        	if (res != null){ // unzip failed
        		file.delete();
        		return res;
        	}
        	
        	// delete file
        	file.delete();
            
    	} catch (Exception e) {
    		wl.release();
			e.printStackTrace();
			return "Download failed";
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
	private String unzip(File directory, File zip){
		try {
			FileInputStream fis = new FileInputStream(zip);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null){
				File entryFile = new File(directory, entry.getName());
				// if directory, create dir
				if (entry.isDirectory()){
					if (!entryFile.exists())
						entryFile.mkdir();
				}
				else { // create file
					FileOutputStream fos = new FileOutputStream(entryFile);
					byte[] buffer = new byte[4096]; 
					for (int c = zis.read(buffer); c != -1; c = zis.read(buffer)) { 
						fos.write(buffer, 0, c); 
					}
					zis.closeEntry();
					fos.close();
				}
			}
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		return null;
	}

}
