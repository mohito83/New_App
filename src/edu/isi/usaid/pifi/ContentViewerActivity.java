package edu.isi.usaid.pifi;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.WindowManager;
import edu.isi.usaid.pifi.fragments.DescriptionFragment;
import edu.isi.usaid.pifi.fragments.HtmlFragment;
import edu.isi.usaid.pifi.fragments.VideoPlayerFragment;

/**
 * 
 * @author jenniferchen
 * 
 * viewer for individual content using sliding pages
 */
public class ContentViewerActivity extends FragmentActivity {
	
	private ViewPager pager;
	
	private PagerAdapter pagerAdapter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content_viewer);
		
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
		if (type.equals(ExtraConstants.TYPE_VIDEO)){
			f.add(VideoPlayerFragment.newInstance(
					getIntent().getStringExtra(ExtraConstants.PATH),
					getIntent().getStringExtra(ExtraConstants.TITLE)));
			f.add(DescriptionFragment.newInstance(getIntent().getStringExtra(ExtraConstants.DESCRIPTION)));
		}
		else if (type.equals(ExtraConstants.TYPE_ARTICLE)){
			f.add(HtmlFragment.newInstance(getIntent().getStringExtra(ExtraConstants.URI)));
			f.add(DescriptionFragment.newInstance("No Description")); // TODO description for article
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
