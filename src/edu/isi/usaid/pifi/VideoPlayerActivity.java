package edu.isi.usaid.pifi;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

/**
 * 
 * @author jenniferchen
 * 
 * This activity shows the video and its description
 * 
 */
public class VideoPlayerActivity extends Activity implements VideoControllerView.MediaPlayerControl {

    private VideoView videoSurface;
    
    private VideoControllerView controller;
    
    private String videoSource;
    
    private static final int FULLSCREEN_ACTIVITY = 1011;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        
        // this is to show the up button on action bar to go back to home screen
     	getActionBar().setDisplayHomeAsUpEnabled(true);
     	
     	// UI objects
     	videoSurface = (VideoView) findViewById(R.id.videoSurface);
     	TextView titleView = (TextView)findViewById(R.id.videoPlayerTitle);
		TextView descView = (TextView)findViewById(R.id.videoPlayerDesc);
		
     	// intent
     	Intent i = getIntent();
        videoSource = i.getStringExtra("video");
        titleView.setText(i.getStringExtra("title"));
		descView.setText(i.getStringExtra("description"));
        
		// video controller
        controller = new VideoControllerView(this);
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
        
        videoSurface.setVideoPath(videoSource);
        videoSurface.start();
        
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        return false;
    }
    
    @Override
    /**
     * Implemented this so that when fullscreen returns back here
     * we have the position where video was at.
     * 
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      switch(requestCode) {
        case (FULLSCREEN_ACTIVITY) : {
          if (resultCode == Activity.RESULT_OK) {
        	  int pos = data.getIntExtra("position", 0);
        	  videoSurface.seekTo(pos);
        	  videoSurface.start();
          }
          break;
        } 
      }
    }


    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
    	return videoSurface.getCurrentPosition();
    }

    @Override
    public int getDuration() {
    	return videoSurface.getDuration();
    }

    @Override
    public boolean isPlaying() {
    	return videoSurface.isPlaying();
    }

    @Override
    public void pause() {
    	videoSurface.pause();
    }

    @Override
    public void seekTo(int i) {
        videoSurface.seekTo(i);
    }

    @Override
    public void start() {
        videoSurface.start();
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {
    	int pos = videoSurface.getCurrentPosition();
    	videoSurface.stopPlayback();
		Intent i = new Intent(getApplicationContext(), FullscreenVideoActivity.class);
		i.putExtra("video", videoSource);
		i.putExtra("position", pos);
		startActivityForResult(i, FULLSCREEN_ACTIVITY);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.video_player, menu);
		return true;
	}
    
	@Override
	public void onBackPressed() {
		videoSurface.stopPlayback();
	    super.onBackPressed();
	    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
	}

	@Override
	public boolean onNavigateUp() {
		videoSurface.stopPlayback();
		boolean r = super.onNavigateUp();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		return r;
	}

}
