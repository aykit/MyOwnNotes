package org.aykit.owncloud_notes;

import org.aykit.MyOwnNotes.R;
import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("SimpleDateFormat")
public class NoteSingleActivity extends Activity {
	
	public static final String TAG = NoteSingleActivity.class.getSimpleName();
	
	private NotesOpenHelper notesOpenHelper;
	private SQLiteDatabase sqlDatabase;
	
	private EditText editTextContent;
	private String title;
	private String content;
	private String status = "";
	private long id;
	private boolean isNewNote;
	private boolean noButtonWasPressed;
	private boolean wasPaused;
	private boolean wasCreatedBefore;
	private boolean debugOn;
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_note_single);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		debugOn = settings.getBoolean(SettingsActivity.PREF_EXTENSIVE_LOG, false);
		
		editTextContent = (EditText) findViewById(R.id.edittext_note_content);
		id = -1;
		
		notesOpenHelper = new NotesOpenHelper(this);
		
		wasCreatedBefore = settings.getBoolean("wasCreatedBefore", false);
		
		if(!wasCreatedBefore)
		{
			//this note's onCreate()-method is genuinely called for the first time.
			wasCreatedBefore = true; //now remember that is was called once.
			Intent intent = getIntent();
			isNewNote = intent.getBooleanExtra("isNewNote", false);
			
			if(!isNewNote)
			{
				//open saved note: load note-data from intent

				content = intent.getStringExtra("content");
				title = getFirstLineOf(content);

				getActionBar().setTitle(title);
				id = intent.getLongExtra("id", -1);
				//Log.d(TAG, "id from intent: " + id);
				status = intent.getStringExtra("status");

				
				editTextContent.setText(content);
				
				if(id == -1)
				{
					//something is wrong with this entry
					Log.e(TAG, "there was a problem with this note:" + title);
				}
				
				//make sure that keyboard is not shown right up
				getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

			}
			else
			{
				//new note
				boolean useDateAndTimeAsDefaultTitle = settings.getBoolean(SettingsActivity.PREF_DEFAULT_TITLE, true); 
				if(useDateAndTimeAsDefaultTitle)
				{
					//set note title to current date and time
					Calendar calendar = Calendar.getInstance();
					Date date = calendar.getTime();
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
					String dateAndTime = format.format(date);
					
					editTextContent.setText(dateAndTime + "\n");
				}
				
				getActionBar().setTitle(R.string.new_note);
			}
		}
		else
		//this note's onCreate()-method has been already called some time ago.
		{
			//get saved content from preferences
			getActionBar().setTitle( getFirstLineOf(settings.getString("content", "") ) );
			editTextContent.setText( settings.getString("content", "") );
		}
		
		wasPaused = false;
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(debugOn)
		{	
			Log.d(TAG, "resuming note");
		}
		noButtonWasPressed = true;
		wasPaused = settings.getBoolean("wasPaused", false);
		if (wasPaused)
		{
			content = editTextContent.getText().toString();
			id = settings.getLong("id", -1);
			status = settings.getString("status", "");
		}
		
		makeSureSqlDatabaseIsOpen();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		if(debugOn)
		{
			Log.d(TAG, "pausing note");
		}
		wasPaused = true;
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("wasPaused", true);
		editor.putBoolean("wasCreatedBefore", true);
		
		if (noButtonWasPressed)
		{
			saveNote();
			
			editor.putLong("id", id);
			editor.putString("status", status);
			editor.putString("content", editTextContent.getText().toString() );
		}
		editor.commit();
		
		if(sqlDatabase != null)
		{
			if( sqlDatabase.isOpen() && ! sqlDatabase.inTransaction() )
			{
				sqlDatabase.close();
				if(notesOpenHelper != null)
				{
					notesOpenHelper.close();
				}
			}
		}
	}
	
	public void onBackPressed()
	{
		noButtonWasPressed = false;
		saveNote();
		super.onBackPressed();
	}
	
	/**
	 * returns the first line of the String contentToParse. looks for the first linebreak (<code>\n</code>)
	 * 
	 * @param contentToParse	String containing content.
	 * @return	String containing the first line of contentToParse - without tailing <code>\n</code> or an empty String if there is no linebreak.
	 */
	private String getFirstLineOf(String contentToParse)
	{
		if(contentToParse.indexOf("\n") != -1)
		{
			return content.substring(0, content.indexOf("\n") );
		}
		else
		{
			return contentToParse;
		}
	}
	
	/**
	 * checks whether <code>notesOpenHelper</code> is open and
	 * whether <code>sqlDatabase</code> is open
	 * and makes sure, that both are.
	 */
	public void makeSureSqlDatabaseIsOpen ()
	{
		if(notesOpenHelper == null)
		{
			notesOpenHelper = new NotesOpenHelper(this);
		}
		
		if(sqlDatabase == null)
		{
			sqlDatabase = notesOpenHelper.getWritableDatabase();
		}
		else
		{
			if( ! sqlDatabase.isOpen() )
			{
				sqlDatabase = notesOpenHelper.getWritableDatabase();
			}
		}
	}
	
	public void setNoteContentView(String textString)
	{
		this.editTextContent.setText(textString);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note_single, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId() )
		{
			case android.R.id.home:
				//save and go back to NoteListActivity
				noButtonWasPressed = false;
				saveNote();
				NavUtils.navigateUpFromSameTask(this);
		        return true;
		        
			case R.id.action_save:
				//save note and go back to NoteListActivity
				noButtonWasPressed = false;
				saveNote();
				finish();
				return true;
				
			case R.id.action_delete:
				//delete this note
				noButtonWasPressed = false;
				deleteNote();
				finish();
				return true;
				
				
			default :
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void saveNote()
	{
		//save or update currently opened note
		Log.d(TAG, "saving note");
		
		String newContent = editTextContent.getText().toString();
		
		makeSureSqlDatabaseIsOpen();
		
		if(isNewNote)
		{
			//no row in table NoteTable to update exists. create new.
			if(debugOn)
			{
				Log.d(TAG, "isNewNote");
			}
			
			if( newContent.equals("") )
			{
				//empty note will not be saved
			}
			else
			{
				ContentValues values = new ContentValues();
				values.put(NotesTable.CLOUMN_CONTENT, newContent );
				values.put(NotesTable.COLUMN_STATUS, NotesTable.NEW_NOTE); //mark note as new note
				id = sqlDatabase.insert(NotesTable.NOTES_TABLE_NAME, null, values);
				status = NotesTable.NEW_NOTE;
				isNewNote = false;
				Toast.makeText(this, R.string.toast_new_note_saved, Toast.LENGTH_SHORT).show();
			}
		}
		else if(status.equals(NotesTable.NEW_NOTE))
		{
			//check whether this new note has been changed before first upload
			if(debugOn)
			{
				Log.d(TAG, "status = new note");
			}
			
			if (! newContent.equals(content) )
			{
				//note is new but was changed before first upload
				//must update existing note in NoteTable
				String selection = NotesTable.COLUMN_ID + " = ?";
				String[] selectionArgs = { Long.toString(id) };
				
				ContentValues values = new ContentValues();
				values.put(NotesTable.CLOUMN_CONTENT, newContent);
				values.put(NotesTable.COLUMN_STATUS, NotesTable.NEW_NOTE); //mark note as new note
				
				sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
				Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
			}
			else
			{
				//nothing to do here. note has not been changed.
				if(debugOn)
				{
					Log.d(TAG, "do nothing, new note");
				}
			}
		}
		else
		{
			//check whether existing note has been changed
			if(debugOn)
			{
				Log.d(TAG, "existing note");
			}
			
			if (! newContent.equals(content) ) 
			{
				//must update existing NoteTable
				if(debugOn)
				{
					Log.d(TAG, "existing note was changed, do save");
				}
				//Log.d(TAG, "content: " + content + "; newContent: " + newContent);
				//Log.d(TAG, "title: " + title + "; newtitle: " + newTitle);
				//Log.d(TAG, "id: " + id);
				
				String selection = NotesTable.COLUMN_ID + " = ?";
				String[] selectionArgs = { Long.toString(id) };
				
				ContentValues values = new ContentValues();
				values.put(NotesTable.CLOUMN_CONTENT, newContent);
				values.put(NotesTable.COLUMN_STATUS, NotesTable.TO_UPDATE); //mark note for update
				
				sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
				Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
			}
			else
			{
				//nothing to do here. note has not been changed.
				if(debugOn)
				{
					Log.d(TAG, "do nothing");
				}
			}
		}
		
		Log.d(TAG, "note saved successfully");
	}
	
	private void deleteNote()
	{
		Log.d(TAG, "deleting note");
		if(id == -1)
		{
			//unsaved note - nothing to delete
			Toast.makeText(this, R.string.toast_note_deleted, Toast.LENGTH_SHORT).show();
		}
		else
		{
			makeSureSqlDatabaseIsOpen();
			
			String selection = NotesTable.COLUMN_ID + " = ?";
			String[] selectionArgs = { Long.toString(id) };
			
			if(!status.equals(NotesTable.NEW_NOTE) ) //note is not new - mark it for deletion
			{
				ContentValues values = new ContentValues();
				values.put(NotesTable.COLUMN_STATUS, NotesTable.TO_DELETE); //mark note for update
				
				sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
				Toast.makeText(this, R.string.toast_note_marked_to_delete, Toast.LENGTH_SHORT).show();
			}
			else //note only saved in local database - delete it only from local database
			{
				String whereClause = NotesTable.COLUMN_ID + " = ?";
				String[] whereArgs = { Long.toString(id) };
				sqlDatabase.delete(NotesTable.NOTES_TABLE_NAME, whereClause, whereArgs);
				Toast.makeText(this, R.string.toast_note_deleted, Toast.LENGTH_SHORT).show();
			}
		}
	}
}//END:class
