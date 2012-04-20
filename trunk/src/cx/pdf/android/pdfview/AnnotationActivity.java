package cx.pdf.android.pdfview;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.itextpdf.text.BaseColor;

import cx.pdf.android.pdfview.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * PDF Annotation: Activity for work with PDF annotations (insert or update)
 * Date of last change: 20.04.2012
 */
public class AnnotationActivity extends Activity {
  
	private final static String TAG = "annot";
	private int objectID = -1;
	private int flagID = -1;
	private int dbID = -1;
	
	private float llx = 0;
	private float lly = 0;
	
	public int SetAColor = 0; // default annotation color
	public int SetASize = 0; // default annotation size
	    
	/** 
	 * Specifies the color for the annotation.
	 * @param item number of color
	 * @return resource color
	 */
	private int setColor (int item) {
	  	int color = 0;
	   	switch(item) {
	    	case 0 : color = (R.color.yellow); break;
	    	case 1 : color = (R.color.red); break;
	    	case 2 : color = (R.color.blue); break;
	    	case 3 : color = (R.color.green); break;
	    	case 4 : color = (R.color.orange); break;
	    	case 5 : color = (R.color.white); break;
	    	case 6 : color = (R.color.black); break;
	    	case 7 : color = (R.color.gray); break;
	   	}
	   	
	   	return color;
	}
	
	/**
	 * Convert title of annotation to number.
	 * @param String item title of annotation
	 * @return int number of annotation icon
	 */
	private int getTypeNumber (String item) {
	  	int number = 0;
	  	// Cannot switch on a value of type String for source level below 1.7.
	  	if (item.equals("Comment")) { number = 0;
	  	} else if (item.equals("Key")) { number = 1;
	  	} else if (item.equals("Note")) { number = 2;
	  	} else if (item.equals("Help")) { number = 3;
	  	} else if (item.equals("NewParagraph")) { number = 4;
	  	} else if (item.equals("Paragraph")) { number = 5;
	  	} else if (item.equals("Insert")) { number = 6;}
	  	
	   	return number;
	}
	
    /** Colors of the annotations. */
    final BaseColor[] COLORS = {
	   	BaseColor.YELLOW, BaseColor.RED, BaseColor.BLUE, BaseColor.GREEN,
	   	BaseColor.ORANGE, BaseColor.WHITE, BaseColor.BLACK , BaseColor.GRAY
    };  
	
	/**
	 * Inserting new or edited annotation to database.
	 * @param annotList list of annotation items
	 */
	public void insertAnnotation (ArrayList<String> annotList) {
		Annotation datasource = new Annotation(this);
		
		try {
			// open connection
			datasource.open();
			// insert annotation to SQLite database
			datasource.writeAnnotation(annotList, llx, lly);
			
		} catch (Exception e) {
			Log.w(TAG, "Insert annotation: " + e);
			
		} finally {
			// close connection
			datasource.close();
		}
	}
	
	/**
	 * Get account name (pre-fill author of annotation).
	 * @return String account name
	 */
	private String getAccountName () {
		AccountManager manager = AccountManager.get(this); 
	    Account[] accounts = manager.getAccountsByType("com.google"); 
	    List<String> possibleEmails = new LinkedList<String>();
	    String[] parts = null;
	    
	    // load accounts name
	    for (Account account : accounts) {
	      possibleEmails.add(account.name);
	    }

	    // return user name 
	    if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
	        String email = possibleEmails.get(0);
	        parts = email.split("@");
	        if ((parts.length > 0) && (parts[0] != null)) {
	            return parts[0];
	        } else {
	        	return null;
	        }
	    } 
	    	
	   return null;
	}

	/**
	 * Set x and y position points
	 * @param x position
	 * @param y position
	 */
	private void setPoint (float x, float y) {
		llx = x;
		lly = y;
	}
	
	/**
	 * Set identifiers of object, flag and database index
	 * @param cursor database cursor
	 */
	private void setIdentifiers (Cursor cursor) {
		objectID = cursor.getInt(cursor.getColumnIndex("objectid"));
		flagID = cursor.getInt(cursor.getColumnIndex("flag"));
		dbID = cursor.getInt(cursor.getColumnIndex("_id"));
	}
	
	/** Called when the activity is starting */
    @Override
    public void onCreate(Bundle state) {
    	Cursor cursor = null;
        super.onCreate(state);
        final Annotation datasource = new Annotation(this);
        setContentView(R.layout.annotation);
        Intent intent = getIntent();
		Bundle extras = getIntent().getExtras();
		String userName = getAccountName();
		final int subType = extras.getInt("subtype");
		final int page = extras.getInt("page");
		
		setPoint(intent.getFloatExtra(("llx"), -1), intent.getFloatExtra(("lly"), -1));
		
		/** edited annotation with identifier >= 0 */
    	if ((extras != null) && (extras.getInt("id") >= 0)) {
    		
        	try {
        		// add new annotation entry to database
    			datasource.open();
    			cursor = datasource.getAnnotById(extras.getInt("id"));
    			if (cursor != null && cursor.getCount() > 0) {
    				cursor.moveToFirst();
    				try {
    				    					
    					// pre-fill author field
    					String author = cursor.getString(cursor.getColumnIndex("author"));
		    			if (author != "unknown") {
		    				getInputText(R.id.author_annotation).setText(author);
		    			}
		    			
		    			// pre-fill subject field
    					String subject = cursor.getString(cursor.getColumnIndex("subject"));
		    			if (subject != "unknown") {
		    				getInputText(R.id.subject_annotation).setText(subject);
		    			}
		    			
		    			// pre-fill contents field
    					String contents = cursor.getString(cursor.getColumnIndex("contents"));
		    			if (contents != "unknown") {
		    				getInputText(R.id.contents_annotation).setText(contents);
		    			}
		    			
		    			// pre-fill color layout
		    			String color = cursor.getString(cursor.getColumnIndex("color"));
		    			// TODO: transparent
		    			if (color != "unknown" || color != "transparent") {
		    				FrameLayout annotationColor = (FrameLayout) findViewById(R.id.colorLayout);
			            	TextView colorNum = (TextView) findViewById(R.id.color_number);
			    			annotationColor.setBackgroundColor(Color.parseColor("#" + color));
	                    	colorNum.setText(Integer.toString(-1));
		    			}
		    			
		    			// pre-fill type icon layout
    					String type = cursor.getString(cursor.getColumnIndex("type"));
		    			if (type != "unknown") {
		    				ImageView annotationType = (ImageView) findViewById(R.id.typeLayout);
		    				TextView typeNum = (TextView) findViewById(R.id.type_number);
		    				annotationType.setBackgroundColor(Color.BLACK);
		    				annotationType.invalidate();
		    				annotationType.setImageResource(setIcon(getTypeNumber(type)));
		    				typeNum.setText(Integer.toString(getTypeNumber(type)));
		    			}
		    			
		    			// set identifiers
		    			setIdentifiers(cursor);
    				} catch (Exception e) {
    					Log.w(TAG, "set new value to inputText: " + e);
    				}
    			}
    			
    			 /** Spinner for set annotation size */
    		     Spinner SsetSize = (Spinner) findViewById(R.id.annot_size);
    		     TextView LabelSize = (TextView) findViewById(R.id.label_annot_size);
    		     SsetSize.setVisibility(View.GONE);
    		     LabelSize.setVisibility(View.GONE);
    		     
    		// exception
        	} catch (Exception e) {
				Log.w(TAG, "Add new annotation entry: " + e);
				
    		} finally {
    	    	// close database connection
    	    	if (datasource != null) {
    	    		datasource.close();
    	    	}
    	    }
    	
        /** new annotation with identifier < 0 */
    	} else {    
    		// set annotation default size
    		Spinner spinner = (Spinner) findViewById(R.id.annot_size);
	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
	        	AnnotationActivity.this, R.array.size_array, android.R.layout.simple_spinner_item);
	
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        spinner.setAdapter(adapter);
            
	        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
	        	public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {               
	        		SetASize = position;
	            }
	            	 
	            public void onNothingSelected(AdapterView<?> arg0) {
	            	SetASize = 0;
	            }
	
	        });
	        
	        // hidden subtype spinner and label 
		    if (extras.getInt("subtype") == 0) {
		    	spinner.setVisibility(View.GONE);
		    	TextView LabelSize = (TextView) findViewById(R.id.label_annot_size);
		    	LabelSize.setVisibility(View.GONE);
		    } 
		
		    
    		// pre-fill author field
			if (userName != null) {
				//Log.i(TAG, "vyplnuju: " + userName);
				getInputText(R.id.author_annotation).setText(userName);
			}
			
	        // set type is possible for text annotation only
	        if (extras.getInt("subtype") > 0) {
	        	Button BsetType = (Button) findViewById(R.id.setType);
	        	BsetType.setVisibility(View.GONE);
	        	ImageView LimageView = (ImageView) findViewById(R.id.typeLayout);
	        	LimageView.setVisibility(View.GONE);
	        }
	        	
    	}

        /** Save annotation button */
        Button BaddAnnotation = (Button) findViewById(R.id.add_annotation);
        BaddAnnotation.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		
            	// annotation author
            	EditText author = getInputText(R.id.author_annotation);
            	String Aauthor = author.getText().toString();

            	// annotation subject
            	EditText subject = getInputText(R.id.subject_annotation);
            	String Asubject = subject.getText().toString();
            	
            	// contents subject
            	EditText contents = getInputText(R.id.contents_annotation);
            	String Acontents = contents.getText().toString();

            	// annotation color
            	TextView color = (TextView) findViewById(R.id.color_number);
            	int Acolor = Integer.parseInt(color.getText().toString());

        		// annotation type
            	int Atype = textViewToInt(R.id.type_number);
                
                int Asize = datasource.getNewSize(SetASize);
                
                // java.lang.String.isEmpty() was added in Gingerbread (2.3)
            	// if (Aauthor.isEmpty() || Acontents.isEmpty() || Asubject.isEmpty()) {
                
            	// checking if user did not fill out all required fields
            	if (Aauthor == null || Acontents == null || Asubject == null) {
            		Toast.makeText(getApplicationContext(), R.string.filled_required_fields, Toast.LENGTH_SHORT).show();
            	} else { 
            		insertAnnotation(createList(Aauthor, Asubject, Acontents, Acolor, Atype, Asize, objectID, flagID, dbID, subType, page));
            		// close layout
            		finish();
            		Toast.makeText(getApplicationContext(), R.string.annotation_saved, Toast.LENGTH_SHORT).show();
            	}
            	
        	}
        });


        /** Color selection button */
        Button BsetColor = (Button) findViewById(R.id.setColor);
        BsetColor.setOnClickListener(new OnClickListener() {
        	final FrameLayout annotationColor = (FrameLayout) findViewById(R.id.colorLayout);
        	final TextView colorNum = (TextView) findViewById(R.id.color_number);
        	
        	int checkedItem = 0; // selecting radio button
        	Resources res = getResources();
            final CharSequence[] COLORLABS = res.getTextArray(R.array.colors);
        	
            public void onClick(View v) {
        		
                AlertDialog.Builder builderColor = new AlertDialog.Builder(AnnotationActivity.this);
                builderColor.setTitle(R.string.change_color);
                builderColor.setSingleChoiceItems(COLORLABS, checkedItem, new DialogInterface.OnClickListener(){
                   
                	public void onClick(DialogInterface dialogInterface, int item) {
                    	annotationColor.setBackgroundResource(setColor(item));
                    	colorNum.setText(Integer.toString(item));
                    	checkedItem = item; // save actual color of annotation
                    	dialogInterface.dismiss();
                    	
                    }
                });
                
                builderColor.show();
            }
 
        });
        
       
        
        /** Button for change annotation type. */
        Button BsetType = (Button) findViewById(R.id.setType);
        if (cursor != null && !cursor.getString(cursor.getColumnIndex("subtype")).equalsIgnoreCase("Text")) {
        	BsetType.setVisibility(View.GONE);
        	ImageView LimageView = (ImageView) findViewById(R.id.typeLayout);
        	LimageView.setVisibility(View.GONE);
        }
        
        BsetType.setOnClickListener(new OnClickListener() {
        	final ImageView annotationType = (ImageView) findViewById(R.id.typeLayout);
        	final TextView typeNum = (TextView) findViewById(R.id.type_number);
        	int checkedType = 0; // selecting radio button
        	Resources res = getResources();
            final String[] ICONS = res.getStringArray(R.array.icons);
                
            public void onClick(View v) {
            	
                AlertDialog.Builder builderType = new AlertDialog.Builder(AnnotationActivity.this);
                builderType.setTitle(R.string.change_type);
                builderType.setSingleChoiceItems(ICONS, checkedType, new DialogInterface.OnClickListener(){
                   
                	public void onClick(DialogInterface dialogInterface, int item) {
                		annotationType.setBackgroundColor(Color.BLACK);
                		annotationType.invalidate();
                		annotationType.setImageResource(setIcon(item));
                		typeNum.setText(Integer.toString(item));
                		checkedType = item; // save actual annotation type
                    	dialogInterface.dismiss();
                    }
                });
                
                builderType.show();
            }
            
        });
        
        // close cursor
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

    }
    
    /**
     * Create an arrayList of annotation
     * @param author author of annotation
     * @param subject subject of annotation
     * @param contents contents of annotation
     * @param color color of annotation
     * @param type type of annotation
     * @param size default size of annotation
     * @param objectID annotation object identifier
     * @param flagID flag identifier
     * @param dbID database index identifier
     * @param subType subtype of annotation
     * @param page number of page
     * @return created list
     */
    private ArrayList<String> createList (String author, String subject, String contents, int color, 
    	int type, int size, int objectID, int flagID, int dbID, int subType, int page) {
		ArrayList<String> annotList = new ArrayList<String>();
		annotList.add(author);
		annotList.add(subject);
		annotList.add(contents);
		annotList.add(Integer.toString(color));
		annotList.add(Integer.toString(type));
		annotList.add(Integer.toString(size));
		annotList.add(Integer.toString(objectID));
		annotList.add(Integer.toString(flagID));
		annotList.add(Integer.toString(dbID));
		annotList.add(Integer.toString(subType));
		annotList.add(Integer.toString(page));
		
		// return created list
		return annotList;
    }
    
    /** 
     * Specifies the icon for the annotation.
     * @param item number of annotation type
     * @return annotation icon resource
     */
    public int setIcon (int item) {
		int icon = 0;
		switch (item) {
			case 0: icon = (R.drawable.icon_comment); break;
			case 1: icon = (R.drawable.icon_key); break;
			case 2: icon = (R.drawable.icon_note); break;
			case 3: icon = (R.drawable.icon_help); break;
			case 4: icon = (R.drawable.icon_new_paragraph); break;
			case 5: icon = (R.drawable.icon_paragraph); break;
			case 6: icon = (R.drawable.icon_insert); break;
			default: icon = (R.drawable.icon_comment); break;
		}
		
		// icon of annotation
    	return icon;
    }  
    
    
    /** 
     * Selecting text from a text field.
     * @param id resource identifier
     * @return boolean value
     */
    private EditText getInputText (int id) {
		final EditText editText = (EditText) findViewById(id);
        editText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // if the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                  // perform action on key press
                  Toast.makeText(AnnotationActivity.this, editText.getText(), Toast.LENGTH_SHORT).show();
                  return true;
                }
                return false;
            }
        });
        
        return editText;
	}
    
    
    /**
     * Convert textView to integer value
     * @param resource textView identifier
     * @return integer value
     */
    private int textViewToInt (int resource) {
	    final TextView type = (TextView) findViewById(resource);
	    return Integer.parseInt(type.getText().toString());
    }
}
