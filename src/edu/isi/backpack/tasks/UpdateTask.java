/**
 * 
 */

package edu.isi.backpack.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import edu.isi.backpack.activities.ContentListActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author jenniferchen
 */
public class UpdateTask extends AsyncTask<Void, Integer, Void> {

    private ContentListActivity parent;

    private File oldDir;

    private File newDir;

    private ProgressDialog progressBar;

    public UpdateTask(ContentListActivity parent, File oldDir, File newDir, ProgressDialog progress) {
        this.parent = parent;
        this.oldDir = oldDir;
        this.newDir = newDir;
        this.progressBar = progress;
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Void doInBackground(Void... params) {
        try {
            FileUtils.copyDirectory(oldDir, newDir);
            FileUtils.deleteDirectory(oldDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(Void v) {
        parent.reload(true);
        progressBar.dismiss();
    }

}
