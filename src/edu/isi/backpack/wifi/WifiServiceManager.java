/**
 * 
 */

package edu.isi.backpack.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;

/**
 * @author jenniferchen
 */
public class WifiServiceManager {

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
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {

                if (!serviceInfo.getServiceName().equals(wifiServiceName)) {
                    Intent i = new Intent(WifiConstants.WIFI_DEVICE_FOUND_ACTION);
                    i.putExtra(ExtraConstants.DEVICE, serviceInfo);
                    context.sendBroadcast(i);
                }
            }
        };

        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                if (!service.getServiceType().equals(WifiConstants.SERVICE_TYPE)) {
                } else if (service.getServiceName().equals(wifiServiceName)) {
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

                if (!service.getServiceName().equals(wifiServiceName)) {
                    Intent i = new Intent(WifiConstants.WIFI_DEVICE_LOST_ACTION);
                    i.putExtra(ExtraConstants.DEVICE, service);
                    context.sendBroadcast(i);
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
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
