package com.google.amara.chattab;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;

//socket = IO.socket("https://android-chat-server.onrender.com", options);
//
public final class SocketManager {

    private static Socket socket;
    private static boolean initialized = false;
    private static String currentUserId;


    private SocketManager() {}

    public static synchronized void init(String url, String token, String userId) {
        if (initialized) return;

        try {
            IO.Options options   = new IO.Options();
            options.transports   = new String[]{"websocket"};
            options.reconnection = true;
            options.forceNew     = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay    = 2000;
            options.reconnectionDelayMax = 10000;

            options.auth = new HashMap<>();
            options.auth.put("token", token);

            socket = IO.socket(url, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("Socket", "✅ CONNECTED: " + socket.id());

                socket.emit("chat:get_users_with_unread");

            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e("Socket", "🚨 CONNECT ERROR", new Throwable(args[0].toString()))
            );

            socket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d("Socket", "❌ DISCONNECTED")
            );

            initialized   = true;
            currentUserId = userId;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }
    public static synchronized void connect() {
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }

    public static Socket getSocket() {
        return socket;
    }

    public static String getUserId() {
        return currentUserId;
    }
}

