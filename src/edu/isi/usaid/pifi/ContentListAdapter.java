package edu.isi.usaid.pifi;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.isi.usaid.pifi.R;
import edu.isi.usaid.pifi.VideosProtos.Video;

public class ContentListAdapter extends ArrayAdapter<Video> {
	
	private File contentDirectory;
	
	private final Context context;
	
	private final List<Video> values;

	public ContentListAdapter(Context context, List<Video> objects, String directory) {
		super(context, R.layout.content_list_item, objects);
		this.context = context;
		this.values = objects;
		
		contentDirectory = new File(directory);
	}
	
	
	public View getView(int pos, View convertView, ViewGroup parent){
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.content_list_item, parent, false);
		ImageView imageView = (ImageView)rowView.findViewById(R.id.contentThumb);
		TextView titleView = (TextView)rowView.findViewById(R.id.contentTitle);
		TextView descView = (TextView)rowView.findViewById(R.id.contentDesc);
		
		// thumbnail
		// TODO cache image 
		// TODO more efficient list loading: cache image, lazing loading, recycle views
		Video video = values.get(pos);
		String title = video.getSnippet().getTitle();
		String id = video.getId();
		String thumb = id + "_default.jpg";
		String desc = video.getSnippet().getDescription();
		
		// title
		titleView.setText(title);
		imageView.setImageURI(Uri.fromFile(new File(contentDirectory, thumb)));
		descView.setText(desc);
		return rowView;
	}
	
}
