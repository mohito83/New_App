/**
 * 
 */
package edu.isi.usaid.pifi;

/**
 * @author jenniferchen
 *
 */
public interface BookmarkManager {

	public void addBookmark(String id);
	public void removeBookmark(String id);
	public boolean isBookmarked(String id);
}
