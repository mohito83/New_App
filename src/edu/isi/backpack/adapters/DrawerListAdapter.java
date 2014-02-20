/**
 * 
 */
package edu.isi.backpack.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.isi.backpack.R;

/**
 * @author jenniferchen
 *
 */
public class DrawerListAdapter extends ArrayAdapter<DrawerItem> {
	

/**
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public DrawerListAdapter(Context context,
			List<DrawerItem> objects) {
		super(context, R.layout.drawer_list_item, objects);
	}

	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		
		LayoutInflater inflater = (LayoutInflater) getContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		DrawerItem item = getItem(position);
		int type = item.getType();
		
		View view;
		if (type == DrawerItem.HEADER){
			view = inflater.inflate(R.layout.drawer_list_header_item, parent, false);
			TextView textView = (TextView)view.findViewById(R.id.drawerHeaderText);
			textView.setText(item.getLabel());
		}
		else {
			view = inflater.inflate(R.layout.drawer_list_item, parent, false);
			TextView textView = (TextView)view.findViewById(R.id.drawerItemText);
			textView.setText(item.getLabel());
			ImageView checkView = (ImageView)view.findViewById(R.id.drawerItemCheck);
			if (item.isChecked())
				checkView.setVisibility(View.VISIBLE);
			else
				checkView.setVisibility(View.INVISIBLE);
				
		}
		
        return view;
    }

}
