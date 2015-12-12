package org.aykit.MyOwnNotes.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.activities.NoteDetailActivity;
import org.aykit.MyOwnNotes.activities.NoteListActivity;
import org.aykit.MyOwnNotes.database.NotesProvider;
import org.aykit.MyOwnNotes.database.model.Note;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Note detail screen.
 * This fragment is either contained in a {@link NoteListActivity}
 * in two-pane mode (on tablets) or a {@link NoteDetailActivity}
 * on handsets.
 */
public class NoteDetailFragment extends Fragment implements TextWatcher {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_NOTE = "note";

    private Note mNote;

    @Bind(R.id.title)
    EditText titleView;

    @Bind(R.id.content)
    EditText contentView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NoteDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args.containsKey(ARG_NOTE)) {

            mNote = args.getParcelable(ARG_NOTE);

            Activity activity = this.getActivity();
            Toolbar appBarLayout = (Toolbar) activity.findViewById(R.id.toolbar);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mNote.title);
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        titleView.setText(mNote.title);
        titleView.addTextChangedListener(this);
        contentView.setText(mNote.content);
        contentView.addTextChangedListener(this);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_note) {
            final Context appContext = getActivity().getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mNote.setDeleted();
                    appContext.getContentResolver().update(NotesProvider.NOTES.withId(mNote.id), mNote.getContentValues(), null, null);
                }
            }).start();
            getActivity().navigateUpTo(new Intent(getActivity(), NoteListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mNote.title = titleView.getText().toString();
        mNote.content = contentView.getText().toString();
        final Context appContext = getActivity().getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mNote.setEdited();
                appContext.getContentResolver().update(NotesProvider.NOTES.withId(mNote.id), mNote.getContentValues(), null, null);
            }
        }).start();
    }
}
