/**
 * 
 */
package edu.isi.usaid.pifi.data;

/**
 * @author jenniferchen
 *
 */
public class BluetoothItem {
	
	public static final int HEADER = 0;
	
	public static final int KNOWN_BT = 1;
	
	public static final int UNKNOWN_BT = 2;
	
	private String label;
	
	private String address;
	
	private int type;
	/**
	 * 
	 * @param label
	 * @param type - HEADER or KNOWN_BT or UNKNOWN_BT
	 */
	public BluetoothItem(String label, String address, int type){
		this.label = label;
		this.address = address;
		this.type = type;
	}
	
	public String getLabel(){
		return label;
	}
	
	public String getAddress(){
		return address;
	}
	
	public int getType(){
		return type;
	}

}
