package org.aykit.MyOwnNotes.database;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by mklepp on 22/11/15.
 */
@Database(version = NotesDatabase.VERSION)
public class NotesDatabase {
    public static final int VERSION = 1;

    @Table(NoteColumns.class) public static final String NOTES = "notes";
}
