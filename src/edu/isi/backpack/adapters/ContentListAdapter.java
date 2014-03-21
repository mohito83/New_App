
package edu.isi.backpack.adapters;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.isi.backpack.R;
import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.VideoProtos.Video;
import edu.isi.backpack.tasks.BitmapTask;

/**
 * @author jenniferchen Handles the content list
 */
public class ContentListAdapter extends ArrayAdapter<Object> {

    private ContentListActivity context;

    private File contentDirectory;

    private LruCache<String, Bitmap> bitmapCache;

    private SparseBooleanArray selected = new SparseBooleanArray();

    private Bitmap defaultBitmap;

    public ContentListAdapter(ContentListActivity context, List<Object> objects, String directory) {
        super(context, R.layout.content_list_item, objects);

        this.context = context;
        contentDirectory = new File(directory);

        // create a cache for bitmaps
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // kb
        final int cacheSize = maxMemory / 8;
        bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // default bitmap
        defaultBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.news);
    }

    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (bitmapCache.get(key) == null)
            bitmapCache.put(key, bitmap);
    }

    public Bitmap getBitmapFromCache(String key) {
        return bitmapCache.get(key);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        // recycle views
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.content_list_item, parent, false);

            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.contentThumb);
            holder.playButtonView = (ImageView) convertView.findViewById(R.id.playButton);
            holder.titleView = (TextView) convertView.findViewById(R.id.contentTitle);
            holder.catView = (TextView) convertView.findViewById(R.id.contentCatagory);
            holder.descView = (TextView) convertView.findViewById(R.id.contentDesc);
            holder.starView = (ImageView) convertView.findViewById(R.id.star);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Object content = getItem(pos);
        if (content instanceof Video) {
            final Video video = (Video) content;
            String title = video.getSnippet().getTitle();
            String id = video.getId();
            String cat = video.getSnippet().getCategoryId();
            String thumb = id + "_default.jpg";
            String desc = video.getSnippet().getDescription();
            Uri uri = Uri.fromFile(new File(contentDirectory, thumb));
            holder.titleView.setText(title);
            holder.catView.setText(cat);
            holder.descView.setText(desc);
            holder.playButtonView.setVisibility(View.VISIBLE);

            // try to find the image from cache first
            Bitmap bitmap = getBitmapFromCache(uri.toString());
            if (bitmap != null)
                holder.imageView.setImageBitmap(bitmap);
            else {
                BitmapTask task = BitmapTask.BitmapTaskByHeight(holder.imageView, getContext()
                        .getContentResolver(), holder.imageView.getLayoutParams().height,
                        bitmapCache);
                task.execute(uri);
            }

            // bookmark
            if (context.isBookmarked(video.getFilepath()))
                holder.starView.setImageResource(R.drawable.ic_fav_selected);
            else
                holder.starView.setImageResource(R.drawable.ic_fav_unselected);

            holder.starView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (context.isBookmarked(video.getFilepath())) {
                        context.removeBookmark(video.getFilepath(), true);
                        holder.starView.setImageResource(R.drawable.ic_fav_unselected);
                    } else {
                        context.addBookmark(video.getFilepath(), true);
                        holder.starView.setImageResource(R.drawable.ic_fav_selected);
                    }
                }

            });

        } else if (content instanceof Article) {
            final Article article = (Article) content;
            String title = article.getTitle();
            holder.titleView.setText(title);
            holder.catView.setText(R.string.news); // TODO need category for
                                                   // articles
            holder.descView.setText(article.getDomain());
            holder.playButtonView.setVisibility(View.GONE);

            // TODO need thumbnail for articles
            // right now randomly pick one from image folder
            // try to find the image from cache first
            Bitmap bitmap = null;
            String name = article.getFilename();
            String assetsPath = name.substring(0, name.indexOf(".htm"));
            File assetDir = new File(contentDirectory, assetsPath);
            if (assetDir.exists()) {
                File[] files = assetDir.listFiles();
                Arrays.sort(files);
                for (File f : assetDir.listFiles()) {
                    String fileName = f.getName();
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".JPG")
                            || fileName.endsWith(".png") || fileName.endsWith(".PNG")) {
                        Uri uri = Uri.fromFile(f);
                        bitmap = getBitmapFromCache(uri.toString());
                        if (bitmap == null) {
                            BitmapTask task = new BitmapTask(holder.imageView, getContext()
                                    .getContentResolver(),
                                    holder.imageView.getLayoutParams().width,
                                    holder.imageView.getLayoutParams().height, bitmapCache);
                            task.execute(uri);
                        }
                        break;
                    }
                }
            }

            if (bitmap == null)
                bitmap = defaultBitmap;
            holder.imageView.setImageBitmap(bitmap);

            // bookmark
            if (context.isBookmarked(article.getFilename()))
                holder.starView.setImageResource(R.drawable.ic_fav_selected);
            else
                holder.starView.setImageResource(R.drawable.ic_fav_unselected);

            holder.starView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (context.isBookmarked(article.getFilename())) {
                        context.removeBookmark(article.getFilename(), true);
                        holder.starView.setImageResource(R.drawable.ic_fav_unselected);
                    } else {
                        context.addBookmark(article.getFilename(), true);
                        holder.starView.setImageResource(R.drawable.ic_fav_selected);
                    }
                }

            });
        }

        // this will cause the selected item to be highlighted
        if (selected.get(pos))
            convertView.setActivated(true);
        else
            convertView.setActivated(false);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView imageView;

        public ImageView playButtonView;

        public TextView titleView;

        public TextView catView;

        public TextView descView;

        public ImageView starView;
    }

    public boolean toggleSelection(int pos) {
        boolean isSelected = selected.get(pos);
        if (!isSelected) { // not selected, toggle to selected
            selected.put(pos, true);
            notifyDataSetChanged();
            return true;
        } else { // is selected, toggle to not selected
            selected.delete(pos);
            notifyDataSetChanged();
            return false;
        }
    }

    public void removeSelections() {
        selected = new SparseBooleanArray();
        notifyDataSetChanged();
    }

}
