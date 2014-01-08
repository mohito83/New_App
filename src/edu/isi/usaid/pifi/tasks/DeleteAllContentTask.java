/**
 * 
 */
package edu.isi.usaid.pifi.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import edu.isi.usaid.pifi.ContentListActivity;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Article;
import edu.isi.usaid.pifi.metadata.ArticleProtos.Articles;
import edu.isi.usaid.pifi.metadata.VideoProtos.Video;
import edu.isi.usaid.pifi.metadata.VideoProtos.Videos;

/**
 * @author jenniferchen
 *
 */
public class DeleteAllContentTask extends AsyncTask<Void, Integer, Void>{
	
	private ContentListActivity parent;
	
	private File contentDirectory;
	
	private ProgressDialog dialog;
	
	private Videos vMeta;
	
	private Articles aMeta;
	
	private File vFile, aFile;
	
	public DeleteAllContentTask(
			ContentListActivity parent,
			File contentDir, 
			ProgressDialog dialog, 
			Videos vMeta, 
			Articles aMeta, 
			File vFile,
			File aFile){
		this.parent = parent;
		contentDirectory = contentDir;
		this.dialog = dialog;
		this.vMeta = vMeta;
		this.aMeta = aMeta;
		this.vFile = vFile;
		this.aFile = aFile;
		
		int total = vMeta.getVideoCount() + aMeta.getArticleCount();
		dialog.setMax(total);
		publishProgress(0);
	}
	
	@Override
	protected Void doInBackground(Void... v) {
		
		int count = 0;
		
		// for each video
		for (Video video : vMeta.getVideoList()){
			
			count++;
			publishProgress(count);
			
			// delete file
			File file = new File(contentDirectory, video.getFilepath());
			file.delete();
			
			// delete thumbnail
			File thumb = new File(contentDirectory, video.getId() + "_default.jpg");
			thumb.delete();
			
			// delete from bookmark 
			if (parent.isBookmarked(video.getFilepath()))
				parent.removeBookmark(video.getFilepath());
		}
		
		// for each article
		for (Article article : aMeta.getArticleList()){
			
			count++;
			publishProgress(count);
			
			// delete file
			String path = article.getFilename();
			File file = new File(contentDirectory, path);
			String name = file.getName();
			
			// TODO delete thumbnail when we have it
			
			// delete assets 
			// TODO update this code when meta description completed with asset list
			String dirName = name.substring(0, name.indexOf(".htm"));
			File assetDir = new File(file.getParent(), dirName);
			if (assetDir.exists()){
				for (File f : assetDir.listFiles()){
					f.delete();
				}
			}
			
			file.delete();
			assetDir.delete();
			
			// delete from bookmark 
			if (parent.isBookmarked(path))
				parent.removeBookmark(path);
		}
		
		// build new empty metadata
		Videos.Builder newVideos = Videos.newBuilder();
		Articles.Builder newArticles = Articles.newBuilder();
		try {
			FileOutputStream out = new FileOutputStream(vFile);
			newVideos.build().writeTo(out); // write out to file
			out = new FileOutputStream(aFile);
			newArticles.build().writeTo(out); // write out to file
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	protected void onProgressUpdate(Integer... progress) {
        dialog.setProgress(progress[0]);
    }
	
	protected void onPostExecute(Void v) {
		parent.reload(false);
		dialog.dismiss();
	}

}
