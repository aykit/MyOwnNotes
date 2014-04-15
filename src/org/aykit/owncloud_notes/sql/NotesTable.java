package org.aykit.owncloud_notes.sql;

import android.database.sqlite.SQLiteDatabase;

public class NotesTable {

	
	
	public static final String NOTES_TABLE_NAME = "notes";
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TITLE ="title";
	public static final String CLOUMN_CONTENT ="content";
	public static final String COLUMN_TAGS = "tags";
	public static final String COLUMN_STATUS = "noteStatus";
	
	//values for COLUMN_STATUS
	public static final String TO_DELETE = "toDelete";
	public static final String NEW_NOTE = "newNote";
	public static final String TO_UPDATE = "toUpdate";
	
	public static final String[] COLUMNNAMES = {
		COLUMN_ID,
		COLUMN_TITLE,
		CLOUMN_CONTENT,
		COLUMN_TAGS,
		COLUMN_STATUS
		};
	
	private static final String NOTES_TABLE_CREATE =
			"CREATE TABLE " + 
			NOTES_TABLE_NAME +
			"( " +
				COLUMN_ID + " INTEGER primary KEY, " +
				COLUMN_TITLE + " TEXT, " +
				CLOUMN_CONTENT + " TEXT, " +
				COLUMN_TAGS + " TEXT," +
				COLUMN_STATUS + " TEXT" +
			");" ;

	public NotesTable()
	{
		
	}
	
	public static void onCreate( SQLiteDatabase db)
	{
		db.execSQL(NOTES_TABLE_CREATE);
	}
	
	public static void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion)
	{
		//carefull - all content will be deleted - but then again - all content should be in cloud anyways...
		String dropTable = "DROP TABLE IF EXISTS " + NOTES_TABLE_NAME;
		db.execSQL(dropTable);
		
		onCreate(db);
	}
	
	public static void emptyTheDatabase( SQLiteDatabase db)
	{
		String dropTable = "DROP TABLE IF EXISTS " + NOTES_TABLE_NAME;
		db.execSQL(dropTable);
		onCreate(db);
	}
	
	

}//END:CLASS
