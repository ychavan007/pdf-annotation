package cx.pdf.android.lib.pagesview;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * PDF Annotation: SQLite annotation class helper.
 * Creating tables annots, appearance and contents to annots.db database file
 * Date of last change: 20.04.2012
 */
public class AnnotSql extends SQLiteOpenHelper {

	/** Names of tables */
	public static final String TABLE_ANNOTS = "annots";
	public static final String TABLE_APPEARANCE = "appearance";
	public static final String TABLE_CONTENTS = "contents";
	public static final String TABLE_ANNOTS_CONTENTS_APPEARANCE = "annots, contents, appearance";

	private static final String ANNOTATION_DB = "annots.db"; // database file
	private static final String TAG = "annots sqlite"; // debug message tag
	private static final int DATABASE_VERSION = 3; // version of database
	
	/** SQLite statement for create table of annotations */
	public static final String ANNOT_CREATE = "create table IF NOT EXISTS " + TABLE_ANNOTS + "( " 
			+ " _id integer primary key autoincrement, " // identifier
			+ " objectid integer not null, " // PDF object ID
			+ " page integer not null, " // number of page
			+ " type text not null, " // type: (comment(0), key(1), note(2), 
								// help(3), newParagraph(4), paragraph(5), insert(6), etc.)
			+ " subtype text not null, " // subtype (supported: text, circle, square)
			+ " llx float, " // lower left x position
			+ " lly float, " // lower left y position
			+ " urx float, " // upper right x position
			+ " ury float, " // upper right y position
			+ " flag integer " // annotation flag: original(0), modified(1), new(2), deleted(3)
			+ ");";
	
	/** SQLite statement for create table of  */
	public static final String APPEARANCE_CREATE = "create table IF NOT EXISTS " + TABLE_APPEARANCE + "( " 
			+ " _id integer primary key autoincrement, " // SQLite identifier
			+ " objectid integer not null, " // PDF object identifier
			+ " color text not null, " // RGB color
			+ " bgcolor text not null, " // RGB background color
			+ " bdweight integer not null" // border weight
			+ ");";
	
	/** SQLite statement for create table of contents */
	public static final String CONTENTS_CREATE = "create table IF NOT EXISTS " + TABLE_CONTENTS + "( " 
			+ " _id integer primary key autoincrement, " 
			+ " objectid integer not null, " // PDF object identifier
			+ " author text not null, " // author (title by iText)
			+ " subject text not null, " // subject
			+ " contents text not null, " // contents
			+ " moddate text not null " // date of last modified
			+ ");";
	
	public static final String ACAINNER = "SELECT " + TABLE_ANNOTS + ".*, appearance.*, contents.* "
			+ " FROM " + TABLE_ANNOTS + ", appearance, contents "
			+ " WHERE " + TABLE_ANNOTS + ".objectid= appearance.objectid "
			+ " AND " + TABLE_ANNOTS + ".objectid = contents.objectid "
			+ " AND " + TABLE_ANNOTS + "._id = appearance._id "
			+ " AND " + TABLE_ANNOTS + "._id = contents._id";
	
	/** Class constructor */
	public AnnotSql(Context context) {	
		super(context, ANNOTATION_DB, null, DATABASE_VERSION);
	}

	/** Create SQLite database tables  */
	@Override
	public void onCreate(SQLiteDatabase database) {
		try {
			database.execSQL(ANNOT_CREATE);
			database.execSQL(CONTENTS_CREATE);
			database.execSQL(APPEARANCE_CREATE);
			
		} catch (Exception e) {
			Log.w(TAG, "On create: " + e);
		}
	}

	/** Upgrade when reinstall application */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(AnnotSql.class.getName(), "Upgrading database from version " 
			+ oldVersion + " to " + newVersion + ", which will destroy all old data.");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANNOTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTENTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPEARANCE);
		onCreate(db);
	}

}
