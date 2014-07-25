package org.aykit.owncloud_notes;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import org.aykit.MyOwnNotes.R;
import org.aykit.owncloud_notes.classes.MySimpleCursorLoader;
import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.DialogInterface;
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
	private Menu theMenu;
	private boolean connectionError;
	
	private final String apiPath = "/index.php/apps/notes/api/v0.2/notes";
	
	/**
	 * used to turn extensive logcat messages on or off. 
	 * debugOn == true -> show more log messages
	 * debugOn == false -> show only essential messages
	 * edited in SettingsActivity
	 */
	private boolean debugOn;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);
		
		updateSettings();
		debugOn = settings.getBoolean(SettingsActivity.PREF_EXTENSIVE_LOG, false); 
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(SettingsActivity.PREF_MENU_INFLATED, false); //this is done to save the fact that menus are not inflated yet.
		editor.putBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, false);
		editor.commit();
		
		loaderManager = getLoaderManager();
		notesOpenHelper = new NotesOpenHelper(this);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		updateSettings();
		if(settings.getBoolean(SettingsActivity.PREF_AUTOSYNC, true) &&				//autosync must be on
				settings.getBoolean(SettingsActivity.PREF_INITIALIZED, false) &&	//the settings (serveraddress, username, password) must be entered
				settings.getBoolean(SettingsActivity.PREF_MENU_INFLATED, false) ) 	//because we need to check whether the menu has been inflated or not
		{																		  	//synchronizeNotes() accesses the menu. if menu is not inflated and access is tried -> NullPointerException
			if ( ! settings.getBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, false) ) //only start sync if there is no sync already in progress. set by synchronizeNotes() and updateDatabase()
			{
				synchronizeNotes();
			}
		}
		
		//tell settings, that a single note was not created yet.
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("wasCreatedBefore", false);
		editor.putBoolean("wasPaused", false);
		editor.putString("content", "");
		editor.putLong("id", 0);
		editor.putString("status", "");
		editor.commit();
		
		makeSureSqlDatabaseIsOpen();
		
		showAndFillListView();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		updateSettings();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(SettingsActivity.PREF_MENU_INFLATED, false); // just to make sure, it is set to false next time activity is started
		editor.commit();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if(sqlDatabase != null)
		{
			if(sqlDatabase.isOpen() && ! sqlDatabase.inTransaction() )
			{
				sqlDatabase.close();
				
				if(notesOpenHelper != null)
				{
					notesOpenHelper.close();
				}
			}
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
	
	@SuppressWarnings("deprecation")
	private void showAndFillListView()
	{
		makeSureSqlDatabaseIsOpen();
		
		String[] from = { NotesTable.CLOUMN_CONTENT, NotesTable.COLUMN_STATUS };
		int[] to = {R.id.textview_note_row_content, R.id.textview_note_row_marked };
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
		this.theMenu = menu;
		
		//save the fact that the Menu has been inflated
		updateSettings();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(SettingsActivity.PREF_MENU_INFLATED, true);
		editor.commit();
		//then synchronize the notes at startup
		if(settings.getBoolean(SettingsActivity.PREF_AUTOSYNC, true) &&				//autosync must be on
				settings.getBoolean(SettingsActivity.PREF_INITIALIZED, false)  ) 	//because we need to check whether the menu has been inflated or not
		{
			if( ! settings.getBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, false) )
			{
				synchronizeNotes();
			}
		}
		
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
	        	if (debugOn)
	        	{
	        		Log.d(TAG, "menu: create new noten");
	        	}
	        	intent = new Intent(this, NoteSingleActivity.class);
	        	intent.putExtra("isNewNote", true);
	        	startActivity(intent);
	            return true;
	            
	        case R.id.action_sync:
	        	//start synchronizing
	        	if(debugOn) 
	        	{
	        		Log.d(TAG, "menu: start sync");
	        	}
	        	
	        	if( ! settings.getBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, false) )
	        	{
	        		synchronizeNotes();
	        	}
	        	else
	        	{
	        		Toast.makeText(this, R.string.toast_sync_in_progress, Toast.LENGTH_SHORT).show();
	        	}
	        	return true;
	        	
	        case R.id.action_settings:
	            //go to settings
	        	if(debugOn)
	        	{
	        		Log.d(TAG, "menu: open settings");
	        	}
	        	intent = new Intent(this, SettingsActivity.class);
	        	startActivity(intent);
	            return true;
	            
	        case R.id.action_help:
	        	//show help
	        	intent = new Intent(this, HelpActivity.class);
	        	startActivity(intent);
	        	return true;
	        	
	        case R.id.action_about:
	        	//show about
	        	intent = new Intent(this, AboutActivity.class);
	        	startActivity(intent);
	       
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
		Intent intent = new Intent(this, NoteSingleActivity.class);
		intent.putExtra("isNewNote", false); //tell intent that no new note must be created
		
		String content = 	((TextView) v.findViewById(R.id.textview_note_row_content)).getText().toString();
		String status = 	((TextView) v.findViewById(R.id.textview_note_row_marked)).getText().toString();
		
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
	 * checks if there is an active internet connection available.
	 * <br \>
	 * uses <code>ConnectivityManager</code> to check
	 * 
	 * @return	true, iff connected to the internet
	 */
	private boolean hasInternetConnection()
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(NoteListActivity.CONNECTIVITY_SERVICE);
		if (connectivityManager.getActiveNetworkInfo() != null )
		{
			if( connectivityManager.getActiveNetworkInfo().isAvailable() && 
				connectivityManager.getActiveNetworkInfo().isConnected()  )
			{
				return true;
			}
			else
			{
				return false;
			}
			
		} 
		else 
		{
			return false;
		}
	}
	
	/**
	 * initiates synchronization with ownCloud server.
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
		
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, true);
		editor.commit();
		
		showProgressBar();
		connectionError = false;
	
		//check internet connection
		
		String serverUrl = settings.getString(SettingsActivity.PREF_ADDRESS, "https://www.example.com");	//defaultvalue = "https://www.example.com"
		String urlToConnect = "";
		String basePath = "";
		
		try
		{
			//create basePath
			URL tempUrl = new URL(serverUrl);
			//must be like: https://user:password@yourowncloud.com/index.php/apps/notes/api/v0.2/notes
			
			if(tempUrl.getPort() == -1) //no port was given
			{
				basePath = tempUrl.getHost() + tempUrl.getPath() + apiPath;
				if(debugOn)
				{
					Log.d(TAG, "basePath no port: " + basePath);
				}
			}
			else //port was given
			{ 	
				basePath = tempUrl.getHost() + ":" + tempUrl.getPort() + tempUrl.getPath() + apiPath;
				if(debugOn)
				{
					Log.d(TAG, "basePath with port: " + basePath);
				}
			}
			
			urlToConnect = "https://" + basePath;  //this string will be passed to the async tasks
			
			if(debugOn)
			{
				Log.d(TAG, "urlToConnect:" + urlToConnect);
			}
		}
		catch(MalformedURLException e)
		{
			connectionError = true;
			e.printStackTrace();
			if(debugOn)
        	{
				Log.e(TAG, "tempUrl malforemd: serverUrl=" + serverUrl);
        	}
		}
		
		//only proceed if no problems occurred
		if(hasInternetConnection() )
		{
			if (!connectionError)
			{
				//upload new notes
				writeNewNotesToServer(urlToConnect);
				
				//update modified notes
				if(!connectionError)
				{
					writeModifiedNotesToServer(urlToConnect);
				}
				
				//delete marked notes
				if(!connectionError)
				{
					deleteMarkedNotesFromServer(urlToConnect);
				}
				
				//get all notes
				if(!connectionError)
				{
					Log.d(TAG, "getting notes from server");
					new DownloadNotesTask().execute(urlToConnect);
					//rest done in updateDatabase(), which is called when download is finished.
				}
			}
			else
			{
				Toast.makeText(this, R.string.toast_connection_error, Toast.LENGTH_LONG).show();
				hideProgressBar();
			}
		}
		else
		{
			Toast.makeText(this, R.string.toast_no_internet_connection, Toast.LENGTH_LONG).show();
			hideProgressBar();
		}
	}
	
	private void showProgressBar()
	{
		MenuItem item = theMenu.findItem(R.id.action_sync);
		item.setActionView(R.layout.progressbar);
	}
	
	private void hideProgressBar()
	{
		MenuItem item = theMenu.findItem(R.id.action_sync);
		item.setActionView(null);
	}
	
	private void showSSLAlert()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this ); 
		
		builder.setMessage(R.string.alert_ssl_cert_not_trusted);
		
		builder.setPositiveButton(R.string.alert_answer_yes, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User clicked Yes button
	        	   // open link to tutorial
	        	   Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://aykit.org/sites/myownnotes.html") );
	        	   
	        	   startActivity(intent);
	           }
	       }
		);
		builder.setNegativeButton(R.string.alert_answer_no_thanks, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User cancelled the dialog
	           }
	       }
		);
		
		AlertDialog dialog = builder.create();
		dialog.show();

	}
	
	public void updateDatabase(String result)
	{
		//update the database with "result" (= a json with _all_ notes)
		//Toast.makeText(this, result, Toast.LENGTH_LONG).show();
		makeSureSqlDatabaseIsOpen();
		
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
				String content = "";
				if(	jsonObject.has("content") && 
					! jsonObject.getString("content").isEmpty()  
					)
				{
					content = jsonObject.getString("content");
					//substring because notes-api sends the "title" again in first line of "content". annoying but we have to accept it for now.
				}
				else
				{
					content = "";
				}
				
				//Log.d(TAG, "CONTENT:" + content);
				
				values.put(NotesTable.COLUMN_ID, id);
				values.put(NotesTable.CLOUMN_CONTENT, content);
				
				sqlDatabase.insert(NotesTable.NOTES_TABLE_NAME, null, values);
			}
		}
		catch(JSONException jsonE)
		{
			//something went wrong with json
			Toast.makeText(this, R.string.toast_not_correct_json, Toast.LENGTH_LONG).show();
			jsonE.printStackTrace();
			Log.e(TAG, "no correct JSON data returned from server. result from server:" + result);
		}
		
		showAndFillListView(); //refresh listview
		hideProgressBar();
		
		updateSettings();
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(SettingsActivity.PREF_SYNC_IN_PROGRESS, false);
		editor.commit();
	}
	
	public void writeNewNotesToServer(String urlToServer)
	{
		Log.d(TAG, "writing new notes to server");
		//upload all notes with COLUMN_STATUS = NEW_NOTE
		Cursor cursor = getCursor(NotesTable.NEW_NOTE);
		
		if(debugOn)
    	{
			int rows = cursor.getCount();
			Log.d(TAG, "cursor rows new notes:" + rows);
    	}
		
		while(!cursor.isAfterLast() )
		{
			String content = cursor.getString(cursor.getColumnIndex(NotesTable.CLOUMN_CONTENT));
			String toPost = "{ content: \"" + content + "\"}";
			//Log.d(TAG, "to post:" + toPost);
			
			new UploadNotesTask().execute(urlToServer, toPost);
			
			cursor.moveToNext();
		}
		cursor.close();
	}
	
	private void writeModifiedNotesToServer(String urlToServer)
	{
		Log.d(TAG, "writing modified notes to server");
		//upload changes to existing notes marked COLUMN_STATUS = TO_UPDATE
		Cursor cursor = getCursor(NotesTable.TO_UPDATE);
		
		if(debugOn)
    	{
			int rows = cursor.getCount();
			Log.d(TAG, "cursor rows modified notes:" + rows);
    	}
		
		while ( !cursor.isAfterLast() )
		{
			String content = cursor.getString(cursor.getColumnIndex(NotesTable.CLOUMN_CONTENT));
			long id = cursor.getLong( cursor.getColumnIndex(NotesTable.COLUMN_ID) );
			String urlToServerWithNoteId = urlToServer + "/" + id;
			String toPost = "{ content: \"" + content + "\"}";
			
			new UpdateNotesTask().execute(urlToServerWithNoteId, toPost);
			cursor.moveToNext();
		}
		cursor.close();
	}
	
	private void deleteMarkedNotesFromServer(String urlToServer)
	{
		Log.d(TAG, "deleting notes from server");
		//delete all notes with COLUM_STATUS = TO_DELETE
		Cursor cursor = getCursor(NotesTable.TO_DELETE);
		
		if(debugOn)
    	{
			int rows = cursor.getCount();
			Log.d(TAG, "cursor rows to delete:" + rows);
    	}
		
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
		makeSureSqlDatabaseIsOpen();
		
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
		
		return cursor;
	}
	
	
	
	//----------------------------------
	//LoaderManagerMethods. required by interface "LoaderCallbacks<Cursor>"
	//----------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		MySimpleCursorLoader mySimpleCursorLoader;
		String[] projection = NotesTable.COLUMNNAMES;
		
		makeSureSqlDatabaseIsOpen();
		
		mySimpleCursorLoader = new MySimpleCursorLoader(this, sqlDatabase, projection);
		
		
		return mySimpleCursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		simpleCursorAdapter.swapCursor(data);	
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
				
				String auth = settings.getString(SettingsActivity.PREF_USERNAME, "username") + ":" + settings.getString(SettingsActivity.PREF_PASSWOORD, "password");
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				
				
				if (Build.VERSION.SDK_INT > 13) 
				{
					urlConnection.setRequestProperty("Connection", "close");
				}
				
				urlConnection.connect();
				
				int connectionCode = urlConnection.getResponseCode();
				
				if(connectionCode == 200)
				{
					if(debugOn)
					{
						Log.d(TAG, "success @ delete Note");
					}
					return true;
				}
				else if(connectionCode == 404)
				{
					connectionError = true;
					Log.e(TAG, "failure @ delete note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
					return false;
				}
				else if(connectionCode == 403)
				{
					connectionError = true;
					Log.e(TAG, "failure @ delete note. permission problem (error code 403)");
					return false;
				}
				else
				{
					connectionError = true;
					Log.e(TAG, "failure @ delete new Note. response code:" + connectionCode);
					return false;
				}
			}
			catch(MalformedURLException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "malformed url in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "ioException in UpdateNotesTask:" + e.toString());
				return false;
			}
			finally
			{
				if (urlConnection != null)
				{
					urlConnection.disconnect();
				}
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			if(result == false)
			{
				//there was a delete-error. no connection could be made.
				connectionError = true; //this variable is checked before the sql-database is updated.
				Log.e("DELETENOTES", "onPost: delete error");
			}
		}
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
			HttpsURLConnection urlTestConnection = null;
			OutputStream outputStream = null;
			String urlString = strings[0];
			String toPost = strings[1];

			try
			{
				JSONObject json = new JSONObject(toPost);
				url = new URL(urlString);
				
				urlTestConnection = (HttpsURLConnection) url.openConnection();
				
				urlConnection = (HttpsURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("PUT");
				urlConnection.setUseCaches(false);
				
				String auth = settings.getString(SettingsActivity.PREF_USERNAME, "username") + ":" + settings.getString(SettingsActivity.PREF_PASSWOORD, "password");
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				urlTestConnection.setRequestProperty("Authorization", basicAuth);
				
				urlConnection.setFixedLengthStreamingMode(json.toString().getBytes().length);
				urlConnection.setRequestProperty("Content-Type", "application/json");
				
				if (Build.VERSION.SDK_INT > 13) 
				{
						urlConnection.setRequestProperty("Connection", "close");
						urlTestConnection.setRequestProperty("Connection", "close");
				}

				urlTestConnection.connect();
				
				int testConnectionResponseCode = urlTestConnection.getResponseCode();
				
				if( testConnectionResponseCode == 200)
				{
					Log.d(TAG, "update connection ok, doing the updating");
					urlConnection.connect();
					
					outputStream = new BufferedOutputStream(urlConnection.getOutputStream() );
					outputStream.write(json.toString().getBytes());
					outputStream.flush();
					outputStream.close();
					
					int connectionCode = urlConnection.getResponseCode();
				
					if(connectionCode == 200)
					{
						if(debugOn)
						{
							Log.d(TAG, "success @ update new Note");
						}
						return true;
					}
					else if(connectionCode == 404)
					{
						connectionError = true;
						Log.e(TAG, "failure @ update note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
						return false;
					}
					else if(connectionCode == 403)
					{
						connectionError = true;
						Log.e(TAG, "failure @ update note. permission problem (error code 403)");
						return false;
					}				
					else
					{
						connectionError = true;
						Log.e(TAG, "failure @ update new Note. response code:" + connectionCode);
						return false;
					}
				
				}
				else
				{
					connectionError = true;
					Log.e(TAG, "No update connection could be established. Response code: " + testConnectionResponseCode );
					return false;
				}
			}
			catch(MalformedURLException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "malformed url in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "ioException in UpdateNotesTask:" + e.toString());
				return false;
			}
			catch(JSONException jsonE)
			{
				connectionError = true;
				jsonE.printStackTrace();
				Log.e(TAG, "jasonException in UpdateNotesTask:" + jsonE.toString());
				return false;
			}
			finally
			{
				if (urlConnection != null)
				{
					urlConnection.disconnect();
				}
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			if(result == false)
			{
				//there was an update-error. seems that no connection could be made.
				connectionError = true; //this variable is checked before the sql-database is updated.
				Log.e("UPDATETASK", "onPost: update error");
			}
		}
		
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
			HttpsURLConnection urlTestConnection = null;
			OutputStream outputStream = null;
			String urlString = strings[0];
			String toPost = strings[1];

			try
			{
				JSONObject json = new JSONObject(toPost);
				//Log.d(TAG, "json= " + json.toString() );
				url = new URL(urlString);
				
				urlTestConnection = (HttpsURLConnection) url.openConnection();
				
				urlConnection = (HttpsURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod("POST");
				urlConnection.setUseCaches(false);
				
				String auth = settings.getString(SettingsActivity.PREF_USERNAME, "username") + ":" + settings.getString(SettingsActivity.PREF_PASSWOORD, "password");
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				urlTestConnection.setRequestProperty("Authorization", basicAuth);
				
				if (Build.VERSION.SDK_INT > 13) 
				{
						urlConnection.setRequestProperty("Connection", "close");
						urlTestConnection.setRequestProperty("Connection", "close");
				}
				
				urlConnection.setFixedLengthStreamingMode(json.toString().getBytes().length);
				urlConnection.setRequestProperty("Content-Type", "application/json");

				urlTestConnection.connect();
				
				int testConnectionResponseCode = urlTestConnection.getResponseCode();
				
				if( testConnectionResponseCode == 200)
				{
					Log.d(TAG, "upload connection ok, doing the uploading");
					
					urlConnection.connect();
					
					outputStream = new BufferedOutputStream(urlConnection.getOutputStream() );
					outputStream.write(json.toString().getBytes());
					outputStream.flush();
					outputStream.close();
				
					int connectionCode = urlConnection.getResponseCode();
					
					if(connectionCode == 200)
					{
						if(debugOn)
						{
							Log.d(TAG, "success @ upload new Note");
						}
						return true;
					}
					else if(connectionCode == 404)
					{
						connectionError = true;
						Log.e(TAG, "failure @ upload note. note " + urlString.substring(urlString.lastIndexOf('/')) + " does not exist");
						return false;
					}
					else if(connectionCode == 403)
					{
						connectionError = true;
						Log.e(TAG, "failure @ upload note. permission problem (error code 403)");
						return false;
					}
					else
					{
						connectionError = true;
						Log.e(TAG, "failure @ upload new Note. response code:" + connectionCode);
						return false;
					}
				}
				else
				{
					Log.e(TAG, "No upload connection could be established. Response code: " + testConnectionResponseCode );
					return false;
				}
			}
			
			catch(MalformedURLException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "malformed url in UploadNotesTask:" + e.toString());
				return false;
			}
			catch(IOException e)
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, "ioException in UploadNotesTask:" + e.toString());
				return false;
			}
			catch(JSONException jsonE)
			{
				connectionError = true;
				jsonE.printStackTrace();
				Log.e(TAG, "jasonException in UplaodNotesTask:" + jsonE.toString());
				return false;
			}
			finally
			{
				if (urlConnection != null)
				{
					urlConnection.disconnect();
				}
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			if(result == false)
			{
				//there was an upload-error. seems that no connection could be made.
				connectionError = true; //this variable is checked before the sql-database is updated.
				Log.e("UPLOADTASK", "onPost: upload error");
			}
		}
		
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
	    	HttpsURLConnection urlConnection = null;
	    	URL url = null;
	    	
			try {
				url = new URL(anUrl[0]);

				if(debugOn)
				{
					Log.d("DOWNLOADTASK", "url:" + url.toString() );
				}
				
				urlConnection = (HttpsURLConnection) url.openConnection();
				urlConnection.setDoInput(true);
				urlConnection.setRequestMethod("GET");
				
				String auth = settings.getString(SettingsActivity.PREF_USERNAME, "username") + ":" + settings.getString(SettingsActivity.PREF_PASSWOORD, "password");
				
				//Log.d("DOWNLOADTASK", "auth=" + auth);
				
				String basicAuth = "Basic " + new String(Base64.encode(auth.getBytes(), Base64.DEFAULT));
				urlConnection.setRequestProperty("Authorization", basicAuth);
				
				if (Build.VERSION.SDK_INT > 13) 
				{
						urlConnection.setRequestProperty("Connection", "close");
				}
				
				urlConnection.connect();
				int connectionCode = urlConnection.getResponseCode();
				
				if(connectionCode == 200)
				{
					Log.d(TAG, "download connection ok, doing the downloading");
					BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					
					String line;
					while( (line = reader.readLine() ) != null)
					{
						stringBuilder.append(line);
						//Log.d(TAG, "line:" + line);
					}
				}
				else
				{
					connectionError = true;
					Log.e(TAG, "error @ downloading notes. response code: " + connectionCode);
					return "ERROR connection";
				}
			} 
			catch (MalformedURLException e) 
			{
				connectionError = true;
				e.printStackTrace();
				Log.e(TAG, e.toString());
				
				return "ERROR MalformedURLException";
			}
			catch(FileNotFoundException e)
			{
				connectionError = true;
				e.printStackTrace();				
				Log.e(TAG, e.toString() );
				
				return "ERROR FileNotFoundException";
			}
			catch(SSLHandshakeException e)
			{
				connectionError = true;
				if(debugOn)
				{
					e.printStackTrace();
					Log.e(TAG, e.toString());
				}
				
				return "ERROR SSLHandshakeException";
			}
	    	catch (IOException e) 
	    	{
	    		connectionError = true;
	    		e.printStackTrace();
	    		Log.e(TAG, e.toString() );
	    		
	    		return "ERROR IOException";
			}
			finally
			{
				if (urlConnection != null)
				{
					urlConnection.disconnect();
				}
			}
			
	    	return stringBuilder.toString();
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(String result) {
	    	if(result.equals("ERROR MalformedURLException") )
	    	{
	    		Toast.makeText(getApplicationContext(), R.string.toast_url_not_correctly_formed, Toast.LENGTH_LONG).show();
	    		hideProgressBar();
	    	}
	    	else if(result.equals("ERROR IOException") )
			{
	    		Toast.makeText(getApplicationContext(), R.string.toast_url_doesnt_exist, Toast.LENGTH_LONG).show();
	    		hideProgressBar();
			}
	    	else if(result.equals("ERROR FileNotFoundException" ) )
	    	{
	    		Toast.makeText(getApplicationContext(), R.string.toast_connection_error + R.string.toast_check_username_password, Toast.LENGTH_LONG).show();
	    		hideProgressBar();
	    	}
	    	else if(result.equals("ERROR SSLHandshakeException") )
	    	{
	    		hideProgressBar();
	    		showSSLAlert();
	    	}
	    	else if(result.equals("ERROR connection"))
	    	{
	    		Toast.makeText(getApplicationContext(), R.string.toast_connection_error, Toast.LENGTH_LONG).show();
	    		hideProgressBar();
	    	}
	    	else
	    	{
	    		//"result" contains a JSON with _all_ notes from owncloud server.
	    		if( ! connectionError) //only update database, if upload/update/delete cycle was successful
	    		{
	    			updateDatabase(result);
	    			if(debugOn)
	    			{
	    				Log.d(TAG, "updateDatabase() executed" );
	    			}
	    		}
	    		else
	    		{
	    			Log.e(TAG, "list not updated due to connection error");
	    			Toast.makeText(getApplicationContext(), R.string.toast_connection_error, Toast.LENGTH_LONG).show();
	    			hideProgressBar();
	    		}
	    	}
	    	
	    }
	}
}
