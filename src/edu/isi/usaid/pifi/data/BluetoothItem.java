/**
 * 
 */
package edu.isi.usaid.pifi.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author jenniferchen
 *
 */
public class BluetoothItem implements Parcelable{
	
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
	/*
	 * Constructor to use when re-constructing object from a parcel
	 */
	public BluetoothItem(Parcel in)
	{
		readFromParcel(in);
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

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel destination, int flag) {
		// TODO Auto-generated method stub
		destination.writeString(label);
		destination.writeString(address);
		destination.writeInt(type);
		
	}

	public void readFromParcel(Parcel in)
	{
		label = in.readString();
		address = in.readString();
		type = in.readInt();
	}
	/*
	 * This field is needed for Android to be able to create new objects, individually or as arrays. 
	 * This also means that you can use use the default constructor 
	 * to create the object and use another method to hyrdate it as necessary.
	 */
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() { 
		public BluetoothItem createFromParcel(Parcel in) 
		{ 
			return new BluetoothItem(in); 
		}   
		public BluetoothItem[] newArray(int size) 
		{ 
			return new BluetoothItem[size]; 
		} 
	}; 

}
