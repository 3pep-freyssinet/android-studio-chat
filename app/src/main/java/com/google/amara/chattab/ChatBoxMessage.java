package com.google.amara.chattab;

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
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.amara.chattab.helper.SupabaseStorageUploader;
import com.google.amara.chattab.ui.main.ChatRepository;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
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
import java.util.ArrayList;
import java.util.Arrays;
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

        sharedViewModel.setDraftImage(imageUri);

        /*
        SupabaseStorageUploader.uploadImage(requireContext(), imageUri,
                new SupabaseStorageUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        Log.d("Chat", "✅ Upload success: " + publicUrl);

                        requireActivity().runOnUiThread(() ->
                                sharedViewModel.sendImageMessage(publicUrl)
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Chat", "❌ Upload error", e);
                    }
                });
                */
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
        typingContainer = view.findViewById(R.id.typingContainer);


        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("ChatBoxMessage", "onViewCreated ");
        Log.d("IDS", "Main=" + MainApplication.myId);
        Log.d("IDS", "Socket=" + SocketManager.getUserId());

        //chatViewModel.loadConversation(MainApplication.myId, MainApplication.friendId);

        Executors.newSingleThreadExecutor().execute(() -> {
            String withUserId = MainApplication.friendId;
            String myUserId   = MainApplication.myId;

            boolean hasUnread = sharedViewModel.hasUnreadMessages(withUserId);

            if (hasUnread) {

                // 1️⃣ Update local DB immediately
                sharedViewModel.repository.messageDao.markConversationSeen(withUserId, myUserId);

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
        ChatMessageAdapter adapter = new ChatMessageAdapter(myUserId);

        messageRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        messageRecycler.setAdapter(adapter);


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
                typingContainer.setVisibility(View.VISIBLE);
                messageRecycler.post(() ->
                        messageRecycler.scrollToPosition(adapter.getItemCount() - 1)
                );
            } else {
                typingContainer.setVisibility(View.GONE);
            }
        });

        chatViewModel.getMessages(MainApplication.myId, MainApplication.friendId).observe(getViewLifecycleOwner(), list -> {
            Log.d("TYPING", "observer triggered, size=" + list.size());
            if (!list.isEmpty()) Log.d("TYPING", "observer triggered, list=" + list.get(0).toString());
            
            if (list.isEmpty() && !conversationRequested) {
                conversationRequested = true; // ✅ prevent multiple calls
                chatViewModel.loadConversation(MainApplication.myId, MainApplication.friendId);
            }

            // ⭐ FORCE remove typing BEFORE rendering messages
            typingContainer.setVisibility(View.GONE);

            //adapter.submitList(list);// or notifyDataSetChanged()
            adapter.submitList(new ArrayList<>(list)); // ⭐ FORCE refresh


            messageRecycler.post(() ->
                    messageRecycler.scrollToPosition(list.size() - 1)
            );
        });

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
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sharedViewModel.setDraftText(s.toString());
                sharedViewModel.repository.notifyTyping(MainApplication.friendId); //toUserId);

                long now = System.currentTimeMillis();

                // ✅ FIRST keystroke → send start
                if (!isTyping) {
                    isTyping = true;
                    SocketManager.getSocket().emit("typing:start", buildTypingPayload());
                    lastTypingSent = now;

                    Log.d("TYPING_SEND", "typing:start (first)");
                }

                // ✅ keep-alive every few seconds (NOT every key)
                else if (now - lastTypingSent > TYPING_KEEP_ALIVE) {
                    SocketManager.getSocket().emit("typing:start", buildTypingPayload());
                    lastTypingSent = now;

                    Log.d("TYPING_SEND", "typing:start (keep-alive)");
                }

                // reset stop timer
                typingHandler.removeCallbacks(typingTimeout);
                typingHandler.postDelayed(typingTimeout, 2000);


            }
        });

        //send button

        sendButton.setOnClickListener(v -> {
            final String localId = UUID.randomUUID().toString();

            String text  = messageInput.getText().toString().trim();
            Uri imageUri = sharedViewModel.getDraftImage().getValue();

            if (text.isEmpty() && imageUri == null) return;

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
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        typingHandler.removeCallbacksAndMessages(null);
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

    @Override
    public void onPause() {
        super.onPause();
        MainApplication.currentChatUserId = null;
    }
}

