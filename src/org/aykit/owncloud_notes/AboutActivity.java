package org.aykit.owncloud_notes;

import org.aykit.MyOwnNotes.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

public class AboutActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_about);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.about, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch (item.getItemId()) 
	    {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:
		    	
		        NavUtils.navigateUpFromSameTask(this);
		    	return true;
		}
	    return super.onOptionsItemSelected(item);
	}
	
}//END:class
