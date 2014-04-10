package org.aykit.owncloud_notes;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class StartActivity extends Activity {
	
	public static final String TAG = StartActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_start);
		
		Intent intent;
		
		int choice = 9;
		String choiceName;
		
		switch(choice)
		{
			case 0:
				intent = new Intent(this, NoteSingleActivity.class);
				choiceName = "Note Single";
				break;
			
			case 1:
				intent = new Intent(this, SettingsActivity.class);
				choiceName = "Settings";
				break;
				
			default:
				intent = new Intent(this, NoteListActivity.class);
				choiceName = "Note List";
			
		}
		
		Log.d(TAG, choiceName + " has been chosen");
		startActivity(intent);
	}
}
