package com.google.amara.chattab;


import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.example.aymen.androidchat.AllUsersFragment;
//import com.example.aymen.androidchat.ChatUser;
//import com.example.aymen.androidchat.ChatUserAdapter;
//import com.example.aymen.androidchat.ChatMessage;
//import com.example.aymen.androidchat.UserSwipeRecyclerView;
import com.google.amara.chattab.dao.UserUiStateDao;
import com.google.amara.chattab.entities.UserUiState;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

//Ce fragment est défini dans 'TabChatActivity-SectionPager'

public class ChatBoxUsers extends Fragment {

    private static final long REJECTED_DELAY = 60 * 60 * 1000;  //1 hour
    private static final String TAG = "ChatBoxUsers";

    private ChatViewModel       viewModel;
    private ChatUserAdapter     adapter;
    private ChatAllUsersAdapter allUsersAdapter;
    private ChatSharedViewModel vm;

    private List<ChatUser> cachedUsers   = new ArrayList<>();
    private List<ChatUser> cachedFriends = new ArrayList<>();
    private RecyclerView userRecyclerView;

    private String pendingHighlightUserId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);

        vm = new ViewModelProvider(requireActivity())
                .get(ChatSharedViewModel.class);

        Log.d("VM_CHECK", "VM instance = " + viewModel.hashCode());
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_chat_box, container, false);

        FloatingActionButton fab = view.findViewById(R.id.fab);

        fab.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), FindFriendsActivity.class));
        });

        
        //ChatSharedViewModel sharedViewModel =
        //        new ViewModelProvider(requireActivity())
        //                .get(ChatSharedViewModel.class);

        //friend-users adapter
        adapter = new ChatUserAdapter(requireContext());

        //adapter.setOnUserClickListener(user -> {
        //    Log.d("UI", "User clicked: " + user.getNickname());
        //    sharedViewModel.selectUser(user);
        //});

        userRecyclerView = view.findViewById(R.id.user_list);
        userRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        userRecyclerView.setAdapter(adapter);

        userRecyclerView.post(() -> {
            Log.d("RV", "Height = " + userRecyclerView.getHeight());
        });

        //manage swipe gesture
        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                ) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public float getSwipeThreshold(
                            @NonNull RecyclerView.ViewHolder viewHolder
                    ) {
                        return 0.3f;
                    }

                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            int direction
                    ) {

                        int position = viewHolder.getAdapterPosition();

                        ChatUser user = adapter.getCurrentList().get(position);

                        UserUiState state =
                                adapter.getUserStateMap().get(user.getUserId());

                        boolean sentByMe = false;
                        String relation = user.getRelationStatus();

                        if (state != null) {
                            sentByMe = state.sentByMe;
                            relation = state.relationStatus;
                        }

                        // -------------------------
                        // PENDING SENT
                        // Alice → Bob
                        // -------------------------
                        if ("pending".equals(relation) && sentByMe) {

                            if (direction == ItemTouchHelper.LEFT) {

                                viewModel.cancelFriendRequest(user.getUserId());

                                Toast.makeText(
                                        getContext(),
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        // -------------------------
                        // PENDING RECEIVED
                        // Bob → Alice
                        // -------------------------
                        else if ("pending".equals(relation) && !sentByMe) {

                            if (direction == ItemTouchHelper.RIGHT) {

                                viewModel.acceptFriend(user.getUserId());

                                Toast.makeText(
                                        getContext(),
                                        "Friend accepted",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            else if (direction == ItemTouchHelper.LEFT) {

                                viewModel.rejectFriend(user.getUserId());

                                Toast.makeText(
                                        getContext(),
                                        "Friend rejected",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        adapter.notifyItemChanged(position);
                    }
                };

        ItemTouchHelper itemTouchHelper =
                new ItemTouchHelper(callback);

        itemTouchHelper.attachToRecyclerView(userRecyclerView);

        //adapter for 'friend users'
        adapter.setOnUserClickListener(new ChatUserAdapter.OnUserClickListener() {

            @Override
            public void onUserClicked(ChatUser user) {

                vm.selectUser(user);
                Log.d("CHAT_BOX_USER", "onUserClicked User Id = " + user.getUserId());
                viewModel.setCurrentFriendId(user.getUserId());

                if(user.getRelationStatus().equals("pending"))return;

                // 🔥 tell Activity to switch tab
                if (getActivity() instanceof TabChatActivity) {
                    ((TabChatActivity) getActivity()).openChatTab();
                }
            }

            @Override
            public void onCancel(ChatUser user) {
                viewModel.cancelFriendRequest(user.getUserId());
            }

            @Override
            public void onAccept(ChatUser user) {
                Log.d("CHAT_BOX_USER", "onAccept User Id = " + user.getUserId());
                viewModel.acceptFriend(user.getUserId());
            }

            @Override
            public void onReject(ChatUser user) {
                viewModel.rejectFriend(user.getUserId());
            }

            @Override
            public void onAddFriend(ChatUser user) {
                viewModel.onSendRequest(user);
            }

            @Override
            public void onMessage(ChatUser user) {}

            @Override
            public void onLongPress(ChatUser user) {
                showBlockDialog(user);
            }
        });


        /*
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            adapter.updateList(users);
        });


        vm.getUsers().observe( getViewLifecycleOwner(),
                adapter::submitList
        );
        */

        return view;
    }

    private void showBlockDialog(ChatUser user) {

        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext());

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_block_user, null);

        dialog.setContentView(view);

        RadioGroup group = view.findViewById(R.id.durationGroup);
        Button confirm   = view.findViewById(R.id.btn_confirm);

        confirm.setOnClickListener(v -> {

            long durationMs = 0;
            int checkedId = group.getCheckedRadioButtonId();

            if (checkedId == R.id.oneHour) {
                durationMs = 3600_000;
            }
            else if (checkedId == R.id.oneDay) {
                durationMs = 24 * 3600_000;
            }
            else if (checkedId == R.id.sevenDays) {
                durationMs = 7 * 24 * 3600_000;
            }
            else if (checkedId == R.id.forever) {
                durationMs = -1;
            }

            viewModel.blockUser(user.getUserId(), durationMs );

            dialog.dismiss();
        });

        dialog.show();
    }

    private void openUsersDialogOnce() {
        List<ChatUser> filtered = removeExistingFriends(cachedUsers, cachedFriends);
        //showUsersDialog(filtered, cooldownMap);
    }

    private void loadAllUsers() {
        viewModel.fetchAllUsers(); // API call
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

        /*
        //observe messages
        viewModel.messages.observe(getViewLifecycleOwner(), messages -> {

                            if (messages == null || messages.isEmpty()) {
                                Log.d("CHAT", "No messages yet");
                            }
        });
        */

        /*
        //sharedViewModel observes 'user-friends'.
        vm.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users == null) return;
            Log.d("UsersFragment", "👥 Users = " + users.size());
            adapter.submitList(users);
            //adapter.updateList(friends);

        });
        */

        viewModel.getUserStateMap().observe(getViewLifecycleOwner(), stateMap -> {
            for (String key : stateMap.keySet()) {
                Log.d("CHAT_BOX_USER",
                        "key = " + key +
                                " relation = " + stateMap.get(key).relationStatus);
            }
            adapter.setUserStateMap(stateMap);
        });

        viewModel.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            cachedUsers = users;
        });

        viewModel.getFriendUsers().observe(getViewLifecycleOwner(), friends -> {
           Log.d("CHAT_BOX_USER", "Users friend size list = " + friends.size());
            cachedFriends = friends;

            adapter.submitList(friends);
            if (pendingHighlightUserId != null) {
                userRecyclerView.post(() -> {
                    highlightAndScrollToUser(pendingHighlightUserId);
                    pendingHighlightUserId = null;
                });
            }
        });


        viewModel.getAcceptEvents().observe(getViewLifecycleOwner(), userId -> {
            if (userId == null) return;

            Snackbar.make(requireView(),
                    "Your request has been accepted",
                    Snackbar.LENGTH_LONG
            ).show();
        });


        //observe reject request
        viewModel.getRejectEvents().observe(getViewLifecycleOwner(), userId -> {
            if (userId == null) return;

            Snackbar.make(requireView(),
                    "Your request has been rejected",
                    Snackbar.LENGTH_LONG
            ).show();
        });

        viewModel.getSelectedUserId().observe(getViewLifecycleOwner(), userId -> {
            if (userId == null) return;
            Log.d("HIGHLIGHT", "Fragment received = " + userId);
            pendingHighlightUserId = userId;

            //highlightAndScrollToUser(userId);
        });

        /*
        //sharedViewModel observes 'all users'
        vm.getAllUsers().observe(getViewLifecycleOwner(), users -> {

            List<ChatUser> friends =  removeExistingFriends(users);
            showUsersDialog(friends);
        });
        */

    }

    private void highlightAndScrollToUser(String userId) {

        Log.d("HIGHLIGHT", "Applying highlight to = " + userId);

        int position = adapter.getPositionByUserId(userId);
        Log.d("HIGHLIGHT", "Position = " + position);

        if (position == -1) return;

        adapter.setHighlightedUserId(userId);
        userRecyclerView.scrollToPosition(position);
    }

    private void showUsersDialog(List<ChatUser> users, Map<String, Long> cooldownMap) {

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_users);

        RecyclerView rv = dialog.findViewById(R.id.rv_all_users);
        rv.setLayoutManager(new LinearLayoutManager(ChatBoxUsers.this.getContext()));

        ChatAllUsersAdapter adapter = new ChatAllUsersAdapter(
                getContext(),        // context
                users,       // list
                user -> {

                    Executors.newSingleThreadExecutor().execute(() -> {

                        UserUiState state = viewModel.getUserUiStateSync(user.getUserId());

                        long ts = state != null ? state.lastRejectedAt : 0;
                        long now = System.currentTimeMillis();

                        if (ts > 0 && now - ts < 60_000) {

                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(getActivity(),
                                            "Try again later",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );

                            return;
                        }

                        // ✅ ONLY now proceed (on main thread)
                        new Handler(Looper.getMainLooper()).post(() -> {

                            //viewModel.sendFriendRequest(user.getUserId());
                            viewModel.onSendRequest(user);

                            viewModel.setPending(user);

                            viewModel.addFriend(user);
                            viewModel.setCurrentFriendId(user.getUserId());
                            vm.selectUser(user);

                            dialog.dismiss();
                        });
                    });
                },
                cooldownMap
        );

        rv.setAdapter(adapter);
        dialog.show();
    }


    private List<ChatUser> removeExistingFriends(
            List<ChatUser> allUsers,
            List<ChatUser> friends
    ) {

        String myUserId = MainApplication.myId;

        List<ChatUser> result = new ArrayList<>();

        for (ChatUser user : allUsers) {

            // ❌ remove myself
            if (user.getUserId().equals(myUserId)) continue;

            boolean isActiveRelation = false;

            if (friends != null) {
                for (ChatUser f : friends) {

                    if (f.getUserId().equals(user.getUserId())) {

                        String status = f.getRelationStatus();

                        // 🔥 ONLY exclude active states
                        if ("accepted".equals(status) || "pending".equals(status)) {
                            isActiveRelation = true;
                            break;
                        }
                    }
                }
            }

            // ✅ keep rejected users
            if (!isActiveRelation) {
                result.add(user);
            }
        }

        return result;
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
