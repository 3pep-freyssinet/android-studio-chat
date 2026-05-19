package com.google.amara.chattab;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.amara.chattab.entities.UserUiState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChatUserAdapter
        extends RecyclerView.Adapter<ChatUserAdapter.MyViewHolder> {

    
    private List<ChatUser> chatUsers = new ArrayList<>();
    private final Context context;

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_TYPING  = 1;

    public static final String STATUS_PENDING  = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_REJECTED= "rejected";

    public Map<String, UserUiState> getUserStateMap() {
        return stateMap;
    }

    public List<ChatUser> getCurrentList() {
        return chatUsers;
    }

    public enum UserState {
        NONE,
        PENDING_SENT,
        PENDING_RECEIVED,
        FRIEND
    }
    private UserState state;

    private List<ChatUser> users = new ArrayList<>();

    private Map<String, UserUiState> stateMap = new HashMap<>();

    public void setUserStateMap(Map<String, UserUiState> map) {
        if (map == null) {
            this.stateMap = new HashMap<>();
        } else {
            this.stateMap = map;
        }
        notifyDataSetChanged();
    }

    public void updateList(List<ChatUser> newList) {
        chatUsers = newList;
        notifyDataSetChanged();
    }

     // onBindViewHolder uses 'users'


    // Click callback
    public interface OnUserClickListener {
        void onUserClicked(ChatUser user);
        default void onCancel(ChatUser user) {
            // optional
        }
        default void onAccept(ChatUser user) {
            // optional
        }

        default void onReject(ChatUser user) {
            // optional
        }

        void onAddFriend(ChatUser user);

        void onMessage(ChatUser user);

        void onLongPress(ChatUser user);
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
                " nickname=" + user.getNickname() +
                "sent by me = " + (user.isRequestSentByMe()));

        Log.d("HIGHLIGHT", "binding " + user.getUserId() +
                " highlighted=" + user.getUserId().equals(highlightedUserId));

        if (user.getUserId().equals(highlightedUserId)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFE082")); // highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

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

        // reset everything
        holder.messageBtn.setVisibility(View.GONE);
        holder.cancelBtn.setVisibility(View.GONE);
        holder.acceptBtn.setVisibility(View.GONE);
        holder.rejectBtn.setVisibility(View.GONE);

        holder.statusText.setText(null);
        holder.swipeHint.setText(null);

        //get the state
        UserUiState s = stateMap.get(user.getUserId());

        String relation  = user.getRelationStatus();
        boolean sentByMe = user.isRequestSentByMe();

        if (s != null) {
            relation = s.relationStatus;
            sentByMe = s.sentByMe;
        }
        
        if ("accepted".equals(relation)) {
            state = UserState.FRIEND;
        } else if ("pending".equals(relation)) {
            state = sentByMe ? UserState.PENDING_SENT : UserState.PENDING_RECEIVED;
        } else {
            state = UserState.NONE;
        }

        //update 'textStatus'
        if ("accepted".equals(relation)) {

            holder.statusText.setText("Friend");

        } else if ("pending".equals(relation)) {

            if (sentByMe) {
                holder.statusText.setText("Requested");
                holder.statusText.setTextColor(Color.rgb(255,165,0));
                holder.swipeHint.setText("Swipe left to cancel : ← Cancel");
            } else {
                holder.statusText.setText("Wants to connect");
                holder.swipeHint.setText("Swipe to respond : ← Reject    Accept →");
            }

        } else {

            holder.statusText.setText("");
        }

        //set an action
        holder.itemView.setOnClickListener(v -> {

            switch (state) {

                case NONE:
                    listener.onAddFriend(user);
                    break;

                case PENDING_SENT:
                    listener.onCancel(user);
                    break;

                case PENDING_RECEIVED:
                    listener.onAccept(user);
                    break;

                case FRIEND:
                    listener.onMessage(user);
                    break;
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(user);
            return true;
        });

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
        //holder.statusText.setText(user.getRelationStatus());
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

    private void showAddFriend(MyViewHolder holder, ChatUser user) {
        holder.addFriendbtn.setVisibility(View.VISIBLE);
        holder.addFriendbtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClicked(user);
            }
        });
    }

    private void showFriendCancel(MyViewHolder holder, ChatUser user) {

            holder.cancelBtn.setText("Cancel");
            holder.cancelBtn.setVisibility(View.VISIBLE);
            holder.statusText.setText("Request canceled");
            holder.acceptBtn.setVisibility(View.GONE);
            holder.rejectBtn.setVisibility(View.GONE);
            holder.messageBtn.setVisibility(View.GONE);

            holder.cancelBtn.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(user);
            });

    }

    private void showRejected(MyViewHolder holder, ChatUser user) {

        holder.statusText.setVisibility(View.VISIBLE);
        holder.statusText.setText("Request rejected");

        holder.acceptBtn.setVisibility(View.GONE);
        holder.rejectBtn.setVisibility(View.GONE);
        holder.messageBtn.setVisibility(View.GONE); // 🔥 IMPORTANT
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

    public int getPositionByUserId(String userId) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    private String highlightedUserId;

    public void setHighlightedUserId(String userId) {
        this.highlightedUserId = userId;
        Log.d("HIGHLIGHT", "Applying highlight to = " + userId);
        notifyDataSetChanged();
    }

    //------------------UserState-------
    /*
    static class UserState{
        static UserState FRIEND;
        static UserState PENDING_SENT;
        static UserState PENDING_RECEIVED;
        static UserState NONE;
     }
     */
    
        /*
        public enum UserState {
        NONE,
        PENDING_SENT,
        PENDING_RECEIVED,
        FRIEND
        }
         */
   
    // ---------------- ViewHolder ----------------

    static class MyViewHolder extends RecyclerView.ViewHolder {


        public View btnAction;
        TextView nickname;
        TextView timeConnection;
        TextView lastTimeConnection;
        ImageView imageProfile;
        ImageView statusView;
        ImageView badge;
        TextView notSeenMessages;
        TextView unreadBadge;

        TextView swipeHint;
        TextView statusText;
        Button acceptBtn;
        Button rejectBtn;
        Button cancelBtn;
        Button messageBtn;
        Button addFriendbtn;

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
            swipeHint           = itemView.findViewById(R.id.swipe_hint);
            acceptBtn           = itemView.findViewById(R.id.btn_accept);
            rejectBtn           = itemView.findViewById(R.id.btn_reject);
            cancelBtn           = itemView.findViewById(R.id.btn_cancel);
            messageBtn          = itemView.findViewById(R.id.btn_message);
            addFriendbtn        = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}

