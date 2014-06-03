/**
 * 
 */

package edu.isi.backpack.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ScaleXSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import edu.isi.backpack.adapters.BluetoothListAdapter;

import org.balatarin.android.R;

import java.util.ArrayList;

/**
 * @author jenniferchen
 */
public class BluetoothListDialog extends DialogFragment {

    private ArrayList<BluetoothDevice> btItems;

    private TextView titleView;

    private String Devicetitle;

    private boolean DeviceVisibility = false;

    private ListView listView;

    private ArrayAdapter<BluetoothDevice> adapter;

    public static interface IHandler {
        public void onReturnValue(BluetoothDevice device);
    }

    private IHandler handler;

    public void setHandler(IHandler handler) {
        this.handler = handler;
    }

    public void setList(ArrayList<BluetoothDevice> items) {
        btItems = items;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        //Devicetitle = getActivity().getString(R.string.default_device_title);
        View view = inflater.inflate(R.layout.dialog_bluetooth, null);
        titleView = (TextView) view.findViewById(R.id.bluetoothTitle);
        listView = (ListView) view.findViewById(R.id.bluetoothList);
        TextView Lview = (TextView) view.findViewById(R.id.bluetoothVisibility);
        if (!DeviceVisibility)
            Lview.setVisibility(View.VISIBLE);
        else
            Lview.setVisibility(View.GONE);
        adapter = new BluetoothListAdapter(getActivity(), btItems);
        listView.setAdapter(adapter);
        builder.setView(view);
        SpannableStringBuilder ssBuilser = new SpannableStringBuilder(Devicetitle);
        ScaleXSpan span1 = new ScaleXSpan(0);
        ssBuilser.setSpan(span1, 0, ssBuilser.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        builder.setTitle(ssBuilser);
        builder.setNegativeButton(R.string.button_cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        builder.setPositiveButton(R.string.button_setting, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intentBluetooth = new Intent();
                intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intentBluetooth);

            }
        });
        AlertDialog myBuilder = builder.create();
        return myBuilder;
    }

    public void redraw(ArrayList<BluetoothDevice> btItemss) {
        ArrayList<BluetoothDevice> copy = new ArrayList<BluetoothDevice>(btItemss);
        adapter.clear();
        adapter.addAll(copy);
        adapter.notifyDataSetChanged();
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    public void setDeviceTitle(String title) {
        if (title != null)
            Devicetitle = title;
    }

    public void setDeviceVisibility(boolean val) {
        DeviceVisibility = val;
    }

    @Override
    public void onResume() {
        super.onResume();
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                BluetoothDevice item = (BluetoothDevice) listView.getItemAtPosition(arg2);
                if (item.getAddress() != null) {
                    handler.onReturnValue(item);
                }
            }
        });
    }

}
