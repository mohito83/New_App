/**
 * 
 */
package edu.isi.usaid.pifi.data;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import edu.isi.usaid.pifi.R;

/**
 * @author jenniferchen
 *
 */
public class BluetoothListAdapter extends ArrayAdapter<BluetoothItem> {

	/**
	 * @param context
	 * @param resource
	 * @param textViewResourceId
	 * @param objects
	 */
	public BluetoothListAdapter(Context context, List<BluetoothItem> objects) {
		super(context, R.layout.bluetooth_list_item, objects);
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		
		LayoutInflater inflater = (LayoutInflater) getContext()
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		BluetoothItem item = getItem(position);
		int type = item.getType();
		
		View view;
		if (type == BluetoothItem.HEADER){
			view = inflater.inflate(R.layout.bluetooth_list_header_item, parent, false);
			TextView textView = (TextView)view.findViewById(R.id.btHeaderText);
			textView.setText(item.getLabel());
		}
		else {
			view = inflater.inflate(R.layout.bluetooth_list_item, parent, false);
			TextView textView = (TextView)view.findViewById(R.id.btItemText);
			textView.setText(item.getLabel());
		}
		
        return view;
    }

}
