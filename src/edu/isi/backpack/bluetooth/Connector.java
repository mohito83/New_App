/**
 * 
 */
package edu.isi.backpack.bluetooth;

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

import com.google.protobuf.GeneratedMessage;

import edu.isi.backpack.R;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.metadata.ArticleProtos.Article;
import edu.isi.backpack.metadata.ArticleProtos.Articles;
import edu.isi.backpack.metadata.VideoProtos.Video;
import edu.isi.backpack.metadata.VideoProtos.Videos;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * This class defines methods to perform data transaction
 * 
 * @author mohit aggarwl
 * 
 */
public class Connector {

	private static final String TAG = "BackPackConnector";

	private InputStream mmInputStream;
	private OutputStream mmOutputSteam;
	private Notification notification=null;
	private NotificationManager notificationManager = null;
	private boolean  notify= true,destroy = false;
	private Thread t ;
	

	private static byte buffer[] = new byte[8192];

	/**
	 * 
	 */
	public Connector(InputStream in, OutputStream out,Context c) {
		mmInputStream = in;
		mmOutputSteam = out;
		notification = new Notification();
		notification.icon = edu.isi.backpack.R.drawable.ic_launcher;
		notification.tickerText = "BackPack";
		notification.when =System
                .currentTimeMillis();
        notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;
        notification.contentView = new RemoteViews(c.getPackageName(), R.layout.download_progress);
        //notification.contentIntent = pendingIntent;
        notification.contentView.setImageViewResource(edu.isi.backpack.R.drawable.ic_launcher, edu.isi.backpack.R.drawable.ic_action_share);
        notification.contentView.setTextViewText(R.id.status_text, "simulation in progress");
        notification.contentView.setProgressBar(R.id.status_progress, 100, 2, false);
        notificationManager = (NotificationManager) c.getSystemService(
               Context.NOTIFICATION_SERVICE);
        t =  new Thread(new Runnable() {
 	        @Override
 	        public void run() {
 	            while (true) {
 	                try {
 	                	if(destroy)
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
	
	public void cancelNotification(){
		if(notificationManager != null){
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
		Log.i(TAG, "Sending InfoMEssage:" + infoMessage.displayInfoData());
		try {
			ObjectOutputStream oOutStream = new ObjectOutputStream(
					mmOutputSteam);
			oOutStream.writeObject(infoMessage);
		} catch (IOException e) {
			Log.e(TAG,
					"Error occured while sending InfoMessage object:"
							+ e.getMessage());
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
			ObjectInputStream oInputStream = new ObjectInputStream(
					mmInputStream);
			info = (InfoMessage) oInputStream.readObject();
		} catch (StreamCorruptedException e) {
			Log.e(TAG, "Error reading InfoMEssage Object:" + e.getMessage());
		} catch (OptionalDataException e) {
			Log.e(TAG, "Error reading InfoMEssage Object:" + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "Error reading InfoMEssage Object:" + e.getMessage());
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "Error reading InfoMEssage Object:" + e.getMessage());
		}

		// bt disconnection occurred
		if (info == null){
			throw new BluetoothDisconnectedException("Bluetooth Disconnected");
		}
		
		Log.i(TAG,
				"Received InfoMEssage:" + info != null ? info.displayInfoData()
						: "NULL");
		return info;
	}

	/**
	 * This method read data from the file and send it across the Bluetooth
	 * socket.
	 * 
	 * @param f
	 * @return -1 if failure else number of bytes sent
	 */
	public int sendFileData(File f,InfoMessage info) {
		Log.i(TAG, "Start Sending file:" + f.getName());
		int result = -1;
		int totalBytesSent = 0;
		int len = 0;
		try {
			FileInputStream fin = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fin, 8 * 1024);
			BufferedOutputStream mOutputSteam = new BufferedOutputStream(mmOutputSteam);
			while ((len = bis.read(buffer)) != -1) {				
				if(notify){
					notify = false;
					notification.contentView.setTextViewText(R.id.status_text, "Sending file : "+f.getName()+"("+(totalBytesSent/1000)+"Kb/"
																			+(info.getFileSize()/1000)+"Kb)");
					notification.contentView.setProgressBar(R.id.status_progress,  info.getFileSize(), (int)totalBytesSent+info.getFileSize()/15, false);
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
			Log.i(TAG, "Finished sending file:" + f.getName() + "(" + result
					+ " bytes)");
		} catch (FileNotFoundException e) {
			Log.e(TAG,
					"Error reading file (" + f.getName() + "):"
							+ e.getMessage());
			result = -1;
		} catch (IOException e) {
			Log.e(TAG,
					"Error while reading or sending file contents:"
							+ e.getMessage());
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
		Log.i(TAG, "Start receiving file:" + fName);

		String tmpFName = fName + ".tmp";
		Log.i(TAG, "File name: " + fName);
		long fLen = payload.getLength();
		Log.i(TAG, "File size: " + fLen);
		// create a file
		Log.i(TAG, "Creating local file");
		// append .tmp to the file name before the file transfer starts
		File f = new File(sdir, tmpFName);
		boolean saveToFile = true;
		try {
			try {
				f.createNewFile();
			} catch (Exception e) {
				saveToFile = false;
			}
			
			FileOutputStream fos = null;
			if(saveToFile){
				fos = new FileOutputStream(f);
			}
			BufferedOutputStream bos = new BufferedOutputStream(fos, 8 * 1024);
			int bytesRead = 0;
			BufferedInputStream mInputStream = new BufferedInputStream(mmInputStream);
			while (bytesRead < fLen) { // read exactly fLen				
				if(notify){
					notify = false;
					notification.contentView.setTextViewText(R.id.status_text, "Receiving file : "+fName+"("+(bytesRead/1000)+"Kb/"
																			+(fLen/1000)+"Kb)");
					notification.contentView.setProgressBar(R.id.status_progress,  (int)fLen, (int)bytesRead+info.getFileSize()/15, false);
					notificationManager.notify(42, notification);
				}
				int read = mInputStream.read(buffer, 0,
						Math.min((int) fLen - bytesRead, buffer.length));
				if(saveToFile){
					bos.write(buffer, 0, read);
				}
				bytesRead += read;
			}
			


			if(saveToFile && fos != null){
				bos.flush();
				bos.close();
				fos.flush();
				fos.close();
				result = bytesRead;
			}
			Log.i(TAG, "Finished receiving file:" + fName + "(" + result
					+ " bytes)");
		} catch (FileNotFoundException e) {
			Log.e(TAG,
					"Error while writing to  file (" + fName + "):"
							+ e.getMessage());
		} catch (IOException e) {
			Log.e(TAG,
					"Error while reading or writing file contents:"
							+ e.getMessage());
		}

		// after file transfer is complete then rename the file to drop
		// .tmp
		if (result >= 0) {
			File finalFile = new File(sdir, fName);
			boolean isSuccess = f.renameTo(finalFile);
			Log.i(TAG, "File (" + fName + ") tranfer:" + isSuccess);
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
		if (isVideo) {
			try {
				Videos videos = Videos.parseDelimitedFrom(mmInputStream);
				Video v = videos.getVideo(0);
				if (v != null) {
					String metaTempFile = fName + ".dat.tmp";
					String metafile = fName + ".dat";
					File metaTemp = new File(sdir, metaTempFile);
					metaTemp.createNewFile();
					fos = new FileOutputStream(metaTemp);
					Videos.Builder builder = Videos.newBuilder(videos);
					builder.build().writeTo(fos);

					File finalMetaFile = new File(sdir, metafile);
					isSuccess = metaTemp.renameTo(finalMetaFile);
					Log.i(TAG, "MetadataFile tranfer:" + isSuccess);
					Log.i(TAG, "Video Meta data file (" + fName
							+ ") transfer is successfull");
				} else {
					Log.w(TAG, "Video Meta data file (" + fName
							+ ") transfer fails");
				}
				result = 1;
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Error while writing to temp meta data ile file ("
						+ fName + "):" + e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, "Error while reading or writing file (" + fName
						+ ") contents:" + e.getMessage());
			}
		} else {
			try {
				Articles articles = Articles.parseDelimitedFrom(mmInputStream);
				Article v = articles.getArticle(0);
				if (v != null) {
					String metaTempFile = fName + ".dat.tmp";
					String metafile = fName + ".dat";
					File metaTemp = new File(sdir, metaTempFile);
					metaTemp.createNewFile();
					fos = new FileOutputStream(metaTemp);
					Articles.Builder builder = Articles.newBuilder(articles);
					builder.build().writeTo(fos);

					File finalMetaFile = new File(sdir, metafile);
					isSuccess = metaTemp.renameTo(finalMetaFile);
					Log.i(TAG, "MetadataFile tranfer:" + isSuccess);
					Log.i(TAG, "Article Meta data file transfer is successfull");
				} else {
					Log.w(TAG, "Article Meta data file transfer fails");
				}
				result = 1;
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Error while writing to temp meta data ile file ("
						+ fName + "):" + e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, "Error while reading or writing file (" + fName
						+ ") contents:" + e.getMessage());
			}
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
		if (v instanceof Video) {
			Videos.Builder videos = Videos.newBuilder();
			videos.addVideo((Video) v);
			try {
				/* videos.build().writeTo(oStream); */
				videos.build().writeDelimitedTo(mmOutputSteam);
				result = 1;
			} catch (IOException e) {
				Log.e(TAG, "Exception while sending videos protobuf file", e);
			}
		}

		if (v instanceof Article) {
			Articles.Builder articles = Articles.newBuilder();
			articles.addArticle((Article) v);
			try {
				/* videos.build().writeTo(oStream); */
				articles.build().writeDelimitedTo(mmOutputSteam);
				result = 1;
			} catch (IOException e) {
				Log.e(TAG, "Exception while sending article protobuf file", e);
			}
		}

		return result;
	}

}
