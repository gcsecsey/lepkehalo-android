package hu.csecsey.gergely.lepkehalo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

/**
 * Created by gcsecsey on 01/08/2018.
 */

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.MyViewHolder> {
    private Context context;
    private List<Book> bookList;
    ImageLoader imageLoader = HttpRequests.getInstance().getImageLoader();
    RecyclerItemTouchHelper.RecyclerItemTouchHelperListener listener;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView author, title;
        public NetworkImageView thumbnail;
        public RelativeLayout viewBackground, viewForeground;

        public MyViewHolder(final View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.author);
            title = itemView.findViewById(R.id.title);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            viewBackground = itemView.findViewById(R.id.view_background);
            viewForeground = itemView.findViewById(R.id.view_foreground);

            RecyclerView.ViewHolder hello = this;


        }
    }

    public BookListAdapter(Context context, List<Book> bookList, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener listener) {
        this.context = context;
        this.bookList = bookList;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);

        final MyViewHolder holder = new MyViewHolder(itemView);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onTap(holder, holder.getLayoutPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final Book item = bookList.get(position);
        holder.author.setText(item.getAuthor());
        holder.title.setText(item.getTitle());

        if (imageLoader == null)
            imageLoader = HttpRequests.getInstance().getImageLoader();
        holder.thumbnail.setImageUrl(item.getThumbnailUrl(),imageLoader);
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public void removeItem(int position) {
        bookList.remove(position);
        // notify that item is removed
        // don't call notifyDataSetChanged() to keep view
        notifyItemRemoved(position);
    }

    public void restoreItem(Book item, int position) {
        bookList.add(position, item);
        //have to notify about insertion
        notifyItemInserted(position);
    }

}
