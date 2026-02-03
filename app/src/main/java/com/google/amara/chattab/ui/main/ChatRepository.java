package com.google.amara.chattab.ui.main;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.SocketManager;
import com.google.amara.chattab.dao.AppDatabase;
import com.google.amara.chattab.dao.ChatDatabase;
import com.google.amara.chattab.dao.MessageDao;
import com.google.amara.chattab.helper.SupabaseStorageUploader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import io.socket.client.Socket;
/*
  public static synchronized ChatRepository get() {
      if (instance == null) {
          Socket socket = SocketHandler.getSocket();
          instance = new ChatRepository(socket);
      }
      return instance;
  }

   */
public class ChatRepository {

    private static ChatRepository instance;
    private Socket socket;

    private final MutableLiveData<List<ChatUser>> users       = new MutableLiveData<>();

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());

    private boolean listening = false;
    public MessageDao messageDao;

    private ChatRepository(Socket socket) {
        this.socket = socket;
        //attachConnectListener();
    }

    private ChatRepository() {
        this.socket = SocketManager.getSocket();
    }

    private ChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        messageDao     = db.messageDao();
        socket         = SocketManager.getSocket();

        attachSocketListeners();
    }

    public ChatRepository(Application app) {
        ChatDatabase db = ChatDatabase.getInstance(app);
        messageDao      = db.messageDao();
    }

    public void ensureSocketConnected() {
        if (socket != null && !socket.connected()) {
            Log.d("SOCKET", "🔄 Reconnecting socket...");
            socket.connect();
        }
    }

    public void resendPendingMessages() {
        if (socket == null || !socket.connected()) {
            Log.d("SOCKET", "⚠️ Not connected, cannot resend");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessage> pendingMsgs = messageDao.getPendingMessages();

            for (ChatMessage msg : pendingMsgs) {
                try {
                    JSONObject json = msg.toJson();
                    socket.emit("chat:send_message", json);
                    Log.d("RESEND", "📤 Resending: " + msg.localId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void attachSocketListeners() {

        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d("SOCKET", "🟢 Connected");
            resendPendingMessages();   // 🔥 SEND QUEUED MESSAGES HERE
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d("SOCKET", "🔴 Disconnected");
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e("SOCKET", "❌ Connect error");
        });
    }


    public static synchronized ChatRepository get(Context context) {
        if (instance == null) {
            instance = new ChatRepository(context);
        }
        return instance;
    }

    public LiveData<List<ChatUser>> getUsers() {
        return users;
    }

    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return messageDao.getConversation(myId, friendId);
    }

    /**
     * Call once from ViewModel
     */
    public void start() {
        if (listening || socket == null) return;
        listening = true;

        socket.on("chat:users:list", args -> {
            Log.d("ChatRepo", "📥 chat:users:list received");

            JSONArray array = (JSONArray) args[0];
            List<ChatUser> list = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o == null) continue;

                ChatUser u = new ChatUser(
                            o.optString("nickname"),
                            o.optString("id"),
                            o.optInt("status"),
                            o.optInt("notSeenMessages")
                    );
                    list.add(u);
                }
                //filter the list. Remove the user who his id = myId
                String myId = SocketManager.getCurrentUserId();

                List<ChatUser> filtered = new ArrayList<>();

                for (ChatUser u : list) {
                    if (!u.getChatId().equals(myId)) {
                        filtered.add(u);
                    }
                }

                users.postValue(filtered);

                //users.postValue(list);
            });

        //Load conversations from backend.
        socket.on("chat:conversation_history", args -> {
            JSONArray array = (JSONArray) args[0];
            List<ChatMessage> messagesList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);

                ChatMessage msg = new ChatMessage(
                        o.optString("localId"),
                        o.optString("id_from"),
                        o.optString("id_to"),
                        o.optString("message"),
                        o.optString("image_uri"),
                        o.optString("image_url"),
                        o.optString("sent_at"),
                        o.optString("seen"),
                        o.optString("type"),
                        o.optBoolean("pending")

                );

                messagesList.add(msg);
            }

            messages.postValue(messagesList);
        });

        //Receive new message from backend.
        socket.on("chat:new_message", args -> {
            Log.d("ChatRepo", "📥 chat:new_message received");

            JSONObject data = (JSONObject) args[0];
            ChatMessage serverMsg = ChatMessage.fromJson(data);

            replaceOrAddMessage(serverMsg);

            // 🔥 SAVE TO ROOM
            Executors.newSingleThreadExecutor().execute(() ->
                    messageDao.insertMessage(serverMsg)
            );

            /*
            ChatMessage msg = new ChatMessage(
                    o.optString("localId"),
                    o.optString("id_from"),
                    o.optString("id_to"),
                    o.optString("message"),
                    o.optString("image_path"),
                    o.optString("sent_at"),
                    o.optString("seen"),
                    o.optString("type"),
                    o.optBoolean("pending")
            );

            List<ChatMessage> current = messages.getValue();
            if (current == null) current = new ArrayList<>();

            //current.add(msg);

            // 🔥 CREATE NEW LIST
            List<ChatMessage> updated = new ArrayList<>(current);
            updated.add(msg);

            messages.postValue(updated);
            */
        });

        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d("ChatRepo", "🟢 Socket connected — syncing pending messages");
            resendPendingMessages();
        });

    }

    private void resendPendingMessages_() {
        Executors.newSingleThreadExecutor().execute(() -> {

            List<ChatMessage> pendingList = messageDao.getPendingMessages();

            for (ChatMessage msg : pendingList) {
                Log.d("ChatRepo", "📤 Resending pending: " + msg.localId);

                socket.emit("chat:send_message", msg.toJson());
            }
        });
    }


    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public void replaceOrAddMessage(ChatMessage serverMsg) {

        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();

        List<ChatMessage> updated = new ArrayList<>();
        boolean replaced = false;

        for (ChatMessage msg : current) {

            if (msg.getLocalId() != null &&
                    serverMsg.getLocalId() != null &&
                    msg.getLocalId().equals(serverMsg.getLocalId())) {

                // ✅ Merge optimistic + server data
                ChatMessage merged = new ChatMessage(
                        msg.getLocalId(),
                        msg.getId_from(),
                        msg.getId_to(),
                        msg.getMessage(),
                        msg.getLocalImageUri(),   // keep instant local image
                        serverMsg.getRemoteUrl(), // cloud url
                        serverMsg.getSent_at(),   // real timestamp
                        serverMsg.getSeen(),
                        msg.getType(),
                        false                     // not pending anymore
                );

                updated.add(merged);
                replaced = true;

            } else {
                // ✅ KEEP ALL OTHER MESSAGES
                updated.add(msg);
            }
        }

        // Message from other user (no optimistic version)
        if (!replaced) {
            updated.add(serverMsg);
        }

        messages.postValue(updated);

    }



    public void joinConversation(ChatUser user) {
        if (socket == null || !socket.connected()) {
            Log.e("ChatRepo", "❌ Socket not connected");
            return;
        }

        Log.d("ChatRepo", "📤 Emitting chat:join_conversation withUserId=" + user.getChatId());

        JSONObject payload = new JSONObject();
        try {
            payload.put("withUserId", user.getChatId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("chat:join_conversation", payload);
    }

    public void sendMessage(String text) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("toUserId", users.getValue().get(0).getId());
            payload.put("message", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("chat:send_message", payload);
    }

    private void sendMessageWithImage(String message, String imagePath) {
        try {
            socket.emit("chat:send_message", new JSONObject()
                    .put("id_to", users.getValue().get(0).getId())
                    .put("message", message)
                    .put("image_path", imagePath)
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLocalMessage(ChatMessage msg) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();

        List<ChatMessage> updated = new ArrayList<>(current);
        updated.add(msg);

        messages.setValue(updated);
    }

    public void sendImageMessage(String toUserId, String imageUrl) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("to", toUserId); // store this when joinConversation runs
            obj.put("image_url", imageUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("chat:send_image", obj);
    }

    public void sendTextMessage(String text, String toUserId, String localId) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("toUserId", toUserId);
            obj.put("message", text);
            obj.put("localId", localId);
            socket.emit("chat:send_message", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendImageMessage(String text, String imageUrl, String toUserId, String localId) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("localId", localId);
            obj.put("toUserId", toUserId);
            obj.put("message", text);      // caption (can be empty)
            obj.put("image_url", imageUrl);
            socket.emit("chat:send_image", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void uploadImageAndSend(Context context, String text, Uri imageUri, String toUserId, String localId) {

        SupabaseStorageUploader.uploadImage(context, imageUri, new SupabaseStorageUploader.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                sendImageMessage(text, publicUrl, toUserId, localId);
            }

            @Override
            public void onError(Exception e) {
                Log.e("ChatRepo", "Image upload failed", e);
            }
        });
    }

}


