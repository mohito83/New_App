/**
 * 
 */

package edu.isi.backpack.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import edu.isi.backpack.R;

import java.util.ArrayList;

/**
 * @author jenniferchen
 */
public class WifiListDialog extends DialogFragment {

    private ListView listView;
    
    private ArrayList<String> items;

    private ArrayAdapter<String> deviceListAdapter;

    private String serviceName;

    public static interface IHandler {
        public void onReturnValue(String device);
        public void onDismissed();
    }

    private IHandler handler;

    public void setHandler(IHandler handler) {
        this.handler = handler;
    }

    public void setServiceName(String name) {
        serviceName = name;
    }

    public void setList(ArrayList<String> items) {
        this.items = items;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.device_name) + ": " + serviceName);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.wifi_list_dialog, null);
        listView = (ListView) view.findViewById(R.id.wifiDeviceList);
        
        deviceListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.bluetooth_list_item, items);
        listView.setAdapter(deviceListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String item = (String) listView.getItemAtPosition(arg2);
                handler.onReturnValue(item);
            }
        });
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.setView(view);

        AlertDialog myBuilder = builder.create();

        return myBuilder;
    }
    
    public void redraw(ArrayList<String> items) {
        ArrayList<String> copy = new ArrayList<String>(items);
        deviceListAdapter.clear();
        deviceListAdapter.addAll(copy);
        deviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDismiss(DialogInterface d) {
        super.onDismiss(d);
        handler.onDismissed();
    }

}
