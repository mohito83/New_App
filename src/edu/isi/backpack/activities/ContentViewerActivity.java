package edu.isi.backpack.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.isi.backpack.R;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.fragments.HtmlFragment;
import edu.isi.backpack.fragments.VideoPlayerFragment;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.VideoProtos.Video;

/**
 * 
 * @author jenniferchen
 * 
 * viewer for individual content using sliding pages
 */
public class ContentViewerActivity extends FragmentActivity {
	
	private ViewPager pager;
	
	private PageAdapter pagerAdapter;
	
	private File contentDirectory;
	
	private Video video = null;
	
	private Article article = null;
	
	private boolean bookmark = false;
	
	private Menu menu;
	
	private TextView titleTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setDisplayShowTitleEnabled(false);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		View titleView = inflater.inflate(R.layout.scroll_title_view, null);
		titleTextView = (TextView)titleView.findViewById(R.id.title);
		titleTextView.setSelected(true); // if we are using marquee title in action bar, need this to prevent video view from taking over focus
		getActionBar().setCustomView(titleView);
		
		setContentView(R.layout.activity_content_viewer);
		
		bookmark = getIntent().getBooleanExtra(ExtraConstants.BOOKMARK, false);
		
		File sdDir = Environment.getExternalStorageDirectory();
		contentDirectory = new File(sdDir, Constants.contentDirName);
		if (!contentDirectory.exists())
			contentDirectory.mkdir();
		
		pager = (ViewPager)findViewById(R.id.pager);
		pager.setOffscreenPageLimit(2); // prevent fragments from destroyed when moved away from screen
		pagerAdapter = new PageAdapter(getSupportFragmentManager(), getFragments());
		pager.setAdapter(pagerAdapter);

		/** do not delete
		 * this part of the code we might still bring back
		 * 
		// show tabs
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// tab change listener
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			}
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				pager.setCurrentItem(tab.getPosition(), true);
			}
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				
			}
		};
		
		// create tabs, specifying the tab's text and TabListener
        getActionBar().addTab(getActionBar().newTab().setText("View Content").setTabListener(tabListener));
        getActionBar().addTab(getActionBar().newTab().setText("Description").setTabListener(tabListener));
        getActionBar().addTab(getActionBar().newTab().setText("Comments").setTabListener(tabListener));

        // swip listener
        pager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int pos, float arg1, int arg2) {
				// pause video
				Fragment frag = pagerAdapter.getItem(pos);
				if (frag instanceof VideoPlayerFragment){
					VideoPlayerFragment vFrag = (VideoPlayerFragment)frag;
					vFrag.pause();
				}
			}

			@Override
			public void onPageSelected(int arg0) {
				getActionBar().setSelectedNavigationItem(arg0);
			}
        	
        });
        **/
		
		// this prevent video to start over when orientation is changed
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		            WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.content_viewer, menu);
		if (bookmark)
			menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_selected);
		else
			menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_unselected);
		
		if (video != null)
			menu.findItem(R.id.action_web).setVisible(false);
		else
			menu.findItem(R.id.action_web).setVisible(true);
		
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
		if (item.getItemId() == R.id.action_web){
			// go to website
			if (article != null){
				String url = article.getUrl();
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
			return true;
		}
		// user clicked on star
		else if (item.getItemId() == R.id.action_star){
    		
    		// toggle bookmark
    		bookmark = !bookmark;
    		if (bookmark)
    			menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_selected);
    		else
    			menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_unselected);
    		
    		// broadcast
    		Intent i = new Intent();
    		i.setAction(Constants.BOOKMARK_ACTION);
    		if (video != null)
    			i.putExtra(ExtraConstants.ID, video.getFilepath());
    		else
    			i.putExtra(ExtraConstants.ID, article.getFilename());
    		i.putExtra(ExtraConstants.ON, bookmark);
    		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    		
    		return true;
    	}
    	else 
    		return super.onOptionsItemSelected(item);
    		
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	            WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	private List<Fragment> getFragments(){
	
		List<Fragment> f = new ArrayList<Fragment>();
		String type = getIntent().getStringExtra(ExtraConstants.TYPE);
		
		try {
		
			if (type.equals(ExtraConstants.TYPE_VIDEO)){
			
				video = Video.parseFrom(getIntent().getByteArrayExtra(ExtraConstants.CONTENT));
				f.add(VideoPlayerFragment.newInstance(
						contentDirectory + "/" + video.getFilepath(),
						video.getSnippet().getTitle()));
//				f.add(DescriptionFragment.newInstance(video.getSnippet().getPublishedAt(), video.getSnippet().getDescription()));
//				f.add(CommentsFragment.newInstance(video.getCommentsList()));
				titleTextView.setText(video.getSnippet().getTitle());
			}
			else if (type.equals(ExtraConstants.TYPE_ARTICLE)){
			
				article = Article.parseFrom(getIntent().getByteArrayExtra(ExtraConstants.CONTENT));
				File htmlFile = new File(contentDirectory + "/" + article.getFilename());
				Uri uri = Uri.fromFile(htmlFile);
				f.add(HtmlFragment.newInstance(uri.toString(), article.getTitle()));
//				f.add(DescriptionFragment.newInstance(article.getDatePublished(), "")); // TODO description for article
//				f.add(CommentsFragment.newInstance(article.getCommentsList()));
				titleTextView.setText(article.getTitle());
			}
		
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
		
		return f;
		
	}
	
	@Override
	public boolean onNavigateUp() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		return true;
	}
	
	private class PageAdapter extends FragmentPagerAdapter {
		private List<Fragment> fragments;
	    
		public PageAdapter(FragmentManager fm, List<Fragment> fragments) {
	        super(fm);
	        this.fragments = fragments;
	    }

	    @Override
	    public android.support.v4.app.Fragment getItem(int position) {
	        return fragments.get(position);
	    }
	
	    @Override
	    public int getCount() {
	        return fragments.size();
	    }
    }

}
