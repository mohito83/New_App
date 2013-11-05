/**
 * 
 */
package edu.isi.usaid.pifi.dialogs;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
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
	private BroadcastReceiver bluetoothReceiver;
	private ListView listView;
	private ArrayAdapter<BluetoothItem> adapter;
	public static interface IHandler{
		public void onReturnValue(BluetoothItem device);
	}
	
	private IHandler handler;
	
	public void setHandler(IHandler handler){
		this.handler = handler;
	}
	
	public void setList(ArrayList<BluetoothItem> items){
		btItems = items;
	}
	
	@Override
	public void onDestroy() {
		// disable the bluetooth broadcast receiver.
		super.onDestroy();
		this.getActivity().unregisterReceiver(bluetoothReceiver);	
	}
	
	public void setReceiver(BroadcastReceiver receiver) {
		bluetoothReceiver = receiver;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_bluetooth, null);
		listView = (ListView) view.findViewById(R.id.bluetoothList);
		adapter = new BluetoothListAdapter(getActivity(), btItems);
		listView.setAdapter(adapter);
		builder.setView(view);

		return builder.create();
	}
	
	public void redraw(ArrayList<BluetoothItem> btItemss) {		
		ArrayList<BluetoothItem> copy = new ArrayList<BluetoothItem>(btItemss);
		adapter.clear();
		adapter.addAll(copy);
		adapter.notifyDataSetChanged();		
	}
	
	 @Override
	  public void onResume() {
	    super.onResume();
	    listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) 
			{
				BluetoothItem item  = (BluetoothItem)listView.getItemAtPosition(arg2);
				if(item.getAddress() != null)
				{
					handler.onReturnValue(item);
				}
			}
	    });
	  }
	 
	 
}
