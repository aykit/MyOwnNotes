package org.aykit.MyOwnNotes.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;

import org.aykit.MyOwnNotes.database.generated.NotesDatabase;

/**
 * Created by mklepp on 26/11/15.
 */
public class Settings {
    public static final String PREF_ACCOUNT_NAME = "PREF_ACCOUNT_NAME";
    public static final String PREF_ACCOUNT_PASSWORD = "PREF_ACCOUNT_PASSWORD";

    public static final String NOTE_PATH_DEFAULT = "Notes";

    public static Uri getAccountURL(String accountname){
        String[] credentials = accountname.split("@");
        if (credentials.length != 2) {
            return null;
        }
        return Uri.parse("https://"+credentials[1]);
    }

    public static String getAccountUsername(String accountName){
        String[] credentials = accountName.split("@");
        if (credentials.length != 2) {
            return null;
        }
        return credentials[0];
    }

    public static boolean checkRemoteAccess(OwnCloudClient client) {
        ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
        RemoteOperationResult result = refreshOperation.execute(client);

        return result.isSuccess();
    }

    //    if notes folder does'nt exist, create him
    public static boolean checkRemoteFolder(OwnCloudClient client) {

        ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation(Settings.NOTE_PATH_DEFAULT);
        RemoteOperationResult result = refreshOperation.execute(client);

        if (!result.isSuccess()) {
            CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(Settings.NOTE_PATH_DEFAULT, true);
            RemoteOperationResult createResult = createOperation.execute(client);
            if (createResult.isSuccess()) {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }

    public static void clearApp(Context context) {
        // delete stored preferences - username/password
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();

        // delete database
        clearDatabase(context);
    }

    public static void clearDatabase(Context context){
        NotesDatabase db = NotesDatabase.getInstance(context);
        // close before deleting
        db.close();
        // remove database file
        context.deleteDatabase(db.getDatabaseName());
    }
}
