package com.google.amara.chattab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class ChatUserAdapter
        extends RecyclerView.Adapter<ChatUserAdapter.MyViewHolder> {

    private List<ChatUser> chatUsers = new ArrayList<>();
    private final Context context;

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_TYPING  = 1;

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
        int unread    = user.getNotSeenMessagesNumber();

        switch(user.getStatus()) {

            case UserStatus.ONLINE:
                holder.badge.setBackgroundTintList(
                        ColorStateList.valueOf(Color.GREEN));
                break;

            case UserStatus.AWAY:
                holder.badge.setBackgroundTintList(
                        ColorStateList.valueOf(Color.rgb(255,165,0)));
                break;

            default: //offline
                holder.badge.setBackgroundTintList(
                        ColorStateList.valueOf(Color.RED));
        }


        if (unread > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(unread));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        if (user.getImageProfile() != null && !user.getImageProfile().isEmpty()) {
            Glide.with(context)
                    .load(user.getImageProfile())
                    .placeholder(R.drawable.avatar)
                    .circleCrop()
                    .into(holder.imageProfile);
        } else {
            holder.imageProfile.setImageResource(R.drawable.avatar);
        }

        holder.nickname.setText(user.getNickname());
        holder.timeConnection.setText(
                "Connection at: " + user.getConnectedAt()
        );
        holder.lastTimeConnection.setText(
                "Last connection: " + user.getLastConnectedAt()
        );

        Log.d("ChatUserAdapter", "👥 nickname = " + user.getNickname());
        Log.d("ChatUserAdapter", "👥 Not seen messages = " + user.getNotSeenMessagesNumber());

        //holder.notSeenMessages.setText(
        //        "Not seen messages: " + user.getNotSeenMessagesNumber()
        //);

        /*
        if (user.getUnreadCount() > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(String.valueOf(user.getUnreadCount()));
        } else {
            badge.setVisibility(View.GONE);
        }
        holder.notSeenMessages.setText("999");
        */

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
        ImageView badge;
        TextView notSeenMessages;
        TextView unreadBadge;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nickname            = itemView.findViewById(R.id.nickname);
            timeConnection      = itemView.findViewById(R.id.time_connection);
            lastTimeConnection  = itemView.findViewById(R.id.last_time_connection);
            imageProfile        = itemView.findViewById(R.id.image_profile);
            statusView          = itemView.findViewById(R.id.status_view);
            notSeenMessages     = itemView.findViewById(R.id.tv_not_seen_messages);
            unreadBadge         = itemView.findViewById(R.id.tv_unread_badge);
            badge               = itemView.findViewById(R.id.badge);

        }
    }
}

