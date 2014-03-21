
package edu.isi.backpack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.isi.backpack.services.ListenerService;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ListenerService.class));
    }

}
