package org.aykit.MyOwnNotes.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.adapter.DividerItemDecoration;
import org.aykit.MyOwnNotes.adapter.NotesListAdapter;
import org.aykit.MyOwnNotes.asynctasks.SyncNotesAsyncTask;
import org.aykit.MyOwnNotes.database.NoteColumns;
import org.aykit.MyOwnNotes.database.NotesProvider;
import org.aykit.MyOwnNotes.database.model.Note;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A list fragment representing a list of Notes. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link NoteDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private NotesListAdapter adapter;

    private static final int LOADER_NOTES = 20;

    @Bind(android.R.id.list)
    RecyclerView recyclerView;

    @Bind(android.R.id.empty)
    TextView emptyView;

    @Bind(R.id.swipeContainer)
    SwipeRefreshLayout swipeRefreshLayout;

    private BroadcastReceiver syncBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case SyncNotesAsyncTask.SYNC_FINISHED:
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case SyncNotesAsyncTask.SYNC_PROGRESS:
                    break;
            }
        }
    };

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onRefresh() {
        SyncNotesAsyncTask.start(getActivity());
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onNoteSelected(Note note);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public void onNoteSelected(Note note) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        if (adapter != null) {
            recyclerView.setAdapter(adapter);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.accent, R.color.primary);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.START | ItemTouchHelper.END) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final Context appContext = getActivity().getApplicationContext();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Note note = adapter.getItem(viewHolder.getAdapterPosition());
                        note.setDeleted();
                        appContext.getContentResolver().update(NotesProvider.NOTES.withId(note.id), note.getContentValues(), null, null);
                        SyncNotesAsyncTask.start(getActivity());

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                            }
                        });
                    }
                }).start();
            }
        });
        helper.attachToRecyclerView(recyclerView);

        getLoaderManager().initLoader(LOADER_NOTES, null, this);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        if (mActivatedPosition != ListView.INVALID_POSITION) {
//            // Serialize and persist the activated item position.
//            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
//        }
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String select = NoteColumns.STATUS + "<>?";
        String[] selectArgs = new String[]{NoteColumns.STATUS_DELETE};
        String sortOrder = NoteColumns.CREATION_DATE+" ASC";
        return new CursorLoader(getActivity(), NotesProvider.NOTES.CONTENT_URI, null, select, selectArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (adapter == null) {
            adapter = new NotesListAdapter(data);
            adapter.setOnItemClickListener(this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.changeCursor(data);
        }

        if (data != null && data.getCount() > 0) {
            emptyView.animate().alpha(0);
        } else {
            emptyView.animate().alpha(1);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (adapter != null) {
            adapter.changeCursor(null);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Note note = adapter.getItem(position);
        mCallbacks.onNoteSelected(note);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncNotesAsyncTask.SYNC_PROGRESS);
        filter.addAction(SyncNotesAsyncTask.SYNC_FINISHED);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(syncBroadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(syncBroadcastReceiver);

    }
}
