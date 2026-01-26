package com.google.amara.chattab;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.amara.chattab.ui.main.ChatRepository;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketTestActivity extends AppCompatActivity {

    private static final String TAG = "SocketTest";

    // 🔴 CHANGE THIS ONLY

    private static final String SOCKET_URL = "https://android-chat-server.onrender.com";

    //Alice - redmi
    private static final String JWT_TOKEN   = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjE4OSwidXNlcm5hbWUiOiJBbGljZTAxIiwiaWF0IjoxNzY4OTAxOTAyLCJleHAiOjE3Njg5ODgzMDJ9.BqOLyFGYE1L69pfd-3dBfoEz42NS5CuIlU9ezHPdoNU";


    private Socket socket;


        private ChatSharedViewModel vm;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            vm = new ViewModelProvider(this)
                    .get(ChatSharedViewModel.class);

            vm.startChat();

            vm.getUsers().observe(this, users -> {
                if (users == null) return;

                Log.d("MainActivity", "👥 Users = " + users.size());
                for (ChatUser u : users) {
                    Log.d("MainActivity", "User: " + u.getNickname());
                }
            });
        }
    }


