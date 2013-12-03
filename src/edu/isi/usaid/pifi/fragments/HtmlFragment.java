package edu.isi.usaid.pifi.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import edu.isi.usaid.pifi.ExtraConstants;
import edu.isi.usaid.pifi.R;

/**
 * 
 * @author jenniferchen
 * 
 * html viewer
 *
 */
@SuppressLint("SetJavaScriptEnabled")
public class HtmlFragment extends Fragment {
	
	private WebView webview;
	
	public static final HtmlFragment newInstance(String uri){
		HtmlFragment f = new HtmlFragment();
		Bundle b = new Bundle(1);
		b.putString(ExtraConstants.URI, uri);
		f.setArguments(b);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
     	
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_html, container, false);

        // get uri
     	String uri = getArguments().getString(ExtraConstants.URI);
 		
 		// settings
 		webview = (WebView)rootView.findViewById(R.id.webview);
 		WebSettings settings = webview.getSettings();
 		settings.setTextZoom(200);
 		settings.setJavaScriptEnabled(true);
 		settings.setPluginState(PluginState.ON);
 		settings.setBuiltInZoomControls(true);
 		settings.setLoadWithOverviewMode(true);
 		settings.setUseWideViewPort(true);
// 		webview.setWebViewClient(new WebViewClient()); // keep using webview when user click on links

 		// load html content
 		webview.loadUrl(uri);

        return rootView;
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Inflate the menu; this adds items to the action bar if it is present.
		inflater.inflate(R.menu.html, menu);
	}
	


}
