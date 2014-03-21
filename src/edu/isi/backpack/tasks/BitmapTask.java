
package edu.isi.backpack.tasks;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

/**
 * @author jenniferchen This task takes the given URI of an image and displays
 *         the bitmap on the given image view
 */
public class BitmapTask extends AsyncTask<Uri, Integer, Bitmap> {

    private ImageView imageView;

    private ContentResolver cr;

    private int width;

    private int height;

    private LruCache<String, Bitmap> imageCache;

    /**
     * @param imageView
     * @param cr
     * @param w - desired width
     * @param h - desired height
     * @param cache - image cache
     */
    public BitmapTask(ImageView imageView, ContentResolver cr, int w, int h,
            LruCache<String, Bitmap> cache) {
        this.imageView = imageView;
        this.cr = cr;
        width = w;
        height = h;
        this.imageCache = cache;
    }

    /**
     * Create a new task with only desired image height
     * 
     * @param imageView
     * @param cr
     * @param h - desired height
     * @return
     */
    public static BitmapTask BitmapTaskByHeight(ImageView imageView, ContentResolver cr, int h,
            LruCache<String, Bitmap> cache) {
        BitmapTask task = new BitmapTask(imageView, cr, -1, h, cache);
        return task;
    }

    @Override
    protected Bitmap doInBackground(Uri... uri) {

        try {

            // decode the image
            InputStream is = cr.openInputStream(uri[0]);
            Bitmap bm = BitmapFactory.decodeStream(is);
            if (bm == null) {
                // FIXME: Spundun: If we have more images in the folder and we
                // couldn't decode this image, then we should try the others.
                Log.e(this.getClass().getSimpleName(), "Couldn't decode the bitmap for file:"
                        + uri[0].toString());
                return null;
            }
            float sampleSize = getSampleSize(bm.getWidth(), bm.getHeight(), width, height);

            // resize
            Bitmap bitmap = Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() / sampleSize),
                    (int) (bm.getHeight() / sampleSize), false);
            imageCache.put(uri[0].toString(), bitmap);
            return bitmap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(Bitmap result) {
        // FYI, passing null into this call is ok.
        // It keeps the previous image that was set in this view.
        imageView.setImageBitmap(result);
    }

    /**
     * Given image size and desired size, calculate the ratio
     * 
     * @param imageW - image width
     * @param imageH - image height
     * @param reqW - desired width
     * @param reqH - desired height
     * @return
     */
    public static float getSampleSize(int imageW, int imageH, int reqW, int reqH) {
        float sampleSize = 1;

        if (reqW < 0 && imageH > reqH) { // sample by height
            sampleSize = (float) imageH / (float) reqH;
        } else if (imageH > reqH || imageW > reqW) {
            float hr = (float) imageH / (float) reqH;
            float wr = (float) imageW / (float) reqW;
            sampleSize = Math.max(hr, wr);
        }

        return sampleSize;
    }
}
