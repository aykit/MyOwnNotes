package org.aykit.MyOwnNotes.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.aykit.MyOwnNotes.BuildConfig;
import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.asynctasks.SyncNotesAsyncTask;
import org.aykit.MyOwnNotes.database.NotesProvider;
import org.aykit.MyOwnNotes.database.model.Note;
import org.aykit.MyOwnNotes.fragments.NoteDetailFragment;
import org.aykit.MyOwnNotes.fragments.NoteListFragment;
import org.aykit.MyOwnNotes.helpers.LegacyImporter;
import org.aykit.MyOwnNotes.helpers.Settings;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


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

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.navigation)
    NavigationView navigationView;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.button_add)
    FloatingActionButton addButton;

    private BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SyncNotesAsyncTask.SYNC_FAILED:
                    int message = intent.getIntExtra(Intent.EXTRA_TEXT, R.string.toast_connection_error);
                    Toast.makeText(NoteListActivity.this, message, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_app_bar);
        ButterKnife.bind(this);

        checkForLegacyDatabase();

        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context appContext = getApplicationContext();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Note newNote = new Note();
                        Uri uri = appContext.getContentResolver().insert(NotesProvider.NOTES.CONTENT_URI, newNote.getContentValues());
                        if (uri != null) {
                            Cursor result = appContext.getContentResolver().query(uri, null, null, null, null);
                            if (result != null) {
                                result.moveToFirst();
                                newNote = new Note(result);
                                result.close();
                                onNoteSelected(newNote);
                            }
                        }
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

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_logout) {

                    // do logout stuff
                    new MaterialDialog.Builder(NoteListActivity.this)
                            .title(R.string.dialog_logout_title)
                            .positiveText(android.R.string.yes)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                                    Settings.clearApp(NoteListActivity.this);
                                    finish();
                                    startActivity(new Intent(NoteListActivity.this, LoginActivity.class));
                                }
                            })
                            .negativeText(android.R.string.no)
                            .show();
                }
                return false;
            }
        });

        MenuItem logoutItem = navigationView.getMenu().getItem(0);
        if (logoutItem != null){
            String username = PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.PREF_ACCOUNT_NAME, "");
            logoutItem.setTitle(username);
        }

        TextView appNameView = (TextView)navigationView.getHeaderView(0).findViewById(R.id.app_name);
        if (appNameView != null){
            appNameView.setText(appNameView.getText()+" "+ BuildConfig.VERSION_NAME);
        }

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.drawer_open, R.string.drawer_closed){

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncNotesAsyncTask.SYNC_FAILED);

        LocalBroadcastManager.getInstance(this).registerReceiver(syncBroadcastReceiver, filter);

//        start sync after registering receiver
        SyncNotesAsyncTask.start(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(syncBroadcastReceiver);

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

            overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
        }
    }

    @Override
    public void onNoteSwiped(final Note note) {
        new MaterialDialog.Builder(this)
                .title(R.string.dialog_delete_title)
                .content(note.title)
                .positiveText(android.R.string.yes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                note.setDeleted();
                                getContentResolver().update(NotesProvider.NOTES.withId(note.id), note.getContentValues(), null, null);
                                SyncNotesAsyncTask.start(NoteListActivity.this);
                            }
                        }).start();
                    }
                })
                .negativeText(android.R.string.no)
                .show();
    }

    private void checkForLegacyDatabase(){
        LegacyImporter importer = new LegacyImporter(this);
        if (importer.checkForMigration()){
            final List<Note> extractedNotes = importer.extractNotes();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (Note note : extractedNotes) {
                        getContentResolver().insert(NotesProvider.NOTES.CONTENT_URI, note.getContentValues());
                    }
                    SyncNotesAsyncTask.start(NoteListActivity.this);
                }
            }).start();
        }
    }
}
