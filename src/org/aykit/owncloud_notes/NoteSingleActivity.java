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
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NavUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("SimpleDateFormat")
public class NoteSingleActivity 
	extends Activity 
	implements OnClickListener,	OnLongClickListener
{
	
	public static final String TAG = NoteSingleActivity.class.getSimpleName();
	
	private NotesOpenHelper notesOpenHelper;
	private SQLiteDatabase sqlDatabase;
	
	private EditText editTextContent;
	private TextView textViewContent;
	private String title;
	private String content;
	private String status = "";
	private long id;
	private boolean isNewNote;
	private boolean noButtonWasPressed;
	private boolean wasPaused;
	private boolean wasCreatedBefore;
	private boolean debugOn;
	private boolean isEditable;
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		debugOn = settings.getBoolean(SettingsActivity.PREF_EXTENSIVE_LOG, false);
		id = -1;
		
		wasCreatedBefore = settings.getBoolean("wasCreatedBefore", false);
		
		if(!wasCreatedBefore)
		{
			//this note's onCreate()-method is genuinely called for the first time.
			wasCreatedBefore = true; //now remember that is was called once.
			Intent intent = getIntent();
			isNewNote = intent.getBooleanExtra("isNewNote", false);
			
			if(!isNewNote)
			{	//opening saved note
				setContentView(R.layout.activity_note_single_textview);
				textViewContent = (TextView) findViewById(R.id.textview_note_content);
				isEditable = false;
				
				//load note-data from intent
				content = intent.getStringExtra("content");
				title = getFirstLineOf(content);

				getActionBar().setTitle(title);
				id = intent.getLongExtra("id", -1);
				//Log.d(TAG, "id from intent: " + id);
				status = intent.getStringExtra("status");

				textViewContent.setText(content);
				
				if(id == -1)
				{
					//something is wrong with this entry
					Log.e(TAG, "there was a problem with this note:" + title);
				}
				
				
				//make sure that keyboard is not shown right up
				//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
				textViewContent.setClickable(true);
				textViewContent.setMovementMethod(LinkMovementMethod.getInstance());
				textViewContent.setLongClickable(true);
				textViewContent.setOnClickListener(this);
				textViewContent.setOnLongClickListener(this);
			}
			else
			{
				//new note
				setContentView(R.layout.activity_note_single);
				editTextContent = (EditText) findViewById(R.id.edittext_note_content);
				isEditable = true;
				
				boolean useDateAndTimeAsDefaultTitle = settings.getBoolean(SettingsActivity.PREF_DEFAULT_TITLE, true); 
				if(useDateAndTimeAsDefaultTitle)
				{
					//set note title to current date and time
					Calendar calendar = Calendar.getInstance();
					Date date = calendar.getTime();
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
					String dateAndTime = format.format(date);
					
					editTextContent.setText(dateAndTime + "\n");
					editTextContent.setSelection( editTextContent.getText().toString().length() );
				}
				
				getActionBar().setTitle(R.string.new_note);
			}
		}
		else
		//this note's onCreate()-method has been already called some time ago.
		{
			//get saved content from preferences
			content = settings.getString("content", "");
			isEditable = settings.getBoolean("isEditable", false);
			
			getActionBar().setTitle( getFirstLineOf(content ) );
			
			if(isEditable)
			{
				setContentView(R.layout.activity_note_single);
				editTextContent = (EditText) findViewById(R.id.edittext_note_content);
				
				editTextContent.setText( content );
			}
			else
			{
				setContentView(R.layout.activity_note_single_textview);
				textViewContent = (TextView) findViewById(R.id.textview_note_content);
				
				textViewContent.setClickable(true);
				textViewContent.setLongClickable(true);
				textViewContent.setMovementMethod(LinkMovementMethod.getInstance());
				textViewContent.setOnClickListener(this);
				textViewContent.setOnLongClickListener(this);
				
				textViewContent.setText(content);
			}
		}
		
		wasPaused = false;
	}
	
	@Override
	public void onClick(View v) 
	{
		
		int start = textViewContent.getSelectionStart();
		setContentView(R.layout.activity_note_single);
		
		editTextContent = (EditText) findViewById(R.id.edittext_note_content);
		isEditable = true;
		
		editTextContent.setText(content);
		editTextContent.requestFocus();
		if(start >= 0)
		{
			editTextContent.setSelection(start);
		}
		else
		{
			editTextContent.setSelection(0);
		}
		InputMethodManager imm = (InputMethodManager)this.getSystemService(Service.INPUT_METHOD_SERVICE);
	    imm.showSoftInput(editTextContent, InputMethodManager.SHOW_IMPLICIT);
	}
	
	@Override
	public boolean onLongClick(View v) {
		onClick(v);
		return false;
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
			content = settings.getString("content", "");
			id = settings.getLong("id", -1);
			status = settings.getString("status", "");
			isEditable = settings.getBoolean("isEditable", false);
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
		editor.putBoolean("wasPaused", wasPaused);
		editor.putBoolean("wasCreatedBefore", true);
		editor.putBoolean("isEditable", isEditable);
		
		if (noButtonWasPressed)
		{
			if(isEditable)
			{
				saveNote();
			}
			
			editor.putLong("id", id);
			editor.putString("status", status);
			if(isEditable)
			{
				editor.putString("content", editTextContent.getText().toString() );
			}
			else
			{
				editor.putString("content", textViewContent.getText().toString() );
			}
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
			return contentToParse.substring(0, contentToParse.indexOf("\n") );
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
		
		String newContent = "";
		
		if(isEditable)
		{
			newContent = editTextContent.getText().toString();
		}
		else
		{
			newContent = textViewContent.getText().toString();
		}
		
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
