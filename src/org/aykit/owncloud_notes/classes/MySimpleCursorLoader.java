package org.aykit.owncloud_notes.classes;

import org.aykit.owncloud_notes.sql.NotesTable;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MySimpleCursorLoader extends SimpleCursorLoader {
	
	private SQLiteDatabase sqlDatabase;
	private String[] projection;
	
	public MySimpleCursorLoader(Context context, SQLiteDatabase sqlDatabase, String[] projection)
	{
		super(context);
		this.sqlDatabase = sqlDatabase;
		this.projection = projection;
	}
	
	@Override
	public Cursor loadInBackground() {
		Cursor cursor = sqlDatabase.query(NotesTable.NOTES_TABLE_NAME,
				projection,
				null, 
				null, 
				null, 
				null, 
				NotesTable.COLUMN_ID + " ASC");
		
		
		return cursor;
	}

}
