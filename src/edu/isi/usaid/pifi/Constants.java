/**
 * 
 */
package edu.isi.usaid.pifi;

/**
 * @author jenniferchen
 *
 */
public class Constants {

	public static final int CONTENT_VIEWER_ACTIVITY = 1022;
	
	public static final String NEW_COMMENT_ACTION = "edu.isi.usaid.pifi.NewCommentAction";
	
	public static final String META_UPDATED_ACTION = "edu.isi.usaid.pifi.MetaUpdatedAction";
	
	public static final String BT_STATUS_ACTION = "edu.isi.usaid.pifi.BtStatusAction";
	
	public static final String BOOKMARK_ACTION = "edu.isi.usaid.pifi.BookmarkAction";
	
	public static final String contentDirName = "BackpackContent";
	
	public static final String xferDirName = "xfer";
	
	public static final String metaFileName = "videos.dat";
	
	public static final String webMetaFileName = "news.dat";
	
	public static final String defaultContentURL = "http://107.20.184.189/vids/download?filename=%2Fvol%2Ftmp%2FBackpackContent.zip";
	

	/*
	 * Flag for no data transfer
	 */
	public static final short NO_DATA_META = 0;
	
	public static final short META_DATA_RECEIVED=1000;
	
	public static final int META_DATA_TO_SLAVE = 1001;
	
	public static final int DATA_FROM_SLAVE = 1002;
	
	public static final short META_TO_MASTER = 1003;
	/*
	 * Flag for identifying that data is from master
	 */
	public static final short DATA_FROM_MASTER = 1004;
	/*
	 * Flag for identifying data to master
	 */
	public static final short DATA_TO_MASTER = 1002;
	/*
	 * Flag for identifying meta data from master
	 */
	public static final short META_FROM_MASTER = 1003;
	
	
	public static final short OK_RESPONSE = 200;
	public static final short FAIL_RESPONSE = 400;
	

	public static final String VIDEO_THUMBNAIL_ID = "_default.jpg";
	
}
