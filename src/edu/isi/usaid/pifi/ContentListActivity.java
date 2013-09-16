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
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
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
public class ContentListActivity extends Activity implements
ActionBar.OnNavigationListener {
	
	private DrawerLayout drawerLayout;

	private ListView drawerList;

    private ActionBarDrawerToggle drawerToggle;

    private String[] drawerItems = new String[]{"Videos", "Web"};
	
	private File contentDirectory;
	
	private File metaFile;
	
	private File webMetaFile;
	
	private Videos metadata;
	
	private Articles webMetadata;
	
	private ListView contentList;
	
	private ArrayList<String> categories;
	
	private ContentListAdapter contentListAdapter;
	
	private String selectedCat = "All";
	
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
		
		// read meatadata
		metaFile = new File(contentDirectory, Constants.metaFileName);
		webMetaFile = new File(contentDirectory, Constants.webMetaFileName);
		if (!metaFile.exists()){ // TODO no metadata
		}
		try {
			
			webMetadata = Articles.parseFrom(new FileInputStream(webMetaFile));
			ArrayList<Article> articles = new ArrayList<Article>();
			articles.addAll(webMetadata.getArticleList());
			
			metadata = Videos.parseFrom(new FileInputStream(metaFile));
			ArrayList<Video> videos = new ArrayList<Video>();
			videos.addAll(metadata.getVideoList());
			
			
			ArrayList<Object> allContents = new ArrayList<Object>();
			allContents.addAll(videos);
			allContents.addAll(articles);
			
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
			
			// setup menu drawer
			actionBar.setDisplayHomeAsUpEnabled(true);
	        actionBar.setHomeButtonEnabled(true);
			drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			drawerList = (ListView) findViewById(R.id.left_drawer);
			drawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, drawerItems));
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
			
			
			// list of content
			contentList = (ListView)findViewById(R.id.listing);
			contentListAdapter = new ContentListAdapter(this, allContents, contentDirectory.getAbsolutePath());
			contentList.setAdapter(contentListAdapter);
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
			if (cat.equals("All")){
				contentListAdapter.addAll(metadata.getVideoList());
				contentListAdapter.addAll(webMetadata.getArticleList());
			}
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
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(Constants.STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(Constants.STATE_SELECTED_NAVIGATION_ITEM));
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
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(Constants.STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
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
					
					// update adapter's list data
					ArrayList<Article> articles = new ArrayList<Article>();
					articles.addAll(webMetadata.getArticleList());
					
					ArrayList<Video> videos = new ArrayList<Video>();
					videos.addAll(metadata.getVideoList());
					
					ArrayList<Object> allContents = new ArrayList<Object>();
					allContents.addAll(videos);
					allContents.addAll(articles);
					
					contentListAdapter.clear();
					contentListAdapter.addAll(allContents);
					contentListAdapter.notifyDataSetChanged();
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
						metadata = newVideos.build(); // update metadata in memory
						metadata.writeTo(out); // write out to file
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
						webMetadata = newArticles.build(); // update metadata in memory
						webMetadata.writeTo(out); // write out to file
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
