package edu.isi.usaid.pifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.CommentProtos.Comment;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;

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
 * TODO articles have no categories right now
 * 
 */
public class ContentListActivity extends Activity {
	
	public static final String STATE_SELECTED_DRAWER_ITEMS = "selected_drawer_items";
	
	public static final String VIDEO_CONTENT = "Video";
	
	public static final String WEB_CONTENT = "Web";
	
	private DrawerLayout drawerLayout;

	private ListView drawerList;

    private ActionBarDrawerToggle drawerToggle;

    private ArrayList<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
    
    private DrawerListAdapter drawerListAdapter;
	
	private File contentDirectory;
	
	private File metaFile;
	
	private File webMetaFile;
	
	private Videos metadata;
	
	private Articles webMetadata;
	
	private ListView contentList;
	
	private ArrayList<String> categories;
	
	private ArrayList<Object> contentItems = new ArrayList<Object>();
	
	private ContentListAdapter contentListAdapter;
	
	private String selectedCat = "All";
	
	private String selectedType = "All";
	
	private Object currentContent = null;
	
	private BroadcastReceiver broadcastReceiver;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content);
		
		// content directory
		File sdDir = Environment.getExternalStorageDirectory();
		contentDirectory = new File(sdDir, Constants.contentDirName);
		if (!contentDirectory.exists())
			contentDirectory.mkdir();
		
		// Set up the action bar to show a dropdown list for categories
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	    actionBar.setHomeButtonEnabled(true);
	    
	    drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		contentList = (ListView)findViewById(R.id.listing);

		// reload content
		try {
			reload();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	    // setup menu drawer
		drawerToggle = new ActionBarDrawerToggle(
				this,
				drawerLayout,
				R.drawable.ic_drawer,
				R.string.open_drawer,
				R.string.close_drawer){
			public void onDrawerClosed(View view) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
		
		};
		drawerLayout.setDrawerListener(drawerToggle);		
		
		// menu drawer listener
		drawerList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int pos, long id) {
				drawerList.setItemChecked(pos, true);
				DrawerItem selItem = drawerItems.get(pos);
				int type = selItem.getType();
				for (DrawerItem item : drawerItems){
					if (item != selItem && item.getType() == type){ // clear other checks of same type
						item.setChecked(false);
					}
				}
				selItem.setChecked(true);
				applyListFilter(selItem);
			    drawerLayout.closeDrawer(drawerList);
			}
			
		});
		
			
		// content list listener
		contentList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int pos, long id) {
				
				currentContent = contentListAdapter.getItem(pos);
				Intent intent = new Intent(getApplicationContext(), ContentViewerActivity.class);
				
				// if selected a video
				if (currentContent instanceof Video){
					intent.putExtra(ExtraConstants.TYPE, ExtraConstants.TYPE_VIDEO);
					intent.putExtra(ExtraConstants.CONTENT, ((Video)currentContent).toByteArray());
				}
				else if (currentContent instanceof Article){
					intent.putExtra(ExtraConstants.TYPE, ExtraConstants.TYPE_ARTICLE);
					intent.putExtra("content", ((Article)currentContent).toByteArray());
				}
				
				startActivity(intent);
				overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.content, menu);
		return true;
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }
 
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()){
    	case R.id.action_refresh:
    		try {
				reload();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		break;
    	default:
    		break;
    	}
    		
    	return true;
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		SparseBooleanArray array = drawerList.getCheckedItemPositions();
		ArrayList<Integer> sel = new ArrayList<Integer>();
		for (int i = 0; i < array.size(); i++){
			if (array.get(i))
				sel.add(i);
		}
		outState.putIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS, sel);
	}
    
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously state
		if (savedInstanceState.containsKey(STATE_SELECTED_DRAWER_ITEMS)) {
			for (Integer i : savedInstanceState.getIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS)){
				drawerList.setItemChecked(i, true);
				drawerItems.get(i).setChecked(true);
			}
		}
	}
	
	@Override
	public boolean onNavigateUp(){
		if (!drawerLayout.isDrawerOpen(Gravity.LEFT))
			drawerLayout.openDrawer(Gravity.LEFT);
		else
			drawerLayout.closeDrawer(Gravity.LEFT);
		return true;
	}
	
	
	@Override
	protected void onResume(){
		super.onResume();
		
		// register to receive message when a new comment is added
		broadcastReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context c, Intent i) {
				if (i.getAction().equals(Constants.NEW_COMMENT_ACTION)){ // adding new comment
					String user = i.getStringExtra(ExtraConstants.USER);
					String date = i.getStringExtra(ExtraConstants.DATE);
					String comment = i.getStringExtra(ExtraConstants.USER_COMMENT);
					addComment(user, date, comment);
				}
				if (i.getAction().equals(Constants.META_UPDATED_ACTION)){ // meta file updated
					try {
						reload();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.NEW_COMMENT_ACTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.META_UPDATED_ACTION));
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		if (broadcastReceiver != null){
			LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
			
	}
	
	/**
	 * reload metadata and refresh the list
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void reload() throws FileNotFoundException, IOException{
		
		// read meatadata
		metaFile = new File(contentDirectory, Constants.metaFileName);
		webMetaFile = new File(contentDirectory, Constants.webMetaFileName);
		ArrayList<Article> articles = new ArrayList<Article>();
		ArrayList<Video> videos = new ArrayList<Video>();
			
		// content objects from metadata
		if (webMetaFile.exists())
			webMetadata = Articles.parseFrom(new FileInputStream(webMetaFile));
		else 
			webMetadata = Articles.newBuilder().build();
		articles.addAll(webMetadata.getArticleList());
		
		if (metaFile.exists())
			metadata = Videos.parseFrom(new FileInputStream(metaFile));
		else
			metadata = Videos.newBuilder().build();
		videos.addAll(metadata.getVideoList());
			
		contentItems.clear();
		contentItems.addAll(videos);
		contentItems.addAll(articles);
			
		// get a list of categories
		// TODO need a better way to get the list of categories
		categories = new ArrayList<String>();
		for (Video video : videos){
			String id = video.getSnippet().getCategoryId();
			if (!categories.contains(id))
				categories.add(id);
		}
		String[] cats = new String[categories.size()];
		cats = categories.toArray(cats);
			
		// setup menu drawer
        drawerItems.clear();
		drawerItems.add(new DrawerItem("Content Type", DrawerItem.HEADER, false));
		drawerItems.add(new DrawerItem("All", DrawerItem.CONTENT_TYPE, true));
		drawerItems.add(new DrawerItem(VIDEO_CONTENT, DrawerItem.CONTENT_TYPE, false));
		drawerItems.add(new DrawerItem(WEB_CONTENT, DrawerItem.CONTENT_TYPE, false));
		drawerItems.add(new DrawerItem("Categories", DrawerItem.HEADER, false));
		drawerItems.add(new DrawerItem("All", DrawerItem.CATEGORY, true));
		for (String cat : cats){
			drawerItems.add(new DrawerItem(cat, DrawerItem.CATEGORY, false));
		}
			
			
			
		// update/init adapter
		if (drawerListAdapter == null){ // first time
			drawerListAdapter = new DrawerListAdapter(this, drawerItems); 
			drawerList.setAdapter(drawerListAdapter);
		}
		else
			drawerListAdapter.notifyDataSetChanged();
		
		if (contentListAdapter == null){
			contentListAdapter = new ContentListAdapter(this, contentItems, contentDirectory.getAbsolutePath());
			contentList.setAdapter(contentListAdapter);
		}
		else
			contentListAdapter.notifyDataSetChanged();
				
	}
	
	private void applyListFilter(DrawerItem item){
		if (item.getType() == DrawerItem.HEADER)
			return;
		
		if (item.getType() == DrawerItem.CONTENT_TYPE){
			 String newType = item.getLabel();
			 if (newType != selectedType){
				 selectedType = newType;
			 }
			 else 
				 return;
		}
		else if (item.getType() == DrawerItem.CATEGORY){
			String newCat = item.getLabel();
			if (newCat != selectedCat){
				selectedCat = newCat;
			}
			else
				return;
		}
			
		contentListAdapter.clear();
		
		// TODO nothing being taken care of for web category
		if (selectedType.equals("All")){
			if (selectedCat.equals("All")){
				contentListAdapter.addAll(metadata.getVideoList());
				contentListAdapter.addAll(webMetadata.getArticleList());
			}
			else {
				for (Video v : metadata.getVideoList()){
					if (v.getSnippet().getCategoryId().equals(selectedCat))
					contentListAdapter.add(v);
				}
			}
		}
		else if (selectedType.equals(VIDEO_CONTENT)){
			if (selectedCat.equals("All"))
				contentListAdapter.addAll(metadata.getVideoList());
			else {
				for (Video v : metadata.getVideoList()){
					if (v.getSnippet().getCategoryId().equals(selectedCat))
						contentListAdapter.add(v);
				}
			}
		}
		else if (selectedType.equals(WEB_CONTENT)){
			if (selectedCat.equals("All"))
				contentListAdapter.addAll(webMetadata.getArticleList());
		}
		
		
		// update list
		contentListAdapter.notifyDataSetChanged();
	}
	
	/**
	 * add new user comment to metadata
	 * @param comment
	 */
	private void addComment(final String user, final String date, final String comment){
		if (currentContent == null || comment == null || comment.isEmpty())
			return;
		
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				
				// Add comments to metadata

				if (currentContent instanceof Video){
					Videos.Builder newVideos = Videos.newBuilder();
					for (Video video : metadata.getVideoList()){
						
						// copy from original
						Video.Builder videoBuilder = Video.newBuilder();
						videoBuilder.mergeFrom(video);
						
						// found the video to add comment
						if (video.getId().equals(((Video)currentContent).getId())){ 
							
							// create new comment
							Comment.Builder commentBuilder = Comment.newBuilder();
							commentBuilder.setUser(user);
							commentBuilder.setDate(date);
							commentBuilder.setText(comment);
							
							// modify the video snippet by adding comment
							videoBuilder.addComments(commentBuilder);
						}
						
						newVideos.addVideo(videoBuilder);
					}
					
					// write new meta out and update metadata in memory
					try {
						FileOutputStream out = new FileOutputStream(metaFile);
						newVideos.build().writeTo(out); // write out to file
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					
				}
				else if (currentContent instanceof Article){
					Articles.Builder newArticles = Articles.newBuilder();
					for (Article article : webMetadata.getArticleList()){
						
						// copy data from original
						Article.Builder articleBuilder = Article.newBuilder();
						articleBuilder.mergeFrom(article);
						
						// found the article to add comment
						if (article.getUrl().equals(((Article)currentContent).getUrl())){ 
							
							// create new comment
							Comment.Builder commentBuilder = Comment.newBuilder();
							commentBuilder.setUser(user);
							commentBuilder.setDate(date);
							commentBuilder.setText(comment);
							
							// modify the article by adding comment
							articleBuilder.addComments(commentBuilder);
						}
						
						newArticles.addArticle(articleBuilder);
					}
					
					// write new meta out and update metadata in memory
					try {
						FileOutputStream out = new FileOutputStream(webMetaFile);
						newArticles.build().writeTo(out); // write out to file
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// broadcast metadata updated
				Intent i = new Intent();
				i.setAction(Constants.META_UPDATED_ACTION);
				LocalBroadcastManager.getInstance(ContentListActivity.this).sendBroadcast(i);
			}
			
		});
		
		t.start();
	}
}
