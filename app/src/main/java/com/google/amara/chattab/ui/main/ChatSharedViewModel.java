package com.google.amara.chattab.ui.main;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.MainApplication;
import com.google.amara.chattab.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;


public class ChatSharedViewModel extends AndroidViewModel {
    //String localId = UUID.randomUUID().toString();

    public ChatRepository repository;
    private LiveData<List<ChatUser>> users;

    private final MutableLiveData<ChatUser> selectedUser = new MutableLiveData<>();

    //private LiveData<List<ChatMessage>> messages         = repository.getMessages();
    private final MutableLiveData<Uri> pendingImage      = new MutableLiveData<>();

    private final MutableLiveData<Uri> draftImage        = new MutableLiveData<>();
    private final MutableLiveData<String> draftText      = new MutableLiveData<>("");

    public LiveData<Uri> getDraftImage() { return draftImage; }
    public LiveData<String> getDraftText() { return draftText; }

    public void setDraftImage(Uri uri) { draftImage.setValue(uri); }
    public void setDraftText(String text) { draftText.setValue(text); }

    public ChatSharedViewModel(@NonNull Application application) {
        super(application);
        repository = ChatRepository.get(application);
    }

    public void clearDraft() {
        draftImage.setValue(null);
        draftText.setValue("");
    }

    public LiveData<Uri> getPendingImage() {
        return pendingImage;
    }

    public void setPendingImage(Uri uri) {
        pendingImage.setValue(uri);
    }


    public LiveData<ChatUser> getSelectedUser() {
        return selectedUser;
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return repository.getMessages();
    }

    public void selectUser(ChatUser user) {
        selectedUser.setValue(user);
        repository.joinConversation(user);

        // current conversation
        MainApplication.currentChatUserId = user.getId();

        // mark conversation as seen
        JSONObject payload = new JSONObject();
        try {
            payload.put("fromUserId", user.getId());
        } catch (JSONException e) {}

        SocketManager.getSocket().emit("chat:mark_seen", payload);

    }

    public LiveData<List<ChatUser>> getUsers() {
        return repository.getUsers();
    }

    public void startChat() {
        repository.start();
    }

    public LiveData<Boolean> getTyping() {
        return repository.getTyping();
    }

    /*
    public void sendMessage(String text) {

        String myId = SocketManager.getUserId();

        // 1️⃣ Optimistic message
        ChatMessage optimistic = new ChatMessage(
                myId,
                selectedUser.getValue().getId(),
                text,
                "Sending...",
                "pending",
                "image",
                true
        );

        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();

        List<ChatMessage> updated = new ArrayList<>(current);
        updated.add(optimistic);
        messages.setValue(updated);

        // 2️⃣ Send to backend
        ChatRepository.get().sendMessage(text);
    }
    */

    public void sendMessage(String localId,  String text, @Nullable Uri imageUri, Context context) {

        ChatUser user = selectedUser.getValue();
        if (user == null) return;

        String myId = SocketManager.getUserId();

        // 🟡 1. Optimistic bubble (shows instantly)
        ChatMessage optimistic = new ChatMessage(
                null,
                localId,
                myId,
                user.getId(),
                text,           // message
                imageUri != null ? imageUri.toString() : null,   // imageUrl (no image yet)
                null,           // remoteUrl (no image yet)
                "now",          //"sending...",   // sent_at (temporary)
                ChatMessage.STATUS_SENDING, //"pending",      // seen
                imageUri != null ? "image" : "text",         // type
                true            // pending (still not confirmed by server)
        );

        optimistic.setLocalId(localId);
        //optimistic.setUploadProgress(0);

        repository.addLocalMessage(optimistic);

        // 🔥 SAVE TO ROOM (offline safe)
        Executors.newSingleThreadExecutor().execute(() ->
                repository.messageDao.insertMessage(optimistic)
        );


        // 2️⃣ Try to send to server (may fail if offline)
        //if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
        //    SocketManager.getSocket().emit("chat:send_message", optimistic.toJson());
        //}

        // 🟢 2. If there is an image → upload first
        if (imageUri != null) {
            repository.uploadImageAndSend(context, text, imageUri, user.getId(), localId);
            return;
        }
        // 🔵 TEXT MESSAGE FLOW
        if (SocketManager.getSocket() != null && SocketManager.getSocket().connected()) {
            repository.sendTextMessage(text, user.getId(), localId);
        }
    }


    /*
    public void sendImageMessage(String imageUrl) {

        String myId   = SocketManager.getUserId();
        ChatUser user = selectedUser.getValue();
        if (user == null) return;

        ChatMessage optimistic = new ChatMessage(
                myId,
                user.getId(),
                null,   //captionText,    // message (can be null or caption)
                imageUrl,       // imageUrl
                "sending...",
                "pending",
                "image",
                true
        );


        repository.addLocalMessage(optimistic); // show instantly
        repository.sendImageMessage(user.getId(), imageUrl);  // send to backend
    }
     */

    public boolean hasUnreadMessages(String withUserId) {
        String myId = SocketManager.getUserId();
        return repository.messageDao.countUnreadMessages(withUserId, myId) > 0;
    }


    public void resetUnreadCounter(String friendId) {
        //repository.resetUnreadCounter(friendId);
    }
}





