package org.aykit.MyOwnNotes.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import org.apache.commons.io.FilenameUtils;
import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.activities.LoginActivity;
import org.aykit.MyOwnNotes.database.NoteColumns;
import org.aykit.MyOwnNotes.database.NotesProvider;
import org.aykit.MyOwnNotes.database.model.Note;
import org.aykit.MyOwnNotes.helpers.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mklepp on 12/12/15.
 */
public class SyncNotesAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    public static final String SYNC_PROGRESS = "SYNC_PROGRESS";
    public static final String SYNC_FINISHED = "SYNC_FINISHED";
    public static final String SYNC_FAILED = "SYNC_FAILED";

    private Context mContext;
    private OwnCloudClient mClient;


    public static void start(Context context) {
        new SyncNotesAsyncTask(context).execute();
    }

    private SyncNotesAsyncTask(Context context) {
        this.mContext = context;
        mClient = initClient();
    }

    private OwnCloudClient initClient() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        String accountName = prefs.getString(Settings.PREF_ACCOUNT_NAME, null);
        if (accountName == null) {
//            start LoginActivity
            showLoginActivity();
            return null;
        }

        Uri baseUrl = Settings.getAccountURL(accountName);
        String username = Settings.getAccountUsername(accountName);
        String password = prefs.getString(Settings.PREF_ACCOUNT_PASSWORD, null);

        OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(baseUrl, mContext, false);
        client.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(username, password));
        return client;
    }

    private void showLoginActivity() {
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        Intent intent = new Intent(SYNC_PROGRESS);
        intent.putExtra(SYNC_PROGRESS, values[0]);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    /*
        see https://github.com/aykit/MyOwnNotes/blob/85446b180d6ee7cc7d0bfbb7738763c880421d17/src/org/aykit/owncloud_notes/NoteListActivity.java#L369
        - push new notes
        - push notes changes
        - push delete notes
        - get all notes
     */
    @Override
    protected Boolean doInBackground(Void... params) {

        // no credentials found
        if (mClient == null) {
            return false;
        }

        if (!pushNewNotes()) {
            return false;
        }

        if (!pushEditedNotes()) {
            return false;
        }

        if (!deleteNotes()) {
            return false;
        }

        if (!fetchNotes()) {
            return false;
        }

        publishProgress(100);

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(SYNC_FINISHED));
        return true;
    }

    private void publishCreateProgress(int current, int count) {
        publishPartialProgress(0, current, count);
    }

    private void publishUpdateProgress(int current, int count) {
        publishPartialProgress(1, current, count);
    }

    private void publishDeletionProgress(int current, int count) {
        publishPartialProgress(2, current, count);
    }

    private void publishDownloadProgress(int current, int count) {
        publishPartialProgress(3, current, count);
    }


    private void publishPartialProgress(int step, int current, int count) {
        int progress = (int) (25.f / count * current + 25.f * step);
        publishProgress(progress);
    }

    private Cursor getNotesCursorWithStatus(String[] status) {
        String select = NoteColumns.STATUS + "=?";
        String[] selectArgs = status;
        String sortOrder = NoteColumns.CREATION_DATE + " ASC";
        return mContext.getContentResolver().query(NotesProvider.NOTES.CONTENT_URI, null, select, selectArgs, sortOrder);
    }

    private boolean pushNewNotes() {

        Cursor cursor = getNotesCursorWithStatus(new String[]{NoteColumns.STATUS_NEW});

        try {
            int count = cursor.getCount();
            int current = 0;

            while (cursor.moveToNext()) {
                Note note = new Note(cursor);

                createNote(note);

                current += 1;
                publishCreateProgress(current, count);

            }

            return true;

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void createNote(Note note) {
        File fileToUpload = null;

        try {
            fileToUpload = createTempFileFromNote(note);
            String remotePath = getRemoteFilePath(note);

            ExistenceCheckRemoteOperation checkRemoteOperation = new ExistenceCheckRemoteOperation(remotePath, true);
            RemoteOperationResult checkRemoteResult = checkRemoteOperation.execute(mClient);

            // file does exist (see true param for above ExistenceCheckRemoteOperation)
            if (checkRemoteResult.isSuccess()) {

                UploadRemoteFileOperation uploadRemoteFileOperation = new UploadRemoteFileOperation(fileToUpload.getAbsolutePath(), remotePath, "text/plain");

                RemoteOperationResult result = uploadRemoteFileOperation.execute(mClient);

                if (result.isSuccess()) {
                    note.setUploaded();
                } else if (result.getCode().equals(RemoteOperationResult.ResultCode.CONFLICT)) {
// don't destroy anything, create a new entry and let the user resolve the conflict
                    note.filename = generateNewFileName(note.filename);
                    createNote(note);
                } else {
                    handleError(result);
                }
            } else if (checkRemoteResult.getCode().equals(RemoteOperationResult.ResultCode.FILE_NOT_FOUND)) {
                // if file already exists
                note.filename = generateNewFileName(note.filename);
                createNote(note);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileToUpload != null) {
                fileToUpload.delete();
            }

            mContext.getContentResolver().update(NotesProvider.NOTES.withId(note.id), note.getContentValues(), null, null);
        }
    }

    private String generateNewFileName(String name) {
        String nameWithoutExtension = FilenameUtils.removeExtension(name);
        String extension = FilenameUtils.getExtension(name);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return nameWithoutExtension + "_" + sdf.format(new Date()) + "." + extension;
    }

    private boolean pushEditedNotes() {
        Cursor cursor = getNotesCursorWithStatus(new String[]{NoteColumns.STATUS_UPDATE});

        try {
            int count = cursor.getCount();
            int current = 0;

            while (cursor.moveToNext()) {
                Note note = new Note(cursor);

                updateNote(note);

                current += 1;
                publishUpdateProgress(current, count);

            }

            return true;

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void updateNote(Note note) {
        File fileToUpload = null;

        try {
            fileToUpload = createTempFileFromNote(note);
            String remotePath = getRemoteFilePath(note);

            ExistenceCheckRemoteOperation checkRemoteOperation = new ExistenceCheckRemoteOperation(remotePath, true);
            RemoteOperationResult checkRemoteResult = checkRemoteOperation.execute(mClient);

            // file does note exist (see true param for above ExistenceCheckRemoteOperation)
            if (!checkRemoteResult.isSuccess()) {

                UploadRemoteFileOperation uploadRemoteFileOperation = new UploadRemoteFileOperation(fileToUpload.getAbsolutePath(), remotePath, "text/plain");

                RemoteOperationResult result = uploadRemoteFileOperation.execute(mClient);

                if (result.isSuccess()) {
                    note.setUploaded();
                } else if (result.getCode().equals(RemoteOperationResult.ResultCode.CONFLICT)) {
// don't destroy anything, create a new entry and let the user resolve the conflict
                    note.filename = generateNewFileName(note.filename);
                    createNote(note);
                } else {
                    handleError(result);
                }
            } else {
// if it doesn't exist on the server create on the server - let the user resolve this conflicts
                createNote(note);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileToUpload != null) {
                fileToUpload.delete();
            }

            mContext.getContentResolver().update(NotesProvider.NOTES.withId(note.id), note.getContentValues(), null, null);
        }
    }

    private boolean deleteNotes() {

        Cursor cursor = getNotesCursorWithStatus(new String[]{NoteColumns.STATUS_DELETE});

        try {
            int count = cursor.getCount();
            int current = 0;

            while (cursor.moveToNext()) {
                Note note = new Note(cursor);

                deleteNote(note);

                current += 1;
                publishDeletionProgress(current, count);

            }

            return true;

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void deleteNote(Note note) {
        String remotePath = getRemoteFilePath(note);

        RemoveRemoteFileOperation removeRemoteFileOperation = new RemoveRemoteFileOperation(remotePath);
        RemoteOperationResult removeResult = removeRemoteFileOperation.execute(mClient);

// delete locally if file has been deleted or already was deleted
        if (removeResult.isSuccess()) {
            mContext.getContentResolver().delete(NotesProvider.NOTES.withId(note.id), null, null);
        }
    }

    private boolean fetchNotes() {

        ReadRemoteFolderOperation remoteFolderOperation = new ReadRemoteFolderOperation(getRemotePath());

        RemoteOperationResult result = remoteFolderOperation.execute(mClient);


        if (result.isSuccess()) {
            ArrayList<Object> files = result.getData();


            int count = files.size()-1; // Lists current folder
            int current = 0;

            for (Object fileObject : files) {
                RemoteFile remoteFile = (RemoteFile) fileObject;

                String remotePath = remoteFile.getRemotePath();
//      we currently handle text/plain files only
                if (remoteFile.getMimeType().equals("text/plain")) {

                    Note note = null;

                    Uri path = Uri.parse(remoteFile.getRemotePath());
                    String filename = path.getLastPathSegment();

                    String select = NoteColumns.FILENAME + "=?";
                    String[] selectArgs = new String[]{filename};
                    Cursor cursor = mContext.getContentResolver().query(NotesProvider.NOTES.CONTENT_URI, null, select, selectArgs, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        note = new Note(cursor);
                        cursor.close();
//                        Don't pull notes that are not synced yet
                        if (!note.isDone()){
                            continue;
                        }
                    } else {
                        note = new Note();
                        note.filename = filename;
                        note.setUploaded();
                        note.creationDate = (int) (remoteFile.getCreationTimestamp() / 1000);
                    }

                    String tempPath = mContext.getCacheDir().getAbsolutePath();

                    DownloadRemoteFileOperation downloadRemoteFileOperation = new DownloadRemoteFileOperation(remotePath, tempPath);

                    RemoteOperationResult downloadResult = downloadRemoteFileOperation.execute(mClient);
                    String localPath = mContext.getCacheDir().getAbsolutePath() + FileUtils.PATH_SEPARATOR + remotePath;
                    File localFile = new File(localPath);

                    if (downloadResult.isSuccess() && localFile.exists()) {
                        copyFileToNote(localFile, note);
                        localFile.delete();
                    } else {
                        return false;
                    }

                    if (note.id != 0) {
                        mContext.getContentResolver().update(NotesProvider.NOTES.withId(note.id), note.getContentValues(), null, null);
                    } else {
                        mContext.getContentResolver().insert(NotesProvider.NOTES.CONTENT_URI, note.getContentValues());
                    }
                }

                current += 1;
                publishDownloadProgress(current, count);
            }
            return true;
        }

        return false;
    }

    private String getRemotePath() {
        return Settings.NOTE_PATH_DEFAULT + "/";
    }

    private String getRemoteFilePath(Note note) {
        return getRemotePath() + note.filename;
    }

    private void handleError(RemoteOperationResult result) {
        Log.e(SyncNotesAsyncTask.class.getSimpleName(), result.getLogMessage());
        Intent intent = new Intent(SYNC_FAILED);

        switch (result.getCode()) {
            case ACCOUNT_EXCEPTION:
            case ACCOUNT_NOT_FOUND:
            case INCORRECT_ADDRESS:
                intent.putExtra(Intent.EXTRA_TEXT, R.string.toast_check_username_password);
                break;
        }

        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private File createTempFileFromNote(Note note) throws IOException {
        File file = File.createTempFile("note", ".txt", mContext.getCacheDir());

        FileWriter fileWriter = new FileWriter(file);
        fileWriter.append(note.title + "\n");
        fileWriter.append(note.content == null ? "" : note.content);
        fileWriter.close();

        return file;
    }

    private void copyFileToNote(File file, Note note) {
        try {
            InputStream instream = new FileInputStream(file);
            // prepare the file for reading
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);

            note.title = buffreader.readLine();

            note.content = "";

            for (String line = buffreader.readLine(); line != null; line = buffreader.readLine()) {
                note.content += line + "\n";
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
