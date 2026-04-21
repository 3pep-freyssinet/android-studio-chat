package com.google.amara.chattab;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//This fragment is defined in 'TabChatActivity-SectionPager'.
public class ChatBoxMessage extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 100 ;
    private ChatSharedViewModel sharedViewModel;
    private ChatViewModel chatViewModel;
    private RecyclerView messageRecycler;
    private ImageView sendButton, attachButton, imageUpload, previewImage, btnRemoveImage;
    private EditText messageInput;
    private TextView emptyView;
    private LinearLayout pendingLayout, rejectedLayout;
    private TextView pendingStatus, rejectedStatus;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View typingContainer;

    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private boolean isTyping    = false;
    private long lastTypingSent = 0;
    private static final long TYPING_INTERVAL   = 1500; // 1.5 sec
    private static final long TYPING_KEEP_ALIVE = 3000; // 3s
    JSONObject payload = new JSONObject();
    private boolean conversationRequested = false;
    private boolean isUserTyping = false;
    private boolean fragmentJustCreated = true;
    private boolean isFirstLoad         = true;
    private ChatMessageAdapter adapter;
    private List<ChatMessage> fakeMessages = null;

    private final Runnable typingTimeout = () -> {
        isTyping = false;
        SocketManager.getSocket().emit("typing:stop", buildTypingPayload());
    };
   


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleImagePicked(result.getData().getData());
                    }
                }
        );

        // ✅ OK in onCreate (no view access)
        sharedViewModel =
                new ViewModelProvider(requireActivity())
                        .get(ChatSharedViewModel.class);

        chatViewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);

        Log.d("VM_CHECK", "VM instance = " + chatViewModel.hashCode());
    }

    private JSONObject buildTypingPayload() {
        JSONObject obj = new JSONObject();
        String friendId = chatViewModel.getCurrentFriendId().getValue();
        try {
            obj.put("to", friendId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    private void handleImagePicked(Uri imageUri) {
        if (imageUri == null) return;

        Log.d("Chat", "Image selected: " + imageUri);

        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;

        try {
            requireContext().getContentResolver()
                    .takePersistableUriPermission(imageUri, takeFlags);
        } catch (Exception e) {
            Log.e("URI", "Failed to persist permission", e);
        }

        sharedViewModel.setDraftImage(imageUri);
    }


    private void onImageSelected(Uri imageUri) {
        // For now: just send URI to ViewModel
        sharedViewModel.setPendingImage(imageUri);
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view       = inflater.inflate(R.layout.chat_box_message, container, false);

        messageRecycler = view.findViewById(R.id.recycler_messages);
        emptyView       = view.findViewById(R.id.empty_view);
        sendButton      = view.findViewById(R.id.iv_send);
        imageUpload     = view.findViewById(R.id.iv_attach);
        messageInput    = view.findViewById(R.id.edt_message);

        previewImage    = view.findViewById(R.id.preview_image);
        btnRemoveImage  = view.findViewById(R.id.btn_remove_image);

        attachButton    = view.findViewById(R.id.iv_attach);
        //typingContainer = view.findViewById(R.id.typingContainer);
        pendingLayout   = view.findViewById(R.id.pendingLayout);
        rejectedLayout  = view.findViewById(R.id.rejectedLayout);
        pendingStatus   = view.findViewById(R.id.tv_pending);
        rejectedStatus  = view.findViewById(R.id.tv_rejected);
        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        //to place the layout bottom above the system bar(left arrow, Home, stack).
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // 👇 Use the larger of the two (keyboard OR nav bar)
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);

            v.setPadding(0, 0, 0, bottomPadding);

            return insets;
        });

        ///////////////////////////////
        //update 'header'
        sharedViewModel.getSelectedUser().observe(getViewLifecycleOwner(), selectedUser -> {

            if (selectedUser == null) return;

            String userId = selectedUser.getUserId();

            //update 'statusText' : relationship
            observeUser_(view, userId); // 🔥 delegate

            //"2026-04-06 17:07:34.442457+00"
            //String connectedAt        = TimeUtils.formatSmartTime(selectedUser.getConnectedAt());
            //String lastConnectedAt    = TimeUtils.formatSmartTime(selectedUser.getLastConnectedAt());

            TextView headerName       = view.findViewById(R.id.header_name);
            TextView headerConnectedAt= view.findViewById(R.id.header_connected);
            TextView headerLastSeen   = view.findViewById(R.id.header_last_seen);
            TextView statusText       = view.findViewById(R.id.status_text); //friendship status
            ImageView imageProfile    = view.findViewById(R.id.header_avatar);
            View statusBadge          = view.findViewById(R.id.status_badge);//online/offline

            headerName.setText(selectedUser.getNickname());
            //statusText.setText(selectedUser.getRelationStatus());
            if (selectedUser.getImageProfile() != null && !selectedUser.getImageProfile().isEmpty()) {
                Glide.with(requireActivity())
                        .load(selectedUser.getImageProfile())
                        .placeholder(R.drawable.avatar)
                        .circleCrop()
                        .into(imageProfile);
            } else {
                imageProfile.setImageResource(R.drawable.avatar);
            }

            switch(selectedUser.getOnlineStatus()) {

                case UserStatus.ONLINE:
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));

                    // 🔥 override text when online
                    headerConnectedAt.setText("Connected: now");
                    headerLastSeen.setText("Last seen: < 1 min");
                    break;

                case UserStatus.AWAY:
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(255,165,0)));

                //headerConnectedAt.setText(getString(R.string.connected_at, connectedAt));
                headerConnectedAt.setText(getString(R.string.connected_at, "999"));
                headerLastSeen.setText("Away");
                break;

                default: // OFFLINE
                    statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

                    //headerConnectedAt.setText(getString(R.string.connected_at, connectedAt));
                    headerConnectedAt.setText(getString(R.string.connected_at, "999"));
                    //headerLastSeen.setText(getString(R.string.last_seen, lastConnectedAt));
                    headerLastSeen.setText("888");
                    break;
        }
        });
        //end 'header' update

        //////////////////////////////////////////////////////////////////////////
        //run once
        //sharedViewModel.repository.generateFakeMessages(MainApplication.myId, MainApplication.friendId);
        //if (fakeMessages == null) {
        //    fakeMessages = (List<ChatMessage>) generateFakeMessages();
        //}
        /////////////////////////////////////////////////////////////////////
        sharedViewModel.repository.resetTyping();

        //'executor' is running on background thread
        Executors.newSingleThreadExecutor().execute(() -> {

            String withUserId = chatViewModel.getCurrentFriendId().getValue();
            String myUserId   = MainApplication.myId;

            boolean hasUnread = sharedViewModel.hasUnreadMessages(withUserId);

            if (hasUnread) {

                // 1️⃣ Update local DB
                sharedViewModel.repository.messageDao
                        .markConversationSeen(withUserId, myUserId);

                // 2️⃣ Notify backend
                JSONObject obj = new JSONObject();
                try {
                    obj.put("fromUserId", withUserId);
                    SocketManager.getSocket().emit("chat:mark_seen", obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        /*
        // ✅ SAFE: view lifecycle exists
        sharedViewModel.getSelectedUser().observe(
                getViewLifecycleOwner(),
                user -> {
                    if (user != null) {
                        Log.d("Chat", "Joining conversation with " + user.getChatId());
                        ChatRepository.get().joinConversation(user);
                    }
                }
        );
        */


        String myUserId = SocketManager.getUserId();
        adapter = new ChatMessageAdapter(myUserId);

        chatViewModel.loadInitialMessages();

        //When the user enter 'ChatBoxMessage'
        String friendId = chatViewModel.getCurrentFriendId().getValue();

        if (friendId != null) {
            observeUser(friendId);
        }


        chatViewModel.getAcceptEvents().observe(getViewLifecycleOwner(), userId -> {

            if (userId == null) return;

            Snackbar.make(requireView(),
                    "Your request has been accepted",
                    Snackbar.LENGTH_LONG
            ).show();

        });


        chatViewModel.getRejectEvents().observe(getViewLifecycleOwner(), userId -> {
            Log.d("REJECT_EVENTS", "userId = " +userId);
            if (userId == null) return;

            String current = chatViewModel.getCurrentFriendId().getValue();

            if (!userId.equals(current)) return;

            /*
            // 🔥 Inform user
            Toast.makeText(getContext(),
                    "Your request has been rejected",
                    Toast.LENGTH_SHORT
            ).show();
            */

            // 🔥 Navigate back
            if (getActivity() instanceof TabChatActivity) {
                ((TabChatActivity) getActivity()).openUsersTab();
            }
        });


        //messages observer
        chatViewModel.getCurrentFriendId()
                .observe(getViewLifecycleOwner(), friendId_ -> {

                    Log.d("MESSAGES", "friendId = " + friendId_);

                    if (friendId_ == null) return;

                    String myId = SocketManager.getUserId();

                    chatViewModel.getMessages(myId, friendId_)
                            .observe(getViewLifecycleOwner(), messages -> {

                                Log.d("MESSAGES", "Messages = " + messages.size() + " friendId = " + friendId);
                                adapter.submitList(messages);
                                messageRecycler.post(() -> messageRecycler.scrollToPosition(adapter.getItemCount() - 1));
                            });


                });


    /*
    chatViewModel.getCurrentFriendId()
            .observe(getViewLifecycleOwner(), friendId -> {

            Log.d("MESSAGES", "friendId = " + friendId);

            if (friendId == null) return;

            String myId = SocketManager.getUserId();


                chatViewModel.getUserById(friendId).observe(getViewLifecycleOwner(), user -> {

                    if (user == null) return;

                    if (user.isPending()) {
                        showPendingUI();
                    } else if (user.isAccepted()) {
                        showChatUI();
                    } else if (user.isRejected()) {
                        showRejectedUI();
                    }
                });
            });
    */
        //////////////


        /*
        chatViewModel.repo.getMessages().observe(getViewLifecycleOwner(), list -> {

            int newSize = (list == null) ? 0 : list.size();
            int currentSize = adapter.getItemCount();

            Log.d("MESSAGES", "newSize = " + newSize + ", adapter current size = " + currentSize);

            if(!list.isEmpty()){
                //Log.d("MESSAGES", "message = " + list.get(0).getMessage());
                Log.d("MESSAGES", "serverId = " + list.get(0).getServerId());
                for(int i = 0; i <= list.size() - 1; i++){
                    Log.d("MESSAGES", "message = " + list.get(i).getMessage());
                }
            }


            // 🚫 Prevent flicker: ignore transient empty list
            if (newSize == 0 && currentSize > 0) {
                Log.d("UI", "Ignore empty emission");
                return;
            }

            adapter.submitList(new ArrayList<>(list));


            if (isFirstLoad && newSize > 0) {
                isFirstLoad = false;

                messageRecycler.post(() ->
                        messageRecycler.scrollToPosition(adapter.getItemCount() - 1)
                );
            }
        });
        */



        // In ChatBoxMessages Fragment
        chatViewModel.getIsTyping().observe(getViewLifecycleOwner(), isTyping -> {
            adapter.showTypingIndicator(isTyping);
            if ((Boolean)isTyping) {
                messageRecycler.post(() -> messageRecycler.scrollToPosition(adapter.getItemCount() - 1));
            }
        });

        /* used in development mode
        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId)
                .observe(getViewLifecycleOwner(), list -> {

                    int newSize = (list == null) ? 0 : list.size();
                    int currentSize = adapter.getItemCount();

                    Log.d("UI", "newSize=" + newSize + ", currentSize=" + currentSize);

                    if (newSize == 0 && currentSize > 0) {
                        Log.d("UI", "Ignore empty emission");
                        return;
                    }

                    List<ChatMessage> base = (list == null) ? new ArrayList<>() : list;

                    // 🔥 Add fake messages here
                    List<ChatMessage> combined = new ArrayList<>(base);
                    combined.addAll(fakeMessages);

                    // 🔥 Sort by date (VERY IMPORTANT)
                    Collections.sort(combined, (a, b) ->
                            a.getSent_at().compareTo(b.getSent_at())
                    );

                    List<ChatMessage> displayList = buildDisplayList(combined);

                    adapter.submitList(new ArrayList<>(displayList));

                    if (isFirstLoad && !combined.isEmpty()) {
                        isFirstLoad = false;

                        messageRecycler.post(() ->
                                messageRecycler.scrollToPosition(adapter.getItemCount() - 1)
                        );
                    }
                });
        */

        //observer used in 'production' mode.
        /*
        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId)
                .observe(getViewLifecycleOwner(), list -> {

            if (list == null) return;

            // 1. Handle empty → trigger load if needed
            if (list.isEmpty()) {
                chatViewModel.loadConversation(MainApplication.myId, MainApplication.friendId);
                return;
            }

            // 2. Build UI list
            List<ChatMessage> displayList = buildDisplayList(list);

            // 3. Submit to adapter
            adapter.submitList(displayList);
        });
        */

        //sharedViewModel.getScrollToMessage().observe(getViewLifecycleOwner(), messageId -> {
        //    if (messageId != null) {
        //        scrollToMessage(messageId);
        //    }
        //});

        /*
        String friendId_ = chatViewModel.getCurrentFriendId().getValue();
        String myId_     = SocketManager.getUserId();

        Log.d("CHAT_BOX_MESSAGE", "myId=" + myId_ + ", friendId=" + friendId_);

        chatViewModel.getCurrentFriendId().observe(getViewLifecycleOwner(), friendId -> {
            Log.d("CHAT_BOX_MESSAGE", "Here 1");
            if (friendId == null) return;
            Log.d("CHAT_BOX_MESSAGE", "Here 2");
            String myId = SocketManager.getUserId();

            chatViewModel.getMessages(myId, friendId)
                    .observe(getViewLifecycleOwner(), messages -> {

                        if (messages == null) return;

                        adapter.submitList(messages);
                    });
        });
        */

        /*
        chatViewModel.messages
                .observe(getViewLifecycleOwner(), messages -> {

            if (messages == null || messages.isEmpty()){
                Log.d("CHAT", "No messages yet");

                // ✅ Show empty UI
                emptyView.setVisibility(View.VISIBLE);
                messageRecycler.setVisibility(View.GONE);
                return;
            }

            emptyView.setVisibility(View.GONE);
            messageRecycler.setVisibility(View.VISIBLE);

            String pendingId = sharedViewModel.getPendingMessageId();

            adapter.submitList(messages, () -> {   // 🔴 IMPORTANT: use callback

                if (pendingId == null) return;

                boolean found = false;

                List<ChatMessage> displayList = adapter.getCurrentList(); // 🔴 use adapter list

                for (int i = 0; i < displayList.size(); i++) {

                    ChatMessage msg = displayList.get(i);

                    if (msg.serverId != null &&
                            String.valueOf(msg.serverId).equals(pendingId)) {

                        found = true;

                        Log.d("SCROLL", "✅ Found message at position " + i);

                        messageRecycler.scrollToPosition(i);
                        int finalI = i;
                        messageRecycler.post(() -> {
                            highlightMessage(finalI); // ✅ now view exists
                        });

                        sharedViewModel.clearPendingMessageId();
                        break;
                    }
                }

                if (!found) {
                    Log.d("SCROLL", "⏳ Waiting for message " + pendingId);
                }

            }); // 🔴 callback end
        });
        */
        //observe selected user in tab 0.
        chatViewModel.getSelectedUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;

            Log.d("CHAT", "Opening chat with: " + user.getNickname());

            String myId__ = SocketManager.getUserId();

            chatViewModel.loadMessagesBetweenMeAndOther(myId__, user.getUserId());
        });

        /*
        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId)
                .observe(getViewLifecycleOwner(), list -> {

                    Log.d("HASH",
                            "input hash=" + list.hashCode()
                    );

                    if (list == null) return;

                    Log.d("UI", "Room emitted size=" + list.size());

                    // Optional: avoid flicker
                    if (list.isEmpty() && adapter.getItemCount() > 0) {
                        return;
                    }

                    Log.d("DEBUG", "RAW list size=" + list.size());

                    // 1. Build display list (headers, etc.)
                    List<ChatMessage> displayList = buildDisplayList(list);

                    Log.d("HASH",
                            "displayList hash=" + displayList.hashCode()
                    );


                    Log.d("DISPLAY_LIST", "size=" + displayList.size());

                    for (ChatMessage m : displayList) {
                        Log.d("DISPLAY_LIST",
                                "type=" + m.getItemType() +
                                        ", id=" + m.getLocalId() +
                                        ", text=" + (
                                        m.getItemType() == ChatMessage.TYPE_DATE_HEADER
                                                ? m.getDisplayText()
                                                : m.getMessage()
                                )
                        );
                    }

                    // 2. Submit to adapter
                    adapter.submitList(new ArrayList<>(displayList));

                    //hack
                    //List<ChatMessage> newList = new ArrayList<>(displayList);
                    // 🔥 HARD RESET
                    //adapter.submitList(null);
                    //adapter.submitList(newList);
                    //adapter.notifyDataSetChanged();
                    Log.d("AFTER", "itemCount=" + adapter.getItemCount());

                    // 3. Scroll only on first load
                    if (isFirstLoad && !list.isEmpty()) {
                        isFirstLoad = false;

                        messageRecycler.post(() ->
                                messageRecycler.scrollToPosition(adapter.getCurrentList().size() - 1)
                        );
                    }
                });
        */

        messageRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        messageRecycler.setAdapter(adapter);
        messageRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() == 0) {
                    chatViewModel.repo.loadMoreMessages();
                }
            }
        });

        if (isUserAtBottom()) {
            messageRecycler.scrollToPosition(adapter.getItemCount() - 1);
        }
        
        /*
        sharedViewModel.getMessages().observe(
                getViewLifecycleOwner(),
                adapter::submitList
        );
        */

        /*
        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId)
                .observe(getViewLifecycleOwner(), msgs -> {
                    Log.d("ROOM_FLOW", "Messages from Room: " + msgs.size());
                    adapter.submitList(msgs);
                });
        */

        chatViewModel.getTyping().observe(getViewLifecycleOwner(), isTyping -> {

            Log.d("TYPING_UI", "isTyping=" + isTyping);

            if (isTyping) {
                //typingContainer.setVisibility(View.VISIBLE);
                messageRecycler.post(() ->
                        messageRecycler.scrollToPosition(adapter.getItemCount() - 1)
                );
            } else {
                //typingContainer.setVisibility(View.GONE);
            }
        });

        /*
        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId).observe(getViewLifecycleOwner(), list -> {
            Log.d("TYPING", "observer triggered, size=" + list.size());
            if (!list.isEmpty()) Log.d("TYPING", "observer triggered, list=" + list.get(0).toString());

            if (list.isEmpty() && !conversationRequested) {
                conversationRequested = true; // ✅ prevent multiple calls
                chatViewModel.loadConversation(MainApplication.myId, MainApplication.friendId);
            }

            // ⭐ FORCE remove typing BEFORE rendering messages
            //typingContainer.setVisibility(View.GONE);

            //adapter.submitList(list);// or notifyDataSetChanged()
            adapter.submitList(new ArrayList<>(list)); // ⭐ FORCE refresh


            messageRecycler.post(() ->
                    messageRecycler.scrollToPosition(list.size() - 1)
            );
        });
        */

        sharedViewModel.getDraftImage().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                previewImage.setVisibility(View.VISIBLE);
                btnRemoveImage.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .into(previewImage);
            } else {
                previewImage.setVisibility(View.GONE);
                btnRemoveImage.setVisibility(View.GONE);
            }
        });

        btnRemoveImage.setOnClickListener(v -> sharedViewModel.setDraftImage(null));

        //listen to text changes
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                isUserTyping = true;
            }
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // ❌ Block typing events on initial setup or rotation
                if (fragmentJustCreated) {
                    fragmentJustCreated = false;
                    Log.d("TYPING_BLOCK", "Blocked typing: fragment just created");
                    return;
                }

                sharedViewModel.repository.notifyTyping();
            }
        });

        //send button
        sendButton.setOnClickListener(v -> {
            final String localId = UUID.randomUUID().toString();

            //prevent 3 dots bubble from appearing when send button is clicked.
            fragmentJustCreated = true;

            String text  = messageInput.getText().toString().trim();
            Uri imageUri = sharedViewModel.getDraftImage().getValue();

            if (text.isEmpty() && imageUri == null) return;

            // 👇 hide keyboard
            messageInput.clearFocus();

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(getActivity().getWindow(), messageInput);

            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.ime());
            }

            //stop typing
            //⭐ ADD THIS BEFORE sending message
            //SocketManager.getSocket().emit("typing:stop", payload);
            //String friendId = chatViewModel.getCurrentFriendId().getValue();

            //Get 'friendId' and emit 'emit("typing:stop"'.
            chatViewModel.getCurrentFriendId().observe(getViewLifecycleOwner(), friendId_ -> {
                Log.d("CHAT_BOX_MESSAGE", "Here 3");
                if (friendId_ == null) return;
                Log.d("CHAT_BOX_MESSAGE", "Here 4");
                String myId = SocketManager.getUserId();

                SocketManager.getSocket().emit("typing:stop",
                        sharedViewModel.repository.createTypingPayload(friendId_));

            });

            sharedViewModel.sendMessage(localId, text, imageUri, requireContext());

            messageInput.setText("");
            sharedViewModel.setDraftImage(null); // remove preview
        });


        //attach image = upload image
        imageUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            imagePickerLauncher.launch(intent);
        });
    }

    private void observeUser_(View view, String userId) {

        chatViewModel.getUserById(userId).observe(getViewLifecycleOwner(), user -> {

            if (user == null) return;

            TextView statusText = view.findViewById(R.id.status_text);

            statusText.setText(user.getRelationStatus()); // 🔥 always fresh
        });
    }

    private void observeUser(String friendId) {
        chatViewModel.getUserById(friendId)
                .observe(getViewLifecycleOwner(), user -> {

                    if (user == null) {
                        Log.d("CHAT", "User removed → close or reset UI");

                        showUnknownUI();   // or close chat
                        return;
                    }

                    if (user.isPending()) {
                        showPendingUI();
                    } else if (user.isAccepted()) {
                        showChatUI();
                    } else if (user.isRejected()) {
                        showRejectedUI();
                    }
                });
    }



    private void showUnknownUI() {

    }

    private void showRejectedUI() {

        pendingLayout.setVisibility(View.GONE);
        rejectedLayout.setVisibility(View.VISIBLE);

        messageRecycler.setVisibility(View.GONE);
        messageInput.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);

        rejectedStatus.setVisibility(View.VISIBLE);
        rejectedStatus.setText("❌ Request rejected");
    }

    private void showChatUI() {

        pendingLayout.setVisibility(View.GONE);
        rejectedLayout.setVisibility(View.GONE);

        messageRecycler.setVisibility(View.VISIBLE);
        messageInput.setVisibility(View.VISIBLE);
        sendButton.setVisibility(View.VISIBLE);

        pendingStatus.setVisibility(View.GONE);
    }

    private void showPendingUI() {

        pendingLayout.setVisibility(View.VISIBLE);
        rejectedLayout.setVisibility(View.GONE);

        messageRecycler.setVisibility(View.GONE);
        messageInput.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);

        pendingStatus.setVisibility(View.VISIBLE);
        pendingStatus.setText("⏳ Waiting for response...");
    }

    
    private void scrollToMessage(String messageId) {

        List<ChatMessage> messages = adapter.getCurrentList();

        if (messages == null || messages.isEmpty()) return;

        for (int i = 0; i < messages.size(); i++) {

            ChatMessage msg = messages.get(i);

            Log.d("SCROLL", "msg.serverId = " + msg.serverId + " messageId = " + messageId );

            if (msg.serverId != null &&
                    String.valueOf(msg.serverId).equals(messageId)) {

                Log.d("SCROLL", "scrolling" );

                messageRecycler.scrollToPosition(i);
                highlightMessage(i);
                break;
            }
        }
    }

    private void highlightMessage(int position) {

        RecyclerView.ViewHolder holder =
                messageRecycler.findViewHolderForAdapterPosition(position);

        if (holder == null) return;

        View itemView = holder.itemView;

    int highlightColor = Color.YELLOW;
    int normalColor = Color.TRANSPARENT;

    Handler handler = new Handler(Looper.getMainLooper());

    int duration = 5000; // total time (5 sec)
    int interval = 300;  // blink speed

    Runnable blinkRunnable = new Runnable() {

            long startTime = System.currentTimeMillis();
            boolean visible = true;

            @Override
            public void run() {

                long elapsed = System.currentTimeMillis() - startTime;

                if (elapsed > duration) {
                    itemView.setBackgroundColor(normalColor);
                    return;
                }

                itemView.setBackgroundColor(
                        visible ? highlightColor : normalColor
                );

                visible = !visible;

                handler.postDelayed(this, interval);
            }
        };

    handler.post(blinkRunnable);
    }

    private Collection<? extends ChatMessage> generateFakeMessages() {
        List<ChatMessage> fakeMessages = new ArrayList<>();

        // Today
        fakeMessages.add(createMessage("Hello today 1", getIsoTimeDaysAgo(0)));
        fakeMessages.add(createMessage("Hello today 2", getIsoTimeDaysAgo(0)));

        // Yesterday
        fakeMessages.add(createMessage("Yesterday msg 1", getIsoTimeDaysAgo(1)));
        fakeMessages.add(createMessage("Yesterday msg 2", getIsoTimeDaysAgo(1)));

        // 2 days ago
        fakeMessages.add(createMessage("Old msg 1", getIsoTimeDaysAgo(2)));

        // 5 days ago
        fakeMessages.add(createMessage("Old msg 2", getIsoTimeDaysAgo(5)));

        return fakeMessages;
    }

    private String getIsoTimeDaysAgo(int daysAgo) {
        return Instant.now()
                .minus(daysAgo, ChronoUnit.DAYS)
                .toString();
    }

    private boolean isUserAtBottom() {
        LinearLayoutManager lm = (LinearLayoutManager) messageRecycler.getLayoutManager();
        int lastVisible = lm.findLastVisibleItemPosition();
        return lastVisible >= adapter.getItemCount() - 3;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        typingHandler.removeCallbacksAndMessages(null);
    }

    private ChatMessage createMessage(String text, String isoTime) {
        ChatMessage msg = new ChatMessage();

        msg.setMessage(text);
        msg.setSent_at(isoTime);
        msg.setId_from("378"); // Alice
        msg.setId_to("379");   // Fanny
        msg.setType("text");
        msg.setStatus("sent");
        msg.setPending(true);
        msg.setIsTyping(true);

        return msg;
    }

    public void uploadImage(String imagePath){
        OkHttpClient client = new OkHttpClient();

        File file = new File(imagePath);

        RequestBody fileBody =
                RequestBody.create(file, MediaType.parse("image/*"));

        RequestBody requestBody =
                new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", file.getName(), fileBody)
                        .build();

        Request request = new Request.Builder()
                .url(MainApplication.SOCKET_URL + "/chat/upload")
                .addHeader("Authorization", "Bearer " + MainApplication.JWT_TOKEN)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UPLOAD", "Failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                JSONObject obj = null;
                try {
                    obj = new JSONObject(json);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                String imagePath = null;
                try {
                    imagePath = obj.getString("image_path");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // 🔥 now send socket message
                //sendMessageWithImage(imagePath);
            }
        });
    }

    private List<ChatMessage> buildDisplayList(List<ChatMessage> messages) {

        List<ChatMessage> result = new ArrayList<>();

        String lastDate = null;

        for (ChatMessage msg : messages) {

            String currentDate = extractDate(msg.getSent_at());
            Log.d("HEADER_TEXT", "date=" + currentDate + ", text=" + formatDateHeader(currentDate));
            Log.d("DATE_DEBUG", "msg=" + msg.getSent_at() + " → " + currentDate);

            // 🔥 FIX: first item OR new date
            if (lastDate == null || !currentDate.equals(lastDate)) {

                ChatMessage header = new ChatMessage();
                header.setMessage(null);
                header.setItemType(ChatMessage.TYPE_DATE_HEADER);
                header.setDisplayText(formatDateHeader(currentDate));
                header.setLocalId("header_" + currentDate);
                result.add(header);

                lastDate = currentDate;
            }

            //result.add(msg);
            result.add(new ChatMessage(msg));
        }

        return result;
    }

    private String formatDateHeader(String dateStr) {

        LocalDate date = LocalDate.parse(dateStr);
        LocalDate today = LocalDate.now();

        if (date.equals(today)) return "Today";

        if (date.equals(today.minusDays(1))) return "Yesterday";

        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String extractDate(String isoTime) {

        if (isoTime == null || isoTime.isEmpty()) {
            return "";
        }

        // 🚨 Handle special values like "now"
        if ("now".equalsIgnoreCase(isoTime)) {
            return LocalDate.now().toString();
        }

        try {
            Instant instant = Instant.parse(isoTime);
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            return zdt.toLocalDate().toString(); // YYYY-MM-DD
        } catch (DateTimeParseException e) {
            Log.e("DATE", "Invalid date: " + isoTime, e);

            // fallback to current date (or return empty if you prefer)
            return LocalDate.now().toString();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MainApplication.currentChatUserId = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

