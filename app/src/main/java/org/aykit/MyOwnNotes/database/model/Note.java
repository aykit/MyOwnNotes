package org.aykit.MyOwnNotes.database.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.owncloud.android.lib.resources.files.RemoteFile;

import org.aykit.MyOwnNotes.database.NoteColumns;

/**
 * Created by mklepp on 22/11/15.
 */
public class Note implements Parcelable {
    public long id;
    public String title;
    public String content;
    public int creationDate;
    String status;
    public String filename;

    public static final String NEW_TITLE = "new";

    public Note(){
        this.title = NEW_TITLE;
        this.status = NoteColumns.STATUS_NEW;
        this.creationDate = (int)(System.currentTimeMillis()/1000L);
    }

    public Note(Cursor cursor){
        id = cursor.getLong(cursor.getColumnIndex(NoteColumns._ID));
        title = cursor.getString(cursor.getColumnIndex(NoteColumns.TITLE));
        content = cursor.getString(cursor.getColumnIndex(NoteColumns.CONTENT));
        status = cursor.getString(cursor.getColumnIndex(NoteColumns.STATUS));
        filename = cursor.getString(cursor.getColumnIndex(NoteColumns.FILENAME));
        if (TextUtils.isEmpty(filename) || status.equals(NoteColumns.STATUS_NEW)){
            // Allow ascii-only filename and remove slashes
            filename = title.replaceAll("[^\\x00-\\x7F]", "").replaceAll("[/\\\\]", "")+".txt";
        }
    }

    protected Note(Parcel in) {
        id = in.readLong();
        title = in.readString();
        content = in.readString();
        status = in.readString();
        creationDate = in.readInt();
        filename = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(status);
        dest.writeInt(creationDate);
        dest.writeString(filename);
    }

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override
        public Note createFromParcel(Parcel in) {
            return new Note(in);
        }

        @Override
        public Note[] newArray(int size) {
            return new Note[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        if (id > 0) {
            cv.put(NoteColumns._ID, id);
        }
        cv.put(NoteColumns.STATUS, status);
        cv.put(NoteColumns.TITLE, title);
        if (!TextUtils.isEmpty(content)) {
            cv.put(NoteColumns.CONTENT, content);
        }
        cv.put(NoteColumns.CREATION_DATE, creationDate);
        cv.put(NoteColumns.FILENAME, filename);
        return cv;
    }

    public void setEdited(){
        if (NoteColumns.STATUS_DONE.equals(status)){
            status = NoteColumns.STATUS_UPDATE;
        }
    }

    public void setDeleted() {
        status = NoteColumns.STATUS_DELETE;
    }

    public void setUploaded() {
        status = NoteColumns.STATUS_DONE;
    }

    public boolean isEdited() {
        return status.equals(NoteColumns.STATUS_UPDATE);
    }

    public boolean isDone() {
        return status.equals(NoteColumns.STATUS_DONE);
    }
}
