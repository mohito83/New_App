/**
 * 
 */

package edu.isi.backpack.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import org.toosheh.android.R;
import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author jenniferchen
 */
public class DeleteContentTask extends AsyncTask<Media.Item, Integer, Void> {

    private ContentListActivity parent;

    private File contentDirectory;

    private ProgressDialog dialog;

    private Media metadata;

    private File metaFile;

    private static final int STATUS_DELETE_CONTENT = 1000;

    private static final int STATUS_NEW_METADATA = 2000;

    public DeleteContentTask(ContentListActivity parent, File contentDir, ProgressDialog dialog,
            Media metadata, File metaFile) {
        this.parent = parent;
        contentDirectory = contentDir;
        this.dialog = dialog;
        this.metadata = metadata;
        this.metaFile = metaFile;
    }

    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Void doInBackground(Media.Item... entries) {
        publishProgress(STATUS_DELETE_CONTENT);
        int count = entries.length;
        ArrayList<String> deleted = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            Media.Item content = entries[i];

            // delete file
            File file = new File(contentDirectory, content.getFilename());
            String fileName = file.getName();

            // delete thumbnail
            File thumb = new File(contentDirectory, content.getThumbnail());
            thumb.delete();

            // delete from bookmark
            if (parent.isBookmarked(content.getFilename()))
                parent.removeBookmark(content.getFilename(), false);

            // delete assets
            // asset list
            if (content.getType() == Type.HTML) {
                String dirName = fileName.substring(0, fileName.indexOf(".htm"));
                File assetDir = new File(file.getParent(), dirName);
                if (assetDir.exists()) {
                    for (File f : assetDir.listFiles()) {
                        f.delete();
                    }
                    assetDir.delete();
                }
            }
            
            file.delete();

            deleted.add(content.getFilename());

        }

        // build new metadata
        // copy original video metadata except selected video
        publishProgress(STATUS_NEW_METADATA);
        Media.Builder newMedia = Media.newBuilder();
        for (Media.Item v : metadata.getItemsList()) {

            // if not selected video
            if (!deleted.contains(v.getFilename())) {
                newMedia.addItems(v);
            }
        }

        // write new meta out and update metadata in memory
        try {
            FileOutputStream out = new FileOutputStream(metaFile);
            newMedia.build().writeTo(out); // write out to file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == STATUS_DELETE_CONTENT)
            dialog.setMessage(parent.getString(R.string.deleting_files));
        else if (progress[0] == STATUS_NEW_METADATA)
            dialog.setMessage(parent.getString(R.string.updating_metadata));
    }

    protected void onPostExecute(Void v) {
        parent.reload(false);
        dialog.dismiss();
    }

}
