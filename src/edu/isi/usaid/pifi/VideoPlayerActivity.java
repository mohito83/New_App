package edu.isi.usaid.pifi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoPlayerActivity extends Activity {
	
	private VideoView videoView;
	
	private Button fullscreenButton;
	
	private String videoPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);
		
		// this is to show the up button on action bar to go back to home screen
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		videoView = (VideoView)findViewById(R.id.videoPlayer);
		TextView titleView = (TextView)findViewById(R.id.videoPlayerTitle);
		TextView descView = (TextView)findViewById(R.id.videoPlayerDesc);
		fullscreenButton = (Button)findViewById(R.id.fullscreenButton);
		
		MediaController mc = new MediaController(this);
		mc.setMediaPlayer(videoView);
		videoView.setMediaController(mc);
		
		Intent i = getIntent();
		titleView.setText(i.getStringExtra("title"));
		descView.setText(i.getStringExtra("description"));
		videoPath = i.getStringExtra("video");
		videoView.setVideoPath(videoPath);
		videoView.start();
		
		fullscreenButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				int pos = videoView.getCurrentPosition();
				videoView.stopPlayback();
				Intent i = new Intent(getApplicationContext(), FullscreenVideoActivity.class);
				i.putExtra("video", videoPath);
				i.putExtra("position", pos);
				startActivity(i);
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.video_player, menu);
		return true;
	}
	
	@Override
	public void onBackPressed() {
	    super.onBackPressed();
	    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
	}

	@Override
	public boolean onNavigateUp() {
		boolean r = super.onNavigateUp();
		overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
		return r;
	}
}
