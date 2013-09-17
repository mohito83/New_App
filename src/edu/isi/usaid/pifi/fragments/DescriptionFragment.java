/**
 * 
 */
package edu.isi.usaid.pifi.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.isi.usaid.pifi.ExtraConstants;
import edu.isi.usaid.pifi.R;

/**
 * @author jenniferchen
 * 
 * A page that shows the description of individual content
 *
 */
public class DescriptionFragment extends Fragment {
	
	public static final DescriptionFragment newInstance(String description){
		DescriptionFragment f = new DescriptionFragment();
		Bundle bundle = new Bundle(1);
		bundle.putString(ExtraConstants.DESCRIPTION, description);
		f.setArguments(bundle);
		return f;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_description, container, false);

        TextView descView = (TextView)rootView.findViewById(R.id.fullDescription);
        descView.setText(getArguments().getString(ExtraConstants.DESCRIPTION));

        return rootView;
    }

}
