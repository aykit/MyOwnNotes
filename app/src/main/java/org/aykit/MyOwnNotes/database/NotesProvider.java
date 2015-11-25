package org.aykit.MyOwnNotes.database;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by mklepp on 22/11/15.
 */
@ContentProvider(authority = NotesProvider.AUTHORITY, database = NotesDatabase.class)
public final class NotesProvider {

    public static final String AUTHORITY = "org.aykit.MyOwnNotes.NotesProvider";

    @TableEndpoint(table = NotesDatabase.NOTES)
    public static class NOTES {

        @ContentUri(
                path = NotesDatabase.NOTES,
                type = "vnd.android.cursor.dir/list",
                defaultSort = NoteColumns.TITLE + " ASC")
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
    }
}