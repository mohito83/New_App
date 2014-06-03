/**
 * 
 */
package edu.isi.backpack.adapters;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
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
 *
 */
public class WifiListAdapter extends ArrayAdapter<NsdServiceInfo> {
    

    public WifiListAdapter(Context context, List<NsdServiceInfo> objects) {
        super(context, R.layout.bluetooth_list_item, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        NsdServiceInfo item = getItem(position);

        View view = inflater.inflate(R.layout.bluetooth_list_item, parent, false);
        TextView textView = (TextView) view.findViewById(R.id.btItemText);
        String text = item.getServiceName();
        textView.setText(Html.fromHtml(text));

        return view;
    }

}
