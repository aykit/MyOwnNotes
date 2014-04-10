package org.aykit.owncloud_notes.sql;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NotesOpenHelper
		extends SQLiteOpenHelper
{
	private static final String TAG = NotesOpenHelper.class.getSimpleName();
	
	private static int DATABASE_VERSION = 1;
	private static String DATABASE_NAME ="notes.db";
	
	
	
	//constructors
	public NotesOpenHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public NotesOpenHelper(Context context, String name, CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}
	
	public NotesOpenHelper(Context context, String name, CursorFactory factory, int version, DatabaseErrorHandler errorHandler) 
	{
		super(context, name, factory, version, errorHandler);
	}
	//END:CONSTRUCTORS
	
	
	
	
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		Log.d(TAG, "creating talbe: " + DATABASE_NAME);
		NotesTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Log.d(TAG, "upgrading database: " + DATABASE_NAME);
		NotesTable.onUpgrade(db, oldVersion, newVersion);
	}
	
	@Override
	public void onOpen(SQLiteDatabase db)
	{
		Log.d(TAG, "opening database: " + DATABASE_NAME + ", version: " + db.getVersion() );
		super.onOpen(db);
	}
	
}
