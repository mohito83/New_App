/**
 * 
 */

package edu.isi.backpack.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;

/**
 * @author jenniferchen
 */
public class WifiServiceManager {

    private static final String TAG = "WifiServiceManager";

    private Context context;

    private NsdManager.DiscoveryListener discoveryListener;

    private NsdManager.ResolveListener resolveListener;

    private NsdManager nsdManager;

    private String wifiServiceName;

    public WifiServiceManager(Context c) {
        context = c;

        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed " + "(" + errorCode + "): " + serviceInfo);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded: " + serviceInfo);

                if (!serviceInfo.getServiceName().equals(wifiServiceName)) {
                    Intent i = new Intent(WifiConstants.WIFI_DEVICE_FOUND_ACTION);
                    i.putExtra(ExtraConstants.DEVICE, serviceInfo.getServiceName());
                    context.sendBroadcast(i);
                }
            }
        };

        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                if (!service.getServiceType().equals(WifiConstants.SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(wifiServiceName)) {
                    Log.d(TAG, "Same machine: " + wifiServiceName);
                } else if (service.getServiceName().startsWith(Constants.WIFI_SERVICE_HEADER)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    nsdManager.resolveService(service, resolveListener);

                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);

                if (!service.getServiceName().equals(wifiServiceName)) {
                    Intent i = new Intent(WifiConstants.WIFI_DEVICE_LOST_ACTION);
                    i.putExtra(ExtraConstants.DEVICE, service.getServiceName());
                    context.sendBroadcast(i);
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void setServiceName(String n) {
        wifiServiceName = n;
    }

    public String getServiceName() {
        return wifiServiceName;
    }

    public void startDiscovery() {
        nsdManager.discoverServices(WifiConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                discoveryListener);
    }

    public void stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

}
