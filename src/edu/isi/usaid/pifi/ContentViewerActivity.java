package edu.isi.usaid.pifi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.WindowManager;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.isi.usaid.pifi.fragments.CommentsFragment;
import edu.isi.usaid.pifi.fragments.DescriptionFragment;
import edu.isi.usaid.pifi.fragments.HtmlFragment;
import edu.isi.usaid.pifi.fragments.VideoPlayerFragment;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * 
 * @author jenniferchen
 * 
 * viewer for individual content using sliding pages
 */
public class ContentViewerActivity extends FragmentActivity {
	
	private ViewPager pager;
	
	private PagerAdapter pagerAdapter;
	
	private File contentDirectory;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content_viewer);
		
		File sdDir = Environment.getExternalStorageDirectory();
		contentDirectory = new File(sdDir, Constants.contentDirName);
		if (!contentDirectory.exists())
			contentDirectory.mkdir();
		
		// show "up" menu
		getActionBar().setDisplayHomeAsUpEnabled(true);
		pager = (ViewPager)findViewById(R.id.pager);
		pagerAdapter = new PageAdapter(getSupportFragmentManager(), getFragments());
		pager.setAdapter(pagerAdapter);
		
		// this prevent video to start over when orientation is changed
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		            WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.content_viewer, menu);
		return true;
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
			
				Video video = Video.parseFrom(getIntent().getByteArrayExtra(ExtraConstants.CONTENT));
				f.add(VideoPlayerFragment.newInstance(
						contentDirectory + "/" + video.getFilename(),
						video.getSnippet().getTitle()));
				f.add(DescriptionFragment.newInstance(video.getSnippet().getDescription()));
				f.add(CommentsFragment.newInstance(video.getCommentsList()));
			}
			else if (type.equals(ExtraConstants.TYPE_ARTICLE)){
			
				Article article = Article.parseFrom(getIntent().getByteArrayExtra(ExtraConstants.CONTENT));
				File htmlFile = new File(contentDirectory + "/" + article.getFilename());
				Uri uri = Uri.fromFile(htmlFile);
				f.add(HtmlFragment.newInstance(uri.toString()));
				f.add(DescriptionFragment.newInstance("No Description")); // TODO description for article
				f.add(CommentsFragment.newInstance(article.getCommentsList()));
				
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
