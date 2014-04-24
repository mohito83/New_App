/**
 * 
 */

package edu.isi.backpack.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.WifiConstants;

/**
 * @author jenniferchen The listener starts at boot time registers the device on
 *         wifi. Then waits for incoming wifi connection requests.
 */
public class WifiListenerService extends Service {

    public static final String TAG = "WifiListenerService";

    public static final int MSG_REQUEST_SERVICE_NAME = 0;

    private NsdManager mNsdManager;

    private String registeredName = null;

    private boolean registered = false;

    /**
     * Handler of incoming messages from clients.
     */
    @SuppressLint("HandlerLeak")
    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REQUEST_SERVICE_NAME:
                    Log.i(TAG, "service name request received");
                    while (!registered) {
                    } // wait until registration is complete (either suceeded or
                      // failed)
                    Log.i(TAG, "sending back service name: " + registeredName);
                    Message m = Message.obtain(null, WifiListenerService.MSG_REQUEST_SERVICE_NAME);
                    Bundle b = new Bundle();
                    b.putString("service", registeredName);
                    m.setData(b);
                    try {
                        msg.replyTo.send(m);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private RegistrationListener mRegistrationListener = new RegistrationListener() {

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "Wifi registration failed");
            registered = true;
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Wifi registered: " + serviceInfo.getServiceName());
            registeredName = serviceInfo.getServiceName();
            registered = true;
            Log.i(TAG, "Wifi registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Wifi service unregistered: " + serviceInfo.getServiceName());
            registeredName = null;
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "Wifi unregistration failed");
        }

    };

    @Override
    public void onCreate() {
        registered = false;
        registerService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // unregister wifi
        unregisterService();
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mMessenger.getBinder(); // return messenger to send message
    }

    public void registerService() {

        if (registeredName == null) { // if haven't registered
            WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            String macAddr = wifiMan.getConnectionInfo().getMacAddress();
            if (macAddr == null) {
                registered = true;
                return;
            }

            NsdServiceInfo serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceInfo.setServiceName(Constants.WIFI_SERVICE_HEADER + macAddr);
            serviceInfo.setServiceType(WifiConstants.SERVICE_TYPE);
            serviceInfo.setPort(WifiConstants.SERVICE_PORT);

            mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                    mRegistrationListener);

        } else {
            Log.i(TAG, "Wifi already registered");
        }
    }

    public void unregisterService() {
        Log.i(TAG, "Unregistering Wifi service");
        mNsdManager.unregisterService(mRegistrationListener);
    }

}
