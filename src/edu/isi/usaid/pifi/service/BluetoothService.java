package edu.isi.usaid.pifi.service;
import edu.isi.usaid.pifi.data.BluetoothItem;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class BluetoothService extends Service{

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int onStartCommand(Intent intent, int flags, int startId)
    {
		BluetoothItem device = intent.getExtras().getParcelable("Device");
		Toast.makeText(this, device.getLabel()+" "+device.getAddress(),
				Toast.LENGTH_LONG).show();
        /*
        START_STICKY runs the service till we explicitly stop the service
         */
        return START_STICKY;
    }
}
