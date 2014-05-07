/**
 * 
 */

package edu.isi.backpack.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author jenniferchen
 */
public class DeleteAllContentTask extends AsyncTask<Void, Integer, Void> {

    private ContentListActivity parent;

    private File contentDirectory;

    private ProgressDialog dialog;

    private Media metadata;

    private File metaFile;

    public DeleteAllContentTask(ContentListActivity parent, File contentDir, ProgressDialog dialog,
            Media metadata, File metaFile) {
        this.parent = parent;
        contentDirectory = contentDir;
        this.dialog = dialog;
        this.metadata = metadata;
        this.metaFile = metaFile;

        int total = metadata.getItemsCount();
        dialog.setMax(total);
        publishProgress(0);
    }

    @Override
    protected Void doInBackground(Void... v) {

        int count = 0;

        for (Media.Item item : metadata.getItemsList()) {

            count++;
            publishProgress(count);

            // delete file
            File file = new File(contentDirectory, item.getFilename());
            String filename = file.getName();

            // delete thumbnail
            File thumb = new File(contentDirectory, item.getThumbnail());
            thumb.delete();

            // delete from bookmark
            if (parent.isBookmarked(item.getFilename()))
                parent.removeBookmark(item.getFilename(), false);

            // delete assets
            if (item.getType() == Type.HTML) {
                String dirName = filename.substring(0, filename.indexOf(".htm"));
                File assetDir = new File(file.getParent(), dirName);
                if (assetDir.exists()) {
                    for (File f : assetDir.listFiles()) {
                        f.delete();
                    }
                    assetDir.delete();
                }
            }

            file.delete();

        }

        // build new empty metadata
        Media.Builder newMedia = Media.newBuilder();
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
        dialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Void v) {
        parent.reload(false);
        dialog.dismiss();
    }

}
