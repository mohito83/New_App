/**
 * 
 */
package edu.isi.usaid.pifi.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.GeneratedMessage;

import android.util.Log;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;

/**
 * This class defines utility methods for socket programming. Rename the
 * function name if you don't like them
 * 
 * @author mohit aggarwl
 * 
 */
public class SocketUtils {

	private static final String TAG = "SocketUtils";
	private static byte buffer[] = new byte[8 * 1024];

	/**
	 * This method will write data to the socket
	 * 
	 * @param os
	 * @param buffer
	 */
	public static void writeToSocket(OutputStream os, byte[] buffer) {
		try {
			DataOutputStream dos = new DataOutputStream(os);
			dos.write(buffer);
			dos.flush();
		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}

	}

	/**
	 * This method sends the file across the socket
	 * 
	 * @param os
	 * @param buffer
	 * @throws IOException
	 * 
	 * @return totalBytesSent
	 */
	public static int writeToSocket(DataOutputStream dos, File f)
			throws IOException {
		int len = 0;
		FileInputStream fin = new FileInputStream(f);
		dos.writeUTF(f.getName());
		dos.writeLong(f.length());
		int totalBytesSent = 0;
		while ((len = fin.read(buffer)) != -1) {
			dos.write(buffer, 0, len);
			totalBytesSent += len;
		}
		fin.close();
		dos.flush();
		return totalBytesSent;
	}

	/**
	 * This method read data from the socket and write it to the devices's file
	 * system
	 * 
	 * @param dis
	 * @param sdir
	 * @throws IOException
	 */
	public static void readFromSocket(DataInputStream dis, File sdir,
			String fName) throws IOException {
		String tmpFName = fName + ".tmp";
		Log.i(TAG, "File name: " + fName);
		long fLen = dis.readLong();
		Log.i(TAG, "File size: " + fLen);
		// create a file
		Log.i(TAG, "Creating local file");
		// append .tmp to the file name before the file transfer starts
		File f = new File(sdir, tmpFName);
		f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);
		int bytesRead = 0;

		while (bytesRead < fLen) { // read exactly fLen
			int read = dis.read(buffer, 0,
					Math.min((int) fLen - bytesRead, buffer.length));
			fos.write(buffer, 0, read);
			bytesRead += read;
		}

		fos.close();

		// TODO Before renaming the file check the sanity of the
		// transferred file using some checksum

		// after file transfer is complete then rename the file to drop
		// .tmp
		File finalFile = new File(sdir, fName);
		boolean isSuccess = f.renameTo(finalFile);
		Log.i(TAG, "File tranfer:" + isSuccess);
	}

	/**
	 * This method reads temporary meta data (associated with each file
	 * transferred) from the socket and saves it to device's file system
	 * 
	 * @param is
	 * @param sdir
	 * @param fName
	 * @param isVideo
	 * @throws IOException
	 */
	public static void readTmpMetaDataFromSocket(InputStream is, File sdir,
			String fName, boolean isVideo) throws IOException {
		FileOutputStream fos;
		boolean isSuccess;
		if (isVideo) {
			Videos videos = Videos.parseDelimitedFrom(is);
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
				Log.i(TAG, "Video Meta data file transfer is successfull");
			} else {
				Log.w(TAG, "Video Meta data file transfer fails");
			}
		} else {
			Articles articles = Articles.parseDelimitedFrom(is);
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
		}
	}

	/**
	 * This method read the video from the input stream from bluetooth socket
	 * 
	 * @param sdir
	 * @param is
	 * @param isVideo
	 * @return no of bytes received
	 */
	public static int readVideoFromSocket(File sdir, InputStream is) {
		int i = 0;
		try {
			DataInputStream dis = new DataInputStream(is);
			// reading number of file to be received
			int no_of_files = dis.readInt();
			for (i = 0; i < no_of_files; i++) {
				String fName = dis.readUTF();
				readFromSocket(dis, sdir, fName);
				readTmpMetaDataFromSocket(dis, sdir, fName, true);
			}

		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}
		return i;
	}

	/**
	 * This method reads the web content from the socket input stream
	 * 
	 * @param sdir
	 * @param is
	 * @param isVideo
	 * @return no of files received
	 */
	public static int readArticlesFromSocket(File sdir, InputStream is) {

		int i = 0;
		int no_of_files = 0;
		try {
			DataInputStream dis = new DataInputStream(is);

			while ((no_of_files = dis.readInt()) != Integer.MAX_VALUE) {
				String fName = dis.readUTF();
				readFromSocket(dis, sdir, fName);
				readTmpMetaDataFromSocket(dis, sdir, fName, false);
				
				//read each image file

				for (int j = 0; j < no_of_files; j++) {
					fName = dis.readUTF();
					readFromSocket(dis, sdir, fName);
					i++;
				}
				i++;
			}

		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}
		return i;

	}

	/**
	 * This method sends the video package
	 * 
	 * @param root
	 * @param os
	 * @param sendTo
	 */
	public static void sendVideoPackage(File root, OutputStream os,
			List<Video> sendTo) {
		DataOutputStream dos = new DataOutputStream(os);

		try {
			// setting number of files to be sent
			dos.writeInt(sendTo.size());
			for (Video v : sendTo) {
				File f = new File(root, v.getFilepath());

				int totalBytesSent = writeToSocket(dos, f);

				Log.i(TAG, "Sent video " + totalBytesSent + " bytes");

				sendProtoBufToReceiver(v, os);
				Log.i(TAG, "Sent video meta data" + totalBytesSent + " bytes");
			}
		} catch (IOException e) {
			Log.e(TAG, "Exception while sending videos package", e);
		}
	}

	/**
	 * TRhis function will send the corresponding protobuf as temp metdata file
	 * to the receiver
	 * 
	 * @param v
	 * @param oStream
	 */
	private static void sendProtoBufToReceiver(GeneratedMessage v,
			OutputStream oStream) {
		if (v instanceof Video) {
			Videos.Builder videos = Videos.newBuilder();
			videos.addVideo((Video) v);
			try {
				/* videos.build().writeTo(oStream); */
				videos.build().writeDelimitedTo(oStream);
			} catch (IOException e) {
				Log.e(TAG, "Exception while sending videos protobuf file", e);
			}
		}

		if (v instanceof Article) {
			Articles.Builder articles = Articles.newBuilder();
			articles.addArticle((Article) v);
			try {
				/* videos.build().writeTo(oStream); */
				articles.build().writeDelimitedTo(oStream);
			} catch (IOException e) {
				Log.e(TAG, "Exception while sending article protobuf file", e);
			}
		}
	}

	/**
	 * This method will parse the meta data and send it over the output stream
	 * 
	 * @param metaDataFile
	 * @param os
	 */
	public static void sendMetaData(File metaDataFile, OutputStream os) {
		FileInputStream fin = null;
		DataOutputStream dos = new DataOutputStream(os);
		try {

			Log.i(TAG, "Sending meta file size");

			// 1. send metadata size
			long byteCount = metaDataFile.length();
			dos.writeLong(byteCount);

			Log.i(TAG, "Sending meta file");

			if (!metaDataFile.exists()) {
				metaDataFile.createNewFile();
			}

			// 2. send metadata file
			fin = new FileInputStream(metaDataFile);
			int tb = 0;
			int b = 0;
			while (tb < byteCount) {
				b = fin.read(buffer, 0,
						Math.min((int) byteCount - tb, buffer.length));
				dos.write(buffer, 0, b);
				tb += b;
			}
			dos.flush();
			Log.i(TAG, "Sent metadata " + tb + " bytes");

		} catch (FileNotFoundException e) {
			Log.e(TAG, "unable to open file input stream", e);
		} catch (IOException e) {
			Log.e(TAG,
					"unable to perform i/o on file: " + metaDataFile.getName(),
					e);
		} finally {
			try {
				fin.close();
			} catch (IOException e) {
				Log.e(TAG, "Exception while closing the file input stream", e);
			}
		}
	}

	/**
	 * This method receives the meta data file and calculate the delta over them
	 * which will be used in future to complete th sync between two devices.
	 * 
	 * @param localMetaData
	 * @param mmInStream
	 * @throws IOException
	 */
	public static byte[] receiveMetadataFile(File localMetaData,
			InputStream mmInStream) throws IOException {
		// get metadata file size
		DataInputStream dis = new DataInputStream(mmInStream);
		long byteCount = dis.readLong();
		Log.i(TAG, "expecting " + byteCount + " bytes");

		// read metadata
		byte[] buf = new byte[(int) byteCount];
		int bytesRead = 0;
		while (bytesRead < byteCount) {
			bytesRead += mmInStream
					.read(buf, bytesRead, buf.length - bytesRead);
		}

		return buf;
	}

	/**
	 * This method will send the web package to the listener. TODO Reading web
	 * content is bit tricky.. be careful :P
	 * 
	 * @param path
	 * @param mmOutStream
	 * @param sendToWeb
	 */
	public static void sendWebPackage(File root, OutputStream mmOutStream,
			List<Article> sendToWeb) {

		DataOutputStream dos = new DataOutputStream(mmOutStream);

		try {
			// calculate the number of files to be sent this should include the
			// images folder
			// setting number of files to be sent
			for (Article v : sendToWeb) {
				List<File> imageFiles = getWebArticleImages(root, v);
				dos.writeInt(imageFiles.size());
				File f = new File(root, v.getFilename());

				int totalBytesSent = writeToSocket(dos, f);

				Log.i(TAG, "Sent web " + totalBytesSent + " bytes");

				// Send the metadata for the web article
				sendProtoBufToReceiver(v, mmOutStream);
				Log.i(TAG, "Sent web meta data" + totalBytesSent + " bytes");

				// start sending the images associated with the HTML file
				for (File imgFile : imageFiles) {
					totalBytesSent += writeToSocket(dos, imgFile);
				}
				Log.i(TAG, "Sent web images (" + f.getName() + ") data"
						+ totalBytesSent + " bytes");
			}

			// once all article and images associated with them are transferred
			// then send a special message to signal receiver that data transfer
			// if complete.
			// E.g. use code = 200 to proceed
			dos.writeInt(Integer.MAX_VALUE);
			dos.flush();

		} catch (IOException e) {
			Log.e(TAG, "Exception while sending web package", e);
		}

	}

	/**
	 * This method will calculate the web package size. This size include html
	 * files as well as images in the folder.
	 * 
	 * @param path
	 * 
	 * @param sendToWeb
	 * @return
	 */
	public static int getWebPackageSize(File path, List<Article> sendToWeb) {
		int totalFiles = 0;
		for (Article a : sendToWeb) {
			totalFiles++;
			String fileName = a.getFilename();
			String imgFolderName = fileName.substring(0,
					fileName.indexOf(".html"));
			File imgFolder = new File(path, imgFolderName);
			if (imgFolder.isDirectory()) {
				totalFiles += imgFolder.list().length;
			}
		}
		return totalFiles;
	}

	private static List<File> getWebArticleImages(File path, Article artilce) {
		List<File> webImagesPath = new ArrayList<File>();
		String filenName = artilce.getFilename();
		String webImageFolderName = filenName.substring(0,
				filenName.indexOf(".html"));
		File imgFolder = new File(path, webImageFolderName);
		if (imgFolder.isDirectory()) {
			webImagesPath = Arrays.asList(imgFolder.listFiles());
		}

		return webImagesPath;
	}
}
