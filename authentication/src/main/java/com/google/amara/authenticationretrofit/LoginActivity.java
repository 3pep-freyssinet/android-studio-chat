package com.google.amara.authenticationretrofit;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.aymen.androidchat.ChatUser;
import com.example.aymen.androidchat.sql.CredentialsContract;
import com.example.aymen.androidchat.sql.FileDbHelper;
import com.google.amara.authenticationretrofit.remote.ApiUtils;
import com.google.amara.authenticationretrofit.remote.UserService;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.Socket;

import java.util.ArrayList;
import java.util.Date;

import io.socket.emitter.Emitter;
import okhttp3.Credentials;
import retrofit2.Call;
import okhttp3.ResponseBody;

public class LoginActivity extends    AppCompatActivity
                           implements BroadcastNotification,
                                      NetworkConnectionReceiver.ReceiverListener{

    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected   = new ArrayList<>();
    private ArrayList<String> permissions           = new ArrayList<>();

    // integer for permissions results request
    private static final int  ALL_PERMISSIONS_RESULT = 1011;
    private static final int  REQUEST_PERMISSIONS    = 100;
    private static final long BAN_TIME               = 3600000; //1 hour

    //used in postLogin
    private static final int SUCCESS  = 0;
    private static final int REMEMBER = 1;
    private static final int QUIT     = 2;
    private static final int TIMEOUT  = 3;
    private static final int BAN      = 4;

    private static final int RESULT_CODE_LOGIN      = 601;
    private static final int RESULT_CODE_AUTH       = 701;
    private static final int REQUEST_CODE_REGISTER  = 800;
    private static final int RESULT_CODE_REGISTER   = 801;
    private static final int REQUEST_CODE_RESET_PWD = 900;
    private static final int RESULT_CODE_RESET_PWD  = 901;

    public static UserService userService;

    private EditText        edtUsername, edtPassword;
    private TextInputLayout edtUsername_, edtPassword_;

    private Button          btnLogin, btnRegister;

    private TextView        tryCounter, tvLoginStatus;
    private Button          btnQuit;

    private Switch switchRememberMe;

    final int mode = Activity.MODE_PRIVATE;

    final String MYPREFS = "MyPreferences_Login";
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor myEditor;

    private boolean     rememberMe;
    private boolean     isBan;
    private final String CONNECT        = "Connect";
    private final String REPEAT         = "Repeat";

    private int nbTry                = 0;
    private static final int MAX_TRY = 3;

    int usernameTextColor    = 0; //color username text

    private BroadcastReceiver myReceiver           = null;

    private AutenticationInterface auth;
    public static Socket socket; //also used in 'FirstTimeLoginActivity'
    private boolean exists;



    //private boolean firstTime;
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // SharedPreferences are actually saved in a file in the app private directory:
    // /data/data/package_name/shared_prefs/somefilename.xml
    //SharedPreferences database always deleted when app is uninstalled. but In Manifest
    //android:allowBackup="true"
    // If allowBackup is true then data is not clear even if application is uninstall then install.
    // so always keep allowBackup false when data do not want store.
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // wait a thred to terminate and resume the process
    // Thread thread = new Thread(new Runnable(){
    //    void run(){}
    // }).start()
    // thread.join();
    // the above statement make the main UI thread wait until the thread named 'thread' ends.
    // After that the process can continue.
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ATTENTION l'historique (back) est désactivé dans le manifest.
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //curl --basic --user username:password -d "" http://ipaddress/test/login
    // C:\Users\aa>curl  localhost:8080/books/all   ---- json
    //C:\Users\aa>curl  localhost:8080/  ---->Hello from Thorntail!
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public interface Callback{
        void authenticateUser(String username);
    }
    private static Callback          callback;

    public static void setCallback(Socket socket, Callback callback, SharedPreferences sharedPreferences){
        LoginActivity.callback           = callback;
        LoginActivity.socket             = socket;
        LoginActivity.sharedPreferences  = sharedPreferences;
        LoginActivity.myEditor           = sharedPreferences.edit();;
    }


    //@Override
    protected void onCreate_(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //title bar text
        setTitle(R.string.login);
        setContentView(R.layout.activity_login);
        btnQuit         = (Button) findViewById(R.id.btn_quit);
        edtUsername     = (EditText) findViewById(R.id.textInputEditText_identifier);
        edtUsername_    = (TextInputLayout) findViewById(R.id.outlinedTextField_identifier);

        edtPassword     = (EditText) findViewById(R.id.textInputEditText_identifier);
        edtPassword_    = (TextInputLayout) findViewById(R.id.outlinedTextField_pwd);
        btnLogin        = findViewById(R.id.btn_login);
        tryCounter      = findViewById(R.id.textView_Counter);

        tvLoginStatus   = findViewById(R.id.tv_login_status);
        btnRegister     = findViewById(R.id.btn_register);
        btnQuit         = findViewById(R.id.btn_quit);


        //Quit and return to 'NavigatorActivity' in 'app' module
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //button 'Quit' clicked.
                endActivity("fail");
                //postLogin(LoginActivity.this, null, QUIT);
                //System.exit(0);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //title bar text
        setTitle(R.string.login);
        setContentView(R.layout.activity_login);

        edtUsername     = (EditText) findViewById(R.id.textInputEditText_identifier);
        edtUsername_    = (TextInputLayout) findViewById(R.id.outlinedTextField_identifier);

        edtPassword     = (EditText) findViewById(R.id.textInputEditText_pwd);
        edtPassword_    = (TextInputLayout) findViewById(R.id.outlinedTextField_pwd);

        btnLogin        = findViewById(R.id.btn_login);
        tryCounter      = findViewById(R.id.textView_Counter);

        tvLoginStatus   = findViewById(R.id.tv_login_status);
        btnRegister     = findViewById(R.id.btn_register);
        switchRememberMe = findViewById(R.id.remember_me);
        btnQuit         = findViewById(R.id.btn_quit);

        //at startup, set the state of switch from preferences
        boolean rememberMe = sharedPreferences.getBoolean("remember_me", false);
        switchRememberMe.setChecked(rememberMe);

        // Initialise broadcast receiver
        //myReceiver = new MyReceiver(this);
        //broadcastIntent();

        //the socket is transmitted by the interface 'etCallback'
        setEmitterListener_is_user_registered_in_db_res();
        socket.connect();

        //At startup, hide the 'tryCounter' textView.
        tryCounter.setVisibility(View.INVISIBLE);
        btnLogin.setEnabled(false);
        tvLoginStatus.setVisibility(View.INVISIBLE);

        //setEmitterListener_is_user_present_in_db_back(); //not used, since only the ack in needed.

        //send the 'socket' to 'LoginActivity'
        //LoginActivity.socket.connect();

        //get socket
        //socket = com.google.amara.chattab.SocketHandler.getSocket();

        //Retrofit interface
        userService     = ApiUtils.getUserService();

        //Get the preferences
        //Get the sharedPreferences and editor from the class 'PreferencesHandler'

        //the 'Prefrences' are transmitted by the inerface 'setCallback'
        //sharedPreferences = getSharedPreferences(MYPREFS, 0);

        //myEditor = sharedPreferences.edit();

        //setEmitterListener_is_user_present_in_db_back();
        //socket.connect();

        //set permissions to request listed permissions in the manifest
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);

        //if the permissions are already granted in device, permissionsToRequest.size() = 0;
        permissionsToRequest = permissionsToRequest(permissions);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA},
                REQUEST_PERMISSIONS);// Request permissions, the result will be found in 'onRequestpermissionsResult'

        /*
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
                    //show PermissionRationale dialog

            } else {
                //request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA},
                        REQUEST_PERMISSIONS);// Request permissions, the result will be found in 'onRequestpermissionsResult'
            }
        } else {
            //The permissions are already granted in device
        }
        */
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
            case REQUEST_PERMISSIONS :
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                //The user has pressed the 'Deny' button on permission dialog.
                // Manage permissions rejected. Show a dialogue with two buttons : OK and Cancel
                //explaing why thes permissions are mandatory.
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(this).
                                    setMessage("These permissions are mandatory to get your app working correctly. You need to allow them.").
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
                    //here, no permissions rejected. The user has pressed the 'Allow' button on permission dialog.
                    return;
                }

                break;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkIsUserRegisteredRemotely(String user, String pwd) {
        //if the name entered in 'nickname' edit text already exists ?
        //As the user is new or he deleted the app, the local db is also deleted.
        //then ask the server if the user exists or not.

        JSONObject credential  = new JSONObject();
        try {
            credential.put("name", user);
            credential.put("pwd", pwd);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final boolean[] userExist = {false};
        socket.emit("is_user_registered_in_db", credential, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject userExists = ((JSONObject) args[0]);
                if(userExists == null){
                    endActivity("fail");
                    return;
                }

                try {
                    userExist[0] = userExists.getBoolean("exists");
                    isUserRegisteredRemotelyResult(userExist[0], user);

                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void isUserRegisteredRemotelyResult(boolean register, String username) {
        if(register){
            //the user credentials are saved in 'setEmitterListener_is_user_registered_in_db_res()'.
            //the 'firstTime' value in preferences is set in 'setEmitterListener_is_user_registered_in_db_res()'.
            //set 'NICKNAME' in preferences is set in 'successLogin'

            //redirect to chat home page
            successLogin(username);
        }else{
            //Give him 3 attempts. After that if there is no success, ban.
            //nbTry++ is done when the 'repeat' button is clicked.


            if(nbTry > MAX_TRY - 2){
                banish(this, System.currentTimeMillis());
                //update the 'Preferences'
                myEditor.putLong("start_Ban_Time", System.currentTimeMillis());
                myEditor.commit();
                return;
            }
            //another try
            //workaround : 'Only the original thread that created a view hierarchy can touch its views'
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tryCounter.setVisibility(View.VISIBLE);
                    tryCounter.setTextColor(Color.RED);
                    tryCounter.setText("It remains : " + (MAX_TRY - (nbTry + 1) + " attempts"));

                    tvLoginStatus.setVisibility(View.VISIBLE);
                    tvLoginStatus.setTextColor(Color.RED);
                    tvLoginStatus.setText("Login failed");

                    btnLogin.setEnabled(true);
                    btnLogin.setText(REPEAT);
                }
            });
        }
    }

    private void setEmitterListener_is_user_registered_in_db_res() {
        socket.on("is_user_registered_in_db_res", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject userCredential = ((JSONObject) args[0]);
                        //save the credentials in local sqlite db
                        int numRows = saveUserCredentials(userCredential);

                        if(numRows != 1)throw new UnsupportedOperationException("loginActivity. saveUserCredentials return numRows = " + numRows);

                        //Here, the credentials are saved in local and remote db.
                        // set the 'firstTime' value in preferences only when setup.
                        int firstTime = sharedPreferences.getInt("first_time", 0);
                        if(firstTime == 0){
                            myEditor.putInt("first_time", 1);
                            myEditor.commit();
                        }
                    }
                });
            }
        });
    }

    /**
     * Save the user credentials in sqlite db. Insert or replace. see : 'FileDbContentProvider'
     * @param userCredential json object supplied in 'is_user_registered_in_db'
     * @return number of inserted or updated rows.
     */
    private int saveUserCredentials(JSONObject userCredential) {

        ContentValues values = new ContentValues();
        try {
            values.put(CredentialsContract.COLUMN_USERNAME,     userCredential.getString("username"));
            values.put(CredentialsContract.COLUMN_PWD,          userCredential.getString("password"));
            values.put(CredentialsContract.COLUMN_DATE,         userCredential.getString("date"));
            values.put(CredentialsContract.COLUMN_DATE_HISTORY, userCredential.getString("datehistory"));
            values.put(CredentialsContract.COLUMN_PWD_HISTORY,  userCredential.getString("pwdhistory"));
        } catch (JSONException e) {
        e.printStackTrace();
        }

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

            // insert or replace the user in sqlite 'chat_credentials' table
            Uri newRowUri = cr.insert(CredentialsContract.CONTENT_URI_CREDENTIALS, values);
            if(newRowUri != null) numRows = 1;

            return numRows;
        }

    //Save ban info in server.
    private void SaveBanInfo(String applicationId, long startBanTime) {
        JSONObject banInfo = new JSONObject();
        try {
            banInfo.put("applicationId", applicationId);
            banInfo.put("startBanTime",  startBanTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        socket.emit("ban_info", banInfo, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject jsonObject = ((JSONObject) args[0]);

                if(jsonObject == null)notifyError("'Save ban info ' Unknown error = null");


                String banStatus = null;
                try {
                    banStatus = jsonObject.getString("status");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(banStatus.equals("fail"))notifyError("'Save ban info ' Unknown error = fail");

                //show dialog


                //'Animators may only be run on Looper threads' error : workaround
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        //notify the user
                        new AlertDialog.Builder(LoginActivity.this).
                                setMessage("Number of tries reached the limit.").
                                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //postLogin(LoginActivity.this, username, BAN);
                                        //finish();
                                        endActivity("fail");
                                        //System.exit(0);
                                    }
                                })
                                .setCancelable(false)
                                .create().show();
                        Looper.loop();
                    }
                }.start();
            }
        });
    }

    private void notifyError(String message) {
        new AlertDialog.Builder(LoginActivity.this).
                setMessage(message).
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //postLogin(LoginActivity.this, username, BAN);
                        //finish();
                        endActivity("fail");
                        //System.exit(0);
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    //return to the caller which start this activity : 'onActivityResult' of 'NavigatorActivity' in 'App' module
    //status=fail or success
    private void endActivity(String status) {

        // Prepare data intent
        Intent intent = new Intent();
        intent.putExtra("status", status);

        // Activity finished ok, return the data 'onActivityResult' of 'NavigatorActivity' in 'App' module.
        setResult(RESULT_CODE_AUTH, intent); //the data are returned to 'onActivityResult' of 'NavigatorActivity' in 'App' module which launched this intent.
        finish();//obligatoire
    }

    //when the 'back' button is pressed, we come here.
    @Override
    public void onBackPressed(){
        endActivity("fail");
    }

    /*
    //Not used since only tha ack is needed.
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
                            callback.doSomething(exists);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
    */

    //the lyfecycle at startup is : onCreate - onStart - onRestart - onResume - ...
    //the lyfecycle on back pressed is : onRestart - onStart - onResume - ...
    //il faut mettre tout le traitement des vues ici dans 'onStart' ou 'onRestart' de telle sorte si
    //on fait "back" on arrive ici.

    @Override
    protected void onStart() {
        super.onStart();

        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                //edtPassword_.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                //edtPassword_.setHelperTextColor(ColorStateList.valueOf(Color.GREEN));

                String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%&*-]).{6,8}";
                if(!edtPassword.getText().toString().matches(passwordPattern)){

                    btnLogin.setEnabled(false);
                    //edtPassword_.setErrorEnabled(true);
                    //edtPassword_.setError("The password is not valid");

                    if(s.length() == 0){
                        //edtPassword_.setError("No input, password required");
                    }
                    if(s.length() > 8){
                        //edtPassword_.setError("The length password exceeds the limit");
                    }
                    return ;
                }else{
                    //edtPassword_.setHelperText("The password is valid");
                    btnLogin.setEnabled(true);
                    if(edtUsername.getText().toString().isEmpty()){
                        btnLogin.setEnabled(false);
                    }
                }

            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        //username
        edtUsername.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //show the keyboard
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        edtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //edtPassword_.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                //edtPassword_.setHelperTextColor(ColorStateList.valueOf(Color.GREEN));

                btnLogin.setEnabled(false);
                edtPassword_.setEnabled(false);

                String usernamePattern = "[a-zA-Z0-9]{6,8}";
                if(edtUsername.getText().toString().matches(usernamePattern)){
                    edtPassword_.setEnabled(true);

                    //edtUsername_.setErrorTextColor(ColorStateList.valueOf(Color.RED));

                    //edtUsername_.setErrorEnabled(true);
                    //edtUsername_.setError("The identifier is not valid");

                    //treated in pattern
                    //if(s.length() == 0){
                    //   //edtUsername_.setError("No input, identifier required");
                    //}
                    //if(s.length() > 8){
                    //    //edtUsername_.setError("The length identifier exceeds the limit");
                    //}

                }
                //else{
                    //edtUsername_.setHelperText("The identifier is valid");
                    //edtPassword_.setEnabled(true);
                    //btnLogin.setEnabled(true);
                    //if(edtPassword.getText().toString().isEmpty()){
                    //    btnLogin.setEnabled(false);
                    //}
               //}
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });


        //login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString();
                String password = edtPassword.getText().toString();

                String label = ((Button) v).getText().toString();
                switch (label) {
                    case CONNECT:
                        //The validation of the form is done in 'edtUsername.addTextChangedListener'.
                        //do login
                        doLogin(username, password);
                        break;
                    case REPEAT:
                        nbTry++;
                        btnLogin.setText(CONNECT);
                        tvLoginStatus.setVisibility(View.INVISIBLE);
                        tryCounter.setVisibility(View.INVISIBLE);
                        
                        //Set focus and show the keyboard.
                        edtUsername.requestFocus();
                        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE); //ne marche pas.
                        //Ceci marche.
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        //Delete the content of 'username' and 'password'.
                        //edtUsername.setText(null);
                        //edtPassword.setText(null);
                        //Enable the content of 'username'.
                        edtUsername.setEnabled(true);
                        edtPassword.setEnabled(true);
                        break;

                    default:
                        String s = "Invalid action.";
                        break;
                }
            }
        });

        //Register
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //button 'Register' clicked. Redirect to 'FirstTimeLoginActivity'.
                //redirect to 'FirstTimeLoginActivity'
                Intent intent = new Intent(LoginActivity.this, FirstTimeLoginActivity.class);
                intent.putExtra("xx", "yy"); //dummy values
                startActivityForResult(intent, REQUEST_CODE_REGISTER);
            }
        });


        //Remember me switch
        switchRememberMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch 'Rememeber me ' clicked. Save state in 'Preferences'
                Boolean rememberMe = switchRememberMe.isChecked();

                myEditor.putBoolean("remember_me", rememberMe);
                myEditor.commit();
            }
        });


        /* all is done when the 'login' button is clicked then the state is saved in 'Preferences'
        switchRmemberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Snackbar.make(buttonView, "Switch state checked " + isChecked, Snackbar.LENGTH_LONG)
                        .setAction("ACTION",null).show();

                myEditor.putBoolean("rememberMe", isChecked);
                myEditor.commit();
            }
        });
        */

        //Quit
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //button 'Quit' clicked. Return to 'onActivityResult' in 'MainActivity' of 'app' module.
                int i = 0;
                endActivity("fail");

                //doRemoteLogin("azerty", "qwerty");
                //postLogin(LoginActivity.this, null, QUIT);
                //System.exit(0);
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Called when a ban occur.
     * @param context the context.
     * @param startBanTime the time when the ban starts. It is set after 3 unsuccessful attempts to login.
     */
    private void banish(Context context, long startBanTime) {
        //Notify the user how many time it remain to have free access.
        //long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
        //long minutes  = remainingTime /  60000;
        //long secondes = (remainingTime % 60000);
        //int secondes_ = (int)(secondes / 1000);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                //notify the user
                new AlertDialog.Builder(context).
                        setMessage("You are banished. Retry later in 1 hour.").
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                                endActivity("fail");
                            }
                        }).create().show();
                Looper.loop();
            }
        }.start();
    }

    /**
     *
     * @return the state of the connection.
     */
    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo info = cm.getActiveNetworkInfo();
            connected = (info != null) && info.isAvailable() && info.isConnectedOrConnecting();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
            connected = false;
        }
        return connected; //Here connected = false.
    }
////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Redirec the user to first Time page.
     * @param context the context
     */

    private void firstTime(Context context) {
        // intent
        Intent intent = new Intent(context, FirstTimeLoginActivity.class);
        intent.putExtra("xx", "yy"); //dummy values
        startActivity(intent);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * After login successfully, redirect the user to chat.
     * @param context the context.
     * @param username the username.
     * @param status the status of the authentication.
     */
    private void postLogin(Context context, String username, int status) {
        // clear credential fields and send result to the calling activity 'MainActivity.
        edtUsername.setText(null);
        edtUsername.setEnabled(true);
        edtUsername.requestFocus();
        edtPassword.setText(null);

        //Send some extra with the intent
        //Get last connection time
        long lastConnextion = LoginActivity.sharedPreferences.getLong("lastConnection", 0);

        //Set new time connection in preferences.
        LoginActivity.myEditor.putString("username", username);
        LoginActivity.myEditor.putLong("lastConnection", new Date().getTime());
        LoginActivity.myEditor.putInt("status", status);
        LoginActivity.myEditor.commit();

        // Prepare data intent
        Intent intent = new Intent();
        intent.putExtra("username", username);
        intent.putExtra("status", status);
        intent.putExtra("lastConnection", lastConnextion);
        intent.putExtra("isConnected", isConnected());
        // Activity finished ok, return the data
        //Todo : peut-on retourner à ''onActivityResult' of 'MainActivity'' qui n'a pas demarré cette activté
        setResult(RESULT_CODE_LOGIN, intent); //the data are returned to 'onActivityResult' of 'MainActivity' which lanched the intent.
        finish();//obligatoire
    }

    /**
     * To validate  password : it is not null, lenght = 0 and contains legal characteres.
     * @param password the supplied password.
     * @return boolean the supplied identifiants are valid or not.
     */
    private boolean validatePwd(String password) {

        if (password == null || password.trim().length() == 0) {
            //Toast.makeText(this, "Password is required", Toast.LENGTH_LONG).show();
            return false;
        }

        //String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]{2,3}";
        //String passwordPattern = "[a-zA-Z0-9#@]{6,8}";
        //(?=...) = groupe de capture 'noter ?=' qui verifie s'il y a au moins une majuscule. (?=.*?[A-Z])
        String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{6,8}";

        if(!password.matches(passwordPattern))return false;
        return true;
    }


    /**
     * To validate username. It is not null, it has length != 0 and contains legal characters.
     * @param username the supplied username
     * @return boolean the supplied username is valid or not.
     */
    private boolean validateUsername(String username) {
        if (username == null || username.trim().length() == 0) {
            //Toast.makeText(this, "Username is required", Toast.LENGTH_LONG).show();
            return false;
        }

        //String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]{2,3}";
        String usernamePattern = "[a-zA-Z0-9]{6,8}";

        if(!username.matches(usernamePattern))return false;
        return true;
    }


    /**
     * To validate login identifiers (username and password). They are not null, lenght = 0,
     * contains alphabetical characters and digits only.
     * @param username the supplied username
     * @param password the supplied password.
     * @return boolean the supplied identifiers are valid or not.
     */
    private boolean validateLogin(String username, String password) {
        if (username == null || username.trim().length() == 0) {
            //Toast.makeText(this, "Username is required", Toast.LENGTH_LONG).show();
            return false;
        }
        if (password == null || password.trim().length() == 0) {
            //Toast.makeText(this, "Password is required", Toast.LENGTH_LONG).show();
            return false;
        }

        //String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.[a-z]{2,3}";
        String usernamePattern = "[a-zA-Z0-9]{6,8}";
        String passwordPattern = "[a-zA-Z0-9#@]{6,8}";

        if(!username.matches(usernamePattern))return false;
        if(!password.matches(passwordPattern))return false;
        return true;
    }

    /**
     * Do login : In this step, the username and password are valid.
     * 1st : Check the 'first_time'value in 'Preferences' if any.
     * 'first_time'=0 (default), in case of deleted or just installed app. There is no db locally.
     * 'first_time'=1, in case the user is already signed and there is db locally.
     *
     * case ''first_time'=0' (default) : ask the server if the username is present in remote db
     * -- the username is present (deleted app):redirect to login page and check the username and pwd.
     *    See below the case 'first_time'=1'. Download the credentials data fo this user from server to lacal db.
     * -- the username is not present (first time): redirect to signup form.
     *
     * case 'first_time'=1': redirect to login page.
     * Now, check if the username and pwd are present in database.
     *   ** check if the username is present in local db.
     *      - if it ok (present in local db) do check the corresponding password.
     *        -- the corresponding password is ok. Redirect to chat home page
     *        -- no, the password is false. Give the user 3 tries
     *           if the password is ok in these 3 tries, Redirect to chat home page.
     *           if the number of tries is reached, ban the user for 1 hour and exit.
     *       - there is no username present in local db.It might be an error, remain in login form
     *
     * - no the username is not present in local db. It might be the first time. Redirect to signup form.
     * @param username the supplied username who signing.
     * @param password the supplied password who signing.
     */

    private void doLogin(final String username, final String password) {
        //get 'first_time' value from 'Prefernces'
        int firstTime = sharedPreferences.getInt("first_time", 0);
        if(firstTime == 0){
            //default. Ask the server and check the username and pwd
            isUserRegisteredRemotely(username, password); //the result of this question is found in
                                                          // 'isUserRegisteredRemotelyResult'
            return;
        }
        //here the first_time=1. Check username and pwd in sqlite db
        boolean isUserRegisteredLocally = isUserRegisteredLocally(username, password);
        //if the user is not register locally (error). Give him 3 attempts. After that if there is no
        //success, ban
        if(!isUserRegisteredLocally){
            //ask the server
            isUserRegisteredRemotely(username, password);

            /*
            //Give him 3 attempts. After that if there is no success, ban.
            //nbTry++;
            if(nbTry > MAX_TRY - 2){
                //
                banish(this, System.currentTimeMillis());
                //update the 'Preferences'
                myEditor.putLong("start_Ban_Time", System.currentTimeMillis());
                myEditor.commit();
                return;
            }
            int redColor = Color.RED;
            tvLoginStatus.setVisibility(View.VISIBLE);
            tryCounter.setVisibility(View.VISIBLE);

            tvLoginStatus.setTextColor(redColor);
            tvLoginStatus.setText("Login failed");

            tryCounter.setTextColor(redColor);
            tryCounter.setText("It remains : " + (MAX_TRY - nbTry - 1) + " tries");

            btnLogin.setEnabled(true);
            btnLogin.setText(REPEAT);
            */
            return;
        }

        //here the user is register locally.redirect to chat home page.
        successLogin(username);

        //callback.doSomething(exists);

        /*
        // Prepare data intent. the data are returned to 'onActivityResult' of 'MainActivity' of 'app' module which lanched the intent.
        Intent data = new Intent();
        //for illustation only
        data.putExtra("returnKey1", "Swinging on a star. ");
        data.putExtra("returnKey2", "You could be better then you are. ");
        // Activity finished ok, return the data
        setResult(RESULT_CODE_AUTH, data); //the data are returned to 'onActivityResult' of 'MainActivity' of 'app' module which lanched the intent.
        super.finish();//obligatoire, sinon il n'a pas de retour au 'onActivityResult' of 'MainActivity'
        //return;
        */

    }

    private void failureLocalLogin(String username, String password) {
        //At this point, there is an error : the credentials are not corrects. Give a try to user.
        if(nbTry > MAX_TRY - 1){
            //the number of try exceed the limit
            //try to check credentials from server. Asynch task
            isUserRegisteredRemotely(username, password);


            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity(); //like finish();
            }
            //Ban the user for one hour.
            myEditor.putLong("startBanTime", new Date().getTime());
            myEditor.commit();

            //notify the user, he exceed thr 3 attemps.
            new AlertDialog.Builder(this).
                    setMessage("Number of tries exceed the limit.").
                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            postLogin(LoginActivity.this, username, BAN);
                            //finish();

                            //System.exit(0);
                        }
                    }).create().show();

            */

            return;
        }
        //The number of tries has not reach the limit
        tryCounter.setVisibility(View.VISIBLE);
        tryCounter.setText((nbTry + 1) + "/" + MAX_TRY);
        btnLogin.setText(REPEAT);
        btnLogin.requestFocus();
        //Show the keyboard : ne marche pas
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        edtUsername.setEnabled(false);
        edtPassword.setEnabled(false);
    }

    //Redirect to 'NavigatorActivity' witch launched ths activity.
    private void successLogin(String username) {
        //update the preferences
        myEditor.putBoolean("remember_me", switchRememberMe.isChecked());
        myEditor.commit();

        //set 'NICKNAME' in preferences
        myEditor.putString("NICKNAME", username);
        myEditor.commit();

        //login success, return to 'NavigatorActivity' witch launched ths activity.
        // Prepare data intent
        Intent intent = new Intent();
        intent.putExtra("status", "success");
        intent.putExtra("NICKNAME", username);

        // Activity finished ok, return the data 'onActivityResult' of 'NavigatorActivity' in 'App' module.
        setResult(RESULT_CODE_AUTH, intent); //the data are returned to 'onActivityResult' of 'NavigatorActivity' in 'App' module which launched this intent.
        finish();//obligatoire
    }

    private void isUserRegisteredRemotely(String username, String password) {
        //async
        checkIsUserRegisteredRemotely(username, password);

        //the user is not registered in local nor in server. redirect to registration
        //if(!isUserRegisteredRemotely){
        //    //redirect to registration
        //    return false;
        //}
        //return false;
    }

    /**
     * Get user from local db (sqlite) if any
     * @param username username of the user who ask to login.
     * @param password password of the user who ask to login.
     * @return true or false
     */

    private boolean isUserRegisteredLocally(String username, String password) {
        ChatUser chatUser = null;
        String[] mProjection = new String[] {
                CredentialsContract.COLUMN_ID,
                CredentialsContract.COLUMN_USERNAME,
                CredentialsContract.COLUMN_PWD
        };

        String tableName  = CredentialsContract.CREDENTIALS_TABLE_NAME;

        //Method : ContentProvider
        FileDbHelper dbHelper = new FileDbHelper(this);
        SQLiteDatabase db     = dbHelper.getWritableDatabase();
        String query          = "SELECT username, password FROM " + tableName + " WHERE " +
                                "username" + " =? AND password" + " =?";

        //This query will return '1' if the table has at least one row, and '0' if it is empty.
        //String query          = "SELECT EXISTS (SELECT 1 FROM credentials)";

        Cursor cursor = db.rawQuery(query,
                new String[]{username, password},
                //new String[]{},
                null
        );
        if(cursor == null ) return false;
        //la table exist, username n'existe pas, cursor.getCount() = 0.
        //si la table n'existe pas, il a une exception
        //
        if(cursor.getCount() != 1)return false;
        return true;
    }

    private boolean isUserRegisteredLocally_(String username, String password) {
        ChatUser chatUser = null;
        String[] mProjection = new String[] {
                CredentialsContract.COLUMN_ID,
                CredentialsContract.COLUMN_USERNAME,
                CredentialsContract.COLUMN_PWD
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                CredentialsContract.CONTENT_URI_CREDENTIALS,
                mProjection,
                CredentialsContract.COLUMN_USERNAME  + " =? ",

                new String[]{username},

                null
        );
        if(cursor == null ) return false;
        //la table exist, username n'existe pas, cursor.getCount() = 0.
        //
        if(cursor.getCount() != 1)return false;
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        //Register : we come from 'FirstTimeLogin Activity' either
        // quit button is pressed and status = 'fail'
        // or
        // success registration and status='success'
        if (requestCode == REQUEST_CODE_REGISTER &&   // send code for login
                resultCode == RESULT_CODE_REGISTER && // receive code when login ends.
                null != data) { //data sent from 'postLogin' in 'LoginActivity' in module 'authentication'
            // or data sent from 'FirstTimeLoginActivity' in module 'authentication'
            String status = data.getStringExtra("status");
            //String status_ = status != null ? "success" : "fail";
            //if(username != null)callback.authenticateUser(username);

            //returned to 'onActivityResult' in 'NavigatorActivity' of 'app' module.
            endActivity(status);
        }
           //Reset pwd : we come from 'ChangePasswordActivity'
            // success reset pwd and status='success'
            if (requestCode == REQUEST_CODE_RESET_PWD &&   // send code for reset pwd
                    resultCode == RESULT_CODE_RESET_PWD && // receive code when reset pwd ends.
                    null != data) { //data sent from  'ChangePasswordActivity' in module 'authentication'

                String status = data.getStringExtra("status");
                //String status_ = status != null ? "success" : "fail";
                //if(username != null)callback.authenticateUser(username);

                //return to 'onActivityResult' in 'NavigatorActivity' of 'app' module.
                endActivity(status);

            //if ((status == -1))
            //    throw new UnsupportedOperationException("login. Unexpected return value (-1)");
            //if ((status == SUCCESS) || (status == REMEMBER)) startChat();
            //if ((status == QUIT) || (status == BAN) || (status == TIMEOUT)) finish();
        }
    }

    private void doLogin_(final String username, final String password) {
        //local check
        String localUsername = sharedPreferences.getString("username", null);
        String localPassword = sharedPreferences.getString("password", null);

        if((localUsername == null) || (localPassword == null)){
            //Redirect the user to register
            Intent intent = new Intent(LoginActivity.this, FirstTimeLoginActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            startActivity(intent);
            return;
        }

        //Check the credentials : compare the supplied username and pwd with those stored in preference.
        //if they match, redirect the user to next activity.
        if(username.toString().equals(localUsername.toString()) && (password.toString().equals(localPassword.toString()))) {
            //Clear the try counter
            tryCounter.setVisibility(View.INVISIBLE);
            tryCounter.setVisibility(View.INVISIBLE);
            nbTry = 0;

            //Save the state of 'rememberMe' of the checkBox
            myEditor.putBoolean("rememberMe", rememberMe);
            myEditor.commit();
            // intent
            //login start main activity
            //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            //intent.putExtra("username", username);
            //startActivity(intent);
            postLogin(LoginActivity.this, username, SUCCESS);
            return;
        }
        //At this point, there is an error : the credentials are not corrects. Give a try to user.
        if(nbTry > MAX_TRY - 2){
            //the number of try exceed the limit
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity(); //like finish();
            }
            //Ban the user for one hour.
            myEditor.putLong("startBanTime", new Date().getTime());
            myEditor.commit();

            //notify the user, he exceed thr 3 attemps.
            new AlertDialog.Builder(this).
                    setMessage("Number of tries exceed the limit.").
                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            postLogin(LoginActivity.this, username, BAN);
                            //finish();

                            //System.exit(0);
                        }
            }).create().show();
            return;
        }
        tryCounter.setVisibility(View.VISIBLE);
        tryCounter.setText((nbTry + 1) + "/" + MAX_TRY);
        btnLogin.setText(REPEAT);
        btnLogin.requestFocus();
        //Show the keyboard : ne marche pas
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        edtUsername.setEnabled(false);
        edtPassword.setEnabled(false);

        //Remote login, do nothing
        boolean remoteLogin = false; //testing purpose
        if(remoteLogin)doRemoteLogin(username, password);
    }


    /**
     * Check the credentials remotly on the server
     * @param username the supplied username.
     * @param password the supplied password.
     */
    private void doRemoteLogin(final String username, String password) {
        //Create credentials
        String credentials = Credentials.basic(username, password);

        //Avec la méthode ci-dessous, l'appel n'arrive pas au server.
        //
        //String basic = username + ":" + password;
        //String authToken = android.util.Base64.encodeToString(basic.getBytes(), android.util.Base64.DEFAULT);
        //authToken_ = "Basic " + authToken;
        //String basic_ = "Basic %s";

        //final Call<ResponseBody> call = userService.valideAutorization(String.format(basic_, authToken), "azerty");
        //final Call<ResponseBody> call = userService.valideAutorization(authToken_, "azerty");
        //final Call<ResponseBody> call = userService.valideAutorization(basic, "azerty");

        //call the 'validateAutorization' method in the inserface 'UserService' in folder 'remote'
        final Call<ResponseBody> call = userService.valideAutorization(credentials, "azerty-qwerty");
        /*
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if(!response.isSuccessful()) {
                    //Toast.makeText(LoginActivity.this, "Connexion error", Toast.LENGTH_LONG).show();

                    //errorBody() : The raw response body of an unsuccessful response.
                    //errorBody().string() : renvoie le message d'erreur dand le endpoint
                    //return Response.status(Response.Status.EXPECTATION_FAILED).entity("error").build();

                    return;
                }

                if(response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        //System.out.println("body = "+body);
                        //System.out.println("Headers = "+response.headers());
                        //System.out.println("isSuccessful = "+response.isSuccessful()); //Returns true if code() is in the range [200..300).
                    } catch (IOException e) {
                        e.printStackTrace();
                        e.getMessage();
                    }
                    //Save the state of 'rememberMe' of the checkBox
                    myEditor.putBoolean("rememberMe", rememberMe);
                    myEditor.commit();
                    // intent
                    //login start main activity
                    //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username", username);
                    //startActivity(intent);
                    postLogin(LoginActivity.this, username, SUCCESS);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                t.getMessage();
                return;
            }
        });
        */
    }
    //The broadcat network has sent notification
    @Override
    public void sendNetworkNotification(String status) {
        View view = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(view, "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();
        //or
        Snackbar.make(findViewById(android.R.id.content), "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();

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

                //Notify the user
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setMessage("No Internet connection");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                }).create().show();

                break;

                 /*
                //Get list of fragments
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                if(fragments.isEmpty())return;
                int index  = 0;
                int index_ = 0;
                int i      = 0;
                */

            //the 'sectionsPagerAdapter' method is not used
                /*
                for (int i = 0; i <= sectionsPagerAdapter.getCount() - 1; i++) {
                    Fragment fragment = sectionsPagerAdapter.getItem(i);
                    if (ChatBoxActivity.class.isInstance(fragment)) index = i;
                    if (ChatBoxMessage.class.isInstance(fragment)) index_ = i;
                }
                */

                /*
                for(Fragment f0 : fragments){
                    if(ChatBoxActivity.class.isInstance(f0)) index = i;
                    if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                    i++;
                }

                ChatBoxActivity f = (ChatBoxActivity)fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                int x = 0;

                if((f != null) && (f.isVisible()))x = 0;

                 */

        }
    }

    private void broadcastIntent() {
        registerReceiver(myReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onNetworkChange(boolean isConnected) { }

}