
package edu.isi.backpack.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
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
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jenniferchen viewer for individual content using sliding pages
 */
public class ContentViewerActivity extends FragmentActivity {

    private ViewPager pager;

    private PageAdapter pagerAdapter;

    private File contentDirectory;

    private Media.Item content = null;

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
        titleTextView = (TextView) titleView.findViewById(R.id.title);
        titleTextView.setSelected(true); // if we are using marquee title in
                                         // action bar, need this to prevent
                                         // video view from taking over focus
        getActionBar().setCustomView(titleView);

        setContentView(R.layout.activity_content_viewer);

        bookmark = getIntent().getBooleanExtra(ExtraConstants.BOOKMARK, false);

        File appDir = getExternalFilesDir(null);
        contentDirectory = new File(appDir, Constants.contentDirName);
        if (!contentDirectory.exists())
            contentDirectory.mkdir();

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOffscreenPageLimit(2); // prevent fragments from destroyed when
                                        // moved away from screen
        pagerAdapter = new PageAdapter(getSupportFragmentManager(), getFragments());
        pager.setAdapter(pagerAdapter);

        /**
         * do not delete this part of the code we might still bring back // show
         * tabs
         * getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS); //
         * tab change listener ActionBar.TabListener tabListener = new
         * ActionBar.TabListener() {
         * 
         * @Override public void onTabUnselected(Tab tab, FragmentTransaction
         *           ft) { }
         * @Override public void onTabSelected(Tab tab, FragmentTransaction ft)
         *           { pager.setCurrentItem(tab.getPosition(), true); }
         * @Override public void onTabReselected(Tab tab, FragmentTransaction
         *           ft) { } }; // create tabs, specifying the tab's text and
         *           TabListener
         *           getActionBar().addTab(getActionBar().newTab().setText
         *           (getString
         *           (R.string.tab_content)).setTabListener(tabListener));
         *           getActionBar
         *           ().addTab(getActionBar().newTab().setText(getString
         *           (R.string.tab_desc)).setTabListener(tabListener));
         *           getActionBar
         *           ().addTab(getActionBar().newTab().setText(getString
         *           (R.string.tab_comments)).setTabListener(tabListener)); //
         *           swip listener pager.setOnPageChangeListener(new
         *           OnPageChangeListener(){
         * @Override public void onPageScrollStateChanged(int arg0) { }
         * @Override public void onPageScrolled(int pos, float arg1, int arg2) {
         *           // pause video Fragment frag = pagerAdapter.getItem(pos);
         *           if (frag instanceof VideoPlayerFragment){
         *           VideoPlayerFragment vFrag = (VideoPlayerFragment)frag;
         *           vFrag.pause(); } }
         * @Override public void onPageSelected(int arg0) {
         *           getActionBar().setSelectedNavigationItem(arg0); } });
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

        if (content.getType() == Type.VIDEO)
            menu.findItem(R.id.action_web).setVisible(false);
        else
            menu.findItem(R.id.action_web).setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_web) {
            // confirm
            new AlertDialog.Builder(ContentViewerActivity.this)
                    .setTitle(R.string.confirm_open_web_title)
                    .setMessage(R.string.confirm_open_web)
                    .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // go to website
                            if (content.getType() == Type.HTML) {
                                String url = content.getUrl();
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            }
                        }
                    }).setNegativeButton(R.string.button_cancel, null).show();

            return true;
        }
        // user clicked on star
        else if (item.getItemId() == R.id.action_star) {

            // toggle bookmark
            bookmark = !bookmark;
            if (bookmark)
                menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_selected);
            else
                menu.findItem(R.id.action_star).setIcon(R.drawable.ic_fav_unselected);

            // broadcast
            Intent i = new Intent();
            i.setAction(Constants.BOOKMARK_ACTION);
            i.putExtra(ExtraConstants.ID, content.getFilename());
            i.putExtra(ExtraConstants.ON, bookmark);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);

            return true;
        } else
            return super.onOptionsItemSelected(item);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private List<Fragment> getFragments() {

        List<Fragment> f = new ArrayList<Fragment>();

        try {

            content = Media.Item.parseFrom(getIntent().getByteArrayExtra(ExtraConstants.CONTENT));
            if (content.getType() == Type.VIDEO)
                f.add(VideoPlayerFragment.newInstance(
                        contentDirectory + "/" + content.getFilename(), content.getTitle()));
            else if (content.getType() == Type.HTML) {
                File htmlFile = new File(contentDirectory + "/" + content.getFilename());
                Uri uri = Uri.fromFile(htmlFile);
                f.add(HtmlFragment.newInstance(uri.toString(), content.getTitle()));
            }

            titleTextView.setText(content.getTitle());

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
