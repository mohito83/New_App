/**
 * 
 */

package edu.isi.backpack.sync;

/**
 * This class defines the ACK payload to be sent after each transaction
 * 
 * @author mohit aggarwl
 */
public class AckPayload implements Payload {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private short ack;

    private short orgMsgType;

    public AckPayload() {

    }

    /**
     * @return the ack
     */
    public short getAck() {
        return ack;
    }

    /**
     * @param ack the ack to set
     */
    public void setAck(short ack) {
        this.ack = ack;
    }

    /**
     * @return the orgMsgType
     */
    public short getOrgMsgType() {
        return orgMsgType;
    }

    /**
     * @param orgMsgType the orgMsgType to set
     */
    public void setOrgMsgType(short orgMsgType) {
        this.orgMsgType = orgMsgType;
    }

    public AckPayload clone() {
        AckPayload payload = new AckPayload();
        payload.setAck(getAck());
        payload.setOrgMsgType(getOrgMsgType());
        return payload;
    }
}
