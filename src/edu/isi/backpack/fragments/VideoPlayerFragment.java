
package edu.isi.backpack.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;
import edu.isi.backpack.R;
import edu.isi.backpack.VideoControllerView;
import edu.isi.backpack.activities.FullscreenVideoActivity;
import edu.isi.backpack.constants.ExtraConstants;

/**
 * @author jenniferchen This activity shows the video and its description
 */
public class VideoPlayerFragment extends Fragment implements VideoControllerView.MediaPlayerControl {

    private VideoView videoSurface;

    private VideoControllerView controller;

    private String videoSource;

    private int pausedPos = 0;

    private boolean resumePlay = false;

    private static final int FULLSCREEN_ACTIVITY = 1011;

    public static final VideoPlayerFragment newInstance(String source, String title) {
        VideoPlayerFragment f = new VideoPlayerFragment();
        Bundle b = new Bundle(2);
        b.putString(ExtraConstants.PATH, source);
        b.putString(ExtraConstants.TITLE, title);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_video_player,
                container, false);

        // UI objects
        videoSurface = (VideoView) rootView.findViewById(R.id.videoSurface);
        TextView titleView = (TextView) rootView.findViewById(R.id.videoPlayerTitle);

        // intent
        videoSource = getArguments().getString(ExtraConstants.PATH);
        titleView.setText(getArguments().getString(ExtraConstants.TITLE));

        // video controller
        controller = new VideoControllerView(getActivity());
        controller.setMediaPlayer(this);
        controller.setAnchorView((FrameLayout) rootView.findViewById(R.id.videoSurfaceContainer));

        videoSurface.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                controller.show();
                return false;
            }

        });
        videoSurface.setVideoPath(videoSource);
        if (savedInstanceState != null) {
            int pos = savedInstanceState.getInt(ExtraConstants.POSITION, 0);
            videoSurface.seekTo(pos);
        } else {
            // initial preview
            // preview = ThumbnailUtils.createVideoThumbnail(
            // videoSource,
            // MediaStore.Images.Thumbnails.MINI_KIND);
            // videoSurface.setBackground(new BitmapDrawable(getResources(),
            // preview));
            // controller.show();
            start();
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ExtraConstants.POSITION, getCurrentPosition());
    }

    @Override
    public void onPause() {
        super.onPause();
        pausedPos = videoSurface.getCurrentPosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoSurface.seekTo(pausedPos);
        if (resumePlay)
            videoSurface.start();
        resumePlay = false;
    }

    @Override
    /**
     * Implemented this so that when fullscreen returns back here
     * we have the position where video was at.
     * 
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (FULLSCREEN_ACTIVITY): {
                if (resultCode == Activity.RESULT_OK) {
                    pausedPos = data.getIntExtra(ExtraConstants.POSITION, 0);
                    resumePlay = data.getBooleanExtra(ExtraConstants.RESUME, false);
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
        videoSurface.setBackgroundResource(0); // clear thumbnail
        videoSurface.start();
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void toggleFullScreen() {
        int pos = videoSurface.getCurrentPosition();
        boolean isPlaying = videoSurface.isPlaying();
        videoSurface.stopPlayback();
        Intent i = new Intent(getActivity(), FullscreenVideoActivity.class);
        i.putExtra(ExtraConstants.PATH, videoSource);
        i.putExtra(ExtraConstants.POSITION, pos);
        i.putExtra(ExtraConstants.RESUME, isPlaying);
        startActivityForResult(i, FULLSCREEN_ACTIVITY);
    }

}
