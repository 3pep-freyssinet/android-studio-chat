package com.google.amara.chattab.ui.main;

//import static com.google.amara.chattab.MainApplication.friendId;

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
import com.google.amara.chattab.dao.UserDao;
import com.google.amara.chattab.helper.SupabaseStorageUploader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    //friend users
    private final MutableLiveData<List<ChatUser>> friendUsers = new MutableLiveData<>();

    //currentFriendId
    private final MutableLiveData<String> currentFriendId     = new MutableLiveData<>();


    //all users
    private final MutableLiveData<List<ChatUser>> allUsers    = new MutableLiveData<>();
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());

    //private final MediatorLiveData<List<ChatMessage>> messages = new MediatorLiveData<>();

    private final MutableLiveData<Boolean> typing                = new MutableLiveData<>(false);
    private final MutableLiveData<ChatUser> selectedUserLiveData = new MutableLiveData<>();

    private final MutableLiveData<String> rejectEvents        = new MutableLiveData<>();
    private final MutableLiveData<String> acceptEvents        = new MutableLiveData<>();

    public LiveData<String> getAcceptEvents() {
        return acceptEvents;
    }
    public LiveData<String> getRejectEvents() { return rejectEvents;
    }

    public void fetchMessagesFromApi(String myId, String friendId) {
        // Build the request body with login credentials
        RequestBody requestBody = new FormBody.Builder()
                .add("myId", myId)
                .add("friendId", friendId)
                .build();

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/fetch-messages")
                .addHeader("Authorization", "Bearer " + MainApplication.JWT_TOKEN)
                .post(requestBody) //new FormBody.Builder().build())
                .build();
        OkHttpClient okHttpClient     = new OkHttpClient();//default.
        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                JSONArray array = null;
                try {
                    array = new JSONArray(response.body().string());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                List<ChatMessage> messagesList = new ArrayList<>();


                messages.postValue(messagesList); // 🔥 KEY LINE
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    //send request friendship to backend.
    public void sendFriendRequest(String fromUserId, String toUserId) {

        //default client
        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("fromUserId", Integer.parseInt(fromUserId));
            json.put("toUserId", Integer.parseInt(toUserId));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/friend-request")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "❌ Friend request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String res = response.body().string();

                Log.d("API", "✅ Friend request response: " + res);

                if (!response.isSuccessful()) {
                    Log.e("API", "❌ Error: " + response.code());
                    return;
                }

                // optional: parse response
            }
        });
    }


    //load friend-users.
    public void loadFriendUsersFromApi() {
        friendUsers.postValue(new ArrayList<>()); // ✅ clear old value first

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/load-user-friends")
                .addHeader("Authorization", "Bearer " + MainApplication.JWT_TOKEN)
                .post(new FormBody.Builder().build())
                .build();
        OkHttpClient okHttpClient     = new OkHttpClient();//default.
        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                JSONArray array;
                try {
                    array = new JSONArray(response.body().string());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                List<ChatUser> usersList = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = null;
                    try {
                        obj = array.getJSONObject(i);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    String onRelationStatus = obj.isNull("relation_status")?
                            "none" : obj.optString("relation_status");

                    ChatUser user = null;
                    try {
                        user = new ChatUser(
                                String.valueOf(obj.getInt("id")),
                                obj.getString("nickname"),
                                obj.getInt("online_status"),
                                onRelationStatus,
                                999 //dummy
                        );
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    usersList.add(user);
                }
                friendUsers.postValue(new ArrayList<>(usersList));
                Log.d("DEBUG", "List hash = " + friendUsers.hashCode());

                //users.postValue(usersList); // 🔥 KEY LINE
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void fetchMessages(String myId, String friendId) {
        fetchMessagesFromApi(myId, friendId);
    }

    public void fetchAllUsers() {
        loadAllUsersFromApi();
    }


    public void loadPendingRequests(String myId) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/friends-pending?userId=" + myId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "❌ Pending request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String res = response.body().string();

                try {
                    JSONArray array = new JSONArray(res);

                    List<ChatUser> pendingUsers = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {

                        JSONObject obj = array.getJSONObject(i);

                        String onRelationStatus = obj.isNull("relation_status")?
                                "none" : obj.optString("relation_status");

                        ChatUser user = new ChatUser(
                                String.valueOf(obj.getInt("id")),
                                obj.getString("nickname"),
                                obj.getInt("online_status"),
                                onRelationStatus,
                                999 //dummy

                        );

                        user.setRelationStatus("pending"); // ⭐ IMPORTANT

                        pendingUsers.add(user);
                    }

                    // 👉 store locally
                    insertPendingUsers(pendingUsers);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void insertPendingUsers(List<ChatUser> users) {
        Executors.newSingleThreadExecutor().execute(() -> {
            userDao.insertAll(users);
        });
    }

    private void loadAllUsersFromApi() {
        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/load-all-users")
                .addHeader("Authorization", "Bearer " + MainApplication.JWT_TOKEN)
                .post(new FormBody.Builder().build())
                .build();
        OkHttpClient okHttpClient     = new OkHttpClient();//default.
        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                JSONArray array = null;
                try {
                    array = new JSONArray(response.body().string());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                List<ChatUser> allUsersList = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = null;
                    try {
                        obj = array.getJSONObject(i);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    String onRelationStatus = obj.isNull("relation_status")?
                            "none" : obj.optString("relation_status");

                    ChatUser user = new ChatUser(
                            obj.optString("id"),
                            obj.optString("nickname"),
                            obj.optInt("online_status"),
                            onRelationStatus,


                            99 //obj.optInt("friend_id") // ✅ temporary chatId
                    );

                    allUsersList.add(user);
                }

                allUsers.postValue(allUsersList); // 🔥 KEY LINE
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public LiveData<List<ChatUser>> getAllUsers() {
        return allUsers;
    }

    public LiveData<List<ChatUser>> getAllFriendUsers() {
        return userDao.getFriendUsers();
    }


    public void addFriend(ChatUser user) {
        List<ChatUser> current = friendUsers.getValue();

        if (current == null) current = new ArrayList<>();

        for (ChatUser u : current) {
            if (u.getUserId().equals(user.getUserId())) {
                return; // already exists
            }
        }

        current.add(user);
        friendUsers.setValue(current);
    }

    public void acceptFriend(String myId, String friendId) {

        //default client
        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("fromUserId", Integer.parseInt(friendId)); // Fanny
            json.put("toUserId", Integer.parseInt(myId));       // Alice
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/users/friend-accept")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("API", "❌ Accept failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Log.d("API", "✅ Friend accepted");

                // 🔥 Update local DB
                Executors.newSingleThreadExecutor().execute(() -> {
                    userDao.updateRelationStatus(friendId, "accepted");
                });
            }
        });
    }
//
public void rejectFriend(String myId, String friendId) {

    OkHttpClient client = new OkHttpClient();

    JSONObject json = new JSONObject();
    try {
        json.put("fromUserId", Integer.parseInt(myId));     // Alice
        json.put("toUserId", Integer.parseInt(friendId));   // Fanny
    } catch (JSONException e) {
        e.printStackTrace();
        return;
    }

    RequestBody body = RequestBody.create(
            json.toString(),
            MediaType.parse("application/json; charset=utf-8")
    );

    Request request = new Request.Builder()
            .url("https://android-chat-server.onrender.com/users/friend-reject")
            .post(body)
            .build();

    client.newCall(request).enqueue(new Callback() {

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e("API", "❌ Reject failed", e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {

            Log.d("API", "🚫 Friend rejected");

            // 🔥 Remove locally
            Executors.newSingleThreadExecutor().execute(() -> {
                userDao.deleteById(friendId);
                // optional:
                // messageDao.deleteConversation(myId, friendId);
            });
        }
    });
}

    public interface UserStatusListener {
        void onUserStatusChanged(ChatUser user);
    }

    private UserStatusListener listener;

    public LiveData<Boolean> getTyping() {
        return typing;
    }

    private LiveData<List<ChatMessage>> currentSource;

    //private List<ChatMessage> dbMessages = new ArrayList<>();

    private boolean isTyping = false;
    //private Handler typingHandler = new Handler(Looper.getMainLooper());

    private final Handler typingHandler = new Handler(Looper.getMainLooper());

    private final Runnable typingTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("TYPING", "typing timeout → stop");
            Log.e("TYPING_TRACE", "typing FALSE");
            typing.postValue(false);
        }
    };

    private Runnable stopTypingRunnable;
    //private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingTimeout;
    private final Set<String> onlineUsersSnapshot = new HashSet<>();

    private boolean   listening = false;
    public MessageDao messageDao;
    public UserDao    userDao;
    private Context   appContext;
    private final String VIEW_TYPE_TYPING = "3";
    private long lastTypingSentAt = 0;
    private volatile long lastUserInputAt = 0;

    //pagination
    private static final int PAGE_SIZE = 20;
    private int loadedMessages = 0;

    private ChatRepository(Socket socket) {
        this.socket = socket;
        //attachConnectListener();
    }

    private ChatRepository() {
        this.socket = SocketManager.getSocket();
    }

    //constructor
    private ChatRepository(Context context) {

        Log.d("REPO", "Repository instance = " + this);

        appContext = context;
        AppDatabase db = AppDatabase.getInstance(context);
        messageDao     = db.messageDao();
        userDao        = db.userDao();
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

    public void resetTyping() {
        typing.setValue(false);
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



    public void notifyTyping() {

        String toUserId = currentFriendId.getValue();

        if (!isTyping) {
            isTyping = true;
            socket.emit("typing:start", createTypingPayload(toUserId));
            Log.d("TYPING_SEND", "typing:start SENT");
        }

        if (stopTypingRunnable != null) {
            typingHandler.removeCallbacks(stopTypingRunnable);
        }

        stopTypingRunnable = () -> {
            isTyping = false;
            socket.emit("typing:stop", createTypingPayload(toUserId));
            Log.d("TYPING_SEND", "typing:stop SENT");
        };

        typingHandler.postDelayed(stopTypingRunnable, 1500);
    }



    public void markUserInput() {
        lastUserInputAt = System.currentTimeMillis();
    }

    private boolean isUserActivelyTyping() {
        return System.currentTimeMillis() - lastUserInputAt < 800;
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

    public void setUserStatusListener(UserStatusListener listener) {
        this.listener = listener;
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

    public LiveData<List<ChatUser>> getFriendUsers() {
        return userDao.getFriendUsers();
    }


    /*
    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return messageDao.getConversation(myId, friendId);
    }
    */


    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return messageDao.getConversation(myId , friendId);
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

        //if (dbMessages.isEmpty()) {
        //    Log.d("MERGE", "Skip empty emission");
        //    return; // ⭐ CRITICAL FIX
        //}

        //if (dbMessages != null) {
        //    merged.addAll(dbMessages);
        //}

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
                String onRelationStatus = o.isNull("relation_status")?
                        "none" : o.optString("relation_status");
                ChatUser u = new ChatUser(
                        o.optString("id"),
                        o.optString("nickname"),
                        o.optInt("online_status"),
                        onRelationStatus,
                        99 //o.optInt("notSeenMessages")
                );
                list.add(u);
            }
            //filter the list. Remove the user who his id = myId
            String myId = SocketManager.getCurrentUserId();

            List<ChatUser> filtered = new ArrayList<>();

            for (ChatUser u : list) {
                if (!u.getUserId().equals(myId)) {
                    filtered.add(u);
                }
            }

            friendUsers.postValue(filtered);

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
        /*
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
        */

        socket.on("chat:new_message", args -> {

            isTyping = false;

            JSONObject data       = (JSONObject) args[0];
            ChatMessage serverMsg = ChatMessage.fromJson(data);

            String fromId = serverMsg.id_from;
            boolean isMe  = fromId.equals(MainApplication.myId);

            if (!isMe) {

                // ✅ 1. Set current chat user id
                //currentFriendId.postValue(fromId);

                // ✅ 2. Notify UI to open chat
                //chatNavigationLiveData.postValue(fromId);

                // ✅ 3. Ensure user appears in list (temporary)
                ensureUserExists(fromId, "unknown");
            }

            // 🔥 ONLY write to DB
            Executors.newSingleThreadExecutor().execute(() ->
                    messageDao.insertMessage(serverMsg)
            );

            // ✅ keep this logic (it's correct)
            if (serverMsg.id_to.equals(MainApplication.myId)
                    && serverMsg.id_from.equals(MainApplication.currentChatUserId)) {

                JSONObject payload = new JSONObject();

                try {
                    payload.put("fromUserId", serverMsg.id_from);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                socket.emit("chat:mark_seen", payload);
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

                    user.userId         = String.valueOf(obj.getInt("id"));
                    user.nickname       = obj.getString("nickname");
                    user.onlineStatus   = obj.optInt("online_status", 0);
                    user.relationStatus = obj.isNull("relation_status")?
                                         "none" : obj.optString("relation_status");;

                    user.connectedAt    = obj.getString("connected_at");
                    user.lastConnectedAt       = obj.getString("last_seen_at");
                    user.notSeenMessagesNumber = obj.getInt("unread_count");

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                users_.add(user);
            }

            friendUsers.postValue(users_);

            //Log.d("ChatRepo", "📥 chat:users_with_unread : users_ : " + users_.get(0).getNickname());

            //Log.d("ChatRepo", "📥 chat:users_with_unread : users : " + users.getValue().get(0).getNickname());
            //Log.d("ChatRepo", "📥 chat:users_with_unread : users_ : " + users_.get(0).getNotSeenMessagesNumber());
        });


        socket.on("user_status", args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String userId = data.getString("userId");
                String statusStr = data.getString("status");

                int status;
                if (statusStr.equals("offline")) {
                    status = 0;
                } else if (statusStr.equals("online")) {
                    status = 1;
                } else {
                    status = 2;
                }

                Log.d("ChatRepo", "user_status: " + userId + " -> " + status);

                List<ChatUser> current = friendUsers.getValue();
                if (current == null) return;

                for (ChatUser user : current) {

                    if (user.getUserId().equals(userId)) {

                        user.setOnlineStatus(status);

                        // 🔥 Update selected user if needed
                        //ChatUser selected = selectedUserLiveData.getValue();

                        //if (selected != null && selected.getChatId().equals(userId)) {
                        //    selected.setStatus(status);

                            // 🔥 Notify ViewModel
                            if (listener != null) {
                                listener.onUserStatusChanged(user);
                            }
                        //}

                        break;
                    }
                }

                friendUsers.postValue(new ArrayList<>(current));

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


        /*
        socket.on("typing:start", args -> {
            Log.d("TYPING", "typing:start received");

            //if (Boolean.TRUE.equals(typing.getValue())) return; // ignore duplicate

            if (!Boolean.TRUE.equals(typing.getValue())) {
                typing.postValue(true);
            }
            //typing.postValue(true);

            // ✅ cancel ONLY this runnable
            typingHandler.removeCallbacks(typingTimeoutRunnable);

            // ✅ schedule again
            typingHandler.postDelayed(typingTimeoutRunnable, 5000);
        });
        */

        socket.on("typing:start", args -> {
            try {
                JSONObject data = (JSONObject) args[0];

                String fromUserId = data.optString("from");
                String toUserId   = data.optString("to");

                String myId = SocketManager.getUserId();
                String currentChatUserId = MainApplication.currentChatUserId;

                Log.e("TYPING_TRACE", "typing:start payload=" + data.toString());

                // ✅ ONLY accept if:
                // 1. NOT from me
                // 2. AND from the user I'm currently chatting with

                if (fromUserId == null ||
                        fromUserId.equals(myId) ||
                        !fromUserId.equals(currentChatUserId)) {

                    Log.e("TYPING_TRACE", "IGNORED typing event");
                    return;
                }

                Log.e("TYPING_TRACE", "ACCEPTED typing event");

                typing.postValue(true);

                typingHandler.removeCallbacks(typingTimeoutRunnable);
                typingHandler.postDelayed(typingTimeoutRunnable, 5000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        /*
        socket.on("typing:stop", args -> {
            Log.d("TYPING", "typing:stop received");

            typingHandler.removeCallbacks(typingTimeoutRunnable);
            typing.postValue(false);
        });
        */

        socket.on("typing:stop", args -> {
            try {
                JSONObject data = (JSONObject) args[0];

                String fromUserId = data.optString("from");
                String myId = SocketManager.getUserId();
                String currentChatUserId = MainApplication.currentChatUserId;

                if (fromUserId == null ||
                        fromUserId.equals(myId) ||
                        !fromUserId.equals(currentChatUserId)) {

                    return;
                }

                typingHandler.removeCallbacks(typingTimeoutRunnable);
                typing.postValue(false);

            } catch (Exception e) {
                e.printStackTrace();
            }
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

        socket.on("friend:request_received", args -> {

            JSONObject data = (JSONObject) args[0];
            String fromId   = data.optString("fromUserId");
            String nickname = data.optString("nickname", "Unknown");

            Log.d("SOCKET", "📩 Friend request from " + fromId + " nickname=" + nickname);

            // 🔥 Add user locally
            ensureUserExists(fromId, nickname);

            // 🔥 mark as pending
            Executors.newSingleThreadExecutor().execute(() -> {
                userDao.insertOrUpdatePending(fromId);
            });
        });

        socket.on("friend:request_rejected", args -> {
            JSONObject data = (JSONObject) args[0];
            String fromId   = data.optString("fromUserId");

            Log.d("SOCKET", "❌ Rejected by " + fromId);

            Executors.newSingleThreadExecutor().execute(() -> {
                userDao.setRejected(fromId);
            });

            // 🔥 emit event (Repository level)
            rejectEvents.postValue(fromId);
        });

        socket.on("friend:request_accepted", args -> {

            JSONObject data = (JSONObject) args[0];

            String fromId   = data.optString("fromUserId");   // Alice
            String nickname = data.optString("nickname", "Unknown");

            Log.d("SOCKET", "✅ Accepted by " + fromId);

            Executors.newSingleThreadExecutor().execute(() -> {

                ChatUser existing = userDao.getUserById(fromId);

                if (existing == null) {

                    // 🔥 CREATE USER (THIS WAS MISSING)
                    ChatUser newUser = new ChatUser(
                            fromId,
                            nickname,
                            1,
                            "accepted",
                            99
                    );
                    Log.d("SOCKET", "✅ Accepted : insert new user " + newUser);
                    userDao.insert(newUser);
                } else {
                    Log.d("SOCKET", "✅ Accepted : update user. fromId =  " + fromId);
                    // 🔥 UPDATE USER
                    userDao.setAccepted(fromId);

                }
            });
            // 🔥 MOVE THIS HERE (MAIN THREAD)
            acceptEvents.postValue(fromId);
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

    private void ensureUserExists(String userId, String nickname) {

        Executors.newSingleThreadExecutor().execute(() -> {

            ChatUser existing = userDao.getUserById(userId);

            if (existing == null) {
                ChatUser newUser = new ChatUser(
                        userId,
                        nickname,   // or fetch nickname later
                        0,
                        "pending",
                        99 //dummy
                );

                if (userDao.exists(userId) == 0) {
                    userDao.insert(newUser);
                }
            }
        });
    }

    public LiveData<ChatUser> getUserById(String userId) {
        return userDao.getUserByIdLive(userId);
    }

    private void applyPresence() {

        List<ChatUser> current = friendUsers.getValue();
        if (current == null || current.isEmpty()) return;

        for (ChatUser u : current) {

            if (onlineUsersSnapshot.contains(u.getUserId())) {
                u.setOnlineStatus(1);
            } else {
                u.setOnlineStatus(0);
            }

        }

        friendUsers.postValue(new ArrayList<>(current));
    }

    private void updateUserBadge(String userId, String status) {
    }

    public void incrementUnreadCounter(String fromUserId) {

        List<ChatUser> currentUsers = friendUsers.getValue();
        if (currentUsers == null) return;

        List<ChatUser> updatedList = new ArrayList<>();

        for (ChatUser user : currentUsers) {

            if (fromUserId.equals(user.userId)) {
                user.notSeenMessagesNumber += 1;
            }

            updatedList.add(user);
        }

        friendUsers.postValue(updatedList);  // 🔥 new list instance
        Log.d("ChatRepo", "🟢 nickname = " + friendUsers.getValue().get(0).getNickname());
        Log.d("ChatRepo", "🟢 not seen messages = " + friendUsers.getValue().get(0).getNotSeenMessagesNumber());
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

        Log.d("ChatRepo", "📤 Emitting chat:join_conversation withUserId=" + user.getUserId());

        JSONObject payload = new JSONObject();
        try {
            payload.put("withUserId", user.getUserId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("chat:join_conversation", payload);
    }

    public void sendMessage(String text) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("toUserId", friendUsers.getValue().get(0).getUserId());
            payload.put("message", text);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("chat:send_message", payload);
    }

    private void sendMessageWithImage(String message, String imagePath) {
        try {
            socket.emit("chat:send_message", new JSONObject()
                    .put("id_to", friendUsers.getValue().get(0).getUserId())
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

    public void loadInitialMessages() {
        loadedMessages = 0;

        String myId     = MainApplication.myId;
        String friendId = currentFriendId.getValue();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessage> batch = messageDao.getMessagesBatch(myId, friendId, PAGE_SIZE, loadedMessages);
            Collections.reverse(batch);

            loadedMessages += batch.size();

            // Update MediatorLiveData
            new Handler(Looper.getMainLooper()).post(() -> messages.setValue(batch));
        });
    }

    public LiveData<List<ChatMessage>> loadMessagesBetweenMeAndOther(String me, String other) {

        return messageDao.getMessagesBetween(me, other);
    }

    public void loadMoreMessages() {
        String myId = MainApplication.myId;
        String friendId = currentFriendId.getValue();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessage> batch = messageDao.getMessagesBatch(myId, friendId, PAGE_SIZE, loadedMessages);
            Collections.reverse(batch); // oldest first

            if (!batch.isEmpty()) {
                loadedMessages += batch.size();

                // Merge batch with existing messages
                List<ChatMessage> current = messages.getValue() != null ? messages.getValue() : new ArrayList<>();
                current.addAll(0, batch); // prepend older messages
                new Handler(Looper.getMainLooper()).post(() -> messages.setValue(current));
            }
        });
    }

    public void generateFakeMessages(String myId, String friendId) {

        Executors.newSingleThreadExecutor().execute(() -> {

            List<ChatMessage> fakeMessages = new ArrayList<>();

            for (int i = 1; i <= 100; i++) {
                ChatMessage msg = new ChatMessage();
                msg.localId = UUID.randomUUID().toString();
                msg.id_from = (i % 2 == 0) ? myId : friendId;
                msg.id_to = (i % 2 == 0) ? friendId : myId;
                msg.message = "Fake message #" + i;
                msg.sent_at = Instant.now()
                        .minusSeconds(60L * (100 - i))
                        .toString();
                msg.status = "seen";
                msg.type = "text";
                msg.pending = true;
                msg.isTyping = true;

                fakeMessages.add(msg);
            }

            messageDao.insertMessages(fakeMessages);

            Log.d("FAKE_MSG", "Inserted into DB: " + fakeMessages.size());
        });
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


