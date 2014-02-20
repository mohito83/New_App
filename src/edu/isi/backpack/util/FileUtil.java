/**
 * 
 */
package edu.isi.backpack.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author jenniferchen
 *
 */
public class FileUtil {
	
	/**
	 * 
	 * @param directory - where to extract to
	 * @param zip - zipped file
	 * @return
	 */
	public static String unzip(File directory, File zip){
		try {
			FileInputStream fis = new FileInputStream(zip);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null){
				File entryFile = new File(directory, entry.getName());
				// if directory, create dir
				if (entry.isDirectory()){
					if (!entryFile.exists())
						entryFile.mkdir();
				}
				else { // create file
					FileOutputStream fos = new FileOutputStream(entryFile);
					byte[] buffer = new byte[4096]; 
					int len;
					while ((len = zis.read(buffer)) > 0) { 
						fos.write(buffer, 0, len); 
					}
					IOUtils.closeQuietly(fos);
				}
			}
			zis.closeEntry();
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		return null;
	}

}
