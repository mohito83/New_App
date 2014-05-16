package edu.isi.backpack.activities;

import org.toosheh.android.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

@SuppressLint("NewApi")

public class HelpActivity extends Activity {
	
	ViewPager viewPager;
	 Button skip,next;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  
    // remove title
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    Bundle extras = getIntent().getExtras();
    String value="splash";
    if (extras != null) {
       value = extras.getString("type");
    }
    
    if (value.equals("splash")) {
       ImageView splash = new ImageView(getBaseContext());
       splash.setImageResource(R.drawable.splash);
	       splash.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
       setContentView(splash);
    }else{
	    
	    setContentView(R.layout.activity_help);
	    
	   
	    skip = (Button) findViewById(R.id.skip);
	    skip.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	    next = (Button) findViewById(R.id.next);
	    next.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				viewPager.setCurrentItem(viewPager.getCurrentItem()+1);
			}
		});
	
	    viewPager = (ViewPager) findViewById(R.id.view_pager);
	    final ImagePagerAdapter adapter = new ImagePagerAdapter();
	    viewPager.setAdapter(adapter);
	    viewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				if(arg0 == adapter.mImages.length-1){
					finish();
				}
				if(arg0 == adapter.mImages.length-2){
					next.setText("FINISH");
				}else{
					next.setText("NEXT");
				}
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
    }
  }

    private class ImagePagerAdapter extends PagerAdapter {
        public int[] mImages = new int[] {
                R.drawable.image1, R.drawable.image3, R.drawable.image2, R.drawable.image4,
                R.drawable.image5, R.drawable.ic_launcher
        };

        @Override
        public int getCount() {
            return mImages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((ImageView) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = HelpActivity.this;
            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(mImages[position]);
            ((ViewPager) container).addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
      ((ViewPager) container).removeView((ImageView) object);
    }
  }
}