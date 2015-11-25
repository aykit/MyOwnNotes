package org.aykit.MyOwnNotes.adapter;

/**
 * Created by mklepp on 22/11/15.
 */
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.database.NoteColumns;
import org.aykit.MyOwnNotes.database.model.Note;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotesListAdapter extends RecyclerView.Adapter<NotesListAdapter.ViewHolder> {

    private Cursor mCursor;
    private AdapterView.OnItemClickListener mOnItemClickListener;

    public NotesListAdapter(Cursor cursor) {
        mCursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.row_note, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView, this);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String title = mCursor.getString(mCursor.getColumnIndex(NoteColumns.TITLE));
        String mode = mCursor.getString(mCursor.getColumnIndex(NoteColumns.STATUS));

        holder.note.setText(title);
        holder.status.setText(mode);
    }

    @Override
    public int getItemCount() {
        return mCursor!=null?mCursor.getCount():0;
    }

    public void changeCursor(Cursor newCursor) {
        this.mCursor = newCursor;
    }

    public Note getItem(int position) {
        mCursor.moveToPosition(position);
        return new Note(mCursor);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void onItemHolderClick(RecyclerView.ViewHolder itemHolder){
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(null, itemHolder.itemView,
                    itemHolder.getAdapterPosition(), itemHolder.getItemId());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Bind(R.id.note)
        TextView note;
        @Bind(R.id.statusText)
        TextView status;

        private NotesListAdapter mAdapter;

        ViewHolder(View v, NotesListAdapter adapter) {
            super(v);
            ButterKnife.bind(this, v);
            v.setOnClickListener(this);
            mAdapter = adapter;
        }

        @Override
        public void onClick(View v) {
            mAdapter.onItemHolderClick(this);
        }
    }
}