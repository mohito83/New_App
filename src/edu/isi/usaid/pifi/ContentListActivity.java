package edu.isi.usaid.pifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;
import edu.isi.usaid.pifi.data.ContentListAdapter;
import edu.isi.usaid.pifi.data.DrawerItem;
import edu.isi.usaid.pifi.data.DrawerListAdapter;
import edu.isi.usaid.pifi.dialogs.BluetoothListDialog;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.CommentProtos.Comment;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;
import edu.isi.usaid.pifi.services.ConnectionService;
import edu.isi.usaid.pifi.services.ListenerService;
import edu.isi.usaid.pifi.tasks.DeleteContentTask;
import edu.isi.usaid.pifi.tasks.DownloadTask;

/**
 * 
 * @author jenniferchen
 * 
 *         This is the main activity of the app. It shows a list of content from
 *         the content directory. The activity reads the list of content from
 *         the metadata file, which uses protocol buffers. It shows the content
 *         as a list.
 * 
 *         User can select a content to view.
 * 
 *         TODO need categories for web articles
 * 
 */
public class ContentListActivity extends Activity implements BookmarkManager{
	
	public static final String STATE_SELECTED_DRAWER_ITEMS = "selected_drawer_items";

	public static final String VIDEO_CONTENT = "Video";

	public static final String WEB_CONTENT = "Web";
	
	public static final String STARRED_BOOKMARK = "Starred";
	
	private static final String SETTING_BOOKMARKS = "bookmarks";
	
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
	
	private String selectedBookmark = "All";
	
	private Object currentContent = null;

	// register to receive message when a new comment is added
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context c, Intent i) {
			if (i.getAction().equals(Constants.NEW_COMMENT_ACTION)) { // adding
																		// new
																		// comment
				String user = i.getStringExtra(ExtraConstants.USER);
				String date = i.getStringExtra(ExtraConstants.DATE);
				String comment = i.getStringExtra(ExtraConstants.USER_COMMENT);
				addComment(user, date, comment);
			} else if (i.getAction().equals(Constants.META_UPDATED_ACTION)) { // meta
			
				reload(false);

			} else if (i.getAction().equals(Constants.BT_STATUS_ACTION)) {
				/*
				 * if (btStatusDialog == null){ btStatusDialog = new
				 * AlertDialog.Builder(ContentListActivity.this).create();
				 * btStatusDialog.setCancelable(false);
				 * btStatusDialog.setButton(DialogInterface.BUTTON_POSITIVE,
				 * "OK", new OnClickListener() { public void
				 * onClick(DialogInterface dialog, int which) {
				 * dialog.dismiss(); } }); }
				 * btStatusDialog.setMessage(i.getStringExtra
				 * (ExtraConstants.STATUS)); if (!btStatusDialog.isShowing())
				 * btStatusDialog.show();
				 */
				Toast toast = Toast.makeText(ContentListActivity.this,
						i.getStringExtra(ExtraConstants.STATUS),
						Toast.LENGTH_LONG);
				toast.show();
			}
			else if (i.getAction().equals(Constants.BOOKMARK_ACTION)){
				String id = i.getStringExtra(ExtraConstants.ID);
				boolean on = i.getBooleanExtra(ExtraConstants.ON, false);
				if (on)
					addBookmark(id);
				else
					removeBookmark(id);
				contentListAdapter.notifyDataSetChanged();
			}
			
		}

	};

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getName().isEmpty()) {
					Log.i("Empty device", device.getAddress());
					return;
				}
				bts.add(device);
				dialog.redraw(bts);

				// }
				// When discovery is finished.
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {

				// setProgressBarIndeterminateVisibility(false);
				/*
				 * setTitle(R.string.select_device); if
				 * (mNewDevicesArrayAdapter.getCount() == 0) { String noDevices
				 * = getResources().getText(R.string.none_found).toString();
				 * mNewDevicesArrayAdapter.add(noDevices); }
				 */
			}
		}
	};

	private ArrayList<BluetoothDevice> bts;

	private static final int REQUEST_ENABLE_BT = 3;

	private BluetoothAdapter mBluetoothAdapter = null;

	private BluetoothListDialog dialog;
	
	private SharedPreferences settings;
	
	private Set<String> bookmarks;
	
	private Object rowActionMode = null;
	
	private ArrayList<Object> selectedRowItems = new ArrayList<Object>(); 
	
	private ActionMode.Callback rowActionCallback = new ActionMode.Callback() {
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			rowActionMode = null;
			selectedRowItems.clear();
			contentListAdapter.removeSelections();
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inf = mode.getMenuInflater();
			inf.inflate(R.menu.row_selection, menu);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()){
			
			/* delete content
			 * 1. video/article file
			 * 2. thumbnail
			 * 3. bookmark
			 * 4. metadata entry
			 */
			case R.id.action_row_delete:
				// TODO need to make sure not doing sync at the same time
				
				
				final ProgressDialog progress = new ProgressDialog(ContentListActivity.this);
				progress.setTitle("Deletion in Progress");
				progress.setMessage("Deleting...");
				progress.setCancelable(false);
				progress.show();
				DeleteContentTask task = new DeleteContentTask(
						ContentListActivity.this,
						contentDirectory, 
						progress, 
						metadata,
						webMetadata, 
						metaFile,
						webMetaFile);
				task.execute(selectedRowItems.toArray());

				mode.finish();
				return true;
			default:
				return false;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content);
		
		// restore user preferences
		settings = getPreferences(MODE_PRIVATE);
		bookmarks = settings.getStringSet(SETTING_BOOKMARKS, new HashSet<String>());
		
		bts = new ArrayList<BluetoothDevice>();
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
		contentList = (ListView) findViewById(R.id.listing);

		// reload content
		reload(true);

		// setup menu drawer
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.open_drawer,
				R.string.close_drawer) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(getTitle());
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(getTitle());
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

		};
		drawerLayout.setDrawerListener(drawerToggle);

		// menu drawer listener
		drawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos,
					long id) {
				drawerList.setItemChecked(pos, true);
				DrawerItem selItem = drawerItems.get(pos);
				int type = selItem.getType();
				for (DrawerItem item : drawerItems) {
					if (item != selItem && item.getType() == type) { // clear
																		// other
																		// checks
																		// of
																		// same
																		// type
						item.setChecked(false);
					}
				}
				selItem.setChecked(true);
				applyListFilter(selItem);
				drawerLayout.closeDrawer(drawerList);
			}

		});

		// content list listener
		contentList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int pos, long id) {
				
				// if not in editing mode, open the content
				if (rowActionMode == null){ 
					currentContent = contentListAdapter.getItem(pos);
					Intent intent = new Intent(getApplicationContext(), ContentViewerActivity.class);
					
					// if selected a video
					if (currentContent instanceof Video){
						intent.putExtra(ExtraConstants.TYPE, ExtraConstants.TYPE_VIDEO);
						intent.putExtra(ExtraConstants.CONTENT, ((Video)currentContent).toByteArray());
						intent.putExtra(ExtraConstants.BOOKMARK, bookmarks.contains(((Video)currentContent).getFilepath()));
					}
					else if (currentContent instanceof Article){
						intent.putExtra(ExtraConstants.TYPE, ExtraConstants.TYPE_ARTICLE);
						intent.putExtra("content", ((Article)currentContent).toByteArray());
						intent.putExtra(ExtraConstants.BOOKMARK, bookmarks.contains(((Article)currentContent).getFilename()));
					}
					
					startActivity(intent);
					overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
				}
				// if in editing mode, add/remove selection
				else {
					boolean select = contentListAdapter.toggleSelection(pos);
					if (select)
						selectedRowItems.add(contentListAdapter.getItem(pos));
					else
						selectedRowItems.remove(contentListAdapter.getItem(pos));
				}
			}

		});
		
		// long click on list item
		contentList.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int pos, long id) {
				if (rowActionMode != null)
					return false;
				
				selectedRowItems.add(contentListAdapter.getItem(pos));
				contentListAdapter.toggleSelection(pos);
				rowActionMode = startActionMode(rowActionCallback);
				view.setSelected(true);
				return true;
			}
			
		});
		
		// Start bluetooth listener service
		if (!isListenerServiceRunning())
			startService(new Intent(this, ListenerService.class));
		
		
		// register broadcast receiver
		// local messages
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.NEW_COMMENT_ACTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.META_UPDATED_ACTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.BOOKMARK_ACTION));
				
		// global messages (from other processes)
		registerReceiver(broadcastReceiver, new IntentFilter(Constants.BT_STATUS_ACTION));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.content, menu);
		return super.onCreateOptionsMenu(menu);
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
    	if (item.getItemId() == R.id.action_refresh){
    		reload(false);
			return true;
    	}
    	else if (item.getItemId() == R.id.action_sync){
    		sync();
    		return true;
    	}
    	else if (item.getItemId() == R.id.action_download){
    		// confirm download
    		new AlertDialog.Builder(this)
    			.setTitle("Download Content")
    			.setMessage("Do you want to download default content? This will overwrite existing content.")
    			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String url = "http://shinyichen.com/shared/BackpackContent.zip";
			    		ProgressDialog pd;
			    		pd = new ProgressDialog(ContentListActivity.this);
			    		pd.setMessage("Download Content");
			    		pd.setIndeterminate(true);
			    		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			    		pd.setCancelable(true);
			    		DownloadTask task = new DownloadTask(ContentListActivity.this, pd);
			    		task.execute(url);
					}
				})
				.setNegativeButton("No", null).show();
    		
    		return true;
    	}
    	else 
    		return super.onOptionsItemSelected(item);
    		
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		SparseBooleanArray array = drawerList.getCheckedItemPositions();
		ArrayList<Integer> sel = new ArrayList<Integer>();
		for (int i = 0; i < array.size(); i++) {
			if (array.get(i))
				sel.add(i);
		}
		outState.putIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS, sel);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously state
		if (savedInstanceState.containsKey(STATE_SELECTED_DRAWER_ITEMS)) {
			for (Integer i : savedInstanceState
					.getIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS)) {
				drawerList.setItemChecked(i, true);
				drawerItems.get(i).setChecked(true);
			}
		}
	}

	@Override
	public boolean onNavigateUp() {
		if (!drawerLayout.isDrawerOpen(Gravity.LEFT))
			drawerLayout.openDrawer(Gravity.LEFT);
		else
			drawerLayout.closeDrawer(Gravity.LEFT);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (broadcastReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					broadcastReceiver);
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
		stopService(new Intent(getBaseContext(), ConnectionService.class));
	}
	
	/**
	 * reload metadata and refresh the list
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void reload(boolean firsttime) {
		
		// read meatadata
		metaFile = new File(contentDirectory, Constants.metaFileName);
		webMetaFile = new File(contentDirectory, Constants.webMetaFileName);
		ArrayList<Article> articles = new ArrayList<Article>();
		ArrayList<Video> videos = new ArrayList<Video>();

		// content objects from metadata
		try {
		if (webMetaFile.exists())
			webMetadata = Articles.parseFrom(new FileInputStream(webMetaFile));
		else
			webMetadata = Articles.newBuilder().build();
		articles.addAll(webMetadata.getArticleList());

		if (metaFile.exists())
			metadata = Videos.parseFrom(new FileInputStream(metaFile));
		else
			metadata = Videos.newBuilder().build();
		} catch (Exception e){
			e.printStackTrace();
		}
		videos.addAll(metadata.getVideoList());

		contentItems.clear();
		contentItems.addAll(videos);
		contentItems.addAll(articles);

		// get a list of categories
		// TODO need a better way to get the list of categories
		categories = new ArrayList<String>();
		for (Video video : videos) {
			String id = video.getSnippet().getCategoryId();
			if (!categories.contains(id))
				categories.add(id);
		}
		String[] cats = new String[categories.size()];
		cats = categories.toArray(cats);

		// setup menu drawer
        drawerItems.clear();
        drawerItems.add(new DrawerItem("Bookmarks", DrawerItem.HEADER, false));
        drawerItems.add(new DrawerItem("All", DrawerItem.BOOKMARKS, selectedBookmark.equals("All")));
        drawerItems.add(new DrawerItem(STARRED_BOOKMARK, DrawerItem.BOOKMARKS, selectedBookmark.equals(STARRED_BOOKMARK)));
		drawerItems.add(new DrawerItem("Content Type", DrawerItem.HEADER, false));
		drawerItems.add(new DrawerItem("All", DrawerItem.CONTENT_TYPE, selectedType.equals("All")));
		drawerItems.add(new DrawerItem(VIDEO_CONTENT, DrawerItem.CONTENT_TYPE, selectedType.equals(VIDEO_CONTENT)));
		drawerItems.add(new DrawerItem(WEB_CONTENT, DrawerItem.CONTENT_TYPE, selectedType.equals(WEB_CONTENT)));
		drawerItems.add(new DrawerItem("Categories", DrawerItem.HEADER, false));
		drawerItems.add(new DrawerItem("All", DrawerItem.CATEGORY, selectedCat.equals("All")));
		for (String cat : cats){
			drawerItems.add(new DrawerItem(cat, DrawerItem.CATEGORY, selectedCat.equals(cat)));
		}
		if (!firsttime)
			applyListFilter();
			
			
		// update/init adapter
		if (drawerListAdapter == null) { // first time
			drawerListAdapter = new DrawerListAdapter(this, drawerItems);
			drawerList.setAdapter(drawerListAdapter);
		} else
			drawerListAdapter.notifyDataSetChanged();
		
		if (contentListAdapter == null){
			contentListAdapter = new ContentListAdapter(this, contentItems, contentDirectory.getAbsolutePath());
			contentList.setAdapter(contentListAdapter);
		} else
			contentListAdapter.notifyDataSetChanged();

	}
	
	/**
	 * no changes to filter, just re-apply to the list
	 */
	private void applyListFilter(){
		applyListFilter(null);
	}
	
	private void applyListFilter(DrawerItem item){
		
		// if something selected/de-selected
		if (item != null){
			if (item.getType() == DrawerItem.HEADER)
				return;
			
			if (item.getType() == DrawerItem.BOOKMARKS){
				String newBookmark = item.getLabel();
				if (!newBookmark.equals(selectedBookmark))
					selectedBookmark = newBookmark;
				else
					return;
			}
			
			if (item.getType() == DrawerItem.CONTENT_TYPE){
				 String newType = item.getLabel();
				 if (!newType.equals(selectedType)){
					 selectedType = newType;
				 }
				 else 
					 return;
			}
			else if (item.getType() == DrawerItem.CATEGORY){
				String newCat = item.getLabel();
				if (!newCat.equals(selectedCat)){
					selectedCat = newCat;
				}
				else
					return;
			}
		}
		
		contentListAdapter.clear();

		// TODO nothing being taken care of for web category
		ArrayList<Object> list = new ArrayList<Object>();
		if (selectedType.equals("All")){
			if (selectedCat.equals("All")){
				list.addAll(metadata.getVideoList());
				list.addAll(webMetadata.getArticleList());
			}
			else {
				for (Video v : metadata.getVideoList()){
					if (v.getSnippet().getCategoryId().equals(selectedCat))
						list.add(v);
				}
			}
		} else if (selectedType.equals(VIDEO_CONTENT)) {
			if (selectedCat.equals("All"))
				list.addAll(metadata.getVideoList());
			else {
				for (Video v : metadata.getVideoList()) {
					if (v.getSnippet().getCategoryId().equals(selectedCat))
						list.add(v);
				}
			}
		} else if (selectedType.equals(WEB_CONTENT)) {
			if (selectedCat.equals("All"))
				list.addAll(webMetadata.getArticleList());
		}
		
		
		// if selected starred
		if (selectedBookmark.equals("All"))
			contentListAdapter.addAll(list);
		else {
			for (Object o : list){
				String filename = null;
				if (o instanceof Video)
					filename = ((Video)o).getFilepath();
				else
					filename = ((Article)o).getFilename();
				if (bookmarks.contains(filename))
					contentListAdapter.add(o);
			}
		}
		
		// update list
		contentListAdapter.notifyDataSetChanged();
	}

	/**
	 * add new user comment to metadata
	 * 
	 * @param comment
	 */
	private void addComment(final String user, final String date,
			final String comment) {
		if (currentContent == null || comment == null || comment.isEmpty())
			return;

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {

				// Add comments to metadata

				if (currentContent instanceof Video) {
					Videos.Builder newVideos = Videos.newBuilder();
					for (Video video : metadata.getVideoList()) {

						// copy from original
						Video.Builder videoBuilder = Video.newBuilder();
						videoBuilder.mergeFrom(video);

						// found the video to add comment
						if (video.getId().equals(
								((Video) currentContent).getId())) {

							// create new comment
							Comment.Builder commentBuilder = Comment
									.newBuilder();
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

				} else if (currentContent instanceof Article) {
					Articles.Builder newArticles = Articles.newBuilder();
					for (Article article : webMetadata.getArticleList()) {

						// copy data from original
						Article.Builder articleBuilder = Article.newBuilder();
						articleBuilder.mergeFrom(article);

						// found the article to add comment
						if (article.getUrl().equals(
								((Article) currentContent).getUrl())) {

							// create new comment
							Comment.Builder commentBuilder = Comment
									.newBuilder();
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
				LocalBroadcastManager.getInstance(ContentListActivity.this)
						.sendBroadcast(i);
			}

		});

		t.start();
	}
	
	private void sync(){
		
		bts.clear();
		/* To check if the device has the bluetooth hardware */
		String TAG = "BluetoothFileTransferActivity";
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG,
					"No bluetooth capabilities available on the device. Exiting!!");
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			// finish();
			return;
		}

		/*
		 * create an Broadcast register and register the event that you are
		 * interested in
		 */
		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		if (!mBluetoothAdapter.isEnabled()) {
			// make your device discoverable
			Intent makeDiscoverable = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			makeDiscoverable.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivityForResult(makeDiscoverable, REQUEST_ENABLE_BT);
		} else {
			Log.d(TAG,
					"Bluetooth is already enabled. Setting up the file transfer");
		}

		searchForBTDevices();

		dialog = new BluetoothListDialog();
		dialog.setHandler(new BluetoothListDialog.IHandler() {

			@Override
			public void onReturnValue(BluetoothDevice device) {
				dialog.dismiss();
				mBluetoothAdapter.cancelDiscovery();
				Intent bluetoothServiceIntent = new Intent(getBaseContext(),
						ConnectionService.class);
				bluetoothServiceIntent.putExtra("Device", device);
				startService(bluetoothServiceIntent);
			}
		});
		dialog.setList(bts);
		dialog.setReceiver(mReceiver);
		dialog.show(getFragmentManager(), "BluetoothListDialog");

	}

	private void searchForBTDevices() {

		mBluetoothAdapter.startDiscovery();
	}

	private boolean isListenerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (ListenerService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	private void saveBookmarks(){
		SharedPreferences.Editor editor = settings.edit();
		editor.putStringSet(SETTING_BOOKMARKS, bookmarks);
		editor.commit();
	}

	/* (non-Javadoc)
	 * @see edu.isi.usaid.pifi.BookmarkManager#addBookmark(java.lang.String)
	 */
	@Override
	public void addBookmark(String id) {
		if (!bookmarks.contains(id)){
			bookmarks.add(id);
			saveBookmarks();
		}
	}

	/* (non-Javadoc)
	 * @see edu.isi.usaid.pifi.BookmarkManager#removeBookmark(java.lang.String)
	 */
	@Override
	public void removeBookmark(String id) {
		if (bookmarks.contains(id)){
			bookmarks.remove(id);
			saveBookmarks();
		}
	}

	/* (non-Javadoc)
	 * @see edu.isi.usaid.pifi.BookmarkManager#isBookmarked(java.lang.String)
	 */
	@Override
	public boolean isBookmarked(String id) {
		if (bookmarks.contains(id))
			return true;
		else
			return false;
	}
}
