package com.google.amara.chattab.ui.main;

import android.app.Application;

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
import com.google.amara.chattab.entities.UserUiState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChatViewModel extends AndroidViewModel {

    public  ChatRepository repo = null;

    //pagination
    private static final int PAGE_SIZE = 20;
    private int loadedMessages         = 0;
    private String pendingMessageId;

    private final MutableLiveData<List<ChatUser>> usersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ChatUser>> allUsers      = new MutableLiveData<>();
    private final MutableLiveData<ChatUser>        selectedUser = new MutableLiveData<>();
    private final MutableLiveData<String> currentFriendId       = new MutableLiveData<>();



    //constructor
    public ChatViewModel(@NonNull Application application) {
        super(application);
        repo = ChatRepository.get(application);
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
        currentFriendId.postValue(friendId);

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
}

