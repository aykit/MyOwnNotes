package org.aykit.MyOwnNotes.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.aykit.MyOwnNotes.database.model.Note;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mklepp on 21/02/16.
 */
public class LegacyImporter {

    static final String LEGACY_DB_NAME = "notes.db";

    private Context mContext;

    public LegacyImporter(Context context) {
        this.mContext = context;
    }

    private File getDatabasePath(){
        return mContext.getDatabasePath(LEGACY_DB_NAME);
    }

    public boolean checkForMigration(){
        File oldDb = getDatabasePath();

        if (oldDb.exists()){
            return true;
        }

        return false;
    }

    public List<Note> extractNotes(){

        List<Note> extractedNotes = new ArrayList<>();

        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(), null);

        Cursor cursor = db.rawQuery("SELECT * FROM notes WHERE noteStatus IS NOT NULL", null);

        if (cursor!=null)
        {
            while(cursor.moveToNext()){
                Note note = new Note();
                note.title = "Unsynchronized Note from previous version";
                note.content = cursor.getString(cursor.getColumnIndex("content"));

                extractedNotes.add(note);
            }

            // close and delete old database
            db.close();
            getDatabasePath().delete();
        } else {
            db.close();
        }

        return extractedNotes;
    }
}
