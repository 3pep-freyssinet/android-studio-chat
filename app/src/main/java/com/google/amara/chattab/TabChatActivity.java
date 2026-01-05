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
import androidx.viewpager.widget.ViewPager;

import io.socket.client.Ack;
import io.socket.emitter.Emitter;
import io.socket.client.Socket;

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

import com.example.aymen.androidchat.ChatBoxAdapter;
import com.example.aymen.androidchat.ChatBoxMessage;
import com.example.aymen.androidchat.ChatBoxActivity;
import com.example.aymen.androidchat.ChatUser;
import com.example.aymen.androidchat.ChatUserAdapter;
import com.example.aymen.androidchat.Message;

import com.example.aymen.androidchat.sql.LastUsersContract;
import com.example.aymen.androidchat.sql.MessagesContract;
import com.example.aymen.androidchat.sql.UsersContract;
import com.google.amara.chattab.upload.FileUploadManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import com.google.amara.chattab.ui.main.SectionsPagerAdapter;

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

/***************************************************************************************************
Both pressing home button and receiving a call don't remove the activity from the task's stack,
and will be available when you re-enter the app => onPause() => onStop().
as the activity lifecycle diagram shows, re-entering the app calls => onRestart() => onStart() => onResume()

Pressing the back button instead kills the activity => onPause() => onStop() => onDestroy()
re-entering the app in this case calls the classics => onCreate() => onStart() => onResume()
 **************************************************************************************************/
/*      Internal and external storage

Every Android-compatible device supports a shared “external storage” that
 you can use to save files. This can be a removable storage media (such as
 an SD card) or an internal (non-removable) storage. Files saved to the
 external storage are world-readable and can be modified by the user when
 they enable USB mass storage to transfer files on a computer.
“external storage” is defined as the directory tree returned by :
 'Environment.getExternalStorageDirectory()'
external storage location : sdcard or /storage, to eventually the current /mnt/shell/emulated/0.

“internal storage” is a specific directory, unique to your app, where your
 app can place files. As suggested in the docs, those files by default are
 read-write for your app and deny-all for any other app.

    getCacheDir()
    getDir()
    getDatabasePath()
    getFilesDir()
    openFileInput()
    openFileOutput()
Other methods will rely upon these, such as openOrCreateDatabase().
 Other classes also will rely upon these, such as SQLiteOpenHelper and SharedPreferences.

 The internal storage is located at : 'data/data/your.application.package.name'
*/

public class TabChatActivity extends AppCompatActivity
                             implements ChatBoxMessage.SendMessage,
                                        ChatBoxMessage.DownloadAttachment,
                                        ChatUserAdapter.UserData,
                                        ChatBoxAdapter.Attachment,
                                        ChatBoxActivity.UserNotification,
                                        ChatBoxActivity.AllUsersList,
                                        AllUsersAdapter.UserReference,
                                        BroadcastNotification{


    private static final int NB_MESSAGES_TO_DOWNLOAD = 10;
    public static Emitter.Listener join_Emitter;

    private TabLayout   tabs;
    public Socket       socket;
    public String       Nickname, IdNickname;  //current user nickname and id
    private JSONObject  Profile;               //current user image profile

    //public ArrayList<Message> messageListUser;          // tous les messages destinés à cet user sur lequel on a cliqué
    //public ArrayList<ChatUser> chatUserList;              // all users connected sauf le courant qui est 'Nickname'

    private ViewPager viewPager;
    private SectionsPagerAdapter sectionsPagerAdapter;

    private static final int INTENT_RESULT_CODE    = 101;
    private static final int GET_VIEW_REQUEST_CODE = 200;
    private static final int REQUEST_CODE_ALL_USERS= 300;
    private static final int INTENT_NEW_USER_RESULT_CODE = 301;

    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;

    //private FileUploadManager fileUploadManager;
    private String content = "";
    private ArrayList<String> downloadParts = new ArrayList<>();
    private ArrayList<FileUploadManager> fileUploadManagerArrayList;
    private ImageView thumbAttachmentFrom; //a link clicked in 'ChatBoxAdapter' to download an image from server

    private int index = 0;       //index of files in upload_chunk
    private JSONObject downloadedChunk = new JSONObject();   //JSON witch contain downloaded chunks

    private ProgressDialog progressDialog;
    //private String mimeType;                 //mime type of downloaded attachment.
    private String selectedNickname;         //The selected user in connection pane

    private String uploadAttachmentReference;//The reference of uploaded attachment.
    private Uri uploadAttachmentUri;         //The uri of uploaded attachment.

    //private String downloadAbsolutePath;
    private File file;  //used to build file in :  File fileFolder = Environment.getExternalStoragePublicDirectory(
    //              Environment.DIRECTORY_DOWNLOADS);;
    //              file = new File(fileFolder, getFilename(ref))

    //camera fields
    private Uri         photoURI;
    private String      imageFileName;
    private String      currentPhotoPath;
    private final int   REQUEST_TAKE_PHOTO    = 200;

    private long        connectionTime;
    private long        lastConnectionTime;
    private long        disconnectionTime;
    private ChatUser    currentChatUser;
    private ChatUser[]  lastSessionUsers;
    private String      imageProfile;
    private String      imageProfileUri;
    private ArrayList<NotSeenMessage> notSeenMessages; //contains : author, nb not neen messages
    private AlertDialog alertDialog;
    private int passage = 0;

    //manage user interaction
    private Handler  handler = new Handler();
    private Runnable runnable;
    private int ii;
    private File compressedFile;
    private MutableLiveData<String> listen;
    private boolean once     = true;
    private boolean redirect = false;

    private BroadcastReceiver myReceiver;


    //We have receive notification sent by broadcast
    @Override
    public void sendNetworkNotification(String networkStatus) {

        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if(fragments.isEmpty())return;
        int index  = 0;
        int index_ = 0;
        int i      = 0;


        //the 'sectionsPagerAdapter' method is not used
                /*
                for (int i = 0; i <= sectionsPagerAdapter.getCount() - 1; i++) {
                    Fragment fragment = sectionsPagerAdapter.getItem(i);
                    if (ChatBoxActivity.class.isInstance(fragment)) index = i;
                    if (ChatBoxMessage.class.isInstance(fragment)) index_ = i;
                }
                */


                for(Fragment f0 : fragments){
                    if(ChatBoxActivity.class.isInstance(f0)) index = i;
                    if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                    i++;
                }

                ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                if((f  != null) && (f.isVisible()))f.sendNetworkNotification(networkStatus);
                if((f_ != null) && (f_.isVisible()))f_.sendNetworkNotification(networkStatus);
    }

    //private ChatBoxMessage  chatBoxMessage;
    //private ChatBoxActivity chatBoxActivity;


    /*
    private Handler handle = new Handler() {//utilisé dans progress dialog bar.
        //@Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            progressDialog.incrementProgressBy(10);
        }
    };
    */

    public enum DataHolder {
        INSTANCE;

        private List<Object> mObjectList;

        public static boolean hasData() {
            return INSTANCE.mObjectList != null;
        }

        public static void setData(final List<Object> objectList) {
            INSTANCE.mObjectList = objectList;
        }

        public static List<Object> getData() {
            final List<Object> retList = INSTANCE.mObjectList;
            INSTANCE.mObjectList = null;
            return retList;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        //MutableLiveData object creation
        listen = new MutableLiveData<>();
        listen.setValue("Initial value: 0");

        //Listener for listening the changes
        listen.observe(this,new Observer<String>() {
            @Override
            public void onChanged(String changedValue) {
                Toast.makeText(TabChatActivity.this, "new value " + listen.getValue(),Toast.LENGTH_LONG).show();
                listen.postValue(changedValue);
            }
        });

        //Changing values randomly
        new CountDownTimer(30000, 3000) {
            public void onTick(long millisUntilFinished) {
                listen.setValue("Changed value: " + new Random().nextInt(61));
            }

            public void onFinish() {
                listen.setValue("Final value: 100");
            }
        }.start();
        */

        System.out.println("************ onCreate ******");
        if(savedInstanceState != null){
            System.out.println("apres onSaved ii = " + ii);
            ii = savedInstanceState.getInt("ii", 0) + 5;
            System.out.println("apres onSaved getInt ii = " + ii);
            redirect = savedInstanceState.getBoolean("redirect");
            //String fileUploadManagerArrayList_ = savedInstanceState.getString("fileUploadManagerArrayList", null);
            //fileUploadManagerArrayList = new ArrayList<FileUploadManager>(fileUploadManagerArrayList_);
            //System.out.println("************ onCreate ******chatMessageList = " + fileUploadManagerArrayList.size());
        }else{
            ii = 10;
        }

        myReceiver = new MyReceiver(this);
        broadcastIntent();

        viewPager = findViewById(R.id.view_pager);

        //Setup progress dialog
        progressDialog = new ProgressDialog(TabChatActivity.this);
        progressDialog.setProgressNumberFormat("%1d/%2d");
        NumberFormat percentInstance = NumberFormat.getPercentInstance();
        percentInstance.setMaximumFractionDigits(0);

        progressDialog.setTitle("ProgressDialog bar example");
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressPercentFormat(percentInstance);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.hide();
        ////////////////////////////////////////////////////////////////////////////////////////////
        //do nothing
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Log.d("ovech", "back stack changed");
                Log.d("ovech", "back stack count = " + getSupportFragmentManager().getBackStackEntryCount());
                if(getSupportFragmentManager().getBackStackEntryCount()>0) {
                    int i = 0;
                } else {
                    int i = 1;
                }
                int i = 2;
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////
        //Manage user interaction
        //already used un NoInteractionSingleton
        runnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                //Toast.makeText(TabChatActivity.this, "user is inactive from last 5 minutes", Toast.LENGTH_LONG).show();

                //Notify the user
                new AlertDialog.Builder(TabChatActivity.this)
                        .setMessage("Your are inactive in users list for a while, need more time ?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //startHandler();
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create()
                        .show();
            }
        };
        //startHandler();

        ////////////////////////////////////////////////////////////////////////////////////////////

        //The number of tabs is fixed in 'getCount' of the adapter 'sectionsPagerAdapter';
        tabs = findViewById(R.id.tabs);

        sectionsPagerAdapter = new SectionsPagerAdapter(TabChatActivity.this, getSupportFragmentManager());
        viewPager.setAdapter(sectionsPagerAdapter);

        // notify the adapter to update the recycler view.
        //sectionsPagerAdapter.notifyDataSetChanged();

        fileUploadManagerArrayList = new ArrayList<>();

        //public static final String URL = "http://10.0.2.2:8080";      //Emulator
        //public static final String URL = "http://localhost:8080";     //Device
        //public static final String URL = "https://dry-springs-89362.herokuapp.com/";

        //Set the array list
        //chatUserList    = new ArrayList<ChatUser>();
        //messageList     = new ArrayList<Message>();
        //messageListUser = new ArrayList<>();

        //jump = false;   //in restart, if jump = true, do not join twice

        //for test only
        //Message message = new Message("mono", "01", "bis", "02",
        //        "hello from mono to bis", 0, new Object());
        //messageList.add(message);

        // get the extras
        Nickname                    = getIntent().getExtras().getString("Nickname");
        connectionTime              = getIntent().getExtras().getLong("connection_time");
        final int[] firstTime       = {getIntent().getExtras().getInt("first_time")};       //array one element to convert it to final
        boolean imageProfileChanged = getIntent().getExtras().getBoolean("image_profile_changed");
        currentChatUser             = getIntent().getExtras().getParcelable("current_user");
        notSeenMessages             = getIntent().getParcelableArrayListExtra("not_seen_messages");
        imageProfile                = getIntent().getExtras().getString("image_profile");       //may be null

        //for test only
        if(imageProfile != null){
            final byte[] decodedBytes = Base64.decode(imageProfile, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        imageProfileUri             = getIntent().getExtras().getString("image_profile_uri");   //may be null

        socket                      = SocketHandler.getSocket();

        //already done when socket is set in "MainActivity'
        //socket.on(Socket.EVENT_CONNECT_ERROR, mConnectionErrorListener);
        //socket.on(Socket.EVENT_CONNECT, mConnectionListener);

        setListen_join();
        setListen_lastuserjoined();
        setListen_messagedetection();
        setListen_uploadMessageComplete();
        setListen_uploadFileComplete();
        setListen_getAllUsers();
        setListen_on_get_user_back();
        setListen_on_get_users_back();
        setListen_on_get_last_users_back();

        socket.connect();

        /*
        //no user interaction
        socket.on("no_activity", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = (JSONObject) args[0];
                        AlertDialog.Builder builder = new AlertDialog.Builder(TabChatActivity.this);
                        builder.setTitle("No activity");
                        builder.setMessage("Need more time?");

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                socket.emit("more_time", true);
                                //finish();
                                //return;
                            }
                        });
                        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                socket.close();
                                finish();
                                return;
                            }
                        });
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        //.setNegativeButton(android.R.string.no, null)
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        //.show();

                        alertDialog = builder.create();
                        alertDialog.show();
                    }
                });
            }
        });
        */

        /*
        //The user does not respond to timeout message. The server send a logoff.
        socket.on("logoff", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = (JSONObject) args[0];
                        //alertDialog.dismiss();
                        //socket.close();
                        //finish();
                        //return

                        AlertDialog.Builder builder = new AlertDialog.Builder(TabChatActivity.this);
                        builder.setTitle("No activity");
                        builder.setMessage("Logoff");

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                                socket.close();
                                finish();
                                return;
                            }
                        });
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        //.setNegativeButton(android.R.string.no, null)
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        //.show();

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
            }
        });
        */

        //The last statements may be equal 0 for the first time or when the app has been deleted.
        // In the case the app has been deleted, get values from the server if any. If there are no values
        // in server buid a new 'ChatUser'

        // In the case it is the first time, the table 'users' from witch we get information exists but
        // doesn't contain entry for this user.

        /*
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //do something
            }
        });
        t.start(); // spawn thread
        try {
            t.join(1000);  // wait 1s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //not waiting
        */

        //default bitmap : avatar
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), com.example.aymen.androidchat.R.drawable.avatar);
        if(Nickname.equals("mono"))bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.amy_jones);
        if(Nickname.equals("bis"))bitmap  = BitmapFactory.decodeResource(getResources(), R.drawable.eugene_lee);
        if(Nickname.equals("ter"))bitmap  = BitmapFactory.decodeResource(getResources(), R.drawable.gary_donovan);

        //convert bitmap to array
        byte[] imageByte = convertBitmapToByteArray(bitmap);

            /*
            //Scale the images to 200x200 pixels.
            // Calculate inSampleSize
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;    //true only to get width and height

            int bitmap_width  = 100;
            int bitmap_height = 100;
            options.inSampleSize = calculateInSampleSize(options, bitmap_width, bitmap_height);
            options.inJustDecodeBounds = false;

            // Create the bitmap from byte[] The bitmap keep its original dimensions.
            Bitmap bitmap_ = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length, options);

            // Compress and encode the bitmap to base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap_.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

            byte[] byteArray = byteArrayOutputStream .toByteArray();
            */

        //Encode the bitmap to base64
        String encodedImage = Base64.encodeToString(imageByte, Base64.DEFAULT);

        //Convert the encoded image to JSONObject and send it
        Profile = new JSONObject();
        try {
            Profile.put(Nickname, encodedImage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        //Get and build the 'currentChatUser'
        //firstTime[0] = true; //for test only
        if(firstTime[0]){
            //The app has been deleted or it is really the first time. 
            // Get user data from server
            
            socket.emit("get_user", Nickname);
            socket.on("get_user_back", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject user = ((JSONObject) args[0]);
                    String      imageProfile    = null;
                    int         status          = 0;
                    long        connectedAt     = 0;
                    long        lastConnectedAt = 0;
                    long        disconnectedAt  = 0;
                    String      blacklistAuthor = null;
                    int         notSeenMessages = 0;
                    JSONObject  connectedWith   = null;

                    try {
                        imageProfile    = user.getString("imageProfile");
                        status          = user.getInt("status");
                        connectedAt     = user.getLong("connectedAt");
                        lastConnectedAt = user.getLong("lastConnectedAt");
                        disconnectedAt  = user.getLong("disconnectedAt");
                        blacklistAuthor = user.getString("blacklistAuthor");
                        notSeenMessages = user.getInt("notSeenMessages");
                        connectedWith   = user.getJSONObject("connectedWith");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //here 'firstTime == true', the above returned values are null or does not exists.

                    // if 'imageProfile' is not null, we are in case where the app ha been deleted,
                    // then fill the local db and  next time set 'firstTime' = false
                    //if 'imageProfile' is null we are really in the first time where noting exists
                    // build a 'ChatUser' and fill the local db and  next time set 'firstTime' = false

                    //Set 'firstTime' for next time
                    firstTime[0] = false;   //the array one element to convert it to final
                    //Store 'firstTime' in 'Preferences'
                    MainActivity.editor.putBoolean("first_time", firstTime[0]);
                    MainActivity.editor.commit();

                    if(imageProfile == null){
                        //It is really first time, build a 'ChatUser', fill the local db
                        try {
                            currentChatUser = new ChatUser (
                                    Nickname,
                                    null,
                                    Profile.getString(Nickname),
                                    ChatUser.userConnect,
                                    0,
                                    connectionTime,
                                    0,
                                    0,
                                    null
                            );

                            //Save the user in local db
                            int numRows = saveChatUser(currentChatUser);
                            
                            //if(uri == null)throw new UnsupportedOperationException("saveChatUserInfos");
                            initSocket();

                            //Save in server will be done in 'join'
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }else{
                        //the app has been deleted, fill the local db with values from server
                        currentChatUser = new ChatUser (
                                Nickname,
                                null,
                                imageProfile,
                                status,
                                notSeenMessages,
                                connectionTime,
                                disconnectedAt,
                                connectedAt,    //here, lastConnectedAt is equal the 'connectedAt' get from the server
                                blacklistAuthor
                        );
                        //Save the 'chatUser' in local db
                        int numRows = saveChatUser(currentChatUser);

                        //since tha app has been deleted, no need to send image profile to the other users.
                        //currentChatUser.imageProfile = null;
                        //Il y a un pb si 'imageProfile' = null. Si le destinataire ne trouve l'image
                        // dans sa bd locale, il va la chercher dans le server. pb du callback va apparaître.

                        initSocket();
                    }
                }
            });
 
        }else{
            //if(!imageProfileChanged)currentChatUser.imageProfile = null;

            //get the infos from local db. In this case, it is not first time, theses infos has
            // been transmitted in extra intent then no need to get them from local db
            currentChatUser = getChatUser(Nickname);

            //getLastSessionUsers(Nickname);
            initSocket();
        }
        */

        //String profile = currentChatUser.imageProfile;

        /*
        if(lastConnectionTime == 0) {
            //get values from local db
            //ChatUser currentUser = getConnectionInfos(Nickname);
            ChatUser currentUser = null;
            if (currentUser == null){
                //get values from server
                socket.emit("get_connection_infos", Nickname, new Ack() {
                    @Override
                    public void call(Object... args) {
                        String imageProfile     = (String) args[0];
                        int status              = (int) args[1];
                        String blacklistAuthor  = (String) args[2];
                        long connectionTime     = Long.parseLong((String) args[3]);
                        lastConnectionTime      = Long.parseLong((String) args[4]);
                        disconnectionTime       = Long.parseLong((String) args[5]);

                        if(imageProfile != null){
                            //save the values locally
                            //ChatUser currentUser = SaveConnectionInfos(Nickname);

                            //save thes values in 'Preferences'
                            MainActivity.editor.putLong("disconnected_at",   disconnectionTime);
                            MainActivity.editor.putLong("last_connected_at", lastConnectionTime);

                            MainActivity.editor.commit();
                        }
                    }
                });
            }
        }
        */

        tabs.setupWithViewPager(viewPager);
        //TabHost tabhost = getTabHost();  //TabHostActivity

        //'PlaceholderFragment' and 'PageViewModel' are called by 'SectionsPagerAdapter' if there is
        //no fragment supplied for each tab.

        //tabs text color
        tabs.setTabTextColors(
                // Unselected Tab Text Color : white color
                ContextCompat.getColor(this, R.color.unselectedTabTextColor),

                // Selected Tab Text Color : black color
                ContextCompat.getColor(this, R.color.selectedTabTextColor)
        );


        //At startup, by default, tab(0) is selected. its text is colored in gradient.
        tabs.getTabAt(0).select();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            tabs.getTabAt(0).view.setBackground(getResources().getDrawable(R.drawable.gradient));
        }else{
            tabs.getTabAt(0).view.setBackgroundColor(Color.CYAN);
        }

        //tabs event
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == 0) {
                    selectedNickname = null; //no user selected;

                    //update the server with this information
                    //Send the current user 'Nickname' and the selectedNickname 'selectedNicknameId'
                    // to server. It can put them in map
                    if(socket != null)
                        socket.emit("current_selected_user", Nickname, null, null );
                }

                // Toast. makeText(TabChatActivity.this, "tab 0 is selected", Toast. LENGTH_SHORT).show();

                //super.onTabSelected(tab);
                //background color : gradient color.
                //tab.view.setBackgroundColor(getResources().getColor(R.color.selectedTabColor));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    tab.view.setBackground(getResources().getDrawable(R.drawable.gradient));
                }else{
                    tab.view.setBackgroundColor(Color.CYAN);
                }
                //The indicator is not visible. It is overlapped by the backgroud color.
                //tabs.setSelectedTabIndicatorColor(Color.BLACK);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.view.setBackgroundColor(getResources().getColor(R.color.unselectedTabColor));

            }

            //onTabReselected is called whenever a tab is clicked again while it is already showing.
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //tab.view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }
        });

        /*
        //Floating Action Button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        //For internal storage, no need to provide any permissions.
        // add : 'android:requestLegacyExternalStorage="true"' to manifest
        //To access other files in shared storage on a device that runs Android 10 (API level 29),
        // it's recommended that you temporarily opt out of scoped storage by setting
        // requestLegacyExternalStorage to true in your app's manifest file.
        // In order to access media files using native files methods on Android 10,
        // you must also request the READ_EXTERNAL_STORAGE permission

        //permissions for external storage : getExternalFilesDir()
        // we add permissions we need to request location of the users

        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);
        permissionsToRequest = permissionsToRequest(permissions);

        // Request permissions, the result will be found in 'onRequestpermissionsResult'.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                //'requestPermissions' is a method of Activity and 'permissionsToRequest' is an array list
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);

            }
        }

         //c'est ça qui marche
        final int REQUEST_PERMISSIONS = 100;
        if ((ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {

                    if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) &&
                            (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) &&
                            (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.CAMERA))) {

                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.CAMERA},
                                REQUEST_PERMISSIONS);
                    }
        }else {
            //Do something
        }


        //FileUploadManager fileUploadManager = new FileUploadManager();
        File file  = getFilesDir();  //data/user/0/<package>/files  <--- internal storage  or //data/data/<package>/files
        File file_ = Environment.getExternalStorageDirectory();     //storage/emulated/0/   <---- external storage

        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/acrobat_trial.txt";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/media2.mp4";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/media2.avi";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/avatar.jpg";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/test.txt";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/Avis_d_impot_2020_sur_les_revenus_2019.pdf";
        //final String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/demo.jpg";
        //String UPLOAD_FILE_PATH = getFilesDir().getPath()+"/IMG_20200526_191331.jpg"; //3.5 Mo
        //final String UPLOAD_FILE_PATH = Environment.getExternalStorageDirectory().getPath()+"/Pictures/IMG_20200526_191331.jpg";

        //fileUploadManager = new FileUploadManager();
        //boolean prepare   = fileUploadManager.prepare(UPLOAD_FILE_PATH, TabChatActivity.this);
        //if(!prepare)return; //there is exception in file.

        ////////////////////////////////////////////////////////////////////////////////////////////
        //training : interface, listener
        // Create the custom object
        MyCustomObject object = new MyCustomObject();
        // Step 4 - Setup the listener for this object
        object.setCustomObjectListener(new MyCustomObject.MyCustomObjectListener() {

            @Override
            public void onDataLoaded(String data) {
                TabChatActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast. makeText(TabChatActivity.this, data, Toast. LENGTH_SHORT).show();
                    }
                });

            }
        });
        ////////////////////////////////////////////////////////////////////////////////////////////
        //save file to internal or external storage
        //final String PDF_PATH = getFilesDir().getPath()+"/impot-2019.pdf";
        final String PDF_PATH = getFilesDir().getPath()+"/media2.mp4";

        /*
        try {
            //InputStream in = getAssets().open(PDF_PATH);
            File f_ = new File(PDF_PATH);
            FileInputStream in = new FileInputStream(f_);
            //writeFileToInternalStorage(f_);

            //Not used
            Uri savedFileUri = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                //savedFileUri = savePDFFile(this, in, "files/pdf", "eliza.pdf", "resume");
            }
            //Log.d("URI: ", savedFileUri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }


        //Split file and save parts in internal or external storage
        //String f = getFilesDir().getPath()+"/media2.mp4";
        String f = getFilesDir().getPath()+"/KAMEL_SBANIOULI_HIGH_Hmamm_deraj_vocals.wav";
        */
        //splitFile(f);
        ////////////////////////////////////////////////////////////////////////////////////////////
        //'initSocket()' is called when fragment is attached 'getSupportFragmentManager().addFragmentOnAttachListener' parce que
        // si 'initSocket' is called  from 'onCreate' 'getSupportFragmentManager' ne donne pas les deux fragments 'ChatBoxActivity' et 'ChatBoxMessage'
        // cf 'SectionPagerAdapter'.
        ////////////////////////////////////////////////////////////////////////////////////////////
        //initSocket();//not used  'called when fragment is attached'.
        //socket.connect();

        //Async task
        //UploadFilesTask uploadFiles = new UploadFilesTask(this, socket);

        //Executing async task
        //uploadFiles.execute();


        // if (savedInstanceState == null) {
        //    getSupportFragmentManager().beginTransaction()
        //            .replace(R.id.view_pager, new ChatBoxMessage())
        //            .commitNow();
        //}
    }

    private void setListen_on_get_last_users_back() {
        socket.on("get_last_users_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray lastUsers = ((JSONArray) args[0]);
                        initSocket5(lastUsers);
                    }
                });
            }
        });

    }

    private void broadcastIntent() {
        registerReceiver(myReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    //Manage user interaction
    @Override
    public void onUserInteraction() {
        // TODO Auto-generated method stub
        super.onUserInteraction();
        //stopHandler();//stop first and then start
        //startHandler();
    }

    //Manage user interaction
    //public void stopHandler() {
    //    handler.removeCallbacks(runnable);
    //}

    //manage user interaction
    //manage user interaction
    //public void startHandler() {
        //handler.postDelayed(runnable, 1 * 60 * 1000); //for 1 minutes
    //}

    /**
     * Get last session contacts for the supplied user from local sqlite db. Sort the map of users by time
     * @param nickname the user
     * @return A sorted map (by time) of the users who contact the supplied user
     */
    private  Map<String,Long> getLastSessionUsers(String nickname) {
        //get users from local db. if nothing found, get them from server
        //Get users from server
        String[] mProjection = new String[]
                {
                        LastUsersContract.COLUMN_ID,
                        LastUsersContract.COLUMN_USER_ORIGINE,
                        LastUsersContract.COLUMN_USER_TARGET,
                        LastUsersContract.COLUMN_TIME
                };

        //Method : ContentProvider

        Cursor cursor = getContentResolver().query(
                LastUsersContract.CONTENT_URI_LAST_USERS,
                mProjection,
                LastUsersContract.COLUMN_USER_ORIGINE + " =? OR " +
                        LastUsersContract.COLUMN_USER_TARGET + " =? ",

                new String[]{nickname, nickname},

                LastUsersContract.COLUMN_TIME + " DESC"
        );

        if (cursor == null) return null;
        if (cursor.getCount() == 0) return new HashMap<>();

            cursor.moveToPosition(-1);

            long id = 0;
            String from = null;
            String to = null;
            long time = 0;

            int i = 0;
            //Map<String, Long> map = new HashMap<>();      //HashMap doesn't preserve any order
            Map<String, Long> map = new LinkedHashMap<>();  //HashMap doesn't preserve any order
            //ArrayList<String> arrayList = new ArrayList<>();
            while (cursor.moveToNext()) {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_ID));
                from = cursor.getString(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_USER_ORIGINE));
                to = cursor.getString(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_USER_TARGET));
                time = cursor.getLong(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_TIME));
                //arrayList.add(to);
                if (from.equals(nickname)) map.put(to, time);
                if (to.equals(nickname)) map.put(from, time);
            }
            cursor.close();
            return map;
    }

    private  ChatUser getUser(String nickname) {
        //select ... where MediaTypeId IN (1, 2)

        ChatUser chatUser = null;
        String[] mProjection = new String[]
        {
            UsersContract.COLUMN_ID,
            UsersContract.COLUMN_USER_NAME,
            UsersContract.COLUMN_PROFILE,
            UsersContract.COLUMN_STATUS,
            UsersContract.COLUMN_NOT_SEEN,
            UsersContract.COLUMN_CONNECTED,
            UsersContract.COLUMN_DISCONNECTED_AT,
            UsersContract.COLUMN_BLACKLIST_AUTHOR
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                UsersContract.CONTENT_URI_USERS,
                mProjection,
                UsersContract.COLUMN_USER_NAME  + " =? ",

                new String[]{nickname},

                null
        );

        if((cursor == null) || (cursor.getCount() == 0))return null;

        if (cursor != null) {
            cursor.moveToPosition(-1);
        }

        long id                = 0;
        String userName        = null;
        String imageProfile    = null;
        int status             = 0;
        int notSeenMessages    = 0;
        long connectedAt       = 0;
        long disconnectedAt    = 0;
        String blacklistAuthor = null;

        int i = 0;
        cursor.moveToNext();
            id              = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_ID));
            userName        = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_USER_NAME));
            imageProfile    = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_PROFILE));
            status          = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_STATUS));
            notSeenMessages = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_NOT_SEEN));
            connectedAt     = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_CONNECTED));
            disconnectedAt  = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_DISCONNECTED_AT));
            blacklistAuthor = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_BLACKLIST_AUTHOR));

            // Build the 'ChatUser' object.
            chatUser = new ChatUser (
                userName,
                null,
                imageProfile,
                status,
                notSeenMessages,
                connectedAt,
                disconnectedAt,
                0,
                blacklistAuthor
            );

        cursor.close();
        return chatUser;
    }

    private  ChatUser[] getChatUsers(Set<String> nicknames) {
        if(nicknames.size() == 0)return new ChatUser[0];

        //get users data from local db. if nothing found, get them from server
        //Get users from server
        //Iterator<String> keys = nicknames.iterator();
        String[] users = nicknames.toArray(new String[nicknames.size()]);
        //select ... where MediaTypeId IN (1, 2)

        //ChatUser[] chatUser = new ChatUser[nicknames.size()];
        String[] mProjection = new String[]
        {
                UsersContract.COLUMN_ID,
                UsersContract.COLUMN_USER_NAME,
                UsersContract.COLUMN_PROFILE,
                UsersContract.COLUMN_STATUS,
                UsersContract.COLUMN_NOT_SEEN,
                UsersContract.COLUMN_CONNECTED,
                UsersContract.COLUMN_DISCONNECTED_AT,
                UsersContract.COLUMN_BLACKLIST_AUTHOR
        };

        TextUtils.join(",", users);

        /*
        boolean first = true;

        String inClause__ = "(";

        for(String v : users){
            if(first){
                first = false;
            } else {
                inClause__ += ",";
            }
            inClause__+= "'" + v + "'";
        }
        inClause__ += ")";


        String inClause = "";

        for(String v : users){
            if(first){
                first = false;
            } else {
                inClause += ",";
            }
            inClause += v ;
        }
        inClause += "";

        String[] inClause_ = new String[]{inClause};
        */

        /*
        String values[] = {"23","343","33","55","43"};
        String inClause = values.toString();

        //at this point inClause will look like "[23,343,33,55,43]"
        //replace the brackets with parentheses
        inClause = inClause.replace("[","(");
        inClause = inClause.replace("]",")");

        //now inClause will look like  "(23,343,33,55,43)" so use it to construct your SELECT
        String select = "select * from table_name where id in " + inClause;
         */

        //set the '?' placeholders
        StringBuilder sb = new StringBuilder(users.length * 2 - 1);
        sb.append("?");
        for (int i = 1; i < users.length; i++) {
            sb.append(",?");
        }
        String param = sb.toString();

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                UsersContract.CONTENT_URI_USERS,
                mProjection,
                UsersContract.COLUMN_USER_NAME  + " IN ( " + param + " ) ",
                users,
                null
        );

        if(cursor == null)return null;
        if(cursor.getCount() == 0) return new ChatUser[0];

        cursor.moveToPosition(-1);

        long id                = 0;
        String userName        = null;
        String imageProfile    = null;
        int status             = 0;
        int notSeenMessages    = 0;
        long connectedAt       = 0;
        long disconnectedAt    = 0;
        String blacklistAuthor = null;

        //if(cursor.getCount() != nicknames.size())
        //    throw new UnsupportedOperationException("cursor size and nicknames mismatch : ");

        ChatUser[] chatUser = new ChatUser[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            id              = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_ID));
            userName        = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_USER_NAME));

            //imageProfile may be null
            imageProfile    = cursor.isNull(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_PROFILE)) ?
                            null : cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_PROFILE));

            status          = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_STATUS));
            notSeenMessages = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_NOT_SEEN));
            connectedAt     = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_CONNECTED));
            disconnectedAt  = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_DISCONNECTED_AT));

            //blacklistAuthor may be null
            blacklistAuthor = cursor.isNull(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_BLACKLIST_AUTHOR)) ?
                    null : cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_BLACKLIST_AUTHOR));

            // Build the 'ChatUser' object.
            ChatUser chatUser_ = new ChatUser (
                    userName,
                    null,
                    imageProfile,
                    status,
                    notSeenMessages,
                    connectedAt,
                    disconnectedAt,
                    0,
                    blacklistAuthor
            );

            chatUser[i] = chatUser_;
            i++;
        }

        //order the 'chatUser' array given by the cursor with respect to the supplied 'Set<String> nicknames'
        List<ChatUser> chatUser__ = Arrays.asList(chatUser);
        ChatUser[]  chatUser_ =  new ChatUser[chatUser.length];
        int j = 0;
        Iterator iterator = nicknames.iterator();
        while(iterator.hasNext()){ //run over reference items 'nicknames'
            String nickname = (String)iterator.next();
            Iterator iterator_ = chatUser__.iterator();
            boolean match = false;
            while(iterator_.hasNext()){ //run over the array items 'chatUser__' to modify places
                ChatUser chatUser0 = (ChatUser)iterator_.next();
                String nickname0 = chatUser0.nickname;
                if(nickname0.equals(nickname)){
                    chatUser_[j] = chatUser0;
                    match = true;
                    break;
                }
            }
            //The following statement is not right. if the list is already sorted the boolean
            // 'match' remains false
            //if(!match) throw new UnsupportedOperationException("Cannot sort array items");
            j++;
        }

        cursor.close();
        return chatUser_;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException exception) {
                // Error occurred while creating the File
                exception.getMessage();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoURI = FileProvider.getUriForFile(this,
                            "com.example.aymen.androidchat.cameraFileprovider", photoFile);

                    //In case of thumbnail do not use the following statement
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    //}catch(IllegalArgumentException ex){
                }catch(Exception ex){
                    ex.getMessage();
                }
            }
        }
    }

    //Create image file for camera
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_"; //il est tres important le dernier '_',. il permer de séparer le nombre aléatoire ajouté par  'createTempFile' plus bas.

        //the pictures will be found in device file explorer in : storage/sdcard0/Android/data/<package>/files/Pictures/....jpg
        //File storageDir0 = getExternalFilesDir(Environment.DIRECTORY_PICTURES);  //storage/emulated/0/Android/dta/<package>/files/Pictures dans gallery il y a creation d'un dossier 'Pictures'

        //File storageDir1 = Environment.getExternalStorageDirectory();  //storage/emulated/0
        //File storageDir2 = getFilesDir();    //data/user/0/<package>/files dans le device file explorer, il y a : /data/data/<package>:files/....jpg
        //File storageDir3 = getCacheDir();

        //Camera photos
        File storageDir   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File storageDir   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //File root = Environment.getExternalStorageDirectory();
        //File storageDir = new File(root.getAbsolutePath()+"/DCIM/");

        //File storageDir = new File(getFilesDir().getPath());  //ne marche pas :exception
        //File imagesFolder = new File(storageDir, "camera");
        //imagesFolder.mkdirs();
        //File imageFile = new File(storageDir, imageFileName+".jpg");

        //
        //Avant l'extension '.jpg', il a insertion automatique d'un nombre aleatoire qui disparait avec 'imageFile.deleteOnExit()'
        //
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,     /* prefix */
                    ".jpg",     /* suffix */
                    storageDir        /* directory */
            );
        }catch (IOException ex){
            ex.getMessage();
        }

        // deletes file when the virtual machine terminate.
        imageFile.deleteOnExit();

        //  path used in mediastore column '_DATA'. See galleryAddPicture
        currentPhotoPath = imageFile.getAbsolutePath();//original

        return imageFile;
    }

    /*
    private void ConnectSSL() {
        HostnameVerifier myHostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        TrustManager[] trustAllCerts= new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

        }};

        SSLContext mySSLContext = null;
        try {
            mySSLContext = SSLContext.getInstance("TLS");
            try {
                mySSLContext.init(null, trustAllCerts, null);
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder().hostnameVerifier(myHostnameVerifier).sslSocketFactory(mySSLContext.getSocketFactory()).build();

        // default settings for all sockets
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
        IO.setDefaultOkHttpCallFactory(okHttpClient);

        // set as an option
        IO.Options opts = new IO.Options();
        opts.callFactory = okHttpClient;
        opts.webSocketFactory = okHttpClient;

        //socket = IO.socket("https://" + ADDRESS + ":PORT", opts);
        try {
            socket = IO.socket("https://murmuring-garden-67075.herokuapp.com/", opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        socket.connect();
    }
    */

    private void ConnectToHeroku() {

        Object[] objects = {"ack test"};
        socket.emit("update_item", objects, new Ack() {
            @Override
            public void call(Object... args) {
                //TODO process ACK
                String response = (String)args[0];
            }
        });

        //if it is a restart, we do not do the 'join'.
        socket.emit("join", "Smith");

        //implementing socket listeners when a user connect.
        socket.on("join_accepted", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String nickname = (String) args[0];     //user nickname who join
                        String idNickname = (String) args[1];   //id user who join
                    }
                });
            }
        });
    }

    private void getResponse() {
        URL url = null;
        String response;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    //Your code goes here
                    URL url = new URL("https://murmuring-garden-67075.herokuapp.com/");
                    String response = getResponseFromHttpUrl(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /**
     * Gets the response from http Url request
     * @param url
     * @return
     * @throws IOException
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.addRequestProperty("Accept","application/json");
        connection.addRequestProperty("Content-Type","application/json");
        connection.addRequestProperty("Authorization","Bearer <spotify api key>");

        try {
            InputStream in = connection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            connection.disconnect();
        }
    }

    public void addImageToGallery(final String filePath, final Context context) {
        /*
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        */
        //Mediastore method : working
        String imageFileName = "demo_demo";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.TITLE, imageFileName);
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName+".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.Media.DATA, filePath);
        //contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }

        ContentResolver resolver = getContentResolver();
        Bitmap bitmap;
        Uri uri;
        try {
            // Requires permission WRITE_EXTERNAL_STORAGE
            bitmap = BitmapFactory.decodeFile(filePath);
            uri    = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);


            //dump the mediastore cursor content of this bitmap. The output will be in logcat.
            Cursor cursor = resolver.query(uri, null, null, null, null);
            DatabaseUtils.dumpCursor(cursor);
        } catch (Exception e) {
            e.getMessage();
            Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "The image is not added to gallery",Snackbar.LENGTH_LONG).show();

            return;
        }

        try (OutputStream stream = resolver.openOutputStream(uri)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                throw new IOException("Error compressing the picture.");
            }
        } catch (Exception e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            e.getMessage();
        }

    }

    private void writeFileToInternalStorage(File file) {

        ContentResolver resolver = getContentResolver();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            return;
        }

        //If the Build.VERSION.SDK_INT is not >= Build.VERSION_CODES.Q, use fileprovider
        //target path
        //final String PDF_PATH = getFilesDir().getPath()+"/impot-2019-1.pdf";
        //final String PDF_PATH = getFilesDir().getPath()+"/IMG_20200526_191331-1.jpg";
        //final String PDF_PATH = getFilesDir().getPath()+"/media2-1.mp4";

        /*
        Environment.getExternalStorageDirectory(), if this were Windows, would return C:\.
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        if this were Windows, would return some standard location on the C:\ drive where the user would typically look to find saved movies.
         */

        //final String PDF_PATH = Environment.getExternalStorageDirectory().getPath()+"/media2-1.mp4";
        final String PDF_PATH = getFilesDir().getPath()+"/media2-1.mp4";
        Environment.getExternalStorageDirectory();          //--> /storage/emulated/0/Pictures, /storage/emulated/0/DCIM, ... ATTENTION : il y a une différence entre 'getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)'.
        Environment.getExternalStorageDirectory().list();   //-->Pictures, DCIM, Movies, ...
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); //--->/storage/emulated/0/Android/data/com.google.amara.chattab/files/Download

        try {
            //File outputFile = new File(PDF_PATH);     //internal storage
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/media2-5.mp4"); //--> se trouve dans sdCard/download ou storage/emulated/0/download
            //File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/media2-3.mp4");


            uri = FileProvider.getUriForFile(this, "com.google.amara.chattab.fileprovider", outputFile);

        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }

        try {

            int size = (int) file.length();
            byte[] bytes = new byte[size];

            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();

            OutputStream fos = resolver.openOutputStream(uri);

            fos.write(bytes);
            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @NonNull
    private Uri savePDFFile(@NonNull final Context context,
                            @NonNull InputStream in,
                            @NonNull final String mimeType,
                            @NonNull final String displayName,
                            @Nullable final String subFolder) throws IOException {

        //String relativeLocation = Environment.DIRECTORY_DOCUMENTS;
        String relativeLocation = Environment.DIRECTORY_DOWNLOADS;

        //if (!TextUtils.isEmpty(subFolder)) {
        //    relativeLocation += File.separator + subFolder;
        //}

        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
        contentValues.put(MediaStore.Video.Media.TITLE, "SomeName");
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        final ContentResolver resolver = context.getContentResolver();
        OutputStream stream = null;
        Uri uri = null;

        try {
            final Uri contentUri = MediaStore.Files.getContentUri("external");
            //uri = resolver.insert(contentUri, contentValues);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            }

            ParcelFileDescriptor pfd;
            try {
                //assert uri != null;
                pfd = getContentResolver().openFileDescriptor(uri, "w");
                assert pfd != null;
                FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor());

                byte[] buffer = new byte[4 * 1024];
                int len;
                while ((len = in.read(buffer)) > 0) {

                    out.write(buffer, 0, len);
                }
                out.close();
                in.close();
                pfd.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*
            contentValues.clear();
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(uri, contentValues, null, null);
            stream = resolver.openOutputStream(uri);
            if (stream == null) {
                throw new IOException("Failed to get output stream.");
            }
            */
            return uri;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

    }

    private void splitFile(String filePath) {
        ArrayList<File> files = new ArrayList<>();
        ArrayList<String> files64 = new ArrayList<>();
        File file = new File(filePath);
        FileInputStream fis;
        String newName;
        FileOutputStream chunk;
        int fileSize = (int) file.length();
        int Chunk_Size = 524288 * 2;
        int nChunks = 0, read = 0, readLength = Chunk_Size;
        byte[] byteChunk;
        String fname__ = getFilesDir().getPath()+"/sssdemo.wav"; //after decoding the base 64 string in the same place. Ne marche pas avec un fichier wav
        File ofile__ = new File(fname__);
        FileOutputStream fos__ = null;
        try {
            fis = new FileInputStream(file);
            fos__ = new FileOutputStream(ofile__, true);
            while (fileSize > 0) {
                if (fileSize <= Chunk_Size) {
                    readLength = fileSize;
                }
                byteChunk = new byte[readLength];

                    read = fis.read(byteChunk, 0, readLength);

                fileSize -= read;
                assert (read == byteChunk.length);
                nChunks++;
                newName = getFilesDir().getPath()+"/File.part000" + Integer.toString(nChunks - 1);
                files.add(new File(newName));
                chunk = new FileOutputStream(new File(newName));

                chunk.write(byteChunk);
                String chunk64 = Base64.encodeToString(byteChunk, Base64.DEFAULT);
                byte bytes[] = Base64.decode(chunk64, Base64.DEFAULT);

                files64.add(chunk64);
                fos__.write(bytes);
                fos__.flush();

                chunk.flush();
                chunk.close();
                //byteChunk = null;
                //chunk = null;
            }
            fis.close();
            //fis = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }

        //join base64 string.
        String fname_ = getFilesDir().getPath()+"/ssdemo.wav";//The final file containing the joined parts
        File ofile_ = new File(fname_);
        FileOutputStream fos_ = null;
        //FileInputStream fis;
        byte[] fileBytes_;
        int bytesRead_ = 0;
        try {
            fos_ = new FileOutputStream(ofile_, true);
            for (String s : files64) {
                byte bytes[] = Base64.decode(s, Base64.DEFAULT);
                fos_.write(bytes);
                fos_.flush();
            }
            fos_.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }

        //join byte arrays contained in arrayList 'files' the parts are 'File.part000'
        String fname = getFilesDir().getPath()+"/sdemo.wav"; //final name when all is joined
        File ofile = new File(fname);
        FileOutputStream fos = null;

        //FileInputStream fis;
        byte[] fileBytes;
        int bytesRead = 0;
        try {
            fos = new FileOutputStream(ofile, true);
            for (File file_ : files) {
                fis = new FileInputStream(file_);
                fileBytes = new byte[(int) file_.length()];
                bytesRead = fis.read(fileBytes, 0, (int) file_.length());
                assert (bytesRead == fileBytes.length);
                assert (bytesRead == (int) file_.length());
                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;
                fis.close();
                fis = null;
            }
            fos.close();
        }catch (FileNotFoundException e) {
                e.printStackTrace();
        }catch (IOException e) {
                e.printStackTrace();
        }
    }

    @Override
    public void onStart() {//after restart, we come here and do this twice. We jump the 'join' event below.
        super.onStart();
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //checkSelfPermission(String) come from 'getApplicationContext().checkSelfPermission(String)'
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                //Show a dialogue with two buttons : OK and Cancel
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    return;
                    //if (googleApiClient != null) {
                    //    googleApiClient.connect();
                    //}
                }

                break;
        }
    }

    public void initSocket_______() {
        currentChatUser = new ChatUser(
                Nickname,
                "0",
                "",
                0,  //status
                0,  //notSeenMessagesNumbe
                0, //connectedAt
                0, //disconnectedAt
                0, //lastConnectedAt
                null //blacklistAuthor
        );
        lastSessionUsers = new ChatUser[1];
        lastSessionUsers[0] = currentChatUser;
        initSocket4();
    }

    //
    //called when fragment is attached 'getSupportFragmentManager().addFragmentOnAttachListener' parce que
    // if 'initSocket' is called  from 'onCreate', 'getSupportFragmentManager' ne donne pas les deux fragments 'ChatBoxActivity' et 'ChatBoxMessage'
    // cf 'SectionPagerAdapter'.
    //
    public void initSocket() {
        if(currentChatUser != null){
            //Update the 'currentChatUser' image profile.
            currentChatUser.imageProfile = imageProfile;

            //save the 'currentChatUser' in local db. Remotely, it will be saved in 'join' in 'initSocket4' event.
            int row = saveChatUser(currentChatUser);
            if(row == 0) throw new UnsupportedOperationException("'Save the current chat user in local db'");

            //get the last session user names who contact 'Nickname' from 'chat_last_users' table in local db
            // -- sqlite --
            //table 'lastusers' is modified in 'tabchatActivity/sendMessage'
            //table 'users' is not modified, it dosn't contain a column 'connectedwith'
            // -- server --
            //table 'lastusers' is not modified
            //table 'users' contains column 'connectedwith' and then it is modified in 'message_detection'
            //
            Map<String, Long> lastSessionUsers_ = getLastSessionUsers(Nickname);

            //if(lastSessionUsers_ == null){
            //    //get last users from server
            //    initSocket0();
            //    //initSocket4(null);
            //    return;
            //}

            //build 'lastUsers' objects and do 'join'
            if(!lastSessionUsers_.isEmpty()){
                buildLastUsers(lastSessionUsers_);
                return;
            }

            //here 'lastSessionUsers_' map is empty in local sqlite db. Try the server. and do join
            //the response is found in 'setListen_on_get_last_users_back'.
            socket.emit("get_last_users", Nickname);
            return;
        }

        /*
        //get the current user from server
        //Ask server the names of last session users who contact 'Nickname'
        socket.emit("get_user", Nickname);
        socket.on("get_user_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);

                        //Save this user in local db
                        int nbRows = saveUser(user);

                    }
                });
            }
        });
        */
        /*
        //No user found in local db
        //build a default ChatUser
        currentChatUser = new ChatUser(
                Nickname,
                "",
                imageProfile,
                0,
                0,
                connectionTime,
                0,
                0,
                null
        );

        int row = saveChatUser(currentChatUser);
        if(row == 0) throw new UnsupportedOperationException("'Save the default chat user in local db'");
        */

        //here the 'currentChatUser' object is null. if the account had been deleted, the 'currentChatUser'
        //may be rebuilt from server'
        initSocket0();
        //initSocket4(null);
    }

    private void buildLastUsers(Map<String, Long> lastSessionUsers_) {
        //build the 'ChatUser' object of last session users list from the 'chat_users' local table
        if(lastSessionUsers_ != null)lastSessionUsers = getChatUsers(lastSessionUsers_.keySet());

        //get lastSessionUsers from enum 'LastUsersDataHolder'
        //lastSessionUsers =
        //ChatUser[]lastSessionUsers = new ChatUser[0];
        if(MainActivity.LastUsersDataHolder.hasData()){
            List<Object> list = MainActivity.LastUsersDataHolder.getData();
            ArrayList arrayList = new ArrayList(list);
            Object[] lastSessionUsers__  = (Object[])arrayList.toArray();
            lastSessionUsers = new ChatUser[lastSessionUsers__.length];
            int i = 0;
            for(Object lastSessionUsers___ : lastSessionUsers__){
                lastSessionUsers[i] = lastSessionUsers___ instanceof ChatUser ? ((ChatUser) lastSessionUsers___) : null;;
                i++;
            }
        }

        //update the 'lastSessionUsers' list with the 'notSeenMessagesNumber' supplied in 'notSeenMessages'
        //assert lastSessionUsers != null;
        if(lastSessionUsers != null){
            for(ChatUser chatUser : lastSessionUsers){
                int i = chatUser.notSeenMessagesNumber;
                for (NotSeenMessage notSeenMessage : notSeenMessages) {
                    String notSeenMessageAuthor = notSeenMessage.nickname;
                    int nbNotSeenMessages = Integer.parseInt(notSeenMessage.nbMessages);

                    if (notSeenMessageAuthor.equals(chatUser.nickname)) {
                        i = i + nbNotSeenMessages;
                        chatUser.notSeenMessagesNumber = i;
                        break;
                    }
                }
            }
        }

            /*//dispatch the last session users
            for(ChatUser chatUser : lastSessionUsers){
                //chatBoxActivity.displayReceivedNewUser(chatUser);
                //chatBoxMessage.setChatListUsers(chatUser);
            }*/

        initSocket4();
        return;
    }

    public void initSocket0() {
        /*
        //Get only the names  of last session users who contact 'Nickname' from local db
        Map<String, Long> lastSessionUsersMap = getLastSessionUsers(Nickname);
        if(lastSessionUsersMap != null) {
            //Get the data of of last session users who contact 'Nickname' from local db
            Set<String> keys = lastSessionUsersMap.keySet();
            ChatUser[] chatUsers = getChatUsers(keys);
            initSocket2(chatUsers);
            return;
        }
        */

        //Ask server to get the current user named 'Nickname'
        socket.emit("get_user", Nickname);
    }


    //get last session users who contact 'Nickname'.
    private void setListen_on_get_users_back(){

        //Ask the server to get the last session users who contact 'Nickname'.
        socket.on("get_users_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray users = ((JSONArray) args[0]);
                        //System.out.println("users = " + users.length()+" ******************");
                        initSocket5(users);
                    }
                });
            }
        });
    }


    //response from : 'socket.emit("get_user", Nickname);'
    private void setListen_on_get_user_back(){

        //get the response from the server and get the names of last session users who contact 'Nickname'
        // we come here from 'initSocket0'.
        socket.on("get_user_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);
                        if(user != null){
                            //Build and save the current user in local db
                            JSONObject connectedWith = null;

                            //build the the deleted chat user
                            try {
                                String nickname         = user.getString("nickname");
                                String imageProfile_    = user.isNull("imageprofile") ? null : user.getString("imageprofile");
                                int status              = user.getInt   ("status");
                                int notSeenMessages     = user.getInt   ("notseenmessages");
                                long connectedAT        = user.getLong  ("connected");
                                long disconnectedAt     = user.getLong  ("disconnected");
                                long lastConnectedAt    = user.getLong  ("lastconnected");
                                String blacklistAuthor  = user.isNull("blacklistauthor") ? null : user.getString("blacklistauthor");;
                                connectedWith           = user.isNull("connectedwith") ? null : user.getJSONObject("connectedwith");

                                currentChatUser = new ChatUser(
                                        nickname,
                                        null,
                                        imageProfile_,
                                        status,
                                        notSeenMessages,
                                        connectedAT,
                                        disconnectedAt,
                                        lastConnectedAt,
                                        blacklistAuthor
                                );

                                //update this chatUser
                                //currentChatUser.imageProfile    = (imageProfile != null) ? imageProfile : imageProfile_;
                                currentChatUser.imageProfile    = imageProfile_;
                                currentChatUser.lastConnectedAt = currentChatUser.connectedAt;
                                currentChatUser.connectedAt     = connectionTime;

                                //save this chatUser in local db
                                int row = saveChatUser(currentChatUser);
                                if(row == 0)throw new UnsupportedOperationException("save chat user");

                                //Get last session users who contact this currentChatUser
                                initSocket3(user);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            //get the last session users for this chatUser from server
                            assert connectedWith != null;
                            initSocket3(connectedWith);
                            //aa
                            return;
                        }
                        //build the first time 'currentChatUser'.
                        currentChatUser = new ChatUser(
                                Nickname,
                                null,
                                imageProfile,
                                ChatUser.userConnect,
                                0,
                                connectionTime,
                                0,
                                0,
                                null
                        );
                        //save the 'currentChatUser' locally. Remotely it is saved in server in 'join'
                        int row = saveChatUser(currentChatUser);
                        if(row == 0) throw new UnsupportedOperationException("'Save the first time user in local db'");

                        //Since it is the first time, there is no last session users.
                        initSocket4();
                    }
                });
            }
        });

        /*
        //the server send the last session users who contact 'Nickname'.
        socket.on("get_users_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray users = ((JSONArray) args[0]);
                        //System.out.println("users = " + users.length()+" ******************");
                        initSocket5(users);
                    }
                });
            }
        });
        */
    }


    /**
     *
     * @param user a ChatUser Json formatted
     * @return number of rows saved
     */
    private int saveUser(JSONObject user) {
        // 'chat_users' table fields
        //,nickname TEXT NOT NULL UNIQUE,
        // imageprofile BLOB NOT NULL,
        // status INTEGER NOT NULL,
        // notseen INTEGER NOT NULL,
        // connected INTEGER NOT NULL,
        // disconnectedat INTEGER NOT NULL,
        // blacklistauthor TEXT
        ContentValues values = new ContentValues();
        try {
            values.put(UsersContract.COLUMN_USER_NAME,          user.getString("nickname"));
            if(user.isNull("imageprofile")){
                values.putNull(UsersContract.COLUMN_PROFILE);
            }else{
                values.put(UsersContract.COLUMN_PROFILE,        user.getString("imageprofile"));
            }
            values.put(UsersContract.COLUMN_STATUS,             user.getInt("status"));
            values.put(UsersContract.COLUMN_NOT_SEEN,           user.getInt("notseenmessages"));
            values.put(UsersContract.COLUMN_CONNECTED,          user.getLong("connected"));
            values.put(UsersContract.COLUMN_DISCONNECTED_AT,    user.getLong("disconnected"));
            values.put(UsersContract.COLUMN_BLACKLIST_AUTHOR,   user.getString("blacklistauthor"));
            //values.put(UsersContract.COLUMN_CONNECTEDWITH,      user.getJSONObject("connectedwith"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        INSERT INTO phonebook2(name,phonenumber,validDate)
        VALUES('Alice','704-555-1212','2018-05-08')
        ON CONFLICT(name) DO UPDATE SET
        phonenumber=excluded.phonenumber,
        validDate=excluded.validDate
        WHERE excluded.validDate>phonebook2.validDate;

        INSERT INTO chat_users (nickname, imageprofile, status, notseen, connected, disconnectedat, blacklistauthor)
        VALUES('bison', '', 1, 100, 0, 0, NULL) ON CONFLICT(nickname) DO UPDATE SET
        notseen=excluded.notseen
        WHERE excluded.nickname='bis';
        ;
         */

        ContentResolver cr  = getContentResolver();
        int numRows = 0;

        /*
        if(chatUser.imageProfile.equals(null)){
            //update
            numRows = cr.update(UsersContract.CONTENT_URI_USERS,
                    values,
                    UsersContract.COLUMN_USER_NAME + " =? ",
                    new String[]{chatUser.nickname});
        }else{
            // insert the image profile
            Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);
            if(newRowUri != null) numRows = 1;

            //if row exists, update
            if(newRowUri == null)numRows = cr.update(UsersContract.CONTENT_URI_USERS,
                    values,
                    UsersContract.COLUMN_USER_NAME + " =? ",
                    new String[]{chatUser.nickname});
        }
        */
        Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);
        if(newRowUri != null) numRows = 1;
        return numRows;
    }

    //Build the 'ChatUser' object list of users who contact 'Nickname' last session and save them
    // in 'chat_last_users'  local table.
    public void initSocket5(JSONArray users) {
        lastSessionUsers = null;
        if(null != users){

            lastSessionUsers = new ChatUser[users.length()];
            int nbSavedUsers = 0; //number of saved users
            for(int i = 0; i < users.length(); i++){
                JSONObject userData = null;
                try {
                    userData = users.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String nickname             = null;
                String imageProfile         = null;
                int status                  = 0;
                long connectedAt            = 0;
                long lastConnectedAt        = 0;
                long disconnectedAt         = 0;
                String blacklistAuthor      = null;
                int notSeenMessages         = 0;
                JSONObject connectedWith    = null;

                //[{"nickname":"bis","imageprofile":"[B@b100de4","status":"000",
                // "connected":"1635329963888","lastconnected":"0","disconnected":"1635254341316",
                // "blacklistauthor":null,"notseenmessages":0,
                // "connectedwith":{"ter":1635340703489}},{"nickname":"mono","imageprofile":"[B@c69924d","status":"000","connected":"1635760982739","lastconnected":"0","disconnected":"1635762373845","blacklistauthor":null,"notseenmessages":0,"connectedwith":{"ter":1635411616304}}]
                try {
                    nickname        = userData.getString("nickname");
                    imageProfile    = userData.isNull("imageprofile") ? null : userData.getString("imageprofile");
                    status          = userData.getInt("status");
                    connectedAt     = userData.getLong("connected");
                    lastConnectedAt = userData.getLong("lastconnected");
                    disconnectedAt  = userData.getLong("disconnected");
                    blacklistAuthor = userData.isNull("blacklistauthor") ? null : userData.getString("blacklistauthor");
                    notSeenMessages = userData.getInt("notseenmessages");
                    connectedWith   = userData.isNull("connectedwith") ? null : userData.getJSONObject("connectedwith") ;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //build the chatUser
                lastSessionUsers[i] = new ChatUser(
                        nickname,
                        null,
                        imageProfile,
                        status,
                        notSeenMessages,
                        connectedAt,
                        disconnectedAt,
                        lastConnectedAt,
                        blacklistAuthor
                );

                //Save this user in local db
                int j = saveUser(userData);
                if(j != 0)nbSavedUsers ++;
            }

            if(users.length() != nbSavedUsers)throw new UnsupportedOperationException("'Get and save users from server'");

        }


        /*
        for(ChatUser chatUser : lastSessionUsers){
            chatBoxActivity.displayReceivedNewUser(chatUser);
            chatBoxMessage.setChatListUsers(chatUser);

        }
        */
        //Display the last session users
        initSocket2(lastSessionUsers);

        /*
        for(ChatUser chatUser : lastSessionUsers) {
            //chatBoxActivity.displayReceivedNewUser(chatUser);
            //chatBoxMessage.setChatListUsers(chatUser);
        }
         */
    }

    //Dispatch the 'ChatUser' object
    public void initSocket2(ChatUser[] lastSessionUsers) {
        //the following part do the 'join', ...
        initSocket4();


        //contourner le tag
        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        int index  = 0;
        int index_ = 0;
        int i      = 0;

        //the 'sectionsPagerAdapter' method is not used
        /*
        for (int i = 0; i <= sectionsPagerAdapter.getCount() - 1; i++) {
            Fragment fragment = sectionsPagerAdapter.getItem(i);
            if (ChatBoxActivity.class.isInstance(fragment)) index = i;
            if (ChatBoxMessage.class.isInstance(fragment)) index_ = i;
        }
        */


        for(Fragment f0 : fragments){
            if(ChatBoxActivity.class.isInstance(f0)) index = i;
            if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
            i++;
        }

        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

        //ChatBoxActivity f = (ChatBoxActivity) sectionsPagerAdapter.getItem(index);
        //ChatBoxMessage f_ = (ChatBoxMessage)  sectionsPagerAdapter.getItem(index_);

        for(ChatUser chatUser : lastSessionUsers){
            f.displayReceivedNewUser(chatUser);
            f_.setChatListUsers(chatUser);
        }
    }

    /**
     * we know the names of last session users who contact 'Nickname'. they are in 'JSONObject user'.
     *  'connectedwith' column.
     *   now ask the server for the corresponding 'users' object and save them in local db.
     * @param user json object with a key 'connectedwith' that contains the name of all users who
     *        contact 'Nickname' (current user).
     */

    public void initSocket3( JSONObject user) {

        JSONObject users = null;
        try {
            users = user.getJSONObject("connectedwith");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //put the users name and time in map
        Map<String,Long> usersName = new HashMap<>();
        if(users != null) {
            Iterator<String> iterator = users.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                long value = 0;
                try {
                    value = (Long)users.get(key);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                usersName.put(key, value);
            }

            //sort the map elements with 'order'
            final boolean order = false; // true=from little to big
            List<Map.Entry<String, Long>> list = new ArrayList<>(usersName.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
                public int compare(Map.Entry<String, Long> o1,
                                   Map.Entry<String, Long> o2) {
                    if (order) {
                        return o1.getValue().compareTo(o2.getValue());
                    } else {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                }
            });

            //sort the map
            Map<String,Long> usersName_ = new LinkedHashMap<>();
            for (Map.Entry<String, Long> element : list) {
                usersName_.put(element.getKey(), element.getValue());
            }

            //Todo : Save or update the 'Nickname' chatUser in local db with
            // the users that connect it.
            int nbRows = saveLastSessionUsers(Nickname, usersName_);
            if(nbRows != usersName_.size())throw new UnsupportedOperationException("'initSocket3' saved users mismatch");

            //Next, prepare to ask server for data of last session users object.
            Object[] usersObject = usersName_.keySet().toArray();

            String[] usersArray = Arrays.asList(usersObject).toArray(new String[usersObject.length]);

            //convert array to json
            JSONArray usersJSON = new JSONArray();
            for (int i = 0; i < usersArray.length; i++) {
                usersJSON.put(usersArray[i]);
            }
            socket.emit("get_users", usersJSON); // the reponse of this 'emit' will be found
            //in 'socket.on("get_users_back"' event which redirect to 'initSocket5()'
        }
    }

    private int saveLastSessionUsers(String nickname, Map<String, Long> usersMap) {

        ContentResolver contentResolver = getContentResolver();
        ContentValues values = new ContentValues();
        Iterator<String> iterator = usersMap.keySet().iterator();
        int nbRows = 0;
        Uri newRowUri = null;
        while(iterator.hasNext()){

            String key = iterator.next();
            Long value = usersMap.get(key);

            values.put(LastUsersContract.COLUMN_USER_ORIGINE, nickname);
            values.put(LastUsersContract.COLUMN_USER_TARGET, key);
            values.put(LastUsersContract.COLUMN_TIME, value);

            newRowUri = contentResolver.insert(LastUsersContract.CONTENT_URI_LAST_USERS, values);
            if(newRowUri != null)nbRows++;
            newRowUri = null; //for the next iteration, reset 'newRowUri'.
        }
        return nbRows;
    }

    public void initSocket4() {
        //super.onStart();
        //connect your socket client to the server
        //public static final String URL = "http://10.0.2.2:8080";      //Emulator
        //public static final String URL = "http://localhost:8080";       //Device
        //public static final String URL = "https://dry-springs-89362.herokuapp.com/";
        //socket = IO.socket("http://10.0.2.2:3000"); //emulator
        //socket = IO.socket("http://localhost:3000");  //device

        //Default bitmap
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.trump);
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic);
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.maison_20180508_153845_original);

        //Send the server a query to register. Back in 'userjoinedthechat', it send us an 'id'
        //We can also get the 'id' in callback
        //Prepare data to send in 'emit'

        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("Nickname", currentChatUser.nickname);
            jsonObjectData.put("Status", currentChatUser.status); //0=gone, 1=connect, 2=standby, 3=blacklist, 4=not assigned
            jsonObjectData.put("ConnectionTime", currentChatUser.connectedAt);
            jsonObjectData.put("LastConnectionTime", currentChatUser.lastConnectedAt);
            jsonObjectData.put("DisconnectionTime", currentChatUser.disconnectedAt);
            jsonObjectData.put("NotSeenMessages", currentChatUser.notSeenMessagesNumber);
            jsonObjectData.put("BlacklistAuthor", currentChatUser.blacklistAuthor == null ? JSONObject.NULL : currentChatUser.blacklistAuthor);
            jsonObjectData.put("Profile", (currentChatUser.imageProfile == null) ? JSONObject.NULL : currentChatUser.imageProfile);
            jsonObjectData.put("ProfileUri", (imageProfileUri == null) ? JSONObject.NULL : imageProfileUri);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
        //build the json from 'currentChatUser' object to send to server
        Gson gson = new Gson();
        String jsonChatuser = null;
        JSONObject jsonChatuserObject = null;
        try {
            jsonChatuser = gson.toJson(currentChatUser);
        }catch (Exception e){
            e.getStackTrace();
        }
        try {
            //Le JSONObject est construit à partir des propriétés de l'objet 'currentChatUser'
            jsonChatuserObject = new JSONObject(jsonChatuser);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        */

        String[] data = {jsonObjectData.toString()};
        //JSONObject[] data = {jsonObjectData};

        //setListen_join();
        //socket.emit("join", Nickname, Profile, connectionTime, lastConnectionTime);
        //socket.emit("join", data);//marche, mais erreur dans le build
        socket.emit("join", (Object []) data);

        /*
        socket.emit("join_", data, new Ack() {
            @Override
            public void call(Object... args) {
                //TODO process ACK
                String response = (String) args[0];
                if (!response.equals("success")) {
                    //Notify the user who join his infos are not sent.
                    Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content),
                            Nickname + " infos are not saved on server", Snackbar.LENGTH_LONG).show();
                }

                //contourner le flag
                //Get list of fragments
                List<Fragment> fragments = getSupportFragmentManager().getFragments();

                int index = 0;
                int index_ = 0;
                int i = 0;

                for (Fragment f0 : fragments) {
                    if (ChatBoxActivity.class.isInstance(f0)) index = i;
                    if (ChatBoxMessage.class.isInstance(f0)) index_ = i;
                    i++;
                }

                ChatBoxActivity f = (ChatBoxActivity) fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage) fragments.get(index_);

                //ChatBoxActivity f = (ChatBoxActivity) sectionsPagerAdapter.getItem(index);
                //ChatBoxMessage f_ = (ChatBoxMessage)  sectionsPagerAdapter.getItem(index_);

                    //
                    //for(ChatUser chatUser : chatUsers){
                    //    f.displayReceivedNewUser(chatUser);
                    //    f_.setChatListUsers(chatUser);
                    //}

            }
        });
        */

        //messageList = new ArrayList<>();

        //setting up recyler

        //RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        //myRecylerView.setLayoutManager(mLayoutManager);
        //myRecylerView.setItemAnimator(new DefaultItemAnimator());

        // message send action
        /*
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //retrieve the nickname and the message content and fire the event 'messagedetection
                if (!messagetxt.getText().toString().isEmpty()) {
                    //get the chat id of 'bis'
                    Iterator<ChatUser> iterator = chatUserList.iterator();
                    while (iterator.hasNext()) {
                        ChatUser chatUser = iterator.next();
                        if (chatUser.getNickname().equals("bis")) {
                            toNicknameid = chatUser.getId();
                            break;
                        }
                    }
                    socket.emit("messagedetection", Nickname, Id, toNicknameid, messagetxt.getText().toString());
                    messagetxt.setText(" ");
                }
            }
        });
        */


        /*
        socket.on("get_user_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);
                    }
                });
            }
        });
        */



        //we receive blacklist notification
        socket.on("blacklist", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String authorBlacklist = (String) args[0];
                        String statusBlacklist = (String) args[1];

                        //update connected users list
                        //contourner le flag
                        //Get list of fragments
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        //ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        f.displayReceivedBlacklist( authorBlacklist, statusBlacklist);
                        //f_.setChatListUsers( chatUser);

                    }
                });
            }
        });


        /*
        //Received a logoff notification from the server.
        socket.on("logoff", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject notification = (JSONObject) args[0];
                        try {
                            String raison = notification.getString("reason");   //{reason: xxx}
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        */

        //Received notification that the sent messages are seen (read).
        socket.on("messages_read_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String toNickname = (String) args[0];

                        //update the messages
                        //Get list of fragments to update the remaining users.
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        //f.displayReceivedDiconnectUser(nickname);

                        //The update is done in 'ChatBoxAdapter'
                        f_.updateMessages(Nickname, toNickname); //from = 'Nickname', to = 'toNickname'
                    }
                });
            }
        });


        // getting messages from server after emitting 'get_messages'
        socket.on("get_messages_res", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray messages = (JSONArray) args[0];
                        ArrayList<Message> arrayListMessage =  new ArrayList<>();
                        //Ack callback = (Ack)args[0];
                        for(int i = 0; i <= messages.length() - 1; i++){
                            Message message = null;
                            try {
                                //Il faut que les champs de la table 'messages' dans la bd soient
                                // identiques aux champs de l'objet 'Message' pour pouvoir utiliser l'instruction suivante
                                //message = new Gson().fromJson(messages.getJSONObject(i).toString(), Message.class);

                                String fromnickname = messages.getJSONObject(i).getString("fromnickname");
                                String tonickname   = messages.getJSONObject(i).getString("tonickname");
                                String message_     = messages.getJSONObject(i).getString("message");
                                String time_        = messages.getJSONObject(i).getString("time");
                                long time           = Long.parseLong(time_);
                                String extra        = messages.getJSONObject(i).getString("extra");     //may be empty
                                String extraname    = messages.getJSONObject(i).getString("extraname"); //may be empty
                                String mime         = messages.getJSONObject(i).getString("mime");      //may be empty
                                String ref          = messages.getJSONObject(i).getString("ref");
                                String seen         = messages.getJSONObject(i).getString("seen");
                                String deletedfrom  = messages.getJSONObject(i).getString("deletedfrom");
                                String deletedto    = messages.getJSONObject(i).getString("deletedto");

                                if(messages.getJSONObject(i).isNull("extra")){
                                    extra      = null;
                                    extraname  = null;
                                    mime       = null;
                                }

                                message = new Message(fromnickname,
                                        tonickname,
                                        message_,
                                        time,
                                        extra,
                                        extraname,
                                        ref,
                                        mime,
                                        seen,
                                        deletedfrom,
                                        deletedto
                                );

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            arrayListMessage.add(message);
                        }

                        //send the arraylist to 'ChatBoxMessage.getMessagesFromServerRes'
                        //Get list of fragments to update the remaining users.
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        //f.displayReceivedDiconnectUser(nickname);
                        f_.getMessagesFromServerRes(arrayListMessage);
                    }
                });
            }
        });

        //the server send a user disconnect
        socket.on("userdisconnect", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //extract data from fired event
                        JSONObject jsonObject = (JSONObject) args[0]; //the user who disconnect
                        String disconnectNickname = null;
                        String time = null;
                        try {
                            disconnectNickname = jsonObject.getString("name");
                            time = jsonObject.getString("time");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //get the disconnect 'chatUser' with name is 'disconnectNickname' from local db.
                        ChatUser disconnectUser = getChatUser(disconnectNickname);

                        //update the 'disconnectUser' status value.
                        disconnectUser.status         = 0; //0=disconnect, 1= connect, 2=standby, 3=blacklist
                        disconnectUser.disconnectedAt = Long.parseLong(time);

                        //save the updated 'disconnectUser' in local db. In the server, it has been saved
                        //in 'disconnect' event.
                        int rows = saveChatUser(disconnectUser);
                        if(rows != 1)  throw new UnsupportedOperationException("Unexpected error at : 'save disconnected user'");

                        //Get list of fragments to update the remaining users.
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        f.displayReceivedDiconnectUser(disconnectNickname);
                        f_.displayReceivedDiconnectUser(disconnectNickname);

                        //notify
                        Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), disconnectNickname + " is gone", Snackbar.LENGTH_LONG).show();

                        //update the adapter of view pager
                        //sectionsPagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        //server send a 'standby' notification
        socket.on("user_standby_back", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //extract data from fired event
                        String standbyNickname = (String) args[0]; //the user who is standby

                        if(standbyNickname.equals(Nickname)) return; //it is me who emit the 'standby' event

                        //it is not me. someone else is standby
                        //get the standby 'chatUser' witch name is 'standbyNickname'
                        ChatUser standbyUser = getChatUser(standbyNickname);

                        //update the 'standbyUser' status value.
                        standbyUser.status = 2; //0=disconnect, 1= connect, 2=standby, 3=blacklist

                        //Get list of fragments to notify the remaining users.
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        f.displayReceivedStandbyUser(standbyNickname);
                        f_.displayReceivedStandbyUser(standbyNickname);

                        //notify
                        //Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), nickname + " is standby", Snackbar.LENGTH_LONG).show();


                        //update the adapter of view pager
                        //sectionsPagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        //the server send back standby
        socket.on("userbackstandby", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //extract data from fired event
                        String backStandbyNickname = (String) args[0]; //the user who is coming back from standby

                        //get the back standby 'chatUser' with name is 'backStandbyNickname'
                        ChatUser backStandbyUser = getChatUser(backStandbyNickname);

                        //update the 'backStandbyUser' status value.
                        backStandbyUser.status = 1; //0=disconnect, 1= connect, 2=standby, 3=blacklist

                        //Get list of fragments to update the remaining users.
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        f.displayReceivedBackStandbyUser(backStandbyNickname);
                        f_.displayReceivedBackStandbyUser(backStandbyNickname);

                        //notify
                        //Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), nickname + " is back", Snackbar.LENGTH_LONG).show();

                        //update the adapter of view pager
                        //sectionsPagerAdapter.notifyDataSetChanged();
                    }
                });
            }
        });


       /*
        socket.on("downloadFileMoreData", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0]; //'Name', 'Place', 'Percent'
                        //extract data from fired event.
                        String filename = "";
                        String data     = "";
                        int place       = 0;
                        int percent     = 0;
                        try {
                            filename    = json_data.getString("Name");
                            data        += json_data.getString("Data");
                            downloadParts.add(data);
                            content     += data;
                            place       = json_data.getInt("Place");
                            percent     = json_data.getInt("Percent");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        */

        //Prepare to download attachment data from server
        socket.on("download_chunks", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0]; //'Ref','Size', 'Data'
                        //extract data from fired even
                        String ref  = "";
                        String data = "";
                        int size    = 0;
                        //int place = 0;
                        try {
                            ref   = json_data.getString("Ref");
                            size  = json_data.getInt("Size");
                            data  = json_data.getString("Data");
                            data  = data.replaceAll("\\\\", "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Create a new Entry in a JSON 'downloadedChunk' Variable witch contain
                        // downloaded chunks.
                        try {
                            downloadedChunk.put("Ref", ref);
                            downloadedChunk.put("Size", size);
                            downloadedChunk.put("Data", "");
                            downloadedChunk.put("Downloaded", 0); //total amount downloaded
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Create a JSON to send to server
                        JSONObject chunkStatus    = new JSONObject();
                        try {
                            chunkStatus.put("File", index);
                            chunkStatus.put("Place", 0);
                            chunkStatus.put("Percent", 0);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Show the progress bar
                        progressDialog.show();
                        progressDialog.setTitle("Downloading : ");
                        progressDialog.setProgress(0);  //reset the progress bar since it has been used in 'upload'.

                        socket.emit("downloadFileMoreData", chunkStatus);
                        index++; //index de chunks


                        //getResources().getDrawable(R.drawable.demo_carre_vert);
                        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.demo_carre_vert);
                        /*
                        byte[] decodedByte = Base64.decode(data, 0);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
                        thumbAttachmentFrom.setImageBitmap(bitmap);

                        Dialog dialog1 = new Dialog(TabChatActivity.this); //Do not name dialog because the DialogAlert onClick positive button is named dialog.
                        dialog1.show();

                        //Enlarge the view to screen width.
                        View enlargedView = enlargeView(bitmap, dialog1);
                        */
                        //decode base64Data and send it to 'ChatBoxMessage' to update the 'Message' object. The it is displayed in 'ChatBoxAdapter'.
                        //Get list of fragments.
                        /*
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }
                        //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        //f.displayReceivedBackStandbyUser("");
                        f_.displayReceivedAttachment(fromNickname, toNickname, data);
                        */
                    }
                });
            }
        });

        //download chunks from the server then save the full file in mediastore (not local database).
        socket.on("download_file_chunks", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0]; //'Data'

                        //playload     = Buffer.from(base64Data, 'base64').toString('binary');

                        //extract data from fired even
                        int index   = 0;
                        String ref  = "";
                        String data = "";
                        int size    = 0;
                        try {
                            index  = json_data.getInt("File");
                            ref    = json_data.getString("Ref");
                            size   = json_data.getInt("Size");
                            data   = json_data.getString("Data");
                            //data = data.replaceAll("\\\\", "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Update the JSON
                        try {
                        int downloaded_ = downloadedChunk.getInt("Downloaded");
                        downloaded_  += data.length();

                        String data_ = downloadedChunk.getString("Data");
                        data_ += data;

                        downloadedChunk.put("Ref", ref);
                        downloadedChunk.put("Size", size);
                        downloadedChunk.put("Data", data_ );
                        downloadedChunk.put("Downloaded", downloaded_);

                        //Show the progress bar
                        //progressDialog.show();

                        //Percent
                        int percent_ = downloadedChunk.getInt("Downloaded") / downloadedChunk.getInt("Size");
                        //handle.sendMessage(handle.obtainMessage());

                        //if (progressDialog.getProgress() == progressDialog.getMax()) progressDialog.dismiss();

                        if(downloadedChunk.getInt("Downloaded") == downloadedChunk.getInt("Size")) { //If File is Fully Uploaded
                            //The download is complete, hide the progress bar.
                            progressDialog.hide();

                            //Notify server we are done.
                            socket.emit("downloadFileComplete", "{ 'IsSuccess' : true }");

                            //Get mime type of attachment from local database from table 'message'.
                            String mimeType =
                                getMimeType(downloadedChunk.getString("Ref"));

                            //Todo if(mimeType.equals("") or (mimeType == null)

                            //Get the type or extension. It is needed in intent displaying attachment
                            String type = getType(mimeType);

                            String intentType = null;
                            switch(type) {
                                case "pdf":
                                    intentType = "application/pdf";
                                    break;
                                case "mp3":
                                    intentType = "audio/*";
                                    break;
                                case "jpg":
                                    intentType = "image/jpg";
                                    break;
                                case "jpeg":
                                    intentType = "image/jpg";
                                    break;
                                case "png":
                                    intentType = "image/png";
                                    break;
                                case "txt":
                                    intentType = "text/plain";
                                    break;
                                default:
                                    // Not supported type
                                    intentType = null;
                            }


                            //Get base-64 string image file
                            String imageFile = downloadedChunk.getString("Data").toString();

                            //Save the base64 string in file system with file provider
                            Uri uri = saveBase64String(imageFile, mimeType, ref);

                            //Save in gallery the Bitmap built from base-64 string and add an entry
                            // in 'Preferences'
                            //save the pdf document in 'Downloads' folder and add entry in 'Preferences'

                            /*
                            switch (mimeType){
                                case("jpg") :
                                case("jpeg"):
                                    galleryAddPicture(uri, ref);
                                    break;
                                case("pdf"):
                                    downloadAddFile(uri, ref);
                                    break;
                            }
                            */

                            //Save localy in sqlite db the Bitmap built from base-64 string.
                            //sqliteAddPicture(uri);

                            //display the attachment in intent.
                            showAttachment(null, uri, mimeType);

                            /*
                            Intent target = new Intent(Intent.ACTION_VIEW);

                            target.setDataAndType(uri, intentType); //"application/pdf", "audio/*; "image/png", "image/jpg", "image/jpeg"
                            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            Intent intent = Intent.createChooser(target, "Open File");
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                // Instruct the user to install a PDF reader here, or something
                            }
                            */
                           /*
                            //Display the image in dialog
                            byte[] decodedByte = Base64.decode(imageFile, 0);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
                            thumbAttachmentFrom.setImageBitmap(bitmap);
                            */
                            /**
                            Dialog dialog1 = new Dialog(TabChatActivity.this); //Do not name dialog because the DialogAlert onClick positive button is named dialog.
                            dialog1.show();

                            //Enlarge the view to screen width.
                            View enlargedView = enlargeView(bitmap, dialog1);
                            **/
                            /*
                            //Show the dialog fragment 'DownloadChunk'.
                            android.app.FragmentManager fm = getFragmentManager();
                            DownloadChunk downloadChunkFragment = DownloadChunk.newInstance(decodedByte);
                            downloadChunkFragment.show(fm, "fragment_download_chunk");
                            */

                        }else{
                            //continue downloading and update the progressDialog
                            progressDialog.incrementProgressBy(1);
                            progressDialog.setMessage(" Downloaded : " + downloaded_ + " / " + size);

                            //Create a JSON to send to server
                            JSONObject chunkStatus    = new JSONObject();
                            try {
                                chunkStatus.put("File", index);
                                chunkStatus.put("Place", downloadedChunk.getInt("Downloaded"));
                                int percent = downloadedChunk.getInt("Downloaded") / downloadedChunk.getInt("Size");
                                chunkStatus.put("Percent", percent);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            socket.emit("downloadFileMoreData", chunkStatus);

                        }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }



                        //traitement if download complete or not
                        //if complete
                        //   socket.emit('downloadFileCompleteRes', { 'IsSuccess' : true });
                        //else

                    }
                });
            };
        });

    }//end initSocket4

    /**
     * Save or update the chat  user infos in local sqlite db
     * @param chatUser the supplied chat user object to save
     * @return number of inserted or updated rows.
     */
    private int saveChatUser(ChatUser chatUser) {
        ContentValues values = new ContentValues();
        values.put(UsersContract.COLUMN_USER_NAME,          chatUser.nickname);
        //values.put(UsersContract.COLUMN_CHAT_ID,          chatUser.chatId);
        if(chatUser.imageProfile == null){
            values.putNull(UsersContract.COLUMN_PROFILE);
        }else{
            values.put(UsersContract.COLUMN_PROFILE,        chatUser.imageProfile);
        }
        values.put(UsersContract.COLUMN_STATUS,             chatUser.status);
        values.put(UsersContract.COLUMN_NOT_SEEN,           chatUser.notSeenMessagesNumber);
        values.put(UsersContract.COLUMN_CONNECTED,          chatUser.connectedAt);
        values.put(UsersContract.COLUMN_DISCONNECTED_AT,    chatUser.disconnectedAt);
        values.put(UsersContract.COLUMN_BLACKLIST_AUTHOR,   chatUser.blacklistAuthor);

        //if 'chatUser.imageProfile != null', there is 'insert' then 'chatUser.imageProfile' is put in values.
        //if 'chatUser.imageProfile == null', there is 'update' then 'chatUser.imageProfile' is not put in values.
        //if(chatUser.imageProfile != null)values.put(UsersContract.COLUMN_PROFILE, chatUser.imageProfile);

        /*
        INSERT INTO phonebook2(name,phonenumber,validDate)
        VALUES('Alice','704-555-1212','2018-05-08')
        ON CONFLICT(name) DO UPDATE SET
        phonenumber=excluded.phonenumber,
        validDate=excluded.validDate
        WHERE excluded.validDate>phonebook2.validDate;

        INSERT INTO chat_users (nickname, imageprofile, status, notseen, connected, disconnectedat, blacklistauthor)
        VALUES('bison', '', 1, 100, 0, 0, NULL) ON CONFLICT(nickname) DO UPDATE SET
        notseen=excluded.notseen
        WHERE excluded.nickname='bis';
        ;
         */

        ContentResolver cr  = getContentResolver();
        int numRows = 0;

        // insert the 'chatUser'. If it exists, it is replaced. See 'FileDbContentProvider'
        Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);

        if(newRowUri != null) numRows = 1;

        /*
        //if row exists, update
        if(newRowUri == null)numRows = cr.update(UsersContract.CONTENT_URI_USERS,
                values,
                UsersContract.COLUMN_USER_NAME + " =? ",
                new String[]{chatUser.nickname});
        */

        return numRows;
    }


    private ChatUser getChatUser(String nickname) {
        ChatUser chatUser = null;

        String[] projection = {
                UsersContract.COLUMN_USER_NAME,
                UsersContract.COLUMN_PROFILE,
                UsersContract.COLUMN_STATUS,
                UsersContract.COLUMN_NOT_SEEN,
                UsersContract.COLUMN_CONNECTED,
                UsersContract.COLUMN_DISCONNECTED_AT,
                UsersContract.COLUMN_BLACKLIST_AUTHOR
        };

        Cursor cursor = getContentResolver().query(UsersContract.CONTENT_URI_USERS,
                projection,
                //FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=? OR "+FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=?" ,
                //new String[]{selectedNickname, Nickname, Nickname, selectedNickname},

                UsersContract.COLUMN_USER_NAME + "=?" , new String[]{nickname},

                null);
        if(cursor == null || cursor.getCount() == 0){
            cursor.close();
            return null;
        }

        cursor.moveToFirst();
        //String mimeType = null;
        //cursor.moveToPosition(-1);

        String username         = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_USER_NAME));
        String imageProfile     = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_PROFILE));
        int status              = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_STATUS));
        int notSeenMessages     = cursor.getInt(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_NOT_SEEN));
        long connectedAt        = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_CONNECTED));
        long disconnectedAt     = cursor.getLong(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_DISCONNECTED_AT));
        String blacklistAuthor  = cursor.getString(cursor.getColumnIndexOrThrow(UsersContract.COLUMN_BLACKLIST_AUTHOR));

        //build the 'ChatUser' object
        chatUser = new ChatUser (
                username,
                null,
                imageProfile,
                status,
                notSeenMessages,
                connectionTime,
                disconnectedAt,
                connectedAt,    //here, lastConnectedAt is equal the 'connectedAt' get from the db
                blacklistAuthor
        );
        cursor.close();
        return chatUser;
    }


    /**
     * Add a file (pdf, txt, ...) with uri to device in 'DIRECTORY_DOWNLOADS'.
     * @param uri uri of the file
     * @param reference the reference of the file to add. It is a number linking the the message and the file.
     * @return true if the file is saved in 'download'. False otherwise.
     */
    private void downloadAddFile(Uri uri, String reference) {
        /*
        //Get bitmap from uri.
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        //We can't use mediastore, ONLY ANDROID 11
        //get inputstream from uri
        InputStream  inputStreamFromUri = null;
        try {
            inputStreamFromUri = getContentResolver().openInputStream(uri);
            byte[] data = new byte[inputStreamFromUri.available()];
            inputStreamFromUri.read(data);

            OutputStream os = new FileOutputStream(file);
            os.write(data);
            inputStreamFromUri.close();
            os.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        //target storage where the file will be saved.
        File storageDir   = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }
        //File storageDir   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        long time = new Date().getTime();
        mimeType = getMimeType(uri);
        //target file
        File file = new File(storageDir, "shared_"+time+"."+mimeType);
        */
        ////////////////////////////////////////////////////////////////////////////////////////////
        //Prepare the mediastore to store the target file ONLY ANDROID 11
        /*
        ContentValues contentValues = new ContentValues();
        //contentValues.put(MediaStore.Images.ImageColumns._ID, time);
        contentValues.put(MediaStore.Downloads.TITLE, "shared_" + uri.getLastPathSegment());
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, "shared_" + uri.getLastPathSegment());
        contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.Downloads.DATE_ADDED, System.currentTimeMillis());
        contentValues.put(MediaStore.Downloads.DATA, downloadAbsolutePath);

        //Do not remove
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Downloads.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }
        Uri uri2 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            uri2 = MediaStore.Downloads.getContentUri("external");
        }
        uri2 = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri fileUri = this.getContentResolver().insert(uri2, contentValues);
        */
        /*
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(downloadAbsolutePath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);


        try {
            Uri finalUri = uri2;
            MediaScannerConnection.scanFile(this,
                new String[]{downloadAbsolutePath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        getContentResolver().insert(finalUri, contentValues);
                        Log.i("onScanCompleted", uri.getPath());
                    }
                });
        }catch(Exception e){
            e.getStackTrace();
        }
         */
        ////////////////////////////////////////////////////////////////////////////////////////////
        /*
        //Get id of inserted row in 'downloads'
        //uri2 = MediaStore.Downloads.EXTERNAL_CONTENT_URI;           //Environment.DIRECTORY_DOWNLOADS;
        uri2 = MediaStore.Files.getContentUri("external"); //Environment.DIRECTORY_DOCUMENTS;
        String[] projection = {MediaStore.Files.FileColumns._ID};

        Cursor cursor = this.getContentResolver().query(uri2, projection, null, null, null);

        long id = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.DownloadColumns._ID);
                id = Long.parseLong(cursor.getString(idIndex));
            }
        }
        //Get uri of inserted row in 'downloads'
        Uri uri1 = ContentUris.withAppendedId(uri2, id);
        */
        ////////////////////////////////////////////////////////////////////////////////////////////
        /*
        //Compress the bitmap
        ContentResolver resolver = this.getContentResolver();
        try (OutputStream stream = resolver.openOutputStream(fileUri)) {
            int  bytesAvailable = inputStreamFromUri.available();
            // int bufferSize = 1024;
            int maxBufferSize = 1 * 1024 * 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            final byte[] buffers = new byte[bufferSize];
            int read = 0;
            while ((read = inputStreamFromUri.read(buffers)) != -1) {
                stream.write(buffers, 0, read);
            }
            Log.e("File Size","Size " + file.length());
            inputStreamFromUri.close();
            stream.close();

            saved = true;
            stream.flush();
        } catch (Exception e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            e.getMessage();
            saved = false;
        }
        */





            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] {file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {

                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                            //Add entry in 'Preferences'
                            if(uri != null){
                                SharedPreferences.Editor editor = MainActivity.preferences.edit();
                                //editor.putString(reference, imageUri.toString());
                                editor.putString(reference, uri.toString());
                                editor.commit(); // commit changes
                            }

                        }
                    });
    }

    /**
     * Save the picture with uri locally in sqlite db
     * @param uri uri of the picture
     */
    private void sqliteAddPicture(Uri uri) {

    }

    /**
    * Add a bitmap with uri to gallery device in 'DIRECTORY_PICTURES'.
     * @param uri uri of bitmap
     * @param reference the reference of bitmap. It is a number linking the the message and bitmap.
     * @return true if the bitmap is saved in gallery. False otherwise.
     */
    private boolean galleryAddPicture(Uri uri, String reference) {
        boolean saved = false;

        //Get bitmap from uri.
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //target storage where the bitmap will be found.
        File storageDir   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File storageDir   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        long time = new Date().getTime();

        //target file
        //File file = new File(storageDir, getFilename(reference)+"."+mimeType);
        File file = new File(storageDir, getFilename(reference));

        ////////////////////////////////////////////////////////////////////////////////////////////
        //Mediastore method : working
        //Prepare the mediastore to store the target file.
        ContentValues contentValues = new ContentValues();
        //contentValues.put(MediaStore.Images.ImageColumns._ID, time);
        contentValues.put(MediaStore.Images.ImageColumns.TITLE, "shared_" + file.getName());
        contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, "shared_" + file.getName());
        contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.ImageColumns.DATA, file.getAbsolutePath());
        //Do not remove
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.ImageColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }

        Uri imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        ////////////////////////////////////////////////////////////////////////////////////////////

        //Get id of inserted row in mediastore
        Uri uri2 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.ImageColumns._ID};

        Cursor cursor = this.getContentResolver().query(uri2, projection, null, null, null);

        long id = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
                id = Long.parseLong(cursor.getString(idIndex));
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        //Compress the bitmap
        ContentResolver resolver = this.getContentResolver();
        try (OutputStream stream = resolver.openOutputStream(imageUri)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                return false;
            }
            saved = true;
            stream.flush();
        } catch (Exception e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            e.getMessage();
            saved = false;
        }

        //Add entry in 'Preferences'
        if(saved){
            SharedPreferences.Editor editor = MainActivity.preferences.edit();
            //editor.putString(reference, imageUri.toString());
            editor.putString(reference, uri.toString());
            editor.commit(); // commit changes
        }
        return saved;
    }

    /**
     * Get the name of attachment without extension
     * @param ref is the reference in the message object.
     * @return the filename
     */

    private String getFilename(String ref) {

        String[] mProjection = new String[]{
                MessagesContract.COLUMN_EXTRANAME
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(MessagesContract.CONTENT_URI_MESSAGES,
                mProjection,
                //FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=? OR "+FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=?" ,
                //new String[]{selectedNickname, Nickname, Nickname, selectedNickname},

                MessagesContract.COLUMN_REFERENCE+"=?" , new String[]{ref},

                null);

        if (cursor != null) cursor.moveToFirst();

        String filename = null;

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            filename    = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRANAME));
        }
        return filename;
    }

    /**
     * Get type (extension) from mime type. Note type is like : 'jpg', 'pdf' and mime type is like :
     * 'image/jpg', 'application/pdf'
     * @param mimeType the supplied mime type
     * @return the type (extension).
     */
    private String getType(String mimeType){
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(mimeType);
    }

    /**
     * Get mime type of attachment like : 'application/pdf', 'image/jpg'
     * @param ref is the reference of the message object witch is associeted to this attachment.
     * @return mimetype like : 'application/pdf', 'image/jpg'
     */
    private String getMimeType(String ref) {

        String[] mProjection = new String[]{
                MessagesContract.COLUMN_MIME
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(MessagesContract.CONTENT_URI_MESSAGES,
                mProjection,
                //FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=? OR "+FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=?" ,
                //new String[]{selectedNickname, Nickname, Nickname, selectedNickname},

                MessagesContract.COLUMN_REFERENCE + "=?" , new String[]{ref},

                null);

        if (cursor != null) cursor.moveToFirst();

        String mimeType = null;

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MIME));
        }
        return mimeType;
    }

    //when we come back after 'Home' we arrive here and go to start.
    @Override
    public void onRestart() {   //after restart, we go to start
        super.onRestart();
        System.out.println("************ onRestart ******");
        //notify the users i'm coming back after 'Home'
        socket.emit("backstandby", Nickname); //

    }
    @Override
    public void onResume() {
        super.onResume();
        System.out.println("************ onResume ******");
        //if(getSupportFragmentManager().getFragments().get(0).isAdded())initSocket();
        //initSocket();

        getSupportFragmentManager().addFragmentOnAttachListener(new FragmentOnAttachListener() {
            @Override
            public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
                //if(ChatBoxMessage.class.isInstance(fragment) ||
                //        ChatBoxActivity.class.isInstance(fragment))initSocket();
                if(ChatBoxMessage.class.isInstance(fragment))initSocket();
            }
        });

        //startHandler();//user interaction dialog flicking
    }

    //le bouton back pressed ne marche plus.
    //@Override
    //public void onBackPressed() {
    //    //int i = 0;
    //};

    //'Back' call 'finish()' and 'onPause()' and 'onStop()'.
    //l'application est fermée mais reste dans la pile. Elle sera créée de nouveau par 'onCreate(), onStart()...'
    //
    //@Override
    //public void finish() {
    //    super.finish();
    //}

    public Fragment getVisibleFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if(fragments != null){
            for(Fragment fragment : fragments){

                if (fragment == null || !fragment.isVisible()) {
                    continue;
                }
                int i = 0;
                //return fragment;
            }
        }
        return null;
    }

    //when the 'back' button is pressed in fragment either 'ChatBoxActivity' or 'ChatBoxMessage' we come here.
    @Override
    public void onBackPressed(){
        //index = 0 (chatBoxActivity), index = 1 (chatBoxMessage)
        int index = viewPager.getCurrentItem();
        int count = getSupportFragmentManager().getBackStackEntryCount();

        /*
        //Get the fragment visible -- not working, the visible fragment is not correct
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        Fragment fragmentVisible = getVisibleFragment();
        if(ChatBoxActivity.class.isInstance(fragmentVisible)) {
            socket.disconnect();
            finish();
            super.onBackPressed();
        }
        */

        switch (index) {
            case 0 : 
                //'ChatBoxActivity'
                    socket.disconnect();
                    finish();
                    super.onBackPressed();
                break;

            case 1:
                //'ChatBoxMessage'
                // open the 'ChatBoxActivity' fragment to show the list of connected users.
                tabs.getTabAt(0).select();
                break;

            default:
                // unknown fragment
                throw new UnsupportedOperationException("Unexpected error at : 'onBackPressed'");
        }


        /*
        FragmentManager fm = getSupportFragmentManager();
        int count_ = fm.getBackStackEntryCount();
        if (count_ >= 1) {
            if (fm.findFragmentById(R.id.view_pager) instanceof ChatBoxActivity) {
                super.onBackPressed();
            }
        } else {
            // Show your root fragment  here
        }
        */

        //notify the users, i'm going.
        //socket.emit("disconnect_", Nickname); //'disconnect' is a key word.
        //socket.emit(Socket.EVENT_DISCONNECT, Nickname);

    }

    @Override
    public void finish() {
        //super.finish();


        //Save the chat messages for all connected uers.
        //contourner le flag
        //Get list of fragments
        /*
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        int index = 0;
        int i = 0;
        for(Fragment f : fragments){
            if(ChatBoxMessage.class.isInstance(f)) index = i;
            i++;
        }

        ChatBoxMessage f = (ChatBoxMessage)fragments.get(index);

        f.saveAllMessages();
        */

        // Prepare data intent. the data are returned to 'onActivityResult' of 'MainActivity' which lanched the intent.
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //for illustation only
        intent.putExtra("returnKey1", "Swinging on a star. ");
        intent.putExtra("returnKey2", "You could be better then you are. ");
        // Activity finished ok, return the data
        setResult(INTENT_RESULT_CODE, intent); //the data are returned to 'onActivityResult' of 'MainActivity' which lanched the intent.

        super.finish();//obligatoire
    }


    @Override
    public void onPause() {
        System.out.println("************ onPause ******");
        if(!isFinishing()) {
            //the user is standby. Notify the users.
            socket.emit("standby", Nickname); //
            //stopHandler();//user interaction
        }
            /*
            //Save the chat messages for all connected uers.
            //contourner le flag
            //Get list of fragments
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            int index = 0;
            int i = 0;
            for(Fragment f : fragments){
                if(ChatBoxMessage.class.isInstance(f)) index = i;
                i++;
            }

            ChatBoxMessage f = (ChatBoxMessage)fragments.get(index);

            f.saveAllMessages();
        }
        //socket.emit("disconnect", Nickname);
        */
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        System.out.println("************ onStop ******");
        //stopHandler(); //user interaction
        //viewPager.removeAllViews();
    }

    @Override
    public void onDestroy() {
        //socket.disconnect();
        //stopHandler(); //user interaction
        super.onDestroy();
        System.out.println("************ onDestroy ******");
        socket.off("userjoinedthechat");
        socket.off("lastuserjoinedthechat");
        socket.off("uploadFileComplete");
        socket.off("uploadMessageComplete");
        socket.off("message_detection_back");
        socket.disconnect();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        System.out.println("************ onSaveInstanceState ****** ii = " + ii);
        outState.putInt("ii", ii);
        outState.putBoolean("redirect", redirect);
    }

    //@Override
    //public int getItemPosition(Object object) {
    //    return PagerAdapter.POSITION_NONE;  // This will get invoke as soon as you call notifyDataSetChanged on viewPagerAdapter.
    //}

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return stream.toByteArray();
        }finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    e.printStackTrace();
                    //Log.e(Helper.class.convertBitmapToByteArray(), "ByteArrayOutputStream was not closed");
                }
            }
        }
    }

    //After clicking 'Download Attachment' in 'ChatBoxMessage' we arrive here.
    public void downloadAttachment(String id) {
        //ask server to  begin download attachment. The response of the server is done in :'socket.on("download_chunks' event.
        socket.emit("start_download", "");
    }

    /**
     * After selecting an attachment, we arrive here.
     * The message is not sent yet. It is sent only when the upload attachment is successfully.
     * //compress the attachments before upload.
     */
    public void uploadAttachment(String attachmentUriPath, Message message) {
        String fromNickname     = message.getFromNickname();
        String toNickname       = message.getToNickname();
        String messageContent   = message.getMessage();
        long time               = message.getTime();
        String mimeType         = message.getMimeType();

        //save the 'reference' and 'uri path' of the uploaded image. They are used in
        //socket.on("uploadFileComplete")
        uploadAttachmentReference = message.getRef();

        try {
            MediaScannerConnection.scanFile(this,
                    new String[]{attachmentUriPath}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            uploadAttachmentUri = uri;
                            if(uri != null)uploadAttachmentNext (attachmentUriPath, message);
                            //error in Redmi uri is null
                            Log.i("onScanCompleted", uri.getPath());
                        }
                    });
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    //compress the attachments before upload.
    public void uploadAttachmentNext (String attachmentUriPath, Message message) {

        fileUploadManagerArrayList.clear();

        //listen.setValue("Initial value: 20");
        //listen.postValue("Final value: 20");

        compressedFile     = new File(attachmentUriPath);  //for 'pdf', 'txt' file.

        //If the attachment is an image(jpg, jpeg, or png) do compression
        if(message.mimeType.equals("image/jpg") || message.mimeType.equals("image/jpeg") ){
            //Before compression
            File mFile_     = new File(attachmentUriPath);

            //Compress the attachment before upload
            compressedFile = CompressBitmapFile(mFile_);
        }

        //trigger a database request
        //socket.emit("user", fromNickname, toNickname, toId, jsonObjectMessage, time);

        //send attachment to Async task
        //UploadFilesTask uploadFiles = new UploadFilesTask(this, socket, uris.get(0));
        //Executing async task
        //uploadFiles.execute();

        //final String UPLOAD_FILE_PATH = uriPaths.get(0);

        //this.fileUploadManager = new FileUploadManager();
        //String UPLOAD_FILE_PATH = null;

        //Prepare json to send to server in 'socket.emit("uploadFileStart", jsonArr)' below.
        //The json contains the compressed file and informations about it.
        JSONArray jsonArr = new JSONArray();

        //Get instance of 'FileUploadManager' which manage the file transmitted to server.
        //The compressed file 'compressedFile' is split in part and stored in array list
        //each part is sent to server.
        FileUploadManager fileUploadManager = new FileUploadManager(this);
        fileUploadManager.prepare(compressedFile);

        JSONObject res    = new JSONObject();
        String filename = "";

        //Array list to store the fileUploadManager
        ArrayList<FileUploadManager> fileUploadManagerArrayList_ = new ArrayList<FileUploadManager>();
        try {
            //res.put("Name", fileUploadManager.getFileName());mFile.getName()
            //res.put("Size", fileUploadManager.getFileSize());
            //res.put("Uri", uriPath);
            res.put("Owner", message.fromNickname);
            res.put("Ref",   message.getRef());
            res.put("Time",  message.getTime());

            //Adjust the filename length to column database table 'messages' whitch do no not over 50 characters
            filename = (compressedFile.getName().length() > 45) ? compressedFile.getName().substring(0, 45) : compressedFile.getName();

            //res.put("Name", compressedFile.getName());
            res.put("Name", filename);
            res.put("Size", compressedFile.length());
            res.put("Mime", message.getMimeType());
            jsonArr.put(res);

            //add the 'fileUploadManager'
            fileUploadManagerArrayList_.add(fileUploadManager);
        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: Log errors some where..
        }

        setListening_uploadFileMoreDataReq(fileUploadManagerArrayList_);
        //setListen_uploadFileComplete();
        // Tell server we are ready to start uploading.
        // This will trigger node server 'uploadFileStart' event.
        socket.emit("uploadFileStart", jsonArr);
        progressDialog.setProgress(0);
    }

    private void setListening_uploadFileMoreDataReq(ArrayList<FileUploadManager> fileUploadManagerArrayList_) {
        //After emitting 'uploadFileStart' to server,the server asks for more data to upload
         socket.on("uploadFileMoreDataReq", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0]; //'Place' : Place, 'Percent
                        //extract data from fired event.
                        int place   = 0;
                        int index   = 0;
                        final int index_ = 0;
                        int percent = 0;
                        try {
                            index   = json_data.getInt("File");
                            place   = json_data.getInt("Place");
                            percent = json_data.getInt("Percent");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // Read the next chunk and send it to server.
                        int offset = place;
                        //System.out.println("uploadFileMoreDataReq  ii = " + ii);
                        //System.out.println("uploadFileMoreDataReq  listen = " + listen.getValue());
                        //System.out.println("uploadFileMoreDataReq************ compressedFile.length() = " + compressedFile.length());
                        //System.out.println("uploadFileMoreDataReq************ fileUploadManagerArrayList = " + fileUploadManagerArrayList.size());
                        FileUploadManager fileUploadManager = fileUploadManagerArrayList_.get(0);
                        try {
                            //FileUploadManager fileUploadManager = new FileUploadManager();
                            //fileUploadManager.prepare(uriPath, TabChatActivity.this);
                            fileUploadManager.read(offset);// we can access 'getData()' and 'getBytesRead()'
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        JSONArray jsonArr = new JSONArray();
                        JSONObject res    = new JSONObject();

                        String filename   = (fileUploadManager.getFileName().length() > 45) ? fileUploadManager.getFileName().substring(0, 45) : fileUploadManager.getFileName();

                        try {
                            res.put("File", index);
                            //res.put("Name", fileUploadManager.getFileName());
                            res.put("Name", filename);
                            res.put("Data", fileUploadManager.getData());
                            res.put("chunkSize", fileUploadManager.getBytesRead());
                            jsonArr.put(res);

                            // This will trigger server 'uploadFileChuncks' function
                            socket.emit("uploadFileChuncks", res); //jsonArr);

                            //setup a progress bar when the upload begin
                            int finalPlace = place;
                            //Il faut faire 'runOnUiThread' sinon le progress se fait mal

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    progressDialog.setTitle("Uploading : " + filename);
                                    progressDialog.setMessage("Uploading : " + finalPlace + "/" + fileUploadManager.getFileSize());
                                    progressDialog.incrementProgressBy(1);
                                    progressDialog.show();
                                }
                            });

                            //increment the progress bar
                            //long increment = (fileUploadManager.getFileSize() == 0) ? 0 : (fileUploadManager.getBytesRead() * 100) / fileUploadManager.getFileSize();

                            //if(place != 0)
                            //progressDialog.incrementProgressBy(1);
                            //passage++;
                            //System.out.println("progress = " + progressDialog.getProgress() + " passage = " + passage);
                            //progressDialog.setMessage("Uploading : " + place + "/" + fileUploadManager.getFileSize());
                            //progressDialog.setMessage("Uploading : " + passage);

                        } catch (JSONException e) {
                            //TODO: Log errors some where..
                        }
                    }
                });
            }
        });
    }

    private void setListen_getAllUsers() {
        socket.on("get_all_users_bak", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray users = (JSONArray) args[0];
                        //'users' ne sera jamais null, il contient, au minimum,le user qui vient de se connecter.
                        //donc inutile de faire if(users.equal(null)) return;

                        ArrayList<ChatUser> arrayListUsers = new ArrayList<>();

                        for(int i = 0; i <= users.length() - 1; i++){
                            ChatUser user = null;
                            try {
                                //Il faut que les champs de la table 'users' dans la bd soient
                                // identiques aux champs de l'objet 'ChatUser' pour pouvoir utiliser l'instruction suivante
                                //user = new Gson().fromJson(users.getJSONObject(i).toString(), ChatUser.class);

                                String nickname       = users.getJSONObject(i).getString("nickname");
                                //String imageprofile   = users.getJSONObject(i).getString("imageprofile");

                                int status            = users.getJSONObject(i).getInt("status");
                                long connectedAt      = users.getJSONObject(i).getLong("connected");
                                long disconnectedAt   = users.getJSONObject(i).getLong("disconnected");
                                long lastConnectedAt  = users.getJSONObject(i).getLong("lastconnected");
                                String blacklistAuthor= users.getJSONObject(i).getString("blacklistauthor");

                                if(users.getJSONObject(i).isNull("notSeenMessagesNumber")){
                                    int notSeenMessagesNumber      = 0;
                                }

                                String imageprofile = (users.getJSONObject(i).isNull("imageprofile")) ?
                                    null : users.getJSONObject(i).getString("imageprofile");

                                //do not list the current user and the last session users.
                                boolean isUserInLastSessionUsers = false;
                                if(lastSessionUsers != null){
                                    if((lastSessionUsers.length == 0)){
                                        if(nickname.equals(Nickname))isUserInLastSessionUsers = true;
                                    }else{
                                        for(ChatUser chatUser : lastSessionUsers){
                                            if(chatUser.nickname.equals(nickname) || (nickname.equals(Nickname))){
                                                isUserInLastSessionUsers = true;
                                                //break;
                                            }
                                        }
                                    }
                                }else{
                                    isUserInLastSessionUsers = false;
                                }

                                if(isUserInLastSessionUsers) continue;

                                user = new ChatUser(nickname,
                                        null,
                                        imageprofile,
                                        status,
                                        0,
                                        connectedAt,
                                        disconnectedAt,
                                        lastConnectedAt,
                                        blacklistAuthor
                                );

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            arrayListUsers.add(user);
                        }

                        DataHolder.setData(Arrays.asList(arrayListUsers.toArray()));

                        //Il y a une exception dans l'instruction suivante :
                        //intent.putParcelableArrayListExtra("all_users", arrayListUsers);
                        // à cause de la taille de 'arrayListUsers'.
                        //Pour contourner cette exception, on transmet 'arrayListUsers' dans 'Enum'
                        // 'Enum' est utilisé dans 'DisplayAllusersActivity'.

                        Intent intent = new Intent(TabChatActivity.this, DisplayAllUsersActivity.class);
                        //intent.putParcelableArrayListExtra("all_users", arrayListUsers);

                        intent.putExtra("current_user", (Parcelable)currentChatUser);

                        //intent.putExtra("fragment_index", 0); //pass zero for Fragment one.
                        //intent.putExtra("NIckname", NICKNAME);
                        //intent.putExtra("image_profile", imageProfile_);
                        //intent.putExtra("first_time", "");
                        //intent.putExtra("image_profile_changed", "");
                        //intent.putExtra("connection_time", connectedAt);

                        startActivityForResult(intent, REQUEST_CODE_ALL_USERS, null);
                    }
                });
            }
        });
    }

    public void setListen_uploadFileComplete(){
        //The upload of attachment is complete
        socket.on("uploadFileComplete", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0]; //{'IsSuccess' : true }
                        try {
                            boolean isSuccess = json_data.getBoolean("IsSuccess");  //{"IsSuccess", "true"}
                            progressDialog.hide();

                            if(isSuccess){
                                // Add entry in 'Preferences' the uri of the attachment
                                SharedPreferences.Editor editor = MainActivity.preferences.edit();
                                editor.putString(uploadAttachmentReference, uploadAttachmentUri.toString());
                                editor.commit(); // commit changes

                                //display the sent message with its attachments.
                                //Get list of fragments
                                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                                int index  = 0;
                                int index_ = 0;
                                int i = 0;
                                for(Fragment f : fragments){
                                    if(ChatBoxActivity.class.isInstance(f)) index = i;
                                    if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                                    i++;
                                }

                                //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                                //f.displayReceivedBackStandbyUser(nickname);
                                //f_.btn.setEnabled(true);
                                f_.performClickNext_(); //send the message

                                //clear
                                //fileUploadManagerArrayList.clear();
                                once = true;
                                socket.off("uploadFileMoreDataReq");

                            }else{
                                //Notify the user, the upload is not successfull
                                Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "the upload is not successfull", Snackbar.LENGTH_LONG).show();

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void setListen_join(){
        //implementing socket listeners when a user connect.
        socket.on("userjoinedthechat", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String nickname         = (String) args[0];     //user nickname who join
                        String idNickname       = (String) args[1];     //user id who join
                        String profile          = (null == args[2]) ? null : (String)args[2];
                        //String profile          = p.equals("null") ? null : p;     //image profile of the user who join may be null.
                        int status              = ((Number)args[3]).intValue();
                        //int j = (int)args[3];   //The value sent by the server is 'Integer'
                        long connectionTime_    = ((Number) args[4]).longValue(); //connection time

                        //j = (int)args[4];
                        long lastConnectionTime_= ((Number) args[5]).longValue(); //last connection time
                        long disconnectionTime_ = ((Number) args[6]).longValue(); //disconnection time
                        int notSeenMessages     = ((Number) args[7]).intValue();
                        String blacklistAuthor  = (String) args[8];

                        ChatUser chatUser;
                        //build the 'chatUser'
                        if (nickname.equals(Nickname)) { //it is the current user
                            //IdNickname = idNickname;
                            currentChatUser.chatId = idNickname;
                            ////////////////////////////////////////////////////////////////////////
                            // le seul endroit ou 'getSupportFragmentManager().getFragments()' is not null
                            //contourner le tag
                            //Get list of fragments
                            if(lastSessionUsers != null){
                                List<Fragment> fragments = getSupportFragmentManager().getFragments();

                                int index  = 0;
                                int index_ = 0;
                                int i = 0;
                                for(Fragment f0 : fragments){
                                    if(ChatBoxActivity.class.isInstance(f0)) index = i;
                                    if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                                    i++;
                                }

                                ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                                for(ChatUser chatUser_ : lastSessionUsers) {
                                    if(chatUser_ != null){
                                        f.displayReceivedNewUser(chatUser_);
                                        f_.setChatListUsers(chatUser_);
                                    }
                                }

                            }
                            return;
                            //show an empty fragment
                            //getSupportFragmentManager().beginTransaction()
                            //            .replace(R.id.ll_recycler_activity_chat_box, new ChatBoxActivityEmpty())
                            //            .commitNow();
                            ////////////////////////////////////////////////////////////////////////
                            //return;
                            //}else {
                        }
                        //It is not the current user, build a usual chat user and save it in local db.
                        chatUser = new ChatUser(
                                nickname,
                                idNickname,
                                profile, // == null) ? getChatUser(nickname).imageProfile : profile,
                                status,
                                notSeenMessages,
                                connectionTime_,
                                disconnectionTime_,
                                lastConnectionTime_,
                                blacklistAuthor
                        );

                            /*
                            //build the json from 'currentChatUser' object to send to server
                            Gson gson = new Gson();
                            String jsonChatuser = null;
                            JSONObject jsonChatuserObject = null;
                            try {
                                jsonChatuser = gson.toJson(currentChatUser);
                            }catch (Exception e){
                                e.getStackTrace();
                            }
                            try {
                                //Le JSONObject est construit à partir des propriétés de l'objet 'currentChatUser'
                                jsonChatuserObject = new JSONObject(jsonChatuser);
                                //jsonChatuserObject.put("toId", "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            */

                        //Save the 'chatUser' object of the user who is joined locally in slite db
                        int numRows = saveChatUser(chatUser);
                        if(numRows != 1) throw new UnsupportedOperationException("New chat user not saved in local db");

                            /*
                            if(uri == null){
                                //notify the user
                                Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content),
                                        nickname + " chat infos are not not saved locally", Snackbar.LENGTH_LONG).show();
                            }
                            */
                            /*
                            //Send the 'jsonChatuserObject' to server where it will be saved in pg database.
                            String[] objects = {jsonChatuserObject.toString()}; //Le json sous forme de string contient l'objet 'currentChatUser'.
                            //socket.emit("messagedetection", objects);

                            socket.emit("save_chat_user_infos", objects, new Ack() {
                                @Override
                                public void call(Object... args) {
                                    //TODO process ACK
                                    String response = (String)args[0];
                                    if(!response.equals("success")){
                                        //Notify the user who emit his infos in the 'currentChatUser' are not sent.
                                        Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content),
                                                nickname + " infos are not saved on server", Snackbar.LENGTH_LONG).show();
                                    }

                                    //Ack ack = (Ack) args[args.length - 1];
                                    //ack.call();
                                }
                            });
                            */

                        //return;
                        //}

                        /*
                        //test if reconnect
                        boolean reconnect = false;
                        Iterator<ChatUser> iterator = chatUserList.iterator();
                        while(iterator.hasNext()){
                            ChatUser chatUser_ = iterator.next();
                            if(chatUser_.getNickname().equals(nickname))reconnect = true;
                        }

                        //Notify 'ChatBoxMessage' and 'ChatBoxActivity' that the user is reconnecting
                        //contourner le flag
                        //Get list of fragments
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        f.updateChatListUsers( nickname, idNickname);
                        f_.updateChatListUsers( nickname, idNickname);

                        if(reconnect)return;
                        */
                        //System.out.println("********************* user joined. all connected = "+people+" image profile = "+imageProfile);

                        //chatUserList.clear();
                        /*
                       //Build the array list of connected users.
                        Iterator<String> keys = people.keys();
                        while(keys.hasNext()){
                            String nickname_ = (String)keys.next();
                            String id_ = null;
                            String imageProfile = null;
                            try {
                                id_          = people.getString(nickname_);
                                imageProfile = profile.getString(nickname_);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            ChatUser chatUser = new ChatUser(nickname_, id_, imageProfile);
                            chatUserList.add(chatUser);

                            //System.out.println("********************************* nickname = "+nickname_+" id = "+id_+" image profile = "+imageProfile);
                        }
                         */

                        //Get the number of not seen messages for this user
                        //int notSeenImages = 0;

                        //Ask the server

                        // Build the 'ChatUser' object to send to 'ChatBoxMessage' and 'ChatBoxActivity'
                        //ChatUser chatUser_ = null;
                        /*
                        chatUser_ = new ChatUser(nickname,
                                idNickname,
                                profile,
                                true, ChatUser.userConnect,
                                0,
                                connectionTime_,
                                lastConnectionTime_,
                                0,
                                null
                        );
                        */
                        //contourner le tag
                        //Get list of fragments
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f0 : fragments){
                            if(ChatBoxActivity.class.isInstance(f0)) index = i;
                            if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        //do no display the joined users
                        //f.displayReceivedNewUser( chatUser);
                        //f_.setChatListUsers( chatUser);

                        for(ChatUser chatUser_ : lastSessionUsers) {
                            if(chatUser_ != null){
                                //f.displayReceivedNewUser(chatUser_);
                                f_.setChatListUsers(chatUser_);
                            }
                        }

                        //notify
                        Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), nickname + " is joined", Snackbar.LENGTH_LONG).show();

                        //sectionsPagerAdapter.getItemPosition();

                        // add the new updated list to the adapter
                        //chatUserAdapter = new ChatUserAdapter(chatUserList);

                        // notify the adapter to update the recycler view
                        //chatUserAdapter.notifyDataSetChanged();

                        //set the adapter for the recycler view
                        //myRecylerView.setAdapter(chatUserAdapter);

                        //Response to the sender which has 'nickname' and 'id'.

                        //Send back my info ('Nickname','IdNickname', 'Profile', 'connectionTime') to
                        // user ('nickname, 'idNickname') who is just joined
                        //setListen_lastuserjoined();
                        socket.emit("lastuserjoined", nickname, idNickname,
                                currentChatUser.nickname,
                                currentChatUser.chatId,
                                currentChatUser.imageProfile,
                                currentChatUser.status,
                                currentChatUser.connectedAt,
                                currentChatUser.lastConnectedAt,
                                currentChatUser.disconnectedAt,
                                currentChatUser.notSeenMessagesNumber,
                                currentChatUser.blacklistAuthor
                        );

                        //Get the json object
                        //JsonObject jsonObj_ = JsonParser.parseString(json0).getAsJsonObject();
                        //Get the keys.
                        //Iterator<String> objectKeys = people.keys();
                        //System.out.println("********************************* objectKeys = "+objectKeys);
                        //org.json.JSONObject cannot be cast to java.lang.String[]

                        //add the connected user to map
                        //connectedUsers.put(nickname, id);
                        //String nickname = "";
                        /*
                        switch (nickname) {
                            case "mono":
                                tv1.setText(nickname);
                                break;

                            case "bis":
                                tv2.setText(nickname);
                                break;

                            case "ter":
                                tv3.setText(nickname);
                                break;

                            default:
                                // unknown user
                        }
                        */
                        //Toast.makeText(ChatBoxActivity.this, " connected " + nickname+" id = "+id+" all = "+people, Toast.LENGTH_LONG).show();
                    }//end run
                });
            }
        });
    }


    public void setListen_lastuserjoined(){
        // last user joined. it will receive from previous users their nickname and id.
        socket.on("lastuserjoinedthechat", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                List<Fragment> fragments_ = getSupportFragmentManager().getFragments();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<Fragment> fragments__ = getSupportFragmentManager().getFragments();
                        String nickname         = (String) args[0];   //sender nickname
                        String idNickname       = (String) args[1];   //sender id
                        String p                = (String) args[2];
                        String profile          = (p == null) ? null : p;
                        //String profile          = (String) args[2];   //sender image profile ={nickname:image profile}
                        int status              = (int) args[3];      //0=gone, 1=connect, 2=standby, 3=blacklisted
                        long connectionTime_    = ((Number)args[4]).longValue();    // current connection time
                        long lastConnectionTime_= ((Number)args[5]).longValue();    // last connection time
                        long disconnectionTime_ = ((Number)args[6]).longValue();    //disconnect time
                        int notSeenMessages     = ((Number)args[7]).intValue();     //the number of not seen messages
                        String blacklistAuthor  = (String) args[8];                 // the author of blacklist

                        // Build the 'ChatUser' object
                        ChatUser chatUser = null;
                        chatUser = new ChatUser(
                                nickname,
                                idNickname,
                                //(profile == null) ? getChatUser(nickname).imageProfile : profile,
                                profile,
                                status,
                                notSeenMessages,
                                connectionTime_,
                                disconnectionTime_,
                                lastConnectionTime_,
                                blacklistAuthor
                        );    //See 'ChatUser' for the value of 'status'.


                        //Save the 'ChatUser' object locally in slite db
                        int numRows = saveChatUser(chatUser);

                        /*
                        if(uri == null){
                            //notify the user
                            Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content),
                                    nickname + " chat infos are not not saved locally", Snackbar.LENGTH_LONG).show();
                        }
                        */

                        //Send 'chatUser' to 'ChatBoxMessage' and 'ChatBoxActivity'
                        //contourner le flag
                        //Get list of fragments
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        int index  = 0;
                        int index_ = 0;
                        int i = 0;
                        for(Fragment f : fragments){
                            if(ChatBoxActivity.class.isInstance(f)) index = i;
                            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                            i++;
                        }

                        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                        ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                        //do not display the other users who are already connected
                        //f.displayReceivedNewUser( chatUser);
                        f_.setChatListUsers( chatUser);

                        //chatUserList.add(chatUser);
                        //sectionsPagerAdapter.notifyDataSetChanged();

                        // add the new updated list to the adapter
                        //chatUserAdapter = new ChatUserAdapter(chatUserList);

                        // notify the adapter to update the recycler view
                        //chatUserAdapter.notifyDataSetChanged();

                        //set the adapter for the recycler view
                        //myRecylerView.setAdapter(chatUserAdapter);

                        //Response to the sender with 'nickname' and 'id'.
                        //socket.emit("lastuserjoined", nickname, id, Nickname, Id, Profile);
                    }
                });
            }
        });
    }

    public void setListen_messagedetection() {
        //for testing
        socket.on("message_detection_back_", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast. makeText(TabChatActivity.this, "message_detection_back", Toast. LENGTH_LONG).show();

                            }
                        });
                    }
        });

        //we received a message from the server :
        // - notify the user that he has received a new message,
        // - add this message to the message list
        // - update the adapter.
        // - insert or update the table 'chat_last_users'
        //
        //Received message from server
        socket.on("message_detection_back", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        try {
                           //System.out.println("***message_detection_back");
                            //extract data from fired event
                            String fromNickname = data.getString("fromNickname");   //from


                            String toNickname = data.getString("toNickname");     //from id
                            String message    = data.getString("message");        //message content
                            long time         = data.getLong("time");             //time
                            String extra      = data.getString("extra");          //Thumb image encoded base 64

                            System.out.println("message_detection_back : fromNickname = " + fromNickname + " toNickname = " + toNickname);

                            if(!toNickname.equals(Nickname))return; //This message is not for me=current user = 'Nickname'

                            //convert the JSONObject to string
                            String jsonString = data.toString();

                            //build the 'Message' object from json
                            Gson g = new Gson();
                            //Message message_ = g.fromJson(jsonString, Message.class);
                            Message message_ = g.fromJson(jsonString, Message.class);//changed 05-09-22

                            if (message_.fromNickname.equals(selectedNickname)) message_.seen = "1";

                            //insert or update the sqlite local table 'chat_last_users'
                            Map<String, Long> map = new HashMap<>();
                            map.put(toNickname, time);
                            int rows = saveLastSessionUsers(fromNickname, map);
                            if (rows != 1)
                                throw new UnsupportedOperationException("unexpected value. Last session user value is not 1");


                            //add the message to the messageList
                            //messageList.add(m);

                            /*
                            //get the message for this user
                            TabChatActivity.messageListUser.clear();
                            Iterator<Message> iterator = messageList.iterator();
                            while(iterator.hasNext()){

                                Message message_ = iterator.next();
                                if(message_.getToNickname().equals(Nickname) &&
                                        (message_.getFromNickname().equals(nickname)))TabChatActivity.messageListUser.add(message_);
                                }
                            */
                            //update the adapter of view pager
                            //sectionsPagerAdapter.notifyDataSetChanged();

                            //Get list of fragments
                            List<Fragment> fragments = getSupportFragmentManager().getFragments();
                            int index = 0; //index to ChatBoxMessage
                            int index_ = 0; //index to ChatBoxActivity
                            int i = 0;
                            for (Fragment f : fragments) {
                                if (ChatBoxMessage.class.isInstance(f)) index = i;
                                if (ChatBoxActivity.class.isInstance(f)) index_ = i;
                                i++;
                            }

                            ChatBoxMessage f = (ChatBoxMessage) fragments.get(index);
                            ChatBoxActivity f_ = (ChatBoxActivity) fragments.get(index_);

                            //f.displayReceivedMessage( messageList);   //tous les messages destinés à 'Nickname'.
                            f.displayReceivedMessage(message_);        //le dernier message reçu destiné à 'Nickname'.

                            //Notify the 'to' user that he has received message from 'from' user.
                            f_.displayNotificationNotSeenMessage(message_, selectedNickname);

                            //Not good, we just come from the server
                            //save last users in server. it is done in 'message_detection'.
                            //in server, last user in stored in field 'connectedWith' in 'uers' table.
                            // in local, last user is tored in 'chat.last_user' table.
                            //saveLastSessionUsersRemotelly(fromNickname, map);

                            //The last users are saved in 'messagedetection' event.
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    //not used. the last user infos are saved in server in 'messageDetection' event.
    private void saveLastSessionUsersRemotelly(String fromNickname, Map<String, Long> map) {
        Iterator<String> iterator = map.keySet().iterator();

        JSONObject lastUser = new JSONObject();
        while (iterator.hasNext()) {

            String key = iterator.next();
            Long value = map.get(key);

            try {
                lastUser.put("nicknameorigine", fromNickname);
                lastUser.put("nicknametarget", key);
                lastUser.put("time", value);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            socket.emit("last_user", lastUser, new Ack() {
                @Override
                public void call(Object... args) {
                    String status = (String) args[0];
                    if(status.equals("fail")) throw new UnsupportedOperationException("unexpected value when saving last users in server");
                }
            });
        }
    }

    public void setListen_uploadMessageComplete() {
        //The message is uploaded succesfully and it will be saved in 'performClickNext()'.
        socket.on("uploadMessageComplete", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject json_data = (JSONObject) args[0];
                        try {
                            boolean isMessageSaved = json_data.getBoolean("IsSuccess");  //{"IsSuccess", "true"}

                            if(isMessageSaved){

                                //The attachment is successfully uploaded, now send the message.
                                //Get list of fragments
                                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                                int index  = 0;
                                int index_ = 0;
                                int i = 0;
                                for(Fragment f : fragments){
                                    if(ChatBoxActivity.class.isInstance(f)) index = i;
                                    if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                                    i++;
                                }

                                //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                                //f.displayReceivedBackStandbyUser(nickname);
                                //f_.btn.setEnabled(true);
                                f_.performClickNext(); //display the message

                            }else{
                                //Notify the user, the upload is not successfull
                                Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "the message is not uploaded successfully", Snackbar.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    //called from 'ChatBoxMessage.getMessageFromServer'
    @Override
    public void getMessagesFromServer(String SelectedNickname, String Nickname, String IdNickname) {
        //String[] objects = {SelectedNickname, Nickname, IdNickname};
        String[] objects = {SelectedNickname, Nickname, IdNickname};

        JSONObject data    = new JSONObject();
        try {
            data.put("SelectedNickname", SelectedNickname);
            data.put("Nickname",         Nickname);
            data.put("IdNickname",       IdNickname);
            data.put("nbMessages",       NB_MESSAGES_TO_DOWNLOAD);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("get_messages", data, new Ack() {
            @Override
            public void call(Object... args) {
                //TODO process ACK
                //the number of messages to download is sent by the server in callback
                int nbMessages = (int) args[0];

                //show progress bar
                //Exception : 'android.view.ViewRootImpl$CalledFromWrongThreadException:
                // Only the original thread that created a view hierarchy can touch its views.'
                // To fix this exception, we use the following statement
                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        ChatBoxMessage.customCircularProgress.setVisibility(View.VISIBLE);
                        TextView textView = ChatBoxMessage.customCircularProgress.findViewById(com.example.aymen.androidchat.R.id.tv_circular_progress);
                        textView.setText("Downloading " + String.valueOf(nbMessages) + " messages");
                    }
                });
                ////////////////////////////////////////////////////////////////////////////////////
                /*
                JSONArray messages = (JSONArray) args[0];
                ArrayList<Message> arrayListMessage =  new ArrayList<>();
                //Ack callback = (Ack)args[0];
                for(int i = 0; i <= messages.length() - 1; i++){
                    Message message = null;
                    try {
                        //Il faut que les champs de la table 'messages' dans la bd soient
                        // identiques aux champs de l'objet 'Message' pour pouvoir utiliser l'instruction suivante
                        //message = new Gson().fromJson(messages.getJSONObject(i).toString(), Message.class);

                        String fromnickname = messages.getJSONObject(i).getString("fromnickname");
                        String tonickname   = messages.getJSONObject(i).getString("tonickname");
                        String message_     = messages.getJSONObject(i).getString("message");
                        String time_        = messages.getJSONObject(i).getString("time");
                        long time           = Long.parseLong(time_);
                        String extra        = messages.getJSONObject(i).getString("extra");     //may be empty
                        String extraname    = messages.getJSONObject(i).getString("extraname"); //may be empty
                        String mime         = messages.getJSONObject(i).getString("mime");      //may be empty
                        String ref          = messages.getJSONObject(i).getString("ref");
                        String seen         = messages.getJSONObject(i).getString("seen");
                        String deletedfrom  = messages.getJSONObject(i).getString("deletedfrom");
                        String deletedto    = messages.getJSONObject(i).getString("deletedto");

                        if(messages.getJSONObject(i).isNull("extra")){
                            extra      = null;
                            extraname  = null;
                            mime       = null;
                        }

                        message = new Message(fromnickname,
                                tonickname,
                                message_,
                                time,
                                extra,
                                extraname,
                                ref,
                                mime,
                                seen,
                                deletedfrom,
                                deletedto
                        );

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    arrayListMessage.add(message);
                }

                //send the arraylist to 'ChatBoxMessage.getMessagesFromServerRes'
                //Get list of fragments to update the remaining users.
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                int index  = 0;
                int index_ = 0;
                int i = 0;
                for(Fragment f : fragments){
                    if(ChatBoxActivity.class.isInstance(f)) index = i;
                    if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                    i++;
                }

                //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                //f.displayReceivedDiconnectUser(nickname);
                f_.getMessagesFromServerRes(arrayListMessage);
                 */
                ////////////////////////////////////////////////////////////////////////////////////
            }
        });
    }

    /**
     * Update message in server when it is deleted in client side by swiping the message to right.
     * @param ref Reference of the message
     * @param delete may be 1 or 2. If it is 1, the message sent by 'from' to 'to' is deleted. If it is 2
     */
    @Override
    public void updateMessage(String ref, String delete) {

        JSONObject data    = new JSONObject();
        try {
            data.put("Ref", ref);
            data.put("Delete", delete);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("update_message", data, new Ack() {
            @Override
            public void call(Object... args) {
                //TODO process ACK
                int nbUpdatedRow = Integer.parseInt((String)args[0]);

                //contourner le flag
                //Get list of fragments
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                int index  = 0;
                int index_ = 0;
                int i = 0;
                for(Fragment f0 : fragments){
                    if(ChatBoxActivity.class.isInstance(f0)) index = i;
                    if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                    i++;
                }

                ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                //f.displayReceivedNewUser( chatUser);
                f_.messageSwipeRecyclerView.updateMessageRes( nbUpdatedRow);
            }
        });

    }

    //@Override
    public void chatBoxMessageFragmentAttached() {

        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        /*
        //get the fragment by tag. There is a pb here : the 'chatBoxMessage' is null
        ChatBoxMessage chatBoxMessage = (ChatBoxMessage) getSupportFragmentManager().findFragmentByTag("chat_box_message");

        for(ChatUser chatUser : lastSessionUsers){
            //f.displayReceivedNewUser(chatUser);
            chatBoxMessage.setChatListUsers(chatUser);
        }
        */
        //contourner le tag

        int index  = 0;
        int index_ = 0;
        int i      = 0;

        for(Fragment f0 : fragments){
            if(ChatBoxActivity.class.isInstance(f0)) index = i;
            if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
            i++;
        }

        //ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
        //chatBoxMessage = (ChatBoxMessage)fragments.get(index_);
        //if(chatBoxActivity == null)return;
        //initSocket1();

        //ChatBoxActivity f = (ChatBoxActivity) sectionsPagerAdapter.getItem(index);
        //ChatBoxMessage f_ = (ChatBoxMessage)  sectionsPagerAdapter.getItem(index_);

        /*
        if(lastSessionUsers ==  null)return;
        for(ChatUser chatUser : lastSessionUsers){
            //chatBoxActivity.displayReceivedNewUser(chatUser);
            chatBoxMessage.setChatListUsers(chatUser);

        }
        */
    }

    /**
     * Compress an image file
     * @param file the file to compress
     * @return compressed file or null
     */
    public File CompressBitmapFile(File file){
        try {
            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;

            // factor of downsizing the image
            InputStream inputStream = getContentResolver().openInputStream(Uri.fromFile(file));
            //FileInputStream inputStream = new FileInputStream(file);

            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;

            // Find the correct scale value. It should be the power of 2.

            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            //Get bitmap from file
            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            //Create a new image file and then return it.
            //file.createNewFile();
            //File file_ = new File("aa.jpg");

            //String filePath = getFilesDir().getPath(); //data/user/0/[package]/files
            String filePath = getCacheDir().getPath(); //data/user/0/[package]/cache

            String filename = (file.getName().length() > 50) ? file.getName().substring(0, 50) : file.getName();
            File imageFile  = new File(filePath, filename);

            FileOutputStream outputStream = new FileOutputStream(imageFile);

            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);

            return imageFile;
        } catch (Exception e) {
            return null;
        }
    }

    //After clicking 'Send Message' in 'ChatBoxMessage' we arrive here. we have : the object 'message'
    // and an array list of attachments.
    //The attachment if any is sent below in 'uploadAttachment'.
    // In this method, we send only the message to server.
    // save the fields ' fromNickname' and 'toNickname' in sqlite local table 'chat_last_users'.
    @Override
    //public void sendMessage(String fromNickname, String fromId, String toNickname, String toId, String message) {
    @SuppressLint("NewApi")
    public void sendMessage(String toId, Message message, ArrayList<String> attachmentsPathUri) {
        String fromNickname   = message.getFromNickname();
        String toNickname     = message.getToNickname();
        String messageContent = message.getMessage();
        long time             = message.getTime();
        String ref            = message.getRef();       //ref of the message
        String extra          = message.getExtraName(); //thumb of attachment if any
        String extraName      = message.getExtra();     //filename of thumb of attachment if any
        String mime           = message.getMimeType();  // mime type of attachment joined to this message

        System.out.println("sendMessage : fromNickname = " + fromNickname + " toNickname = " + toNickname);

        //Paths of uris attachments.
        ArrayList<String> uriPaths   = attachmentsPathUri;

        /*
        String tag = "android:switcher:" + R.id.view_pager + ":" + 1;
        ChatBoxActivity f = (ChatBoxActivity) getSupportFragmentManager().findFragmentByTag(tag);
        f.displayReceivedData(message);
        tabs.getTabAt(1).select();
         */

        //Prepare to add key 'toId' to 'jsonObjectMessage' before emit
        Gson gson                    = new Gson();
        String jsonMessage           = null;
        JSONObject jsonObjectMessage = null;
        try {
            jsonMessage = gson.toJson(message);

        }catch (Exception e){
            e.getStackTrace();
        }
        try {
            jsonObjectMessage = new JSONObject(jsonMessage); //Le JSONObject est construit à partir des propriétés de l'objet 'Message'
            jsonObjectMessage.put("toId", toId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //insert or update the table 'chat_last_users'
        Map<String,Long> map = new HashMap<>();
        map.put(toNickname, time);
        int rows = saveLastSessionUsers(fromNickname, map);
        if(rows != 1)throw new UnsupportedOperationException("unexpected value. Last session user value is not 1");

        //Send the message to server where it will be broadcasted and saved in pg database.
        //The 'connectedWith' column in 'users' table is updated.
        // The attachment is sent below in 'uploadAttachment'.
        // expected boolean callback 'seen'
        //String[] objects = {toId, jsonObjectMessage.toString()};
        String[] objects = {jsonObjectMessage.toString()}; //Le json sous forme de string contient l'objet 'message' et 'toId'.
        //socket.emit("messagedetection", objects);

        //setListen_messagedetection();
        //setListen_uploadMessageComplete();
        socket.emit("messagedetection", objects, new Ack() { //the response is found in 'setListen_message_detection_back'
            @Override
            public void call(Object... args) {
                //TODO process ACK
                boolean isMessageRead = (boolean)args[0];

                //update the displayed messages with 'seen' information only in the case 'isMessageRead=1'
                //By default the message is not seen : seen=0, isMessageRead = false
                if(isMessageRead){
                    message.seen = "1";
                }

                /*
                if(!response.equals("success")){
                    //Notify the user, the massage is not sent.
                    Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content),
                            "The message is not sent", Snackbar.LENGTH_LONG).show();

                    //Remove the message from the message sent.
                }
                */
                //Ack ack = (Ack) args[args.length - 1];
                //ack.call();
            }
        });
    }


    public static String byteArrayToString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return buffer.toString();
    }

    public static String SHA1() {
        //test SHA1("123") will result in : 40bd001563085fc35165329ea1ff5c5ecbdbbeef
        Random r = new Random();
        int a = Math.abs(r.nextInt());

        String timeString   = String.valueOf(new Date().getTime());
        String randomString = String.valueOf(a);
        String clearString = timeString + randomString;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            return byteArrayToString(messageDigest.digest());
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }

    //Get path from uri
    private String getPath(final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if(isKitKat) {
            // MediaStore (and general)
            return getPathFromUriForApi19(uri);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA; //"MediaColumns.DATA"; ne marche pas // original "_data"; ne marche pas
        final String[] projection = {
                column
        };

        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @SuppressLint("NewApi")
    @TargetApi(19)
    private String getPathFromUriForApi19(Uri uri) {
        //Log.e(tag, "+++ API 19 URI :: " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //Log.e(tag, "+++ Document URI");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                //Log.e(tag, "+++ External Document URI");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    //Log.e(tag, "+++ Primary External Document URI");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }else{
                    // Handle non-primary volumes
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        //getExternalMediaDirs() added in API 21
                        File[] external = getExternalMediaDirs();
                        if (external.length > 1) {
                            String filePath = external[1].getAbsolutePath();
                            return filePath.substring(0, filePath.indexOf("Android")) + split[1]; //like /storage/3334-6339/Download/filename
                        }
                    } else {
                        return "/storage/" + type + "/" + split[1];
                    }
                }
            }//end external storage
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                //Log.e(tag, "+++ Downloads External Document URI");
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                //Log.e(tag, "+++ Media Document URI");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    //Log.e(tag, "+++ Image Media Document URI");
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    //Log.e(tag, "+++ Video Media Document URI");
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    //Log.e(tag, "+++ Audio Media Document URI");
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //Log.e(tag, "+++ No DOCUMENT URI :: CONTENT ");

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //Log.e(tag, "+++ No DOCUMENT URI :: FILE ");
            return uri.getPath();
        }
        return null;
    }

    @SuppressLint("NewApi")
    private String getPathFromUri(Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        //String id = wholeID.split(":")[1];
        String id = wholeID;

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }


    //After clicking on user on 'Connections' pane, we arrive here.
    //From here we can :
    // --send data to 'ChatBoxMessage.displayReceivedData' so it can display messages for that user.
    // --emit to 'selectedChatUser' to tell him we have read his messages.
    @Override
    public void sendUserData(ChatUser selectedChatUser) {
        //ChatUser selectedChatUser0 = selectedChatUser;

        //The 'selectedChatUser' exists in the 'notSeenMessages' array list
        //for(NotSeenMessage notSeenMessage : notSeenMessages){
        //        String exists = notSeenMessage.nickname.equals(selectedChatUser.nickname) ? "true" : "false";
        //}

        /*
        //get not seen messages
        socket.emit("get_not_seen_messages", selectedChatUser.getNickname(), Nickname, null);
        socket.on("get_not_seen_messages_res", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray json_data = (JSONArray) args[0];
                        if(json_data != null){
                            JSONObject[] jsonObjects = new JSONObject[json_data.length()];
                            for(int i = 0; i <= json_data.length() - 1; i++){
                                try {
                                    jsonObjects[i] = (JSONObject)json_data.get(i);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            //populate 'Message' array list
                            //Le nom des keys de 'jsonObjects' doivent correspondre aux noms des propriéts de 'Messages'
                            //S'il n'y a pas de correspondance, dans 'gson.fromJson' les propriétés de 'Message' vont être nulles.

                            ArrayList<Message> notSeenMessages = new ArrayList<>();
                            Gson gson = new Gson();

                            for(int i = 0; i <= jsonObjects.length - 1; i++){
                                Message message = gson.fromJson(jsonObjects[i].toString(), Message.class);
                                notSeenMessages.add(message);
                            }

                            //save the 'notSeenMessages
                            if(notSeenMessages.size() > 0){
                                int nbMessagesSaved = saveMessages(notSeenMessages);
                                if(nbMessagesSaved != notSeenMessages.size())throw new UnsupportedOperationException(" not seen messages are not saved");
                            }
                        }

                        //do the next
                        sendUserData_(selectedChatUser0); // see first statement
                    }
                });
            }
        });
        */

        //do the next
        sendUserData_(selectedChatUser); // see first statement
    }

    /**
     * Save the array list message . The save is done  in table 'chat_messages' in database 'chat.db'.
     * The database will be found in  'data/data/<package>/databases/chat.db
     * Return value is an uri of the added message.
     * @param messages an array list of objet 'Message' to save.
     * @return the number of rows inserted.
     */

    public int saveMessages(ArrayList<Message> messages) {
        
        ContentValues[] values = new ContentValues[messages.size()];
        for(int i = 0; i <= messages.size() - 1; i++){
            ContentValues value = new ContentValues();
            value.put(MessagesContract.COLUMN_FROM, messages.get(i).fromNickname);
            value.put(MessagesContract.COLUMN_TO, messages.get(i).toNickname);
            value.put(MessagesContract.COLUMN_MESSAGE, messages.get(i).message);
            value.put(MessagesContract.COLUMN_REFERENCE, messages.get(i).ref);
            value.put(MessagesContract.COLUMN_DATE, messages.get(i).time);
            value.put(MessagesContract.COLUMN_EXTRA, messages.get(i).extra);        //may be null
            value.put(MessagesContract.COLUMN_EXTRANAME, messages.get(i).extraName);//may be null
            value.put(MessagesContract.COLUMN_MIME, messages.get(i).mimeType);      //may be null
            value.put(MessagesContract.COLUMN_SEEN, messages.get(i).seen);
            value.put(MessagesContract.COLUMN_DELETED_FROM, messages.get(i).deletedFrom);
            value.put(MessagesContract.COLUMN_DELETED_TO, messages.get(i).deletedTo);

            if(messages.get(i).extra == null){
                value.putNull(MessagesContract.COLUMN_EXTRA);
                value.putNull(MessagesContract.COLUMN_EXTRANAME);
                value.putNull(MessagesContract.COLUMN_MIME);
            }
            values[i] = value;
        }

        int nbRows = getContentResolver().bulkInsert(MessagesContract.CONTENT_URI_MESSAGES, values);

        if(nbRows != messages.size()) throw new UnsupportedOperationException("Unexpected value. Insert bulk messages");

        return nbRows;
    }

    public void sendUserData_(ChatUser selectedChatUser) {
        //To know wich user is currently selected. Set a global variable
        this.selectedNickname = selectedChatUser.getNickname();

        // We do 2 things in emit :
        // 1- Send the current user 'Nickname' and the selected nickname 'selectedNickname' to server.
        // to make a pair.
        // 2- In server side, the 'seen' field of the message sent by 'Nickname' to
        // 'selectedChatUser.nickname' is set to 1
        // the server return in 'messages_read_back'
        // In client side, all the messages sent by 'Nickname' to 'selectedChatUser.nickname'
        // are set seen 'seen=1' in 'ChatboxMessage.updateMessages'

        socket.emit("current_selected_user", Nickname, selectedChatUser.nickname, selectedChatUser.chatId );

        //tell 'selectedChatUser' we have read his messages, 'Nickname' is the current user
        //socket.emit("messages_read", Nickname, selectedChatUser.chatId); //from = Nickname, to = selectedChatUser.chatId

        //Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + ViewPager.getCurrentItem());
        // based on the current position you can then cast the page to the correct Fragment class and call some method inside that fragment to reload the data:
        //if (0 == ViewPager.getCurrentItem() && null != fragment) {
        //    ((TabFragment1)fragment).reloadFragmentData();
        //}

        //not used
        sectionsPagerAdapter.getItem(viewPager.getCurrentItem()); //return fragment associeted with that position

        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        //
        //Les fragments sont ajoutés au viewPager dans l'adapter 'SectionsPagerAdapter' sans tag.
        //La recherche de fragments par tag retourne null.
        //
        //getSupportFragmentManager().executePendingTransactions();
        //String tag = "android:switcher:" + R.id.view_pager + ":" + 1;
        //ChatBoxMessage f = (ChatBoxMessage) getSupportFragmentManager().findFragmentByTag(tag);
        //getSupportFragmentManager().executePendingTransactions();

        //contournement du tag
        int index = 0;
        int i = 0;
        for(Fragment f : fragments){
            if(ChatBoxMessage.class.isInstance(f)) index = i;
            i++;
        }

        ChatBoxMessage f = (ChatBoxMessage)fragments.get(index);

        //f.getTag();
        //f.displayReceivedData(nickname, id, messageListUser);

        f.displayReceivedData(selectedChatUser, currentChatUser);

        //update the adapter of view pager
        //sectionsPagerAdapter.notifyDataSetChanged();

        //Select the tab to display
        tabs.getTabAt(1).select();

        /*
        // Il a un pb : le fragment ne peut pas etre remplacé
        //Send data to fragment 'ChatBoxMessage'
        ChatBoxMessage fragmentTarget = new ChatBoxMessage(); //fragment receiver or target
        Bundle args = new Bundle();
        args.putString("nickname", nickname);
        args.putString("id", id);
        fragmentTarget.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.rl_chat_box_message, f ) //Basically in transaction.replace() the first parameter will be the id of the layout (linear layout, relative layout, ...) in which you want to place your fragment. And the second parameter should be the fragment.
                .commit();
        */

    }

    // When we click in 'holder.linkAttachmentFrom.setOnClickListener(new View.OnClickListener()' or
    // in 'holder.linkAttachmentTo.setOnClickListener(new View.OnClickListener()' in 'ChatBoxAdapter',
    // We arrive here.
    // 1st case : 'holder.linkAttachmentFrom.setOnClickListener(...) is clicked :
    //  The picture is in gallery (not in local db). Its uri is in 'Preferences', open it in intent and show it.
    // 2nd case : 'holder.linkAttachmentTo.setOnClickListener(...) is clicked :
    //  Here we face the question :the picture we are clicking is it already saved locally or not ?
    //  1st case : the picture is not already saved locally (not found in Preferences)
    //      The download of the attachment is triggered in : 'socket.emit("start_download", reference);'
    //      it is downloaded in chunks. At the end of downloading the picture is saved in Mediastore,
    //      saved localy (Preferences) and displayed in intent. See : 'socket.on("download_file_chunks", new Emitter.Listener()'
    //  2nd case : the picture is already saved locally (found in Preferences)
    //      no need to download it again. Simply, display it in intent.
    //
    @Override
    public void getAttachment(String reference) {

        //Check if the attachment is already in gallery. If not, send request to server to download it.
        if(isAttachmentInGallery(reference)){
            //Here, the attachment is already present in gallery. No need to download it again from the server.
            //Show it in intent.

            Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "Opening in intent", Snackbar.LENGTH_LONG).show();

            showAttachment(reference, null, null);
         }else{
            //Here, the attachment is not present in gallery. Download it from the server, save it in Mediastore,
            // save it locally (Preferences) and Show it in intent.
            //send request to server to download attachment.
            //The full download is done in 'socket.on("download_file_chunks", new Emitter.Listener()'

            Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "Downloading", Snackbar.LENGTH_LONG).show();

            socket.emit("start_download", reference);//the response is found in : 'socket.on('download_chunks', json)'
        }
    }


    private void showAttachment(String reference, Uri uri, String intentType) {
        //Log.i("showAttachment 0", "intentType " + intentType);
        //Get uri from reference
        if(reference != null){
            String uri_ = MainActivity.preferences.getString(reference, null);
            uri         = Uri.parse(uri_);
            intentType  = getMimeType(uri);
        }

        if(intentType == null)throw new UnsupportedOperationException("'ShowAttachment' getType is null");

        //if(intentType.equals("image/jpeg")) intentType = "image/jpg";

        //test uri, if there is a bitmap associated with the uri
        /*
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        //Log.i("showAttachment 1", "intentType " + intentType);

        //display the attachment in intent.
        Intent target = new Intent(Intent.ACTION_VIEW);
        //intentType = "image/*";
        target.setDataAndType(uri, intentType); //"application/pdf", "audio/*; "image/png", "image/jpg", "image/jpeg"
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //list of apps in device managing 'ACTION_VIEW'
        /*
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(target, 0);
        ArrayList<Intent> targetIntents = new ArrayList<Intent>();
        for (ResolveInfo currentInfo : activities) {
            String packageName = currentInfo.activityInfo.packageName;
            String name = currentInfo.activityInfo.name;
        }
        */
        ////////////////////////////////////////////////////////////////////////////////////////////

        //Set chooser
        Intent intent = Intent.createChooser(target, "Open File with : ");
        try {
            //startActivityForResult(intent, GET_VIEW_REQUEST_CODE);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Instruct the user to install a PDF reader here, or something
            e.getMessage();
        }
    }

    /**
     * Check if attachment is present in 'Gallery'. To do this, check the 'preferences' since when
     * the attachment is saved in Mediastore an entry is added in preferences.
     * //We can also search in Mediastore if the name of the attachment is already here.
     * //MediaStore.MediaColumns.DISPLAY_NAME == name of attachment
     * The name of the attachment can be get from message which is linked to the attachment.
     *
     * @param reference of the attachment in 'Preferences'
     * @return true, if the attachment is found and false otherwise.
     */
    private boolean isAttachmentInGallery(String reference) {
        String uri = MainActivity.preferences.getString(reference, null);
        //if(reference_ == null)return false;
        //return (reference_.equals(reference));
        return (uri != null);
    }

    @Override
    public void displayAttachment(ImageView thumbAttachmentFrom) {
        this.thumbAttachmentFrom = thumbAttachmentFrom;
    }

    /**
     *
     * @param bitmap to be displayed in dialog
     * @param dialog a dialog without 'cancel' 'OK' buttons.
     * @return
     */
    private View enlargeView(Bitmap bitmap, Dialog dialog) {
        //The simple clic enlarge the image. A long clic on the enlarged image copy it on the imageView.
        //Enlarge the image

        // custom dialog
        //dialog = new Dialog(context);    //context must refer to activity. See Catalogue call to 'CustomAdapter'
        dialog.setContentView(R.layout.popup);
        dialog.setTitle("Title...");
        dialog.show();

        // set the custom dialog components - text, image and button
        //TextView text = (TextView) dialog.findViewById(R.id.text);
        //text.setText("Android custom dialog example!");
        ImageView imageView1 = (ImageView) dialog.findViewById(R.id.imagePopup);
        TextView tvShare     = (TextView) dialog.findViewById(R.id.tv_share);
        TextView tvDownload  = (TextView) dialog.findViewById(R.id.tv_download);
        TextView tvQuit      = (TextView) dialog.findViewById(R.id.tv_quit);

       //Event : Quit
        tvQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvQuit Clicked
                //Toast. makeText(TabChatActivity.this, "Quit clicked", Toast. LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        //Event : Share
        tvShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvShare Clicked
                Toast. makeText(TabChatActivity.this, "Share clicked", Toast. LENGTH_LONG).show();
                Uri uri = saveImage(bitmap);
                shareImageUri(uri);

                //shareBitmap(bitmap);
            }
        });

        //Event : Download
        tvDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvDownload Clicked
                Toast. makeText(TabChatActivity.this, "Download clicked", Toast. LENGTH_LONG).show();
                Uri uri = saveFileToExternalStorage(bitmap);
            }
        });

        //Get the screen size
        //DisplayMetrics dm = context.getResources().getDisplayMetrics();
        //int widthPixels = dm.widthPixels;

        //Get the dimensions of the screen
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int screenWidth   = dm.widthPixels;
        int screenHeight  = dm.heightPixels;

        //Create a bitmap with dimensions a physical screen size.
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, screenWidth, screenWidth, true);

        imageView1.setImageBitmap(bitmapResized);

        return imageView1;
    }


    private Uri saveFileToExternalStorage(Bitmap bitmap) {
        //TODO - Should be processed in another thread
        //File imagesFolder = new File(getCacheDir(), "images");
        File imagesFolder = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "images");
        File imagesFile   = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "shared_image.jpg");
        //File imagesFolder_ = new File(getFilesDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(imagesFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.google.amara.chattab.fileprovider", imagesFile);

            String url = MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), "");

        } catch (IOException e) {
            //Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
            e.getMessage();
        }
        return uri;

        /*
        Environment.getExternalStorageDirectory(), if this were Windows, would return C:\.
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        if this were Windows, would return some standard location on the C:\ drive where the user would typically look to find saved movies.


        //final String PDF_PATH = Environment.getExternalStorageDirectory().getPath()+"/media2-1.mp4";
        final String PDF_PATH = getFilesDir().getPath()+"/media2-1.mp4";
        Environment.getExternalStorageDirectory();          //--> /storage/emulated/0/Pictures, /storage/emulated/0/DCIM, ... ATTENTION : il y a une différence entre 'getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)'.
        Environment.getExternalStorageDirectory().list();   //-->Pictures, DCIM, Movies, ...
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); //--->/storage/emulated/0/Android/data/com.google.amara.chattab/files/Download

        try {
            //File outputFile = new File(PDF_PATH);     //internal storage
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/shared_image.jpg"); //--> se trouve dans sdCard/download ou storage/emulated/0/download
            //File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "/media2-3.mp4");


            uri = FileProvider.getUriForFile(this, "com.google.amara.chattab.fileprovider", outputFile);

        }catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        */

    }


    /**
     * Saves the image as JPG to the app's cache directory. It is located at :/data/data/<package>/images
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    private Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(getCacheDir(), "images");
        //File imagesFolder = new File(getFilesDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.google.amara.chattab.myFileprovider", file);

        } catch (IOException e) {
            //Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
            e.getMessage();
        }
        return uri;
    }

    /**
     * Saves the base-64 string as a file to the app directory :/data/data/<package>/<DIRECTORY_PICTURES>
     * @param base64String base64 string to save in file.
     * @return Uri of the saved file or null.
     */
    private Uri saveBase64String(String base64String, String mimeType, String ref) {
        //TODO - Should be processed in another thread

        //Get file type or extension (not mime-type) from base-64 string.
        String authority = null;
        String environmentFolder = null;
        switch (mimeType) {//check file's extension
            case "application/pdf":
                authority = "com.google.amara.chattab.PdfFileprovider";
                environmentFolder = Environment.DIRECTORY_DOWNLOADS;    //storage/emulated/0/Download
                break;
            case "audio/mp3":
                authority = "com.google.amara.chattab.Mp3Fileprovider";
                environmentFolder = Environment.DIRECTORY_MUSIC;        //storage/emulated/0/Music
                break;
            case "image/jpg":
                authority = "com.google.amara.chattab.JpgFileprovider";
                environmentFolder = Environment.DIRECTORY_PICTURES;     //storage/emulated/0/Pictures
                break;
            case "image/jpeg":
                authority = "com.google.amara.chattab.JpegFileprovider";
                environmentFolder = Environment.DIRECTORY_PICTURES;
                break;
            case "txt/plain":
                authority = "com.google.amara.chattab.TxtFileprovider";
                //environmentFolder = Environment.DIRECTORY_DOCUMENTS;    //Api 19
                environmentFolder = Environment.DIRECTORY_DOWNLOADS; //workaround api 19 limitation
                break;
            default:
                authority = null;
                break;
        }

        // Get bytes from base-64 string
        byte[] fileAsBytes = Base64.decode(base64String.toString(), 0); //default flag = 0.

        //target folder where the bytes will be written
        //File fileFolder = new File(getCacheDir(), "jpg/");  //type = jpg, png, pdf, ....
        //File fileFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File fileFolder = Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_DOWNLOADS);

        File fileFolder = Environment.getExternalStoragePublicDirectory(environmentFolder);

        //File fileFolder  = new File(fileFolder_, "/");  //type = jpg, png, pdf, ....


        //File imagesFolder = new File(getFilesDir(), "images");
        Uri uri = null;
        long time = new Date().getTime();
        try {
            fileFolder.mkdirs();
            //target file where the bytes will be written
            //file = new File(fileFolder, getFilename(ref)+"."+mimeType);

            //Get extension from mime type
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(mimeType);

            //'file ' is used in mediastore see 'downloadAddFile' :  file.getAbsolutePath()
            file = new File(fileFolder, getFilename(ref)); // + "." + extension);

            //check the extension
            Uri uriFile          = Uri.fromFile(file);
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uriFile.toString());

            //String downloadAbsolutePath = file.getAbsolutePath(); //used in mediastore see 'downloadAddFile'

            FileOutputStream fos = new FileOutputStream(file);

            fos.write(fileAsBytes);
            fos.flush();
            fos.close();
            uri = FileProvider.getUriForFile(this, authority, file);

            //for testing purpose
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] {file.toString() }, new String[]{mimeType},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {

                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                            //Add entry in 'Preferences'
                            if(uri != null){
                                SharedPreferences.Editor editor = MainActivity.preferences.edit();
                                //editor.putString(reference, imageUri.toString());
                                editor.putString(ref, uri.toString());
                                editor.commit();
                            }
                        }
                    }
            );
            ////////////////////////////////////////////////////////////////////////////////////////
           /*
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            File storageDir   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            File file_ = new File(storageDir, "shared_"+time+"."+mimeType);
            ///////////////////////////////////////////////////////////////////////////////////////
            //Mediastore method : working

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.TITLE, "shared_" + mimeType);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "shared_" + mimeType + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Images.Media.DATA, file_.getAbsolutePath());
            //Do not rmove
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            }

            Uri imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            ContentResolver resolver = this.getContentResolver();
            try (OutputStream stream = resolver.openOutputStream(imageUri)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                    throw new IOException("Error compressing the picture.");
                }
                stream.flush();
            } catch (Exception e) {
                if (uri != null) {
                    resolver.delete(uri, null, null);
                }
                e.getMessage();
            }
            */
            ////////////////////////////////////////////////////////////////////////////////////////

        } catch (IOException e) {
            //Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
            e.getMessage();
        }
        return uri;
    }


    /**
     * Shares the JPG image from Uri.
     * @param uri Uri of image to share.
     */
    private void shareImageUri(Uri uri){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        Intent chooser = Intent.createChooser(intent, "Chooser Title");
        chooser.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivity(chooser);
        //startActivity(intent);
    }


    private HashSet<Integer> getUniqueNumbers(int range, int size){
        HashSet<Integer> numbers = new HashSet<>(size);
        while (numbers.size() < size) {
            int number = new Random().nextInt(range-1) + 1;
            numbers.add(number);
        }
        return numbers;
    }

    /**
     * Returns the Uri which can be used to delete/work with images in the photo gallery.
     * @param displayName Path to IMAGE on SD card
     * @return Uri in the format of... content://media/external/images/media/[NUMBER]
     */
    private Uri getUriFromPath(String displayName) {
        long photoId;
        Uri photoUri = MediaStore.Images.Media.getContentUri("external");

        String[] projection = {MediaStore.Images.ImageColumns._ID};
        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = getContentResolver().query(
                photoUri,
                projection,
                MediaStore.Images.ImageColumns.DISPLAY_NAME + " LIKE ?", new String[] { displayName },
                null
        );
        assert cursor != null;
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        photoId = cursor.getLong(columnIndex);

        cursor.close();
        return Uri.parse(photoUri.toString() + "/" + photoId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //le retour de 'DisplayAllUsersActivity'
        if (requestCode == REQUEST_CODE_ALL_USERS &&   // send code for login
                resultCode == INTENT_NEW_USER_RESULT_CODE && // receive code when login ends.
                null != data) {
            ChatUser chatUser = data.getParcelableExtra("new_chat_user");

            //Save this 'chatUser' locally. It is present already in server.
            int nbRowsSaved = saveChatUser(chatUser);
            if(nbRowsSaved == 0)throw new UnsupportedOperationException("added new user is not saved");

            //Send this 'chatUser' to 'ChatBoxActivity' fragment to display it in list.
            //Get list of fragments
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            int index  = 0;
            int index_ = 0;
            int i = 0;
            for(Fragment f : fragments){
                if(ChatBoxActivity.class.isInstance(f)) index = i;
                if(ChatBoxMessage.class.isInstance(f)) index_ = i;
                i++;
            }

            ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
            //ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

            f.displayReceivedNewUser(chatUser);
            //f_.btn.setEnabled(true);
            //f_.performClickNext_(); //send the message
        }
    }

    /**
     * Get mime type like : 'application/jpeg', ...
     * @param uri the supplied uri
     * @return a mime type like : 'application/jpeg', ...
     */
    public String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            //fileExtension is like 'jpg', 'jpeg', 'png', ...
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());

            // mime type is like : 'application/jpeg', ...
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase());
        }
        return mimeType;
    }

    @Override
    public void notifyBlacklist(String authorBlacklist,
                                String blacklistedNickname,
                                String blacklistedNicknameId,
                                String blacklist) {
        //notify the user 'nickname' that it is blacklisted
        //build the json to send to server


        JSONObject jsonObjectData = new JSONObject();
        try {
            jsonObjectData.put("blacklistAuthor", authorBlacklist);
            jsonObjectData.put("blacklistNickname", blacklistedNickname);
            jsonObjectData.put("blacklistId", blacklistedNicknameId);
            jsonObjectData.put("blacklistStatus", blacklist);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String[] data = {jsonObjectData.toString()};

        socket.emit("blacklist", (Object[]) data);
    }

    //@Override
    public void fragmentAttached() {
        Fragment fragment = (Fragment) getSupportFragmentManager().
                findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + viewPager.getCurrentItem());

        //get the fragment by tag
         //chatBoxActivity = (ChatBoxActivity) getSupportFragmentManager().findFragmentByTag("chat_box_user");
        //if(chatBoxMessage == null) return;
         //initSocket1();

        /*
        for(ChatUser chatUser : lastSessionUsers){
            chatBoxActivity.displayReceivedNewUser(chatUser);
            //f_.setChatListUsers(chatUser);
        }
        */

        /*
        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        int index  = 0;
        int index_ = 0;
        int i      = 0;

        for(Fragment f0 : fragments){
            if(ChatBoxActivity.class.isInstance(f0)) index = i;
            if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
            i++;
        }

        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
        */

        //ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

        //ChatBoxActivity f = (ChatBoxActivity) sectionsPagerAdapter.getItem(index);
        //ChatBoxMessage f_ = (ChatBoxMessage)  sectionsPagerAdapter.getItem(index_);

        //ChatBoxActivity sf = (ChatBoxActivity)getSupportFragmentManager()
        //        .findFragmentById(R.id.search_result);

        /*
        for(ChatUser chatUser : lastSessionUsers){
            f.displayReceivedNewUser(chatUser);
            //f_.setChatListUsers(chatUser);
        }
        */
    }

    Emitter.Listener mConnectionErrorListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Error connection, notify the user
                    AlertDialog.Builder builder = new AlertDialog.Builder(TabChatActivity.this);
                    builder.setTitle("Connection error");
                    builder.setMessage("No connection with the server");

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            socket.close();
                            finish();
                            return;
                        }
                    });

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    //.setNegativeButton(android.R.string.no, null)
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    //.show();

                    AlertDialog alert = builder.create();
                    alert.show();

                }
            });
        }
    };

    Emitter.Listener mConnectionListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(TabChatActivity.this.findViewById(android.R.id.content), "Connection", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    };

    //after clicking in add users in 'ChatBoxActivity' we arrive here.
    @Override
    public void getAllUsersList() {
        //getSupportFragmentManager().beginTransaction()
        //            .replace(R.id.view_pager, AllUsersFragment.newInstance())
        //            .commitNow();

        socket.emit("get_all_users"); //the response of emit is found in : 'setListen_getAllUsers()'
    }

    //After clicking on a user in the list of all users registered, we arrive here.
    //The selected user is displayed in the list of chat.
    @Override
    public void sendUserReference(ChatUser chatUser) {
        //Send this 'chatUser' to 'ChatBoxActivity' fragment to display it in list.
        //Get list of fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        int index  = 0;
        int index_ = 0;
        int i = 0;
        for(Fragment f : fragments){
            if(ChatBoxActivity.class.isInstance(f)) index = i;
            if(ChatBoxMessage.class.isInstance(f)) index_ = i;
            i++;
        }

        ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
        //ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

        f.displayReceivedNewUser(chatUser);
        //f_.btn.setEnabled(true);
        //f_.performClickNext_(); //send the message
    }
}