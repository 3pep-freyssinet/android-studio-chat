package com.google.amara.chattab;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

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

public class MainApplication extends Application {

    public static final String SOCKET_URL = "https://android-chat-server.onrender.com";


    //Alice - redmi
    //public static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjIyOSwidXNlcm5hbWUiOiJBbGljZTEiLCJpYXQiOjE3NzA4ODcwNTEsImV4cCI6MTc3MDk3MzQ1MX0.vQ-a1epW4ihp9OIb5T5W1mFWGUDKXVCqU6iJaGS19xg";
    //public static String myId     = "229";
    //public static String friendId = "230";


    private static final String TAG        = "SocketTestActivity";

    //Fanny- poco
    public static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjIzMCwidXNlcm5hbWUiOiJGYW5ueTEiLCJpYXQiOjE3NzA4ODcxMzYsImV4cCI6MTc3MDk3MzUzNn0.5KfPbqYk3UvgJAaTd3rIaWEQVYIToeLzk_GI1EBhGU8";
    public static String myId     = "230";
    public static String friendId = "229";

    private static Socket socket;

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

    @Override
    public void onCreate() {
        super.onCreate();

        //String token = JwtStorage.getToken(this);
        //if (token == null) return;

        //String socketUrl = BuildConfig.SOCKET_URL;
        String userId = JwtUtils.getUserId(JWT_TOKEN); // decode JWT "userId"

        SocketManager.init(SOCKET_URL, JWT_TOKEN, userId);
        SocketManager.connect();
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


    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
    }

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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { //sdk=26

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
}
