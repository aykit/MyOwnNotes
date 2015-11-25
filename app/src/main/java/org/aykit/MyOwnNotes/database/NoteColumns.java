package org.aykit.MyOwnNotes.database;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.Check;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Created by mklepp on 22/11/15.
 */
public interface NoteColumns {

    String STATUS_NEW = "new";
    String STATUS_DELETE = "delete";
    String STATUS_UPDATE = "update";
    String STATUS_DONE = "done";

    @DataType(INTEGER) @PrimaryKey @AutoIncrement String _ID = "_id";

    @DataType(TEXT) @NotNull String TITLE = "title";
    @DataType(TEXT) String CONTENT = "content";

    @DataType(TEXT)
    @Check(NoteColumns.STATUS + " in ('" + NoteColumns.STATUS_NEW + "', '"
            + NoteColumns.STATUS_UPDATE + "', '"
            + NoteColumns.STATUS_DELETE + "', '"
            + NoteColumns.STATUS_DONE + "')")
    String STATUS = "status";

    @DataType(INTEGER)
    String CREATION_DATE = "creation_date";
}
