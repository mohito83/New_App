/**
 * 
 */

package edu.isi.backpack.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.WifiConstants;

/**
 * @author jenniferchen
 */
public class WifiListenerService extends Service {

    public static final String TAG = "WifiListenerService";

    private NsdManager mNsdManager;

    private String registeredName = null;

    private RegistrationListener mRegistrationListener = new RegistrationListener() {

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "Wifi registration failed");
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "Wifi registered: " + serviceInfo.getServiceName());
            registeredName = serviceInfo.getServiceName();
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
        // Debug.waitForDebugger();
        registerService();
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
        return null;
    }

    public void registerService() {

        if (registeredName == null) { // if haven't registered
            Log.i(TAG, "Register wifi");
            WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            String macAddr = wifiMan.getConnectionInfo().getMacAddress();
            // String phoneModel = android.os.Build.MODEL;

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
