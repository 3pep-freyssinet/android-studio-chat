package com.google.amara.chattab;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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

import com.example.aymen.androidchat.AllUsersFragment;
import com.example.aymen.androidchat.ChatUser;
import com.example.aymen.androidchat.Message;
import com.example.aymen.androidchat.sql.LastUsersContract;
import com.example.aymen.androidchat.sql.MessagesContract;
import com.example.aymen.androidchat.sql.ProfileHistoryContract;
import com.example.aymen.androidchat.sql.UsersContract;

//import com.google.amara.authenticationretrofit.ChangePasswordActivity;
import com.google.android.material.snackbar.Snackbar;

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
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.androidcodeman.simpleimagegallery.ImageProfileGalleryMainActivity;
import com.google.gson.Gson;


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
    private List<NotSeenMessage> notSeenMessages;

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

        notSeenMessagesSummary = (TextView) findViewById(R.id.tv_not_seen_messages_summary);

        preferences  = null; //NavigatorActivity.preferences;
        editor       = null; //NavigatorActivity.editor;

        //get extra if any
        Bundle extras   = getIntent().getExtras();
        if(extras != null){ //we come from 'NavigatorActivity', there is an extra
            String NICKNAME = extras.getString("NICKNAME");
            nickname.setText(NICKNAME);
        }

        //The 'Setting' is first launched and followed by 'NavigatorActivity' witch transmit 'socket' to all
        //activities they need it.
        //The socket is setup in 'NavigatorActivity', get it via 'SocketHandler'
        socket       = SocketHandler.getSocket();

        //socket.on(Socket.EVENT_CONNECT_ERROR, mConnectionErrorListener);
        //socket.on(Socket.EVENT_CONNECT, mConnectionListener);

        //setEmitterListener_note_by_id_res();//test to remove in production

        setEmitterListener_get_all_not_seen_messages_res();
        //setEmitterListener_save_get_image_profile_uri_back();
        setEmitterListener_get_image_profile_uri_back();
        setEmitterListener_get_user_back();
        setEmitterListener_is_user_present_in_db_back();
        setEmitterListener_get_user_ban_back();

        socket.connect();

        //test socket, remove in production
        socket.emit("test_socket", "in onCreate", new Ack() {
            @Override
            public void call(Object... args) {
                String res =  (String) args[0];
            }
        });

        //for test only. server-browser running on 8000. the client is the browser
        socket.emit("chat_message", "Hello the world");

        //if (true)return;

        //get app id
        String applicationId = getPackageName();

        ////////////////////////////////////////////////////////////////////////////////////////////
        //Set permissions
        ////////////////////////////////////////////////////////////////////////////////////////////
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);

        //get the permissions to request
        permissionsToRequest = permissionsToRequest(permissions);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            // if permission is not granted then we are requesting for the permissions on below line.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            // if permission is already granted then we are displaying a toast message as permission granted.
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }

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
        } else {
            //Do something
        }
////////////////////////////////////////////////////////////////////////////////////////////////////
        //Set the default rounded image profile
        Bitmap bitmap0 = BitmapFactory.decodeResource(getResources(),R.drawable.avatar);

        //resize bitmap : the width or hight do not greater than 200 px
        bitmap0 = resizeBitmap(bitmap0, 200);

        //Display a rounded image
        //bitmap0 = getRoundedCornerBitmap(bitmap0, 200);
        bitmap0 = getRoundedCornerImage(bitmap0, 200);

        //The above methods 'getRoundedCornerImage' or 'getRoundedCornerBitmap' are the same as below.
        //all are based on 'PorterDuff' algorithm.

            /*
            int w0 = bitmap0.getWidth(), h0 = bitmap0.getHeight();
            int radius0 = w0 > h0 ? h0 : w0; // set the smallest edge as radius.

            RoundedImageView  roundedImageView0 = new RoundedImageView(MainActivity.this);
            Bitmap roundBitmap0 = roundedImageView0.getCroppedBitmap(bitmap0, radius0);
            */

        //By default image profile is set to default.
        imageProfile.setImageBitmap(bitmap0);
        ////////////////////////////////////////////////////////////////////////////////////////////
        //user-interaction
            runnable = new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    //Toast.makeText(this., "user is inactive from last 5 minutes",Toast.LENGTH_LONG).show();
                    //Notify the user
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Your are inactive in connection page join for a while, need more time ?")
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

        notSeenMessages = new ArrayList<>();

        ////////////////////////////////////////////////////////////////////////////////////////////
        //login
        //Intent intent = new Intent(MainActivity.this, LoginActivity.class); //image_profile_gallery
        //intent.putExtra("data", "data"); //dummy
        //startActivityForResult(intent, REQUEST_CODE_LOGIN);
        ////////////////////////////////////////////////////////////////////////////////////////////

        /*
        String myID;
        int myversion = 0;

        myversion = Integer.valueOf(android.os.Build.VERSION.SDK);
        if (myversion < 23) {
            TelephonyManager mngr = (TelephonyManager)
                    getSystemService(Context.TELEPHONY_SERVICE);
            myID= mngr.getDeviceId();
        }else {
            myID =
                    Settings.Secure.getString(getApplicationContext().getContentResolver(),
                            Settings.Secure.ANDROID_ID);
        }
        */

        //finish building the components (buttons, ...)
        startChat();

        //get the extra sent either by : 'LoginActivity' when the user login successfully
        // or
        // by : 'FirstTimeLoginActivity' when the user register successfully
        this.NICKNAME = getIntent().getExtras().getString("NICKNAME");

        nickname.setText(NICKNAME);
        nickname.setEnabled(false);

        nextOncreate();

        //Get all not seen messages if any
        //get not seen messages and display the authors of theses messages if any


        // il un error : 'Can not perform this action after onSaveInstanceState'
        //get all not seen messages from server. Since if the user 'NICKNAME' is disconnected
        //his message are stored in the server not locally.
        //this statement is async and call to fragment

        //display  = display the not seen messages in home page under the start button
        //dispatch = next thing to do.
        getAllNotSeenMessages(NICKNAME, null, true);
        display = true;
        dispatch = "next_on_create";
        socket.emit("get_all_not_seen_messages", NICKNAME);// the result will be found in socket.on('get_all_not_seen_messages_res', result)
        return;

        //the call to 'nextOncreate()' is done at the end of 'get_all_not_seen_messages' event.
        //nextOncreate();
        //siji();
    }//end 'onCreate'

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        //inflater.inflate(R.menu.menu_main, menu);
        MenuItem menuItem =  menu.findItem(R.id.remember_me);

        //set the value found in 'preferences'
        boolean rememberMe = preferences.getBoolean("remember_me", false);
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

    public synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }    return uniqueID;
    }

    private void endActivity(String status) {
        //super.finish();
    }



    public void siji(){ }

    //manage user interaction
    @Override
    public void onUserInteraction() {
        // TODO Auto-generated method stub
        super.onUserInteraction();
        //stopHandler();//stop first and then start
        //startHandler();
    }

    //manage user interaction
    //public void stopHandler() {
        //handler.removeCallbacks(runnable);
   //}

    //manage user interaction
    //public void startHandler() {
    //    handler.postDelayed(runnable, 1 * 60 * 1000); //for 1 minutes
    // }

    //After check the network, we come here to set buttons : camera, gallery, folder, history, delete

    public void startChat_(){}

    public void startChat(){

        ////////////////////////////////////////////////////////////////////////////////////////////
        //nickname.setTypeface(null, Typeface.BOLD);
        ////////////////////////////////////////////////////////////////////////////////////////////

         //event on 'parameter' button
        ivParam.setEnabled(true);
        //ivParam.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));
        ivParam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //parameter clicked, show the parameter setting.
                //for testing redirect to 'ChangePasswordActivity' in 'authentication' module.

                //check ban info in preferences. The user may be banished when attemting to change his pwd
                long startBanTime = preferences.getLong("startBanTime", 0L);
                if (startBanTime != 0) {

                    long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
                    if (remainingTime >= 0L) {
                        //the ban is not ended, notify the user and exit.
                        banishDialog(MainActivity.this, startBanTime);
                        return;
                    } else {
                        //the ban is ended. Remove info from the Preferences and redirect to change pwd again.
                        editor.remove("startBanTime");
                        editor.commit();
                    }
                }
                //here the ban is ended or not set. redirect
                //Intent intent = new Intent(MainActivity.this, com.google.amara.authenticationretrofit.ChangePasswordActivity.class);
                //intent.putExtra("NICKNAME", NICKNAME);
                //startActivity(intent);
                //return;
            }
        });

        //Edit text event
        nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        //Startup
        camera.setEnabled(false);
        //camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //camera clicked, disable it.
                cameraIntent();
            }
        });

        //Startup
        gallery.setEnabled(false);
        //gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //gallery clicked

                //Select and pick one image from device gallery.
                //The image is saved in 'profile gallery' in 'onActivityResult'

                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                //        MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //the images are found in '//data/user/0/[package]/files'

                //give an exception : 'com.android.providers.downloads.DownloadStorageProvider
                //                     uri content://com.android.providers.downloads.documents/document/23 from pid=7615,
                //                     uid=10100 requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs'
                //workaroud : 'intent.setAction(Intent.ACTION_OPEN_DOCUMENT);

                Intent intent = new Intent();
                 intent.setAction(Intent.ACTION_OPEN_DOCUMENT);

                 //Intent intent = new Intent(Intent.ACTION_PICK);

                intent.setType("image/*");

                try {
                    startActivityForResult(intent, REQUEST_CODE_GALLERY);
                    //The 'chooser' is not shown
                    //startActivityForResult(Intent.createChooser(intent, "Choose Picture"), REQUEST_CODE_GALLERY);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        //Startup
        folder.setEnabled(false);
        //folder.setImageDrawable(getResources().getDrawable(R.drawable.folder_50_gray));
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //folder clicked
            }
        });

        //Startup
        history.setEnabled(false);
        //history.setImageDrawable(getResources().getDrawable(R.drawable.history_64));
        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // To disable buttons : 'Gallery', 'Camera' and enable 'Delete' are done at the end of
                //'History' part in 'onActivityResult'

                //Get the list of image profile uri from local db.
                //The image profile are stored in 'Mediastore' when the button 'enter_the_chat' is clicked.
                // The uris are stored in 'saveProfileImageUri' when the button 'enter_the_chat' is clicked.

                //Get the string entered in edittext and call it 'username'
                String username = nickname.getText().toString();

                if ((null == NICKNAME) && (username.isEmpty())) return;

                ArrayList<String> imageProfileHistoryUris = new ArrayList<>();
                if ((null == NICKNAME) && (!username.isEmpty())) {
                    //Get the image profile history from the server
                    //imageProfileHistoryUris = getImageProfileUris(username);
                    //getImageProfileUris(username); //since the user is new, he has not image profile history

                    return; //the next steps are done in 'getImageProfileUris'.
                }
                if ((null != NICKNAME) && (!username.isEmpty())) { //if 'NICKNAME' is not null, the edt field is filled with 'NICKNAME'
                    //Get the image profile history from the local db
                    imageProfileHistoryUris = getImagesProfile(username);

                }
                if ((null != NICKNAME) && (username.isEmpty())) { //the user has deleted the edt field witch is filled with 'NICKNAME'
                    //Get the image profile history from the local db
                    imageProfileHistoryUris = getImagesProfile(username);
                }

                if (imageProfileHistoryUris.isEmpty()) {
                    //Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "No images profile found in histotry", Snackbar.LENGTH_LONG).show();

                    //ask the server
                    getImagesProfileFromServer(username);

                    return;
                }

                //Here, the 'imageProfileHistoryUris' is not empty add it as extra. Build intent
                //and send it to 'ImageProfileGalleryMainActivity' in 'image_profile_gallery' module
                Intent intent = new Intent(MainActivity.this, ImageProfileGalleryMainActivity.class); //image_profile_gallery
                intent.putExtra("imageProfileHistoryUri", imageProfileHistoryUris);

                //do nothing
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                //test
                    //exception : 'com.android.providers.downloads.DownloadStorageProvider
                    // uri content://com.android.providers.downloads.documents/document/23 from pid=7615,
                    // uid=10100 requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
                    //Uri uri = Uri.parse(imageProfileHistoryUris.get(0));
                    //Bitmap bitmap = getBitmap(getContentResolver(), uri);
                //try {
                //    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                //} catch (IOException e) {
                //    e.printStackTrace();
                //}


                //intent.putExtra("current_user", (Parcelable)currentChatUser);

                //intent.putExtra("fragment_index", 0); //pass zero for Fragment one.
                //intent.putExtra("NIckname", NICKNAME);
                //intent.putExtra("image_profile", imageProfile_);
                //intent.putExtra("first_time", "");
                //intent.putExtra("image_profile_changed", "");
                //intent.putExtra("connection_time", connectedAt);

                startActivityForResult(intent, REQUEST_CODE_IMAGE_PROFILE_HISTORY);

            }
        });

        //Startup
        delete.setEnabled(false);
        //delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64_gray));
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delete clicked
                delete.setEnabled(false);
                delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64_gray));

                camera.setEnabled(true);
                camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));

                gallery.setEnabled(true);
                gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64));


                history.setEnabled(true);
                history.setImageDrawable(getResources().getDrawable(R.drawable.history_64));

                //Show the default profile image
                //imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.avatar));

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                //resize bitmap : the width or hight do not greater than 200 px
                bitmap = resizeBitmap(bitmap, 200);

                //get base64 string
                //imageProfile_   = getBase64String(bitmap); //used in 'ChatUser'

                //Display a rounded image
                int w = bitmap.getWidth(), h = bitmap.getHeight();
                int radius = w > h ? h : w; // set the smallest edge as radius.

                RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                imageProfile.setImageBitmap(roundBitmap);

                imageProfile_ = null; //base-64 string used in 'ChatUser'
            }
        });

        //test, remove in production mode
        //socket.emit("all_notes", NICKNAME);
        //socket.emit("note_by_id", 10);

        /*
        //test : to remove in production
        socket.emit("note_by_id", 10, new Ack() {
            @Override
            public void call(Object... args) {
                JSONObject jSONObject = ((JSONObject) args[0]);
                try {
                    String resAck = jSONObject.getString("res");
                    Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "ack = " + resAck, Snackbar.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        */


        //ConnectToHeroku();
        //ConnectSSL();

        //socket.on(Socket.EVENT_CONNECT, mConnectionListener);
        //socket.on(Socket.EVENT_DISCONNECT, mDisconnectionListener);
        ////socket.on(Socket.EVENT_CONNECT_ERROR,mErrorListener);
        ////socket.on(Socket.EVENT_MESSAGE,mMessageListener);
        //socket.on(Socket.EVENT_CONNECT_ERROR, mConnectionErrorListener);

        //Received a logoff notification from the server.
        /*
        socket.on("logoff", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject notification = (JSONObject) args[0];
                        try {
                            String raison   = notification.getString("reason");   //{reason: xxx, socketId :socket.id}
                            String socketId = notification.getString("socketId");   //{reason: xxx, socketId :socket.id}

                            //open alertDialog
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("Inactivity : " + socketId).
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //to do
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        */

        //that statement is done in 'socket.emit("get_all_not_seen_messages'.
        //nextOncreate();
    }

    //it is the first time, get images profile from the server.
    private void getImagesProfileFromServer(String username) {
        boolean found = false;
        socket.emit("get_image_profile_uri", username); //the result will be found in ''setEmitterListener_get_image_profile_uri_back()
    }

    //it is the first time, the server has sent the images profile.
    // Save the image profile uris in db locally
    private void imagesProfileFromServerResult(JSONArray data) {

        if(data.length() != 0){
            //get arraylist of uri from json
            ArrayList<String> imageProfileUris = new ArrayList<>();
            for (int i = 0; i <= data.length() - 1; i++) {
                try {
                    JSONObject dat = (JSONObject) data.getJSONObject(i);
                    imageProfileUris.add(i, dat.getString("uri"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Intent intent = new Intent(MainActivity.this, ImageProfileGalleryMainActivity.class); //ImageProfileGalleryMainActivity.class);
            intent.putExtra("imageProfileHistoryUri", imageProfileUris);
            startActivityForResult(intent, REQUEST_CODE_IMAGE_PROFILE_HISTORY);

            //Save the image profile uris in db locally.
            int rows = saveImageProfileUris(data);
            if (rows != data.length())
                throw new UnsupportedOperationException("Download image profile uris from server : unexpected value");

            //Set the flag that the image profile uris are downloaded and saved successfully
            imageProfileHistoryUrisSavedLocally = true;
        } else {
            Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "No images profile found", Snackbar.LENGTH_LONG).show();

            //Disable 'Delete' and 'History' button, no image profile to display.

            history.setEnabled(false);
            history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));

            delete.setEnabled(false);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64_gray));

            //Enable 'Gallery' and 'Camera'
            camera.setEnabled(true);
            camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));

            gallery.setEnabled(true);
            gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64));

            //Display default image
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

            //resize bitmap : the width or hight do not be greater than 200 px
            bitmap = resizeBitmap(bitmap, 200);

            //Display a rounded image
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            int radius = w > h ? h : w; // set the smallest edge as radius.

            RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
            Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

            //get base64 string
            imageProfile_ = getBase64String(roundBitmap); //used in 'ChatUser'

            //Set ImageView
            imageProfile.setImageBitmap(roundBitmap);
        }
            /////////////////////////////////
            /*
            if (found) {
                ArrayList<String> imagesProfile = new ArrayList<>();

                //save the image found in local db
                saveImagesProfileLocally(imagesProfile, username);
            } else {
                //no image profile found in local db nor remote
                //Disable 'Delete' button, no image profile to display.
                delete.setEnabled(false);
                delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64_gray));

                //Enable 'Gallery' and 'Camera'
                camera.setEnabled(true);
                camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));

                gallery.setEnabled(true);
                gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64));

                //Display default image
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                //resize bitmap : the width or hight do not greater than 200 px
                bitmap = resizeBitmap(bitmap, 200);

                //Display a rounded image
                int w = bitmap.getWidth(), h = bitmap.getHeight();
                int radius = w > h ? h : w; // set the smallest edge as radius.

                RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                //get base64 string
                imageProfile_ = getBase64String(roundBitmap); //used in 'ChatUser'

                //Set ImageView with final rounded default image.
                imageProfile.setImageBitmap(roundBitmap);
                return;
            }
            */
    }


    private Bitmap getBitmap(final ContentResolver contentResolver, final Uri photoUri){
            Bitmap bitmap = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                final ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, photoUri);
                try {
                    bitmap = ImageDecoder.decodeBitmap(source);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                InputStream inputStream = null;
                try {
                    inputStream = contentResolver.openInputStream(photoUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return bitmap;
    }

    /**
     * Called when a ban occur.
     * @param context the context.
     * @param startBanTime the time when the ban starts. It is set after 3 unsuccessful attempts to login.
     * It is set in prefrences
     */
    private void banishDialog(Context context, long startBanTime) {
        //Notify the user how many time it remains to have free access.
        long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
        long minutes  = remainingTime /  60000;
        long secondes = (remainingTime % 60000);
        int secondes_ = (int)(secondes / 1000);

        new AlertDialog.Builder(context).
                setMessage("It remains : " + minutes + " minutes et " + secondes_ + " secondes to log").
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create().show();
    }

    //get ban info from the server
    private void setEmitterListener_get_user_ban_back() {
        socket.on("get_ban_info_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray ban_ = ((JSONArray) args[0]);
                        if(ban_ == null){
                            //do login
                            //Intent intent = new Intent(MainActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            //return;
                        }

                        String startBanTime = null;
                        String packageId    = null;
                        try {
                            JSONObject ban = (JSONObject)ban_.get(0);
                            packageId    = ban.isNull("packageid") ? null : ban.getString("packageid");
                            startBanTime = ban.isNull("startbantime") ? null : ban.getString("startbantime");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(packageId == null){
                            //do login
                            //Intent intent = new Intent(MainActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            //return;
                        }

                        //here there is packageId, show the user o dialog and exit.
                        long remainingTime = BAN_TIME - (new Date().getTime() - Long.parseLong(startBanTime));
                        if(remainingTime >= 0L){
                            //the ban is not ended, notify the user and exit.
                            banishDialog(MainActivity.this, Long.parseLong(startBanTime) );
                            return;
                        }else{
                            //the ban is ended. Remove info from the Preferences and login again
                            editor.remove("startBanTime");
                            editor.commit();

                            //login
                            //Intent intent = new Intent(MainActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            //return;
                        }
                    }
                });
            }
        });
    }

    //show the user a sign in form to login
    private void doLogin() {

        //show the user a sign in form  to get his account after he deleted it or
        //a register form if it is the first time
        // the call to 'nextOncreate()'is done in 'FirstTimeLoginActivity.setCallback';

        //The 'onActivityResult' is used to end activity.
        // We receive the result of this intent in interface between 'app' module and 'authentication'.
        //Intent intent = new Intent(MainActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
        //intent.putExtra("authentication", "authentication"); //not used, for illustration
        //startActivityForResult(intent, REQUEST_CODE_AUTH);
    }

    private void notUsed1() {
        nickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //disable 'enter chat' button if the field is empty or it contains space.

                //Animator error, workaround
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn.setEnabled(false);

                        nickname.setTextColor(Color.RED);

                        //looking for space
                        Pattern space = Pattern.compile("\\s+");
                        Matcher matcherSpace = space.matcher(s.toString());
                        boolean containsSpace = matcherSpace.find();

                        if (containsSpace == true) nickname.setError("Spaces are not allowed");


                        if ((s.toString().length() != 0) && (containsSpace == false)) {
                            btn.setEnabled(true);
                            int currentColor = nickname.getCurrentTextColor();
                            if (currentColor == Color.RED) nickname.setTextColor(Color.BLACK);
                        }


                    }
                });
                //check if the name entered is already registered. if it is registered, colour it in red.
                //The check is done on 'enter' click button in 'checkIsUserRegistered' method
                //int red   = Color.RED;
                //int black = Color.BLACK;
                //nickname.setTextColor(black);

                //not used here. The check is done on 'enter' click button
                //checkIsUserRegistered(s);

                /*
                FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                AllUsersFragment f  = AllUsersFragment.newInstance();
                ft.replace(R.id.ll_not_seen_messages, f);
                ft.commit();
                */
            }

            //@Override//for test fragment usage
            public void onTextChanged_(CharSequence s, int start, int before, int count) {
                //disable 'enter chat' button if the field is empty.
                btn.setEnabled(false);
                if (s.toString().length() != 0)btn.setEnabled(true);

                FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                AllUsersFragment f  = AllUsersFragment.newInstance();
                ft.replace(R.id.ll_not_seen_messages, f);
                ft.commit();
            }

            //@Override
            public void onTextChanged__(CharSequence s, int start, int before, int count) {
                //is there a user with this name ? If it is equal to Nickname, then its image profile
                // is 'oldImageProfile'
                //else it is another user which exists in db otherwise, it is a new user
                if(s.toString().equals(NICKNAME)){
                    //reset the not seen messages authors list to its original
                    FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                    NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(jsonArray2); //jsonArray2 contains not Seen Messages Authors
                    ft.replace(R.id.ll_not_seen_messages, f); //it is a framelayout, see 'activity_main_.xml'
                    ft.commit();

                    //reset image profile to old
                    if(oldImageProfile != null){
                        byte[] decodedString = Base64.decode(oldImageProfile, Base64.DEFAULT);
                        Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        //Resize the bitmap 'decodedByte'
                        decodedByte = resizeBitmap(decodedByte, 200);

                        //crop (round) the image and display it.
                        int w0 = decodedByte.getWidth(), h0 = decodedByte.getHeight();
                        int radius0 = w0 > h0 ? h0 : w0; // set the smallest edge as radius.

                        RoundedImageView  roundedImageView_ = new RoundedImageView(MainActivity.this);
                        Bitmap roundBitmap0 = roundedImageView_.getCroppedBitmap(decodedByte, radius0);

                        //get base64 string
                        imageProfile_ = getBase64String(roundBitmap0);

                        //Set ImageView to original
                        imageProfile.setImageBitmap(roundBitmap0);
                    }
                    return;
                }
                //Here the username is not equal to 'Nickname'. it is another user or a new user.
                // get chat user from local db
                ChatUser chatUser = getChatUser(s.toString());

                //Display chat user in fragment
                if(chatUser == null){
                    //the user is new, show the default image profile
                    FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                    NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(null);
                    ft.replace(R.id.ll_not_seen_messages, f);
                    ft.commit();

                    //reset the image view to default and display default 'avatar' image
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                    //resize bitmap : the width or hight do not greater than 200 px
                    bitmap = resizeBitmap(bitmap, 200);

                    //Display a rounded image
                    int w = bitmap.getWidth(), h = bitmap.getHeight();
                    int radius = w > h ? h : w; // set the smallest edge as radius.

                    RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                    Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                    //get base64 string
                    imageProfile_ = null; //getBase64String(roundBitmap);

                    //Set ImageView to avatar
                    imageProfile.setImageBitmap(roundBitmap);
                    return;
                }
                //here the new user exits. Display his image profile and get its 'allNotSeenMessages' from server.
                getAllNotSeenMessages(s.toString(), null, true);

                //Display the image profile of the existing chat user
                String image = chatUser.imageProfile; //base64 string
                if(image == null){
                    //show avatar
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                    //resize bitmap : the width or hight do not greater than 200 px
                    bitmap = resizeBitmap(bitmap, 200);

                    //Display a rounded image
                    int w = bitmap.getWidth(), h = bitmap.getHeight();
                    int radius = w > h ? h : w; // set the smallest edge as radius.

                    RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                    Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                    //get base64 string
                    imageProfile_ = null; //getBase64String(roundBitmap);

                    //Set ImageView to avatar
                    imageProfile.setImageBitmap(roundBitmap);
                    return;
                }
                //the image of the existing chat user is not null, show it.
                byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
                Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                //Resize the bitmap 'decodedByte'
                decodedByte = resizeBitmap(decodedByte, 200);

                //crop (round) the image and display it.
                int w0 = decodedByte.getWidth(), h0 = decodedByte.getHeight();
                int radius0 = w0 > h0 ? h0 : w0; // set the smallest edge as radius.

                RoundedImageView  roundedImageView_ = new RoundedImageView(MainActivity.this);
                Bitmap roundBitmap0 = roundedImageView_.getCroppedBitmap(decodedByte, radius0);

                //get base64 string
                imageProfile_ = getBase64String(roundBitmap0);

                //Set ImageView to original
                imageProfile.setImageBitmap(roundBitmap0);

                ////////////////////////////////////////////////////////////////////////////////////
                /*
                //Enable 'History' only if 'imageProfile' is null
                if (imageProfile_ == null) {
                    history.setEnabled(true);
                    history.setImageDrawable(getResources().getDrawable(R.drawable.history_64));
                    if (s.length() == 0) {
                        //disable 'History'
                        history.setEnabled(false);
                        history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));
                    }
                }

                //reset image profile to default if edt text is changed
                if (imageProfile_ != null) {
                    //disable the not seen messages author list
                    FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                    NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(null);
                    ft.replace(R.id.ll_not_seen_messages, f);
                    ft.commit();

                    //reset the image view to default and display default 'avatar' image
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                    //resize bitmap : the width or hight do not greater than 200 px
                    bitmap = resizeBitmap(bitmap, 200);

                    //Display a rounded image
                    int w = bitmap.getWidth(), h = bitmap.getHeight();
                    int radius = w > h ? h : w; // set the smallest edge as radius.

                    RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                    Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                    //get base64 string
                    imageProfile_ = null; //getBase64String(roundBitmap);

                    //Set ImageView to avatar
                    imageProfile.setImageBitmap(roundBitmap);
                }
                if (imageProfile_ == null) {
                    if(s.toString().equals(NICKNAME)){

                        if(oldImageProfile == null){
                            //reset image profile to default avatar
                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                            //resize bitmap : the width or hight do not greater than 200 px
                            bitmap = resizeBitmap(bitmap, 200);

                            //Display a rounded image
                            int w = bitmap.getWidth(), h = bitmap.getHeight();
                            int radius = w > h ? h : w; // set the smallest edge as radius.

                            RoundedImageView roundedImageView = new RoundedImageView(MainActivity.this);
                            Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

                            //get base64 string
                            imageProfile_ = getBase64String(roundBitmap);

                            //Set ImageView to avatar
                            imageProfile.setImageBitmap(roundBitmap);
                        }else{
                            //reset the not seen messages authors list
                            FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                            NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(jsonArray2);
                            ft.replace(R.id.ll_not_seen_messages, f);
                            ft.commit();

                            //reset image profile to old
                            byte[] decodedString = Base64.decode(oldImageProfile, Base64.DEFAULT);
                            Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            //Resize the bitmap 'decodedByte'
                            decodedByte = resizeBitmap(decodedByte, 200);

                            //crop (round) the image and display it.
                            int w0 = decodedByte.getWidth(), h0 = decodedByte.getHeight();
                            int radius0 = w0 > h0 ? h0 : w0; // set the smallest edge as radius.

                            RoundedImageView  roundedImageView_ = new RoundedImageView(MainActivity.this);
                            Bitmap roundBitmap0 = roundedImageView_.getCroppedBitmap(decodedByte, radius0);

                            //get base64 string
                            imageProfile_ = getBase64String(roundBitmap0);

                            //Set ImageView to original
                            imageProfile.setImageBitmap(roundBitmap0);
                        }
                    }
                }
                */
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void notUsed() {

        //not used. same as above. See 'setEmitterListener_get_all_not_seen_messages_res'
        socket.on("get_all_not_seen_messages_res_", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jsonObject = (JSONObject) args[0];
                        ArrayList<Message> messages = new ArrayList<>();
                        JSONArray jsonArray1 = null;
                        //JSONArray jsonArray2 = null;
                        JSONArray jsonArray3 = null;
                        try {

                            jsonArray1 = jsonObject.isNull("notSeenMessages") ?
                                    null : jsonObject.getJSONArray("notSeenMessages");
                            jsonArray2 = jsonObject.isNull("notSeenMessagesAuthor") ?
                                    null : jsonObject.getJSONArray("notSeenMessagesAuthor");
                            jsonArray3 = jsonObject.isNull("lastContacts") ?
                                    null : jsonObject.getJSONArray("lastContacts");

                            if(jsonArray1 != null){
                                //build the 'Message' objects

                                for(int i = 0; i <= jsonArray1.length() - 1; i++){
                                    Message message = new Gson().fromJson(jsonArray1.getJSONObject(i).toString(), Message.class);
                                /*
                                Message message = new Message(
                                        jsonArray1.getJSONObject(i).getString("fromnickname"),
                                        jsonArray1.getJSONObject(i).getString("tonickname"),
                                        jsonArray1.getJSONObject(i).getString("message"),
                                        jsonArray1.getJSONObject(i).getLong("time"),
                                        jsonArray1.getJSONObject(i).isNull("extra") ? null : jsonArray1.getJSONObject(i).getString("extra"),
                                        jsonArray1.getJSONObject(i).isNull("extraname") ? null : jsonArray1.getJSONObject(i).getString("extraname"),
                                        jsonArray1.getJSONObject(i).getString("ref"),
                                        jsonArray1.getJSONObject(i).isNull("mimetype") ? null : jsonArray1.getJSONObject(i).getString("mimetype"),
                                        jsonArray1.getJSONObject(i).getString("seen"),
                                        jsonArray1.getJSONObject(i).getString("deletedfrom"),
                                        jsonArray1.getJSONObject(i).getString("deletedto")
                                );
                                */
                                    messages.add(message);
                                }
                                //save not seen messages in local db
                                int rows = saveNotSeenMessages(messages);
                                //not seen messages
                                //allNotSeenMessages = jsonArray2;

                                //last contacts witch are authors of not seen messages
                                //convert JSONArray to map and extract the authors of the not seen messages
                                Map<String, Long> map = new HashMap<>();
                                if (jsonArray3 != null) {
                                    for(int i = 0; i <= jsonArray3.length() - 1; i++){
                                        try {
                                            JSONObject jsonObject_ = (JSONObject)jsonArray3.get(i);
                                            String key = jsonObject_.getString("fromnickname");
                                            long value  = jsonObject_.getLong("time");
                                            map.put(key, value);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //update the 'lastUser' table in local bd
                                    saveLastSessionUsers(NICKNAME, map);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //build the 'notSeenMessage' objects : author, number of the not seen messages
                        notSeenMessages = new ArrayList<NotSeenMessage>();
                        if(jsonArray2 != null){
                            for(int i = 0; i <= jsonArray2.length() - 1; i++){
                                try {
                                    //JSONObject jsonObject = (JSONObject)jsonArray2.get(i);
                                    NotSeenMessage notSeenMessage = new Gson().fromJson(jsonArray2.getJSONObject(i).toString(), NotSeenMessage.class);

                                   /*
                                   NotSeenMessage notSeenMessage = new NotSeenMessage(
                                           jsonObject.getString("nickname"),
                                           jsonObject.getString("nb"),
                                           jsonObject.isNull("imageprofile") ? null : jsonObject.getString("imageprofile")
                                   );
                                   */
                                    notSeenMessages.add(notSeenMessage);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        //next
                        Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "allNotSeenMessages", Snackbar.LENGTH_LONG).show();

                        //display "not seen messages" in a fragment
                        if(display){
                            FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
                            NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(jsonArray2);
                            ft.replace(R.id.ll_not_seen_messages, f);
                            //Exception : "Can not perform this action after onSaveInstanceState"
                            ft.commit();
                            //ft.commitAllowingStateLoss();
                        }

                        //dispatch
                        if(dispatch == null)return;
                        if(dispatch.equals("next_on_create")){
                            nextOncreate();
                            return;
                        }
                        if(dispatch.equals("init_socket1"))initSocket1();
                    }
                });
            }
        });
    }

    private void checkIsUserRegistered(String s) {
        //if the name entered in 'nickname' edit text already exists ?
        //As the user is new or he deleted the app, the local db is also deleted.
        //then ask the server if the user exists or not.

        socket.emit("is_user_registered_in_db", s, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject userExists = ((JSONObject) args[0]);
                boolean userExist     = false;
                try {
                    userExist = userExists.getBoolean("exists");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //userExist     = false; //for test only to remove in production

                if(!userExist){
                    //the name is new and it is not registered
                    //Enable 'EnterChat' button and save the name in preferences
                    //workaround 'Animators may only be run on Looper threads'
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btn.setEnabled(true);
                            NICKNAME = nickname.getText().toString().trim();
                            /*
                            //test
                            if(!NICKNAME.equals(NICKNAME_TEMP)){ //if the NICKNAME in preferences is modified
                                //in test mode, take 'NICKNAME' from edt and in production take it from preferences
                                NICKNAME = nickname.getText().toString(); //for test only to remove in production mode

                                //save the NICKNAME in preferences if it is modified.
                                editor.putString("Nickname", NICKNAME);
                                editor.commit();
                            }
                            */
                            //save the NICKNAME in preferences if it is modified.
                            editor.putString("Nickname", NICKNAME);
                            editor.commit();

                            initSocket1();
                        }
                    });

                            /*
                            //set the final value of NICKNAME found in edt field.
                            //'temp' is the value found in preferences. save it temporarily in 'temp' variable.
                            String temp = NICKNAME; //may be null after reset or equals the value found in preferences
                            NICKNAME = nickname.getText().toString();

                            //save the NICKNAME in prefrences
                            editor.putString("Nickname", NICKNAME);
                            editor.commit();
                            */

                    //Case first time or app deleted. The 'Preferences' is empty. Try to get data from
                    // server and build the 'currentChatUser'.
                    //This will be done in 'TabChatActivity'
                    //if((null == NICKNAME) && (!nickname.getText().toString().isEmpty())){
                    //    initSocket(nickname.getText().toString());
                    //}

                    //initSocket1();
                }else{
                    //the name already exists in server db, notify the user and open a dialog
                    //Disable  'EnterChat' button and color in red the input
                    //Only the original thread that created a view hierarchy can touch its views.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btn.setEnabled(false);
                            int red   = Color.RED;
                            int black = Color.BLACK;
                            nickname.setTextColor(red);
                        }
                    });


                            /*
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle("Connection error");
                                    builder.setMessage("The username'" + nickname.getText().toString() + "' already exists.");

                                    // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            socket.close();
                                            finish();
                                            return;
                                        }
                                    }).create().show();
                                }
                            });
                            */
                }
            }
        });
    }

    //Not used
    private void setEmitterListener_is_user_present_in_db_back() {
        socket.on("is_user_present_in_db_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);
                        boolean exists = false;
                        try {
                            exists = user.getBoolean("exists");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(exists){
                            //notify the user that it is already in db
                        }else{
                            //save the name in prefernces
                            //set the final value of NICKNAME found in edt field.
                            //'temp' is the value found in preferences. save it temporarily in 'temp' variable.
                            String temp = NICKNAME; //may be null after reset or equals the value found in preferences
                            NICKNAME = nickname.getText().toString();

                            //save the NICKNAME in prefrences
                            editor.putString("Nickname", NICKNAME);
                            editor.commit();

                            //Case first time or app deleted. The 'Preferences' is empty. Try to get data from
                            // server and build the 'currentChatUser'.
                            //This will be done in 'TabChatActivity'
                            //if((null == NICKNAME) && (!nickname.getText().toString().isEmpty())){
                            //    initSocket(nickname.getText().toString());
                            //}
                            initSocket1();
                        }

                    }
                });
            }
        });
    }

    private void setEmitterListener_get_user_back() {
        socket.on("get_user_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);
                        if(user != null) {
                            //Save the current user in local db
                            //int nbRows = saveUser(user);

                            //build the 'currentChatUser' object
                            try {
                                String nickname         = user.getString("nickname");

                                String imageProfile     = (user.isNull("imageprofile")) ? null :
                                        user.getString("imageprofile");

                                int status              = user.getInt   ("status");
                                int notSeenMessages     = user.getInt   ("notseenmessages");
                                long connectedAT        = user.getLong  ("connected");
                                long disconnectedAt     = user.getLong  ("disconnected");
                                long lastConnectedAt    = user.getLong  ("lastconnected");
                                String blacklistAuthor  = user.getString("blacklistauthor");

                                //update the fields : 'connectedAT', 'disconnectedAt', 'lastConnectedAt'
                                currentChatUser = new ChatUser(
                                        nickname,
                                        null,
                                        imageProfile,
                                        status,
                                        notSeenMessages,
                                        connectedAT,        //connectedAT becomes now,
                                        disconnectedAt,     //disconnectedAt becomes ' lastConnectedAt',
                                        lastConnectedAt,    //lastConnectedAt becomes 'connectedAT',
                                        blacklistAuthor
                                );
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //save this 'currentChatUser' in local db
                            //int row = saveChatUser(currentChatUser);
                            //if(row == 0) throw new UnsupportedOperationException("'Save the user in local db'");

                            //update the image profile string
                            //imageProfile_ = currentChatUser.imageProfile; //base 64 string

                        }else{
                            //The user is not found, it is a new user who just registered
                            currentChatUser = new ChatUser(
                                    NICKNAME,
                                    null,
                                    null,
                                    1,
                                    0,
                                    new Date().getTime(),
                                    0,
                                    0,
                                    null
                            );
                        }
                        //update the image profile string
                        imageProfile_ = currentChatUser.imageProfile; //base 64 string

                        //initSocket1();
                        updateProfile();
                    }
                });
            }
        });
    }

    //It is the first time, The server has sent back the uris of images profile.
    //Save the image profile uris in db locally
    private void setEmitterListener_get_image_profile_uri_back() {
        socket.on("get_image_profile_uri_back", new Emitter.Listener() {
            //array one element to convert to final
            final ArrayList<String>[] imageProfileUris = new ArrayList[]{new ArrayList<>()};
            final JSONArray[] data = {null};
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data = (JSONArray) args[0];
                        //for (int i = 0; i <= data.length() - 1; i++) {
                        //    try {
                        //        JSONObject dat = (JSONObject) data.getJSONObject(i);
                        //        imageProfileUris[0].add(dat.getString("uri"));//array one element to convert to final
                        //    } catch (JSONException e) {
                        //        e.printStackTrace();
                        //    }
                        //}

                        imagesProfileFromServerResult(data);

                    }
                });
            }
        });
    }


    private void setEmitterListener_save_get_image_profile_uri_back() {
        socket.on("get_image_profile_uri_back", new Emitter.Listener() {
            //array one element to convert to final
            final ArrayList<String>[] imageProfileUris = new ArrayList[]{new ArrayList<>()};
            final JSONArray[] data = {null};
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray data = (JSONArray) args[0];
                        for (int i = 0; i <= data.length() - 1; i++) {
                            try {
                                JSONObject dat = (JSONObject) data.getJSONObject(i);
                                imageProfileUris[0].add(dat.getString("uri"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if(data != null) {
                            //Save the result in db locally
                            int rows = saveImageProfileUris(data);
                            if (rows != data.length())
                                throw new UnsupportedOperationException("Download image profile uris from server : unexpected value");
                        }
                    }
                });
            }
        });
    }

    private void setEmitterListener_note_by_id_res() {
        socket.on("note_by_id_res", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject jSONObject = ((JSONObject) args[0]);
                        try {
                            String resAck = jSONObject.getString("res");
                            Snackbar.make(MainActivity.this.findViewById(android.R.id.content), resAck, Snackbar.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void setEmitterListener_get_all_not_seen_messages_res() {
        socket.on("get_all_not_seen_messages_res", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getAllNotSeenMessagesResult((JSONObject) args[0]);
                    }
                });
            }
        });
    }


    //analyse the response from the server.
    private void getAllNotSeenMessagesResult(JSONObject jsonObject) {
        //JSONObject jsonObject = (JSONObject) args[0];
        ArrayList<Message> messages       = new ArrayList<>();  //all not seen messages
        ArrayList<Message> messages_      = new ArrayList<>(); //all not seen messages after disconnect
        ArrayList<ChatUser> lastChatUsers = new ArrayList<>();

        JSONArray jsonArray0 = null;
        JSONArray jsonArray1 = null;
        //JSONArray jsonArray2 = null;
        JSONArray jsonArray20 = null;
        JSONArray jsonArray21 = null;
        JSONArray jsonArray3 = null;
        JSONArray jsonArray4 = null;
        JSONArray jsonArray5 = null;
        JSONArray jsonArray6 = null;
        JSONArray jsonArray7 = null;
        try {

             jsonArray0 = jsonObject.isNull("notSeenMessages") ?         //all not seen messages before and after disconnect
                    null : jsonObject.getJSONArray("notSeenMessages");
            jsonArray1 = jsonObject.isNull("notSeenMessagesAfterDisconnect") ?  //not seen messages after disconnect
                    null : jsonObject.getJSONArray("notSeenMessagesAfterDisconnect");
            jsonArray2 = jsonObject.isNull("notSeenMessagesAuthors") ?   //Short Chatuser object of user (nickname, image profile, nbMessages)  who sent not seen messages
                    null : jsonObject.getJSONArray("notSeenMessagesAuthors");
            jsonArray21 = jsonObject.isNull("allContactUsers") ?   //all users who contact nickname
                    null : jsonObject.getJSONArray("allContactUsers");
            jsonArray20 = jsonObject.isNull("notSeenMessagesAuthorsAfterDisconnection") ?   //full Chatuser object of user who sent not seen messages after disconnect
                    null : jsonObject.getJSONArray("notSeenMessagesAuthorsAfterDisconnection");


            /*
            jsonArray3 = jsonObject.isNull("lastContacts") ?
                    null : jsonObject.getJSONArray("lastContacts"); //String name and connection time of all users who send messages
            jsonArray4 = jsonObject.isNull("lastUsers") ?           //ChatUser object of all users who send messages
                    null : jsonObject.getJSONArray("lastUsers");
            jsonArray5 = jsonObject.isNull("lastUsers_") ?           //all ChatUser object who send message
                    null : jsonObject.getJSONArray("lastUsers_");
            jsonArray6 = jsonObject.isNull("users_after_disconnect") ?  //ChatUser object who send message after deconnection
                    null : jsonObject.getJSONArray("users_after_disconnect");
            jsonArray7 = jsonObject.isNull("not_registered_users") ?  //array of names of the users who send message after deconnection and they are not registered
                    null : jsonObject.getJSONArray("not_registered_users");
            */

            //All not seen messages before and after disconnection. Not used
            if(jsonArray0 != null){
                //build the 'Message' objects

                for(int i = 0; i <= jsonArray0.length() - 1; i++){
                    Message message = new Gson().fromJson(jsonArray0.getJSONObject(i).toString(), Message.class);
                                /*
                                Message message = new Message(
                                        jsonArray1.getJSONObject(i).getString("fromnickname"),
                                        jsonArray1.getJSONObject(i).getString("tonickname"),
                                        jsonArray1.getJSONObject(i).getString("message"),
                                        jsonArray1.getJSONObject(i).getLong("time"),
                                        jsonArray1.getJSONObject(i).isNull("extra") ? null : jsonArray1.getJSONObject(i).getString("extra"),
                                        jsonArray1.getJSONObject(i).isNull("extraname") ? null : jsonArray1.getJSONObject(i).getString("extraname"),
                                        jsonArray1.getJSONObject(i).getString("ref"),
                                        jsonArray1.getJSONObject(i).isNull("mimetype") ? null : jsonArray1.getJSONObject(i).getString("mimetype"),
                                        jsonArray1.getJSONObject(i).getString("seen"),
                                        jsonArray1.getJSONObject(i).getString("deletedfrom"),
                                        jsonArray1.getJSONObject(i).getString("deletedto")
                                );
                                */
                    messages.add(message);
                }

                //save not seen messages after disconnect in local db in table 'message'
                //int rows = saveNotSeenMessages(messages);

                //not seen messages
                //allNotSeenMessages = jsonArray2;

                //last contacts witch are authors of not seen messages
                //convert JSONArray to map and extract the authors of the not seen messages
                /*
                Map<String, Long> map = new HashMap<>();
                if (jsonArray3 != null) {
                    for(int i = 0; i <= jsonArray3.length() - 1; i++){
                        try {
                            JSONObject jsonObject_ = (JSONObject)jsonArray3.get(i);
                            String key = jsonObject_.getString("fromnickname");
                            long value  = jsonObject_.getLong("time");
                            map.put(key, value);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    //update the 'lastUser' table in local bd
                    saveLastSessionUsers(NICKNAME, map);
                }
                */
                //build the 'ChatUser' object of last session users list from the 'chat_users' local table
                //if(map != null)lastSessionUsers = getChatUsers(map.keySet());
                //List<Object> list = Arrays.asList(lastSessionUsers);
                //LastUsersDataHolder.setData(list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //all not seen messages after disconnect. This is used to update the local table 'messages' with the server
        if(jsonArray1 != null){
            //build the 'Message' objects

            for(int i = 0; i <= jsonArray1.length() - 1; i++){
                Message message = null;
                try {
                    message = new Gson().fromJson(jsonArray1.getJSONObject(i).toString(), Message.class);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                messages_.add(message);
            }

            //save not seen messages after disconnect in local db in table 'messages'
            int rows = saveNotSeenMessages(messages_);

            //The following statement is already done in 'saveNotSeenMessages'
            //if(rows != messages.size()) throw new UnsupportedOperationException("Unexpected value. Insert bulk messages");

        }


        //built the 'NotSeenMessage' objects : author, image profile, number of the not seen messages
        // to display in home page at startup
        //notSeenMessages = new ArrayList<NotSeenMessage>();
        if(jsonArray2 != null){
            for(int i = 0; i <= jsonArray2.length() - 1; i++){
                try {
                    //JSONObject jsonObject = (JSONObject)jsonArray2.get(i);
                    NotSeenMessage notSeenMessage = new Gson().fromJson(jsonArray2.getJSONObject(i).toString(), NotSeenMessage.class);

                   /*
                   NotSeenMessage notSeenMessage = new NotSeenMessage(
                           jsonObject.getString("nickname"),
                           jsonObject.getString("nb"),
                           jsonObject.isNull("imageprofile") ? null : jsonObject.getString("imageprofile")
                   );
                   */

                    notSeenMessages.add(notSeenMessage);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //get all 'ChatUser' object users whom send messages after disconnect.
        //This is used only to update local table 'users'.
        if(jsonArray20 != null){
            for(int i = 0; i <= jsonArray20.length() - 1; i++){
                ChatUser chatUser = null;
                try {
                    chatUser = new Gson().fromJson(jsonArray20.getJSONObject(i).toString(), ChatUser.class);

                                /*
                                Message message = new Message(
                                        jsonArray1.getJSONObject(i).getString("fromnickname"),
                                        jsonArray1.getJSONObject(i).getString("tonickname"),
                                        jsonArray1.getJSONObject(i).getString("message"),
                                        jsonArray1.getJSONObject(i).getLong("time"),
                                        jsonArray1.getJSONObject(i).isNull("extra") ? null : jsonArray1.getJSONObject(i).getString("extra"),
                                        jsonArray1.getJSONObject(i).isNull("extraname") ? null : jsonArray1.getJSONObject(i).getString("extraname"),
                                        jsonArray1.getJSONObject(i).getString("ref"),
                                        jsonArray1.getJSONObject(i).isNull("mimetype") ? null : jsonArray1.getJSONObject(i).getString("mimetype"),
                                        jsonArray1.getJSONObject(i).getString("seen"),
                                        jsonArray1.getJSONObject(i).getString("deletedfrom"),
                                        jsonArray1.getJSONObject(i).getString("deletedto")
                                );
                                */
                    lastChatUsers.add(chatUser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //update the local db table 'users' with the new users if any.
        //See 'insertBulk'
        for(ChatUser chatUser : lastChatUsers){
            int i = saveChatUser_(chatUser);
            //Redmi error i==0
            if(i == 0 ) throw new UnsupportedOperationException("Unexpected value. Insert user not done");
        }

        //Convert 'arraylist' to 'array' . Method classic
        //int i = 0;
        //for(ChatUser chatUser : lastChatUsers){
        //    lastSessionUsers[i] = chatUser;
        //    i++;
        //}


        //get users name and connection time of all users whom contact nickname : old and new users
        // to update local table 'lastUser' with the server
        //Todo : add only entries for users after disconnect.
        Map<String, Long> map = new HashMap<>();
        if (jsonArray21 != null) {
            for(int i = 0; i <= jsonArray21.length() - 1; i++){
                try {
                    JSONObject jsonObject_ = (JSONObject)jsonArray21.get(i);
                    JSONObject key = jsonObject_.getJSONObject("connectedWith");
                    Iterator iterator = key.keys();
                    while(iterator.hasNext()){
                        String key_ = (String) iterator.next(); //user name
                        long value  = key.getLong(key_);        // connection time
                        map.put(key_, value);
                    }

                    //long value  = jsonObject_.getLong("time");
                    //map.put(key, value);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            //update the 'lastUser' table in local bd
            saveLastSessionUsers(NICKNAME, map);

            //build the 'ChatUser' object of last session users list from the 'chat_users' local table
            if(map != null)lastSessionUsers = getChatUsers(map.keySet());
            //Todo : do the follwing test
            //if(lastSessionUsers.length != map.keySet().size())throw new UnsupportedOperationException("Unexpected value. 'lastSessionUsers != map.keySet()'");
            Object[] o  = (Object[])lastSessionUsers;

            List<Object> list = Arrays.asList(o);
            LastUsersDataHolder.setData(list);
        }

        /*
        //users who send messages after disconnection
        ArrayList<ChatUser> afterDisconnectionUsers = null;
        for(int i = 0; i <= jsonArray6.length() - 1; i++){
            ChatUser chatUser = null;
            try {
                chatUser = new Gson().fromJson(jsonArray4.getJSONObject(i).toString(), ChatUser.class);
                afterDisconnectionUsers.add(chatUser);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        */
        //Save locally in sqlite db the users who send messages after disconnect.
        //update the locale db with the new users if any
        //See 'insertBulk'
        /*
        for(ChatUser chatUser : afterDisconnectionUsers){
            int i = saveChatUser(chatUser);
            if(i == 0 ) throw new UnsupportedOperationException("Unexpected value. Insert user not done");
        }
        */
        //get the ChatUser object of the unregistered users whom send messages after disconnect
        //get it from


        //next
        Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "get allNotSeenMessages", Snackbar.LENGTH_LONG).show();

        int nb = 0;
        //count all not seen messages sent from all users
        Iterator iterator = notSeenMessages.iterator();
        while (iterator.hasNext()){
            NotSeenMessage notSeenMessages = (NotSeenMessage) iterator.next();
            nb = nb + Integer.parseInt(notSeenMessages.nbMessages);
        }
        //update the number of displayed messages
        notSeenMessagesSummary.setText("Not seen Messages : " + nb);

        //display "not seen messages" in a fragment
        if(display){
            FragmentTransaction ft     = getSupportFragmentManager().beginTransaction();
            NotSeenMessagesFragment f  = NotSeenMessagesFragment.newInstance(jsonArray2);
            ft.replace(R.id.ll_not_seen_messages, f);
            //exception : "can not perform this action after onSaveInstanceState"
            // or
            // exception : 'FragmentManager has been destroyed'
            ft.commit();
            //ft.commitNow(); same error as 'ft.commit()'
            //ft.commitAllowingStateLoss();
        }

        //dispatch
        if(dispatch == null)return;
        //if(dispatch.equals("next_on_create"))nextOncreate();
        //if(dispatch.equals("init_socket1"))initSocket1();
    }


    //ask the server to get all not seen messages for thr user 'nickname'.
    //This is only valid if the user has connected and his name is saved in preferences.
    //If it is new or has deleted his account, there are no seen messages and this method is not called
    //This method is called when the preferences contains the 'nickname'
    private void getAllNotSeenMessages(String nickname, final String dispatch, final boolean display) {
        //get not seen messages
        socket.emit("get_all_not_seen_messages", nickname); //the response will be found in : 'setListen_get_all_not_seen_messages_res'
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

    private int saveNotSeenMessages(ArrayList<Message> messages) {

        Iterator<Message> iterator = messages.iterator(); //chatMessageList.iterator();
         ContentValues[] values = new ContentValues[messages.size()];
        for(int i = 0; i <= messages.size() - 1; i++){
            Message message = iterator.next();
            ContentValues value = new ContentValues();
            value.put(MessagesContract.COLUMN_FROM, message.fromNickname);
            value.put(MessagesContract.COLUMN_TO, message.toNickname);
            value.put(MessagesContract.COLUMN_MESSAGE, message.message);
            value.put(MessagesContract.COLUMN_REFERENCE, message.ref);
            value.put(MessagesContract.COLUMN_DATE, message.time);

            if(message.extra == null){
                value.putNull(MessagesContract.COLUMN_EXTRA);
            }else{
                value.put(MessagesContract.COLUMN_EXTRA, message.extra);
            }
            if(message.extraName == null){
                value.putNull(MessagesContract.COLUMN_EXTRANAME);
            }else{
                value.put(MessagesContract.COLUMN_EXTRANAME, message.extraName);
            }
            if(message.mimeType == null){
                value.putNull(MessagesContract.COLUMN_MIME);
            }else{
                value.put(MessagesContract.COLUMN_MIME, message.mimeType);
            }

            value.put(MessagesContract.COLUMN_SEEN, message.seen);

            if(message.deletedFrom == null){
                value.putNull(MessagesContract.COLUMN_DELETED_FROM);
            }else{
                value.put(MessagesContract.COLUMN_DELETED_FROM, message.deletedFrom);
            }

            if(message.deletedTo == null){
                value.putNull(MessagesContract.COLUMN_DELETED_TO);
            }else{
                value.put(MessagesContract.COLUMN_DELETED_TO, message.deletedTo);
            }
            values[i] = value;
        }
        int nbRows = 0;
        try {
            nbRows = getContentResolver().bulkInsert(MessagesContract.CONTENT_URI_MESSAGES, values);
        }catch(Exception e){
            e.printStackTrace();
        }
        if(nbRows != messages.size()) throw new UnsupportedOperationException("Unexpected value. Insert bulk messages");

        return nbRows;
    }

    //update the profile and save the 'currentChatUser' in local db
    public void updateProfile(){
            //update the 'currentChatUser'
            //currentChatUser.imageProfile = imageProfile_;

            //get old values before change
            lastConnectedAt          = currentChatUser.connectedAt;
            long lastdisconnectedAt  = currentChatUser.disconnectedAt;

            //set current values
            connectedAt     = new Date().getTime();
            disconnectedAt  = 0;

            currentChatUser.connectedAt     = connectedAt;
            currentChatUser.lastConnectedAt = 0; //not used
            currentChatUser.disconnectedAt  = 0;
            currentChatUser.status          = 1; //0=disconnect, 1=connect, ... cf 'chatUser'

            //save this 'currentChatUser' in local db. It will be saved in the server in 'join' event
            int row = saveChatUser(currentChatUser);
            if(row == 0) throw new UnsupportedOperationException("'Save the user in local db'");

            //base 64 string is null in the case it is associated with the default image
            imageProfile_ = currentChatUser.imageProfile;

            //Pour trouver l'uri' associée à cette image, il faut aller dans le 'Mediastore' ou
            //sauvegarder l'image et son uri dans l'historique.

            if(imageProfile_ == null){
                //Set default image profile. Display a rounded avatar image
                //imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.avatar));

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.avatar);

                //resize bitmap : the width or hight do not greater than 200 px
                bitmap = resizeBitmap(bitmap, 200);

                //get a rounded image
                bitmap = getRoundedCornerImage(bitmap, 200);
                imageProfile.setImageBitmap(bitmap);

                    /*
                    //convert bitmap to base64 string
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        }
                    });

                    //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] imageByte = stream.toByteArray();
                    imageProfile_ = Base64.encodeToString(imageByte, Base64.DEFAULT);
                    */

                //Enable the 'Camera' button to take photo
                camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));
                camera.setEnabled(true);

                //Enable the 'gallery' button to pick photo
                gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64));
                gallery.setEnabled(true);

                //Enable the 'history' button to pick photo
                history.setImageDrawable(getResources().getDrawable(R.drawable.history_64));
                history.setEnabled(true);

            }else{
                //Display a rounded image 'imageProfile'
                byte[] decodedString = Base64.decode(imageProfile_, Base64.DEFAULT);
                Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                //imageProfile.setImageBitmap(decodedByte);

                //Resize the bitmap 'decodedByte'
                decodedByte = resizeBitmap(decodedByte, 200);

                //crop (round) the image and display it.
                int w = decodedByte.getWidth(), h = decodedByte.getHeight();
                int radius = w > h ? h:w; // set the smallest edge as radius.

                RoundedImageView  roundedImageView = new RoundedImageView(this);
                Bitmap roundBitmap = roundedImageView.getCroppedBitmap(decodedByte, radius);

                imageProfile.setImageBitmap(roundBitmap);
                //Save the image profile base64 string
                oldImageProfile = imageProfile_;

                ////////////////////////////////////////////////////////////////////////////////
                //Enable the 'Delete' button
                delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
                delete.setEnabled(true);

                //Disable the 'Camera' button since there is an image
                camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));
                camera.setEnabled(false);

                //Disable the 'Gallery' button since there is an image
                gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));
                gallery.setEnabled(false);

                //Disable the 'History' button since there is an image
                history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));
                history.setEnabled(false);
            }
    }

    public void nextOncreate(){

        if(null != NICKNAME){

            //Fill the editText
            //nickname.setText(NICKNAME);

            //get the current 'ChatUser' from local db, if nothing is found get it from server.
            currentChatUser = getChatUser(NICKNAME);

            //Get the image profile uri from local db
            //String ImageProfileUri = getImagesProfile(NICKNAME).get(0); //Il est supposé que l'indice 0 correspond à l'imageProfileUri, ce n'est pas vrai

            if(currentChatUser != null){

                //get all not seen messages from server. Since if the user 'NICKNAME' is disconnected
                //his message are stored in the server not locally.
                //this statement is async
                //display  = display the not seen messages in home page under the start button
                //dispatch =
                //getAllNotSeenMessages(NICKNAME, null, true);

                //update the 'currentChatUser'
                //currentChatUser.imageProfile = imageProfile_;

                updateProfile();

                /* done in 'updateProfile'
                //set time connection
                connectedAt     = new Date().getTime();
                lastConnectedAt = currentChatUser.connectedAt;
                disconnectedAt  = currentChatUser.lastConnectedAt;

                currentChatUser.connectedAt     = connectedAt;
                currentChatUser.lastConnectedAt = lastConnectedAt;
                currentChatUser.disconnectedAt  = disconnectedAt;
                */

                //base 64 string is null in the case it is associated with the default image.
                imageProfile_ = currentChatUser.imageProfile;

                //Pour trouver l'uri' associée à cette image, il faut aller dans le 'Mediastore' ou
                //sauvegarder l'image et son uri dans l'historique.

                if(imageProfile_ == null){
                    //Set default image profile. Display a rounded avatar image
                    //imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.avatar));

                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.avatar);

                    //resize bitmap : the width or hight do not greater than 200 px
                    bitmap = resizeBitmap(bitmap, 200);

                    //get a rounded image
                    bitmap = getRoundedCornerImage(bitmap, 200);
                    imageProfile.setImageBitmap(bitmap);

                    /*
                    //convert bitmap to base64 string
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        }
                    });

                    //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] imageByte = stream.toByteArray();
                    imageProfile_ = Base64.encodeToString(imageByte, Base64.DEFAULT);
                    */

                    //Enable the 'Camera' button to take photo
                    camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));
                    camera.setEnabled(true);

                    //Enable the 'gallery' button to pick photo
                    gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64));
                    gallery.setEnabled(true);

                    //Enable the 'history' button to pick photo
                    history.setImageDrawable(getResources().getDrawable(R.drawable.history_64));
                    history.setEnabled(true);

                }else{
                    //here the 'imageProfile' not null. Display a rounded image 'imageProfile'
                    byte[] decodedString = Base64.decode(imageProfile_, Base64.DEFAULT);
                    Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    //imageProfile.setImageBitmap(decodedByte);

                    //Resize the bitmap 'decodedByte'
                    decodedByte = resizeBitmap(decodedByte, 200);

                    //crop (round) the image and display it.
                    int w = decodedByte.getWidth(), h = decodedByte.getHeight();
                    int radius = w > h ? h:w; // set the smallest edge as radius.

                    RoundedImageView  roundedImageView = new RoundedImageView(this);
                    Bitmap roundBitmap = roundedImageView.getCroppedBitmap(decodedByte, radius);

                    imageProfile.setImageBitmap(roundBitmap);
                    //Save the image profile base64 string
                    oldImageProfile = imageProfile_;

                    ////////////////////////////////////////////////////////////////////////////////
                    //Enable the 'Delete' button
                    delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
                    delete.setEnabled(true);

                    //Disable the 'Camera' button since there is an image
                    camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));
                    camera.setEnabled(false);

                    //Disable the 'Gallery' button since there is an image
                    gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));
                    gallery.setEnabled(false);

                    //Disable the 'History' button since there is an image
                    history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));
                    history.setEnabled(false);
                }

            }else{
                // ToDo : The 'Nickname' exists in the preferences but the 'currentChatUser' is null.
                // this occurs when the account is deleted or when the user is just register.
                // his name is added to preferences and the local db is also deleted
                // //build the 'currentChatUser' object
                getChatUserFromServer(NICKNAME);    //async operation

                //initSocket(NICKNAME);
                //throw new UnsupportedOperationException("unexpected state. The Nickname = " + NICKNAME + " exists " +
                // "in the preferences but the currentChatUser = " + currentChatUser +" is null.");
            }

        }//end (NICKNAME != null)


        /*
        File fileFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // emulator, wiko : -->/storage/emulated/0/Android/data/com.google.amara.chattab/files/Pictures
        //Samsung :            /mnt/sdcard/Android/data/com.google.amara.chattab/files/Pictures

        File fileFolder_ = Environment.getExternalStorageDirectory();
        // emulator, wiko : --> /storage/emulated/0
        //Samsung :            /mnt/sdcard

        File fileFolder__ = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //emulator, samsung : -->/storage/emulated/0/Download
        //Samsung :             /mnt/sdcard/Download
        */

        //editor.putString("key_name", "string value"); // Storing string
        //editor.putInt("key_name", "int value"); // Storing integer
        //editor.putFloat("key_name", "float value"); // Storing float
        //editor.putLong("key_name", "long value"); // Storing long

        //editor.commit(); // commit changes

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(com.example.aymen.androidchat.R.layout.activity_main_, container, false);

        Bundle bundle = this.getArguments();

        if(bundle != null){
            int currentTab = bundle.getInt("tab");
        }
    */
        ///////////////////////////////////////////////////////////////////////////////////////
        // example with 'join' : working. If it is placed in the network 'socket.emit' and wait
        // the response, it is not working.
        final int[][] total = {{0}};
        final boolean[][] done = {{false}};
        Thread b = new Thread(new Runnable(){
            public void run(){
                System.out.println("thread running...");
                    for(int i = 0; i < 100 ; i++){
                        System.out.println("boucle = " + i);
                        total[0][0] += i;
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            System.out.println(e);
                        }
                    }
                    done[0][0] = true;
                }

        });
        b.start();
        try {
            b.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("total = " + total[0][0]);

////////////////////////////////////////////////////////////////////////////////////////////
        //ThreadB = class, working. working. If it is placed in the network 'socket.emit' and wait
        // the response, it is not working.

        class ThreadB extends Thread{
            int total;
            boolean done = false;
            @Override
            public void run(){
                synchronized(this){
                    for(int i = 0; i < 100 ; i++){
                        total += i;
                    }
                    done = true;
                    notify();
                }
            }
        }

        ThreadB bb = new ThreadB();
        bb.start();

        System.out.println("will enter in sync...");
        //rentre dans le bloc 'sync' et s'arrête à 'wait' jusqu'à ce que le thread 'b' envoie le 'notify'. je ne sais pas l'utilité du 'wait'
        synchronized(bb){

            try{
                System.out.println("in sync, waiting for b to complete...");
                while(!bb.done) {
                    bb.wait();
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }

            System.out.println("Class method, Total is: " + bb.total);
        }
        System.out.println("after sync block ...");
////////////////////////////////////////////////////////////////////////////////////////////////////
        //// Thread b__ = new Thread(new Runnable()  not working.
        // Exception : 'object not locked by thread before wait()'
        /*
        Object object = new Object();
        final int[] total_ = {0};
        final boolean[] done_ = {false};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(this){
                    for(int i = 0; i < 100 ; i++){
                        total_[0] += i;
                    }
                    done_[0] = true;
                    notify();
                }
            }
        });

        thread.start();

        System.out.println("will enter in sync...");
        //Ne rentre pas dans le bloc 'sync' et n'executa jusqu'à ce que le thread 'b' envoie le 'notify'. je ne sais pas l'utilité du 'wait'
        synchronized(object){
            try{
                System.out.println("in sync, waiting for b to complete...");
                while(done_[0]) {
                    wait();
                }
            }catch(InterruptedException e){
                e.printStackTrace();
            }

            System.out.println("object monitor, Total is: " + total_[0]);
        }
        System.out.println("after sync block ...");
        */
        /////////////////////////////////////////////////////////////////////////////////

        //call UI component  by id
        //btn      = (Button) findViewById(com.example.aymen.androidchat.R.id.enter_chat);
        //nickname = (EditText) findViewById(com.example.aymen.androidchat.R.id.nickname);

        //change image profile, save it in locale db, set boolean imageProfileChanged to false
        // for next time and save it in 'preferences'

        //for testing
        boolean imageProfileChanged = true; //default false
        //save the 'imageProfileChanged' variable in 'Preferences'
        //NavigatorActivity.editor.putBoolean("image_profile_changed", imageProfileChanged);
        //NavigatorActivity.editor.commit();
    }

    /**
     * Return an array list of uris of images profile of the user from local db
     * @param nickname the name of the user
     * @return array list of uris of image profile
     */
    private ArrayList<String> getImagesProfile(String nickname) {
        ArrayList<String>uris = new ArrayList<>();
        String[] mProjection  = new String[]
        {
            ProfileHistoryContract.COLUMN_ID,
            ProfileHistoryContract.COLUMN_USER_NAME,
            ProfileHistoryContract.COLUMN_URI_PROFILE,
            ProfileHistoryContract.COLUMN_TIME,
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY,
                mProjection,
                ProfileHistoryContract.COLUMN_USER_NAME  + " =? ",

                new String[]{nickname},

                null);
        if(cursor == null) return null;

        cursor.moveToPosition(-1);

        long    id      = 0;
        String  uris_   = null;
        String  userName= null;
        long    time    = 0;

        while (cursor.moveToNext()) {
            id          = cursor.getLong(cursor.getColumnIndexOrThrow(ProfileHistoryContract.COLUMN_ID));
            userName    = cursor.getString(cursor.getColumnIndexOrThrow(ProfileHistoryContract.COLUMN_USER_NAME));
            uris_       = cursor.getString(cursor.getColumnIndexOrThrow(ProfileHistoryContract.COLUMN_URI_PROFILE));
            time        = cursor.getLong(cursor.getColumnIndexOrThrow(ProfileHistoryContract.COLUMN_TIME));

            uris.add(uris_);
        }
        cursor.close();
        return uris;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 'enterchat' btn event
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The button 'Enter' is clicked.
                //disable the button 'btn' to prevent multiple clicks.
                btn.setEnabled(false);

                //anyway, the button is disabled in edt event if it is empty
                if (nickname.getText().toString().isEmpty()) return;

                //check the user is already registered
                if(null == NICKNAME){
                    //The first time or the account has been deleted.
                    checkIsUserRegistered(nickname.getText().toString().trim());
                }else{
                    //The 'NICKNAME' is in preferences
                    initSocket1();
                }

                //if((null == NICKNAME) && (!nickname.getText().toString().isEmpty())){
                /*
                if(null == NICKNAME){//case new user or account deleted
                    socket.emit("is_user_registered_in_db", nickname.getText().toString(), new Ack() {
                        @Override
                        public void call(Object... args) {

                            JSONObject userExists = ((JSONObject) args[0]);
                            boolean userExist     = false;
                            try {
                                userExist = userExists.getBoolean("exists");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            userExist     = false; //for test only to remove in production

                            if(!userExist){
                                //the name is new
                                //set the final value of NICKNAME found in edt field.
                                //'temp' is the value found in preferences. save it temporarily in 'temp' variable.
                                String temp = NICKNAME; //may be null after reset or equals the value found in preferences
                                NICKNAME = nickname.getText().toString();

                                //save the NICKNAME in preferences
                                editor.putString("Nickname", NICKNAME);
                                editor.commit();

                                //Case first time or app deleted. The 'Preferences' is empty. Try to get data from
                                // server and build the 'currentChatUser'.
                                //This will be done in 'TabChatActivity'
                                //if((null == NICKNAME) && (!nickname.getText().toString().isEmpty())){
                                //    initSocket(nickname.getText().toString());
                                //}
                                initSocket1();
                            }else{
                                //the name already exists in server db, notify the user and open a dialog
                                runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                          builder.setTitle("Connection error");
                                          builder.setMessage("The username'" + nickname.getText().toString() + "' already exists.");

                                          // Specifying a listener allows you to take an action before dismissing the dialog.
                                          // The dialog is automatically dismissed when a dialog button is clicked.
                                          builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int which) {
                                                  dialog.dismiss();
                                                  socket.close();
                                                  finish();
                                                  return;
                                              }
                                          }).create().show();
                                      }
                                });
                            }
                        }
                    });
                }
                */
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
        Log.d("onResume", "onResume_restartActivity");
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
        socket.disconnect();

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

    /**
     * Save the uri of the image profile from server.
     * @param nickname the image profile for this user
     * @param profileImageUri the uri of the image profile
     * @param time
     * @return
     */
    private Uri saveProfileImageUriFromServer(String nickname, Uri profileImageUri, long time) {
        ContentValues values = new ContentValues();
        values.put(ProfileHistoryContract.COLUMN_USER_NAME,     nickname);
        values.put(ProfileHistoryContract.COLUMN_URI_PROFILE,   profileImageUri.toString());
        values.put(ProfileHistoryContract.COLUMN_TIME,          time);

        ContentResolver cr  = getContentResolver();

        // insert the image profile uri
        return cr.insert(ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY, values);
    }

        /**
         * Save the uri of the image profile from gallery or camera in local db. no duplicate uri allowed
         * @param nickname the image profile for this user
         * @param profileImageUri the uri of the image profile
         * @param time
         * @return
         */
    private boolean saveProfileImageUriFromGallery(String nickname, Uri profileImageUri, long time) {
        //time = 55555;
        boolean result = false;

        String[] params = {"=?", "=?", "=?"};

        /*
        cursor = db.execute("SELECT ...")
        if cursor.empty:
        db.execute("INSERT ...")
        else:
        db.execute("UPDATE ...")
        */

        //do select :
        ContentResolver cr = getContentResolver();

        // do select image profile uri
        String[] projection = {
                ProfileHistoryContract.COLUMN_USER_NAME,
                ProfileHistoryContract.COLUMN_URI_PROFILE,
                ProfileHistoryContract.COLUMN_TIME
        };
        String where = ProfileHistoryContract.COLUMN_USER_NAME + " =? AND " + ProfileHistoryContract.COLUMN_URI_PROFILE + " =? ";
        String[] selectionArgs = {nickname, profileImageUri.toString()};
        String order = null;

        Cursor cursor = cr.query(
                ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY,
                projection,
                where,
                selectionArgs,
                order);
        if (cursor == null)
            throw new UnsupportedOperationException("Get image from profile history. Unexpected cursor null value");
        int selectedRows = cursor.getCount();
        if (selectedRows != 0 && selectedRows != 1)
            throw new UnsupportedOperationException("Get image from profile history. Unexpected cursor.rowCount : " + selectedRows + " instead 1");

        if (selectedRows == 0) {
            //do insert
            ContentValues values = new ContentValues();
            values.put(ProfileHistoryContract.COLUMN_USER_NAME, nickname);
            values.put(ProfileHistoryContract.COLUMN_URI_PROFILE, profileImageUri.toString());
            values.put(ProfileHistoryContract.COLUMN_TIME, String.valueOf(time));

            Uri uri = cr.insert(
                    ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY,
                    values
            );
            if (uri != null) result = true;

        }
        if (selectedRows == 1) {
            //do update
            ContentValues values = new ContentValues();
            values.put(ProfileHistoryContract.COLUMN_TIME, String.valueOf(time));

            where = ProfileHistoryContract.COLUMN_USER_NAME + " =? AND " + ProfileHistoryContract.COLUMN_URI_PROFILE + " =? ";
            String[] selectionArgs_ = {nickname, profileImageUri.toString()};

            int rowUpdated = cr.update(
                    ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY,
                    values,
                    where,
                    selectionArgs_
            );
            if (rowUpdated == 1) result = true;
        }

        //insert if not exists
        // else do update
        // not working : soit 'insert' soit 'update' sans tenir compte de la condition d'existence
        // en fait : ce sont deux instructions indépendantes
        //
        /*
        String query = "INSERT INTO " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME + " " +
                "(" + ProfileHistoryContract.COLUMN_USER_NAME   + "," +
                      ProfileHistoryContract.COLUMN_URI_PROFILE + "," +
                      ProfileHistoryContract.COLUMN_TIME              +
                ")" +
                " SELECT '" + nickname + "', '" + profileImageUri.toString() + "'," + String.valueOf(time) +
                " WHERE NOT EXISTS (" +
                " SELECT " + ProfileHistoryContract.COLUMN_USER_NAME + "," + ProfileHistoryContract.COLUMN_URI_PROFILE + " FROM " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME +
                    " WHERE " +
                              ProfileHistoryContract.COLUMN_USER_NAME   + " = '" + nickname + "' AND "               +
                              ProfileHistoryContract.COLUMN_URI_PROFILE + " = '" + profileImageUri.toString()  +"'"  +
                ");" +

                " UPDATE " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME +
                " SET "    + ProfileHistoryContract.COLUMN_TIME          + " = '" + String.valueOf(time)       + "'"      +
                " WHERE "  + ProfileHistoryContract.COLUMN_USER_NAME     + " = '" + nickname                   + "' AND " +
                             ProfileHistoryContract.COLUMN_URI_PROFILE   + " = '" + profileImageUri.toString() + "';" ;
        */

        /*
        //insert if not exist only
        String query = "INSERT INTO " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME + " " +
                       "(" + ProfileHistoryContract.COLUMN_USER_NAME   + "," +
                             ProfileHistoryContract.COLUMN_URI_PROFILE + "," +
                             ProfileHistoryContract.COLUMN_TIME              +
                        ")" +
                        " SELECT '" + nickname + "', '" + profileImageUri.toString() + "'," + String.valueOf(time) +
                           " where not exists (" +
                                               "select 1 from " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME + " where " + ProfileHistoryContract.COLUMN_URI_PROFILE + " =?" +
                                             ")";
        */

        /* not working
                +
                                        ProfileHistoryContract.COLUMN_USER_NAME   + "" +
                                        ProfileHistoryContract.COLUMN_URI_PROFILE + ""  +
                                        ProfileHistoryContract.COLUMN_TIME        +
                                ") "                          +
                                //"IN(" + params  + ")"       +
                                "IN(=?, =?, =?)"               +

        			    " WHERE NOT EXISTS " +
                         "("  +
        			          " SELECT 1 FROM " + ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME +
                              " WHERE " + ProfileHistoryContract.COLUMN_URI_PROFILE + " =? "        +
                         ")" ;
        */

        /*
        FileDbHelper dbHelper = new FileDbHelper(this);
        SQLiteDatabase db     = dbHelper.getWritableDatabase();
        Cursor cursor         = null;
        try{
            cursor = db.rawQuery(query,
                    new String[]{
                            //String.valueOf(time),
                            //nickname,
                            //profileImageUri.toString(),
                            //,
                            //profileImageUri.toString()
                     }
            );
        }catch (Exception e){
            e.getMessage();
        }
        */
        cursor.close();
        return result;
    }

    /**
     * Get images profile uris from server and save them in db locally.
     * @param nickname the supplied username
     */
    public void saveImageProfileUris_(String nickname) {
        //Ask server to get data of the current user named 'Nickname'
        socket.emit("get_image_profile_uri", nickname);
        //return imageProfileUris[0];
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

    /** We come here the first time.
     * Save uris of images profile from server in local db
     * @param uris The supplied JSONArray uri of images profile
     * @return The number of rows saved.
     */
    private int saveImageProfileUris(JSONArray uris) {
        int rows = 0;
        //Uri uri  = null;
        ContentValues[] values = new ContentValues[uris.length()];
        for(int i = 0; i <= uris.length() - 1; i++){
            try {
                JSONObject jsonObject   = uris.getJSONObject(i);
                String nickname         = jsonObject.getString("nickname");
                String uri_             = jsonObject.getString("uri");
                long date               = jsonObject.getLong  ("date"); //date when the image profile is saved in db

                //it is better to use bulkInsert. See below
                //uri                     = saveProfileImageUriFromServer(nickname, Uri.parse(uri_), date);

                //fill the 'content values' to use later in 'bulk insert' below.
                ContentValues value     = new ContentValues();
                value.put(ProfileHistoryContract.COLUMN_USER_NAME, nickname);
                value.put(ProfileHistoryContract.COLUMN_URI_PROFILE, uri_);
                value.put(ProfileHistoryContract.COLUMN_TIME, date);

                values[i] = value;
                //if(uri != null)rows++;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //if(rows != uris.length())throw new UnsupportedOperationException("save profile uris in local db : unexpected value");

        int nbInsertedRows = 0;
        try {
            nbInsertedRows = getContentResolver().bulkInsert(ProfileHistoryContract.CONTENT_URI_PROFILE_HISTORY, values);
        }catch(Exception e){
            e.printStackTrace();
        }
        if(nbInsertedRows != uris.length()) throw new UnsupportedOperationException("Unexpected value. Insert bulk image profile");

        return nbInsertedRows;
    }

    public void initSocket(String username) {
        //Ask server to get data of the current user named 'username'. Build 'currentChatUser' and
        // save it in local db.
        socket.emit("get_user", username);

        //get data of the current user named 'username' : See socket.on
    }

    public void initSocket_(String username) {
        //Ask server to get data of the current user named 'username'
        socket.emit("get_user", username);

        //
        // on ne peut pas faire suivre 'socket.emit' et 'socket.on' en même temps
        //

        //get data of the current user named 'username'
        socket.on("get_user_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject user = ((JSONObject) args[0]);
                        if(user != null){
                            //Save the current user in local db
                            int nbRows = saveUser(user);

                            //build the current chat user
                            try {
                                String nickname         = user.getString("nickname");
                                String imageProfile     = user.getString("imageprofile");
                                int status              = user.getInt   ("status");
                                int notSeenMessages     = user.getInt   ("notseenmessages");
                                long connectedAT        = user.getLong  ("connected");
                                long disconnectedAt     = user.getLong  ("disconnected");
                                long lastConnectedAt    = user.getLong  ("lastconnected");
                                String blacklistAuthor  = user.getString("blacklistauthor");

                                currentChatUser = new ChatUser(
                                        nickname,
                                        null,
                                        imageProfile,
                                        status,
                                        notSeenMessages,
                                        connectedAT,
                                        disconnectedAt,
                                        lastConnectedAt,
                                        blacklistAuthor
                                );
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //save this user in local db
                            int row = saveChatUser(currentChatUser);
                            if(row == 0) throw new UnsupportedOperationException("'Save the user in local db'");

                            //update the image profile in 'imageView'
                            imageProfile_ = currentChatUser.imageProfile; //base 64 string

                            /*if(imageProfile_ == null){
                                imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.avatar));

                                *//*
                                //Convert bitmap to base64 string
                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                    }
                                });

                                //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] imageByte = stream.toByteArray();
                                imageProfile_ = Base64.encodeToString(imageByte, Base64.DEFAULT);
                                *//*

                                //Enable the 'Camera' button
                                camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50));
                                camera.setEnabled(true);
                            }else{
                                byte[] decodedString = Base64.decode(imageProfile_, Base64.DEFAULT);
                                Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                imageProfile.setImageBitmap(decodedByte);

                                //Enable the 'Delete' button
                                delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
                                delete.setEnabled(true);
                            }*/

                            //Save the user in local db
                            //save the 'currentChatUser' localy
                            //int row = saveChatUser(currentChatUser);
                            //if(row == 0) throw new UnsupportedOperationException("'Save the first time user in local db'");
                            initSocket1();

                        }else {
                            //No user found, build the first time 'currentChatUser'
                            currentChatUser = null;
                            imageProfile_ = null;

                            //Set the image profile of the not found user to default
                            //imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.avatar));

                        /*
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            }
                        });

                        //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] imageByte = stream.toByteArray();
                        imageProfile_ = Base64.encodeToString(imageByte, Base64.DEFAULT);

                        //imageProfile_ = "";

                         */
                            initSocket1();
                        }
                    }
                });
            }
        });
    }

    public void initSocket1() {

        //free events and disconnect socket from 'MainActivity'
        socket.off("get_all_not_seen_messages_res");
        //socket.off("get_image_profile_uri_back"); //save
        //socket.off("get_image_profile_uri_back"); //get
        socket.off("get_user_back");
        socket.off("get_user_ban_back");
        socket.off("is_user_present_in_db_back");
        socket.off("get_image_profile_uri_back");
        socket.disconnect();

        SocketHandler.setSocket(socket); //used in 'TabChatActivity' after the below intent is sent.

        //The following statement is done in 'onStart'
        //NICKNAME = nickname.getText().toString();

        //save the nickname in 'Preferences'
        //editor.putString("Nickname", NICKNAME);
        //editor.commit();

        //Get connection infos from local db
        //ChatUser currentUser = getChatUser(NICKNAME);

        //int firstTime               = preferences.getInt("first_time", 0);
        //boolean imageProfileChanged = preferences.getBoolean("image_profile_changed", false);

        //save with no duplicate image uri in local db. It is saved in the server in 'join' event.
        //The 'profileImageUri' is set in 'onActivityResult' for the camera and gallery
        if(profileImageUri != null){
            boolean result = saveProfileImageUriFromGallery(NICKNAME, profileImageUri, new Date().getTime());
            if(!result)throw new UnsupportedOperationException("insert image profile uri is failed");
            saveProfileImageUriFromGalleryRemotely(NICKNAME, profileImageUri, new Date().getTime());
        }
        //when the image profile is not modified or the image profile is the default we arrive here .
        nextInitSocket1();

        //Get time of connection
        //long connectedAt      = new Date().getTime();
        //long lastConnectionAt = preferences.getLong("connected_at", 0);
        //long lastConnectionAt = currentUser.connectedAt;
        //currentUser.connectedAt = connectedAt;

                    /*
                    //Case the first time or the app has been deleted. In the case the app has been deleted
                    // get values from the server. If there are no values in server put
                    // 'lastConnectionAt' = the default = 0;
                    //All this is done in 'TabChatActivity'

                    //if(lastConnectionAt == 0)lastConnectionAt = connectedAt;

                    //get the disconnected time. It is set in 'TabChatActivity.Emitter' event 'Emitter.Listener mDisconnectionListener'
                    long disConnectionAt = preferences.getLong("disconnected_at", 0);
                    //Case the first time,
                    //if(disConnectionAt == 0)disConnectionAt = connectedAt;

                    //save the current time
                    editor.putLong("connected_at", connectedAt);

                    //save the nickname
                    editor.putString("nickname", nickname.getText().toString()); // Storing nickname

                    editor.commit();
                    */
        /*
        //dispatch is done in 'nextOnStart'

        Intent intent = new Intent(MainActivity.this, TabChatActivity.class);
        //update the 'currentChatUser' if the nickname has changed. The 'NICKNAME' is the last name
        //in the edt when the btn 'enter' is clicked
        if(currentChatUser != null)currentChatUser.nickname = NICKNAME;
        intent.putExtra("current_user", (Parcelable)currentChatUser);
        intent.putParcelableArrayListExtra("not_seen_messages", (ArrayList<NotSeenMessage>) notSeenMessages);
        //intent.putExtra("fragment_index", 0); //pass zero for Fragment one.
        intent.putExtra("Nickname", NICKNAME);
        intent.putExtra("image_profile", imageProfile_);        //may be null

        //for test only
        if(imageProfile_ != null){
            final byte[] decodedBytes = Base64.decode(imageProfile_, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        intent.putExtra("image_profile_uri", (profileImageUri == null) ? null : profileImageUri.toString());  //may be null
        intent.putExtra("first_time", firstTime);
        intent.putExtra("image_profile_changed", imageProfileChanged);
        intent.putExtra("connection_time", connectedAt);

        startActivityForResult(intent, INTENT_REQUEST_CODE);
        */
    }

    private void saveProfileImageUriFromGalleryRemotely(String nickname, Uri profileImageUri, long time) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("nickname", nickname);
            jsonObject.put("uri", profileImageUri.toString());
            jsonObject.put("time", String.valueOf(time));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("save_image_profile_uri",
                jsonObject,
                new Ack() {
                    @Override
                    public void call(Object... args) {
                        String result = ((String) args[0]);
                        if(result.equals("fail")) throw new UnsupportedOperationException("save image profile uri in server failed ");
                        nextInitSocket1();
                    }
                });
    }

    //after saving the image profile uri on local and remote db, we come here to do the next send data
    //to 'TabChatActivity'
    private void nextInitSocket1() {
        int firstTime               = preferences.getInt("first_time", 0);
        boolean imageProfileChanged = preferences.getBoolean("image_profile_changed", false);

        //dispatch
        Intent intent = new Intent(MainActivity.this, TabChatActivity.class);
        //update the 'currentChatUser' if the nickname has changed. The 'NICKNAME' is the last name
        //in the edt when the btn 'enter' is clicked
        if(currentChatUser != null)currentChatUser.nickname = NICKNAME;
        intent.putExtra("current_user", (Parcelable)currentChatUser);
        intent.putParcelableArrayListExtra("not_seen_messages", (ArrayList<NotSeenMessage>) notSeenMessages);
        //intent.putExtra("fragment_index", 0); //pass zero for Fragment one.
        intent.putExtra("Nickname", NICKNAME);
        intent.putExtra("image_profile", imageProfile_);        //may be null

        //for test only
        if(imageProfile_ != null){
            final byte[] decodedBytes = Base64.decode(imageProfile_, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        }

        intent.putExtra("image_profile_uri", (profileImageUri == null) ? null : profileImageUri.toString());  //may be null
        intent.putExtra("first_time", firstTime);
        intent.putExtra("image_profile_changed", imageProfileChanged);
        intent.putExtra("connection_time", connectedAt);

        startActivityForResult(intent, INTENT_REQUEST_CODE);
    }

    /**
     * Save the chat user infos in sqlite db. Insert or replace. see : 'FileDbContentProvider'
     * @param chatUser the supplied chat user object to save
     * @return number of inserted or updated rows.
     */
    private int saveChatUser(ChatUser chatUser) {
        ContentValues values = new ContentValues();
        values.put(UsersContract.COLUMN_USER_NAME,          chatUser.nickname);
        //values.put(UsersContract.COLUMN_CHAT_ID,          chatUser.chatId);
        values.put(UsersContract.COLUMN_PROFILE,            chatUser.imageProfile);
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
         */

        ContentResolver cr  = getContentResolver();
        int numRows = 0;

            // insert or replace the user in sqlite 'chat_users' table
            Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);
            if(newRowUri != null) numRows = 1;

        return numRows;
    }

    /** This method is used to updaye the table 'users' with the server
     * Save the chat  user infos in sqlite db
     * @param chatUser the supplied chat user object to save
     * @return number of inserted or updated rows.
     */
    private int saveChatUser_(ChatUser chatUser) {
        ContentValues values = new ContentValues();
        values.put(UsersContract.COLUMN_USER_NAME,          chatUser.nickname);
        //values.put(UsersContract.COLUMN_CHAT_ID,          chatUser.chatId);
        values.put(UsersContract.COLUMN_PROFILE,            chatUser.imageProfile);
        values.put(UsersContract.COLUMN_STATUS,             chatUser.status);
        values.put(UsersContract.COLUMN_NOT_SEEN,           chatUser.notSeenMessagesNumber);
        values.put(UsersContract.COLUMN_CONNECTED,          chatUser.connectedAt);
        values.put(UsersContract.COLUMN_DISCONNECTED_AT,    chatUser.disconnectedAt);
        values.put(UsersContract.COLUMN_BLACKLIST_AUTHOR,   chatUser.blacklistAuthor);

        ContentResolver cr  = getContentResolver();
        int numRows = 0;

        // insert the user or replace it if it exist.cf 'FileDbContentProvider'
        Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);
        if(newRowUri != null) numRows = 1;

        return numRows;
    }


    ////insert in table "chat_users"
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
            values.put(UsersContract.COLUMN_PROFILE,            user.getString("imageprofile"));
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
        //insert in table "chat_users"
        Uri newRowUri = cr.insert(UsersContract.CONTENT_URI_USERS, values);
        if(newRowUri != null) numRows = 1;
        return numRows;
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
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


    private void getChatUserFromServer(String nickname){
        socket.emit("get_user", nickname);  // the response is found in "setEmitterListener_get_user_back()".
    }

    // get the 'ChatUser' object for the supplied username from local db.
    private ChatUser getChatUser(String nickname) {
        ChatUser chatUser = null;
        String[] mProjection = new String[] {
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
        if(cursor == null || cursor.getCount() == 0) return null;

        cursor.moveToPosition(-1);

        long id                = 0;
        String userName        = null;
        String imageProfile    = null;
        int status             = 0;
        int notSeenMessages    = 0;
        long connectedAt       = 0;
        long disconnectedAt    = 0;
        String blacklistAuthor = null;

        while (cursor.moveToNext()) {
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
            if(userName == null)chatUser =  null;
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        //login
        if (requestCode == REQUEST_CODE_LOGIN &&   // send code for login
                resultCode == RESULT_CODE_LOGIN && // receive code when login ends.
                null != data) { //data sent from 'postLogin' in 'LoginActivity' in module 'authentication'
            int status = data.getIntExtra("status", -1);
            if((status == -1))throw new UnsupportedOperationException("login. Unexpected return value (-1)");
            if((status == SUCCESS) || (status == REMEMBER))startChat();
            if((status == QUIT) || (status == BAN) || (status == TIMEOUT))finish();
        }

        //after 'back' button is pressed in 'TabChatActivity.finish()'we arrive here
        if (requestCode == INTENT_REQUEST_CODE &&   // send code
                resultCode == INTENT_RESULT_CODE && // receive code.
                null != data) { //data sent from ''

                //send a finish request to 'NavigatorActivity' who launched 'MainActivity'

            Intent data_ = new Intent();
            //for illustation only
            data_.putExtra("returnKey1", "Swinging on a star. ");
            data_.putExtra("returnKey2", "You could be better then you are. ");
            // Activity finished ok, return the data
            setResult(RESULT_CODE_CHAT, data_); //the data are returned to 'onActivityResult' of 'NavigatorActivity' which lanched the intent.


            finish();
        }

        //Camera
        if (requestCode == REQUEST_CODE_CAMERA && null != data) onCaptureImageResult(data);

        //'History'
        if (requestCode == REQUEST_CODE_IMAGE_PROFILE_HISTORY &&   // send code
                resultCode == RESULT_OK && // receive code
                null != data) { //data sent from 'ImageProfileHistory'

            //get the bitmap to display from the selected image in 'ImageProfileGallery'
            //profileImageUri = data.getData();

            String path = data.getStringExtra("path");

            //Bitmap bitmap   = decodeUriToBitmap(this, profileImageUri);

            //Exception : 'requires android.permission.MANAGE_DOCUMENTS or android.permission.MANAGE_DOCUMENTS'
            //test
            //if (CheckPermissions.hasPermission(999, //REQUEST_CODE,
            //        Manifest.permission.READ_EXTERNAL_STORAGE, this)){
            //}

            Bitmap bitmap = null;

            //exception : access denied
            //try {
            //    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), profileImageUri);
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
            //String path = getPathFromUri(this, profileImageUri);
                bitmap = BitmapFactory.decodeFile(path);


            //resize bitmap : the width or hight do not greater than 200 px
            bitmap     = resizeBitmap(bitmap, 200);

            //Display a rounded image
            int w      = bitmap.getWidth(), h = bitmap.getHeight();
            int radius = w > h ? h : w; // set the smallest edge as radius.

            RoundedImageView  roundedImageView = new RoundedImageView(this);
            Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

            //get base64 string
            imageProfile_ = getBase64String(roundBitmap); //used in 'ChatUser'

            //Set ImageView
            imageProfile.setImageBitmap(roundBitmap);
            ///////////////////////////////////////////////////////////////////////////////////////
            //history clicked
            history.setEnabled(false);
            history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));

            camera.setEnabled(false);
            camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));

            gallery.setEnabled(false);
            gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));

            delete.setEnabled(true);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
        }

        //Gallery
        if (requestCode == REQUEST_CODE_GALLERY &&   // send code
                resultCode == RESULT_OK && // receive code
                null != data) { //data sent from 'ImageProfileHistory'

            //marche avec : content://com.android.providers.media.documents/document/image%3A1630094103069
            // content://com.android.providers.downloads.documents/document/2200
            //marche avec : content://media/external/images/media/1630094102639

            String path = data.getData().getPath();

            /*
            //ne fait rien pour exception : 'No persistable permission grants found for UID 10100 and Uri content.... '
            Uri uri = data.getData();
            ContentResolver cr = getContentResolver();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            cr.takePersistableUriPermission(uri, flags);
            */

            getContentResolver().takePersistableUriPermission(
                    data.getData(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            //String path  = getPathFromUri(data.getData());
            //Bitmap bitmap0 = decodeUriToBitmap(this, data.getData());

            //getContentResolver().takePersistableUriPermission(sourceTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //
            try {
                //exception : 'com.android.providers.downloads.DownloadStorageProvider
                // uri content://com.android.providers.downloads.documents/document/23 from pid=7615,
                // uid=10100 requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
                Bitmap bitmap1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //test
            String path_          = getPathFromUri(this, data.getData());
            Uri uriFileScheme    = data.getData(); //displays : content://com.android.providers.media.documents/document/image%3A29
            File file            = new File(data.getData().getPath());
            Uri uriContentScheme = FileProvider.getUriForFile(this, "com.google.amara.chattab.fileprovider", file);
            //uriContentScheme   = content://com.google.amara.chattab.fileprovider/my_file___/document/2200
            //uriFileScheme      = content://com.android.providers.downloads.documents/document/2200

            //profileImageUri    = data.getData();
            Uri sourceTreeUri    = data.getData();
            ////////////////////////////////////////////////////////////////////////////////////////
            //exception : 'No persistable permission grants found for UID 10100 and Uri content.... '
            //workaround exception : remove the following statement .
            //getContentResolver().takePersistableUriPermission(sourceTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            ////////////////////////////////////////////////////////////////////////////////////////

            //String path = getPathFromUri(data.getData());

            //file descriptor method
            Bitmap bitmap1;
            try {
                bitmap1 = getBitmapFromUri(this, data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //mediastore method
            try {
                Bitmap bitmap2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }


           // not used
            /*
           Uri uri        = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
            Cursor cursor = getContentResolver().query(uri,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex0 = cursor.getColumnIndex(filePathColumn[0]);
            int columnIndex1 = cursor.getColumnIndex(filePathColumn[1]);
            String picturePath = cursor.getString(columnIndex0);
            String pictureId   = cursor.getString(columnIndex1);
            cursor.close();
            */

            //if ("content".equalsIgnoreCase(uri.getScheme())) {
            //    String columnIndex_ =  getDataColumn(this, uri, null, null);
            //}
            //

            //get the uri 'content' scheme of the image
            //set the 'profileImageUri', the image is saved in 'onStart'

            profileImageUri = uriFileScheme; //uriFileScheme2UriContentScheme(path);;

            //get the bitmap to display from the selected image
            Bitmap bitmap = decodeUriToBitmap(this, data.getData());

            //get thumbnail image
            Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(bitmap, 100, 100);

            //compress the bitmap
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);//Compress to the JPEG format.
            // quality of 0 means compress for the smallest size. 100 means compress for max visual quality.

            Bitmap bitmapp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());

            //resize the bitmap to have 200px width or height
            bitmap = resizeBitmap(bitmap, 200);


            //Display a rounded image
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            int radius = w > h ? h : w; // set the smallest edge as radius.

            RoundedImageView  roundedImageView = new RoundedImageView(this);
            Bitmap roundBitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

            imageProfile.setImageBitmap(roundBitmap);
            imageProfile_   = getBase64String(bitmap); //used in 'ChatUser'. Si on met 'roundBitmap', il y a un fond noir.
            //We don't use 'roundBitmap' since in the base-64 string the outside area of the circle
            //is colored in black

            ////////////////////////////////////////////////////////////////////////////////////////
            //Disable 'Gallery'
            gallery.setEnabled(false);
            gallery.setImageDrawable(getResources().getDrawable(R.drawable.gallery_64_gray));

            //Disable 'Camera'
            camera.setEnabled(false);
            camera.setImageDrawable(getResources().getDrawable(R.drawable.camera_50_gray));

            //Disable 'History'
            history.setEnabled(false);
            history.setImageDrawable(getResources().getDrawable(R.drawable.history_64_gray));

            //Enable 'Delete'
            delete.setEnabled(true);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.delete_64));
            ////////////////////////////////////////////////////////////////////////////////////////
        }

        //Authentication
        if (requestCode == REQUEST_CODE_AUTH &&   // send code
                resultCode == RESULT_CODE_AUTH && // receive code
                null != data) {//data sent from 'LoginActivity' in 'authentication' mudule

                String status = data.getStringExtra("status");
                System.out.println("onActivityResult of MainActivity status = " + status);

                if (status.equals("fail"))  super.finish(); //end the app
        }
    }

    /**
     * //get uri content scheme from uri file scheme like : '/storage/3334-6339/DCIM/Camera/IMG_20221119_074516.jpg'.
     * The value of 'data.getData() is a uri file scheme
     * @param path is the absolute path of the uri file scheme
     * @return uri content scheme like : 'content://media/external/images/media/1630094103275'
     */
    private Uri uriFileScheme2UriContentScheme(String path) {

        Uri allImagesuri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.Images.Media._ID ,
                MediaStore.Images.Media.DATA ,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID
        };
        String selection = MediaStore.Images.Media.DATA + " Like ? "; //" like? ";
        String[] selectionArgs = {path}; //{picturePath};

        //String[] selectionArgs = {"%Pictures%"}; //{ "Camera"}; //"Camera"};
        String order = null; //MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC";

        Cursor cursor1 = this.getContentResolver().query(allImagesuri,
                projection,
                selection,
                selectionArgs,
                order);
        if(cursor1 == null)throw new UnsupportedOperationException("Get image from gallery. Unexpected cursor null value");

        cursor1.moveToPosition(-1);

        long id_ = 0;
        while(cursor1.moveToNext()) {
            //imageFolder folds = new imageFolder();
            String id = cursor1.getString(cursor1.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            id_ = Long.parseLong(id);
            String name     = cursor1.getString(cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
            String folder   = cursor1.getString(cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
            String datapath = cursor1.getString(cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        }
        cursor1.close();
        if(id_ == 0)throw new UnsupportedOperationException("Get image from gallery. Unexpected id value = 0");


        // not used
        //Uri uriFileScheme = data.getData();
        //File file = new File(data.getData().getPath());
        //Uri uriContentScheme = FileProvider.getUriForFile(this, "com.google.amara.chattab.fileprovider", file);
        //

        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);

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


