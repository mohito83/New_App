/**
 * 
 */
package edu.isi.usaid.pifi.dialogs;

import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import edu.isi.usaid.pifi.ContentListActivity;
import edu.isi.usaid.pifi.R;
import edu.isi.usaid.pifi.data.BluetoothItem;
import edu.isi.usaid.pifi.data.BluetoothListAdapter;

/**
 * @author jenniferchen
 *
 */
public class BluetoothListDialog extends DialogFragment {
	
	private ArrayList<BluetoothItem> btItems;
	private ListView listView;
	public void setList(ArrayList<BluetoothItem> items){
		btItems = items;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_bluetooth, null);
		listView = (ListView) view.findViewById(R.id.bluetoothList);
		listView.setAdapter(new BluetoothListAdapter(getActivity(), btItems));
		builder.setView(view);
		return builder.create();
	}
	
	 @Override
	  public void onResume() {
	    super.onResume();
	    listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				BluetoothItem item  = (BluetoothItem)listView.getItemAtPosition(arg2);
				Toast.makeText(arg0.getContext(), item.getLabel()+" "+item.getAddress(),
						Toast.LENGTH_LONG).show();
				}
	    	});
	  }
	 
	 
}
