package org.aykit.MyOwnNotes.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.aykit.MyOwnNotes.fragments.NoteDetailFragment;
import org.aykit.MyOwnNotes.fragments.NoteListFragment;
import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.database.NotesProvider;
import org.aykit.MyOwnNotes.database.model.Note;


/**
 * An activity representing a list of Notes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link NoteDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link NoteListFragment} and the item details
 * (if present) is a {@link NoteDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link NoteListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class NoteListActivity extends AppCompatActivity
        implements NoteListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_app_bar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        findViewById(R.id.button_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context appContext = getApplicationContext();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Note newNote = new Note();
                        Uri uri = appContext.getContentResolver().insert(NotesProvider.NOTES.CONTENT_URI, newNote.getContentValues());
                        Cursor result = appContext.getContentResolver().query(uri, null, null, null, null);
                        result.moveToFirst();
                        newNote = new Note(result);
                        onNoteSelected(newNote);
                    }
                }).start();
            }
        });

        if (findViewById(R.id.note_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    /**
     * Callback method from {@link NoteListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onNoteSelected(Note note) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(NoteDetailFragment.ARG_NOTE, note);
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            NoteDetailFragment fragment = new NoteDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.note_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item
            Intent detailIntent = new Intent(this, NoteDetailActivity.class);
            detailIntent.putExtras(arguments);
            startActivity(detailIntent);
        }
    }
}
