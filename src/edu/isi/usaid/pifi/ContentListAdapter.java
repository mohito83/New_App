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
import edu.isi.usaid.pifi.VideosProtos.Video;

/**
 * 
 * @author jenniferchen
 * 
 * Handles the content list
 * 
 */
public class ContentListAdapter extends ArrayAdapter<Video> {
	
	private File contentDirectory;
	
	private final Context context;
	
	private final List<Video> values;
	
	private LruCache<String, Bitmap> bitmapCache;

	public ContentListAdapter(Context context, List<Video> objects, String directory) {
		super(context, R.layout.content_list_item, objects);
		this.context = context;
		this.values = objects;
		
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
		ImageView imageView;
		TextView titleView;
		TextView descView;
		
		// recycle views
		if (convertView == null){
			LayoutInflater inflater = (LayoutInflater) context
			        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.content_list_item, parent, false);
		}
		imageView = (ImageView)convertView.findViewById(R.id.contentThumb);
		titleView = (TextView)convertView.findViewById(R.id.contentTitle);
		descView = (TextView)convertView.findViewById(R.id.contentDesc);
		
		
		// TODO lazing loading
		Video video = values.get(pos);
		String title = video.getSnippet().getTitle();
		String id = video.getId();
		String thumb = id + "_default.jpg";
		String desc = video.getSnippet().getDescription();
		Uri uri = Uri.fromFile(new File(contentDirectory, thumb));
		titleView.setText(title);
		descView.setText(desc);
		
		// try to find the image from cache first
		Bitmap bitmap = getBitmapFromCache(uri.getPath());
		if (bitmap != null)
			imageView.setImageBitmap(bitmap);
		else {
			BitmapTask task = BitmapTask.BitmapTaskByHeight(imageView, getContext().getContentResolver(), 60, bitmapCache);
			task.execute(uri);
		}
		return convertView;
	}
	
}
