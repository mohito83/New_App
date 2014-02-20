/**
 * 
 */
package edu.isi.backpack.adapters;

import android.widget.Checkable;

/**
 * @author jenniferchen
 *
 */
public class DrawerItem implements Checkable{

	public static final int HEADER = 0;
	
	public static final int CONTENT_TYPE = 1;
	
	public static final int CATEGORY = 2;
	
	public static final int BOOKMARKS = 3;
	
	private String label;
	
	private int type;
	
	private boolean checked = false;
	
	public DrawerItem(String label, int type, boolean checked){
		this.label = label;
		this.type = type;
		this.checked = checked;
	}
	
	public String getLabel(){
		return label;
	}
	
	public int getType(){
		return type;
	}

	/* (non-Javadoc)
	 * @see android.widget.Checkable#isChecked()
	 */
	@Override
	public boolean isChecked() {
		return checked;
	}

	/* (non-Javadoc)
	 * @see android.widget.Checkable#setChecked(boolean)
	 */
	@Override
	public void setChecked(boolean arg0) {
		checked = arg0;
	}

	/* (non-Javadoc)
	 * @see android.widget.Checkable#toggle()
	 */
	@Override
	public void toggle() {
		checked = !checked;
	}

}
