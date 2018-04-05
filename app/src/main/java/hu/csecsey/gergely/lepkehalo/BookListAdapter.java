package hu.csecsey.gergely.lepkehalo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gcsecsey on 01/08/2018.
 */

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.MyViewHolder> {
    private List<Book> _bookList;
    private Gson _gson = new Gson();
    private SharedPreferences _prefs;
    private final String STORAGE_KEY = "lepkehalo.list.state";

    private ImageLoader imageLoader = HttpRequests.getInstance().getImageLoader();
    private RecyclerItemTouchHelper.RecyclerItemTouchHelperListener _listener;

    private List<BookListListener> _listeners = new ArrayList<BookListListener>();

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView author, title;
        NetworkImageView thumbnail;
        RelativeLayout viewBackground, viewForeground;

        MyViewHolder(final View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.author);
            title = itemView.findViewById(R.id.title);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            viewBackground = itemView.findViewById(R.id.view_background);
            viewForeground = itemView.findViewById(R.id.view_foreground);
        }
    }

    public interface BookListListener {
        void onBookListChanged();
    }

    public void addListener(BookListListener listener) {
        _listeners.add(listener);
    }

    public void notifyBookListChanged() {
        for (BookListListener bll : _listeners)
            bll.onBookListChanged();
        String json = _gson.toJson(_bookList);
        _prefs.edit().putString(STORAGE_KEY, json).apply();
    }

    public BookListAdapter(Context context, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener listener) {
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = _prefs.getString(STORAGE_KEY, "[]");
        Type typetoken = new TypeToken<ArrayList<Book>>() {}.getType();
        _bookList = _gson.fromJson(json, typetoken);
        if (_bookList == null) {
            _bookList = new ArrayList<>();
        }
        _listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);

        final MyViewHolder holder = new MyViewHolder(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.onTap(holder, holder.getLayoutPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final Book item = _bookList.get(position);
        holder.author.setText(item.getAuthor());
        holder.title.setText(item.getTitle());

        if (imageLoader == null)
            imageLoader = HttpRequests.getInstance().getImageLoader();
        holder.thumbnail.setImageUrl(item.getThumbnailUrl(),imageLoader);
    }

    @Override
    public int getItemCount() {
        return _bookList.size();
    }

    public void removeItem(int position) {
        _bookList.remove(position);
        // notify that item is removed
        // don't call notifyDataSetChanged() to keep view
        notifyItemRemoved(position);
        notifyBookListChanged();
    }

    public void restoreItem(Book item, int position) {
        _bookList.add(position, item);
        //have to notify about insertion
        notifyItemInserted(position);
        notifyBookListChanged();
    }

    public void pushItem(Book item) {
        //get the position of the book in the list
        int pos = inList(item);
        //if the book is not in the list, then add it
        if (pos == -1) {
            _bookList.add(0, item);
        } else {
            //if the book was in the list, then bring it forward
            Book temp = _bookList.remove(pos);
            _bookList.add(0, temp);
        }
        notifyItemInserted(0);
        notifyBookListChanged();
    }

    private int inList(Book item) {
        if (getItemCount() == 0) return -1;

        for (Book b : _bookList) {
            if (b.getId() == item.getId()) {
                return _bookList.indexOf(b);
            }
        }
        return -1;
    }

}
