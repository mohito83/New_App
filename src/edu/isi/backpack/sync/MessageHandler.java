/**
 * 
 */

package edu.isi.backpack.sync;

import android.content.ContextWrapper;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;
import edu.isi.backpack.util.BackpackUtils;

import org.balatarin.android.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This class defines methods for handling the messages and data between two
 * devices
 * 
 * @author mohit aggarwl
 */
public class MessageHandler {

    private Connector conn;

    private ContextWrapper wrapper; // to be used for broadcasting
                                    // messaged to the
                                    // ContentListActivity

    private List<Media.Item> contentList;

    private File metaFile;

    /**
     * @param webMetaFile
     * @param metaFile
     */
    public MessageHandler(Connector conn, ContextWrapper wrapper, File metaFile) {
        this.conn = conn;
        this.wrapper = wrapper;
        this.metaFile = metaFile;
    }

    /**
     * This method sends the meta data file and waits a response from the
     * receiver
     * 
     * @param type
     * @param metaFile
     * @return -1 if receiver is failed to receive or process the data sent by
     *         the sender, else 1
     * @throws BluetoothDisconnectedException
     */
    public int sendFullMetaData(short type, File metaFile) throws BluetoothDisconnectedException {
        int result = -1;
        InfoMessage infomessage = BackpackUtils.createInfoMessage(type, metaFile);
        // 1. send the info message informing the data
        conn.sendInfoMessage(infomessage);

        // 2. send actual meta data content
        result = conn.sendFileData(metaFile, infomessage);

        // 3. Receive the ACK message
        if (result >= 0) {
            InfoMessage ackMessage = conn.receiveInfoMessage();
            if (ackMessage.getType() == Constants.ACK_DATA) {
                AckPayload payload = (AckPayload) ackMessage.getPayload();
                if (payload.getAck() == Constants.OK_RESPONSE) {
                    BackpackUtils.broadcastMessage(wrapper,
                            "Sent metadata file: " + metaFile.getName());
                } else {
                    result = -1;
                }
            } else {
                result = -1;
            }
        }

        return result;
    }

    /**
     * This method reads the full meta data file from the socket and calculates
     * the delta which is needed later to prepare the drop package
     * 
     * @param sdir
     * @return
     * @throws BluetoothDisconnectedException
     */
    public int receiveFullMetaData(File sdir) throws BluetoothDisconnectedException {
        int result = -1;
        // 1. Receive the info message
        InfoMessage info = conn.receiveInfoMessage();

        // 2. Receive the actual meta data file
        // just change the name of the received meta data file
        InfoPayload payload = (InfoPayload) info.getPayload();
        String tempMetaFName = payload.getFileName() + "_rx";
        payload.setFileName(tempMetaFName);
        result = conn.readFileData(info, sdir);

        // calculate the delta
        if (result >= 0) {
            BackpackUtils.broadcastMessage(wrapper, "Received metadata file: "
                    + ((InfoPayload) info.getPayload()).getFileName());

            try {
                if (info.getType() == Constants.META_DATA_FULL) {
                    File newMetaFile = new File(sdir, tempMetaFName);
                    contentList = BackpackUtils.getDeltaList(metaFile, newMetaFile);
                    newMetaFile.delete();
                }
            } catch (IOException e) {
                result = -1;
            }
        }

        // 3. Send back ACK message
        short ackVal = result >= 0 ? Constants.OK_RESPONSE : Constants.FAIL_RESPONSE;
        InfoMessage ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA, info.getType(),
                ackVal);
        conn.sendInfoMessage(ackMsg);

        return result;
    }

    /**
     * This method sends the video package to the receiver. The files present in
     * the package are already calculated and stored in videoList.
     * 
     * @param sdir
     * @throws BluetoothDisconnectedException
     */
    public void sendContents(File sdir) throws BluetoothDisconnectedException {
        boolean start = false;
        // send an info message informing start of bulk transfer operation
        InfoMessage bulkMsg = BackpackUtils.createInfoMessage(Constants.START_BULK_TX, null);
        conn.sendInfoMessage(bulkMsg);

        // receive reply from the receiver
        InfoMessage bulkAckMsg = conn.receiveInfoMessage();
        if (bulkAckMsg.getType() == Constants.ACK_DATA) {
            AckPayload payload = (AckPayload) bulkAckMsg.getPayload();
            if (payload.getAck() == Constants.OK_RESPONSE) {
                start = true;
            }
        }

        if (start) {
            for (Media.Item v : contentList) {

                if (v.getType() == Type.VIDEO) {
                    // 1. Send video file
                    // 1.1. send Info message
                    File f = new File(sdir, v.getFilename());
                    InfoMessage info = BackpackUtils
                            .createInfoMessage(Constants.VIDEO_FILE_DATA, f);
                    conn.sendInfoMessage(info);

                    // 1.2. send video contents
                    conn.sendFileData(f, info);

                    // 1.3. receive ACK from the receiver
                    InfoMessage ackMsg = conn.receiveInfoMessage();
                    if (ackMsg.getType() == Constants.ACK_DATA) {
                        AckPayload payload = (AckPayload) ackMsg.getPayload();
                        if (payload.getAck() == Constants.OK_RESPONSE) {
                        } else {
                            continue;
                        }
                    }

                    // 2. Send bitmap image of video
                    // 2.1 send Info message first
                    String thumbnail = v.getThumbnail();
                    File thumbNailFile = new File(sdir, thumbnail);
                    info = BackpackUtils.createInfoMessage(Constants.IMAGE_DATA, thumbNailFile);
                    conn.sendInfoMessage(info);

                    // 2.2 send bitmap content
                    conn.sendFileData(thumbNailFile, info);

                    // 2.3 receive an ACK
                    ackMsg = conn.receiveInfoMessage();
                    if (ackMsg.getType() == Constants.ACK_DATA) {
                        AckPayload payload = (AckPayload) ackMsg.getPayload();
                        if (payload.getAck() == Constants.OK_RESPONSE) {
                        } else {
                        }
                    }

                    // 3. Send temp meta data for video
                    // 3.1 send info message
                    info = BackpackUtils.createInfoMessage(Constants.META_DATA_TMP, f);
                    conn.sendInfoMessage(info);

                    // 3.2 send temp meta data content
                    conn.sendTmpMetaData(v);

                    // 3.3 receive ACK for temp meta data
                    ackMsg = conn.receiveInfoMessage();
                    if (ackMsg.getType() == Constants.ACK_DATA) {
                        AckPayload payload = (AckPayload) ackMsg.getPayload();
                        if (payload.getAck() == Constants.OK_RESPONSE) {
                            BackpackUtils.broadcastMessage(wrapper,
                                    "Sent video file: " + v.getFilename());
                        } else {
                            continue;
                        }
                    }
                } else if (v.getType() == Type.HTML) {
                    // compute the number of images before proceeding further
                    List<File> webImgList = BackpackUtils.getWebArticleImages(sdir, v);

                    // 1. Send web content (.html) file
                    // 1.1 send info message first
                    File f = new File(sdir, v.getFilename());
                    InfoMessage info = BackpackUtils.createInfoMessage(Constants.WEB_FILE_DATA, f);
                    InfoPayload infoPayload = (InfoPayload) info.getPayload();
                    infoPayload.setNoOfImg(webImgList.size());
                    infoPayload.setFileName(v.getFilename());
                    conn.sendInfoMessage(info);

                    // 1.2 send the actual web content file
                    conn.sendFileData(f, info);

                    // 1.3. receive ACK from the receiver
                    InfoMessage ackMsg = conn.receiveInfoMessage();
                    if (ackMsg.getType() == Constants.ACK_DATA) {
                        AckPayload payload = (AckPayload) ackMsg.getPayload();
                        if (payload.getAck() == Constants.OK_RESPONSE) {
                        } else {
                            continue;
                        }
                    }

                    // 2 Send images associated with the web content
                    for (File img : webImgList) {
                        // 2.1 send info message
                        info = BackpackUtils.createInfoMessage(Constants.IMAGE_DATA, img);
                        // modify the web images path.
                        // TODO remove this when embedded images in HTML
                        // functionality is incorporated
                        InfoPayload p = (InfoPayload) info.getPayload();
                        String newImgFileName = v.getFilename().substring(0,
                                v.getFilename().indexOf(".html"))
                                + "/" + img.getName();
                        p.setFileName(newImgFileName);
                        conn.sendInfoMessage(info);

                        // 2.2 Send actual image data
                        conn.sendFileData(img, info);

                        // 2.3 receive ack from the receiver
                        ackMsg = conn.receiveInfoMessage();
                        if (ackMsg.getType() == Constants.ACK_DATA) {
                            AckPayload payload = (AckPayload) ackMsg.getPayload();
                            if (payload.getAck() == Constants.OK_RESPONSE) {
                            } else {
                            }
                        }

                    }

                    // 3 Send tmp web meta data
                    // 3.1 send info message
                    info = BackpackUtils.createInfoMessage(Constants.META_DATA_TMP, f);
                    infoPayload = (InfoPayload) info.getPayload();
                    infoPayload.setFileName(v.getFilename());
                    conn.sendInfoMessage(info);

                    // 3.2 send temp meta data content
                    conn.sendTmpMetaData(v);

                    // 3.3 receive ACK for temp meta data
                    ackMsg = conn.receiveInfoMessage();
                    if (ackMsg.getType() == Constants.ACK_DATA) {
                        AckPayload payload = (AckPayload) ackMsg.getPayload();
                        if (payload.getAck() == Constants.OK_RESPONSE) {
                            BackpackUtils.broadcastMessage(wrapper,
                                    "Sent web content: " + v.getFilename());
                        } else {
                            continue;
                        }
                    }
                }
            }
        }

        // signal stopping of bulk transfer action
        bulkMsg = BackpackUtils.createInfoMessage(Constants.STOP_BULK_TX, null);
        conn.sendInfoMessage(bulkMsg);

        bulkAckMsg = conn.receiveInfoMessage();
        if (bulkMsg.getType() == Constants.ACK_DATA) {
            AckPayload payload = (AckPayload) bulkAckMsg.getPayload();
            if (payload.getAck() == Constants.OK_RESPONSE) {
            }
        }
    }

    /**
     * This method receives the video files and content associated with each
     * video file
     * 
     * @param sdir
     * @throws BluetoothDisconnectedException
     */
    public void receiveFiles(File sdir) throws BluetoothDisconnectedException {
        boolean start = false;
        // wait for bulk data tx signal
        InfoMessage bulkMsg = conn.receiveInfoMessage();
        if (bulkMsg.getType() == Constants.START_BULK_TX) {
            start = true;
        }

        if (start) {
            InfoMessage bulkAckMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
                    Constants.START_BULK_TX, Constants.OK_RESPONSE);
            conn.sendInfoMessage(bulkAckMsg);
        }

        InfoMessage ackMsg = null;
        int result = 0;

        while (start) {
            InfoMessage info = conn.receiveInfoMessage();
            short type = info.getType();

            switch (type) {
                case Constants.STOP_BULK_TX:
                    ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
                            Constants.STOP_BULK_TX, Constants.OK_RESPONSE);
                    conn.sendInfoMessage(ackMsg);
                    start = false;
                    break;

                case Constants.VIDEO_FILE_DATA:
                    result = conn.readFileData(info, sdir);
                    ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA, type,
                            result > 0 ? Constants.OK_RESPONSE : Constants.FAIL_RESPONSE);
                    conn.sendInfoMessage(ackMsg);
                    break;

                case Constants.IMAGE_DATA:
                    result = conn.readFileData(info, sdir);
                    ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA, type,
                            result > 0 ? Constants.OK_RESPONSE : Constants.FAIL_RESPONSE);
                    conn.sendInfoMessage(ackMsg);
                    break;

                case Constants.WEB_FILE_DATA:
                    // create the folder structure similar to that at sender's
                    // side
                    InfoPayload payload = (InfoPayload) info.getPayload();
                    int noOfImgs = payload.getNoOfImg();
                    String fName = payload.getFileName();
                    if (noOfImgs > 0) {
                        fName = fName.substring(0, fName.indexOf(".html"));
                        File webDir = new File(sdir, fName);
                        if (!webDir.exists()) {
                            webDir.mkdirs();
                        }
                    }

                    result = conn.readFileData(info, sdir);
                    ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA, type,
                            result > 0 ? Constants.OK_RESPONSE : Constants.FAIL_RESPONSE);
                    conn.sendInfoMessage(ackMsg);
                    break;

                case Constants.META_DATA_TMP:
                    result = conn.readTmpMetaData(info, sdir, true);
                    ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA, type,
                            result > 0 ? Constants.OK_RESPONSE : Constants.FAIL_RESPONSE);
                    if (result > 0) {
                        BackpackUtils.broadcastMessage(wrapper,
                                wrapper.getString(R.string.received_file) + ": "
                                        + ((InfoPayload) info.getPayload()).getFileName());
                    }
                    conn.sendInfoMessage(ackMsg);
                    break;
            }

        }
    }

}
