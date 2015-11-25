package org.aykit.MyOwnNotes.database.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import org.aykit.MyOwnNotes.database.NoteColumns;

/**
 * Created by mklepp on 22/11/15.
 */
public class Note implements Parcelable {
    long id;
    public String title;
    public String content;
    String status;

    public Note(){

    }

    public Note(Cursor cursor){
        id = cursor.getLong(cursor.getColumnIndex(NoteColumns._ID));
        title = cursor.getString(cursor.getColumnIndex(NoteColumns.TITLE));
        content = cursor.getString(cursor.getColumnIndex(NoteColumns.CONTENT));
        status = cursor.getString(cursor.getColumnIndex(NoteColumns.STATUS));
    }

    protected Note(Parcel in) {
        id = in.readLong();
        title = in.readString();
        content = in.readString();
        status = in.readString();
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(status);
    }
}
