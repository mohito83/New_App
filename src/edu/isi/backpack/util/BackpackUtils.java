/**
 * 
 */

package edu.isi.backpack.util;

import android.content.ContextWrapper;
import android.content.Intent;

import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.metadata.MediaProtos.Media;
import edu.isi.backpack.metadata.MediaProtos.Media.Item.Type;
import edu.isi.backpack.sync.AckPayload;
import edu.isi.backpack.sync.InfoMessage;
import edu.isi.backpack.sync.InfoPayload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class defines the utility methods
 * 
 * @author mohit aggarwl
 */
public class BackpackUtils {

    /**
     * This method calculates the delta for the meta data file
     * 
     * @param metaFile
     * @param newMetaFile
     * @return
     * @throws IOException
     */
    public static List<Media.Item> getDeltaList(File metaFile, File newMetaFile) throws IOException {
        List<Media.Item> deltaList = new ArrayList<Media.Item>();
        FileInputStream finorg = new FileInputStream(metaFile);
        FileInputStream finnew = new FileInputStream(newMetaFile);

        Map<String, Media.Item> newMap = new HashMap<String, Media.Item>();

        if (metaFile.length() > 0 && newMetaFile.length() > 0) {
            for (Media.Item v : Media.parseFrom(finnew).getItemsList()) {
                newMap.put(v.getFilename(), v);
            }

            for (Media.Item nVid : Media.parseFrom(finorg).getItemsList()) {
                if (!newMap.containsKey(nVid.getFilename())) {
                    deltaList.add(nVid);
                }
            }
        } else {
            if (metaFile.length() > 0 && newMetaFile.length() == 0) {
                deltaList.addAll(Media.parseFrom(finorg).getItemsList());
            }
        }

        finorg.close();
        finnew.close();

        return deltaList;
    }

    /**
     * @param path
     * @param artilce
     * @return
     */
    public static List<File> getWebArticleImages(File path, Media.Item article) {
        List<File> webImagesPath = new ArrayList<File>();
        if (article.getType() == Type.HTML) {
            String filenName = article.getFilename();
            String webImageFolderName = filenName.substring(0, filenName.indexOf(".html"));
            File imgFolder = new File(path, webImageFolderName);
            if (imgFolder.isDirectory()) {
                webImagesPath = Arrays.asList(imgFolder.listFiles());
            }
        }
        return webImagesPath;
    }

    /**
     * This method creates the info message
     * 
     * @param type
     * @param f
     * @return
     */
    public static InfoMessage createInfoMessage(short type, File f) {
        InfoMessage infomsg = new InfoMessage();
        infomsg.setType(type);
        InfoPayload payload = new InfoPayload();
        if (f != null) {
            payload.setFileName(f.getName());
            payload.setLength(f.length());
        }
        infomsg.setPayload(payload);
        return infomsg;
    }

    /**
     * This method creates the info message with ack payload
     * 
     * @param type
     * @param orgType
     * @param ackVal
     * @return
     */
    public static InfoMessage createInfoMessage(short type, short orgType, short ackVal) {
        InfoMessage infomsg = new InfoMessage();
        infomsg.setType(type);
        AckPayload ackPayload = new AckPayload();
        ackPayload.setAck(ackVal);
        ackPayload.setOrgMsgType(orgType);
        infomsg.setPayload(ackPayload);
        return infomsg;
    }

    /**
     * This method broadcast the messages to the
     * 
     * @param message
     */
    public static void broadcastMessage(ContextWrapper wrapper, String message) {
        Intent i = new Intent();
        i.setAction(Constants.BT_STATUS_ACTION);
        i.putExtra(ExtraConstants.STATUS, message);
        wrapper.sendBroadcast(i);
    }
}
