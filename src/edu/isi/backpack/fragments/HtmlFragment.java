
package edu.isi.backpack.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import edu.isi.backpack.R;
import edu.isi.backpack.constants.ExtraConstants;

/**
 * @author jenniferchen html viewer
 */
@SuppressLint("SetJavaScriptEnabled")
public class HtmlFragment extends Fragment {

    private WebView webview;

    public static final HtmlFragment newInstance(String uri, String title) {
        HtmlFragment f = new HtmlFragment();
        Bundle b = new Bundle(1);
        b.putString(ExtraConstants.URI, uri);
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
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_html, container, false);

        String uri = getArguments().getString(ExtraConstants.URI);
        String title = getArguments().getString(ExtraConstants.TITLE);
        ((TextView) rootView.findViewById(R.id.htmlTitle)).setText(title);

        // settings
        webview = (WebView) rootView.findViewById(R.id.webview);
        WebSettings settings = webview.getSettings();
        settings.setTextZoom(200);
        settings.setJavaScriptEnabled(false);
        settings.setBuiltInZoomControls(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBlockNetworkLoads(true); // disable loading external
                                             // resources
        // webview.setWebViewClient(new WebViewClient()); // keep using webview
        // when user click on links

        // load html content
        webview.loadUrl(uri);

        return rootView;
    }

}
