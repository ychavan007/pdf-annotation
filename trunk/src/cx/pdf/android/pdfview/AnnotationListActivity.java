package cx.pdf.android.pdfview;

import java.util.ArrayList;
import java.util.List;

import cx.pdf.android.pdfview.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * PDF Annotation: Activity for work with list of annotations
 * Date of last change: 20.04.2012
 */
public class AnnotationListActivity extends Activity {
	private final static String TAG = "annot";
	Annotation datasource = new Annotation(this);
	ArrayList<Annotation> arrayList = null;
	int lastItem = -1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.annot_list);
        arrayList = datasource.getAnnotations();
        insertItem();
 
    }
    
    /** Called when the user has navigated back to it */
    public void onRestart () {
    	super.onRestart();
    	insertItem();
    }
   
    
    /**
     * Create item of annotations list (annotation item)
     * @param cursor database cursor (data of annotation)
     * @param position cursor position
     * @return annotationList
     */
	private AnnotationList createItem (Cursor cursor, int position) {
		cursor.moveToPosition(position);
		if (cursor.getInt(cursor.getColumnIndex("flag")) != 3) { // flag(3) deleted annotation
        	return (new AnnotationList(
        		cursor.getString(cursor.getColumnIndex("author")), 
        		cursor.getString(cursor.getColumnIndex("subject")), 
        		cursor.getString(cursor.getColumnIndex("contents")),
        		cursor.getInt(cursor.getColumnIndex("page")),
        		datasource.convertDateFormat(cursor.getString(cursor.getColumnIndex("moddate"))),
        		cursor.getInt(cursor.getColumnIndex("_id"))
        	));
    	} else {
    		Log.w(TAG, "No annotations to show list (only deleted) " + cursor.getString(cursor.getColumnIndex("flag")));
    	}
		return null;
	}
	
	
	/**
	 * Insert new item to annotationList
	 */
	private void insertItem () {
		final List<AnnotationList> listOfAnnotations = new ArrayList<AnnotationList>();
        ListView list = (ListView) findViewById(R.id.AnnotListView);
        list.setClickable(true);
        AnnotationList al = null;
        
		try {
			// open database
	        datasource.open();
	        Cursor cursor = datasource.getFileAnnots(-1);
	        datasource.close();
	        
	        if (cursor == null) {
	        	Log.w(TAG, "No annotations to show list");
	        } else {
	        	// create annotation list
	        	cursor.moveToFirst();
		        do {
		        	al = createItem(cursor, cursor.getPosition());
		        	if (al != null) 
		        		listOfAnnotations.add(al);
		        	cursor.moveToNext();
				} while (!cursor.isAfterLast());
	        }
	        
	     
	        
        } catch (Exception e) {
        	datasource.close();
        	Log.w(TAG, "Select annotations: " + e);
        }
		
		
        
        final AnnotationListAdapter adapter = new AnnotationListAdapter(this, listOfAnnotations);
        
        // no annotation to view at list
        if (listOfAnnotations.size() < 1) {
        	listOfAnnotations.add(new AnnotationList("<no annotations>", null, null, 0, null, 0));
        	list.setClickable(false);
        } else {
        	// called annotation dialog after onClick at item
	        list.setOnItemClickListener(new OnItemClickListener() {
	            public void onItemClick(AdapterView<?> arg0, View view, int position, long index) {
	            	showAnnotationDialog(position, adapter, listOfAnnotations);
	            }
	        }); 
        }
        
        list.setAdapter(adapter);
	}

	/**
	 * Show annotation dialog
	 * @param position list position
	 * @param adapter list adapter
	 * @param listOfAnnotations list of annotation
	 */
    private void showAnnotationDialog(final int position, 
    	final AnnotationListAdapter adapter, final List<AnnotationList> listOfAnnotations) {

    	LayoutParams params = getWindow().getAttributes(); 
    	params.width = LayoutParams.FILL_PARENT;
    	getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    	
    	// alertDialog
    	LayoutInflater li = getLayoutInflater();
    	View view = li.inflate(R.layout.annot_message, null);
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.create();

    	// subject of annotation
    	TextView annot_subject = (TextView) view.findViewById(R.id.annot_subject);
    	String subject = listOfAnnotations.get(position).getSubject();
    	if (subject != "unknown") {
    		annot_subject.setText(subject);
        } else {
        	annot_subject.setVisibility(View.GONE);
    	}
    	
    	// author of annotation
    	TextView annot_author = (TextView) view.findViewById(R.id.annot_author);
    	String author = listOfAnnotations.get(position).getAuthor();
    	if (author != "unknown") {
    		annot_author.setText(author);
    	} else {
    		annot_author.setVisibility(View.GONE);
    	}
    	
    	// time of last modification
    	TextView annot_moddate = (TextView) view.findViewById(R.id.annot_moddate);
    	String moddate = listOfAnnotations.get(position).getModdate();
    	if (moddate != "unknown") {
    		annot_moddate.setText(moddate);
    	} else {
    		annot_moddate.setVisibility(View.GONE);
    	}
    	
    	// contents of annotation
    	TextView annot_text = (TextView) view.findViewById(R.id.annot_text);
    	annot_text.setText(listOfAnnotations.get(position).getContents());
    	
    	// EDIT button
        builder.setPositiveButton(R.string.ann_edit, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int id) {
            	Intent intent = new Intent();
        		intent.setClass(AnnotationListActivity.this, AnnotationActivity.class);
        		intent.putExtra("id", listOfAnnotations.get(position).getIdentifier());
        		startActivity(intent);
        		lastItem = listOfAnnotations.get(position).getIdentifier();
        		adapter.notifyDataSetChanged();
	        	adapter.notifyDataSetInvalidated();
        		dialog.cancel();
        	}	
        });

    	// DELETE button
    	builder.setNeutralButton(R.string.ann_delete, new DialogInterface.OnClickListener() {
    		public void onClick(final DialogInterface dialog, int id) {
           		AlertDialog.Builder builder = new AlertDialog.Builder(AnnotationListActivity.this);
        		builder.setMessage(R.string.delete_annotation_question)
        		       .setCancelable(true)
        		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface qdialog, int id) {
        		        	   dialog.cancel();
        		        	   qdialog.cancel();
        		        	   datasource.open();
        		        		// delete annotation from database
        		        	   datasource.deleteAnnotation(listOfAnnotations.get(position).getIdentifier());
        		        	   datasource.close();
        		        	   listOfAnnotations.remove(position);
        		        	   if (listOfAnnotations.size() < 1) {
        		        		   listOfAnnotations.add(new AnnotationList("<no annotations>", null, null, 0, null, 0));
        		        	   }
        		        	   adapter.notifyDataSetChanged();
        		        	   adapter.notifyDataSetInvalidated();

        		          		// show toast
        		        	   makeToast(R.string.annotation_deleted);
        		           }
        		       })
        		       .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface qdialog, int id) {
        		                qdialog.cancel();
        		           }
        		       }); 
        		builder.create().show();
			}
        });	
        
    	// OK button
        builder.setNegativeButton(R.string.ann_ok, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int id) {
        		dialog.cancel();
        	}	
        });
    	
    	// show annotation dialog
    	builder.setView(view);
    	if (!(listOfAnnotations.size() == 1 && listOfAnnotations.get(0).isEmpty())) {
    		builder.show();
    	}
    	
    }

    /**
     * Make toast text information
     * @param text resource string identifier
     */
    public void makeToast (int text) {
    	Toast.makeText(getApplicationContext(), text, 
    	Toast.LENGTH_SHORT).show();
    }
}
