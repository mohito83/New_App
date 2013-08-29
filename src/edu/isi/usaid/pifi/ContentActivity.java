package edu.isi.usaid.pifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.isi.usaid.pifi.R;
import edu.isi.usaid.pifi.VideosProtos.Video;
import edu.isi.usaid.pifi.VideosProtos.Videos;

/**
 * 
 * @author jenniferchen
 * 
 * This is the main activity of the app. It shows a list of content from the content directory.
 * The activity reads the list of content from the metadata file, which uses protocol buffers.
 * It shows the content as a list.
 * 
 * User can select a content to view.
 * 
 */
public class ContentActivity extends Activity implements
ActionBar.OnNavigationListener {
	
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	private static final String contentDirName = "PifiContent";
	
	private static final String metaFileName = "videos.dat";
	
	private File contentDirectory;
	
	private File metaFile;
	
	private Videos metadata;
	
	private ListView contentList;
	
	private ArrayList<String> categories;
	
	private ContentListAdapter contentListAdapter;
	
	private String selectedCat = "All";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content);
		
		// content directory
		File sdDir = Environment.getExternalStorageDirectory();
		contentDirectory = new File(sdDir, contentDirName);
		if (!contentDirectory.exists())
			contentDirectory.mkdir();
		
		// read meatadata
		metaFile = new File(contentDirectory, metaFileName);
		if (!metaFile.exists()){ // TODO no metadata
		}
		try {
			
			metadata = Videos.parseFrom(new FileInputStream(metaFile));
			ArrayList<Video> videos = new ArrayList<Video>();
			videos.addAll(metadata.getVideoList());
			
			// TODO need a better way to get the list of categories
			categories = new ArrayList<String>();
			categories.add("All");
			for (Video video : videos){
				String id = video.getSnippet().getCategoryId();
				if (!categories.contains(id))
					categories.add(id);
			}
			String[] cats = new String[categories.size()];
			cats = categories.toArray(cats);
			
			// Set up the action bar to show a dropdown list for categories
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

			// Set up the dropdown list navigation in the action bar.
			actionBar.setListNavigationCallbacks(
			// Specify a SpinnerAdapter to populate the dropdown list.
					new ArrayAdapter<String>(this,
							android.R.layout.simple_list_item_1,
							android.R.id.text1, cats), this);
					
			
			// list of content
			contentList = (ListView)findViewById(R.id.listing);
			contentListAdapter = new ContentListAdapter(this, videos, contentDirectory.getAbsolutePath());
			contentList.setAdapter(contentListAdapter);
			contentList.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int pos, long id) {
					// play video when content selected
					Video video = contentListAdapter.getItem(pos);
					Intent i = new Intent(getApplicationContext(), VideoPlayerActivity.class);
					i.putExtra("video", contentDirectory + "/" + video.getFilename());
					i.putExtra("title", video.getSnippet().getTitle());
					i.putExtra("description", video.getSnippet().getDescription());
					startActivity(i);
					overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
				}
				
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.content, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int index, long itemId) {
		
		// user selected a different category
		String cat = categories.get(index);
		if (cat != selectedCat){
			contentListAdapter.clear();
			if (cat.equals("All"))
				contentListAdapter.addAll(metadata.getVideoList());
			else {
				for (Video v : metadata.getVideoList()){
					if (v.getSnippet().getCategoryId().equals(cat))
					contentListAdapter.add(v);
				}
			}
			// update list
			contentListAdapter.notifyDataSetChanged();
			selectedCat = cat;
		}
		return true;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}
}
