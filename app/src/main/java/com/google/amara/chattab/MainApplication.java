package com.google.amara.chattab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.amara.chattab.utils.JwtUtils;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

public class MainApplication extends Application{

    public static final String SOCKET_URL  = "https://android-chat-server.onrender.com";
    private static final String PREFS_NAME = "myAppPrefs";

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    public static final boolean FCM = false; ;

    //Alice - redmi
    //public static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQ5NywidXNlcm5hbWUiOiJBbGljZTEiLCJpYXQiOjE3NzcwMTcwMjUsImV4cCI6MTc3NzEwMzQyNX0.eO2D4ONURtimHXTnI1LWqftqXh9HpJ8zR0skPHy8rlQ";
    //public static String myId     = "497";
    //public static String friendId = "498";
    //public static final int senderAvatarId = R.drawable.amy_jones;

    private static final String TAG         = "SocketTestActivity";
    public static String currentChatUserId; //set when a user tapes an avatar to open conversation
    public static String currentChatUserId_ = null;
    public static String pendingChatUserId  = null;

    //Fanny- poco
    public static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQ5OCwidXNlcm5hbWUiOiJGYW5ueTEiLCJpYXQiOjE3NzcwMTcxMDMsImV4cCI6MTc3NzEwMzUwM30.OomhVr5ApU3wjbHFfh6KIGpFT30Bp3kfGwRJAztdfbI";
    public static String myId     = "498";
    public static String friendId = "497";
    public static final int senderAvatarId = R.drawable.eugene_lee;

    //Karine- Harry
    //public static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQ0OCwidXNlcm5hbWUiOiJLYXJpbmUxIiwiaWF0IjoxNzc2MjY4MjE4LCJleHAiOjE3NzYzNTQ2MTh9.U-1TM2MlCL7fMynA3WjZIAlB12I1NUQTt3fG_d9h9L4";
    //public static String myId     = "448";
    //public static String friendId = "446";
    //public static final int senderAvatarId = R.drawable.eugene_lee;

    private static Socket socket;

    //SharedPreferences sharedPreferences = PreferenceUtils.getPreferences(context);
    private static SharedPreferences sharedPreferences = null;  //= getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor     = null;

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String status = NetworkUtil.getConnectivityStatusString(context);
            String action = intent.getAction();

            if(action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
                //notify the user
                //test, remove comment in production
                //showNetworkStatus(status);
            }
        }
    };

    public static SharedPreferences getSharedPreferences_() {
        return sharedPreferences;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        //String token = JwtStorage.getToken(this);
        //if (token == null) return;

        //String socketUrl = BuildConfig.SOCKET_URL;
        String userId = JwtUtils.getUserId(JWT_TOKEN); // decode JWT "userId"

        SocketManager.init(SOCKET_URL, JWT_TOKEN, userId);
        SocketManager.connect();


        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(new DefaultLifecycleObserver() {

                    @Override
                    public void onStart(@NonNull LifecycleOwner owner) {

                            Log.d("SOCKET","lifecycle → onStart");

                            //SocketManager.recreateSocket();

                            Socket socket = SocketManager.getSocket();

                        if (socket != null && !socket.connected()) {
                            socket.connect(); // ✅ safe
                        }
                    }

                    @Override
                    public void onStop(@NonNull LifecycleOwner owner) {

                        Log.d("SOCKET","lifecycle → onStop");

                        Socket socket = SocketManager.getSocket();

                        if (socket != null) {

                            // ⭐ tell server user is no longer realtime-reachable
                            if (socket.connected()) {
                                socket.emit("user_status_change", "offline");
                            }

                            // ⭐ kill transport (VERY important on Android)

                            socket.disconnect();
                        }
                    }
                });


    }


        /*
        try {
            IO.Options options = new IO.Options();
            //options.transports = new String[]{"polling", "websocket"};
            options.transports = new String[]{"websocket"};
            options.reconnection = true;
            options.forceNew = true;

            options.auth = new HashMap<>();
            options.auth.put("token", JWT_TOKEN);

            socket = IO.socket(SOCKET_URL, options);

            // 🔥 CORE EVENTS
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "✅ CONNECTED");
                Log.d(TAG, "socket.id = " + socket.id());
            });

            socket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d(TAG, "❌ DISCONNECTED: " + args[0])
            );

            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e(TAG, "🚨 CONNECT ERROR: " + args[0])
            );

            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e(TAG, "🚨 SOCKET ERROR: " + args[0])
            );
            */

            /*
            // 🔥 CUSTOM EVENT
            socket.on("chat:users:list", args -> {
                Log.d(TAG, "📥 chat:users:list RECEIVED");

                JSONArray array = (JSONArray) args[0];
                Log.d(TAG, "Users count = " + array.length());

                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.optJSONObject(i);
                    Log.d(TAG, "User: " + o.toString());
                }
            });


            Log.d(TAG, "Connecting socket...");
            socket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket URL", e);
        }
        */

        /*
        //String token = JwtStorage.getToken(this); // already exists in your app
        String userId = JwtUtils.getUserId(jwtToken); // decode JWT "userId"
        //String socketUrl = BuildConfig.SOCKET_URL;

        SocketManager.init(
                SERVER_URL,
                jwtToken,
                userId
        );

        SocketManager.connect();
        */
        /*
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Socket s = SocketManager.getSocket();
            Log.d("SocketTest", "connected = " + s.connected());
            Log.d("SocketTest", "id = " + s.id());
        }, 3000);

    }
    */

    //public static Socket getSocket(){
    //    return socket;
    //}



    private void showNetworkStatus(String status) {
        //View view = getWindow().getDecorView().findViewById(android.R.id.content);
        //Snackbar.make(view, "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();
        //or
        //Snackbar.make(findViewById(android.R.id.content), "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();

        //enable 'btn' to enter the chat only if there is connection.
        switch (status) {
            case "Wifi enabled":
            case "Mobile data enabled":
                //onConnection();
                break;
            case "No internet is available":
                //btn.setEnabled(false);

                //Get list of fragments
                //List<Fragment> fragments = getSupportFragmentManager().getFragments();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //sdk=26

                }

                //Notify the user
                AlertDialog.Builder b = new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Connection status")
                        .setMessage("No Internet connection.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                //finish();
                            }
                        });
                Dialog d = b.create();
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);          //2003
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);   //2006
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);                 //2002
                d.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);                   //2005
                d.show();

                /*
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setMessage("No Internet connection");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        //finish();
                    }
                });
                        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alertDialog.create().show();
                */

                break;
        }
    }

    public OkHttpClient getHttpClient() {
        return new OkHttpClient();
    }
}
