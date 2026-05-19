package com.google.amara.chattab;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import com.google.amara.chattab.entities.UserUiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.VH> {

    private List<ChatUser> users = new ArrayList<>();
    private Map<String, UserUiState> uiStateMap = new HashMap<>();
    //private final Consumer<ChatUser> onUserClick;

    public interface OnUserActionListener {
        void onAddFriend(ChatUser user);
        void onCancel(ChatUser user);
        void onAccept(ChatUser user);
        void onReject(ChatUser user);
        void onMessage(ChatUser user);
    }

    private OnUserActionListener listener;

    //constructor
    public FindFriendsAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<ChatUser> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void setUiStateMap(Map<String, UserUiState> map) {
        this.uiStateMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH  onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        ChatUser user = users.get(position);
        h.tvName.setText(user.getNickname());

        // 🔥 Reset ALL views (RecyclerView reuse fix)
        h.btnPrimary.setVisibility(View.GONE);
        h.btnSecondary.setVisibility(View.GONE);
        h.tvAction.setVisibility(View.GONE);

        // 🔥 Resolve state (UI state overrides DB)
        UserUiState s = uiStateMap.get(user.getUserId());

        String relation  = user.getRelationStatus();  // "none", "pending", "accepted"
        boolean sentByMe = user.isRequestSentByMe(); // MUST exist in your model

        if (s != null) {
            relation = s.relationStatus;
            sentByMe = s.sentByMe;
        }

        // 🔥 Render
        if ("pending".equals(relation)) {
            showRequested(h, user);
        } else if ("accepted".equals(relation)) {
            showFriend(h, user);
        } else { // "none"
            showAddFriend(h, user);
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    private void showAddFriend(VH h, ChatUser user) {
        h.btnPrimary.setVisibility(View.VISIBLE);
        h.btnPrimary.setText("+ Add friend");
        h.btnPrimary.setOnClickListener(v -> {
            // optimistic UI via Room
            // ViewModel should upsert UserUiState(pending, sentByMe=true)

            listener.onAddFriend(user);
        });
    }

    private void showCancel(VH h, ChatUser user) {
        h.btnPrimary.setVisibility(View.VISIBLE);
        h.btnPrimary.setText("Cancel");
        h.btnPrimary.setOnClickListener(v -> {
            listener.onCancel(user);
        });
    }

    private void showRequested(VH h, ChatUser user) {

        h.tvAction.setVisibility(View.VISIBLE);
        h.tvAction.setText("REQUESTED");
        h.tvAction.setTextColor(Color.BLACK);
        h.tvAction.setEnabled(false);
    }

    private void showFriend(VH h, ChatUser user) {
        h.btnPrimary.setVisibility(View.GONE);
        h.tvAction.setVisibility(View.VISIBLE);
        h.tvAction.setTextColor(Color.BLACK);
        h.tvAction.setText("FRIEND");
        h.tvAction.setEnabled(false);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAction;
        Button btnPrimary, btnSecondary;

        VH(View v) {
            super(v);
            tvName          = v.findViewById(R.id.tv_name);
            tvAction        = v.findViewById(R.id.tv_action); // optional label
            btnPrimary      = v.findViewById(R.id.btn_primary);
            btnSecondary    = v.findViewById(R.id.btn_secondary);
        }
    }
}
