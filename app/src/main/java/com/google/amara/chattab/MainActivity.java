package com.google.amara.chattab;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

//import com.google.amara.authenticationretrofit.ChangePasswordActivity;
import com.google.amara.chattab.ui.main.ChatRepository;
import com.google.amara.chattab.ui.main.ChatSharedViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import com.androidcodeman.simpleimagegallery.ImageProfileGalleryMainActivity;
//import com.google.gson.Gson;


//public class MainActivity extends Fragment {
public class MainActivity extends AppCompatActivity
        implements Thread.UncaughtExceptionHandler{

    private static final String RECIPIENT = "tomcat.user@yahoo.co.in";
    private static final long BAN_TIME    = 3600000; //1 hour

    private static final int INTENT_REQUEST_CODE  = 100;
    private static final int INTENT_RESULT_CODE   = 101;
    private static final int CAMERA_REQUEST_CODE  = 200;
    private static final int REQUEST_CODE_CAMERA  = 300;
    private static final int REQUEST_CODE_IMAGE_PROFILE_HISTORY = 400;
    private static final int REQUEST_CODE_GALLERY = 500;
    private static final int REQUEST_CODE_LOGIN   = 600;
    private static final int RESULT_CODE_LOGIN    = 601;
    private static final int REQUEST_CODE_AUTH    = 700;
    private static final int RESULT_CODE_AUTH     = 701;
    private static final int REQUEST_CODE_CHAT    = 800;
    private static final int RESULT_CODE_CHAT     = 801;


    //used in postLogin
    private static final int SUCCESS  = 0;
    private static final int REMEMBER = 1;
    private static final int QUIT     = 2;
    private static final int TIMEOUT  = 3;
    private static final int BAN      = 4;

    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private static final Object MAX_RETRIES = 3;



    private Button      btn;
    private EditText    nickname;
    private ImageView   ivParam, imageProfile, camera, gallery, folder, history, delete;
    private TextView    notSeenMessagesSummary;

    public static String NICKNAME, NICKNAME_TEMP;;
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    //camera fields
    private Uri photoURI;
    private String imageFileName;
    private String currentPhotoPath;
    private static final int REQUEST_TAKE_PHOTO    = 200;
    private ChatUser currentChatUser;
    private String imageProfile_;   //base 64 string
    private Uri profileImageUri;    //uri of the profile image

    public Socket socket;
    private long  connectedAt, lastConnectedAt, disconnectedAt;
    private ChatUser[]  lastSessionUsers;

    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private JSONArray         allNotSeenMessages;

    private boolean     imageProfileHistoryUrisSavedLocally = false;
    private String      oldImageProfile;
    private JSONArray   jsonArray2; //contains not seen messages authors
    private boolean     display;    //used in socket.on
    private String      dispatch;   //used in socket.on

    //manage user interaction
    private Handler     handler = new Handler();
    private Runnable    runnable;

    //network connection
    private static boolean    network           = false;
    private BroadcastReceiver myReceiver        = null; //manage network status

    private static String uniqueID              = null;
    private static final String PREF_UNIQUE_ID  = "PREF_UNIQUE_ID";
    private static final String PREFS_NAME      = "myAppPrefs";
    private static final String PIN_PREF        = "pin_pref";


    public static final String JWT_TOKEN   = MainApplication.JWT_TOKEN;

    //Fanny
    //"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQxOSwidXNlcm5hbWUiOiJGYW5ueTEiLCJpYXQiOjE3NzUwMzUwMjAsImV4cCI6MTc3NTEyMTQyMH0.PWNd-IFN8WtLlGatnVmlCNDlmhm3AOuvJ-YgFu5DB2c";

    private static final String FCM_UPDATED_AT = "aaa";

    private ChatSharedViewModel vm;

    public enum LastUsersDataHolder {
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

    private String getDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new Date(time));
    }

    public interface FirebaseIdCallback{
        void onSuccess(String firebaseId);
        void onFailure(Exception exception);
    }

    public interface FirebaseIdCallback_{
        void onSuccess(String firebaseId);
        void onFailure(Exception exception);
    }

    public interface SendTokensToBackendCallback{
        void onSuccess();
        void onFailure(Exception exception);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main__);//activity_main__ = linear layout, activity_main_ = relative layout

        //call UI component  by id
        ivParam      = findViewById(R.id.iv_parameter);
        imageProfile = findViewById(R.id.image_profile);

        camera       = findViewById(R.id.iv_camera_);
        gallery      = findViewById(R.id.iv_gallery);
        folder       = findViewById(R.id.iv_folder);
        history      = findViewById(R.id.iv_history);
        delete       = findViewById(R.id.iv_delete);

        nickname     = (EditText) findViewById(R.id.nickname);
        btn          = (Button) findViewById(R.id.enter_chat);

        //notSeenMessagesSummary = (TextView) findViewById(R.id.tv_not_seen_messages_summary);

        preferences  = null; //NavigatorActivity.preferences;
        editor       = null; //NavigatorActivity.editor;

        //get extra if any
        Bundle extras   = getIntent().getExtras();
        if(extras != null){ //we come from 'NavigatorActivity', there is an extra
            String NICKNAME = extras.getString("NICKNAME");
            nickname.setText(NICKNAME);
            Intent intent = getIntent();

            String senderId   = intent.getStringExtra("senderId");
            String senderName = intent.getStringExtra("senderName");
        }

        //socket is defined in 'MainApplication'
        //socket = SocketManager.getSocket();
        //SocketManager.connect();

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    "chat_channel",              // 🔥 MUST MATCH backend EXACTLY
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Chat notifications");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        */

        //send avatar-image to backend.
        String avatarBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAUA..."; // short Base64 placeholder
        JSONObject payload  = new JSONObject();
        String avatarUrl    = null;
        try {
            payload.put("avatarBase64", avatarBase64); // or null
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        vm = new ViewModelProvider(this)
                .get(ChatSharedViewModel.class);

        vm.startChat();

        /*
        vm.getUsers().observe(this, users -> {
            if (users == null) return;

            Log.d("MainActivity", "👥 Users = " + users.size());
            for (ChatUser u : users) {
                Log.d("MainActivity", "User: " + u.getNickname());
            }
        });
        */

        //FCM
        if(MainApplication.FCM){
            FirebaseApp.initializeApp(this);
            checkFirebaseId();
        }


        //get the users list with unread messages
        //SocketManager.getSocket().emit("chat:get_users_with_unread");


        ////////////////////////////////////////////////////////////////////////////////////////////
        //The 'Setting' is first launched and followed by 'NavigatorActivity' witch transmit 'socket' to all
        //activities they need it.
        //The socket is setup in 'NavigatorActivity', get it via 'SocketHandler'
        //socket       = SocketHandler.getSocket();

        //socket.on(Socket.EVENT_CONNECT_ERROR, mConnectionErrorListener);
        //socket.on(Socket.EVENT_CONNECT, mConnectionListener);

        //setEmitterListener_note_by_id_res();//test to remove in production

        /*
        setEmitterListener_get_all_not_seen_messages_res();
        //setEmitterListener_save_get_image_profile_uri_back();
        setEmitterListener_get_image_profile_uri_back();
        setEmitterListener_get_user_back();
        setEmitterListener_is_user_present_in_db_back();
        setEmitterListener_get_user_ban_back();
        */

        //test
        //setEmitterListener_test_back();

        /*
        //Socket socket = SocketManager.getSocket();
        //test socket, remove in production
        socket.emit("test_socket", "test socket", new Ack() {
            @Override
            public void call(Object... args) {
                String res =  (String) args[0];
            }
        });
        */

        //for test only. server-browser running on 8000. the client is the browser
        ///socket.emit("chat_message", "Hello the world");



    }//end 'onCreate'

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String senderId = intent.getStringExtra("senderId");
        //openChat(senderId);
    }

    private void checkFirebaseId() {
        // Simulating async Firebase ID check
        getFirebaseId_(this, new FirebaseIdCallback() {
            @Override
            public void onSuccess(String firebaseId) {
                // Store Firebase ID or log it
                Log.d("StartupState", "Firebase ID: " + firebaseId);

                //store the fcm token in 'SharedPreferences'
                SharedPreferences sharedPreferences = MainApplication.getSharedPreferences_();
                SharedPreferences.Editor editor     = sharedPreferences.edit();
                editor.putString("FCM_TOKEN", firebaseId);
                editor.apply();

                //send token to backend
                sendTokensToBackend(MainActivity.this,
                        firebaseId,
                        FCM_UPDATED_AT,
                        JWT_TOKEN,
                        new SendTokensToBackendCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("StartupState", "Firebase ID successfully saved in backend: ");
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                Log.d("StartupState", "Firebase failed to saved in backend");
                            }
                });
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e("StartupState", "Firebase ID fetch failed: " + exception.getMessage());
                //proceedToNextState(StartupState.SHOW_LOGIN);
            }
        });

    }

    public static void getFirebaseId_(Context context, FirebaseIdCallback callback) {
        getFirebaseId(context, ((int)(MAX_RETRIES) - 1), new FirebaseIdCallback_() {
            @Override
            public void onSuccess(String firebaseId) {
                //save 'firebaseId' in 'Shared Preferences'

                //SharedPreferences sharedPreferences = PreferenceUtils.getPreferences(context);
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

                SharedPreferences.Editor editor =sharedPreferences.edit();
                if(firebaseId != null){
                    editor.putString("FIREBASE_ID", firebaseId);
                    editor.apply();
                    callback.onSuccess(firebaseId);
                    return;
                }
                callback.onFailure(new Exception("FIREBASE_ID_NULL"));
            }

            @Override
            public void onFailure(Exception exception) {
                callback.onFailure(new Exception("Firebase id exception"));
            }
        });
    }

    public static void getFirebaseId(Context context,
                                     int retryCount,
                                     FirebaseIdCallback_ callback) {
        //Task<String> task = FirebaseInstallations.getInstance().getId();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM", "Fetching FCM token failed", task.getException());
                        callback.onFailure(task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM", "FCM Token: " + token);

                    // Send this to backend
                    callback.onSuccess(token);
                });

        /*
        if (task.isComplete()) {
            if (task.isSuccessful()) {
                String firebaseId = task.getResult();
                Log.d("TAG", "✅ Immediate Firebase ID: " + firebaseId);
                callback.onSuccess(firebaseId);
            } else {
                Log.e("TAG", "❌ Immediate Firebase ID error", task.getException());
                if (retryCount >= 0) {
                    long delay = (long) Math.pow(2, (int)(MAX_RETRIES) - retryCount) * 1000; // Exponential backoff: 1s, 2s, 4s
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            getFirebaseId(context, retryCount - 1, callback), delay);
                } else {
                    callback.onFailure(task.getException());
                }
            }
        } else {
            task.addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    String firebaseId = t.getResult();
                    Log.d("TAG", "✅ Firebase ID (delayed): " + firebaseId);
                    callback.onSuccess(firebaseId);
                } else {
                    Log.e("TAG", "❌ Firebase ID fetch failed", t.getException());
                    callback.onFailure(t.getException());
                }
            });
        }
        */
    }

    //called from 'onStart'
    private void nextInitSocket1() {

        //dispatch
        Intent intent = new Intent(MainActivity.this, TabChatActivity.class);
        startActivityForResult(intent, INTENT_REQUEST_CODE);
    }

    private void setEmitterListener_test_back() {
        socket.on("test_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject test_ = ((JSONObject) args[0]);
                        boolean test = false;
                        try {
                            test = test_.getBoolean("exists");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    private void showAuthErrorDialog(String message) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Authentication error")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Login again", (dialog, which) -> {
                        logoutAndRedirect();
                    })
                    .show();
        });
    }

    private void logoutAndRedirect() {
        com.google.amara.chattab.session.AuthSession.clearAll(); // clear jwt + refresh token
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
        //Intent intent = new Intent(this, LoginActivity.class);
        Intent intent = new Intent(AuthContract.ACTION_REQUIRE_LOGIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Send the FCM token and JWT token to backend
    private static void sendTokensToBackend(Context context, String fcmToken, String fcmUpdatedAt,
                                            String jwtToken, SendTokensToBackendCallback callback) {

        OkHttpClient okHttpClient     = new OkHttpClient();
        //OkHttpClient okHttpClient = ((MyApplication) getApplicationContext()).getHttpClient();

        //OkHttpClient okHttpClient = ((MyApplication)context.getApplicationContext()).getOkHttpClient();

        /*
        RequestBody requestBody = new FormBody.Builder()
                .add("fcm_token", fcmToken)
                .build();
        */

        RequestBody requestBody = new FormBody.Builder()
                .add("fcm_token", fcmToken)
                .add("fcm_updated_at", String.valueOf(fcmUpdatedAt))
                .build();

        //String jsonBody = "{\"fcm_token\": \"" + fcmToken + "\"}";
        //String jsonBody = "{\"fcm_updated_at\": \"" + fcmUpdatedAt + "\"}";
        //RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("https://android-chat-server.onrender.com/fcm/store-fcm-token")  // Replace with your backend URL
                //.url("http://localhost:5000/fcm/store-fcm-token")
                .addHeader("Authorization", "Bearer " + jwtToken)  // Send the JWT for authentication
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();  // Handle failure
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    //throw new IOException("Unexpected code " + response);
                    callback.onFailure(new Exception());
                }

                // Handle successful registration of FCM token
                Log.d("FCM", "FCM token sent successfully");
                //The FCM is already saved in 'Shared Preferences'
                callback.onSuccess();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        //inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem =  menu.findItem(R.id.remember_me);

        //set the value found in 'preferences'
        boolean rememberMe = true; //preferences.getBoolean("remember_me", false);
        menuItem.setChecked(rememberMe);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        switch (item.getItemId()) {

            case R.id.help:
                // do something
                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),"Help",Snackbar.LENGTH_LONG ).show();
                return true;
            case R.id.about:
                // do something

                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),"About",Snackbar.LENGTH_LONG ).show();
                */

                /*
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putParcelableArrayListExtra("elec_tranches", tranches);
                intent.putParcelableArrayListExtra("gas_tranches", gasTranches);
                intent.putExtra("others_settings", othersSettings);
                startActivityForResult(intent, REQUEST_TRANCHES_ELEC);
                */
                /*
                return true;
                */
            /*
            case R.id.remember_me:
                // do something
                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),"Remember me",Snackbar.LENGTH_LONG ).show();
                //Toggles checkbox state.
                item.setChecked(!item.isChecked());

                //save in 'Preferences'
                editor.putBoolean("remember_me", item.isChecked());
                editor.commit();
                */

                /*
                //The array list '' is sorted by date.
                intent = new Intent(MainActivity.this, HistoryActivity.class);
                intent.putStringArrayListExtra("history_settings", histories);
                startActivityForResult(intent, REQUEST_HISTORY);
                */
                /*
                return true;
                */
            /*
            case R.id.reset_pwd:

                    //item.setIconTintMode(PorterDuff.Mode.LIGHTEN);

                // do something
                Snackbar.make(MainActivity.this.findViewById(android.R.id.content),"Reset password",Snackbar.LENGTH_LONG ).show();
                //parameter clicked, show the parameter setting.
                //redirect to 'ChangePasswordActivity' in 'authentication' module.

                //check ban info in preferences. The user may be banished when attempting to change his pwd
                long startBanTime = preferences.getLong("startBanTime", 0L);
                if (startBanTime != 0) {

                    long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
                    if (remainingTime >= 0L) {
                        //the ban is not ended, notify the user and exit.
                        banishDialog(MainActivity.this, startBanTime);
                        return true;
                    } else {
                        //the ban is ended. Remove info from the Preferences and redirect to change pwd again.
                        editor.remove("startBanTime");
                        editor.commit();
                    }
                }
                //here the ban is ended or not set. redirect
                Intent intent = new Intent(MainActivity.this, ChangePasswordActivity.class);
                intent.putExtra("NICKNAME", NICKNAME);
                startActivity(intent);
                return true;

            default:
                return super.onContextItemSelected(item);

        }
        */
        return false;
    }


    @Override
    public void onStart() {
        super.onStart();
        // 'enter chat' btn event
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The button 'Enter' is clicked.
                //disable the button 'btn' to prevent multiple clicks.
                btn.setEnabled(false);

                //launch 'TabChatActivity'
                nextInitSocket1();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // call method
        //checkConnection();

        //manage user interaction
        //startHandler(); //This statement cause the interaction dialog flicking
        //Log.d("onResume", "onResume_restartActivity");


        ChatRepository.get(getApplication()).ensureSocketConnected();

        /*
        String withUserId = MainApplication.friendId; //selectedUserId; // the person I'm chatting with

        Log.d("MainActivity", "📥 withUserId : " + withUserId);

        JSONObject obj = new JSONObject();
        try {
            obj.put("withUserId", withUserId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Socket socket = SocketManager.getSocket();
        Log.d("MainActivity", "📥 socket : " + socket);
        socket.emit("chat:mark_seen", obj);
        */
    }

    public void onPostResume() {
        super.onPostResume();
        int i;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        // call method
        //checkConnection();

        //manage ser interaction
        //stopHandler();
        Log.d("onPause", "onPauseActivity change");
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        //stopHandler(); //user interaction
    }


    @Override
    protected void onRestart() {
        super.onRestart();

        //manage user interaction
        //startHandler();
        Log.d("onRestart", "onRestart_restartActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopHandler();
        //this.unregisterReceiver(myReceiver);
        //Log.d("onDestroy", "onDestroyActivity change");

    }

    //@Override
    public void finish_() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        }else{
            super.finish();
        }
    }

    @Override
    public void onBackPressed(){
        //notify the users, i'm going.
        //socket.emit("disconnect_", Nickname); //'disconnect' is a key word.
        //socket.emit(Socket.EVENT_DISCONNECT, Nickname);
        //socket.disconnect();

        //return to the caller activity 'NavigatorActivity' witch launched this activity as intent
        // Prepare data intent
        Intent intent = new Intent();
        intent.putExtra("status", "end");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Activity finished ok, return the data 'onActivityResult' of 'NavigatorActivity' in 'App' module.
        setResult(RESULT_CODE_CHAT, intent); //the data are returned to 'onActivityResult' of 'NavigatorActivity' in 'App' module which launched this intent.
        finish();//obligatoire

        super.onBackPressed();
    }




    public static Bitmap getRoundedCornerImage(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242; //Color.RED; no effect to change color outside the
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);  //no effect to change (r,g, b) color. a=255, outside the circle become white.
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap,int roundPixelSize) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = roundPixelSize;
        paint.setAntiAlias(true);
        canvas.drawRoundRect(rectF,roundPx,roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * Get uris of images profile from server and save them in db locally. Display a chooser to pick
     * an image.
     * @param nickname  the supplied username.
     */
    public void getImageProfileUris(String nickname) {
        //Ask server to get data of the current user named 'Nickname'
        socket.emit("get_image_profile_uri", nickname);
        //return imageProfileUris[0];
    }



    /**
     * Image captured from camera. Compress it and save the image in Mediastore in 'DIRECTORY_PICTURES'
     * //Get the bitmap and base64-string
     * @param data the data supplied by the camera
     */
    private void onCaptureImageResult(Intent data) {
        if(null == data.getExtras())return;
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        //File destination = new File(Environment.getExternalStorageDirectory(),
        //        System.currentTimeMillis() + ".jpg");
        File destination = new File( getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                System.currentTimeMillis() + ".jpg");

        Uri uri_ = Uri.fromFile(destination);
        String destinationAbsolutePath = destination.getAbsolutePath();

        FileOutputStream fos;
        try {
            destination.createNewFile();
            fos = new FileOutputStream(destination);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set the 'imageProfile' string used in 'Chatuser'.
        imageProfile_ = getBase64String(thumbnail);

        //Display a rounded image
        int w = thumbnail.getWidth(), h = thumbnail.getHeight();
        int radius = w > h ? h:w; // set the smallest edge as radius.

        RoundedImageView  roundedImageView = new RoundedImageView(this);
        Bitmap roundBitmap = roundedImageView.getCroppedBitmap(thumbnail, radius);

        imageProfile.setImageBitmap(roundBitmap);

        //Do nothing
        //Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        //Uri contentUri = Uri.fromFile(destination);
        //mediaScanIntent.setData(contentUri);
        //this.sendBroadcast(mediaScanIntent);

        //Save the captured image from camera in Mediastore
        //Mediastore method : working
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.ImageColumns.TITLE, destination.getName());
        contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, destination.getName());
        contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.ImageColumns.DATA, destinationAbsolutePath);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Images.ImageColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }

        //set the 'profileImageUri', the captured image is saved in 'onStart'
        ContentResolver resolver = this.getContentResolver();
        // The uri 'profileImageUri' is like : content://media/external/images/media/1630094103276
        profileImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //Disable 'Camera'. One shoot only
        camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));
        camera.setEnabled(false);

        //disable 'History'
        history.setEnabled(false);
        history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));

        //disable 'gallery'
        gallery.setEnabled(false);
        gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));

        //Enable 'Delete'
        delete.setEnabled(true);
        delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
        ////////////////////////////////////////////////////////////////////////////////////////////
    }

    private String getBase64String(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        byte[] imageBytes   = baos.toByteArray();
        String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        return base64String;
    }



    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        /*
        if (takePictureIntent.resolveActivity(getPackageManager()) == null){
            //notify the user, there is not a camera
            Snackbar.make(this.findViewById(android.R.id.content), "No camera found", Snackbar.LENGTH_LONG).show();
            return;
        }
        */
        // Create the File where the photo should go
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {

        }
        //ne sert pas. Le path peut être construit à partir de l'uri ci dessous.
        String photoPath = photoFile.getAbsolutePath();

        // Continue only if the File was successfully created
        if (photoFile != null) {
            //Uri photoURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //N=24
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.aymen.androidchat.cameraFileprovider",
                        photoFile);
            } else {
                photoURI =  Uri.fromFile(photoFile);
            }

            //If 'MediaStore.EXTRA_OUTPUT' is present, 'data' is not null in 'onActivityresult' and the uri image is found in
            //'data.getData'.
            //if 'MediaStore.EXTRA_OUTPUT'is not present, 'data' is null in 'onActivityresult' and the image is found in location
            // of 'photoUri'.

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            //takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            //Create chooser
            // Authorize the uri read and write permissions of the stored image to the camera application
            /*
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, photoURI , Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            */
            //s'il y a plusieurs apps, le chooser est proposé par défaut.
            // on peut construire un chooser personnalisé pour exclure certaines app du chooser.
            startActivityForResult(Intent.createChooser(takePictureIntent, "Select Camera Tool"), CAMERA_REQUEST_CODE);//CAMERA_REQUEST_CODE);
            //startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);

            /*
            try {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }catch (Exception e){
                e.getStackTrace();
            }
             */
            //startActivity(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        //File storageDir = Environment.getExternalStorageDirectory();
        //String storageDirState = Environment.getExternalStorageState();

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


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

    private String getPathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @SuppressLint("NewApi")
    private String getPathFromUri_(Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];
        //String id = wholeID;

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

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor       = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {

            int width  = bitmap.getWidth();
            int height = bitmap.getHeight();

            float bitmapRatio = (float)width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }


    private Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return bitmap;
    }

    public static Bitmap decodeUriToBitmap(Context mContext, Uri uri) {
        Bitmap bitmap = null;
        try {
            InputStream image_stream;
            try {
                image_stream = mContext.getContentResolver().openInputStream(uri);
                bitmap       = BitmapFactory.decodeStream(image_stream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
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
    ////////////////////////////////////////////////////////////////////////////////////////////////
    Emitter.Listener mConnectionListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "Connection", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    };

    Emitter.Listener mDisconnectionListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "Disconnect", Snackbar.LENGTH_LONG).show();

                    //Get time disconnect and save it in prefrences
                    long disconnectionTime = new Date().getTime();
                    //Todo : the 'disconnectionTime' perhaps it is not equal to the time stored in the server in the event 'socket.on('disconnect''

                    //Save in 'preferences' the 'disconnectionTime'.
                    MainActivity.editor.putLong("disconnected_at", disconnectionTime);
                    MainActivity.editor.commit();

                    socket.close();
                    finish();
                    return;
                }
            });
        }
    };

    Emitter.Listener mConnectionErrorListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Error connection, notify the user
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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



    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable throwable) {

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //Toast.makeText(getApplicationContext(), "Application crashed", Toast.LENGTH_LONG).show();

                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Crash occured. Do you want to send log report?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //startHandler();
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                String subject = "Your App is crashing, Please fix it!";
                                StringBuilder body = new StringBuilder("Error Log : ");
                                body.append('\n').append('\n');
                                body.append(getReport(throwable)).append('\n').append('\n');
                                // sendIntent.setType("text/plain");
                                sendIntent.setType("message/rfc822");
                                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { RECIPIENT });
                                sendIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
                                sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                                sendIntent.setType("message/rfc822");
                                // context.startActivity(Intent.createChooser(sendIntent, "Error Report"));
                                startActivity(sendIntent);

                                dialogInterface.dismiss();
                                System.exit(-1);
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
                Looper.loop();
            }
        }.start();

        try {
            Thread.sleep(10000); // Let the Toast or AlertDialog display before app will get shutdown
        }
        catch (InterruptedException e) {
            // Ignored.
        }
    }

    private StringBuilder getReport(Throwable throwable) {
        StringBuilder report = new StringBuilder();
        Date curDate = new Date();
        report.append("Error Report collected on : ").append(curDate.toString()).append('\n').append('\n');
        report.append("Informations :").append('\n');
        //ddInformation(report);
        report.append('\n').append('\n');
        report.append("Stack:\n");
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        // Exception will write all stack trace to string builder
        throwable.printStackTrace(printWriter);
        report.append(result.toString());
        printWriter.close();
        report.append('\n');
        report.append("**** End of current Report ***");
        //Log.e(UnCaughtException.class.getName(), "Error while sendErrorMail" + report);
        return report;
    }
}


