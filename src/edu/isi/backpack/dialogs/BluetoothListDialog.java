/**
 * 
 */
package edu.isi.backpack.dialogs;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.isi.backpack.R;
import edu.isi.backpack.adapters.BluetoothListAdapter;

/**
 * @author jenniferchen
 *
 */
public class BluetoothListDialog extends DialogFragment {
	
	private ArrayList<BluetoothDevice> btItems;
	private TextView titleView;
	private ListView listView;
	private ArrayAdapter<BluetoothDevice> adapter;
	public static interface IHandler{
		public void onReturnValue(BluetoothDevice device);
	}
	
	private IHandler handler;
	
	public void setHandler(IHandler handler){
		this.handler = handler;
	}
	
	public void setList(ArrayList<BluetoothDevice> items){
		btItems = items;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_bluetooth, null);
		titleView = (TextView)view.findViewById(R.id.bluetoothTitle);
		listView = (ListView) view.findViewById(R.id.bluetoothList);
		adapter = new BluetoothListAdapter(getActivity(), btItems);
		listView.setAdapter(adapter);
		builder.setView(view);

		return builder.create();
	}
	
	public void redraw(ArrayList<BluetoothDevice> btItemss) {		
		ArrayList<BluetoothDevice> copy = new ArrayList<BluetoothDevice>(btItemss);
		adapter.clear();
		adapter.addAll(copy);
		adapter.notifyDataSetChanged();		
	}
	
	public void setTitle(String title){
		titleView.setText(title);
	}
	 @Override
	  public void onResume() {
	    super.onResume();
	    listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) 
			{
				BluetoothDevice item  = (BluetoothDevice)listView.getItemAtPosition(arg2);
				if(item.getAddress() != null)
				{
					handler.onReturnValue(item);
				}
			}
	    });
	  }
	 
	 
}
