package com.google.amara.chattab;
import android.os.Handler;
import android.os.Looper;
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
    private  static String SERVER_URL;
    private static String token;
    private static String userId;
    private static IO.Options options_;



    private static final Handler heartbeatHandler = new Handler(Looper.getMainLooper());

    private static final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {

            Socket socket = SocketManager.getSocket();

            if (socket != null && socket.connected()) {
                socket.emit("presence_ping");
            }

            heartbeatHandler.postDelayed(this, 20000);   // ⭐ 20 seconds
        }
    };

    private SocketManager() {}

    public static synchronized void init(String url, String token, String userId) {

        SERVER_URL    = url;
        token  = token;
        userId = userId;

        if (initialized) return;

        try {
            IO.Options options   = new IO.Options();
            options.transports   = new String[]{"websocket"};
            options.reconnection = true; //false;
            options.forceNew     = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay    = 2000;
            options.reconnectionDelayMax = 10000;

            options.auth = new HashMap<>();
            options.auth.put("token", token);

            options_ = options;

            socket = IO.socket(url, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("SOCKET", "✅ CONNECTED: " + socket.id());

                String pending = MainApplication.pendingChatUserId;
                if (pending != null) {
                    Log.d("SOCKET", "Sending delayed chat:open → " + pending);

                    socket.emit("chat:open", pending);

                    MainApplication.pendingChatUserId = null;
                }

                socket.emit("chat:get_users_with_unread");
                socket.emit("user_status_change", "online");


                //heartbeatHandler.postDelayed(heartbeatRunnable, 20000);
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e("SOCKET", "🚨 CONNECT ERROR", new Throwable(args[0].toString()))
            );
            //Log.d("Socket", "❌ DISCONNECTED");

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d("SOCKET", "❌ DISCONNECTED");
                //heartbeatHandler.removeCallbacks(heartbeatRunnable);
            });

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

    public static void recreateSocket() {
        try {
            //if (socket != null) {
            //    socket.off();
            //    socket.disconnect();
            //   socket.close();
            //}

            socket = IO.socket(SERVER_URL, options_);

            Log.d("SOCKET","🆕 new socket instance");

            // ⭐ attach listeners again
            attachListeners(socket);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void attachListeners(Socket s) {

        s.on(Socket.EVENT_CONNECT, args ->
                Log.d("SOCKET","🟢 Connected"));

        s.on(Socket.EVENT_DISCONNECT, args ->
                Log.d("SOCKET","🔴 Disconnected"));

        s.on(Socket.EVENT_CONNECT_ERROR, args ->
                Log.d("SOCKET","⚠️ Connect error"));
    }

    public static Socket getSocket() {
        return socket;
    }

    public static String getUserId() {
        return currentUserId;
    }
}

