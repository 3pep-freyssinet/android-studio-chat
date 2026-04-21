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
import android.widget.Button;
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

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_ACCEPTED = "accepted";

    private List<ChatUser> users = new ArrayList<>();

    public void updateList(List<ChatUser> newList) {
        chatUsers = newList;
        notifyDataSetChanged();
    }

     // onBindViewHolder uses 'users'


    // Click callback
    public interface OnUserClickListener {
        void onUserClicked(ChatUser user);
        default void onAccept(ChatUser user) {
            // optional
        }

        default void onReject(ChatUser user) {
            // optional
        }
    }

    private OnUserClickListener listener;

    public ChatUserAdapter(Context context) {
        this.context = context;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatUser> users) {
        chatUsers = users != null ? new ArrayList<>(users) : new ArrayList<>();
        Log.d("ADAPTER", "submitList size = " + chatUsers.size());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        int size = chatUsers != null ? chatUsers.size() : 0;
        Log.d("ADAPTER", "getItemCount = " + size);
        return size;
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
        Log.d("ChatUserAdapter", "onBindViewHolder called");

        ChatUser user = chatUsers.get(position);
        int unread    = user.getNotSeenMessagesNumber();

        Log.d("ADAPTER", "Binding user: " + user.getUserId() +
                " status=" + user.getOnlineStatus() +
                " nickname=" + user.getNickname());


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

        if (STATUS_PENDING.equals(user.getRelationStatus())) {
            showAcceptReject(holder, user);
        }
        else if (STATUS_ACCEPTED.equals(user.getRelationStatus())) {
            showFriend(holder, user);
        }
        else {
            showUnknown(holder, user);
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
        holder.statusText.setText(user.getRelationStatus());
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

    private void showUnknown(MyViewHolder holder, ChatUser user) {

        holder.statusText.setVisibility(View.VISIBLE);
        holder.statusText.setText("Unknown");

        holder.acceptBtn.setVisibility(View.GONE);
        holder.rejectBtn.setVisibility(View.GONE);

        holder.messageBtn.setVisibility(View.VISIBLE); // optional

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClicked(user);
        });
    }

    private void showFriend(MyViewHolder holder, ChatUser user) {

        holder.statusText.setVisibility(View.VISIBLE);
        holder.statusText.setText("Friend");

        holder.acceptBtn.setVisibility(View.GONE);
        holder.rejectBtn.setVisibility(View.GONE);

        //holder.messageBtn.setVisibility(View.VISIBLE);
        //holder.messageBtn.setOnClickListener(v -> {
        //    if (listener != null) listener.onUserClicked(user);
        //});

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClicked(user);
        });
    }

    private void showAcceptReject(MyViewHolder holder, ChatUser user) {

        holder.statusText.setVisibility(View.VISIBLE);
        holder.statusText.setText("Pending request");

        holder.acceptBtn.setVisibility(View.VISIBLE);
        holder.rejectBtn.setVisibility(View.VISIBLE);

        holder.messageBtn.setVisibility(View.GONE); // no chat yet (optional)

        holder.acceptBtn.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(user);
        });

        holder.rejectBtn.setOnClickListener(v -> {
            if (listener != null) listener.onReject(user);
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

        TextView statusText;
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

            statusText          = itemView.findViewById(R.id.status_text);
            acceptBtn           = itemView.findViewById(R.id.btn_accept);
            rejectBtn           = itemView.findViewById(R.id.btn_reject);
            messageBtn          = itemView.findViewById(R.id.btn_message);



        }
    }
}

