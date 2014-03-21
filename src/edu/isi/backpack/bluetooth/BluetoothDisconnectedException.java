/**
 * 
 */

package edu.isi.backpack.bluetooth;

/**
 * @author jenniferchen
 */
public class BluetoothDisconnectedException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public BluetoothDisconnectedException(String message) {
        super(message);
    }

    public BluetoothDisconnectedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
