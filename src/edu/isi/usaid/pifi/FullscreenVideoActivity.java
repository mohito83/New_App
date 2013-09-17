package edu.isi.usaid.pifi;

import edu.isi.usaid.pifi.fragments.VideoPlayerFragment;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.VideoView;
/**
 * 
 * @author jenniferchen
 * 
 * This activity is a fullscreen video view
 * 
 */
public class FullscreenVideoActivity extends Activity implements VideoControllerView.MediaPlayerControl {
	
	private VideoView videoView;
	
	private VideoControllerView controller;
	
	private String videoSource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen_video);
		
		videoView = (VideoView)findViewById(R.id.fullVideoSurface);
		videoSource = getIntent().getStringExtra(ExtraConstants.PATH);
		videoView.setVideoPath(videoSource);
		int pos = getIntent().getIntExtra(ExtraConstants.POSITION, 0);
		videoView.seekTo(pos); // start where left off
		
		controller = new VideoControllerView(this);
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.fullVideoSurfaceContainer));
        
		videoView.start();
		
		videoView.setOnCompletionListener(new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer arg0) {
				finish();
			}
			
		});
		
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        controller.show();
        return false;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fullscreen_video, menu);
		return true;
	}

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
    	return videoView.getCurrentPosition();
    }

    @Override
    public int getDuration() {
    	return videoView.getDuration();
    }

    @Override
    public boolean isPlaying() {
    	return videoView.isPlaying();
    }

    @Override
    public void pause() {
    	videoView.pause();
    }

    @Override
    public void seekTo(int i) {
    	videoView.seekTo(i);
    }

    @Override
    public void start() {
    	videoView.start();
    }

    @Override
    public boolean isFullScreen() {
        return true;
    }

    @Override
    public void toggleFullScreen() {
    	int pos = videoView.getCurrentPosition();
    	videoView.stopPlayback();
		Intent i = new Intent(getApplicationContext(), VideoPlayerFragment.class);
		i.putExtra(ExtraConstants.POSITION, pos);
		setResult(RESULT_OK, i);
		finish();
    }
    
    @Override
	public void onBackPressed() {
    	videoView.stopPlayback();
	    super.onBackPressed();
	    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
	}

}
