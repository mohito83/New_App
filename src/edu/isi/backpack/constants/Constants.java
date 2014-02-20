/**
 * 
 */
package edu.isi.backpack.constants;

/**
 * @author jenniferchen
 *
 */
public class Constants {

	public static final int CONTENT_VIEWER_ACTIVITY = 1022;
	
	public static final String NEW_COMMENT_ACTION = "edu.isi.backpack.NewCommentAction";
	
	public static final String META_UPDATED_ACTION = "edu.isi.backpack.MetaUpdatedAction";
	
	public static final String BT_STATUS_ACTION = "edu.isi.backpack.BtStatusAction";
	
	public static final String BT_CONNECTED_ACTION = "edu.isi.backpack.BtConnectedAction";
	
	public static final String BT_DISCONNECTED_ACTION = "edu.isi.backpack.BtDisonnectedAction";
	
	public static final String BOOKMARK_ACTION = "edu.isi.backpack.BookmarkAction";
	
	public static final String contentDirName = "BackpackContent";
	
	public static final String xferDirName = "xfer";
	
	public static final String videoMetaFileName = "videos.dat";
	
	public static final String webMetaFileName = "news.dat";
	

	/****************************************************/
	/*****************Transaction states*****************/
	/****************************************************/
	public static final short META_DATA_EXCHANGE = 1000;
	public static final short FILE_DATA_EXCHANGE = 1001; 
	public static final short SYNC_COMPLETE	=	1002;
	
	
	/**************************************************/
	/*********Codes used for Infomessage types*********/
	/**************************************************/
	public static final short VIDEO_META_DATA_FULL = 1;
	public static final short WEB_META_DATA_FULL = 2;
	public static final short VIDEO_META_DATA_TMP = 3;
	public static final short WEB_META_DATA_TMP = 4;
	public static final short VIDEO_IMAGE_DATA = 5;
	public static final short WEB_IMAGE_DATA = 6;
	public static final short VIDEO_FILE_DATA = 7;
	public static final short WEB_FILE_DATA = 8;
	public static final short START_BULK_TX = 9;
	public static final short STOP_BULK_TX = 10;
	public static final short ACK_DATA = 15;
	public static final short OK_RESPONSE = 200;
	public static final short FAIL_RESPONSE = 400;
	
	
	public static final String VIDEO_THUMBNAIL_ID = "_default.jpg";
	
	/**
	 * This constant defined the timeout period for which your bluetooth device will be visible to other devices.
	 */
	public static final int VISIBILITY_TIMEOUT = 3600;
	
}
