
package edu.isi.backpack.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import edu.isi.backpack.R;
import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.VideoProtos.Video;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author jenniferchen Handles the content list
 */
public class ContentListAdapter extends ArrayAdapter<Object> {

    private ContentListActivity context;

    private File contentDirectory;

    private SparseBooleanArray selected = new SparseBooleanArray();

    // TODO this map is never cleared except clear() is called
    // Need to manage this list to better manager memory
    private HashMap<String, String> thumbs = new HashMap<String, String>();

    private DisplayImageOptions imageOptions;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    private String defaultNewsThumb;

    public ContentListAdapter(ContentListActivity context, List<Object> objects, String directory) {
        super(context, R.layout.content_list_item, objects);

        this.context = context;
        contentDirectory = new File(directory);
        defaultNewsThumb = "drawable://" + R.drawable.news;
        imageOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.news)
                .cacheInMemory(true).cacheOnDisc(true).build();

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
            holder.publishedDate = (TextView) convertView.findViewById(R.id.contentPublishedDate);

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
            String uri = thumbs.get(id);
            if (uri == null) {
                String thumb = id + "_default.jpg";
                uri = Uri.fromFile(new File(contentDirectory, thumb)).toString();
                thumbs.put(id, uri);
            }

            String desc = video.getSnippet().getDescription();

            holder.titleView.setText(title);
            holder.catView.setText(cat);
            holder.descView.setText(check_for_www(desc));
            holder.playButtonView.setVisibility(View.VISIBLE);
            String date="";
            if(video.getSnippet() != null)
            	if(video.getSnippet().getPublishedAt() != null){
            		try {
            			date = video.getSnippet().getPublishedAt().substring(0, 10);
            		} catch (IndexOutOfBoundsException e) {
            		}
            	}
            holder.publishedDate.setText(date);
            
            imageLoader.displayImage(uri, holder.imageView, imageOptions);

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
            holder.titleView.setText(article.getTitle());
            holder.catView.setText(R.string.news); // TODO need category for
                                                   // articles
            holder.descView.setText(check_for_www(article.getDomain()));
            holder.playButtonView.setVisibility(View.GONE);
            String date="";
            if(article.getDatePublished() != null){
            	try {
                	date = article.getDatePublished().toString().substring(0, 4)
                			+"-"+article.getDatePublished().toString().substring(4, 6)
                			+"-"+article.getDatePublished().toString().substring(6, 8);
				} catch (IndexOutOfBoundsException e) {
				}
            }
            holder.publishedDate.setText(date);
            // TODO need thumbnail for articles
            // right now randomly pick one from image folder
            // try to find the image from cache first
            // Bitmap bitmap = null;
            final String name = article.getFilename();
            String uri = thumbs.get(name);
            if (uri == null) {
                String assetsPath = name.substring(0, name.indexOf(".htm"));
                File assetDir = new File(contentDirectory, assetsPath);
                if (assetDir.exists()) {
                    File[] files = assetDir.listFiles();
                    Arrays.sort(files);
                    for (File f : assetDir.listFiles()) {
                        String fileName = f.getName();
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".JPG")
                                || fileName.endsWith(".png") || fileName.endsWith(".PNG")) {
                            uri = Uri.fromFile(f).toString();
                            break;
                        }
                    }
                }

                if (uri == null || uri.contains("%2")) // TODO bug in content
                                                       // package, bad image
                                                       // name
                    uri = defaultNewsThumb;

                thumbs.put(name, uri);

            }
            imageLoader.displayImage(uri, holder.imageView, imageOptions);

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
    
    private String check_for_www(String desc){
    	if(desc.startsWith("www."))
    		desc = desc.replace("www.", "");
    	else if(desc.startsWith("WWW."))
    		desc = desc.replace("WWW.", "");
		return desc;
    }

    private static class ViewHolder {
        public ImageView imageView;

        public ImageView playButtonView;

        public TextView titleView;

        public TextView catView;

        public TextView descView;

        public ImageView starView;
        
        public TextView publishedDate;
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

    @Override
    public void clear() {
        super.clear();
        thumbs.clear();
    }
}
