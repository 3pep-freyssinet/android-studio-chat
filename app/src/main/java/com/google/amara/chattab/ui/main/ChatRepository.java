package com.google.amara.chattab.ui.main;

import static com.google.amara.chattab.MainApplication.friendId;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.MainApplication;
import com.google.amara.chattab.SocketManager;
import com.google.amara.chattab.dao.AppDatabase;
import com.google.amara.chattab.dao.MessageDao;
import com.google.amara.chattab.helper.SupabaseStorageUploader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    MediatorLiveData<List<ChatUser>> usersWithUnread          = new MediatorLiveData<>();
    
    private final MutableLiveData<List<ChatUser>> users       = new MutableLiveData<>();

    //private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());

    private final MediatorLiveData<List<ChatMessage>> messages = new MediatorLiveData<>();

    private final MutableLiveData<Boolean> typing              = new MutableLiveData<>(false);

    public LiveData<Boolean> getTyping() {
        return typing;
    }

    private LiveData<List<ChatMessage>> currentSource;

    private List<ChatMessage> dbMessages = new ArrayList<>();
    private boolean isTyping = false;
    //private Handler typingHandler = new Handler(Looper.getMainLooper());

    private final Handler typingHandler = new Handler(Looper.getMainLooper());

    private final Runnable typingTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("TYPING", "typing timeout → stop");
            typing.postValue(false);
        }
    };

    private Runnable stopTypingRunnable;
    //private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingTimeout;
    private final Set<String> onlineUsersSnapshot = new HashSet<>();

    private boolean listening = false;
    public MessageDao messageDao;
    private Context appContext;
    private final String VIEW_TYPE_TYPING = "3";


    private ChatRepository(Socket socket) {
        this.socket = socket;
        //attachConnectListener();
    }

    private ChatRepository() {
        this.socket = SocketManager.getSocket();
    }

    //constructor
    private ChatRepository(Context context) {
        appContext = context;
        AppDatabase db = AppDatabase.getInstance(context);
        messageDao     = db.messageDao();
        socket         = SocketManager.getSocket();

        attachSocketListeners();
    }

    //constructor
    public ChatRepository(Application app) {
        AppDatabase db  = AppDatabase.getInstance(app);
        messageDao      = db.messageDao();
    }

    //👉 That method is now obsolete ❌
    private void addTypingMessage() {

        Log.d("TYPING", "addTypingMessage called");

        List<ChatMessage> current = messages.getValue();
        if (current == null) current = new ArrayList<>();

        for (ChatMessage m : current) {
            if (m.isTyping()) return;
        }

        List<ChatMessage> updated = new ArrayList<>(current);

        ChatMessage typingMsg = new ChatMessage();
        typingMsg.setTyping(true);

        updated.add(typingMsg);

        Log.d("TYPING", "posting typing message, size=" + updated.size());

        messages.postValue(updated);
    }

    private void removeTypingMessage() {

        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        List<ChatMessage> updated = new ArrayList<>(current);

        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).isTyping()) {
                updated.remove(i);
                break;
            }
        }

        messages.postValue(updated);
    }

    public void notifyTyping(String toUserId) {

        if (!isTyping) {
            isTyping = true;

            socket.emit("typing:start", createTypingPayload(toUserId));
        }

        if (stopTypingRunnable != null) {
            typingHandler.removeCallbacks(stopTypingRunnable);
        }

        stopTypingRunnable = () -> {
            isTyping = false;

            socket.emit("typing:stop", createTypingPayload(toUserId));
        };

        typingHandler.postDelayed(stopTypingRunnable, 1500);
        typingHandler.postDelayed(() -> removeTypingMessage(), 3000);
    }

    public JSONObject createTypingPayload(String toUserId) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("to", toUserId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
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
                    Log.d("SOCKET", "Emit socket id: " + socket.id());

                    if ("image".equals(msg.getType())) continue; // handled by upload worker

                    if (msg.getRemoteUrl() == null || msg.getRemoteUrl().isEmpty()) {
                        // 📝 TEXT MESSAGE
                        socket.emit("chat:send_message", json);
                        Log.d("RESEND", "📤 Resending TEXT: " + msg.localId);
                    } else {
                        // 🖼 IMAGE MESSAGE
                        socket.emit("chat:send_image", json);
                        Log.d("RESEND", "📤 Resending IMAGE: " + msg.localId);
                    }

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
            processPendingImageUploads(); 
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d("SOCKET", "🔴 Disconnected");
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e("SOCKET", "❌ Connect error");
        });
    }

    private void processPendingImageUploads() {
        Executors.newSingleThreadExecutor().execute(() -> {

            List<ChatMessage> pendingImages = messageDao.getPendingImageUploads();

            if (pendingImages.isEmpty()) {
                Log.d("UPLOAD", "No pending image uploads");
                return;
            }

            Log.d("UPLOAD", "Found " + pendingImages.size() + " pending image(s)");

            for (ChatMessage msg : pendingImages) {

                if (msg.getLocalImageUri() == null) continue;

                Uri localUri = Uri.parse(msg.getLocalImageUri());

                Log.d("UPLOAD", "Uploading offline image: " + msg.getLocalId());

                SupabaseStorageUploader.uploadImage(appContext, localUri, new SupabaseStorageUploader.UploadCallback() {

                    @Override
                    public void onSuccess(String publicUrl) {

                        Executors.newSingleThreadExecutor().execute(() -> {
                            messageDao.updateRemoteUrl(msg.getLocalId(), publicUrl);
                        });

                        Log.d("UPLOAD", "Upload success: " + msg.getLocalId());

                        sendImageMessage(
                                msg.getMessage(),
                                publicUrl,
                                msg.getId_to(),
                                msg.getLocalId()
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("UPLOAD", "Upload failed again: " + msg.getLocalId(), e);
                    }
                });
            }
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

    /*
    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return messageDao.getConversation(myId, friendId);
    }
    */


    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return messageDao.getConversation(myId, friendId);
    }


    /*
    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {

        if (currentSource != null) {
            messages.removeSource(currentSource);
        }

        currentSource = messageDao.getConversation(myId, friendId);

        messages.addSource(currentSource, list -> {
            Log.d("TYPING", "Room emitted size=" + (list == null ? 0 : list.size()));

            dbMessages = list != null ? list : new ArrayList<>();
            mergeAndPost();
        });

        return messages;
    }
    */

    private void mergeAndPost() {

        List<ChatMessage> merged = new ArrayList<>();

        if (dbMessages.isEmpty()) {
            Log.d("MERGE", "Skip empty emission");
            return; // ⭐ CRITICAL FIX
        }

        if (dbMessages != null) {
            merged.addAll(dbMessages);
        }

        /*
        if (isTyping) {
            ChatMessage typingMsg = new ChatMessage();
            typingMsg.setTyping(true);
            typingMsg.setType(VIEW_TYPE_TYPING);
            typingMsg.setLocalId("typing_" + friendId);
            merged.add(typingMsg);
        }
        */

        Log.d("TYPING", "merged size=" + merged.size());

        //messages.postValue(merged);
        //messages.setValue(merged);
        new Handler(Looper.getMainLooper()).post(() -> {
            messages.setValue(merged);
        });
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

                applyPresence();
            });

        //Load conversations from backend.
        socket.on("chat:conversation_history", args -> {
            JSONArray array = (JSONArray) args[0];
            List<ChatMessage> messagesList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);

                ChatMessage msg = new ChatMessage(
                        o.optInt("serverId"),
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
            isTyping = false;
            mergeAndPost();

            JSONObject data = (JSONObject) args[0];
            ChatMessage serverMsg = ChatMessage.fromJson(data);

            replaceOrAddMessage(serverMsg);

            if (serverMsg.id_to.equals(MainApplication.myId)
                    && serverMsg.id_from.equals(MainApplication.currentChatUserId)) {

                JSONObject payload = new JSONObject();

                try {
                    payload.put("fromUserId", serverMsg.id_from);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                Log.d("ChatRepo","myId="+MainApplication.myId);
                Log.d("ChatRepo","msg_to="+serverMsg.id_to);
                Log.d("ChatRepo","msg_from="+serverMsg.id_from);
                Log.d("ChatRepo","currentChatUserId="+MainApplication.currentChatUserId);

                socket.emit("chat:mark_seen", payload);

                Log.d("ChatRepo","payload="+payload);
            }
        });

        socket.on("chat:delivered", args -> {
            JSONObject obj = (JSONObject) args[0];
            String localId = obj.optString("localId");

            updateMessageStatus(localId, ChatMessage.STATUS_DELIVERED);
        });

        socket.on("chat:seen", args -> {
            JSONObject obj  = (JSONObject) args[0];
            String friendId = obj.optString("fromUserId");

            //markConversationSeen(fromUserId);

            String myId = SocketManager.getUserId();

            // 1️⃣ Update locally (only messages FROM friend TO me)
            List<ChatMessage> current = messages.getValue();
            if (current == null) return;

            List<ChatMessage> updated = new ArrayList<>();

            for (ChatMessage msg : current) {
                if (msg.getId_from().equals(myId) &&
                        msg.getId_to().equals(friendId) &&
                        !ChatMessage.STATUS_SEEN.equals(msg.getStatus())) {

                    msg.setStatus(ChatMessage.STATUS_SEEN);

                    Executors.newSingleThreadExecutor().execute(() ->
                            messageDao.updateStatus(msg.getLocalId(), ChatMessage.STATUS_SEEN)
                    );
                }

                ChatMessage updatedMsg = new ChatMessage(
                        msg.getServerId(),
                        msg.getLocalId(),
                        msg.getId_from(),
                        msg.getId_to(),
                        msg.getMessage(),
                        msg.getLocalImageUri(),
                        msg.getRemoteUrl(),
                        msg.getSent_at(),
                        ChatMessage.STATUS_SEEN,
                        msg.getType(),
                        false
                );
                updated.add(updatedMsg);
                //updated.add(msg);
            }
            messages.postValue(updated);
        });

        socket.on("chat:message_status_update", args -> {

            JSONObject data = (JSONObject) args[0];

            //int serverId  = data.optInt("serverId");
            int serverId = data.has("serverId")
                    ? data.optInt("serverId")
                    : data.optInt("id");

            String status = data.optString("status");

            Executors.newSingleThreadExecutor().execute(() -> {
                messageDao.updateStatusByServerId(serverId, status);
            });

            // 🔥 ALSO UPDATE MEMORY
            List<ChatMessage> current = messages.getValue();
            if (current == null) return;

            List<ChatMessage> updated = new ArrayList<>();

            for (ChatMessage msg : current) {
                if (String.valueOf(msg.getServerId()).equals(String.valueOf(serverId))) {
                    msg.setStatus(status);
                    msg.setPending(false);
                }
                updated.add(msg);
            }

            messages.postValue(updated);
        });

        socket.on("chat:users_with_unread", args -> {

            JSONArray array = (JSONArray) args[0];

            List<ChatUser> users_ = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {

                JSONObject obj   = null;
                ChatUser user    = new ChatUser();
                try {
                    obj = array.getJSONObject(i);

                    user.chatId      = String.valueOf(obj.getInt("id"));
                    user.nickname    = obj.getString("nickname");
                    user.status      = obj.optInt("status", 0);
                    user.connectedAt = obj.getString("connected_at");
                    user.lastConnectedAt       = obj.getString("last_seen_at");
                    user.notSeenMessagesNumber = obj.getInt("unread_count");

                } catch (JSONException e) {
                        throw new RuntimeException(e);
                 }

                users_.add(user);
            }

            users.postValue(users_);

            Log.d("ChatRepo", "📥 chat:users_with_unread : users_ : " + users_.get(0).getNickname());

            //Log.d("ChatRepo", "📥 chat:users_with_unread : users : " + users.getValue().get(0).getNickname());
            Log.d("ChatRepo", "📥 chat:users_with_unread : users_ : " + users_.get(0).getNotSeenMessagesNumber());
        });


        socket.on("user_status", args -> {
            int status;
            try {
                JSONObject data = (JSONObject) args[0];
                String userId   = data.getString("userId");
                String status_  = data.getString("status");

                if(status_.equals("offline")){
                    status = 0;
                }else{
                    if(status_.equals("online")){
                        status = 1;
                    }else{
                        status = 2;//standby
                    }
                }

                Log.d("ChatRepo", "user_status: " + userId + " -> " + status);

                List<ChatUser> current = users.getValue();
                if (current == null) return;
                for (ChatUser u : current) {
                    if (u.getChatId().equals(userId)) {
                        u.setStatus(status);
                        break;
                    }
                }
                users.postValue(new ArrayList<>(current));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        socket.on("online_users", args -> {

            JSONArray arr = (JSONArray) args[0];

            onlineUsersSnapshot.clear();

            for (int i = 0; i < arr.length(); i++) {
                onlineUsersSnapshot.add(String.valueOf(arr.optInt(i)));
            }

            Log.d("SOCKET", "snapshot stored: " + onlineUsersSnapshot);

            applyPresence();

        });

        //socket.on("typing:start", args -> {
        //    runOnUiThread(() -> adapter.showTyping());
        //});


        socket.on("typing:start", args -> {
            Log.d("TYPING", "typing:start received");

            //if (Boolean.TRUE.equals(typing.getValue())) return; // ignore duplicate

            typing.postValue(true);

            // ✅ cancel ONLY this runnable
            typingHandler.removeCallbacks(typingTimeoutRunnable);

            // ✅ schedule again
            typingHandler.postDelayed(typingTimeoutRunnable, 5000);
        });


        socket.on("typing:stop", args -> {
            Log.d("TYPING", "typing:stop received");

            typingHandler.removeCallbacks(typingTimeoutRunnable);
            typing.postValue(false);
        });


        socket.on("chat:conversation", args -> {

            JSONArray array = (JSONArray) args[0];
            ArrayList<ChatMessage> arrayList = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                arrayList.add(ChatMessage.fromJson(obj));
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                messageDao.insertAll(arrayList); // 🔥 THIS FEEDS ROOM
            });
        });

        /*
        socket.on("typing:stop", args -> {
            runOnUiThread(() -> adapter.hideTyping());
        });
        */


        /*
        socket.on("typing:stop", args -> {

            List<ChatMessage> current = messages.getValue();
            if (current == null) return;

            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).isTyping()) {
                    current.remove(i);
                    break;
                }
            }

            messages.postValue(current);
        });
        */

        /*
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d("ChatRepo", "🟢 Socket connected — syncing pending messages");
            resendPendingMessages();
        });
        */

    }

    private void applyPresence() {

        List<ChatUser> current = users.getValue();
        if (current == null || current.isEmpty()) return;

        for (ChatUser u : current) {

            if (onlineUsersSnapshot.contains(u.getChatId())) {
                u.setStatus(1);
            } else {
                u.setStatus(0);
            }

        }

        users.postValue(new ArrayList<>(current));
    }

    private void updateUserBadge(String userId, String status) {
    }

    public void incrementUnreadCounter(String fromUserId) {

        List<ChatUser> currentUsers = users.getValue();
        if (currentUsers == null) return;

        List<ChatUser> updatedList = new ArrayList<>();

        for (ChatUser user : currentUsers) {

            if (fromUserId.equals(user.chatId)) {
                user.notSeenMessagesNumber += 1;
            }

            updatedList.add(user);
        }

        users.postValue(updatedList);  // 🔥 new list instance
        Log.d("ChatRepo", "🟢 nickname = " +users.getValue().get(0).getNickname());
        Log.d("ChatRepo", "🟢 not seen messages = " +users.getValue().get(0).getNotSeenMessagesNumber());
    }



    public void markConversationSeen(String friendId) {

        String myId = SocketManager.getUserId();

        // 1️⃣ Update locally (only messages FROM friend TO me)
        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        List<ChatMessage> updated = new ArrayList<>();

        for (ChatMessage msg : current) {
            if (msg.getId_from().equals(friendId) &&
                    msg.getId_to().equals(myId) &&
                    !ChatMessage.STATUS_SEEN.equals(msg.getStatus())) {

                msg.setStatus(ChatMessage.STATUS_SEEN);

                Executors.newSingleThreadExecutor().execute(() ->
                        messageDao.updateStatus(msg.getLocalId(), ChatMessage.STATUS_SEEN)
                );
            }

            updated.add(msg);
        }

        messages.postValue(updated);

        // 2️⃣ Notify backend
        if (socket != null && socket.connected()) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("withUserId", friendId);
                socket.emit("chat:mark_seen", obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateMessageStatus(String localId, String newStatus) {
        List<ChatMessage> current = messages.getValue();
        if (current == null) return;

        List<ChatMessage> updated = new ArrayList<>();

        for (ChatMessage msg : current) {
            if (localId.equals(msg.getLocalId())) {
                msg.setStatus(newStatus);
            }
            updated.add(msg);
        }

        messages.postValue(updated);

        Executors.newSingleThreadExecutor().execute(() ->
                messageDao.updateStatus(localId, newStatus)
        );
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
                        serverMsg.getServerId(), //msg.getServerId(),
                        msg.getLocalId(),
                        msg.getId_from(),
                        msg.getId_to(),
                        msg.getMessage(),
                        msg.getLocalImageUri(),   // keep instant local image
                        serverMsg.getRemoteUrl(), // cloud url
                        serverMsg.getSent_at(),   // real timestamp
                        ChatMessage.STATUS_SENT,  //serverMsg.getSeen(),
                        msg.getType(),
                        false                     // not pending anymore
                );

                updated.add(merged);
                replaced = true;

                // 🔥 SAVE TO ROOM
                Executors.newSingleThreadExecutor().execute(() ->
                                messageDao.insertMessage(merged));

            } else {
                // ✅ KEEP ALL OTHER MESSAGES
                updated.add(msg);
            }
        }

        // Message from other user (no optimistic version)
        if (!replaced) {

            if (serverMsg.getLocalId() == null) {
                serverMsg.setLocalId(UUID.randomUUID().toString());
            }

            updated.add(serverMsg);

            Executors.newSingleThreadExecutor().execute(() ->
                    messageDao.insertMessage(serverMsg)
            );
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
                //sendImageMessage(text, publicUrl, toUserId, localId);

                messageDao.updateRemoteUrl(localId, publicUrl);

                // Now emit to server
                sendImageMessage(text, publicUrl, toUserId, localId);

            }

            @Override
            public void onError(Exception e) {
                Log.e("ChatRepo", "Image upload failed", e);
            }
        });
    }

    public void fetchConversationFromServer(String myId, String friendId) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("withUserId", friendId);

            socket.emit("chat:get_conversation", obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //no longer used
    /*
    public void resetUnreadCounter(String friendId) {

        List<ChatUser> currentUsers = users.getValue();
        if (currentUsers == null) return;

        List<ChatUser> updatedList = new ArrayList<>();

        for (ChatUser user : currentUsers) {

            if (friendId.equals(user.chatId)) {
                user.notSeenMessagesNumber = 0;
            }

            updatedList.add(user);
        }

        users.setValue(updatedList);   // 🔥 NEW LIST INSTANCE
    }
    */
}


