/**
 * 
 */

package edu.isi.backpack.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import edu.isi.backpack.R;
import edu.isi.backpack.adapters.WifiListAdapter;

import java.util.ArrayList;

/**
 * @author jenniferchen
 */
public class WifiListDialog extends DialogFragment {

    private ListView listView;

    private ArrayList<NsdServiceInfo> items;

    private WifiListAdapter deviceListAdapter;

    private String serviceName;

    public static interface IHandler {
        public void onReturnValue(NsdServiceInfo device);

        public void onDismissed();
    }

    private IHandler handler;

    public void setHandler(IHandler handler) {
        this.handler = handler;
    }

    public void setServiceName(String name) {
        serviceName = name;
    }

    public void setList(ArrayList<NsdServiceInfo> items) {
        this.items = items;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.device_name) + ": " + serviceName);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.wifi_list_dialog, null);
        listView = (ListView) view.findViewById(R.id.wifiDeviceList);

        deviceListAdapter = new WifiListAdapter(getActivity(), items);
        listView.setAdapter(deviceListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                NsdServiceInfo item = (NsdServiceInfo) deviceListAdapter.getItem(arg2);
                handler.onReturnValue(item);
            }
        });
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.setView(view);

        AlertDialog myBuilder = builder.create();

        return myBuilder;
    }

    public void redraw(ArrayList<NsdServiceInfo> items) {
        ArrayList<NsdServiceInfo> copy = new ArrayList<NsdServiceInfo>(items);
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
