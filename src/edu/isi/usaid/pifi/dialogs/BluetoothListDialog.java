/**
 * 
 */
package edu.isi.usaid.pifi.dialogs;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import edu.isi.usaid.pifi.R;
import edu.isi.usaid.pifi.data.BluetoothItem;
import edu.isi.usaid.pifi.data.BluetoothListAdapter;

/**
 * @author jenniferchen
 *
 */
public class BluetoothListDialog extends DialogFragment {
	
	private ArrayList<BluetoothItem> btItems;
	
	public void setList(ArrayList<BluetoothItem> items){
		btItems = items;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_bluetooth, null);
		ListView listView = (ListView) view.findViewById(R.id.bluetoothList);
		listView.setAdapter(new BluetoothListAdapter(getActivity(), btItems));
		builder.setView(view);
		
		return builder.create();
	}

}
