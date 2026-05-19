package com.google.amara.chattab.ui.main;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

//import com.example.aymen.androidchat.ChatUser;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.MainApplication;
import com.google.amara.chattab.SocketManager;
import com.google.amara.chattab.dao.AppDatabase;
import com.google.amara.chattab.dao.UserUiStateDao;
import com.google.amara.chattab.entities.UserUiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ChatViewModel extends AndroidViewModel {

    public  ChatRepository repo = null;

    //pagination
    private static final int PAGE_SIZE = 20;
    private int loadedMessages         = 0;
    private String pendingMessageId;

    private final MutableLiveData<List<ChatUser>> usersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ChatUser>> allUsers      = new MutableLiveData<>();
    private final MutableLiveData<String>     selectedUserId    = new MutableLiveData<>();
    private final MutableLiveData<ChatUser>     selectedUser    = new MutableLiveData<>();
    private final MutableLiveData<String> currentFriendId       = new MutableLiveData<>();
    private final MutableLiveData<Map<Long, UserUiState>> userStates = new MutableLiveData<>(new HashMap<>());

    private LiveData<Map<String, UserUiState>> userStateMap;


    //constructor
    /*
    public ChatViewModel(@NonNull Application application) {
        super(application);
        repo = ChatRepository.get(application);
    }
    */

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repo = ChatRepository.get(application);

        UserUiStateDao dao = AppDatabase
                .getInstance(application)
                .userUiStateDao();

        userStateMap = Transformations.map(
                dao.getAllStates(),
                list -> {
                    Map<String, UserUiState> map = new HashMap<>();
                    for (UserUiState s : list) {
                        map.put(s.userId, s);
                    }
                    return map;
                }
        );
    }

    public LiveData<List<ChatUser>> getAllFriendUsers() {
        return repo.getAllFriendUsers();
    }

    public void setAllUsers(List<ChatUser> users) {
        allUsers.setValue(users);
    }

    public LiveData<List<ChatUser>> getConnectedUsers() {
        return repo.getFriendUsers();
    }

    public LiveData<List<ChatMessage>> getMessages(String myId, String friendId) {
        return repo.getMessages(myId, friendId);
    }

    public LiveData<Boolean> getTyping() {
        return repo.getTyping();
    }

    public void loadConversation(String myId, String friendId) {
        repo.fetchConversationFromServer(myId, friendId);
    }


    public void loadInitialMessages() {
        repo.loadInitialMessages();
    }

    public LiveData<Boolean> getIsTyping() {
        return repo.getTyping();
    }

    ;
    public void requestScrollToMessage(String messageId) {
        this.pendingMessageId = messageId;
    }

    public String getPendingMessageId() {
        return pendingMessageId;
    }

    public void clearPendingMessageId() {
        pendingMessageId = null;
    }

    public void refreshMessages(String myId, String friendId) {
        repo.fetchMessages(myId, friendId); // 🔥 force reload
    }


    public LiveData<String> getRejectEvents() {
        return repo.getRejectEvents();
    }

    public LiveData<String> getAcceptEvents() {
        return repo.getAcceptEvents();
    }

    public LiveData<List<ChatUser>> getFriendUsers() {
        return repo.getFriendUsers();
    }

    public void fetchAllUsers() {
        repo.fetchAllUsers();
    }

    public void addFriend(ChatUser user) {
        repo.addFriend(user);
    }

    public void onSendRequest(ChatUser user) {
        String fromUserId = SocketManager.getUserId();
        Log.d("FRIENDS", "ChatViewModel, fromUserId = " + fromUserId + " : " + user.getUserId() + " : " + user.getNickname() + " :");
        repo.sendFriendRequest(fromUserId, user);
    }

    public void onCancelRequest(ChatUser user) {
        String fromUserId = SocketManager.getUserId();
        repo.cancelFriendRequest(fromUserId, user);
    }

    public void sendFriendRequest(String toUserId) {
        String myId = SocketManager.getUserId();
        repo.sendFriendRequest(myId, toUserId);
    }

    public void setSelectedUser(ChatUser user) {
        selectedUser.setValue(user);
    }

    public LiveData<ChatUser> getSelectedUser() {
        return selectedUser;
    }

    public void loadMessagesBetweenMeAndOther(String myId, String id) {
        repo.loadMessagesBetweenMeAndOther(myId, id);
    }

    public LiveData<String> getCurrentFriendId() {
        return currentFriendId;
    }

    public void setCurrentFriendId(String friendId) {
        //currentFriendId.setValue(friendId);
        currentFriendId.setValue(friendId);

    }

    public LiveData<List<ChatMessage>> messages =
            Transformations.switchMap(currentFriendId, friendId -> {
                if (friendId == null) return new MutableLiveData<>(new ArrayList<>());
                return repo.getMessages(MainApplication.myId, friendId);
            });

    public LiveData<List<ChatUser>> getAllUsers() {
        return repo.getAllUsers();
    }

    public UserUiState getUserUiStateSync(String userId) {
        return repo.getUserUiStateSync(userId);
    }

    public boolean isInCooldown(String userId) {
        UserUiState state = repo.getUserUiStateSync(userId);

        long ts = state != null ? state.lastRejectedAt : 0;
        long now = System.currentTimeMillis();

        return ts > 0 && now - ts < 60_000;
    }

    public LiveData<List<UserUiState>> getAllUiStates() {
        return repo.getAllUiStates();
    }

    public void acceptFriend(String friendId) {
        repo.acceptFriend(SocketManager.getUserId(), friendId);
    }

    public void rejectFriend(String friendId) {
        repo.rejectFriend(SocketManager.getUserId(), friendId);
    }

    public LiveData<ChatUser> getUserById(String userId) {
        return repo.getUserById(userId);
    }

    public void setPending(ChatUser user) {
        Executors.newSingleThreadExecutor().execute(() -> {

            ChatUser pendingUser = new ChatUser(
                    user.getUserId(),
                    user.getNickname(),
                    user.getOnlineStatus(),
                    "pending",
                    0
            );

            repo.userDao.insert(pendingUser);
        });
    }

    public void fetchPendingRequests(String myId) {
        repo.fetchPendingRequests(myId);
    }

    public LiveData<String> getSelectedUserId() {
        return selectedUserId;
    }

    public void selectUserById(String userId) {
        Log.d("HIGHLIGHT", "VM selectUserById = " + userId);
        selectedUserId.postValue(userId);
    }

    public void openPendingRequest(String userId) {
        //get pending requests
        fetchPendingRequests(MainApplication.myId);

        // optional: highlight / scroll to user
        selectUserById(userId);
    }

    public void cancelFriendRequest(String userId) {
        repo.cancelFriendRequest(userId);
    }

    public LiveData<Map<String, UserUiState>> getUserStateMap() {
        return userStateMap;
    }

    public void loadAllUsers() {
        repo.loadAllUsers();
    }

    public void blockUser(String blockedUserId, long durationMs ) {
        repo.blockUser(SocketManager.getUserId(),blockedUserId, durationMs );
    }
}

