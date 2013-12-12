/**
 * 
 */
package edu.isi.usaid.pifi.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
public class DeleteContentTask extends AsyncTask<Object, Integer, Void> {
	
	private ContentListActivity parent;
	
	private File contentDirectory;
	
	private ProgressDialog dialog;
	
	private Videos vMeta;
	
	private Articles aMeta;
	
	private File vFile, aFile;
	
	public DeleteContentTask(
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
	}

	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected Void doInBackground(Object... entries) {
		int count = entries.length;
		for (int i = 0; i < count; i++){
			Object o = entries[i];
			if (o instanceof Video){
				Video video = (Video)o;
				
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
			else if (o instanceof Article){
				Article article = (Article)o;
				
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
			
//			publishProgress((int) ((i / (float) count) * 100));
		}
		
		List<Object> entriesList = Arrays.asList(entries);
		
		// build new metadata
		// copy original video metadata except selected video
		Videos.Builder newVideos = Videos.newBuilder();
		for (Video v : vMeta.getVideoList()){
			
			// if not selected video
			if (!entriesList.contains(v)){
				
				// copy from original
				Video.Builder videoBuilder = Video.newBuilder();
				videoBuilder.mergeFrom(v);
				newVideos.addVideo(videoBuilder);
			}
		}
		
		// copy original video metadata except selected article
		Articles.Builder newArticles = Articles.newBuilder();
		for (Article a : aMeta.getArticleList()){
			
			// if not selected article
			if (!entriesList.contains(a)){
				
				// copy from original
				Article.Builder articleBuilder = Article.newBuilder();
				articleBuilder.mergeFrom(a);
				newArticles.addArticle(articleBuilder);
			}
		}
		
		// write new meta out and update metadata in memory
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
