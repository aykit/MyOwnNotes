package org.aykit.owncloud_notes;
import android.support.v4.app.NavUtils;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import org.aykit.MyOwnNotes.R;


public class SettingsActivity extends Activity {
	
	public static final String TAG = SettingsActivity.class.getSimpleName();
	public static final String PREF_USERNAME = "username";
	public static final String PREF_PASSWOORD = "password";
	public static final String PREF_ADDRESS = "address";
	public static final String PREF_AUTOSYNC = "sync";
	public static final String PREF_DEFAULT_TITLE = "defaultTitle";
	public static final String PREF_INITIALIZED = "initialized";
	public static final String PREF_MENU_INFLATED = "menuInflated";
	
	private final int minimumPasswordLength = 1;
	private final char[] forbiddenSymbols = { '"', '\'' };
	
	private EditText username;
	private EditText password;
	private EditText address;
	private CheckBox autoSync;
	private CheckBox defaultTitle;
	private SharedPreferences settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_settings);
		username = (EditText) findViewById(R.id.edittext_username);
		password = (EditText) findViewById(R.id.edittext_password);
		address = (EditText) findViewById(R.id.edittext_server_address);
		autoSync = (CheckBox) findViewById(R.id.checkbox_sync_automatically);
		defaultTitle = (CheckBox) findViewById(R.id.checkbox_defaultnotetitle);
		
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		username.setText( settings.getString(PREF_USERNAME, ""));
		password.setText(settings.getString(PREF_PASSWOORD, ""));
		address.setText(settings.getString(PREF_ADDRESS, "https://"));
		autoSync.setChecked(settings.getBoolean(PREF_AUTOSYNC, true));
		defaultTitle.setChecked( settings.getBoolean(PREF_DEFAULT_TITLE, true));
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch (item.getItemId()) 
	    {
	    // Respond to the action bar's Up/Home button
		    case android.R.id.home:
		    	
		    	updateSettings();
		        NavUtils.navigateUpFromSameTask(this);
		    	return true;
		}
	    return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		updateSettings();
	}
	
	/**
	 * updates the in <code>DefaultSharedPreferences</code> saved username, password, server-url and checkbox-states
	 * with the given information in <code>EditText</code>-fields.
	 * But only iff <code>EditText</code>s contain valid values.
	 * produces <code>Toasts</code> to let user know if something - and what - has gone wrong. 
	 * <p>calls the following methods to check validity of username, password and url:
	 * <li><code>{@link #isValidUsername(EditText)}</code></li>
	 * <li><code>{@link #isValidPassword(EditText, int)}</code></li>
	 * <li><code>{@link #isValidURL(EditText)}</code></li>
	 * 
	 * @return	<code>true</code> iff username, password, server-url are valid and the new credentials have been saved.
	 * @see	@see  {@link #isValidURL(EditText)}
	 */
	public boolean updateSettings()
	{
		//save changed settings
		Log.d(TAG, "saving settings-changes");
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		
		//check username
		if(isValidUsername(username) )
		{
			editor.putString(PREF_USERNAME, username.getText().toString());
		}
		else {
			Toast.makeText(this, R.string.toast_enter_valid_username, Toast.LENGTH_SHORT).show();
			return false;
		}
		
		//check password
		if(isValidPassword(password, minimumPasswordLength) )
		{
			editor.putString(PREF_PASSWOORD, password.getText().toString());
		}
		else {
			Toast.makeText(this, R.string.toast_enter_password, Toast.LENGTH_SHORT).show();
			return false;
		}
		
		//check URL
		if(isValidURL(address) )
		{
			editor.putString(PREF_ADDRESS, address.getText().toString());
		}
		else {
			Toast.makeText(this, R.string.toast_enter_valid_https, Toast.LENGTH_SHORT).show();
			return false;
		}
		
		//save sync and defaultTitle checkbox-states
		editor.putBoolean(PREF_AUTOSYNC, autoSync.isChecked());
		editor.putBoolean(PREF_DEFAULT_TITLE, defaultTitle.isChecked() );
		editor.putBoolean(PREF_INITIALIZED, true);
		
		editor.commit();
		
		Toast.makeText(this, R.string.settings_text_saved, Toast.LENGTH_SHORT).show();
		
		return true;
	}
	
	/**
	 * checks whether the text (<code>String</code>) in the passed <code>EditText</code> is a valid username - that is
	 * the field must not be empty and the following symbols are not allowed:
	 * <li>"</li>
	 * <li>'</li>
	 * 
	 * @param toCheck	<code>EditText</code> containing the <code>String</code>-field to be validated
	 * @return			<code>true</code> iff the field contains valid data (see above)
	 */
	public boolean isValidUsername(EditText toCheck)
	{
		String stringToCheck = toCheck.getText().toString();
		
		if(stringToCheck.isEmpty() || containsForbiddenSymbols(stringToCheck, forbiddenSymbols) )
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * checks whether a give <code>String</code> contains certain symbols or not
	 * @param toCheck
	 * @return
	 */
	public boolean containsForbiddenSymbols(String toCheck, char... chars)
	{
		if(	toCheck.indexOf(chars[0]) != -1 ||	// "
			toCheck.indexOf(chars[1]) != -1		// '
		  )
		{
			return true;
		}
		else
		{ 
			return false;
		}
	}
	
	/**
	 * checks whether the password in the <code>String</code> of the <code>EditText</code> is a valid password:
	 * <li>it must not be empty</li>
	 * <li>it must be a least <code>minimumPasswordLength</code> long</li>
	 * 
	 * @param toCheck	<code>EditText</code> containing the password <code>String</code>
	 * @param minimumPasswordLength 	<code>int</code> describing the minimal length of a valid password
	 * @return	true iff password is valid
	 */
	public boolean isValidPassword(EditText toCheck, int minimumPasswordLength)
	{
		String stringToCheck = toCheck.getText().toString();
		
		if(stringToCheck.isEmpty() || stringToCheck.length() < minimumPasswordLength )
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * checks whether the <code>String</code> passed in the <code>EditText</code> object is a valid https-url
	 * is valid if:
	 * <li>string is not empty</li>
	 * <li>string is a valid URL (according to <code>URLUtil.isValidUrl()</code></li>
	 * <li>string is a https-URL (according to <code>URLUtil.isHttpsUrl()</code>)</li>
	 * <li>string is at least 13 characters long (example for minimum url: https://ab.at)</li>
	 * <p>If String ends with a slash ("/"), it is removed.</p>
	 * 
	 * @param toCheck	<code>EditText</code> containing the String to be checked
	 * @return	<code>true</code> iff the <code>String</code> contains a valid https-url
	 */
	public boolean isValidURL(EditText toCheck)
	{
		String stringToCheck = toCheck.getText().toString();
		
		if(stringToCheck.isEmpty()					|| 
				!URLUtil.isValidUrl(stringToCheck)	|| 
				!URLUtil.isHttpsUrl(stringToCheck) 	|| 
				stringToCheck.length() < 13 )
		{
			return false;
		}
		else if(stringToCheck.charAt(stringToCheck.length() - 1 ) == '/') //input url ends with "/" (e.g.: https://example.org/  )
		{
			//remove slash at end
			String newAddressWithoutSlashAtEnd = stringToCheck.substring(0, stringToCheck.length() - 1 );
			toCheck.setText(newAddressWithoutSlashAtEnd);
			
			return isValidURL(toCheck);
		}
		else
		{
			return true;
		}
	}
}
