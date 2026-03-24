package com.google.amara.chattab.ui.main;

import android.app.Application;

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
import java.util.List;

import io.socket.client.Socket;

public class ChatViewModel extends AndroidViewModel {

    private final ChatRepository repo;

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
}

