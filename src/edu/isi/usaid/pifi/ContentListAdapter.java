package edu.isi.usaid.pifi;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * 
 * @author jenniferchen
 * 
 * Handles the content list
 * 
 */
public class ContentListAdapter extends ArrayAdapter<Object> {
	
	private File contentDirectory;
	
	private final Context context;
	
	private LruCache<String, Bitmap> bitmapCache;

	public ContentListAdapter(Context context, List<Object> objects, String directory) {
		super(context, R.layout.content_list_item, objects);
		this.context = context;
		
		contentDirectory = new File(directory);
		
		// create a cache for bitmaps
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // kb
		final int cacheSize = maxMemory / 8;
		bitmapCache = new LruCache<String, Bitmap>(cacheSize){
			@Override
			protected int sizeOf(String key, Bitmap bitmap){
				return bitmap.getByteCount() / 1024; 
			}
		};
	}
	
	public void addBitmapToCache(String key, Bitmap bitmap){
		if (bitmapCache.get(key) == null)
			bitmapCache.put(key, bitmap);
	}
	
	public Bitmap getBitmapFromCache(String key){
		return bitmapCache.get(key);
	}
	
	@Override
	public View getView(int pos, View convertView, ViewGroup parent){

		ViewHolder holder;
		
		// recycle views
		if (convertView == null){
			LayoutInflater inflater = (LayoutInflater) context
			        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.content_list_item, parent, false);
			
			holder = new ViewHolder();
			holder.imageView = (ImageView)convertView.findViewById(R.id.contentThumb);
			holder.titleView = (TextView)convertView.findViewById(R.id.contentTitle);
			holder.catView = (TextView)convertView.findViewById(R.id.contentCatagory);
			holder.descView = (TextView)convertView.findViewById(R.id.contentDesc);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		
		Object content = getItem(pos);
		if (content instanceof Video){
			Video video = (Video)content;
			String title = video.getSnippet().getTitle();
			String id = video.getId();
			String cat = video.getSnippet().getCategoryId();
			String thumb = id + "_default.jpg";
			String desc = video.getSnippet().getDescription();
			Uri uri = Uri.fromFile(new File(contentDirectory, thumb));
			holder.titleView.setText(title);
			holder.catView.setText(cat);
			holder.descView.setText(desc);
			
			// try to find the image from cache first
			Bitmap bitmap = getBitmapFromCache(uri.getPath());
			if (bitmap != null)
				holder.imageView.setImageBitmap(bitmap);
			else {
				BitmapTask task = BitmapTask.BitmapTaskByHeight(holder.imageView, getContext().getContentResolver(), holder.imageView.getLayoutParams().height, bitmapCache);
				task.execute(uri);
			}
		}
		else if (content instanceof Article){
			Article article = (Article)content;
			String title = article.getTitle();
			holder.titleView.setText(title);
			holder.catView.setText("news"); // TODO need category for articles
			holder.descView.setText("");
			holder.imageView.setImageBitmap(null);
		}
		return convertView;
	}
	
	private static class ViewHolder {
		public ImageView imageView;
		public TextView titleView;
		public TextView catView;
		public TextView descView;
	}
	
}
