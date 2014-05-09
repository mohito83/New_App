
package edu.isi.backpack.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
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
import android.content.SharedPreferences;
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
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import edu.isi.backpack.BookmarkManager;
import org.toosheh.android.R;
import edu.isi.backpack.adapters.ContentListAdapter;
import edu.isi.backpack.adapters.DrawerItem;
import edu.isi.backpack.adapters.DrawerListAdapter;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.DownloadConstants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.constants.WifiConstants;
import edu.isi.backpack.dialogs.BluetoothListDialog;
import edu.isi.backpack.dialogs.WifiListDialog;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.services.ConnectionService;
import edu.isi.backpack.services.FileMonitorService;
import edu.isi.backpack.services.ListenerService;
import edu.isi.backpack.services.WifiConnectionService;
import edu.isi.backpack.services.WifiListenerService;
import edu.isi.backpack.tasks.ContentManagementTask;
import edu.isi.backpack.tasks.DeleteAllContentTask;
import edu.isi.backpack.tasks.DeleteContentTask;
import edu.isi.backpack.wifi.WifiServiceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author jenniferchen This is the main activity of the app. It shows a list of
 *         content from the content directory. The activity reads the list of
 *         content from the metadata file, which uses protocol buffers. It shows
 *         the content as a list. User can select a content to view. TODO need
 *         categories for web articles
 */
@SuppressLint("NewApi")
public class ContentListActivity extends Activity implements BookmarkManager {

    public static final String TAG = "ContentListActivity";

    public static final String STATE_SELECTED_DRAWER_ITEMS = "selected_drawer_items";

    /** drawer keywords **/

    // public static final String FILTER_ID_VIDEO = "Video";

    // public static final String FILTER_ID_WEB = "Web";

    public static final String FILTER_ID_ALL = "All";

    public static final String FILTER_ID_STARRED = "Starred";

    private static final String SETTING_BOOKMARKS = "bookmarks";

    private String debugBuildId = "dev";

    private boolean releaseMode = true;

    /**
     * drawer keyword : lable label is different depending on phone's language
     * setting
     **/
    // private HashMap<String, String> drawerLabels = new HashMap<String,
    // String>();

    private DrawerLayout drawerLayout;

    private ListView drawerList;

    private ActionBarDrawerToggle drawerToggle;

    private ArrayList<DrawerItem> drawerItems = new ArrayList<DrawerItem>();

    private DrawerListAdapter drawerListAdapter;

    private File contentDirectory;

    private File metaFile;

    private Media metadata;

    private ListView contentList;

    private TextView categoryFilter;

    private ArrayList<String> categories;

    private ArrayList<Media.Item> contentItems = new ArrayList<Media.Item>();

    private ContentListAdapter contentListAdapter;

    private String selectedCat = FILTER_ID_ALL;

    private String selectedType = FILTER_ID_ALL;

    private String selectedBookmark = FILTER_ID_ALL;

    private Media.Item currentContent = null;

    private SimpleDateFormat packageDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private Menu optionMenu;

    private boolean btDebugMsg = false;

    private HashMap<String, NsdServiceInfo> wifiListItems = new HashMap<String, NsdServiceInfo>();

    private WifiListDialog wifiListDialog;

    private WifiServiceManager wifiServiceManager;

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

//            } else if (i.getAction().equals(WifiConstants.CONNECTION_ESTABLISHED_ACTION)) {
//                MenuItem item = optionMenu.findItem(R.id.action_sync_wifi);
//                item.setIcon(R.drawable.ic_action_sync_wifi_active);
//                item.setEnabled(false);
//            } else if (i.getAction().equals(WifiConstants.CONNECTION_CLOSED_ACTION)) {
//                MenuItem item = optionMenu.findItem(R.id.action_sync_wifi);
//                item.setIcon(R.drawable.ic_action_sync_wifi);
//                item.setEnabled(true);
//
//                String msg = i.getStringExtra(ExtraConstants.STATUS);
//                if (msg != null)
//                    new AlertDialog.Builder(ContentListActivity.this)
//                            .setTitle(R.string.sync_report).setMessage(msg)
//                            .setNeutralButton(R.string.button_ok, null).show();
            } else if (i.getAction().equals(Constants.BOOKMARK_ACTION)) {
                String id = i.getStringExtra(ExtraConstants.ID);
                boolean on = i.getBooleanExtra(ExtraConstants.ON, false);
                if (on)
                    addBookmark(id, true);
                else
                    removeBookmark(id, true);
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

    private SharedPreferences settings;

    private Set<String> bookmarks;

    private Object rowActionMode = null;

    private ArrayList<Media.Item> selectedRowItems = new ArrayList<Media.Item>();

    private ActionMode.Callback rowActionCallback = new ActionMode.Callback() {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            rowActionMode = null;
            selectedRowItems.clear();
            contentListAdapter.removeSelections();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inf = mode.getMenuInflater();
            inf.inflate(R.menu.row_selection, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {

            /*
             * delete content 1. video/article file 2. thumbnail 3. bookmark 4.
             * metadata entry
             */
                case R.id.action_row_delete:
                    // TODO need to make sure not doing sync at the same time

                    final ProgressDialog progress = new ProgressDialog(ContentListActivity.this);
                    progress.setTitle(getString(R.string.deletion_in_progress));
                    progress.setMessage(getString(R.string.deleting));
                    progress.setCancelable(false);
                    progress.show();
                    DeleteContentTask task = new DeleteContentTask(ContentListActivity.this,
                            contentDirectory, progress, metadata, metaFile);
                    Media.Item[] items = new Media.Item[0];
                    task.execute(selectedRowItems.toArray(items));

                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }
    };

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
            Log.i(TAG, "bounded to service, request service name");
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
//        startService(new Intent(this, WifiListenerService.class));

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
        settings = getPreferences(MODE_PRIVATE);
        bookmarks = settings.getStringSet(SETTING_BOOKMARKS, new HashSet<String>());

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

        // drawerLabels.put(FILTER_ID_ALL, getString(R.string.drawer_all));
        // drawerLabels.put(FILTER_ID_VIDEO, getString(R.string.drawer_video));
        // drawerLabels.put(FILTER_ID_WEB, getString(R.string.drawer_article));
        // drawerLabels.put(FILTER_ID_STARRED,
        // getString(R.string.drawer_starred));

        // start file monitor service
        Intent intent = new Intent(this, FileMonitorService.class);
        startService(intent);
        setContentView(R.layout.activity_content);

        // Set up the action bar to show a dropdown list for categories
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        contentList = (ListView) findViewById(R.id.listing);
        categoryFilter = (TextView) findViewById(R.id.category);
        categoryFilter.setVisibility(View.GONE);

        // reload content
        reload(true);

        // setup menu drawer
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to
                                         // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to
                                         // onPrepareOptionsMenu()
            }

        };
        drawerLayout.setDrawerListener(drawerToggle);

        // menu drawer listener
        drawerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                drawerList.setItemChecked(pos, true);
                DrawerItem selItem = drawerItems.get(pos);
                int type = selItem.getType();
                for (DrawerItem item : drawerItems) {
                    if (item != selItem && item.getType() == type) { // clear
                                                                     // other
                                                                     // checks
                                                                     // of
                                                                     // same
                                                                     // type
                        item.setChecked(false);
                    }
                }
                selItem.setChecked(true);
                applyListFilter(selItem);
                drawerLayout.closeDrawer(drawerList);
            }

        });

        // content list listener
        contentList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

                // if not in editing mode, open the content
                if (rowActionMode == null) {
                    currentContent = (Media.Item) contentListAdapter.getItem(pos);
                    Intent intent = new Intent(getApplicationContext(), ContentViewerActivity.class);

                    // if selected a video
                    intent.putExtra(ExtraConstants.CONTENT, currentContent.toByteArray());
                    intent.putExtra(ExtraConstants.BOOKMARK,
                            bookmarks.contains(currentContent.getFilename()));

                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else { // if in editing/deletion mode, add/remove selection
                    boolean select = contentListAdapter.toggleSelection(pos);
                    if (select)
                        selectedRowItems.add(contentListAdapter.getItem(pos));
                    else
                        selectedRowItems.remove(contentListAdapter.getItem(pos));
                }
            }

        });

        // long click on list item
        contentList.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                if (rowActionMode != null)
                    return false;

                selectedRowItems.add(contentListAdapter.getItem(pos));
                contentListAdapter.toggleSelection(pos);
                rowActionMode = startActionMode(rowActionCallback);
                return true;
            }

        });

        // Start bluetooth listener service
        if (!isBtListenerServiceRunning())
            startService(new Intent(this, ListenerService.class));

        // register broadcast receiver
        // local messages
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.NEW_COMMENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.META_UPDATED_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BOOKMARK_ACTION));

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

    @Override
    public void onStart() {
        super.onStart();

        // bind to wifiListenerService
        // this will start the service if not already running
//        Log.i(TAG, "bind to service");
//        bindService(new Intent(this, WifiListenerService.class), wifiListenerServiceConn,
//                Context.BIND_AUTO_CREATE);

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
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            sync();
            return true;
//        } else if (item.getItemId() == R.id.action_sync_wifi) {
//
//            wifiListDialog = new WifiListDialog();
//            wifiListDialog.setInitialList(wifiListItems.values());
//            wifiListDialog.setServiceName(wifiServiceManager.getServiceName());
//            wifiListDialog.setHandler(new WifiListDialog.IHandler() {
//
//                @Override
//                public void onReturnValue(NsdServiceInfo device) {
//                    wifiListDialog.dismiss(); // dismiss dialog will also stops
//                    Intent i = new Intent(getBaseContext(), WifiConnectionService.class);
//                    i.putExtra(ExtraConstants.DEVICE, device);
//                    startService(i);
//                }
//
//                @Override
//                public void onDismissed() {
//                    wifiServiceManager.stopDiscovery();
//                    wifiListItems.clear();
//                    wifiListDialog.redraw(wifiListItems.values());
//                }
//            });
//            wifiListDialog.show(getFragmentManager(), "WifiListDialog");
//
//            // start discovering devices
//            wifiServiceManager.startDiscovery();
//
//            return true;
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
                    Log.d(TAG, apk.getAbsolutePath());

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
        } else if (item.getItemId() == R.id.action_download_today) {
            String packageName = packageDateFormat.format(Calendar.getInstance().getTime());
            downloadContent(DownloadConstants.dailyPackageURLPrefix + packageName + ".zip", null);
            return true;
        } else if (item.getItemId() == R.id.action_download_yesterday) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            String packageName = packageDateFormat.format(yesterday.getTime());
            downloadContent(DownloadConstants.dailyPackageURLPrefix + packageName + ".zip", null);
            return true;
        } else if (item.getItemId() == R.id.action_download_2days) {
            Calendar twoDaysAgo = Calendar.getInstance();
            twoDaysAgo.add(Calendar.DATE, -2);
            String packageName = packageDateFormat.format(twoDaysAgo.getTime());
            downloadContent(DownloadConstants.dailyPackageURLPrefix + packageName + ".zip", null);
            return true;
        } else if (item.getItemId() == R.id.action_delete_all) {
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
                                    ContentListActivity.this, contentDirectory, progress, metadata,
                                    metaFile);
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

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.about) + " " + appName)
                    .setMessage(
                            appName + " v." + version + "\n" + getLocalBluetoothName())
                    .setNeutralButton(R.string.button_close, null).show();
            return true;
        } else
            return super.onOptionsItemSelected(item);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        SparseBooleanArray array = drawerList.getCheckedItemPositions();
        ArrayList<Integer> sel = new ArrayList<Integer>();
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i))
                sel.add(i);
        }
        outState.putIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS, sel);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously state
        if (savedInstanceState.containsKey(STATE_SELECTED_DRAWER_ITEMS)) {
            for (Integer i : savedInstanceState.getIntegerArrayList(STATE_SELECTED_DRAWER_ITEMS)) {
                drawerList.setItemChecked(i, true);
                drawerItems.get(i).setChecked(true);
            }
        }
    }

    @Override
    public boolean onNavigateUp() {
        if (!drawerLayout.isDrawerOpen(Gravity.LEFT))
            drawerLayout.openDrawer(Gravity.LEFT);
        else
            drawerLayout.closeDrawer(Gravity.LEFT);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
//        if (boundWifiListener) {
//            unbindService(wifiListenerServiceConn);
//            boundWifiListener = false;
//        }
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
     * reload metadata and refresh the list
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void reload(boolean firsttime) {

        // read meatadata
        metaFile = new File(contentDirectory, Constants.metaFileName);
        ArrayList<Media.Item> contents = new ArrayList<Media.Item>();

        // content objects from metadata
        try {
            if (metaFile.exists())
                metadata = Media.parseFrom(new FileInputStream(metaFile));
            else
                metadata = Media.newBuilder().build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        contents.addAll(metadata.getItemsList());

        contentItems.clear();
        contentItems.addAll(contents);

        // get a list of categories
        categories = new ArrayList<String>();
        for (Media.Item m : contentItems) {
            for (String c : m.getCategoriesList()) {
                if (!categories.contains(c))
                    categories.add(c);
            }
        }
        String[] cats = new String[categories.size()];
        cats = categories.toArray(cats);

        // setup menu drawer
        drawerItems.clear();

        // bookmarks
        drawerItems.add(new DrawerItem(null, getString(R.string.drawer_bookmarks),
                DrawerItem.HEADER, false));
        drawerItems.add(new DrawerItem(FILTER_ID_ALL, getString(R.string.drawer_all),
                DrawerItem.BOOKMARKS, selectedBookmark.equals(FILTER_ID_ALL)));
        drawerItems.add(new DrawerItem(FILTER_ID_STARRED, getString(R.string.drawer_starred),
                DrawerItem.BOOKMARKS, selectedBookmark.equals(FILTER_ID_STARRED)));

        // content type
        drawerItems.add(new DrawerItem(null, getString(R.string.drawer_content_type),
                DrawerItem.HEADER, false));
        drawerItems.add(new DrawerItem(FILTER_ID_ALL, getString(R.string.drawer_all),
                DrawerItem.CONTENT_TYPE, selectedType.equals(FILTER_ID_ALL)));
        drawerItems.add(new DrawerItem(Media.Item.Type.VIDEO.toString(),
                getString(R.string.drawer_video), DrawerItem.CONTENT_TYPE, selectedType
                        .equals(Media.Item.Type.VIDEO.toString())));
        drawerItems.add(new DrawerItem(Media.Item.Type.HTML.toString(),
                getString(R.string.drawer_article), DrawerItem.CONTENT_TYPE, selectedType
                        .equals(Media.Item.Type.HTML.toString())));
        drawerItems.add(new DrawerItem(Media.Item.Type.AUDIO.toString(),
                getString(R.string.drawer_audio), DrawerItem.CONTENT_TYPE, selectedType
                        .equals(Media.Item.Type.AUDIO.toString())));

        // categories
        drawerItems.add(new DrawerItem(null, getString(R.string.drawer_categories),
                DrawerItem.HEADER, false));
        drawerItems.add(new DrawerItem(FILTER_ID_ALL, getString(R.string.drawer_all),
                DrawerItem.CATEGORY, selectedCat.equals(FILTER_ID_ALL)));
        for (String cat : cats) {
            drawerItems.add(new DrawerItem(cat, cat, DrawerItem.CATEGORY, selectedCat.equals(cat)));
        }
        if (!firsttime)
            applyListFilter();

        // update/init adapter
        if (drawerListAdapter == null) { // first time
            drawerListAdapter = new DrawerListAdapter(this, drawerItems);
            drawerList.setAdapter(drawerListAdapter);
        } else
            drawerListAdapter.notifyDataSetChanged();

        if (contentListAdapter == null) {
            contentListAdapter = new ContentListAdapter(this, contentItems,
                    contentDirectory.getAbsolutePath());
            contentList.setAdapter(contentListAdapter);
        } else
            contentListAdapter.notifyDataSetChanged();
        updateFilter();

    }

    private String setFilter(String filter, String select) {
        if (!select.contains(FILTER_ID_ALL))
            if (filter.isEmpty())
                filter = filter + " " + select;
            else
                filter = filter + "," + select;
        return filter;
    }

    private void updateFilter() {
        if (contentListAdapter.isEmpty()) {
            categoryFilter.setVisibility(View.GONE);
            return;
        } else
            categoryFilter.setVisibility(View.VISIBLE);
        String filter = "";
        filter = setFilter(filter, selectedBookmark);
        filter = setFilter(filter, selectedType);
        filter = setFilter(filter, selectedCat);
        if (filter.length() != 0)
            categoryFilter.setText(getString(R.string.filters) + filter);
        else
            categoryFilter.setVisibility(View.GONE);
    }

    /**
     * no changes to filter, just re-apply to the list
     */
    private void applyListFilter() {
        applyListFilter(null);
    }

    private void applyListFilter(DrawerItem item) {

        // if filter selected/de-selected
        if (item != null) {
            if (item.getType() == DrawerItem.HEADER)
                return;

            if (item.getType() == DrawerItem.BOOKMARKS) {
                String newBookmark = item.getId();
                if (!newBookmark.equals(selectedBookmark))
                    selectedBookmark = newBookmark;
                else
                    return;
            }

            if (item.getType() == DrawerItem.CONTENT_TYPE) {
                String newType = item.getId();
                if (!newType.equals(selectedType)) {
                    selectedType = newType;
                } else
                    return;
            } else if (item.getType() == DrawerItem.CATEGORY) {
                String newCat = item.getId();
                if (!newCat.equals(selectedCat)) {
                    selectedCat = newCat;
                } else
                    return;
            }
        }

        contentListAdapter.clear();

        // Apply filters
        contentListAdapter.addAll(metadata.getItemsList());
        if (!selectedType.equals(FILTER_ID_ALL) || !selectedCat.equals(FILTER_ID_ALL)
                || !selectedBookmark.equals(FILTER_ID_ALL)) {
            for (Media.Item m : metadata.getItemsList()) {
                // type filter
                if (!selectedType.equals(FILTER_ID_ALL)) {
                    if (m.getType() != Media.Item.Type.valueOf(selectedType))
                        contentListAdapter.remove(m); 
                }
                // category filter
                if (!selectedCat.equals(FILTER_ID_ALL)) {
                    if (!m.getCategoriesList().contains(selectedCat))
                        contentListAdapter.remove(m);
                }
                // bookmark filter
                if (!selectedBookmark.equals(FILTER_ID_ALL)) {
                    if (!bookmarks.contains(m.getFilename()))
                        contentListAdapter.remove(m);
                }
            }
        }

        // update list
        contentListAdapter.notifyDataSetChanged();
        updateFilter();
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
        String TAG = "BluetoothFileTransferActivity";
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "No bluetooth capabilities available on the device. Exiting!!");
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
            Log.d(TAG, "Bluetooth is already enabled. Setting up the file transfer");
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

    private void saveBookmarks() {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(SETTING_BOOKMARKS);
        editor.apply();
        editor.putStringSet(SETTING_BOOKMARKS, bookmarks);
        editor.apply();
    }

    /*
     * (non-Javadoc)
     * @see edu.isi.backpack.BookmarkManager#addBookmark(java.lang.String)
     */
    @Override
    public void addBookmark(String id, boolean reload) {
        if (!bookmarks.contains(id)) {
            bookmarks.add(id);
            saveBookmarks();
            if (reload)
                reload(false);
        }
    }

    /*
     * (non-Javadoc)
     * @see edu.isi.backpack.BookmarkManager#removeBookmark(java.lang.String)
     */
    @Override
    public void removeBookmark(String id, boolean reload) {
        if (bookmarks.contains(id)) {
            bookmarks.remove(id);
            saveBookmarks();
            if (reload)
                reload(false);
        }
    }

    /*
     * (non-Javadoc)
     * @see edu.isi.backpack.BookmarkManager#isBookmarked(java.lang.String)
     */
    @Override
    public boolean isBookmarked(String id) {
        if (bookmarks.contains(id))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * @see edu.isi.backpack.BookmarkManager#removeAllBookmarks()
     */
    @Override
    public void removeAllBookmarks(boolean reload) {
        bookmarks.clear();
        saveBookmarks();
        if (reload)
            reload(false);
    }

}
