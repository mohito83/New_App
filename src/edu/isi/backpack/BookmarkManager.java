/**
 * 
 */
package edu.isi.backpack;

/**
 * @author jenniferchen
 *
 */
public interface BookmarkManager {

	public void addBookmark(String id, boolean reload);
	public void removeBookmark(String id, boolean reload);
	public boolean isBookmarked(String id);
	public void removeAllBookmarks(boolean reload);
}
