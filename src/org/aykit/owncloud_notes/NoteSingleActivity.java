package org.aykit.owncloud_notes;

import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;
import org.javalite.http.Get;
import org.javalite.http.Http;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NoteSingleActivity extends Activity {
	
	public static final String TAG = NoteSingleActivity.class.getSimpleName();
	private final String user = "steppe_testuser";
	private final String password ="kenny";
	private final String query = "?id=1";
	//https://user:password@yourowncloud.com/index.php/apps/notes/api/v0.2/
	private String theUrl = "https://cloud.gerade.org/index.php/apps/notes/api/v0.2/notes/14582";
	private String theUrl2 = "https://steppe_testuser:kenny@cloud.gerade.org/index.php/apps/notes/api/v0.2/notes";
	private String twitterURL = "https://www.twitter.com";
	
	public static String note ="nothing";
	private EditText editTextContent;
	private EditText editTextTitle;
	private String title;
	private String content;
	private long id;
	private boolean isNewNote;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_single);
		
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
			
			editTextContent.setText(content);
			editTextTitle.setText(title);
			
			if(id == -1)
			{
				//something is wrong with this entry
			}
		}
		
		
	}
	
	public void testGetNoteFromWeb(String anUrl)
	{
		new DownloadNoteTask().execute(anUrl );
	}
	
	public void setView(String textString)
	{
		this.editTextContent.setText(textString);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note_single, menu);
		return true;
	}
	
	public void button_save(View view)
	{
		//save button clicked. save note and close note
		saveNote();
		finish();
	}
	
	public void button_delete(View view)
	{
		if(id == -1)
		{
			//unsaved note - nothing to delete
			finish();
		}
		else
		{
			NotesOpenHelper notesOpenHelper = new NotesOpenHelper(this);
			SQLiteDatabase sqlDatabase = notesOpenHelper.getWritableDatabase();
			
			String whereClause = NotesTable.COLUMN_ID + " = ?";
			String[] whereArgs = { Long.toString(id) };
			sqlDatabase.delete(NotesTable.NOTES_TABLE_NAME, whereClause, whereArgs);
			Toast.makeText(this, "note deleted, id=" + id, Toast.LENGTH_SHORT).show();
			sqlDatabase.close();
			
			finish();
		}
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
				
			default :
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void saveNote()
	{
		//save or update currently opened note
		Log.d(TAG, "saving note");
		Toast toast = Toast.makeText(this, R.string.note_has_been_saved, Toast.LENGTH_SHORT);
		toast.show();
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
			long inserted = sqlDatabase.insert(NotesTable.NOTES_TABLE_NAME, null, values);
			Log.d(TAG, "new note saved successfully: " + inserted);
			
		}
		else
		{
			//must update existing NoteTable
			String selection = NotesTable.COLUMN_ID + " = ?";
			String[] selectionArgs = { Long.toString(id) };
			
			ContentValues values = new ContentValues();
			values.put(NotesTable.COLUMN_TITLE, newTitle );
			values.put(NotesTable.CLOUMN_CONTENT, newContent);
			
			sqlDatabase.update(NotesTable.NOTES_TABLE_NAME, values, selection, selectionArgs);
			
		}
		Log.d(TAG, "note saved successfully");
		
		sqlDatabase.close();
	}
	
	public void button_test(View view)
	{
		testGetNoteFromWeb(theUrl2);
	}
	
	private class DownloadNoteTask extends AsyncTask<String, Void, String> {
	    /** The system calls this to perform work in a worker thread and
	      * delivers it the parameters given to AsyncTask.execute() */
	    protected String doInBackground(String... anUrl) {
	    	
	    	StringBuilder stringBuilder = new StringBuilder();
	    	URL url;
	    	
			try {
				url = new URL(anUrl[0]);
				HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
				
				/*
				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("GET");
				String auth = user + ":" + password; 
				urlConnection.setRequestProperty("Authorization", auth);
				*/
				Log.d(TAG, "before");
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));  // HIER HAUT IRGENDWAS NICHT GANZ HIN :(((
				
				Log.d(TAG, "after");
				String line;
				while( (line = reader.readLine() ) != null)
				{
					stringBuilder.append(line);
					Log.d(TAG, "line:" + line);
				}
			} 
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
				Log.e(TAG, e.toString());
				
			}
	    	catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.toString());
			}
	    	
	    	
	    	return stringBuilder.toString();
	    	
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(String result) {
	    	setView(result);
	    }
	    
	    protected void onProgressUpdate() {
	    	Log.d(TAG, "doing....");
	         
	    }
	}
}
