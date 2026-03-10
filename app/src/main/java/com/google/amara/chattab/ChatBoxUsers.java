package com.google.amara.chattab;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.example.aymen.androidchat.AllUsersFragment;
//import com.example.aymen.androidchat.ChatUser;
//import com.example.aymen.androidchat.ChatUserAdapter;
//import com.example.aymen.androidchat.ChatMessage;
//import com.example.aymen.androidchat.UserSwipeRecyclerView;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//Ce fragment est défini dans 'TabChatActivity-SectionPager'

public class ChatBoxUsers extends Fragment {

    private ChatViewModel viewModel;
    private ChatUserAdapter adapter;
    private ChatSharedViewModel vm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_chat_box, container, false);

        ChatSharedViewModel sharedViewModel =
                new ViewModelProvider(requireActivity())
                        .get(ChatSharedViewModel.class);

        adapter = new ChatUserAdapter(requireContext());

        adapter.setOnUserClickListener(user -> {
            Log.d("UI", "User clicked: " + user.getNickname());
            sharedViewModel.selectUser(user);
        });

        RecyclerView userRecyclerView = view.findViewById(R.id.user_list);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        userRecyclerView.setAdapter(adapter);

        vm = new ViewModelProvider(requireActivity())
                .get(ChatSharedViewModel.class);

        adapter.setOnUserClickListener(user -> {
            vm.selectUser(user);

            // 🔥 tell Activity to switch tab
            if (getActivity() instanceof TabChatActivity) {
                ((TabChatActivity) getActivity()).openChatTab();
            }

            //sharedViewModel.resetUnreadCounter(user.chatId);
        });

        vm.getUsers().observe(
                getViewLifecycleOwner(),
                adapter::submitList
        );

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        /*
        viewModel.getConnectedUsers().observe(
                getViewLifecycleOwner(),
                users -> {
                    Log.d("UI", "Users received in fragment: " + users.size());
                    adapter.submitList(users);
                }
        );
        */

        vm = new ViewModelProvider(requireActivity())
                .get(ChatSharedViewModel.class);
        vm.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users == null) return;

            Log.d("UsersFragment", "👥 Users = " + users.size());
        });
    }
}


/*
public class ChatBoxUsers extends Fragment {

    private ChatViewModel viewModel;
    //private ChatSharedViewModel sharedViewModel;
    private ChatUserAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);

        //sharedViewModel = new ViewModelProvider(requireActivity())
        //        .get(ChatSharedViewModel.class);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.activity_chat_box,
                container,
                false
        );

        RecyclerView recyclerView = view.findViewById(R.id.user_list);
        FloatingActionButton fab = view.findViewById(R.id.fab);

        adapter = new ChatUserAdapter(requireContext());
        adapter.setOnUserClickListener(user -> {
            sharedViewModel.selectUser(user);
        });

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(v -> viewModel.onAddUserClicked());

        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getConnectedUsers().observe(
                getViewLifecycleOwner(),
                users -> {
                    Log.d("UI", "Users received in fragment: " + users.size());
                    adapter.submitList(users);
                }
        );
    }
}
*/
