package com.google.amara.chattab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class ChatUserAdapter
        extends RecyclerView.Adapter<ChatUserAdapter.MyViewHolder> {

    private List<ChatUser> chatUsers = new ArrayList<>();
    private final Context context;

    // Click callback
    public interface OnUserClickListener {
        void onUserClicked(ChatUser user);
    }

    private OnUserClickListener listener;

    public ChatUserAdapter(Context context) {
        this.context = context;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    // 🔥 Called by Fragment when backend data changes
    public void submitList(List<ChatUser> users) {
        chatUsers = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MyViewHolder holder,
            int position
    ) {
        ChatUser user = chatUsers.get(position);

        holder.nickname.setText(user.getNickname());
        holder.timeConnection.setText(
                "Connection at: " + user.getConnectedAt()
        );
        holder.lastTimeConnection.setText(
                "Last connection: " + user.getLastConnectedAt()
        );

        // Click on row
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClicked(user);
            }
        });
    }

    // ---------------- ViewHolder ----------------

    static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView nickname;
        TextView timeConnection;
        TextView lastTimeConnection;
        ImageView imageProfile;
        ImageView statusView;
        TextView notSeenMessages;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nickname = itemView.findViewById(R.id.nickname);
            timeConnection = itemView.findViewById(R.id.time_connection);
            lastTimeConnection = itemView.findViewById(R.id.last_time_connection);
            imageProfile = itemView.findViewById(R.id.image_profile);
            statusView = itemView.findViewById(R.id.status_view);
            notSeenMessages = itemView.findViewById(R.id.tv_not_seen_messages);
        }
    }
}

