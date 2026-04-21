
package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;
import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.amara.chattab.ui.main.ChatViewModel;
import com.google.amara.chattab.upload.FileUploadManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import com.google.amara.chattab.ui.main.SectionsPagerAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;


public class TabChatActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ChatSharedViewModel sharedViewModel;
    public static String Nickname = "Me";
    private ChatSharedViewModel vm;
    private String pendingMessageId;
    private ChatViewModel chatViewModel;



    // 👈 for notification
    public interface LoadUserFriendsFromBackendCallback{
        void onSuccess(List<ChatUser> users);
        void onFailure(Exception exception);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabLayout tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.view_pager);

        vm = new ViewModelProvider(this)
                .get(ChatSharedViewModel.class);

        chatViewModel = new ViewModelProvider(this)
                .get(ChatViewModel.class);

        //load user friends from backend. The userId is in jwt.
        vm.loadUsers();

        //observe the selected user
        vm.getSelectedUser().observe(this, user -> {
            if (user != null && pendingMessageId != null) {
                vm.requestScrollToMessage(pendingMessageId);
                pendingMessageId = null;
            }
        });

        //'TabChatActivity' is called from 'MainActivity' and when the notification is clicked.
        // Then we do a check. I the getIntent contains 'senderId' as an extra, we are coming from a notification.

        Intent intent = getIntent();
        boolean isFromNotification = intent != null && intent.hasExtra("senderId");
        int initialTab = isFromNotification ? 1 : 0;

        viewPager.setAdapter(new ChatPagerAdapter(this));

        // 👇 force Tab 1 BEFORE mediator
        viewPager.setCurrentItem(0, false);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(position == 0 ? "Users" : "Chat");
                }
        ).attach();

        if (isFromNotification) {
            String senderId = intent.getStringExtra("senderId");
            String messageId = intent.getStringExtra("messageId");
            handleNotificationNavigation(senderId, messageId );
        }



        //load the user friends

        // 🔥 THIS IS THE AUTO-SWITCH LOGIC

        /*
        sharedViewModel = new ViewModelProvider(this)
                .get(ChatSharedViewModel.class);
        sharedViewModel.startChat();
        sharedViewModel.getSelectedUser().observe(this, user -> {
            if (user != null) {
                viewPager.setCurrentItem(1, true);
            }
        });
        */
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("LIFECYCLE", "onResume");


        String friendId = chatViewModel.getCurrentFriendId().getValue();

        MainApplication.currentChatUserId_ = friendId;

        Socket socket = SocketManager.getSocket();

        if (socket != null) {

            if (socket.connected()) {
                socket.emit("chat:open", friendId);
            } else {
                MainApplication.pendingChatUserId = friendId;
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("LIFECYCLE", "onPause");


        MainApplication.currentChatUserId_ = null;

        Socket socket = SocketManager.getSocket();
        if (socket != null && socket.connected()) {
            //socket.emit("chat:close");
        }

    }

    private void handleNotificationNavigation(String senderId, String messageId) {

        // 1. Switch to chat tab
        viewPager.setCurrentItem(1, true);
        //pendingMessageId = messageId;
        vm.setPendingMessageId(messageId); //✅

        // 2. Tell ViewModel which user to open
        vm.selectUserById(senderId); // 👈 we'll implement this

        // 3. 🔥 force refresh after selection
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            //chatViewModel.refreshMessages();
        }, 500);
    }

    public void openChatTab() {
        viewPager.setCurrentItem(1, true); // 🔥 Tab 1
    }

    public void openUsersTab() {
        viewPager.setCurrentItem(0, true); // 🔥 Tab 0
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.hasExtra("senderId")) {
            String senderId = intent.getStringExtra("senderId");
            handleNotificationNavigation(senderId, pendingMessageId);
        }
    }
}

