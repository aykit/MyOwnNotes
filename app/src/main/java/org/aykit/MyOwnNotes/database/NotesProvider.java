package org.aykit.MyOwnNotes.database;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

import org.aykit.MyOwnNotes.BuildConfig;

/**
 * Created by mklepp on 22/11/15.
 */
@ContentProvider(authority = NotesProvider.AUTHORITY, database = NotesDatabase.class)
public final class NotesProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID+".NotesProvider";
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);


    private static Uri buildUri(String... paths) {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths) {
            builder.appendPath(path);
        }
        return builder.build();
    }

    @TableEndpoint(table = NotesDatabase.NOTES)
    public static class NOTES {

        @ContentUri(
                path = NotesDatabase.NOTES,
                type = "vnd.android.cursor.dir/list",
                defaultSort = NoteColumns.TITLE + " ASC")
        public static final Uri CONTENT_URI = buildUri(NotesDatabase.NOTES);

        @InexactContentUri(
                name = "NOTE_ID",
                path = NotesDatabase.NOTES + "/#",
                type = "vnd.android.cursor.item/note",
                whereColumn = NoteColumns._ID,
                pathSegment = 1)
        public static Uri withId(long id) {
            return buildUri(NotesDatabase.NOTES, String.valueOf(id));
        }
    }

}