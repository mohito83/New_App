package edu.isi.backpack.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.VideoView;
import edu.isi.backpack.R;
import edu.isi.backpack.VideoControllerView;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.fragments.VideoPlayerFragment;
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
		boolean resume = getIntent().getBooleanExtra(ExtraConstants.RESUME, false);
		videoView.seekTo(pos); // start where left off
		
		controller = new VideoControllerView(this);
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) findViewById(R.id.fullVideoSurfaceContainer));
        
        // if never started the video
        // show preview
        if (pos == 0 && !resume){
        	Bitmap preview = ThumbnailUtils.createVideoThumbnail(
        			videoSource,
        	        MediaStore.Images.Thumbnails.MINI_KIND);
        	videoView.setBackground(new BitmapDrawable(getResources(), preview));
        }
        
		if (resume)
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
    	videoView.setBackgroundResource(0);
    	videoView.start();
    }

    @Override
    public boolean isFullScreen() {
        return true;
    }

    @Override
    public void toggleFullScreen() {
    	int pos = videoView.getCurrentPosition();
    	boolean isPlaying = videoView.isPlaying();
    	videoView.stopPlayback();
		Intent i = new Intent(getApplicationContext(), VideoPlayerFragment.class);
		i.putExtra(ExtraConstants.POSITION, pos);
		i.putExtra(ExtraConstants.RESUME, isPlaying);
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
