/**
 * 
 */

package edu.isi.backpack.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.widget.RemoteViews;

import com.google.protobuf.GeneratedMessage;

import edu.isi.backpack.metadata.MediaProtos.Media;

import org.balatarin.android.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Date;

/**
 * This class defines methods to perform data transaction
 * 
 * @author mohit aggarwl
 */
public class Connector {

    private Context context;

    private InputStream mmInputStream;

    private OutputStream mmOutputSteam;

    private Notification notification = null;

    private NotificationManager notificationManager = null;

    private boolean notify = true, destroy = false;

    private Thread t;

    private static byte buffer[] = new byte[8192];

    /**
	 * 
	 */
    public Connector(InputStream in, OutputStream out, Context c) {
        context = c;
        mmInputStream = in;
        mmOutputSteam = out;
        notification = new Notification();
        notification.icon = R.drawable.ic_launcher;
        notification.tickerText = "BackPack";
        notification.when = System.currentTimeMillis();
        notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
        notification.contentView = new RemoteViews(c.getPackageName(), R.layout.download_progress);
        // notification.contentIntent = pendingIntent;
        notification.contentView.setImageViewResource(R.drawable.ic_launcher,
                R.drawable.ic_action_share);
        notification.contentView.setTextViewText(R.id.status_text,
                c.getString(R.string.sync_in_progress));
        notification.contentView.setProgressBar(R.id.status_progress, 100, 2, false);
        notificationManager = (NotificationManager) c
                .getSystemService(Context.NOTIFICATION_SERVICE);
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (destroy)
                            break;
                        Thread.sleep(2500);
                        notify = true;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        });
        t.start();
    }

    public void cancelNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(42);
        }
        destroy = true;
    }

    /**
     * This method sends the InfoMessage
     * 
     * @param infoMessage
     * @return 0 if successful else -1 for failure
     */
    public int sendInfoMessage(InfoMessage infoMessage) {
        try {
            ObjectOutputStream oOutStream = new ObjectOutputStream(mmOutputSteam);
            oOutStream.writeObject(infoMessage);
        } catch (IOException e) {
            return -1;
        }
        return 0;
    }

    /**
     * This method receives the InfoMessage object. It implements a timeout of 2
     * seconds for receiving the data.
     * 
     * @return null for failure
     * @throws BluetoothDisconnectedException
     */
    public InfoMessage receiveInfoMessage() throws BluetoothDisconnectedException {
        InfoMessage info = null;
        try {
            ObjectInputStream oInputStream = new ObjectInputStream(mmInputStream);
            info = (InfoMessage) oInputStream.readObject();
        } catch (StreamCorruptedException e) {
        } catch (OptionalDataException e) {
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        }

        // bt disconnection occurred
        if (info == null) {
            throw new BluetoothDisconnectedException("Bluetooth Disconnected");
        }

        return info;
    }

    /**
     * This method read data from the file and send it across the Bluetooth
     * socket.
     * 
     * @param f
     * @return -1 if failure else number of bytes sent
     */
    public int sendFileData(File f, InfoMessage info) {
        int result = -1;
        int totalBytesSent = 0;
        int len = 0;
        try {
            FileInputStream fin = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fin, 8 * 1024);
            BufferedOutputStream mOutputSteam = new BufferedOutputStream(mmOutputSteam);
            while ((len = bis.read(buffer)) != -1) {
                if (notify) {
                    notify = false;
                    notification.contentView.setTextViewText(R.id.status_text,
                            context.getString(R.string.sending_file) + ": " + f.getName() + "("
                                    + (totalBytesSent / 1000) + "Kb/" + (info.getFileSize() / 1000)
                                    + "Kb)");
                    notification.contentView.setProgressBar(R.id.status_progress,
                            info.getFileSize(), (int) totalBytesSent + info.getFileSize() / 15,
                            false);
                    notificationManager.notify(42, notification);
                }
                mOutputSteam.write(buffer, 0, len);
                totalBytesSent += len;
            }
            bis.close();
            fin.close();
            mOutputSteam.flush();
            mmOutputSteam.flush();
            result = totalBytesSent;
        } catch (FileNotFoundException e) {
            result = -1;
        } catch (IOException e) {
            result = -1;
        }

        return result;
    }

    /**
     * This method reads the file data from the BluetoothSocket and save it on
     * the mobile device.
     * 
     * @param info
     * @param sdir
     * @return -1 if failure else the number of bytes received on the line
     */
    public int readFileData(InfoMessage info, File sdir) {
        int result = -1;
        InfoPayload payload = (InfoPayload) info.getPayload();

        String fName = payload.getFileName();

        String tmpFName = fName + ".tmp";
        long fLen = payload.getLength();
        // create a file
        // append .tmp to the file name before the file transfer starts
        File f = new File(sdir, tmpFName);
        if (f.exists())
            f.delete(); // delete any existing unfinished file
        boolean saveToFile = true;
        try {
            try {
                f.createNewFile();
            } catch (Exception e) {
                saveToFile = false;
            }

            FileOutputStream fos = null;
            if (saveToFile) {
                fos = new FileOutputStream(f);
            }
            BufferedOutputStream bos = new BufferedOutputStream(fos, 8 * 1024);
            int bytesRead = 0;
            BufferedInputStream mInputStream = new BufferedInputStream(mmInputStream);

            // track when we have been blocked for too long
            Date blockStart = null;
            Date now = null;

            while (bytesRead < fLen) { // read exactly fLen
                if (notify) {
                    notify = false;
                    notification.contentView.setTextViewText(R.id.status_text,
                            context.getString(R.string.receiving_file) + ": " + fName + "("
                                    + (bytesRead / 1000) + "Kb/" + (fLen / 1000) + "Kb)");
                    notification.contentView.setProgressBar(R.id.status_progress, (int) fLen,
                            (int) bytesRead + info.getFileSize() / 15, false);
                    notificationManager.notify(42, notification);
                }

                if (mmInputStream.available() > 0) {
                    blockStart = null;

                    int read = mInputStream.read(buffer, 0,
                            Math.min((int) fLen - bytesRead, buffer.length));
                    if (saveToFile) {
                        bos.write(buffer, 0, read);
                    }
                    bytesRead += read;
                } else if (blockStart == null) {
                    blockStart = new Date(); // started blocking
                } else {
                    now = new Date();
                    // if we have been block for 15 seconds
                    // quit this file
                    if ((now.getTime() - blockStart.getTime()) > 15000) {
                        bytesRead = -1;
                        break; // quit receiving this file
                    }
                }
            }

            if (saveToFile && fos != null) {
                bos.flush();
                bos.close();
                fos.flush();
                fos.close();
                result = bytesRead;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        // after file transfer is complete then rename the file to drop
        // .tmp
        if (result >= 0) {
            File finalFile = new File(sdir, fName);
            boolean isSuccess = f.renameTo(finalFile);
        }

        return result;
    }

    /**
     * This method reads and saves the temporary meta data content to the device
     * 
     * @param info
     * @param sdir
     * @param isVideo
     * @return -1 if failure else 1
     */
    public int readTmpMetaData(InfoMessage info, File sdir, boolean isVideo) {
        int result = -1;
        InfoPayload payload = (InfoPayload) info.getPayload();

        FileOutputStream fos;
        String fName = payload.getFileName();
        boolean isSuccess;
        try {
            Media media = Media.parseDelimitedFrom(mmInputStream);
            Media.Item v = media.getItems(0);
            if (v != null) {
                String metaTempFile = fName + ".db.tmp";
                String metafile = fName + ".db";
                File metaTemp = new File(sdir, metaTempFile);
                metaTemp.createNewFile();
                fos = new FileOutputStream(metaTemp);
                Media.Builder builder = Media.newBuilder(media);
                builder.build().writeTo(fos);

                File finalMetaFile = new File(sdir, metafile);
                isSuccess = metaTemp.renameTo(finalMetaFile);
            } else {
            }
            result = 1;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        return result;
    }

    /**
     * This function will send the corresponding protobuf as temp metdata file
     * to the receiver
     * 
     * @param v
     * @param oStream
     * @return -1 if failure else 1
     */
    public int sendTmpMetaData(GeneratedMessage v) {
        int result = -1;
        Media.Builder mediaBuilder = Media.newBuilder();
        mediaBuilder.addItems((Media.Item) v);
        try {
            mediaBuilder.build().writeDelimitedTo(mmOutputSteam);
            result = 1;
        } catch (IOException e) {
        }

        return result;
    }
}
