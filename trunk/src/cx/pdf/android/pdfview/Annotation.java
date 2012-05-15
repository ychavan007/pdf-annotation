package cx.pdf.android.pdfview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.html.WebColors;
import com.itextpdf.text.pdf.CMYKColor;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfBorderArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfIndirectReference;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;

import cx.pdf.android.lib.pagesview.AnnotSql;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;


/**
 * PDF Annotation: Main class of annotations
 * Creating new text, circle and square annotations. Editing, change position and change size of annotation.
 * Date of last change: 20.04.2012
 */
public class Annotation {
	
		private final static String TAG = "annot";
		// Database fields
		private SQLiteDatabase database;
		private static AnnotSql dbAnnot;
		private PdfReader reader = null;
	    private ArrayList<Annotation> annotations = new ArrayList<Annotation>();
	    public int annotSize = 20; // default size value
	    public SharedPreferences options = null;
	    private static String TAN = null, TACA = null, TAP = null, TCO = null;
	    ArrayList<ArrayList<String>> annotationArrayListA = new ArrayList<ArrayList<String>>();
	    
	    /** Supported types of annotations. */
	    public CharSequence[] TYPES = {
	    	"Comment", "Key", "Note", "Help", "NewParagraph", "Paragraph", "Insert"
	    };
	    
	    /** Supported subtypes of annotations. */
	    public CharSequence[] SUBTYPES = {
	    	"Text", "Circle", "Square"
	    };
	    
	    /** Supported colors of a new annotations. */
	    public CharSequence[] COLORS = {
	    	"ffffff00", // yellow(0)
	    	"ffff0000", // red(1)
	    	"ff0000ff", // blue(2)
	    	"ff00ff00", // green(3)
	    	"ffff3300", // orange(4)
	    	"ffffffff", // white(5) 
	    	"ff000000", // black(6)
	    	"ff808080"  // gray(7)
	    };
	    
	    /** Annotations to array. */
	    public ArrayList<Annotation> getAnnotations() { 
	    	return annotations; 
	    }
	    
		/** Class constructor. */
		@SuppressWarnings("static-access")
		public Annotation(Context context) {
			dbAnnot = new AnnotSql(context);
			TAN = dbAnnot.TABLE_ANNOTS;
			TAP = dbAnnot.TABLE_APPEARANCE;
			TCO = dbAnnot.TABLE_CONTENTS;
			TACA = dbAnnot.TABLE_ANNOTS_CONTENTS_APPEARANCE;	
		}

		/** Opening a new database connection. */
		public void open() throws SQLException {
			try {
				//if (database == null) {
					database = dbAnnot.getWritableDatabase();
				//}
				
			} catch (SQLException e) {
				Log.w(TAG, "Open database: " + e);
			}
			
		}

		/** Close database connection. */
		public void close() {
			try {
				if (database != null && database.isOpen())
					dbAnnot.close();
			} catch (SQLException e) {
				Log.e(TAG, "Close database: " + e);
			}
		}
		
		/**
		 * Create time stamp to PDF file (for exam: 01.01.1970 01:01:01 converted to
		 * 19700101010101+01'00').
		 * @return String time stamp 
		 */
		private String getTimeStamp () {
	        String format = "yyyyMMddHHmmssZ";
	        SimpleDateFormat sdf = new SimpleDateFormat(format);
	        String tmpdate = (String) sdf.format(new Date());
	        String moddate = "D:" + tmpdate.subSequence(0, 17) + "'" 
	        	+ tmpdate.subSequence(17, 19) + "'";
			return moddate;
		}
		
		/**
		 * Calculate actual annotation size.
		 * @param id identifier of annotation
		 * @return height and width of annotation
		 */
		public ArrayList<String> calcAnnotSize (int id) {
			ArrayList<String> al = new ArrayList<String>();
			Cursor cursor = null;
			float height = 0, width = 0;
			String[] columns = {"llx", "lly", "urx", "ury"};
			cursor = database.query(TAN, columns, " _id = " + id, null, null, null, null);
			
			if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
				height = cursor.getFloat(cursor.getColumnIndex("urx"))
					- cursor.getFloat(cursor.getColumnIndex("llx"));
				width = cursor.getFloat(cursor.getColumnIndex("ury"))
					- cursor.getFloat(cursor.getColumnIndex("lly"));
			}
			
			al.add(Float.toString(height));
			al.add(Float.toString(width));
			return  al;
		}
		
		/**
		 * Get x and y position of annotation.
		 * @param id identifier of annotation
		 * @return x and y position of annotation
		 */
		public ArrayList<String> getXYPosition (int id) {
			ArrayList<String> al = new ArrayList<String>();
			Cursor cursor = null;
			String[] columns = {"llx", "lly", "urx", "ury"};
			cursor = database.query(TAN, columns, " _id = " + id, null, null, null, null);
			
			if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
				al.add(Float.toString(cursor.getFloat(cursor.getColumnIndex("llx"))));
				al.add(Float.toString(cursor.getFloat(cursor.getColumnIndex("lly"))));
				al.add(Float.toString(cursor.getFloat(cursor.getColumnIndex("urx"))));
				al.add(Float.toString(cursor.getFloat(cursor.getColumnIndex("ury"))));
			}
			
			return  al;
		}
		
		/**
		 * Save new annotation position to SQLite database.
		 * @param id identifier of annotation
		 * @param posX x position
		 * @param posY y position
		 * @param action if 0 update position, if 1 update size in edit mode, if 2 confirm update size
		 */
		public void saveNewAnnotPos (int id, float posX, float posY, int action) {
			ContentValues values = new ContentValues();
			ArrayList<String> al = null;
			Cursor cursor = getAnnotById(id);
			int flag = -1;
			
			try {
				cursor.moveToFirst();
				flag = cursor.getInt(cursor.getColumnIndex("flag"));
				switch (action) {
				case 0 :
					al = calcAnnotSize(id);
					values.put("llx", posX);
					values.put("lly", posY);
					values.put("urx", posX + Float.parseFloat(al.get(0)));
					values.put("ury", posY + Float.parseFloat(al.get(1)));
				break;
				case 1 :
					al = getXYPosition(id);
					int ret = 0;
					float llx = Float.parseFloat(al.get(0));
					float lly = Float.parseFloat(al.get(1));
					float urx = Float.parseFloat(al.get(2));
					float ury = Float.parseFloat(al.get(3));

					if (llx > urx) {
						values.put("llx", urx);
						values.put("urx", llx);
					}
					
					if (lly > ury) {
						values.put("lly", ury);
						values.put("ury", lly);
					}
					
					if (llx < urx && lly < ury) {
						return;
					}
						
					
				break;
				case 2 :
				default :
					values.put("urx", posX);
					values.put("ury", posY);
				break;
				}
				
				if (flag == 0) // flag(0) original
					values.put("flag", 1); // flag(1) modified
				
				// update position
				database.update(TAN, values, "_id = " + id, null);	
				
			} catch (Exception e) {
				Log.w(TAG, "Save new annots position to database: " + e);
			}
			
		}
		
		
		/**
		 * Create or update annotation from actual annotation layout.
		 * @param title title of annotation
		 * @param text text of annotation
		 * @param color color of annotation
		 * @param type type of annotation
		 */
		public void writeAnnotation (ArrayList<String> annotList, float llx, float lly) {
			int flag = Integer.parseInt(annotList.get(7));
			int objectid = Integer.parseInt(annotList.get(6));
			int index = Integer.parseInt(annotList.get(8));
			int color = Integer.parseInt(annotList.get(3));
			int type = Integer.parseInt(annotList.get(4));
			int size = Integer.parseInt(annotList.get(5));
			ContentValues values = new ContentValues();
			

			values.put("author", annotList.get(0));
			values.put("subject", annotList.get(1));
			values.put("contents", annotList.get(2));
			values.put("moddate", getTimeStamp());
			
			// insert new annotation to the database
			if (objectid < 0 && flag != 2) {
				values.put("objectid", -1);
				// insert contents
				database.insert(TCO, null, values);
				values.clear();
				
				values.put("page", Integer.parseInt(annotList.get(10)));
				values.put("llx", llx - size/2);
				values.put("lly", lly - size);
				values.put("urx", llx + size/2);
				values.put("ury", lly);
				values.put("type", type > -1 ? this.TYPES[type].toString() : "Note");
				values.put("subtype", this.SUBTYPES[Integer.parseInt(annotList.get(9))].toString());
				values.put("flag", 2); // new annotation flag(2)
				values.put("objectid", -1); // new object without id
				
				// insert annotation
				database.insert(TAN, null, values);
				values.clear();
				
				if (color > -1)	values.put("color", this.COLORS[color].toString());
				values.put("objectid", -1); // new object without id
				values.put("bgcolor", "transparent");
				values.put("bdweight", 2);
				
				// insert appearance
				database.insert(TAP, null, values);

			// update annotation at the database
			} else {
				database.update(TCO, values, "objectid = " + objectid + " AND _id = " + index, null);
				values.clear();
								
				values.put("flag", (flag != 2) ? 1 : flag); // modified annotation flag(1)
				values.put("type", type > -1 ? this.TYPES[type].toString() : "Note");
				database.update(TAN, values, "objectid = " + objectid + " AND _id = " + index, null);
				values.clear();
				
				if (color > -1)	values.put("color", this.COLORS[color].toString());
				values.put("bgcolor", "transparent");
				
				// update appearance
				database.update(TAP, values, "objectid = " + objectid + " AND _id = " + index, null);
			}
			database.close();
			return;
		}
		
		/**
		 * Insert extracted annotations and appearance into SQLite database.
		 * @param an extracted annotations
		 */
		private void insertExtractedAnnots (ArrayList<ArrayList<String>> an) {
			// long ts = System.currentTimeMillis();
			
	    	
			// insert contents to database
			InsertHelper tco = new InsertHelper(database, TCO);
			try {
				for (int o = 0; o < an.size(); o++) {
					// get the InsertHelper ready to insert a single row
					tco.prepareForInsert();
		 
		            // add the data for each column
					tco.bind(tco.getColumnIndex("author"), an.get(o).get(0));
					tco.bind(tco.getColumnIndex("subject"), an.get(o).get(1));
					tco.bind(tco.getColumnIndex("contents"), an.get(o).get(2));
					tco.bind(tco.getColumnIndex("moddate"), an.get(o).get(10));
		            tco.bind(tco.getColumnIndex("objectid"), an.get(o).get(12));
		 
		            // insert the row into the database.
		            tco.execute();
		         }
			    	
			} catch (Exception e) {
				Log.w(TAG, "Cannot insert annotation into database!" + e);
			    	
		    } finally {
		    	tco.close();
		    }
			
			// insert annotation to database
			InsertHelper tan = new InsertHelper(database, TAN);
			try {
				for (int o = 0; o < an.size(); o++) {		 
					// get the InsertHelper ready to insert a single row
					tan.prepareForInsert();
			 
		            // add the data for each column
					tan.bind(tan.getColumnIndex("objectid"), an.get(o).get(12));
					tan.bind(tan.getColumnIndex("page"), an.get(o).get(14));
					tan.bind(tan.getColumnIndex("llx"), an.get(o).get(6));
					tan.bind(tan.getColumnIndex("lly"), an.get(o).get(7));
					tan.bind(tan.getColumnIndex("urx"), an.get(o).get(8));
					tan.bind(tan.getColumnIndex("ury"), an.get(o).get(9));
					tan.bind(tan.getColumnIndex("type"), an.get(o).get(3));
					tan.bind(tan.getColumnIndex("subtype"), an.get(o).get(4));
					tan.bind(tan.getColumnIndex("flag"), an.get(o).get(11));
		 
		            // insert the row into the database.
					tan.execute();
		         }
			    	
			} catch (Exception e) {
				Log.w(TAG, "Cannot insert annotation into database!" + e);
			    	
		    } finally {
		    	tan.close();
		    }
			
			
			// insert appearance to database
			InsertHelper tap = new InsertHelper(database, TAP);
			try {
				for (int o = 0; o < an.size(); o++) {		 
					// get the InsertHelper ready to insert a single row
					tap.prepareForInsert();
			 
		            // add the data for each column
					tap.bind(tap.getColumnIndex("objectid"), an.get(o).get(12));
					tap.bind(tap.getColumnIndex("color"), an.get(o).get(5));
					tap.bind(tap.getColumnIndex("bgcolor"), an.get(o).get(13));
					tap.bind(tap.getColumnIndex("bdweight"), ((an.get(o).size() > 15) ? an.get(o).get(15) : "2"));

		            // insert the row into the database.
					tap.execute();
		         }
			    	
			} catch (Exception e) {
				Log.w(TAG, "Cannot insert annotation into database!" + e);
			    	
		    } finally {
		    	tap.close();
		    }
			   
			// long te = System.currentTimeMillis();
			// Log.i("time ", "TIME INEND " + (te-ts));
			return;
		}
		
		/**
		 * Check flagged annotation into database before exit.
		 * @return true if annotations flagged, false other way
		 */
		public Boolean checkBeforeExit () {
			Cursor cursor = null;
			try {
				open(); // database open (must be here!)
				String[] column = {"COUNT(flag) AS count"};
				cursor = database.query(TAN, column, "flag > 0", null, null, null, null);

				if ((cursor != null) && (cursor.getCount() > 0)) {
					cursor.moveToFirst();
					return (cursor.getInt(cursor.getColumnIndex("count")) > 0)  ? true : false;
				}
				
			} catch (Exception e) {
				Log.e(TAG, "Check before exit: " + e);
				
			} finally {
				if (database != null && database.isOpen()) {
					this.close();
				}
			}
			
			return false;
		}
		
		/**
		 * Select all annotations of a specific file from database.
		 * @param params
		 * @return
		 */
		public Cursor getFileAnnots (int page, int... params) {
			assert params.length <= 1;
		    boolean app = params.length > 0 ? true : false;
			Cursor cursor = null;
			
			try {
				if (app) {
					// select appearance from SQLite database
					cursor = database.query(TAP, null, "objectid = " + params[0], null, null, null, null);
				} else if (page > -1) {
					cursor =  database.rawQuery(AnnotSql.ACAINNER + " AND page = " + (page+1), null);
				} else {
					// select annotations from SQLite database
					cursor =  database.rawQuery(AnnotSql.ACAINNER, null);
				}
				
				if ((cursor != null) && (cursor.getCount() > 0)) {
					cursor.moveToFirst();
				} else {
					// Log.w(TAG, "No annotation into database.");
				}
				
			} catch (Exception e) {
				Log.w(TAG, "Select annots from database: " + e);
			}
			
			return cursor;
		}
		
		/**
		 * Select annotation by id column from database.
		 * @param id identifier of database record
		 * @return data cursor
		 */
		public Cursor getAnnotById (int id) {
			Cursor cursor = null;
			
			try {
				cursor =  database.rawQuery(AnnotSql.ACAINNER + " AND " + TAN + "._id =" + id, null);
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					return cursor;
				}
				
			} catch (Exception e) {
				Log.e(TAG, "SQL get annot by ID: " + e);
			}

			return null;
		}

		/**
		 * Remove an annotation by id column from database.
		 * @param id
		 */
		public void deleteAnnotation(int id) {
			ContentValues values = new ContentValues();
			Cursor cursor = null;
			
			// open database
			if (!database.isOpen()) {
				open();
			}
			
			try {
				// select annotation from database
				cursor = database.query(TAN, null, " _id = " + id, null, null, null, null);
				
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					// set flag(3) deleted
					values.put("flag", 3);
					database.update(TAN, values, " _id = " + id, null);
					
				} else {
					Log.w(TAG, "Remove annotation: " + id);
				}
				
			} catch (android.database.sqlite.SQLiteException e) {
				Log.w(TAG, "Remove annotation id: " + id + " message: " + e);
			}
			
			return;
		}
		
		/**
		 * Create rectangle of position.
		 * @param llx lower left corner X position
		 * @param lly lower left corner Y position
		 * @param urx upper right corner X position
		 * @param ury upper right corner Y position
		 * @return rectangle of position.
		 */
		protected Rectangle createPosition(float llx, float lly, float urx, float ury) {
	        Rectangle rect = new Rectangle(llx, lly, urx, ury);
	        
	        return rect;
	    }
		
		/**
		 * Convert integer type of annotation to String.
		 * @param type number of type
		 * @return String
		 */
		protected String getType (int type) {
			return (String) TYPES[type];
		}
		
		/**
		 * Delete annotations into SQLite database.
		 */
		public void truncateAnnots () {
			try {
				database.delete(TAN, null, null);
				database.delete(TCO, null, null);
				database.delete(TAP, null, null);
			} catch (Exception e) {
				Log.e(TAG, "Database error (delete tables): " + e);
			}
		}
		
		/**
		 * Create a PDF color with integer values ranging from 0 to 255.
		 * @param red RGB red
		 * @param green RGB green
		 * @param blue RGB blue
		 * @return PDF Color array
		 */
		private float[] getColorArray (float red, float green, float blue) {
			float[] colorArray = { 
        			(float) (((double) red)   / 255d), 
        			(float) (((double) green) / 255d),
                    (float) (((double) blue)  / 255d) 
            };
			
			return colorArray;
		}
		
		/**
		 * Create a PDF position array.
		 * @param rect (llx, lly, urx, ury)
		 * @return positionArray [llx, lly, urx, ury]
		 */
		private float[] getPositionArray (Rectangle rect) {
			float[] positionArray = { 
				rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop() 
            };
			
			return positionArray;
		}
		
		/**
		 * Save modified, inserted or removed annotations to PDF file. 
		 * @param filePath	file path of reading file
		 * @param newFileName	new entered file name
		 */
		public boolean saveAnnotToFile (File oldFile, File newFile) {
			Cursor cursor = null;
			boolean confirm = false;

			cursor = getFileAnnots(-1);
			Rectangle rect = null;

		    try {
		        
		        cursor.moveToFirst();
		        PdfReader pdfReader = new PdfReader(oldFile.getPath());
		        PdfStamper pdfStamper = new PdfStamper(pdfReader, new FileOutputStream(newFile+".tmp"));
		        do {

		        	if (cursor == null || cursor.getCount() < 1) {
		        		// Log.w(TAG, "No annotations to save to this PDF file.");
		        		break;
		        	}
		    		        	
		        	/** annotation was edited flag(1) */
		        	if (cursor.getInt(cursor.getColumnIndex("flag")) == 1) {
				        PdfObject obj = (PdfObject) pdfReader.getPdfObject(
				        		cursor.getInt(cursor.getColumnIndex("objectid")));

				        if (obj.isDictionary()) {
				            PdfDictionary dict = (PdfDictionary) obj; 


				            if (dict.get(PdfName.TYPE) == PdfName.ANNOT) {
				            	
				            	dict.remove(PdfName.AP); // remove an appearance dictionary
				            	
				            	// create rectangle of position
					            rect = createPosition(
					            	cursor.getFloat(cursor.getColumnIndex("llx")), 
					            	cursor.getFloat(cursor.getColumnIndex("lly")), 
					            	cursor.getFloat(cursor.getColumnIndex("urx")), 
					            	cursor.getFloat(cursor.getColumnIndex("ury"))
					            );
					            dict.put(PdfName.RECT, new PdfArray(getPositionArray(rect)));

				            	dict.put(PdfName.T, // set a new author
				            			new PdfString(cursor.getString(cursor.getColumnIndex("author")), PdfObject.TEXT_UNICODE));

				            	dict.put(new PdfName("Subj"), // set a new subject
				            			new PdfString(cursor.getString(cursor.getColumnIndex("subject")), PdfObject.TEXT_UNICODE));

				            	dict.put(PdfName.CONTENTS, // set new contents
				            			new PdfString(cursor.getString(cursor.getColumnIndex("contents")), PdfObject.TEXT_UNICODE));

				            	dict.put(PdfName.NAME, // set new type
				            			new PdfName(cursor.getString(cursor.getColumnIndex("type")).toString()));

				            	dict.put(new PdfName("M"), // set new date of modified
				            			new PdfString(cursor.getString(cursor.getColumnIndex("moddate"))));
				            	
				            	if (!cursor.getString(cursor.getColumnIndex("color")).equalsIgnoreCase("unknown")) {
					            	int ci = Color.parseColor("#" + cursor.getString(cursor.getColumnIndex("color")));

					            	dict.put(PdfName.C, // set new color
					            		new PdfArray(getColorArray(Color.red(ci), Color.green(ci), Color.blue(ci))));
					            }
				            } 
				            
				        }
				        
				    /** annotation was inserted flag(2) */
		        	} else if (cursor.getInt(cursor.getColumnIndex("flag")) == 2) {
		        
		        		// synchronize PDF page and cursor page
		        		if (cursor != null) {
		        			
					        // create rectangle
						    rect = createPosition(
						    	cursor.getFloat(cursor.getColumnIndex("llx")), 
						      	cursor.getFloat(cursor.getColumnIndex("lly")), 
						      	cursor.getFloat(cursor.getColumnIndex("urx")), 
						      	cursor.getFloat(cursor.getColumnIndex("ury"))
						    );
						            
						    // create annotation by subtype
						    PdfAnnotation annotation = null;
						    switch (getSubtypeNum(cursor.getString(cursor.getColumnIndex("subtype")))) {
						    case 0 : // create text
							    annotation = PdfAnnotation.createText(
							    	pdfStamper.getWriter(), 
							     	rect, 
							      	cursor.getString(cursor.getColumnIndex("author")),
							       	cursor.getString(cursor.getColumnIndex("contents")),
							        false, 
							        cursor.getString(cursor.getColumnIndex("type"))
							    );
						    break;
						    case 1 : // create circle
						      	annotation = PdfAnnotation.createSquareCircle(
							        pdfStamper.getWriter(), 
							        rect, 
							        cursor.getString(cursor.getColumnIndex("contents")),
									false
								);
						         	annotation.setBorder(new PdfBorderArray(0, 0, (cursor.getInt(cursor.getColumnIndex("bdweight")))
						            	/*, new PdfDashPattern()*/));
						            annotation.put(PdfName.NAME, 
						            	new PdfString(cursor.getString(cursor.getColumnIndex("type"))));
						            annotation.put(PdfName.T, 
							           		new PdfString(cursor.getString(cursor.getColumnIndex("author")), 
							           		PdfObject.TEXT_UNICODE));
						    break;
						    case 2 : // create square
						            	annotation = PdfAnnotation.createSquareCircle(
										    pdfStamper.getWriter(), 
										    rect, 
										    cursor.getString(cursor.getColumnIndex("contents")),
										    true
						            	);
						            	annotation.setBorder(new PdfBorderArray(0, 0, (cursor.getInt(cursor.getColumnIndex("bdweight")))
						            		/*, new PdfDashPattern()*/));
						            	annotation.put(PdfName.NAME, 
						            		new PdfString(cursor.getString(cursor.getColumnIndex("type"))));
						            	annotation.put(PdfName.T, 
							            		new PdfString(cursor.getString(cursor.getColumnIndex("author")), 
							            		PdfObject.TEXT_UNICODE));
						    break;
						    }
						            
						            // set type of object (annotation object type)
						            annotation.put(PdfName.TYPE,  PdfName.ANNOT);
						            
						            // set subtype
						            String subtype = cursor.getString(cursor.getColumnIndex("subtype"));
						            if (subtype != null || subtype != "unknown") {
						            	annotation.put(PdfName.SUBTYPE, new PdfName(subtype));
						            }
						            
						            // set subject
						            String subject = cursor.getString(cursor.getColumnIndex("subject"));
						            if (subject != null || subject != "unknown") {
						            	annotation.put(new PdfName("Subj"), new PdfString(cursor.getString(cursor.getColumnIndex("subject")), 
							            	PdfObject.TEXT_UNICODE));
						            }
						            
					        // set date of modified
					       	if (!cursor.getString(cursor.getColumnIndex("moddate")).equalsIgnoreCase("unknown")) {
					          	annotation.put(new PdfName("M"), 
					        		new PdfString(cursor.getString(cursor.getColumnIndex("moddate")).toString()));
					        }
						            
					        // set color
						    if (!cursor.getString(cursor.getColumnIndex("color")).equalsIgnoreCase("unknown")) {
						      	String c = cursor.getString(cursor.getColumnIndex("color")).substring(2);
						    	annotation.setColor(WebColors.getRGBColor("#" + c));
						    }
						    pdfStamper.addAnnotation(annotation, cursor.getInt(cursor.getColumnIndex("page")));		        			
		        		}

				       
				    /** annotation was deleted flag(3) */
		        	} else if (cursor.getInt(cursor.getColumnIndex("flag")) == 3) {

		        		PdfDictionary page;
		        		PdfArray annotsArray;
		        		try {
			        		// actual object identifier
			        		PdfObject obj = (PdfObject) pdfReader.getPdfObject(
		                			cursor.getInt(cursor.getColumnIndex("objectid")));
			        		int pageNo = cursor.getInt(cursor.getColumnIndex("page"));
			        		
			                   page = pdfReader.getPageN(pageNo);
			                   annotsArray = page.getAsArray(PdfName.ANNOTS);
			                   
			                   if (annotsArray != null) {
				                   for (int j = 0; j < annotsArray.size(); j++) {
				                	   PdfIndirectReference ir = annotsArray.getAsIndirectObject(j);
				                	   PdfObject o = pdfReader.getPdfObject(ir.getNumber());
				                	   // delete annotation
				                	   if (o.equals(obj)) {
				                		   annotsArray.remove(j);
				                		   break;
				                	   } 
				                    }
			                   } 
			                   
		        		} catch (Exception e) {
		        			Log.w(TAG, "Delete annotation: null pointer " + e);
		        		}
	
		        	}

			        cursor.moveToNext();
			        
		        } while (!cursor.isAfterLast());
		        
		        // confirm changes and close
	            pdfStamper.close();
	            pdfReader.close();
	        
		        // save to file
		        saveTo (oldFile, newFile);
		        
		        // successfully saved
		        confirm = true;
		        
		    } catch (IOException e) {
		    	Log.e(TAG, "I/O exception: " + e);
		    } catch (DocumentException e) {
		    	Log.e(TAG, "Document exception: " + e);
		    } catch (java.lang.IllegalStateException e) {
		    	Log.e(TAG, "Illegal state exception: " + e);
		    }
			return confirm;
			 
			
		}
		
		/**
		 * Actualize annotation flag to "original" after save to PDF file.
		 */
		public void actualizeAnnots () {
			ContentValues values = new ContentValues();
			values.put("flag", 0); // flag(0) original
			database.update(TAN, values, null, null);
		}
		
	    /**
	     * Convert PDF date-time format.
	     * @param moddate PDF date
	     * @return application converted date
	     */
	    public String convertDateFormat (String moddate) {
	    	SimpleDateFormat curFormater = new SimpleDateFormat("'D:'yyyyMMddkkmmssZZZZ"); 
			String newDateStr = null;
			
			try {
				Date dateObj = curFormater.parse(moddate.replace("'", ""));
				SimpleDateFormat postFormater = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss"); 
				newDateStr = postFormater.format(dateObj);
	    		 
			} catch (Exception e) {
				Log.w(TAG, "Parsing a date: " + e);
			}
			
			return newDateStr; 
	    }
		
		/**
		 * Get subtype number from a string
		 * @param subtype subtype of annotation
		 * @return number of subtype
		 */
		private int getSubtypeNum (String subtype) {
			int number = 0;
			
			try {
				if (subtype.equalsIgnoreCase("Text")) {
					number = 0;
				} else if (subtype.equalsIgnoreCase("Circle")) {
					number = 1;
				} else if (subtype.equalsIgnoreCase("Square")) {
					number = 2;
				}
				
			} catch (Exception e) {
				Log.e(TAG, "Get subtype of annotation: " + e);
			} 
			
			return number; 
		}
		
		/**
		 * Save changes to PDF file.
		 * @param oldFile reading file
		 * @param newFile writing file
		 */
		private void saveTo (File oldFile, File newFile) {
			// rename temp file
	        File temp = new File(newFile.getParent() + "/" + newFile.getName() +".tmp");
	        File result = new File(newFile.getPath());

	        temp.renameTo(result);
	        return;
		}
		
		/**
		 * Load all annotations from PDF file.
		 * @param filePath file path to viewed file
		 * @param actPage number of actual page
		 */
		public void loadAnnotFromFile (String filePath, int actPage) {
			File file = new File(filePath);
			
			try {
	            this.reader = new PdfReader(file.getAbsolutePath());
	            // extract annotation from file
	            this.extractFromAllPages();
	            
			} catch(FileNotFoundException e) {
	            Log.e(TAG, "File " + file.getAbsolutePath() + " not found.");
	            
	        } catch (IOException e) {
	        	Log.e(TAG, "Unable to read from file " + file.getAbsolutePath() + ".");
	        }
			
			return;
		}
		
		/** 
		 * Read number of pages and extract annotation from file. 
		 */
	    private void extractFromAllPages () {
	        int totalPages = reader.getNumberOfPages();
	        annotationArrayListA.clear();
	        
	        // do not change this !!!
	        for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
	        	this.extract(pageNo);
	        }
	        insertExtractedAnnots(annotationArrayListA);
	        
			// close database connection
			// database.close(); 
			
	        return;
	    }
	    
	    /**
	     * Supported subtype control
	     * @param PdfDctionary annot
	     * @return true if supported, false if not
	     */
	    private boolean isSubtypeSupported (PdfDictionary annot) {
	    	if ("/Text".matches(annot.get(PdfName.SUBTYPE).toString()) 
	    		|| "/Square".matches(annot.get(PdfName.SUBTYPE).toString())
	    		|| "/Circle".matches(annot.get(PdfName.SUBTYPE).toString())) {
	    		return true;
	    	}
	    	
	    	return false;
	    }
	    
	    private String getPdfColor (PdfDictionary annot, PdfName pdfName) {
	    	String color = null;
           	float cyan=0.0f, magenta=0.0f, yellow=0.0f, black=0.0f;
           	PdfArray cArr = annot.getAsArray(pdfName);
           	if (cArr == null) return new String("transparent");
           	// get CMYK colors
           	cyan = cArr.getAsNumber(0).floatValue();
           	magenta = cArr.getAsNumber(1).floatValue();
           	yellow = cArr.getAsNumber(2).floatValue();
            if (cArr.size() > 4) black = cArr.getAsNumber(3).floatValue();
            	CMYKColor cmyk = new CMYKColor(cyan, magenta, yellow, black);

            	switch (cArr.size()) {
            	case 1 : // no color, transparent
            		color = new String("transparent");
            		break;
            	case 2 : // device gray color
            		Log.w(TAG, "Device gray color not supported.");
            		break;
            	case 3 : // device RGB color
            		int cRGB = Color.rgb(255-cmyk.getRed(), 255-cmyk.getGreen(), 255-cmyk.getBlue());	
                	// CMYK to HEX conversion 
                	color = Integer.toHexString(cRGB);
            		break;
            	case 4 : // device CMYK color
            		Log.w(TAG, "Device CMYK color not supported.");
            		break;
            	default :
            		Log.w(TAG, "Unsupported type of colors.");
            		break;
            	
            	}
                return color;
	    }
	    
		/**
		 * Set a new size of annotation from insert menu
		 * @param size index of size size(0): small, size(1): medium, size(2): big
		 * @return real size of annotation
		 */
		public int getNewSize (int size) {
			int newSize = 0;
			switch (size) {
				case 0 : newSize = 20; break;
				case 1 : newSize = 30; break;
				case 2 : newSize = 40; break;
			}
			return newSize;
		}
	    
	    private String getPdfObjectId (PdfObject itob) {
	    	 try {
				Pattern p = Pattern.compile("(\\d+)(\\s+)(.*)"); // for example: 155 0 R
				Matcher m = p.matcher(itob.toString());
				if (m.find()) {

				   return m.group(1);
				    	
				} else {
				   Log.w(TAG, "Don't match the pattern: " + m.group(0) + " find: " + m.group(1));
				}
			 } catch (Exception e) {
				 Log.w(TAG, "Iterator: " + itob.toString());
			 }
	    	 
	    	 return null;
	    }
	    

	    /**
	     * Extract annotations from file.
	     * @param pageNumber number of page
	     */
		private void extract (int pageNumber) {

			
			PdfDictionary page = reader.getPageN(pageNumber);
			
		    // annotation not found in specific file
		    if (page ==  null || page.getAsArray(PdfName.ANNOTS) == null) { 
		      	Log.w(TAG, "Annotations not found " + ((page == null) ? "" : "page: " + pageNumber));
		        return; 
		    }


		    PdfArray annots = page.getAsArray(PdfName.ANNOTS);
		    for (Iterator<PdfObject> i = annots.listIterator(); i.hasNext();) {
		    	PdfObject itob = (PdfObject) i.next();
		     	PdfDictionary annot = (PdfDictionary) PdfReader.getPdfObject(itob);
		     	ArrayList<String> annotationArrayList = new ArrayList<String>();
		     	
		        // check subtype of annotation (supported: text, square, circle)
		        if (isSubtypeSupported(annot)) {
		        	
		        	// set unmarked annotation invisible
		        	if (annot.get(PdfName.STATE) != null) {
			       		if (annot.get(PdfName.AUTHOR) != null)
			       		if ("Unmarked".matches(annot.get(PdfName.STATE).toString()))
			       			continue;
		        	}
		        	/** get author */
			        try {
			        	annotationArrayList.add(annot.getAsString(PdfName.T).toUnicodeString()); // (0) author
			        } catch (Exception e) {
			           	Log.w(TAG, "Author: " + e);
			           	annotationArrayList.add("unknown");
			        }
			       
			        /** get subject */
			        try {
			        	annotationArrayList.add(annot.getAsString(new PdfName("Subj")).toUnicodeString()); // (1) subject
			        } catch (Exception e) {
			          	annotationArrayList.add("unknown");
			        }
			           
			        /** get text */
			        try {
			        	
			        	annotationArrayList.add(annot.getAsString(PdfName.CONTENTS).toUnicodeString()); // (2) text
			        } catch (Exception e) {
			          	annotationArrayList.add("unknown");
			        }
			            
			        /** get type */
			        try {
			        	annotationArrayList.add(annot.get(PdfName.NAME).toString().replace("/", "")); // (3) type
			        } catch (Exception e) {
			          	annotationArrayList.add("Note");
			        }
			        
			        /** get subtype */
			        try {
			        	annotationArrayList.add(annot.get(PdfName.SUBTYPE).toString().replace("/", "")); // (4) subtype
			        } catch (Exception e) {
			          	annotationArrayList.add("Text");
			        }
			            
			        /** get color */
			        annotationArrayList.add(getPdfColor(annot, PdfName.C)); // (5) color
			            
			        /** get rectangle of position */
			        // read position (PDF rectangles are stored as [llx, lly, urx, ury])
		            PdfArray rectArr = annot.getAsArray(PdfName.RECT); 
		            float llx = rectArr.getAsNumber(0).floatValue();
	                float lly = rectArr.getAsNumber(1).floatValue();
	                float urx = rectArr.getAsNumber(2).floatValue();
	               	float ury = rectArr.getAsNumber(3).floatValue();

	               	annotationArrayList.add(Float.toString(llx)); // (6) rectangle
	               	annotationArrayList.add(Float.toString(lly));
	               	annotationArrayList.add(Float.toString(urx));
	               	annotationArrayList.add(Float.toString(ury));
				    
				    try {
				    	annotationArrayList.add(annot.get(PdfName.M).toString()); // (10) date of annotation modified

			        } catch (Exception e) {
			           	Log.w(TAG, "Author: " + e);
			           	annotationArrayList.add("unknown");
			        }
				        
				    /** set flag */
				    annotationArrayList.add(Integer.toString(0)); // AN(11) flag original(0)
				    
				    /** set object ID */ 
				    annotationArrayList.add(getPdfObjectId(itob)); // AN(12) object ID

				    /** set appearance */
				    // appearance supported for circle and square only
			    	if (getSubtypeNum((annot.get(PdfName.SUBTYPE)).toString().replace("/", "")) > 0) {
			    		annotationArrayList.add(getPdfColor(annot, new PdfName("IC"))); // AP(13) background color
			    	} else {
			    		annotationArrayList.add("transparent");
			    	}
			    	
			    	/** set page number */ 
				    annotationArrayList.add(Integer.toString(pageNumber)); // AN(14) page
				    
		        	/** get border */
		        	PdfArray border = annot.getAsArray(PdfName.BORDER); 
		        	int borderWeight = 0;
		        	if (border != null) {
		        		if (border.getAsNumber(2).isNumber()) {
			        		borderWeight = border.getAsNumber(2).intValue();
		        		}
		        		annotationArrayList.add(Integer.toString(borderWeight)); // AP(15) border weight
		        	} else {
		        		annotationArrayList.add("2"); // AP(15) border weight
		        	}
		        	
		        	// insert extracted informations
		        	annotationArrayListA.add(annotationArrayList);
			        // annotationArrayList.clear(); // clear annotation array list
			        
		        } else {
		          	// unknown subtype of annotation
		           	Log.w(TAG, "Unsupported subtype: " + annot.get(PdfName.SUBTYPE).toString() );
		           	if (annot.get(PdfName.T) != null)
		           		Log.w(TAG, "title: " + annot.get(PdfName.T).toString());
		        }
    
		    }
		    

			
			return;
		}
}

