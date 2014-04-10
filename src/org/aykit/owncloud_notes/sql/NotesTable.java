package org.aykit.owncloud_notes.sql;

import android.database.sqlite.SQLiteDatabase;

public class NotesTable {

	
	
	public static final String NOTES_TABLE_NAME = "notes";
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE ="title";
	public static final String CLOUMN_CONTENT ="content";
	public static final String COLUMN_TAGS = "tags";
	
	public static final String[] COLUMNNAMES = {
		COLUMN_ID,
		COLUMN_TITLE,
		CLOUMN_CONTENT,
		COLUMN_TAGS
		};
	
	private static final String NOTES_TABLE_CREATE =
			"CREATE TABLE " + 
			NOTES_TABLE_NAME +
			"( " +
				COLUMN_ID + " INTEGER primary KEY autoincrement, " +
				COLUMN_TITLE + " TEXT, " +
				CLOUMN_CONTENT + " TEXT, " +
				COLUMN_TAGS + " TEXT" +
			");" ;

	public NotesTable()
	{
		
	}
	
	public static void onCreate( SQLiteDatabase db)
	{
		db.execSQL(NOTES_TABLE_CREATE);
		String firstEntry = "INSERT INTO " + NOTES_TABLE_NAME + " VALUES (0, 'the first note', 'this is the content of the first note.', ''), (1, 'the second note','this is the content of the second note', '')";
		db.execSQL(firstEntry);
	}
	
	public static void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//carefull - all content will be deleted - but then again - all content should be in cloud anyways...
		String dropTable = "DROP TABLE IF EXISTS " + NOTES_TABLE_NAME;
		db.execSQL(dropTable);
		
		onCreate(db);
	}
	
	

}//END:CLASS
