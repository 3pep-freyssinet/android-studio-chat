package com.google.amara.authenticationretrofit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.amara.authenticationretrofit.remote.UserService;
import com.google.android.material.textfield.TextInputLayout;
import com.example.aymen.androidchat.sql.CredentialsContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.socket.client.Ack;
import io.socket.client.Socket;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class FirstTimeLoginActivity extends AppCompatActivity {

    private final String SUIVANT      = "Next";
    private final String VALIDER_USER = "Check the identifier";
    private final String VALIDER_PWD  = "Check the password";
    private final String REGISTER     = "Register";

    private static final int RESULT_CODE_REGISTER  = 801;

    private TextInputLayout username_, password1_, password2_;
    private EditText username, password1, password2;
    private TextView registerStatus;
    private Button button, quit;

    private TextView statusUsername;
    private TextView statusPwd;
    public TextView statusRegistration;
    private UserService userService;
    public boolean userFound = false; //false = username not found in database.
    private TextView labelPwd1, labelPwd2;
    private int labelPwd1Height, labelPwd2Height, password1height, password2height, statusPwdHeight;
    private ImageView userInformation, pwdInformation;

    final String MYPREFS = "MyPreferences_Login";
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor myEditor;

    public interface Callback_{
        void registerUser(String username);
    }
    private static Callback_ callback;
    private static Socket    socket;

    public static void setCallback(Socket socket, Callback_ callback, SharedPreferences sharedPreferences){
        FirstTimeLoginActivity.callback             = callback;
        FirstTimeLoginActivity.socket               = socket;
        FirstTimeLoginActivity.sharedPreferences    = sharedPreferences;
        FirstTimeLoginActivity.myEditor             = sharedPreferences.edit();;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //title bar text
        setTitle(R.string.first_time_login);
        setContentView(R.layout.activity_first_time_login);

        //Get Retrofit client
        userService     = LoginActivity.userService; //ApiUtils.getUserService();

        username        = (EditText) findViewById(R.id.textInputEditText_identifier);
        username_       = (TextInputLayout)findViewById(R.id.outlinedTextField_identifier);

        password1_      = (TextInputLayout)findViewById(R.id.outlinedTextField_pwd1);
        password1       = (EditText) findViewById(R.id.textInputEditText_pwd1);

        password2_      = (TextInputLayout)findViewById(R.id.outlinedTextField_pwd2);
        password2       = (EditText) findViewById(R.id.textInputEditText_pwd2);

        button          = (Button)   findViewById(R.id.Btn_Validation);
        quit            = (Button)   findViewById(R.id.Btn_quit);

        registerStatus  = (TextView) findViewById(R.id.tv_register_status);

        socket.connect();

        Bundle extras   = getIntent().getExtras();
        if(extras != null){ //we come from 'ChangePasswordActivity', there is an extra
            String username_ = extras.getString("username");
            username.setText(username_);
        }

        button.setText(VALIDER_USER);

        //statusUsername.setText(null);
        //statusPwd.setText(null);
        //statusRegistration.setText(null);

        //password1.setEnabled(false);
        //password2.setEnabled(false);
        //password2.setText("GetHeight");

        //labelPwd1.measure(0, 0);
        //labelPwd1Height = labelPwd1.getMeasuredHeight(); //get heigh

        //password1.measure(0, 0);
        //password1height = password1.getMeasuredHeight(); //get height

        //labelPwd2.measure(0, 0);
        //labelPwd2Height = labelPwd2.getMeasuredHeight(); //get heigh

       // password2.measure(0, 0);
        //password2height = password2.getMeasuredHeight(); //get height

        //statusPwd.measure(0, 0);
        //statusPwdHeight = statusPwd.getMeasuredHeight(); //get height

        //labelPwd1.setHeight(0);
        //password1.setHeight(0);
        //labelPwd2.setHeight(0);
        //password2.setHeight(0);
        //statusPwd.setHeight(0);

        //password1.setEnabled(false);
        //password2.setEnabled(false);


        //password1.setVisibility(View.INVISIBLE);
        //password2.setVisibility(View.INVISIBLE);

        //password1.setText(null);
        //password2.setText(null);

        //pwdInformation.setVisibility(View.INVISIBLE);
        //labelPwd1.setVisibility(View.INVISIBLE);
        //labelPwd2.setVisibility(View.INVISIBLE);

        //statusPwd.setVisibility(View.INVISIBLE);
        //statusPwd.setText(null);
        //statusPwd.setHeight(0);

        button.setEnabled(false);
        password1_.setEnabled(false);
        password2_.setEnabled(false);

        //txtUsername.setText("Welcome ");
        //if(extras != null){
        //    username = extras.getString("username");
        //    txtUsername.setText("Welcome " + username);
        //}
    }//end onCreate



    //@Override
    public void onStart() {
        super.onStart();

        /*
        //username information
        userInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //usernameInfo clicked popup information.
                new AlertDialog.Builder(FirstTimeLoginActivity.this).
                        setMessage(R.string.userInfo).
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        }).create().show();
            }
        });
        */

        /*
        //password information
        pwdInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pwdInfo clicked popup information.
                new AlertDialog.Builder(FirstTimeLoginActivity.this).
                        setMessage(R.string.pwdInfo).
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //finish();
                            }
                        }).create().show();
            }
        });
        */

        //Username focus
        username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus) {
                    //show the keyboard
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);//ne marche pas

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(username, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        password1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                }
            }
        });

        password2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {}});

        /*
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //finishAffinity(); // même chose que finish();
                finish();
                System.exit(0);
            }
        });
        */

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String label = ((Button) v).getText().toString();
                switch (label){
                    case VALIDER_USER :
                        //We come here when the syntax is ok. The syntax check is done in 'OnTextChange'.
                        //if the validation is ok (the syntax is ok and the username is not found
                        // in the database) then the access to password field is allowed.
                        checkUsername(username);
                        break;
                    case VALIDER_PWD :
                        //if the validation is ok, the access to register button is allowed.
                        checkPwd(password1, password2);
                        break;
                    case REGISTER:
                        register(username, password1);
                        break;
                    case SUIVANT :
                        gotoNext(username);
                        break;
                    default:
                        String s = "Invalid label.";
                        break;
                }

                //checkUsername(username);
                //true, the user is unique.
                //if(!checku)return;
                //boolean checkp = checkPwd(password1, password2);
                //if(!checkp)return;
                //register(username.getText().toString(), password1.getText().toString());
            }
        });

        username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                }else{ }
            }
        });

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                //Snackbar.make(FirstTimeLoginActivity.this.findViewById(android.R.id.content),
                //        " username modified", Snackbar.LENGTH_LONG).show();

                //reset pwd edt
                if(password1.length() != 0){
                    password1.setText(null, null); //clear password1.
                    password1_.setEnabled(false);
                    password1_.setErrorEnabled(false);
                    password1_.setHelperTextEnabled(false);
                }
                if(password2.length() != 0){
                    password2.setText(null, null); //clear password2.
                    password2_.setEnabled(false);
                    password2_.setErrorEnabled(false);
                    password2_.setHelperTextEnabled(false);
                }

                button.setText(VALIDER_USER);
                button.setEnabled(false);

                username_.setErrorEnabled(false);
                username_.setError("The identifier is not valid");
                username_.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                username_.setEndIconDrawable(getResources().getDrawable(R.drawable.error_transparent));
                username_.setEndIconActivated(true);


                //if((s.length() >= 6) && (s.length() <= 8)){
                //    username_.setEndIconActivated(false);
                //    username_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                //}

                String usernamePattern = "[a-zA-Z0-9]{6,8}";
                if(s.toString().matches(usernamePattern)){
                    button.setEnabled(true);
                    username_.setErrorEnabled(false);
                    username_.setHelperText("The identifier is valid.");
                    username_.setHelperTextColor(ColorStateList.valueOf(Color.GREEN));
                    username_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                    //included in pattern
                    //if(s.length() == 0){
                    //    username_.setError("No input, identifier required");
                    //}
                    //if(s.length() > 8){
                    //username_.setError("The length identifier exceeds the limit");
                    //}
                    return ;
                }


                /*
                password1.setText(null, null); //clear password
                password2.setText(null, null); //clear password
                password1.setEnabled(false);
                password2.setEnabled(false);
                statusUsername.setText(null);
                statusPwd.setText(null);
                statusRegistration.setTextColor(Color.MAGENTA);
                statusRegistration.setText("En cours ...");
                */
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        password1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                int i = start;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                password2.setText(null, null); //clear password.
                password2_.setErrorEnabled(false);
                password2_.setHelperTextEnabled(false);
                password2_.setEnabled(true);

                password1_.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                password1_.setErrorIconDrawable(null);

                button.setText(REGISTER);
                button.setEnabled(false);

                password1_.setHelperTextColor(ColorStateList.valueOf(Color.GREEN));
                password1_.setHelperText("The password is valid");
                password1_.setBoxStrokeColor(Color.BLACK);

                //password1_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                //password1_.setEndIconActivated(false);
                //password1_.setErrorEnabled(false);

                //if((s.length() >= 6) && (s.length() <= 8)){
                //    username_.setEndIconActivated(false);
                //    username_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                //}

                String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%&*-]).{6,8}";

                if(!password1.getText().toString().matches(passwordPattern)){
                    password2_.setEnabled(false);

                    password1_.setBoxStrokeColor(Color.RED);
                    password1_.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                    password1_.setErrorEnabled(true);
                    password1_.setError("The password is not valid");

                    if(s.length() == 0){
                        password1_.setError("no input, valid password required");
                    }
                    if(s.length() > 8){
                        password1_.setError("The length password exceeds the limit");
                    }
                    return ;
                }
                if(password1.length() == password2.length()){
                    password2_.setHelperTextEnabled(true);
                    password2_.setHelperText("Passwords are equals");
                    button.setEnabled(true);
                    if(!password1.getText().toString().equals(password2.getText().toString())){
                        button.setEnabled(false);
                        password2_.setHelperTextEnabled(true);
                        password2_.setHelperText("Passwords are not equals");
                        return;
                    }
                    //hide the keyboard
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                }

            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        password2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                password2_.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                password2_.setErrorIconDrawable(null);
                password2_.setBoxStrokeColor(Color.BLACK);

                button.setEnabled(false);
                password1_.setErrorIconDrawable(null);

                password2_.setHelperTextColor(ColorStateList.valueOf(Color.GREEN));
                password2_.setHelperText("The password is valid");

                //password2_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                //password2_.setEndIconActivated(false);
                //password2_.setErrorEnabled(false);

                //if((s.length() >= 6) && (s.length() <= 8)){
                //    username_.setEndIconActivated(false);
                //    username_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));
                //}

                String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{6,8}";

                if(!password2.getText().toString().matches(passwordPattern)){
                    password2_.setErrorTextColor(ColorStateList.valueOf(Color.RED));
                    password2_.setBoxStrokeColor(Color.RED);
                    password2_.setErrorEnabled(true);
                    password2_.setError("The password is not valid");

                    if(s.length() == 0){
                        password2_.setError("No input, valid password required");
                    }
                    if(s.length() > 8){
                        password2_.setError("The length password exceeds the limit");
                    }
                    return ;
                }

                if(password1.length() == password2.length()){
                    password2_.setHelperTextEnabled(true);
                    password2_.setHelperText("Passwords are equals");
                    button.setEnabled(true);
                    if(!password1.getText().toString().equals(password2.getText().toString())){
                        button.setEnabled(false);
                        password2_.setHelperTextEnabled(true);
                        password2_.setError("Passwords are not equals");
                        return;
                    }
                    //hide the keyboard
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        //Quit
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //button 'Quit' clicked
                //returned to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
                endActivity("fail");
            }
        });
    }

    /**
     * To validate  password : it is not null, lenght != 0 and contains legal characters.
     * @param password the supplied password to validate.
     * @return boolean the supplied identifiers are valid or not.
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

    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void gotoNext(EditText user) {
        String username = user.getText().toString();
        //Redirect
        postLogin( username);
    }

    private void checkUsername(EditText username) {

        //Here, the username supplied is valid. Check if it is found in server database.
        checkIsUserRegisteredRemotely(username.getText().toString());
    }

    private void isUsernameUnique(final String username, boolean userExists) {
        //Local check. If there is already a user with this username.
        if(userExists){

            //Error ; "Only the original thread that created a view hierarchy can touch its views.". workaround

            int color = Color.RED;
            String status = "The user : " + username + " already exists.";
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  //The user exists in db,
                  username_.setErrorEnabled(true);
                  username_.setError("'" + username + "' already exists.");
                  username_.setEndIconDrawable(getResources().getDrawable(R.drawable.error_transparent));
                  button.setEnabled(false);
              }
            });
            return;
        }

        //Here the username is unique.update the status of username

        //workaround : Error : "Only the original thread that created a view hierarchy can touch its views.".
        runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  //The user is unique in remote db,
                  username_.setErrorEnabled(false);
                  username_.setHelperTextEnabled(true);
                  username_.setHelperText("'" + username + "' is accepted.");
                  username_.setEndIconDrawable(getResources().getDrawable(R.drawable.check_transparent));

                  //change the name of button
                  button.setText(REGISTER);
                  button.setEnabled(false);
                  password1_.setEnabled(true);
                  password2_.setEnabled(false);
                  button.setEnabled(false);
              }
        });
    }

    //Retrofit, not used
    private void remoteCheckUsername(final String username) {

        //Remote register
        Socket socket = LoginActivity.socket;

        final Call<ResponseBody> call = userService.checkUser(username);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                //there is a response :
                //'notfound' no user with this username found.
                //'found' a user with this username is found.
                if(response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        boolean userFound = (body.equals("notfound")) ? false : true; //'notfound' = false, no user found in database.

                        //update the status
                        int color = userFound ? Color.RED : Color.GREEN;
                        String status = userFound ? "L'utilisatur '"+username+"' existe déjà." : "OK";

                        statusUsername.setTextColor(color);
                        statusUsername.setText(status);

                        statusRegistration.setTextColor(color);
                        statusRegistration.setText(status);

                        //Enable pwd field.
                        if(!userFound) {
                            button.setText(VALIDER_PWD);

                            labelPwd1.setHeight(labelPwd1Height);
                            password1.setHeight(password1height);
                            labelPwd2.setHeight(labelPwd2Height);
                            password2.setHeight(password2height);
                            statusPwd.setHeight(statusPwdHeight);

                            password1.setVisibility(View.VISIBLE);
                            password2.setVisibility(View.VISIBLE);

                            pwdInformation.setVisibility(View.VISIBLE);
                            labelPwd1.setVisibility(View.VISIBLE);
                            labelPwd2.setVisibility(View.VISIBLE);
                            statusPwd.setVisibility(View.VISIBLE);

                            password1.setEnabled(true);
                        }
                        return;

                    } catch (IOException e) {
                        e.printStackTrace();
                        e.getMessage();
                        statusUsername.setTextColor(Color.RED);
                        statusUsername.setText("Erreur vérification.");

                        statusRegistration.setTextColor(Color.RED);
                        statusRegistration.setText("Erreur vérification.");
                        return;
                    }
                    //Save the state of 'rememberMe' of the checkBox
                    //myEditor.putBoolean("rememberMe", rememberMe);
                    //myEditor.commit();
                    // intent
                    //login start main activity
                    //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username", username);
                    //startActivity(intent);
                    //postLogin(LoginActivity.this);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                t.getMessage();
                statusUsername.setTextColor(Color.RED);
                statusUsername.setText("Erreur vérification.");

                statusRegistration.setTextColor(Color.RED);
                statusRegistration.setText("Erreur vérification.");
                return;
            }
        });
    }

    /**
     * Save the credentials in local and remote database
     * @param user Editext of username
     * @param pwd Editext of password
     */
    private void register(EditText user, EditText pwd) {

        //save in local sqlite database
        String date = String.valueOf(System.currentTimeMillis());
        int row = localRegister(user.getText().toString(), pwd.getText().toString(), date);

        //save in server. Async operation
        remoteRegister(user.getText().toString(), pwd.getText().toString(), date);

        /*
        //update status
        int statusColor      = (row == 1) && (register) ? Color.GREEN : Color.RED;
        String StatusMessage = (row == 1) && (register) ? "Registration successfull" : "Registration failure";

        registerStatus.setTextColor(statusColor);
        registerStatus.setText(StatusMessage);

        //disable pwd field editing
        password1.setEnabled(false);
        password2.setEnabled(false);

        //redirect the freshly registered user to chat
        button.setText(SUIVANT);
        */

        //do nothing
        //boolean remoteRegister = false;
        //if(remoteRegister)remoteRegister(user.getText().toString(), pwd.getText().toString());
    }

    /**
     * Save the user credentials in sqlite db. Insert or replace. see : 'FileDbContentProvider'
     * @param username the supplied username to save
     * @param password the supplied password to save
     * @return number of inserted or updated rows.
     */
    private int localRegister(String username, String password, String date) {

            //at first time only when signing
            String dateHistory = date;
            String pwdHistory  = password;

            ContentValues values = new ContentValues();
            values.put(CredentialsContract.COLUMN_USERNAME,     username);
            values.put(CredentialsContract.COLUMN_PWD,          password);
            values.put(CredentialsContract.COLUMN_DATE,         date);
            values.put(CredentialsContract.COLUMN_DATE_HISTORY, dateHistory);
            values.put(CredentialsContract.COLUMN_PWD_HISTORY,  pwdHistory);


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
            if(newRowUri == null) throw new UnsupportedOperationException("FirstTimeActivity. insert : Unexpected URI value = null");
            numRows = 1;

            return numRows;
        }

    //redirect to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
    //we come here after button 'quit' is pressed with status='fail'
    // when the registration is successfull the data are sent to 'NavigatorActivity' via 'registerUser' interface
    private void endActivity(String status) {

        // Prepare data intent
        Intent intent = new Intent();
        //String username_ = (status.equals("success")) ? username.getText().toString() : "";
        intent.putExtra("status", status);

        // Activity finished ok, return the data to 'onActivityResult' of 'LoginActivity'
        // in 'Authentication' module which launched this intent.
        setResult(RESULT_CODE_REGISTER, intent); //the data are returned to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
        finish();//obligatoire
    }

    //when the 'back' button is pressed, we come here.
    @Override
    public void onBackPressed(){
        endActivity("fail");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkIsUserRegisteredRemotely(String user) {
        //if the name entered in 'nickname' edit text already exists ?
        //As the user is new or he deleted the app, the local db is also deleted.
        //then ask the server if the user exists or not.

        final boolean[] userExist = {false};
        socket.emit("is_user_exists", user, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject userExists = ((JSONObject) args[0]);

                try {
                    userExist[0] = userExists.getBoolean("exists");
                    isUsernameUnique(user, userExist[0]);
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Register credentials in db
    //return "true' or "false"
    private void remoteRegister(String user, String pwd, String date) {

        final boolean[] userRegister = {false};
        socket.emit("register_user_in_db", user, pwd, date, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject userRegistered = ((JSONObject) args[0]);

                try {
                    userRegister[0] = userRegistered.getBoolean("insert");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Error : Only the original thread that created a view hierarchy can touch its views.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(userRegister[0]){
                            //put information in preferences
                            //myEditor.putString("NICKNAME", user);
                            //myEditor.putLong("connectTime", new Date().getTime());

                            //notify the user in 'sign-in' form
                            registerStatus.setVisibility(View.VISIBLE);
                            registerStatus.setTextColor(Color.GREEN);
                            registerStatus.setText("Registration success");

                            //System.out.println("remoteRegister in FirstTimeActivity send username to MainActivity of app module  user = " + user + " userRegister[0] = " + userRegister[0] );

                            //show the next step
                            username_.setEnabled(false);
                            password1_.setEnabled(false);
                            password2_.setEnabled(false);
                            button.setText(SUIVANT);

                        }else{
                            //ToDo : the user is not succesfully registered, notify the user
                            //ToDo : let him try 3 time
                            //notify the user in 'sign-in' form
                            registerStatus.setVisibility(View.VISIBLE);
                            registerStatus.setTextColor(Color.RED);
                            registerStatus.setText("Registration failure");
                        }
                    }
                });
            }
        });
        //return userRegister[0];
    }


    private void remoteRegister_(EditText user, EditText pwd) {

        //Remote register
        Socket socket = LoginActivity.socket;

        final String username  = user.getText().toString();
        final String password  = pwd.getText().toString();

        final String password64 = android.util.Base64.encodeToString(password.getBytes(), android.util.Base64.DEFAULT);

        final Call<ResponseBody> call = userService.registerUser(new Book(username, password64));

        //Call call = userService.login(username, password);
        //call = userService.valideAutorization("", "azerty");

        call.enqueue(new Callback<ResponseBody>() {
            String status;
            int color;

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {


                if(response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        //System.out.println("body = "+body);
                        //System.out.println("Headers = "+response.headers());

                        boolean registrationUserStatus = body.equals("success") ? true : false;

                        //update textview status
                        status = registrationUserStatus ? "Success." : "Echec : Erreur enregistrement.";
                        color  = registrationUserStatus ? Color.GREEN : Color.RED;
                        statusRegistration.setTextColor(color);
                        statusRegistration.setText(status);

                        //lock
                        if(registrationUserStatus){
                            //L'écriture suivante est du au fait que la variable 'username' est une variable locale
                            //dans l'argument de la méthode 'register'.
                            //Il aurait fallu changer le nom dans la méthode create() et faire :
                            // username1  = (EditText) findViewById(R.id.edtUsername);
                            //
                            FirstTimeLoginActivity.this.username.setEnabled(false);
                            password1.setEnabled(false);
                            password2.setEnabled(false);
                            button.setText(SUIVANT);

                            //Save the login identifier in preference
                            //Get the sharedPreferences and editor
                            LoginActivity.myEditor.putBoolean("firstTime", false);
                            LoginActivity.myEditor.putString("username", username);
                            LoginActivity.myEditor.putString("password", password); //x64
                            LoginActivity.myEditor.commit();

                            //Redirect
                            //postLogin(FirstTimeLoginActivity.this, username);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        e.getMessage();
                        color = Color.RED;
                        status = "Fail : Error server.";
                        FirstTimeLoginActivity.this.statusRegistration.setText(status);
                        return;
                    }

                    //Save the state of 'rememberMe' of the checkBox
                    //myEditor.putBoolean("rememberMe", rememberMe);
                    //myEditor.commit();
                    // intent
                    //login start main activity
                    //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username", username);
                    //startActivity(intent);
                    //postLogin(LoginActivity.this);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                t.getMessage();
                color  = Color.RED;
                status = "Fail : Error server.";
                FirstTimeLoginActivity.this.statusRegistration.setText(status);
                return;
            }
        });
    }

    private void postLogin(String username) {
        //The user is registered successfully send information to
        // 'registerUser' in 'NavigatorActivity' of 'app' module.
        callback.registerUser(username);

        //end this activity and return to the 'LoginActivity' of 'Authenticate' module which launched this intent.
        endActivity("success");
    }

    /**
     * Conformité du mot de passe syntaxe.
     * @param password1 pwd du champs 1
     * @param password2 pwd du champs 2
     */
    private void checkPwd(EditText password1, EditText password2) {
        if((password1.length() == 0) || (password2.length() == 0)){
            statusPwd.setTextColor(Color.RED);
            statusPwd.setText("Champs vide.");
            return;
        }
        if(password1.getText().length() != password2.getText().length()){
            statusPwd.setTextColor(Color.RED);
            statusPwd.setText("Les deux mots de passe ne sont pas de même longueur.");
            return;
        }
        if(!password1.getText().toString().equals(password2.getText().toString())){
            statusPwd.setTextColor(Color.RED);
            statusPwd.setText("Les deux mots de passe ne correspondent pas.");
            return;
        }

        //String passwordPattern = "[a-zA-Z0-9#@]{6,8}";
        String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{6,8}";

        if(!password1.getText().toString().matches(passwordPattern) || !password2.getText().toString().matches(passwordPattern)){
            statusPwd.setTextColor(Color.RED);
            statusPwd.setText("Caractères non valides.");
            return ;
        }

        statusPwd.setTextColor(Color.GREEN);
        statusPwd.setText("OK.");

        //hide the keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);//ne marche pas
        button.setText(REGISTER);
        return;
    }
}