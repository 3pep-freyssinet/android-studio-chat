package com.google.amara.chattab;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;


public class ChatAllUsersAdapter
        extends RecyclerView.Adapter<ChatAllUsersAdapter.MyViewHolder> {

    private List<ChatUser> chatAllUsers = new ArrayList<>();
    private final Context context;
    private OnUserClickListener_ listener;

     // onBindViewHolder uses 'chatAllUsers'


    // Click callback
    public interface OnUserClickListener_ {
        void onUserClicked(ChatUser user);
    }

    //Constructor
    public ChatAllUsersAdapter(Context context,
                               List<ChatUser> chatAllUsers,
                               OnUserClickListener_ listener) {

        this.context = context;
        this.chatAllUsers = chatAllUsers;
        this.listener = listener;
    }

    public void updateList(List<ChatUser> newList) {
        chatAllUsers = newList;
        notifyDataSetChanged();
    }

    public void setOnUserClickListener(OnUserClickListener_ listener) {
        this.listener = listener;
    }

    // 🔥 Called by Fragment when backend data changes
    public void submitList(List<ChatUser> users) {
        chatAllUsers = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return chatAllUsers.size();
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
        ChatUser user = chatAllUsers.get(position);
        int unread    = user.getNotSeenMessagesNumber();

        //Normal click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClicked(user);
        });

        switch(user.getOnlineStatus()) {

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

        Log.d("ChatAllUserAdapter", "👥 nickname = " + user.getNickname());
        Log.d("ChatAllUserAdapter", "👥 Not seen messages = " + user.getNotSeenMessagesNumber());

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
        Button acceptBtn;
        Button rejectBtn;
        Button messageBtn;



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
            acceptBtn           = itemView.findViewById(R.id.btn_accept);
            rejectBtn           = itemView.findViewById(R.id.btn_reject);
            messageBtn          = itemView.findViewById(R.id.btn_message);



        }
    }
}

