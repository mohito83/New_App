/**
 * 
 */

package edu.isi.backpack.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.toosheh.android.R;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import edu.isi.backpack.BookmarkManager;
import edu.isi.backpack.activities.ContentListActivity;
import edu.isi.backpack.activities.ContentViewerActivity;
import edu.isi.backpack.adapters.ContentListAdapter;
import edu.isi.backpack.adapters.DrawerItem;
import edu.isi.backpack.adapters.DrawerListAdapter;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.tasks.DeleteContentTask;

/**
 * @author mohit aggarwl
 */
public class MainContentFragment extends Fragment implements BookmarkManager {

    private Context appContext;
    private HashMap<String, String> drawerLabels = new HashMap<String, String>();
    public static final String FILTER_ID_ALL = "All";
    public static final String FILTER_ID_STARRED = "Starred";
    private static final String SETTING_BOOKMARKS = "bookmarks";
    private String selectedCat = FILTER_ID_ALL;
    private String selectedType = FILTER_ID_ALL;
    private String selectedBookmark = FILTER_ID_ALL;
    private Set<String> bookmarks = new HashSet<String>();
    private ListView contentList;
    private TextView categoryFilter;
    private ArrayList<String> categories;
    private ArrayList<Media.Item> contentItems = new ArrayList<Media.Item>();
    private DrawerListAdapter drawerListAdapter;
    private Media.Item currentContent = null;
    private ActionBarDrawerToggle drawerToggle;
    private ArrayList<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private Object rowActionMode = null;
    private Media metadata;
    private ContentListAdapter contentListAdapter;
    private ArrayList<Media.Item> selectedRowItems = new ArrayList<Media.Item>();
    private File contentDirectory;
    private File metaFile;
    private SharedPreferences settings;
    
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

                    final ProgressDialog progress = new ProgressDialog(appContext);
                    progress.setTitle(getString(R.string.deletion_in_progress));
                    progress.setMessage(getString(R.string.deleting));
                    progress.setCancelable(false);
                    progress.show();
                    DeleteContentTask task = new DeleteContentTask((ContentListActivity)appContext,
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
    
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent i) {
            if (i.getAction().equals(Constants.BOOKMARK_ACTION)) {
                String id = i.getStringExtra(ExtraConstants.ID);
                boolean on = i.getBooleanExtra(ExtraConstants.ON, false);
                if (on)
                    addBookmark(id, true);
                else
                    removeBookmark(id, true);
            }
        }
    };

    public MainContentFragment newInstance(Context appContext) {
        MainContentFragment f = new MainContentFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerLabels.put(FILTER_ID_ALL, getString(R.string.drawer_all));
        drawerLabels.put(Media.Item.Type.VIDEO.toString(), getString(R.string.drawer_video));
        drawerLabels.put(Media.Item.Type.HTML.toString(), getString(R.string.drawer_article));
        drawerLabels.put(Media.Item.Type.AUDIO.toString(), getString(R.string.drawer_audio));
        drawerLabels.put(FILTER_ID_STARRED, getString(R.string.drawer_starred));
        
        // content directory
        appContext = getActivity();
        File appDir = appContext.getExternalFilesDir(null);
        contentDirectory = new File(appDir, Constants.contentDirName);
        if (!contentDirectory.exists()) {
            contentDirectory.mkdir();
        }
        
        LocalBroadcastManager.getInstance(appContext).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BOOKMARK_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_content, container,
                false);

        drawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawer_layout);
        drawerList = (ListView) rootView.findViewById(R.id.left_drawer);
        contentList = (ListView) rootView.findViewById(R.id.listing);
        categoryFilter = (TextView) rootView.findViewById(R.id.category);
        categoryFilter.setVisibility(View.GONE);

        // reload content
        reload(true);

        // setup menu drawer
        drawerToggle = new ActionBarDrawerToggle((Activity)appContext, drawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                ((Activity)appContext).invalidateOptionsMenu(); // creates call to
                                         // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                ((Activity)appContext).invalidateOptionsMenu(); // creates call to
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
                    Intent intent = new Intent(appContext, ContentViewerActivity.class);

                    // if selected a video
                    intent.putExtra(ExtraConstants.CONTENT, currentContent.toByteArray());
                    intent.putExtra(ExtraConstants.BOOKMARK,
                            bookmarks.contains(currentContent.getFilename()));

                    startActivity(intent);
                    ((Activity)appContext).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
                rowActionMode = ((Activity)appContext).startActionMode(rowActionCallback);
                return true;
            }

        });
        
        return rootView;
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

    private void saveBookmarks() {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(SETTING_BOOKMARKS);
        editor.apply();
        editor.putStringSet(SETTING_BOOKMARKS, bookmarks);
        editor.apply();
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
            /*for (String c : m.getCategoriesList()) {
                if (!categories.contains(c))
                    categories.add(c);
            }*/
            //only the first item in the category list is important
            if(!categories.contains(m.getCategoriesList().get(0))){
                categories.add(m.getCategoriesList().get(0));
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
        // drawerItems.add(new DrawerItem(Media.Item.Type.AUDIO.toString(),
        // getString(R.string.drawer_audio), DrawerItem.CONTENT_TYPE,
        // selectedType
        // .equals(Media.Item.Type.AUDIO.toString())));

        // categories
        drawerItems.add(new DrawerItem(null,
                getString(R.string.drawer_categories),
                DrawerItem.HEADER, false));
        drawerItems.add(new DrawerItem(FILTER_ID_ALL,
                getString(R.string.drawer_all),
                DrawerItem.CATEGORY, selectedCat.equals(FILTER_ID_ALL)));
        for (String cat : cats) {
            drawerItems.add(new DrawerItem(cat, cat, DrawerItem.CATEGORY,
                    selectedCat.equals(cat)));
        }
        if (!firsttime)
            applyListFilter();

        // update/init adapter
        if (drawerListAdapter == null) { // first time
            drawerListAdapter = new DrawerListAdapter(appContext, drawerItems);
            drawerList.setAdapter(drawerListAdapter);
        } else
            drawerListAdapter.notifyDataSetChanged();

        if (contentListAdapter == null) {
            contentListAdapter = new ContentListAdapter((ContentListActivity)appContext, contentItems,
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
        categoryFilter.setVisibility(View.VISIBLE);
        String filter = "";
        filter = setFilter(filter, drawerLabels.get(selectedBookmark));
        filter = setFilter(filter, drawerLabels.get(selectedType));
        // filter = setFilter(filter, drawerLabels.get(selectedCat));
        if (selectedCat != null && selectedCat.length() > 0) {
            filter = filter + "," + selectedCat;
        }
        if (filter.length() != 0)
            categoryFilter.setText(getString(R.string.filters) + filter);
        else
            categoryFilter.setVisibility(View.GONE);
    }

    /**
     * @return the drawerToggle
     */
    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    /**
     * @return the drawerList
     */
    public ListView getDrawerList() {
        return drawerList;
    }

    /**
     * @return the drawerItems
     */
    public ArrayList<DrawerItem> getDrawerItems() {
        return drawerItems;
    }

    /**
     * @return the drawerLayout
     */
    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    /**
     * @return the metadata
     */
    public Media getMetadata() {
        return metadata;
    }

    /**
     * @return the metaFile
     */
    public File getMetaFile() {
        return metaFile;
    }
    
    
}
