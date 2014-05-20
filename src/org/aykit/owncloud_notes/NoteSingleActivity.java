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
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("SimpleDateFormat")
public class NoteSingleActivity extends Activity {
	
	public static final String TAG = NoteSingleActivity.class.getSimpleName();
	
	private EditText editTextContent;
	private EditText editTextTitle;
	private String title;
	private String content;
	private String status;
	private long id;
	private boolean isNewNote;
	private SharedPreferences settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_note_single);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		editTextContent = (EditText) findViewById(R.id.edittext_note_content);
		editTextTitle = (EditText) findViewById(R.id.edittext_note_title);
		id = -1;
		
		Intent intent = getIntent();
		isNewNote = intent.getBooleanExtra("isNewNote", false);
		
		if(!isNewNote)
		{
			//open saved note: load note-data from intent
			content = intent.getStringExtra("content");
			title = intent.getStringExtra("title");
			id = intent.getLongExtra("id", -1);
			status = intent.getStringExtra("status");
			
			editTextContent.setText(content);
			editTextTitle.setText(title);
			
			if(id == -1)
			{
				//something is wrong with this entry
				Log.e(TAG, "there was a problem with this note:" + title);
			}
		}
		else
		{
			boolean useDateAndTimeAsDefaultTitle = settings.getBoolean(SettingsActivity.PREF_DEFAULT_TITLE, true); 
			if(useDateAndTimeAsDefaultTitle)
			{
				//set note title to current date and time
				Calendar calendar = Calendar.getInstance();
				Date date = calendar.getTime();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
				String dateAndTime = format.format(date);
				
				editTextTitle.setText(dateAndTime);
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
				saveNote();
				NavUtils.navigateUpFromSameTask(this);
		        return true;
		        
			case R.id.action_save:
				//save note and go back to NoteListActivity
				saveNote();
				finish();
				return true;
				
			case R.id.action_delete:
				//delete this note
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
		//Log.d(TAG, "saving note");
		NotesOpenHelper notesOpenHelper = new NotesOpenHelper(this);
		SQLiteDatabase sqlDatabase = notesOpenHelper.getWritableDatabase();
		
		String newTitle = editTextTitle.getText().toString();
		String newContent = editTextContent.getText().toString();
		
		if(isNewNote)
		{
			//no row in table NoteTable to update exists. create new.
			ContentValues values = new ContentValues();
			values.put(NotesTable.COLUMN_TITLE, newTitle);
			values.put(NotesTable.CLOUMN_CONTENT, newContent );
			values.put(NotesTable.COLUMN_STATUS, NotesTable.NEW_NOTE); //mark note as new note
			sqlDatabase.insert(NotesTable.NOTES_TABLE_NAME, null, values);
			//Log.d(TAG, "new note saved successfully: " + inserted);
			Toast.makeText(this, R.string.toast_new_note_saved, Toast.LENGTH_SHORT).show();
			
		}
		else if(status.equals(NotesTable.NEW_NOTE))
		{
			//note is new but was changed before first upload
			//must update existing NoteTable
			String selection = NotesTable.COLUMN_ID + " = ?";
			String[] selectionArgs = { Long.toString(id) };
			
			ContentValues values = new ContentValues();
			values.put(NotesTable.COLUMN_TITLE, newTitle );
			values.put(NotesTable.CLOUMN_CONTENT, newContent);
			values.put(NotesTable.COLUMN_STATUS, NotesTable.NEW_NOTE); //mark note as new note
			
			sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
			Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
		}
		else
		{
			//must update existing NoteTable
			String selection = NotesTable.COLUMN_ID + " = ?";
			String[] selectionArgs = { Long.toString(id) };
			
			ContentValues values = new ContentValues();
			values.put(NotesTable.COLUMN_TITLE, newTitle );
			values.put(NotesTable.CLOUMN_CONTENT, newContent);
			values.put(NotesTable.COLUMN_STATUS, NotesTable.TO_UPDATE); //mark note for update
			
			sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
			Toast.makeText(this, R.string.toast_changes_saved, Toast.LENGTH_SHORT).show();
			
		}
		//Log.d(TAG, "note saved successfully");
		
		sqlDatabase.close();
		notesOpenHelper.close();
	}
	
	private void deleteNote()
	{
		if(id == -1)
		{
			//unsaved note - nothing to delete
			Toast.makeText(this, R.string.toast_note_deleted, Toast.LENGTH_SHORT).show();
		}
		else
		{
			
			NotesOpenHelper notesOpenHelper = new NotesOpenHelper(this);
			SQLiteDatabase sqlDatabase = notesOpenHelper.getWritableDatabase();
			
			String selection = NotesTable.COLUMN_ID + " = ?";
			String[] selectionArgs = { Long.toString(id) };
			
			if(!status.equals(NotesTable.NEW_NOTE) ) //if not new note
			{
				ContentValues values = new ContentValues();
				values.put(NotesTable.COLUMN_STATUS, NotesTable.TO_DELETE); //mark note for update
				
				sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
				Toast.makeText(this, R.string.toast_note_marked_to_delete, Toast.LENGTH_SHORT).show();
			}
			else //delete note from sql database
			{
				String whereClause = NotesTable.COLUMN_ID + " = ?";
				String[] whereArgs = { Long.toString(id) };
				sqlDatabase.delete(NotesTable.NOTES_TABLE_NAME, whereClause, whereArgs);
			}
			
			sqlDatabase.close();
			notesOpenHelper.close();
		}
	}
}//END:class
