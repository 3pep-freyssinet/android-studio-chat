package com.google.amara.chattab;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.amara.chattab.entities.UserUiState;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class FindFriendsActivity extends AppCompatActivity {

    private ChatViewModel viewModel;
    private FindFriendsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        RecyclerView rv = findViewById(R.id.rv_users);
        rv.setLayoutManager(new LinearLayoutManager(this));

        //manage tool bar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        adapter = new FindFriendsAdapter(new FindFriendsAdapter.OnUserActionListener() {

            @Override
            public void onAddFriend(ChatUser user) {
                Log.d("FRIENDS", "onAddFriend");
                viewModel.onSendRequest(user);
            }

            @Override
            public void onCancel(ChatUser user) {

                viewModel.onCancelRequest(user);
            }

            @Override
            public void onAccept(ChatUser user) {
                Log.d("FRIENDS", "onAccept userId = " + user.getUserId() + " : " + user.getNickname());
                viewModel.acceptFriend(user.getUserId());
            }

            @Override
            public void onReject(ChatUser user) {
                //viewModel.onReject(user);
            }

            @Override
            public void onMessage(ChatUser user) {
                // open chat if needed
            }
        });

        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // 🔥 1) Observe users (once, not inside click)
        viewModel.getAllUsers().observe(this, users -> {
            if (users == null) return;

            // (optional) filter out already accepted friends if you want
            // users = removeExistingFriends(users, cachedFriends);

            adapter.setUsers(users);
        });



        // 🔥 2) Observe UI states (local overrides)
        viewModel.getAllUiStates().observe(this, states -> {
            Log.d("FRIENDS", "getAllUiStates: ");
            Map<String, UserUiState> map = new HashMap<>();
            if (states != null) {
                for (UserUiState s : states) {
                    Log.d("FRIENDS", "getAllUiStates: " + s.userId + " : " + s.relationStatus);
                    map.put(s.userId, s);
                }
            }
            adapter.setUiStateMap(map);
        });

        // 🔥 Trigger load once
        viewModel.loadAllUsers();
    }
}