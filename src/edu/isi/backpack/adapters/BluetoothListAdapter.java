/**
 * 
 */

package edu.isi.backpack.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.balatarin.android.R;

import java.util.List;

/**
 * @author jenniferchen
 */
public class BluetoothListAdapter extends ArrayAdapter<BluetoothDevice> {

    private Context context;

    /**
     * @param context
     * @param resource
     * @param textViewResourceId
     * @param objects
     */
    public BluetoothListAdapter(Context context, List<BluetoothDevice> objects) {
        super(context, R.layout.bluetooth_list_item, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        BluetoothDevice item = getItem(position);
        // int type = item.getType();

        View view;
        // if (type == BluetoothItem.HEADER){
        // view = inflater.inflate(R.layout.bluetooth_list_header_item, parent,
        // false);
        // TextView textView = (TextView)view.findViewById(R.id.btHeaderText);
        // textView.setText(item.getLabel());
        // }
        // else {
        view = inflater.inflate(R.layout.bluetooth_list_item, parent, false);
        TextView textView = (TextView) view.findViewById(R.id.btItemText);
        // This is to ensure that for Bluetooth device with no name should have
        // their address displayed.
        String itemTxt = item.getName() != null ? item.getName() : item.getAddress();
        String text;
        if (item.getBondState() == BluetoothDevice.BOND_BONDED)
            text = itemTxt + " (<font color=#41A317>" + context.getString(R.string.paired)
                    + "</font>)";
        else
            text = itemTxt + " (<font color=#FF0000>" + context.getString(R.string.unpaired)
                    + "</font>)";
        textView.setText(Html.fromHtml(text));
        // }

        return view;
    }

}
