/**
 * 
 */
package edu.isi.backpack.fragments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.isi.backpack.R;
import edu.isi.backpack.constants.Constants;
import edu.isi.backpack.constants.ExtraConstants;
import edu.isi.backpack.metadata.CommentProtos.Comment;

/**
 * @author jenniferchen
 * 
 * A page to show comments for an individual content
 *
 */
public class CommentsFragment extends Fragment {
	
	private List<Comment> comments;
	
	private EditText Comment;
	
	private Button addButton;
	
	private TextView commentsView;
	
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
	
	public static final CommentsFragment newInstance(List<Comment> comments){
		CommentsFragment f = new CommentsFragment();
		f.setComments(comments);
		return f;
	}
	
	public void setComments(List<Comment> comments){
		this.comments = comments;
	}
	
	@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
	        // Inflate the layout containing a title and body text.
	        ViewGroup rootView = (ViewGroup) inflater
	                .inflate(R.layout.fragment_comments, container, false);
	
	        Comment = (EditText)rootView.findViewById(R.id.userComment);
	        addButton = (Button)rootView.findViewById(R.id.submitComment);
	        commentsView = (TextView)rootView.findViewById(R.id.comments);
	        
	        ListIterator<Comment> iter = comments.listIterator(comments.size()); // bi-directional iterator
	        while (iter.hasPrevious()){ // want to show comments in reverse order
	        	Comment comment = iter.previous();
	        	commentsView.append(Html.fromHtml(
	        			"<b>" + comment.getUser() + "</b> (" + comment.getDate() + ")<br />"));
	        	commentsView.append(Html.fromHtml(comment.getText() + "<br/><br/>"));
	        	
	        }
	        
	        
	        addButton.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					String text = Comment.getText().toString();
					if (!text.isEmpty()){
						String user = "anonymous";
						Date d = new Date();
						String date = sdf.format(d);
						
						// add to top of the view
						CharSequence oldText = commentsView.getText();
						commentsView.setText(Html.fromHtml(
								"<p><b>" + user + "</b> (" + date + ")<br />" + text));
						commentsView.append(oldText);
						Comment.setText("");
						
						// broadcast a new intent
						Intent i = new Intent();
						i.setAction(Constants.NEW_COMMENT_ACTION);
						i.putExtra(ExtraConstants.USER, user);
						i.putExtra(ExtraConstants.DATE, date);
						i.putExtra(ExtraConstants.USER_COMMENT, text);
						LocalBroadcastManager.getInstance(CommentsFragment.this.getActivity()).sendBroadcast(i);
					}
				}
	        	
	        });
	        
	        return rootView;
	    }
	
	}
