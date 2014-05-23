
package edu.isi.backpack.activities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.toosheh.android.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.DownloadConstants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;
import edu.isi.backpack.dialogs.BluetoothListDialog;
import edu.isi.backpack.dialogs.WifiListDialog;
import edu.isi.backpack.fragments.MainContentFragment;
import edu.isi.backpack.services.ConnectionService;
import edu.isi.backpack.services.FileMonitorService;
import edu.isi.backpack.services.ListenerService;
import edu.isi.backpack.services.WifiConnectionService;
import edu.isi.backpack.services.WifiListenerService;
import edu.isi.backpack.tasks.ContentManagementTask;
import edu.isi.backpack.tasks.DeleteAllContentTask;
import edu.isi.backpack.wifi.WifiServiceManager;

/**
 * @author jenniferchen This is the main activity of the app. It shows a list of
 *         content from the content directory. The activity reads the list of
 *         content from the metadata file, which uses protocol buffers. It shows
 *         the content as a list. User can select a content to view. TODO need
 *         categories for web articles
 */
@SuppressLint("NewApi")
public class ContentListActivity extends FragmentActivity {

    public static final String STATE_SELECTED_DRAWER_ITEMS = "selected_drawer_items";

    /** drawer keywords **/

    // public static final String FILTER_ID_VIDEO = "Video";

    // public static final String FILTER_ID_WEB = "Web";

    private String debugBuildId = "dev";

    private boolean releaseMode = true;

    /**
     * drawer keyword : lable label is different depending on phone's language
     * setting
     **/

    private SimpleDateFormat packageDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private Menu optionMenu;

    private boolean btDebugMsg = false;

    private HashMap<String, NsdServiceInfo> wifiListItems = new HashMap<String, NsdServiceInfo>();

    private WifiListDialog wifiListDialog;

    private WifiServiceManager wifiServiceManager;
    
    private MainContentFragment currentSelectedFragment;

    // register to receive message when a new comment is added
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent i) {
            if (i.getAction().equals(Constants.NEW_COMMENT_ACTION)) { // adding
                                                                      // new
                                                                      // comment
                                                                      // String
                                                                      // user =
                                                                      // i.getStringExtra(ExtraConstants.USER);
                // String date = i.getStringExtra(ExtraConstants.DATE);
                // String comment =
                // i.getStringExtra(ExtraConstants.USER_COMMENT);
                // addComment(user, date, comment);
            } else if (i.getAction().equals(Constants.META_UPDATED_ACTION)) { // meta

                reload(false);

            } else if (i.getAction().equals(Constants.BT_STATUS_ACTION)) {
                if (btDebugMsg) {
                    Toast toast = Toast.makeText(ContentListActivity.this,
                            i.getStringExtra(ExtraConstants.STATUS), Toast.LENGTH_SHORT);
                    toast.show();
                }
            } else if (i.getAction().equals(Constants.BT_CONNECTED_ACTION)) {
                MenuItem item = optionMenu.findItem(R.id.action_sync);
                item.setIcon(R.drawable.ic_action_share_active);
                item.setEnabled(false);
            } else if (i.getAction().equals(Constants.BT_DISCONNECTED_ACTION)) {
                MenuItem item = optionMenu.findItem(R.id.action_sync);
                item.setIcon(R.drawable.ic_action_share);
                item.setEnabled(true);

                String msg = i.getStringExtra(ExtraConstants.STATUS);
                if (msg != null)
                    new AlertDialog.Builder(ContentListActivity.this)
                            .setTitle(R.string.sync_report).setMessage(msg)
                            .setNeutralButton(R.string.button_ok, null).show();

                // } else if
                // (i.getAction().equals(WifiConstants.CONNECTION_ESTABLISHED_ACTION))
                // {
                // MenuItem item = optionMenu.findItem(R.id.action_sync_wifi);
                // item.setIcon(R.drawable.ic_action_sync_wifi_active);
                // item.setEnabled(false);
                // } else if
                // (i.getAction().equals(WifiConstants.CONNECTION_CLOSED_ACTION))
                // {
                // MenuItem item = optionMenu.findItem(R.id.action_sync_wifi);
                // item.setIcon(R.drawable.ic_action_sync_wifi);
                // item.setEnabled(true);
                //
                // String msg = i.getStringExtra(ExtraConstants.STATUS);
                // if (msg != null)
                // new AlertDialog.Builder(ContentListActivity.this)
                // .setTitle(R.string.sync_report).setMessage(msg)
                // .setNeutralButton(R.string.button_ok, null).show();
            } else if (i.getAction().equals(WifiConstants.WIFI_DEVICE_FOUND_ACTION)) {
                NsdServiceInfo device = i.getParcelableExtra(ExtraConstants.DEVICE);
                if (device != null && !wifiListItems.containsKey(device.getServiceName())) {
                    wifiListItems.put(device.getServiceName(), device);
                    wifiListDialog.redraw(wifiListItems.values());
                }
            } else if (i.getAction().equals(WifiConstants.WIFI_DEVICE_LOST_ACTION)) {
                NsdServiceInfo device = i.getParcelableExtra(ExtraConstants.DEVICE);
                if (device != null && wifiListItems.containsKey(device.getServiceName())) {
                    wifiListItems.remove(device.getServiceName());
                    wifiListDialog.redraw(wifiListItems.values());
                }
            }

        }

    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!bts.contains(device)) {
                    bts.add(device);
                    dialog.redraw(bts);
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                // setProgressBarIndeterminateVisibility(false);
                /*
                 * setTitle(R.string.select_device); if
                 * (mNewDevicesArrayAdapter.getCount() == 0) { String noDevices
                 * = getResources().getText(R.string.none_found).toString();
                 * mNewDevicesArrayAdapter.add(noDevices); }
                 */
                dialog.setTitle(bts.size() + " " + getString(R.string.devices_found));
                ContentListActivity.this.unregisterReceiver(mReceiver);
            }
        }
    };

    private ArrayList<BluetoothDevice> bts;

    private static final int SYNC_REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothListDialog dialog;

    private Messenger wifiListenerMessenger;

    private boolean boundWifiListener = false;

    private ServiceConnection wifiListenerServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            wifiListenerMessenger = new Messenger(service);
            boundWifiListener = true;
            // request serviceName WifiListenerService
            Message msg = Message.obtain(null, WifiListenerService.MSG_REQUEST_SERVICE_NAME, 0, 0);
            msg.replyTo = incomingMessenger;
            try {
                wifiListenerMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            wifiListenerMessenger = null;
            boundWifiListener = false;
        }
    };

    final Messenger incomingMessenger = new Messenger(new IncomingHandler());

    private File contentDirectory;

    @SuppressLint("HandlerLeak")
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WifiListenerService.MSG_REQUEST_SERVICE_NAME:
                    String s = msg.getData().getString("service");
                    wifiServiceManager.setServiceName(s);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start WifiListenerService using startService so that it will keep
        // running even after unbiding
        // startService(new Intent(this, WifiListenerService.class));

        // check release/debug mode
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(getPackageName(), 0);
            releaseMode = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // init build id
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", getResources()
                .getConfiguration().locale);
        debugBuildId = "dev_" + sdf.format(new Date());

        // restore user preferences
        /*settings = getPreferences(MODE_PRIVATE);
        //bookmarks = settings.getStringSet(SETTING_BOOKMARKS, new HashSet<String>());
        if (settings.getBoolean("first", true)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first", false);
            editor.apply();
            Intent helpIntent = new Intent(getBaseContext(), HelpActivity.class);
            helpIntent.putExtra("type", "splash");
            startActivity(helpIntent);
        }*/

        bts = new ArrayList<BluetoothDevice>();

        // content directory
        File appDir = getExternalFilesDir(null);
        contentDirectory = new File(appDir, Constants.contentDirName);
        if (!contentDirectory.exists()) {
            contentDirectory.mkdir();

            // // TODO this code will eventually go away
            // // check if user has old content directory,
            // // if so, move everything to new directory
            // File sdDir = Environment.getExternalStorageDirectory();
            // File oldContentDir = new File(sdDir, Constants.contentDirName);
            // if (oldContentDir.exists()) {
            // ProgressDialog progress = new
            // ProgressDialog(ContentListActivity.this);
            // progress.setTitle(R.string.updating_title);
            // progress.setMessage(getString(R.string.updating_msg));
            // progress.setCancelable(false);
            // progress.show();
            // UpdateTask task = new UpdateTask(this, oldContentDir,
            // contentDirectory, progress);
            // task.execute();
            // }
        }

        // start file monitor service
        Intent intent = new Intent(this, FileMonitorService.class);
        startService(intent);
        setContentView(R.layout.activity_content_viewer);
        
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);

        Tab tab = actionBar.newTab()
                           .setText(R.string.tab_hot)
                           .setTabListener(new TabListener(
                                   this, "Hot", MainContentFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab()
                       .setText(R.string.tab_recent)
                       .setTabListener(new TabListener(
                               this, "Recent", MainContentFragment.class));
        actionBar.addTab(tab);

        // Set up the action bar to show a dropdown list for categories
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // Start bluetooth listener service
        startService(new Intent(this, ListenerService.class));

        // register broadcast receiver
        // local messages
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.NEW_COMMENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.META_UPDATED_ACTION));

        // global messages (from other processes)
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.BT_STATUS_ACTION));
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.BT_CONNECTED_ACTION));
        registerReceiver(broadcastReceiver, new IntentFilter(Constants.BT_DISCONNECTED_ACTION));
        registerReceiver(broadcastReceiver, new IntentFilter(
                WifiConstants.CONNECTION_ESTABLISHED_ACTION));
        registerReceiver(broadcastReceiver,
                new IntentFilter(WifiConstants.CONNECTION_CLOSED_ACTION));
        registerReceiver(broadcastReceiver,
                new IntentFilter(WifiConstants.WIFI_DEVICE_FOUND_ACTION));
        registerReceiver(broadcastReceiver, new IntentFilter(WifiConstants.WIFI_DEVICE_LOST_ACTION));

        wifiServiceManager = new WifiServiceManager(this);
    }

    public  void reload(boolean b) {
        //TODO call the reload on the appropriate Tab Fragment
    }

    @Override
    public void onStart() {
        super.onStart();

        // bind to wifiListenerService
        // this will start the service if not already running
        // Log.i(TAG, "bind to service");
        // bindService(new Intent(this, WifiListenerService.class),
        // wifiListenerServiceConn,
        // Context.BIND_AUTO_CREATE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.content, menu);
        optionMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        currentSelectedFragment.getDrawerToggle().syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentSelectedFragment.getDrawerToggle().onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            sync();
            return true;
            // } else if (item.getItemId() == R.id.action_sync_wifi) {
            //
            // wifiListDialog = new WifiListDialog();
            // wifiListDialog.setInitialList(wifiListItems.values());
            // wifiListDialog.setServiceName(wifiServiceManager.getServiceName());
            // wifiListDialog.setHandler(new WifiListDialog.IHandler() {
            //
            // @Override
            // public void onReturnValue(NsdServiceInfo device) {
            // wifiListDialog.dismiss(); // dismiss dialog will also stops
            // Intent i = new Intent(getBaseContext(),
            // WifiConnectionService.class);
            // i.putExtra(ExtraConstants.DEVICE, device);
            // startService(i);
            // }
            //
            // @Override
            // public void onDismissed() {
            // wifiServiceManager.stopDiscovery();
            // wifiListItems.clear();
            // wifiListDialog.redraw(wifiListItems.values());
            // }
            // });
            // wifiListDialog.show(getFragmentManager(), "WifiListDialog");
            //
            // // start discovering devices
            // wifiServiceManager.startDiscovery();
            //
            // return true;
        } else if (item.getItemId() == R.id.action_share_app) {

            // find the apk
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities(
                    mainIntent, 0);
            for (ResolveInfo info : pkgAppsList) {
                String packageName = info.activityInfo.applicationInfo.packageName;
                if (packageName.equals(getPackageName())) {
                    File apk = new File(info.activityInfo.applicationInfo.publicSourceDir);

                    // share apk using bluetooth
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setType("image/*"); // set to a type that
                                                    // bluetooth can accept
                    shareIntent.setPackage("com.android.bluetooth"); // force
                                                                     // send
                                                                     // using bt
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apk));
                    startActivity(shareIntent);

                    return true;
                }
            }

            // apk not found (should not happen)
            new AlertDialog.Builder(this).setTitle(R.string.file_missing)
                    .setMessage(R.string.cant_share_app).setNeutralButton(R.string.button_ok, null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {/*
                                                              * R.id.
                                                              * action_download_today
                                                              * ) {
                                                              */
            String packageName = packageDateFormat.format(Calendar.getInstance().getTime());
            downloadContent(DownloadConstants.dailyPackageURLPrefix + packageName + ".zip", null);
            return true;
        } else if (item.getItemId() == R.id.action_tour_app) {
            Intent helpIntent = new Intent(getBaseContext(), HelpActivity.class);
            helpIntent.putExtra("type", "tour");
            startActivity(helpIntent);
            return true;
        } /*
           * else if (item.getItemId() == R.id.action_download_yesterday) {
           * Calendar yesterday = Calendar.getInstance();
           * yesterday.add(Calendar.DATE, -1); String packageName =
           * packageDateFormat.format(yesterday.getTime());
           * downloadContent(DownloadConstants.dailyPackageURLPrefix +
           * packageName + ".zip", null); return true; } else if
           * (item.getItemId() == R.id.action_download_2days) { Calendar
           * twoDaysAgo = Calendar.getInstance(); twoDaysAgo.add(Calendar.DATE,
           * -2); String packageName =
           * packageDateFormat.format(twoDaysAgo.getTime());
           * downloadContent(DownloadConstants.dailyPackageURLPrefix +
           * packageName + ".zip", null); return true; }
           */else if (item.getItemId() == R.id.action_delete_all) {
            // confirm deletion
            new AlertDialog.Builder(this).setTitle(R.string.delete_all_contents)
                    .setMessage(R.string.confirm_delete_all)
                    .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ProgressDialog progress = new ProgressDialog(
                                    ContentListActivity.this);
                            progress.setTitle(R.string.deletion_in_progress);
                            progress.setMessage(getString(R.string.deleting));
                            progress.setCancelable(false);
                            progress.show();
                            DeleteAllContentTask task = new DeleteAllContentTask(
                                    ContentListActivity.this, contentDirectory, progress, currentSelectedFragment.getMetadata(),
                                    currentSelectedFragment.getMetaFile());
                            task.execute();

                        }
                    }).setNegativeButton(R.string.button_no, null).show();
            return true;
        } else if (item.getItemId() == R.id.action_about) {

            // if running in debug mode
            // show build_id as version
            String appName = getString(R.string.app_name);
            String version = getString(R.string.app_version);
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(getPackageName(), 0);
                appName = (String) pm.getApplicationLabel(info);
                if (releaseMode)
                    version = pm.getPackageInfo(getPackageName(), 0).versionName;
                else
                    version = debugBuildId;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            new AlertDialog.Builder(this).setTitle(getString(R.string.about) + " " + appName)
                    .setMessage(appName + " v." + version + "\n" + getLocalBluetoothName())
                    .setNeutralButton(R.string.button_close, null).show();
            return true;
        } else
            return super.onOptionsItemSelected(item);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        SparseBooleanArray array = currentSelectedFragment.getDrawerList().getCheckedItemPositions();
        ArrayList<Integer> sel = new ArrayList<Integer>();
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i))
                sel.add(i);
        }
        outState.putIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS, sel);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        //TODO fix this issue
        // Restore the previously state
        if (savedInstanceState.containsKey(STATE_SELECTED_DRAWER_ITEMS)) {
            for (Integer i : savedInstanceState.getIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS)) {
                currentSelectedFragment.getDrawerList().setItemChecked(i, true);
                currentSelectedFragment.getDrawerItems().get(i).setChecked(true);
            }
        }
    }

    @Override
    public boolean onNavigateUp() {
        if (!currentSelectedFragment.getDrawerLayout().isDrawerOpen(Gravity.LEFT))
            currentSelectedFragment.getDrawerLayout().openDrawer(Gravity.LEFT);
        else
            currentSelectedFragment.getDrawerLayout().closeDrawer(Gravity.LEFT);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        // if (boundWifiListener) {
        // unbindService(wifiListenerServiceConn);
        // boundWifiListener = false;
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        stopService(new Intent(getBaseContext(), ConnectionService.class));
        stopService(new Intent(getBaseContext(), WifiConnectionService.class));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // turn on bluetooth requested due to sync request
        if (requestCode == SYNC_REQUEST_ENABLE_BT && resultCode != RESULT_CANCELED) {
            // if user allowed turning on bt, try to sync again
            sync();
        }
    }

    /**
     * Download new content from URL and save locally (or use existing local
     * file) Then merge or replace current content
     * 
     * @param url - to download content package, if null, will use locaFile
     * @param localFile - where to save downloaded file, will not be deleted
     */
    protected void downloadContent(final String url, final File localFile) {

        // confirm download
        new AlertDialog.Builder(this).setTitle(R.string.download_content)
                .setMessage(R.string.confirm_download)
                .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // local file (could exist or not)

                        ProgressDialog pd = new ProgressDialog(ContentListActivity.this);
                        pd.setTitle(R.string.download_content_package);
                        pd.setMessage(getString(R.string.downloading));
                        pd.setIndeterminate(true);
                        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        pd.setCanceledOnTouchOutside(false);
                        pd.setCancelable(true);

                        // if file doesn't exist, download
                        if (localFile == null || !localFile.exists()) {
                            ContentManagementTask task = new ContentManagementTask(
                                    ContentListActivity.this, url, pd, localFile, contentDirectory,
                                    false);
                            task.execute();
                        }

                        // file has been downloaded before
                        else {
                            ContentManagementTask task = new ContentManagementTask(
                                    ContentListActivity.this, pd, localFile, contentDirectory,
                                    false);
                            task.execute();
                        }
                    }
                }).setNeutralButton(R.string.button_no, null).show();
    }

    /**
     * add new user comment to metadata
     * 
     * @param comment
     */
    // private void addComment(final String user, final String date, final
    // String comment) {
    // if (currentContent == null || comment == null || comment.isEmpty())
    // return;
    //
    // Thread t = new Thread(new Runnable() {
    //
    // @Override
    // public void run() {
    //
    // // Add comments to metadata
    //
    // if (currentContent instanceof Video) {
    // Videos.Builder newVideos = Videos.newBuilder();
    // for (Video video : metadata.getVideoList()) {
    //
    // // copy from original
    // Video.Builder videoBuilder = Video.newBuilder();
    // videoBuilder.mergeFrom(video);
    //
    // // found the video to add comment
    // if (video.getId().equals(((Video) currentContent).getId())) {
    //
    // // create new comment
    // Comment.Builder commentBuilder = Comment.newBuilder();
    // commentBuilder.setUser(user);
    // commentBuilder.setDate(date);
    // commentBuilder.setText(comment);
    //
    // // modify the video snippet by adding comment
    // videoBuilder.addComments(commentBuilder);
    // }
    //
    // newVideos.addVideo(videoBuilder);
    // }
    //
    // // write new meta out and update metadata in memory
    // try {
    // FileOutputStream out = new FileOutputStream(metaFile);
    // newVideos.build().writeTo(out); // write out to file
    // out.close();
    // } catch (FileNotFoundException e) {
    // e.printStackTrace();
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    //
    // } else if (currentContent instanceof Article) {
    // Articles.Builder newArticles = Articles.newBuilder();
    // for (Article article : webMetadata.getArticleList()) {
    //
    // // copy data from original
    // Article.Builder articleBuilder = Article.newBuilder();
    // articleBuilder.mergeFrom(article);
    //
    // // found the article to add comment
    // if (article.getUrl().equals(((Article) currentContent).getUrl())) {
    //
    // // create new comment
    // Comment.Builder commentBuilder = Comment.newBuilder();
    // commentBuilder.setUser(user);
    // commentBuilder.setDate(date);
    // commentBuilder.setText(comment);
    //
    // articleBuilder.addComments(commentBuilder);
    // // modify the article by adding comment
    // }
    //
    // newArticles.addArticle(articleBuilder);
    // }
    //
    // // write new meta out and update metadata in memory
    // try {
    // FileOutputStream out = new FileOutputStream(webMetaFile);
    // newArticles.build().writeTo(out); // write out to file
    // out.close();
    // } catch (FileNotFoundException e) {
    // e.printStackTrace();
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
    //
    // // broadcast metadata updated
    // Intent i = new Intent();
    // i.setAction(Constants.META_UPDATED_ACTION);
    // LocalBroadcastManager.getInstance(ContentListActivity.this).sendBroadcast(i);
    // }
    //
    // });
    //
    // t.start();
    // }

    private void sync() {

        bts.clear();
        /* To check if the device has the bluetooth hardware */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // finish();
            return;
        }

        /*
         * create an Broadcast register and register the event that you are
         * interested in
         */
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        if (!mBluetoothAdapter.isEnabled()) {
            // make your device discoverable
            Intent makeDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            makeDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    Constants.VISIBILITY_TIMEOUT);
            startActivityForResult(makeDiscoverable, SYNC_REQUEST_ENABLE_BT);
            return;
        } else {
        }

        searchForBTDevices();

        dialog = new BluetoothListDialog();
        dialog.setHandler(new BluetoothListDialog.IHandler() {

            @Override
            public void onReturnValue(BluetoothDevice device) {
                dialog.dismiss();
                mBluetoothAdapter.cancelDiscovery();
                Intent bluetoothServiceIntent = new Intent(getBaseContext(),
                        ConnectionService.class);
                bluetoothServiceIntent.putExtra(ExtraConstants.DEVICE, device);
                startService(bluetoothServiceIntent);
            }
        });
        dialog.setList(bts);
        dialog.setDeviceTitle(getLocalBluetoothName());
        dialog.setDeviceVisibility(getLocalBluetoothVisibility());
        dialog.show(getFragmentManager(), "BluetoothListDialog");

    }

    public boolean getLocalBluetoothVisibility() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            return false;
        return true;
    }

    public String getLocalBluetoothName() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = mBluetoothAdapter.getName();
        if (name == null) {
            name = mBluetoothAdapter.getAddress();
        } else {
            name = getString(R.string.device_name) + ": " + name;
        }
        return name;
    }

    private void searchForBTDevices() {

        mBluetoothAdapter.startDiscovery();
    }

    private boolean isBtListenerServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ListenerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This class defines the callback methods of Tab selection, de-selection
     * events
     * 
     * @author mohit aggarwl
     */
    public class TabListener implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class mClass;

        /**
         * Constructor used each time a new tab is created.
         * 
         * @param activity The host Activity, used to instantiate the fragment
         * @param tag The identifier tag for the fragment
         * @param clz The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity activity, String tag, Class clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.replace(android.R.id.content,mFragment);
            }
            currentSelectedFragment = (MainContentFragment)mFragment;
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
            mFragment = null;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }

    }

    public boolean isBookmarked(String filename) {
        return currentSelectedFragment.isBookmarked(filename);
    }

    public void removeBookmark(String filename, boolean b) {
        currentSelectedFragment.removeBookmark(filename, b);
    }

    public void addBookmark(String filename, boolean b) {
        currentSelectedFragment.addBookmark(filename, b);
    }

    public void removeAllBookmarks(boolean b) {
        currentSelectedFragment.removeAllBookmarks(b);
    }
}
