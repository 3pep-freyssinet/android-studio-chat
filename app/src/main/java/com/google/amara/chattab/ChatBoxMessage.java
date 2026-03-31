package com.google.amara.chattab;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.amara.chattab.helper.SupabaseStorageUploader;
import com.google.amara.chattab.ui.main.ChatRepository;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.amara.chattab.utils.TimeUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
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

        sharedViewModel = new ViewModelProvider(requireActivity())
                .get(ChatSharedViewModel.class);

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

        chatViewModel = new ChatViewModel(requireActivity().getApplication());

    }

    private JSONObject buildTypingPayload() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("to", MainApplication.friendId);
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
        sendButton      = view.findViewById(R.id.iv_send);
        imageUpload     = view.findViewById(R.id.iv_attach);
        messageInput    = view.findViewById(R.id.edt_message);

        previewImage    = view.findViewById(R.id.preview_image);
        btnRemoveImage  = view.findViewById(R.id.btn_remove_image);

        attachButton    = view.findViewById(R.id.iv_attach);
        //typingContainer = view.findViewById(R.id.typingContainer);


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

        //update 'header'
        sharedViewModel.getSelectedUser().observe(getViewLifecycleOwner(), selectedUser -> {
            if (selectedUser == null) return;

            String connectedAt = TimeUtils.formatSmartTime(selectedUser.getConnectedAt());
            String lastConnectedAt = TimeUtils.formatSmartTime(selectedUser.getLastConnectedAt());

            TextView headerName       = view.findViewById(R.id.header_name);
            TextView headerConnectedAt= view.findViewById(R.id.header_connected);
            TextView headerLastSeen   = view.findViewById(R.id.header_last_seen);
            ImageView imageProfile    = view.findViewById(R.id.header_avatar);
            View statusBadge          = view.findViewById(R.id.status_badge);


                headerName.setText(selectedUser.getNickname());

                if (selectedUser.getImageProfile() != null && !selectedUser.getImageProfile().isEmpty()) {
                    Glide.with(requireActivity())
                            .load(selectedUser.getImageProfile())
                            .placeholder(R.drawable.avatar)
                            .circleCrop()
                            .into(imageProfile);
                } else {
                    imageProfile.setImageResource(R.drawable.avatar);
                }

                switch(selectedUser.getStatus()) {

                    case UserStatus.ONLINE:
                        statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));

                        // 🔥 override text when online
                        headerConnectedAt.setText("Connected: now");
                        headerLastSeen.setText("Last seen: < 1 min");
                        break;

                    case UserStatus.AWAY:
                        statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(255,165,0)));

                        headerConnectedAt.setText(getString(R.string.connected_at, connectedAt));
                        headerLastSeen.setText("Away");
                        break;

                    default: // OFFLINE
                        statusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

                        headerConnectedAt.setText(getString(R.string.connected_at, connectedAt));
                        headerLastSeen.setText(getString(R.string.last_seen, lastConnectedAt));
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

            String withUserId = MainApplication.friendId;
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

        chatViewModel.loadInitialMessages(MainApplication.myId, MainApplication.friendId);

        /*
        chatViewModel.repo.getMessages().observe(getViewLifecycleOwner(), list -> {

            int newSize = (list == null) ? 0 : list.size();
            int currentSize = adapter.getItemCount();

            Log.d("UI", "newSize=" + newSize + ", currentSize=" + currentSize);

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
                    chatViewModel.repo.loadMoreMessages(MainApplication.myId, MainApplication.friendId);
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

                sharedViewModel.repository.notifyTyping(MainApplication.friendId);
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

            SocketManager.getSocket().emit("typing:stop", sharedViewModel.repository.createTypingPayload(MainApplication.friendId));

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

