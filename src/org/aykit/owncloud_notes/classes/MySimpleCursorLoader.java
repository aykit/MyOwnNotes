package org.aykit.owncloud_notes.classes;

import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MySimpleCursorLoader 
	extends SimpleCursorLoader 
{
	private static final String TAG = MySimpleCursorLoader.class.getSimpleName();
	
	private SQLiteDatabase sqlDatabase;
	private String[] projection;
	private NotesOpenHelper notesOpenHelper;
	private Context context;
	
	public MySimpleCursorLoader(Context context, SQLiteDatabase sqlDatabase, String[] projection)
	{
		super(context);
		this.sqlDatabase = sqlDatabase;
		this.projection = projection;
		this.context = context;
	}
	
	@Override
	public Cursor loadInBackground() {
		
		Log.d(TAG, "loading cursor");
		if( sqlDatabase == null)
		{
			notesOpenHelper = new NotesOpenHelper(context);
			sqlDatabase = notesOpenHelper.getWritableDatabase();
		}
		else
		{
			if( ! sqlDatabase.isOpen() )
			{
				notesOpenHelper = new NotesOpenHelper(context);
				sqlDatabase = notesOpenHelper.getWritableDatabase();
			}
		}
		
		Cursor cursor = sqlDatabase.query(NotesTable.NOTES_TABLE_NAME,
				projection,
				null, 
				null, 
				null, 
				null, 
				NotesTable.COLUMN_ID + " DESC");
		
		
		return cursor;
	}

}
