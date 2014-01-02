/**
 * 
 */
package edu.isi.usaid.pifi.services;

/**
 * This class defines information about the actual payload to be sent before
 * each transaction
 * 
 * @author mohit aggarwl
 * 
 */
public class InfoPayload implements Payload {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long length;
	private String fileName;
	private int noOfImg;

	public InfoPayload() {
		length = 0L;
		fileName = "";
		noOfImg = 0;
	}

	/**
	 * @return the length
	 */
	public long getLength() {
		return length;
	}

	/**
	 * @param length
	 *            the length to set
	 */
	public void setLength(long length) {
		this.length = length;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public InfoPayload clone(){
		InfoPayload payload = new InfoPayload();
		payload.setFileName(getFileName());
		payload.setLength(getLength());
		return payload;
	}

	/**
	 * @return the noOfImg
	 */
	public int getNoOfImg() {
		return noOfImg;
	}

	/**
	 * @param noOfImg the noOfImg to set
	 */
	public void setNoOfImg(int noOfImg) {
		this.noOfImg = noOfImg;
	}
}
