package com.google.amara.chattab.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

//import com.example.aymen.androidchat.ChatUser;

import com.google.amara.chattab.ChatUser;
import com.google.amara.chattab.SocketManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repo = ChatRepository.get();

    public LiveData<List<ChatUser>> getConnectedUsers() {
        return repo.getUsers();
    }
}

