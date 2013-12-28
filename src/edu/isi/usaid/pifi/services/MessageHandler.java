/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.content.ContextWrapper;
import android.util.Log;
import edu.isi.usaid.pifi.Constants;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;

/**
 * This class defines methods for handling the messages and data between two
 * devices
 * 
 * @author mohit aggarwl
 * 
 */
public class MessageHandler {
	private Connector conn;
	private ContextWrapper wrapper; // to be used for broadcasting
									// messaged to the
									// ContentListActivity
	private static final String TAG = "BackPackMessageHandler";
	private List<Video> videoList;
	private List<Article> webList;

	private File videoMetaFile;
	private File webMetaFile;

	/**
	 * @param webMetaFile
	 * @param metaFile
	 * 
	 */
	public MessageHandler(Connector conn, ContextWrapper wrapper,
			File metaFile, File webMetaFile) {
		this.conn = conn;
		this.wrapper = wrapper;
		this.videoMetaFile = metaFile;
		this.webMetaFile = webMetaFile;
	}

	/**
	 * This method sends the meta data file and waits a response from the
	 * receiver
	 * 
	 * @param type
	 * @param metaFile
	 * @return -1 if receiver is failed to receive or process the data sent by
	 *         the sender, else 1
	 */
	public int sendFullMetaData(short type, File metaFile) {
		int result = -1;
		InfoMessage infomessage = BackpackUtils.createInfoMessage(type, metaFile);
		// 1. send the info message informing the data
		conn.sendInfoMessage(infomessage);

		// 2. send actual meta data content
		result = conn.sendFileData(metaFile);

		// 3. Receive the ACK message
		if (result >= 0) {
			InfoMessage ackMessage = conn.receiveInfoMessage();
			if (ackMessage.getType() == Constants.ACK_DATA) {
				AckPayload payload = (AckPayload) ackMessage.getPayload();
				if (payload.getAck() == Constants.OK_RESPONSE) {
					BackpackUtils.broadcastMessage(wrapper, "Sent metadata file: "
							+ metaFile.getName());
					Log.i(TAG,
							"Successfully sent meta data file: "
									+ metaFile.getName());
				} else {
					result = -1;
					Log.i(TAG,
							"Failed to sent meta data file: "
									+ metaFile.getName());
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
	 */
	public int receiveFullMetaData(File sdir) {
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

			Log.i(TAG, "Successfully received metadata file: "
					+ ((InfoPayload) info.getPayload()).getFileName());

			Log.i(TAG, "Calculating the Delta for this file");

			try {
				if (info.getType() == Constants.VIDEO_META_DATA_FULL) {
					File newVideoMetaFile = new File(sdir, tempMetaFName);
					videoList = BackpackUtils.getVideoDeltaList(videoMetaFile,
							newVideoMetaFile);
					newVideoMetaFile.delete();
				} else {
					File newWebMetaFile = new File(sdir, tempMetaFName);
					webList = BackpackUtils.getWebDeltaList(webMetaFile,
							newWebMetaFile);
					newWebMetaFile.delete();
				}
			} catch (IOException e) {
				Log.e(TAG, "Error while caculating delta for meta data file: "
						+ e.getMessage());
				result = -1;
			}
		}

		// 3. Send back ACK message
		short ackVal = result >= 0 ? Constants.OK_RESPONSE
				: Constants.FAIL_RESPONSE;
		InfoMessage ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
				info.getType(), ackVal);
		conn.sendInfoMessage(ackMsg);

		return result;
	}

	/**
	 * This method sends the video package to the receiver. The files present in
	 * the package are already calculated and stored in videoList.
	 * 
	 * @param sdir
	 */
	public void sendVideos(File sdir) {
		boolean start = false;
		// send an info message informing start of bulk transfer operation
		InfoMessage bulkMsg = BackpackUtils.createInfoMessage(
				Constants.START_BULK_TX, null);
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
			for (Video v : videoList) {
				// 1. Send video file
				// 1.1. send Info message
				File f = new File(sdir, v.getFilepath());
				InfoMessage info = BackpackUtils.createInfoMessage(
						Constants.FILE_DATA, f);
				conn.sendInfoMessage(info);

				// 1.2. send video contents
				conn.sendFileData(f);

				// 1.3. receive ACK from the receiver
				InfoMessage ackMsg = conn.receiveInfoMessage();
				if (ackMsg.getType() == Constants.ACK_DATA) {
					AckPayload payload = (AckPayload) ackMsg.getPayload();
					if (payload.getAck() == Constants.OK_RESPONSE) {
						Log.i(TAG, "Successfully sent file: " + f.getName());
					} else {
						Log.i(TAG, "Failed to sent file: " + f.getName());
						continue;
					}
				}

				// 2. Send bitmap image of video
				// 2.1 send Info message first
				String thumbnail = v.getId() + Constants.VIDEO_THUMBNAIL_ID;
				File thumbNailFile = new File(sdir, thumbnail);
				info = BackpackUtils.createInfoMessage(Constants.IMAGE_DATA,
						thumbNailFile);
				conn.sendInfoMessage(info);

				// 2.2 send bitmap content
				conn.sendFileData(thumbNailFile);

				// 2.3 receive an ACK
				ackMsg = conn.receiveInfoMessage();
				if (ackMsg.getType() == Constants.ACK_DATA) {
					AckPayload payload = (AckPayload) ackMsg.getPayload();
					if (payload.getAck() == Constants.OK_RESPONSE) {
						Log.i(TAG,
								"Successfully sent bitmap image: "
										+ f.getName());
					} else {
						Log.i(TAG,
								"Failed to sent bitmap image: " + f.getName());
					}
				}

				// 3. Send temp meta data for video
				// 3.1 send info message
				info = BackpackUtils.createInfoMessage(
						Constants.VIDEO_META_DATA_TMP, f);
				conn.sendInfoMessage(info);

				// 3.2 send temp meta data content
				conn.sendTmpMetaData(v);

				// 3.3 receive ACK for temp meta data
				ackMsg = conn.receiveInfoMessage();
				if (ackMsg.getType() == Constants.ACK_DATA) {
					AckPayload payload = (AckPayload) ackMsg.getPayload();
					if (payload.getAck() == Constants.OK_RESPONSE) {
						Log.i(TAG, "Successfully sent tmp meta data");
						BackpackUtils.broadcastMessage(wrapper, "Sent video file: "
								+ v.getFilename());
					} else {
						Log.i(TAG, "Failed to sent meta data");
						continue;
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
				Log.i(TAG, "Bulk transfer of video files finishes");
			}
		}
	}

	/**
	 * This method receives the video files and content associated with each
	 * video file
	 * 
	 * @param sdir
	 */
	public void receiveFiles(File sdir) {
		boolean start = false;
		// wait for bulk data tx signal
		InfoMessage bulkMsg = conn.receiveInfoMessage();
		if (bulkMsg.getType() == Constants.START_BULK_TX) {
			start = true;
		}

		if (start) {
			InfoMessage bulkAckMsg = BackpackUtils.createInfoMessage(
					Constants.ACK_DATA, Constants.START_BULK_TX,
					Constants.OK_RESPONSE);
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

			case Constants.FILE_DATA:
				result = conn.readFileData(info, sdir);
				ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
						type, result > 0 ? Constants.OK_RESPONSE
								: Constants.FAIL_RESPONSE);
				conn.sendInfoMessage(ackMsg);
				break;

			case Constants.IMAGE_DATA:
				result = conn.readFileData(info, sdir);
				ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
						type, result > 0 ? Constants.OK_RESPONSE
								: Constants.FAIL_RESPONSE);
				conn.sendInfoMessage(ackMsg);
				break;

			case Constants.VIDEO_META_DATA_TMP:
				result = conn.readTmpMetaData(info, sdir, true);
				ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
						type, result > 0 ? Constants.OK_RESPONSE
								: Constants.FAIL_RESPONSE);
				if (result > 0) {
					BackpackUtils.broadcastMessage(wrapper, "Received file: "
							+ ((InfoPayload) info.getPayload()).getFileName());
				}
				conn.sendInfoMessage(ackMsg);
				break;

			case Constants.WEB_META_DATA_TMP:
				result = conn.readTmpMetaData(info, sdir, false);
				ackMsg = BackpackUtils.createInfoMessage(Constants.ACK_DATA,
						type, result > 0 ? Constants.OK_RESPONSE
								: Constants.FAIL_RESPONSE);
				if (result > 0) {
					BackpackUtils.broadcastMessage(wrapper, "Received file: "
							+ ((InfoPayload) info.getPayload()).getFileName());
				}
				conn.sendInfoMessage(ackMsg);
				break;
			}

		}
	}

	/**
	 * This method sends web content to the receiver
	 * 
	 * @param sdir
	 */
	public void sendWebContent(File sdir) {
		boolean start = false;
		// send an info message informing start of bulk transfer operation
		InfoMessage bulkMsg = BackpackUtils.createInfoMessage(
				Constants.START_BULK_TX, null);
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
			for (Article a : webList) {
				// 1. Send web content (.html) file
				// 1.1 send info message first
				File f = new File(sdir, a.getFilename());
				InfoMessage info = BackpackUtils.createInfoMessage(
						Constants.FILE_DATA, f);
				conn.sendInfoMessage(info);

				// 1.2 send the actual web content file
				conn.sendFileData(f);

				// 1.3. receive ACK from the receiver
				InfoMessage ackMsg = conn.receiveInfoMessage();
				if (ackMsg.getType() == Constants.ACK_DATA) {
					AckPayload payload = (AckPayload) ackMsg.getPayload();
					if (payload.getAck() == Constants.OK_RESPONSE) {
						Log.i(TAG, "Successfully sent file: " + f.getName());
					} else {
						Log.i(TAG, "Failed to sent file: " + f.getName());
						continue;
					}
				}

				// 2 Send images associated with the web content
				List<File> webImgList = BackpackUtils
						.getWebArticleImages(sdir, a);
				for (File img : webImgList) {
					// 2.1 send info message
					info = BackpackUtils.createInfoMessage(Constants.IMAGE_DATA,
							img);
					conn.sendInfoMessage(info);

					// 2.2 Send actual image data
					conn.sendFileData(img);

					// 2.3 receive ack from the receiver
					ackMsg = conn.receiveInfoMessage();
					if (ackMsg.getType() == Constants.ACK_DATA) {
						AckPayload payload = (AckPayload) ackMsg.getPayload();
						if (payload.getAck() == Constants.OK_RESPONSE) {
							Log.i(TAG,
									"Successfully sent bitmap image: "
											+ img.getName());
						} else {
							Log.i(TAG,
									"Failed to sent bitmap image: "
											+ img.getName());
						}
					}

				}

				// 3 Send tmp web meta data
				// 3.1 send info message
				info = BackpackUtils.createInfoMessage(
						Constants.WEB_META_DATA_TMP, f);
				conn.sendInfoMessage(info);

				// 3.2 send temp meta data content
				conn.sendTmpMetaData(a);

				// 3.3 receive ACK for temp meta data
				ackMsg = conn.receiveInfoMessage();
				if (ackMsg.getType() == Constants.ACK_DATA) {
					AckPayload payload = (AckPayload) ackMsg.getPayload();
					if (payload.getAck() == Constants.OK_RESPONSE) {
						Log.i(TAG, "Successfully sent tmp meta data");
						BackpackUtils.broadcastMessage(wrapper,
								"Sent web content: " + a.getFilename());
					} else {
						Log.i(TAG, "Failed to sent meta data");
						continue;
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
				Log.i(TAG, "Bulk transfer of web content files finishes");
			}
		}
	}

}
