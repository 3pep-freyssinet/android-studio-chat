package com.google.amara.chattab.ui.main;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

//import com.example.aymen.androidchat.ChatUser;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.SocketManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import io.socket.client.Socket;

public class ChatViewModel extends AndroidViewModel {

    public final ChatRepository repo;

    //pagination
    private static final int PAGE_SIZE = 20;
    private int loadedMessages         = 0;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repo = ChatRepository.get(application);
    }

    public LiveData<List<ChatUser>> getConnectedUsers() {
        return repo.getUsers();
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


    public void loadInitialMessages(String myId, String friendId) {
        repo.loadInitialMessages(myId, friendId);
    }

    public LiveData<Boolean> getIsTyping() {
        return repo.getTyping();
    }
}

