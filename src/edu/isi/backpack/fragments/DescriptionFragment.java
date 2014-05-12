/**
 * 
 */

package edu.isi.backpack.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.isi.backpack.constants.ExtraConstants;

import org.toosheh.android.R;

/**
 * @author jenniferchen A page that shows the description of individual content
 */
public class DescriptionFragment extends Fragment {

    public static final DescriptionFragment newInstance(String date, String description) {
        DescriptionFragment f = new DescriptionFragment();
        Bundle bundle = new Bundle(2);
        bundle.putString(ExtraConstants.DESCRIPTION, description);
        bundle.putString(ExtraConstants.DATE, date);
        f.setArguments(bundle);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_description, container,
                false);

        TextView dateView = (TextView) rootView.findViewById(R.id.publishDate);
        dateView.setText(getArguments().getString(ExtraConstants.DATE));
        TextView descView = (TextView) rootView.findViewById(R.id.fullDescription);
        descView.setText(getArguments().getString(ExtraConstants.DESCRIPTION));

        return rootView;
    }

}
