
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

import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;

import org.toosheh.android.R;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author jenniferchen Handles the content list
 */
public class ContentListAdapter extends ArrayAdapter<Media.Item> {

    private ContentListActivity context;

    private File contentDirectory;

    private SparseBooleanArray selected = new SparseBooleanArray();

    private DisplayImageOptions imageOptions;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    private String defaultNewsThumb;

    public ContentListAdapter(ContentListActivity context, List<Media.Item> objects,
            String directory) {
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
            // holder.catView = (TextView)
            // convertView.findViewById(R.id.contentCatagory);
            holder.descView = (TextView) convertView.findViewById(R.id.contentDesc);
            holder.starView = (ImageView) convertView.findViewById(R.id.star);
            holder.publishedDate = (TextView) convertView.findViewById(R.id.contentPublishedDate);
            /*holder.thumbsUp = (ImageView) convertView.findViewById(R.id.rate_up);
            holder.thumbsDown = (ImageView) convertView.findViewById(R.id.rate_down);*/

            // TODO populate actual vote count based on meta data info
            holder.positiveVotes = (TextView) convertView.findViewById(R.id.positive_vote_count);
            holder.positiveVotes.setText("112");
            holder.negativeVotes = (TextView) convertView.findViewById(R.id.negative_vote_count);
            holder.negativeVotes.setText("17");

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Media.Item content = getItem(pos);
        Type type = content.getType();
        String thumb = content.getThumbnail();

        holder.titleView.setText(content.getTitle());
        String cat = "";
        for (String c : content.getCategoriesList()) {
            cat = cat + c + " ";
        }
        // holder.catView.setText(cat);
        if (type == Type.VIDEO)
            holder.descView.setText(content.getDescription());
        else
            holder.descView.setText(content.getSource());

        SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
        SimpleDateFormat f2 = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat f3 = new SimpleDateFormat("yyyy-MM-dd");
        String date = content.getPubDate();
        try {
            Date d = f1.parse(date);
            date = f3.format(d);
        } catch (ParseException e) {
            try {
                Date d = f2.parse(date);
                date = f3.format(d);
            } catch (ParseException e1) {
            }
        }
        holder.publishedDate.setText(date);

        String uri;
        if (thumb == null || thumb.isEmpty())
            uri = defaultNewsThumb;
        else
            uri = Uri.fromFile(new File(contentDirectory, thumb)).toString();
        imageLoader.displayImage(uri, holder.imageView, imageOptions);
        if (type == Type.VIDEO)
            holder.playButtonView.setVisibility(View.VISIBLE);
        else
            holder.playButtonView.setVisibility(View.GONE);

        // bookmark
        if (context.isBookmarked(content.getFilename()))
            holder.starView.setImageResource(R.drawable.ic_fav_selected);
        else
            holder.starView.setImageResource(R.drawable.ic_fav_unselected);

        holder.starView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (context.isBookmarked(content.getFilename())) {
                    context.removeBookmark(content.getFilename(), true);
                    holder.starView.setImageResource(R.drawable.ic_fav_unselected);
                } else {
                    context.addBookmark(content.getFilename(), true);
                    holder.starView.setImageResource(R.drawable.ic_fav_selected);
                }
            }

        });

       /* // adding on click listener to the vote count
        holder.thumbsUp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = holder.votes.getText().toString();
                int i = 0;
                i = Integer.parseInt(text);
                i++;
                holder.votes.setText(Integer.toString(i));
            }
        });

        holder.thumbsDown.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = holder.votes.getText().toString();
                int i = 0;
                i = Integer.parseInt(text);
                i--;
                holder.votes.setText(Integer.toString(i));
            }
        });*/

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

        public TextView publishedDate;

        public ImageView thumbsUp;

        public ImageView thumbsDown;

        public TextView positiveVotes;
        
        public TextView negativeVotes;
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
