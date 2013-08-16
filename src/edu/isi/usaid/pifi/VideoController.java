package edu.isi.usaid.pifi;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.MediaController;

public class VideoController extends MediaController {

	public VideoController(Context context) {
		super(context);
	}
	
	@Override 
	 public void setAnchorView(View view) {
	     super.setAnchorView(view);

	     Button searchButton = new Button(getContext());
	     searchButton.setText("Search");
	     FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	     params.gravity = Gravity.RIGHT;
	     addView(searchButton, params);
	}

}
