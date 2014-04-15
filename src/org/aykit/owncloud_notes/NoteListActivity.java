package org.aykit.owncloud_notes;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.aykit.MyOwnNotes.R;
import org.aykit.owncloud_notes.classes.MySimpleCursorLoader;
import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class NoteListActivity 
	extends ListActivity 
	implements LoaderCallbacks<Cursor>
{
	public static final String TAG = NoteListActivity.class.getSimpleName();
	
	
	private NotesOpenHelper notesOpenHelper;
	private SQLiteDatabase sqlDatabase;
	private SimpleCursorAdapter simpleCursorAdapter;
	private LoaderManager loaderManager;
	private SharedPreferences settings;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);
		
		updateSettings();
		loaderManager = getLoaderManager();
		notesOpenHelper = new NotesOpenHelper(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		updateSettings();
		if(settings.getBoolean(SettingsActivity.PREF_AUTOSYNC, true) &&
				settings.getBoolean(SettingsActivity.PREF_INITIALIZED, false) )
		{
			synchronizeNotes();
		}
		showAndFillListView();
	}
	
	@SuppressWarnings("deprecation")
	private void showAndFillListView()
	{
		sqlDatabase = notesOpenHelper.getWritableDatabase();
		
		String[] from = { NotesTable.COLUMN_TITLE, NotesTable.CLOUMN_CONTENT, NotesTable.COLUMN_STATUS };
		int[] to = {R.id.textview_note_row_title, R.id.textview_note_row_content, R.id.textview_note_row_marked };
		simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.note_listview_row, null, from, to);
		
		if(loaderManager.getLoader(1) != null) 
		{
			loaderManager.destroyLoader(1);
		}
		
		loaderManager.initLoader(1, null, this);
		
		setListAdapter(simpleCursorAdapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
	    // Handle presses on the action bar items
		Intent intent = null;
	    switch (item.getItemId() ) 
	    {
	        case R.id.action_new:
	            //make new note
	        	//Log.d(TAG, "menu: create new noten");
	        	intent = new Intent(this, NoteSingleActivity.class);
	        	intent.putExtra("isNewNote", true);
	        	startActivity(intent);
	            return true;
	            
	        case R.id.action_sync:
	        	//start synchronizing
	        	//Log.d(TAG, "menu: start sync");
	        	synchronizeNotes();
	        	return true;
	        	
	        case R.id.action_settings:
	            //go to settings
	        	//Log.d(TAG, "menu: open settings");
	        	intent = new Intent(this, SettingsActivity.class);
	        	startActivity(intent);
	            return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
		Intent intent = new Intent(this, NoteSingleActivity.class);
		intent.putExtra("isNewNote", false); //tell intent that no new note must be created
		
		String title = ((TextView) v.findViewById(R.id.textview_note_row_title)).getText().toString();
		String content = ((TextView) v.findViewById(R.id.textview_note_row_content)).getText().toString();
		String status = ( (TextView) v.findViewById(R.id.textview_note_row_marked)).getText().toString();
		
		intent.putExtra("title", title);
		intent.putExtra("content", content);
		intent.putExtra("id", id);
		intent.putExtra("status", status);
		 
		startActivity(intent);
	}
	
	public void updateSettings()
	{
		settings = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	/**
	 * initiates synchronization with owncloud server.
	 * follows this order:
	 * <li>upload all new notes (marked <code>NEW_NOTE</code>)</li>
	 * <li>update all modified notes (marked <code>TO_UPDATE</code>)</li>
	 * <li>delete all notes marked <code>TO_DELETE</code></li>
	 * <li>download all notes</li>
	 */
	public void synchronizeNotes()
	{
		//push new notes
		//get all notes
		updateSettings(); //get newest login-data in case it has changed
		Log.d(TAG, "starting note synchonization");
		showProgressBar();
		
		String username = settings.getString(SettingsActivity.PREF_USERNAME, "username"); 			//defaultvalue = "username"
		String password = settings.getString(SettingsActivity.PREF_PASSWOORD, "password"); 			//defaultvalue = "password"
		String serverUrl = settings.getString(SettingsActivity.PREF_ADDRESS, "https://www.example.com");	//defaultvalue = "https://www.example.com"
		String urlToConnect = "";
		
		try
		{
			URL tempUrl = new URL(serverUrl);
			//must be like: https://user:password@yourowncloud.com/index.php/apps/notes/api/v0.2/notes
			String basePath = tempUrl.getHost() + "/index.php/apps/notes/api/v0.2/notes";
			urlToConnect = "https://" + username + ":" + password + "@" + basePath;
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace();
			Log.e(TAG, "tempUrl malforemd: String=" + serverUrl);
		}
		
		//upload new notes
		Log.d(TAG, "writing new notes to server");
		writeNewNotesToServer(urlToConnect);
		
		//update modified notes
		Log.d(TAG, "writing modified notes to server");
		writeModifiedNotesToServer(urlToConnect);
		
		//delete marked notes
		Log.d(TAG, "deleting notes from server");
		deleteMarkedNotesFromServer(urlToConnect);
		
		
		//get all notes
		new DownloadNotesTask().execute(urlToConnect);
		//rest done in updateDatabase(), which is called when download is finished.
	}
	
	private void showProgressBar()
	{
		ProgressBar pBar = (ProgressBar) findViewById(R.id.pbar);
		pBar.setVisibility(ProgressBar.VISIBLE);
	}
	
	private void hideProgressBar()
	{
		ProgressBar pBar = (ProgressBar) findViewById(R.id.pbar);
		pBar.setVisibility(ProgressBar.GONE);
	}
	
	public void updateDatabase(String result)
	{
		//update the database with "result" (= a json with _all_ notes)
		//Toast.makeText(this, result, Toast.LENGTH_LONG).show();
		
		if(!sqlDatabase.isOpen() )
		{
			sqlDatabase = notesOpenHelper.getWritableDatabase();
		}
		
		notesOpenHelper.emptyTheDatabase(sqlDatabase);
		
		try
		{
			JSONArray jsonArray = new JSONArray(result);
			
			//fill contentvalues to be put in sqlite database
			for(int i = 0; i < jsonArray.length(); i++)
			{
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				ContentValues values = new ContentValues();
				
				long id = jsonObject.getLong("id");
				String title = "";
				if(jsonObject.has("title") && !jsonObject.getString("title").isEmpty() )
				{
					title = jsonObject.getString("title");
				}
				else
				{
					title = "";
				}
				//Log.d(TAG, "TITLE:" + title);
				String content = "";
				if(jsonObject.has("content") && 
						!jsonObject.getString("content").isEmpty() && 
						( jsonObject.getString("content").length() >= ( title.length() + 1) )   )
				{
					content = jsonObject.getString("content").substring(title.length() + 1 );
					//substring because notes-api sends the "title" again in first line of "content". annoying but we have to accept it for now.
				}
				else
				{
					content = "";
				}
				//Log.d(TAG, "CONTENT:" + content);
				
				
				values.put(NotesTable.COLUMN_ID, id);
				values.put(NotesTable.COLUMN_TITLE, title );
				values.put(NotesTable.CLOUMN_CONTENT, content);
				
				sqlDatabase.insert(NotesTable.NOTES_TABLE_NAME, null, values);
				
			}
		}
		catch(JSONException jsonE)
		{
			//something went wrong with json
			Toast.makeText(this, "not getting correct JSON from server. server ok?", Toast.LENGTH_LONG).show();
			jsonE.printStackTrace();
			Log.e(TAG, "no correct JSON data returned from server. first 30 chars from server:" + result.substring(0, 29));
		}
		
		
		//update complete
		sqlDatabase.close();
		notesOpenHelper.close();
		
		showAndFillListView(); //refresh listview
		hideProgressBar();
		
	}
	
	public void writeNewNotesToServer(String urlToServer)
	{
		//upload all notes with COLUMN_STATUS = NEW_NOTE
		Cursor cursor = getCursor(NotesTable.NEW_NOTE);
		
		//int rows = cursor.getCount();
		//Log.d(TAG, "cursor rows new notes:" + rows);
		while(!cursor.isAfterLast() )
		{
			String content = cursor.getString(cursor.getColumnIndex(NotesTable.CLOUMN_CONTENT));
			String title = cursor.getString( cursor.getColumnIndex(NotesTable.COLUMN_TITLE) );
			String toPost = "{ content: \"" + title + "\n" + content + "\"}";
			//Log.d(TAG, "to post:" + toPost);
			
			new UploadNotesTask().execute(urlToServer, toPost);
			
			cursor.moveToNext();
		}
		cursor.close();
	}
	
	private void writeModifiedNotesToServer(String urlToServer)
	{
		//upload changes to existing notes marked COLUMN_STATUS = TO_UPDATE
		Cursor cursor = getCursor(NotesTable.TO_UPDATE);
		//int rows = cursor.getCount();
		//Log.d(TAG, "cursor rows modified notes:" + rows);
		
		while ( !cursor.isAfterLast() )
		{
			String content = cursor.getString(cursor.getColumnIndex(NotesTable.CLOUMN_CONTENT));
			String title = cursor.getString( cursor.getColumnIndex(NotesTable.COLUMN_TITLE) );
			long id = cursor.getLong( cursor.getColumnIndex(NotesTable.COLUMN_ID) );
			String urlToServerWithNoteId = urlToServer + "/" + id;
			String toPost = "{ content: \"" + title + "\n" + content + "\"}";
			
			new UpdateNotesTask().execute(urlToServerWithNoteId, toPost);
			cursor.moveToNext();
		}
		cursor.close();
	}
	
	private void deleteMarkedNotesFromServer(String urlToServer)
	{
		//delete all notes with COLUM_STATUS = TO_DELETE
		Cursor cursor = getCursor(NotesTable.TO_DELETE);
		//int rows = cursor.getCount();
		//Log.d(TAG, "cursor rows to delete:" + rows);
		
		while( !cursor.isAfterLast() )
		{
			long id = cursor.getLong( cursor.getColumnIndex(NotesTable.COLUMN_ID));
			String urlToServerWithNoteId = urlToServer + "/" + id;
			
			new DeleteNotesTask().execute(urlToServerWithNoteId);
			cursor.moveToNext();
		}
		cursor.close();
	}
	
	private Cursor getCursor(String status)
	{
		sqlDatabase = notesOpenHelper.getWritableDatabase();
		
		String selection = NotesTable.COLUMN_STATUS + " = ?";
		String[] selectionArgs = new String[1];
		
		if(status.equals(NotesTable.TO_DELETE))
		{
			selectionArgs[0] = NotesTable.TO_DELETE;
		}
		else if (status.equals(NotesTable.TO_UPDATE))
		{
			selectionArgs[0] = NotesTable.TO_UPDATE ;
		}
		else
		{
			selectionArgs[0] = NotesTable.NEW_NOTE;
		}
		
		Cursor cursor = sqlDatabase.query(NotesTable.NOTES_TABLE_NAME, 
				NotesTable.COLUMNNAMES,
				selection,
				selectionArgs, 
				null, 
				null, 
				null);
		
		cursor.moveToFirst();
		
		sqlDatabase.close();
		return cursor;
	}
	
	
	
	//----------------------------------
	//LoaderManagerMethods. required by interface "LoaderCallbacks<Cursor>"
	//----------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		MySimpleCursorLoader mySimpleCursorLoader;
		String[] projection = NotesTable.COLUMNNAMES;
		
		mySimpleCursorLoader = new MySimpleCursorLoader(this, sqlDatabase, projection);
		
		
		return mySimpleCursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		simpleCursorAdapter.swapCursor(data);
		if(sqlDatabase.isOpen())
		{
			sqlDatabase.close();
		}	
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		simpleCursorAdapter.swapCursor(null);
	}
	
	
	//----------------------------------
	//AsyncTask for doing the deleting
	//----------------------------------
	private class DeleteNotesTask extends AsyncTask<String, Void, Boolean> 
	{
		protected Boolean doInBackground(String... strings) 
		{
			URL url = null;
			HttpsURLConnection urlConnection = null;
			String urlString = strings[0];

			try
			{
				url = new URL(urlString);
				urlConnection = (HttpsURLConnection) url.openConnection();

				urlConnection.setRequestMethod("DELETE");
				urlConnection.setUseCaches(false);
				String auth = url.getUserInfo();
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				urlConnection.connect();
				
				int connectionCode = urlConnection.getResponseCode();
				
				if(connectionCode == 200)
				{
					//Log.d(TAG, "success @ delete Note");
					return true;
				}
				else if(connectionCode == 404)
				{
					Log.e(TAG, "failure @ delete note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
					return false;
				}
				else
				{
					Log.e(TAG, "failure @ delete new Note");
					return false;
				}
				
			}
			catch(MalformedURLException e)
			{
				e.printStackTrace();
				Log.e(TAG, "malformed url in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Log.e(TAG, "ioException in UpdateNotesTask:" + e.toString());
				return false;
			}
			finally
			{
				urlConnection.disconnect();
			}
			
		}
		/*
		protected void onPostExecute(boolean result)
		{
			Toast.makeText(getApplicationContext(), "delete finished with boolean:" + result, Toast.LENGTH_LONG).show();
		}
		*/
	}
		
	//----------------------------------
	//AsyncTask for doing the updating
	//----------------------------------
	private class UpdateNotesTask extends AsyncTask<String, Void, Boolean> 
	{
		protected Boolean doInBackground(String... strings) 
		{
			URL url = null;
			HttpsURLConnection urlConnection = null;
			OutputStream outputStream = null;
			String urlString = strings[0];
			String toPost = strings[1];

			try
			{
				JSONObject json = new JSONObject(toPost);
				url = new URL(urlString);
				urlConnection = (HttpsURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("PUT");
				urlConnection.setUseCaches(false);
				String auth = url.getUserInfo();
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				urlConnection.setFixedLengthStreamingMode(json.toString().getBytes().length);
				
				urlConnection.setRequestProperty("Content-Type", "application/json");

				urlConnection.connect();
				
				outputStream = new BufferedOutputStream(urlConnection.getOutputStream() );
				outputStream.write(json.toString().getBytes());
				outputStream.flush();
				outputStream.close();
				
				int connectionCode = urlConnection.getResponseCode();
				
				if(connectionCode == 200)
				{
					//Log.d(TAG, "success @ update new Note");
					return true;
				}
				else if(connectionCode == 404)
				{
					Log.e(TAG, "failure @ update note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
					return false;
				}
				else
				{
					Log.e(TAG, "failure @ update new Note");
					return false;
				}
				
			}
			catch(MalformedURLException e)
			{
				e.printStackTrace();
				Log.e(TAG, "malformed url in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Log.e(TAG, "ioException in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(JSONException jsonE)
			{
				jsonE.printStackTrace();
				Log.e(TAG, "jasonException in UpdateNotesTask:" + jsonE.toString());
				return false;
			}
			finally
			{
				urlConnection.disconnect();
			}
			
		}
		/*
		protected void onPostExecute(boolean result)
		{
			Toast.makeText(getApplicationContext(), "update finished with boolean:" + result, Toast.LENGTH_LONG).show();
		}
		*/
	}
	
	//----------------------------------
	//AsyncTask for doing the uploading
	//----------------------------------
	private class UploadNotesTask extends AsyncTask<String, Void, Boolean> 
	{
		protected Boolean doInBackground(String... strings) 
		{
			URL url = null;
			HttpsURLConnection urlConnection = null;
			OutputStream outputStream = null;
			String urlString = strings[0];
			String toPost = strings[1];

			try
			{
				JSONObject json = new JSONObject(toPost);
				url = new URL(urlString);
				urlConnection = (HttpsURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("POST");
				urlConnection.setUseCaches(false);
				String auth = url.getUserInfo();
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				urlConnection.setFixedLengthStreamingMode(json.toString().getBytes().length);
				
				urlConnection.setRequestProperty("Content-Type", "application/json");

				urlConnection.connect();
				
				outputStream = new BufferedOutputStream(urlConnection.getOutputStream() );
				outputStream.write(json.toString().getBytes());
				outputStream.flush();
				outputStream.close();
				
				int connectionCode = urlConnection.getResponseCode();
				
				if(connectionCode == 200)
				{
					//Log.d(TAG, "success @ upload new Note");
					return true;
				}
				else if(connectionCode == 404)
				{
					Log.e(TAG, "failure @ update note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
					return false;
				}
				else
				{
					Log.e(TAG, "failure @ upload new Note");
					return false;
				}
				
			}
			catch(MalformedURLException e)
			{
				e.printStackTrace();
				Log.e(TAG, "malformed url in UploadNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Log.e(TAG, "ioException in UploadNotesTask:" + e.toString());
				return false;
			}
			catch(JSONException jsonE)
			{
				jsonE.printStackTrace();
				Log.e(TAG, "jasonException in UplaodNotesTask:" + jsonE.toString());
				return false;
			}
			finally
			{
				urlConnection.disconnect();
			}
			
		}
		/*
		protected void onPostExecute(boolean result)
		{
			Toast.makeText(getApplicationContext(), "upload finished with boolean:" + result, Toast.LENGTH_LONG).show();
		}
		*/
	}
	
	//----------------------------------
	//AsyncTask for doing the downloading
	//----------------------------------
	/**
	 * inner class performing the actual downloading from the web
	 * 
	 */
	private class DownloadNotesTask extends AsyncTask<String, Void, String> 
	{
	    /** The system calls this to perform work in a worker thread and
	      * delivers it the parameters given to AsyncTask.execute() */
	    protected String doInBackground(String... anUrl) {
	    	
	    	StringBuilder stringBuilder = new StringBuilder();
	    	URL url = null;
	    	
			try {
				url = new URL(anUrl[0]);
				HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
				
				urlConnection.setDoInput(true);
				urlConnection.setRequestMethod("GET");
				String auth = url.getUserInfo(); 
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				
				String line;
				while( (line = reader.readLine() ) != null)
				{
					stringBuilder.append(line);
					//Log.d(TAG, "line:" + line);
				}
			} 
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
				Log.e(TAG, e.toString());
				
				return "ERROR MalformedURLException";
			}
	    	catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.toString() );
				return "ERROR IOException";
			}
	    	
	    	return stringBuilder.toString();
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(String result) {
	    	if(result.equals("ERROR MalformedURLException"))
	    	{
	    		Toast.makeText(getApplicationContext(), "the url you request data from is not correctly formed", Toast.LENGTH_LONG).show();
	    		hideProgressBar();
	    	}
	    	else if(result.equals("ERROR IOException"))
			{
	    		Toast.makeText(getApplicationContext(), "the url you request data from doesn't seem to exist. check spelling or your internet-connection", Toast.LENGTH_LONG).show();
	    		hideProgressBar();
			}
	    	else
	    	{
	    		//"result" contains a JSON with _all_ notes from owncloud server.
	    		updateDatabase(result);
	    	}
	    	
	    }
	}
}
