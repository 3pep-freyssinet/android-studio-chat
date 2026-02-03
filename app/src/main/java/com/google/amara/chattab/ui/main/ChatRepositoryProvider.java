package com.google.amara.chattab.ui.main;

import com.google.amara.chattab.SocketHandler;
import com.google.amara.chattab.SocketManager;

import io.socket.client.Socket;

public final class ChatRepositoryProvider {

    private static ChatRepository repository;

    public static synchronized ChatRepository get() {
        if (repository == null) {
            Socket socket = SocketHandler.getSocket();
            //repository    = ChatRepository.get();
        }
        return repository;
    }
}


