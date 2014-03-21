/**
 * 
 */

package edu.isi.backpack.bluetooth;

import java.io.Serializable;

import edu.isi.backpack.constants.Constants;

/**
 * This class defines the Info message packet which is sent before starting the
 * actual file transfer and received as an ACK after file transfer is complete.
 * 
 * @author mohit aggarwl
 */
public class InfoMessage implements Serializable, Cloneable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private short type;

    private Payload payload;

    /**
	 * 
	 */
    public InfoMessage() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the type
     */
    public short getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(short type) {
        this.type = type;
    }

    /**
     * @return the payload
     */
    public Payload getPayload() {
        return payload;
    }

    /**
     * @param payload the payload to set
     */
    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    /**
     * For debug purpose only!!
     * 
     * @return
     */
    public String displayInfoData() {
        StringBuilder builder = new StringBuilder();
        builder.append("Type:" + type + "\n");
        if (type == Constants.ACK_DATA) {
            builder.append("ACK Val:" + ((AckPayload) payload).getAck() + "\n");
            builder.append("OldType:" + ((AckPayload) payload).getOrgMsgType() + "\n");
        } else {
            builder.append("Filename:" + ((InfoPayload) payload).getFileName() + "\n");
            builder.append("File Length:" + ((InfoPayload) payload).getLength() + "\n");
        }
        return builder.toString();
    }

    public int getFileSize() {
        return (int) ((InfoPayload) payload).getLength();
    }

    public InfoMessage copy(InfoMessage message) {
        message.setType(getType());
        if (getType() == Constants.ACK_DATA) {
            message.setPayload(((AckPayload) getPayload()).clone());
        } else {
            message.setPayload(((InfoPayload) getPayload()).clone());
        }
        return message;
    }
}
