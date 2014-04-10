package org.aykit.owncloud_notes;

import org.aykit.owncloud_notes.classes.MySimpleCursorLoader;
import org.aykit.owncloud_notes.sql.NotesOpenHelper;
import org.aykit.owncloud_notes.sql.NotesTable;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		loaderManager = getLoaderManager();
		
		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		showAndFillListView();
	}
	
	@SuppressWarnings("deprecation")
	private void showAndFillListView()
	{
		notesOpenHelper = new NotesOpenHelper(this);
		sqlDatabase = notesOpenHelper.getWritableDatabase();
		
		String[] from = { NotesTable.COLUMN_TITLE, NotesTable.CLOUMN_CONTENT };
		int[] to = {R.id.textview_note_row_title, R.id.textview_note_row_content};
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
	        	Log.d(TAG, "menu: new note chosen");
	        	intent = new Intent(this, NoteSingleActivity.class);
	        	intent.putExtra("isNewNote", true);
	        	startActivity(intent);
	            return true;
	            
	        case R.id.action_sync:
	        	//start synchronizing
	        	Log.d(TAG, "menu: sync chosen");
	        	synchronizeNotes();
	        	return true;
	        	
	        case R.id.action_settings:
	            //go to settings
	        	Log.d(TAG, "menu: settings chosen");
	        	intent = new Intent(this, SettingsActivity.class);
	        	startActivity(intent);
	            return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public void synchronizeNotes()
	{
		//push new notes
		//get all notes
		Log.d(TAG, "starting note synchonization");
		Toast toast = Toast.makeText(this, "synchronization started...", Toast.LENGTH_SHORT);
		toast.show();
	}

	
	@Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
		Intent intent = new Intent(this, NoteSingleActivity.class);
		intent.putExtra("isNewNote", false); //tell intent that no new note must be created
		
		String title;
		String content;
		
		title = ((TextView) v.findViewById(R.id.textview_note_row_title)).getText().toString();
		content = ((TextView)v.findViewById(R.id.textview_note_row_content)).getText().toString();
		
		intent.putExtra("title", title);
		intent.putExtra("content", content);
		intent.putExtra("id", id);
		 
		startActivity(intent);
	}

	
	
	//
	//LoaderManagerMethods
	//
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
		sqlDatabase.close();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		simpleCursorAdapter.swapCursor(null);
	}
}
