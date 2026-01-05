package com.google.amara.authenticationretrofit;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aymen.androidchat.sql.CredentialsContract;
import com.google.amara.authenticationretrofit.remote.UserService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import io.socket.client.Ack;
import io.socket.client.Socket;
import okhttp3.Credentials;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final long BAN_TIME  = 3600000; //1 hour

    private final String REPEAT         = "Recommencer";
    private final String SUIVANT        = "Suivant";
    private final String VALIDER_USER   = "Valider nom utilisateur";
    private final String VALIDER_PWD    = "Valider mot de passe";
    private final String VALIDER_NEW_PWD= "Valider \nle nouveau mot de passe";
    private final String REGISTER       = "Enregistrer les identifiants";
    private final String SIGNUP         = "S'incrire";
    private final String REGISTER_NEW_PWD = "Enregistrer \nle nouveau mot de passe";

    public EditText     username, password1, password2, password3;
    private Button      button, quitter;
    private TextView    statusUsername;
    private TextView    statusPwd1, statusPwd3;
    public TextView     statusRegistration;
    private TextView    labelPwd1, labelPwd2, labelPwd3;
    private TextView    tryCounter;

    private UserService userService;

    private int labelPwd1Height, labelPwd2Height, labelPwd3Height,
                password1height, password2height, password3height,
                statusPwd1Height, statusPwd3Height;

    private int numberTry;
    private int nbTry;   //used in validate username in 'checkUsername'
    private final int MAX_TRY = 3;
    private static final int NUMBER_TRY = 3;

    private static final int RESULT_CODE_RESET_PWD  = 901;

    final String MYPREFS = "MyPreferences_Login";
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor myEditor;

    public interface Callback__{
        void resetPwd(String username);
    }
    private static Callback__ callback;
    private static Socket socket;

    public static void setCallback(Socket socket, Callback__ callback, SharedPreferences sharedPreferences){
        ChangePasswordActivity.callback             = callback;
        ChangePasswordActivity.socket               = socket;
        ChangePasswordActivity.sharedPreferences    = sharedPreferences;
        ChangePasswordActivity.myEditor             = sharedPreferences.edit();;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //title bar text
        setTitle(R.string.change_password);
        setContentView(R.layout.activity_change_password);

        //Get Retrofit client
        //userService = LoginActivity.userService; //ApiUtils.getUserService();

        username  = (EditText) findViewById(R.id.edtUsername);
        password1 = (EditText) findViewById(R.id.edtPassword1);
        password2 = (EditText) findViewById(R.id.edtPassword2);
        password3 = (EditText) findViewById(R.id.edtPassword3);
        button    = (Button)   findViewById(R.id.edtButton);
        quitter   = (Button)   findViewById(R.id.edtQuitter);

        statusUsername      = (TextView) findViewById(R.id.status_username);
        statusPwd1          = (TextView) findViewById(R.id.status_pwd1);
        statusPwd3          = (TextView) findViewById(R.id.status_pwd3);
        statusRegistration  = (TextView) findViewById(R.id.status_registration);
        tryCounter          = (TextView) findViewById(R.id.tv_Counter);

        labelPwd1           = (TextView) findViewById(R.id.label_pwd1);
        labelPwd2           = (TextView) findViewById(R.id.label_pwd2);
        labelPwd3           = (TextView) findViewById(R.id.label_pwd3);

        //check ban info in preferences is done when the button is clicked.

        //quitter.setVisibility(View.INVISIBLE);

        button.setText(VALIDER_USER);
        //statusUsername.setText("null");
        //statusPwd.setText(null);
        statusRegistration.setText(null);
        tryCounter.setVisibility(View.INVISIBLE);

        //password1.setEnabled(false);
        //password2.setEnabled(false);
        //password2.setText("GetHeight");

        //Get the height of items.
        labelPwd1.measure(0, 0);
        labelPwd1Height = labelPwd1.getMeasuredHeight(); //get height

        password1.measure(0, 0);
        password1height = password1.getMeasuredHeight(); //get height

        statusPwd1.measure(0, 0);
        statusPwd1Height = statusPwd1.getMeasuredHeight(); //get height

        //

        labelPwd2.measure(0, 0);
        labelPwd2Height = labelPwd2.getMeasuredHeight(); //get heigh

        password2.measure(0, 0);
        password2height = password2.getMeasuredHeight(); //get height

        //

        labelPwd3.measure(0, 0);
        labelPwd3Height = labelPwd3.getMeasuredHeight(); //get heigh

        password3.measure(0, 0);
        password3height = password3.getMeasuredHeight(); //get height

        statusPwd3.measure(0, 0);
        statusPwd3Height = statusPwd3.getMeasuredHeight(); //get height

        //

        labelPwd1.setHeight(0);
        password1.setHeight(0);
        statusPwd1.setHeight(0);
        labelPwd1.setVisibility(View.INVISIBLE);
        password1.setVisibility(View.INVISIBLE);
        statusPwd1.setVisibility(View.INVISIBLE);
        password1.setText(null);

        labelPwd2.setHeight(0);
        password2.setHeight(0);
        labelPwd2.setVisibility(View.INVISIBLE);
        password2.setVisibility(View.INVISIBLE);
        password2.setText(null);

        labelPwd3.setHeight(0);
        password3.setHeight(0);
        statusPwd3.setHeight(0);
        labelPwd3.setVisibility(View.INVISIBLE);
        password3.setVisibility(View.INVISIBLE);
        statusPwd3.setVisibility(View.INVISIBLE);
        password3.setText(null);

        button.setEnabled(false);

        //fill the username since we come here after connection and the username and his password are known.
        //we can go right to new pwd.
        Bundle extras = getIntent().getExtras();
        String NICKNAME = extras.getString("NICKNAME", null);
        if(null != NICKNAME){
            username.setText(NICKNAME);
            username.setEnabled(false);
            statusUsername.setTextColor(Color.GREEN);
            statusUsername.setText("OK");

            password1.requestFocus();
            button.setText(VALIDER_PWD);

            //show the password view
            labelPwd1.setHeight(labelPwd1Height);
            password1.setHeight(password1height);
            statusPwd1.setHeight(statusPwd1Height);

            labelPwd1.setVisibility(View.VISIBLE);
            password1.setVisibility(View.VISIBLE);
            statusPwd1.setVisibility(View.VISIBLE);

            password1.setEnabled(true);
            password1.requestFocus();
            button.setEnabled(false);
        }

        //txtUsername.setText("Welcome ");
        //if(extras != null){
        //    username = extras.getString("username");
        //    txtUsername.setText("Welcome " + username);
        //}
    }//end onCreate

    /**
     * Called when a ban occur.
     *
     * @param startBanTime the time when the ban starts. It is set after 3 unsuccessful attempts to login.
     */
    private void banish(long startBanTime) {
        //Notify the user how many time it remains to have free access.
        long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
        long minutes       = remainingTime / 60000;
        long secondes      = (remainingTime % 60000);
        int secondes_      = (int) (secondes / 1000);

        new AlertDialog.Builder(this).
                setMessage("It remains : " + minutes + " minutes et " + secondes_ + " secondes to log").
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        endActivity("fail");
                    }
                }).create().show();
        return;
    }

    //@Override
    public void onStart_(){
        super.onStart();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //System.out.println("button clicked = "+((Button) v).getText().toString());
                String label = ((Button) v).getText().toString();
                switch (label){
                    case VALIDER_USER :
                        checkUsername(username.getText().toString());
                        break;
                    case VALIDER_PWD :
                        checkPwd(username, password1);
                        break;
                    case REGISTER :
                        register(username, password2, System.currentTimeMillis());
                        break;
                    case VALIDER_NEW_PWD :
                        checkNewPwd(password2, password3);
                        break;
                    case REGISTER_NEW_PWD:
                        locallyRegisterNewCredential(username, password2);
                        //remoteRegisterNewCredential(username, password2);
                        break;
                    case SUIVANT :
                        gotoNext(username);
                        break;
                    case REPEAT :
                        numberTry++;
                        password1.setEnabled(true);
                        password1.setText(null);
                        statusPwd1.setText(null);
                        password1.requestFocus();
                        quitter.setVisibility(View.INVISIBLE);
                        button.setText(VALIDER_PWD);
                        break;
                    case SIGNUP :
                        gotoSignup(getApplicationContext(), username);
                        break;
                    default:
                        String s = "Invalid action.";
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

        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                labelPwd1.setHeight(0);
                password1.setHeight(0);
                statusPwd1.setHeight(0);
                labelPwd1.setVisibility(View.INVISIBLE);
                password1.setVisibility(View.INVISIBLE);
                statusPwd1.setVisibility(View.INVISIBLE);
                password1.setText(null);    //L'évènement 'addTextChangedListener' de password1 réagit et bloque le button 'Valider nom utilisateur'
                password2.setText(null);

                labelPwd2.setHeight(0);
                password2.setHeight(0);
                labelPwd2.setVisibility(View.INVISIBLE);
                password2.setVisibility(View.INVISIBLE);
                password2.setText(null);    //L'évènement 'addTextChangedListener' de password2 réagit et bloque le button 'Valider nom utilisateur

                labelPwd3.setHeight(0);
                password3.setHeight(0);
                statusPwd3.setHeight(0);
                labelPwd3.setVisibility(View.INVISIBLE);
                password3.setVisibility(View.INVISIBLE);
                statusPwd3.setVisibility(View.INVISIBLE);

                //statusRegistration.setTextColor(Color.MAGENTA);
                //statusRegistration.setText("En cours ...");
                statusUsername.setText(null);//

                button.setText(VALIDER_USER);
                button.setEnabled(true);

                if(s.length() == 0){
                    button.setEnabled(false);
                    statusRegistration.setText(null);

                    //statusUsername.setText(null);
                }

                return;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        numberTry = 0;
        /*
        //Quit button
        quitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ChangePasswordActivity.this.finishAffinity(); //
                    System.exit(0);
                }else{
                    finish();
                    System.exit(0);
                }
            }
        });
        */

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //System.out.println("button clicked = "+((Button) v).getText().toString());
                String label = ((Button) v).getText().toString();
                switch (label){
                    case VALIDER_USER :
                        checkUsername(username.getText().toString());
                        break;
                    case VALIDER_PWD :
                        checkPwd(username, password1);
                        break;
                    case REGISTER :
                        register(username, password2, System.currentTimeMillis());
                        statusPwd3.setText(null);
                        break;
                    case VALIDER_NEW_PWD :
                        checkNewPwd(password2, password3);
                        break;
                    case REGISTER_NEW_PWD:
                        locallyRegisterNewCredential(username, password2);
                        //remoteRegisterNewCredential(username, password2);
                        break;
                    case SUIVANT :
                        gotoNext(username);
                        break;
                    case REPEAT :
                        numberTry++;
                        password1.setEnabled(true);
                        password1.setText(null);
                        password1.requestFocus();
                        statusPwd1.setText(null);
                        quitter.setVisibility(View.INVISIBLE);
                        button.setText(VALIDER_PWD);
                        break;
                    case SIGNUP :
                        gotoSignup(getApplicationContext(), username);
                        break;
                    default:
                        String s = "Invalid action.";
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

        //username
        username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //show the keyboard
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                labelPwd1.setHeight(0);
                password1.setHeight(0);
                statusPwd1.setHeight(0);
                labelPwd1.setVisibility(View.INVISIBLE);
                password1.setVisibility(View.INVISIBLE);
                statusPwd1.setVisibility(View.INVISIBLE);
                password1.setText(null);    //L'évènement 'addTextChangedListener' de password1 réagit et bloque le button 'Valider nom utilisateur'
                password2.setText(null);

                labelPwd2.setHeight(0);
                password2.setHeight(0);
                labelPwd2.setVisibility(View.INVISIBLE);
                password2.setVisibility(View.INVISIBLE);
                password2.setText(null);    //L'évènement 'addTextChangedListener' de password2 réagit et bloque le button 'Valider nom utilisateur

                labelPwd3.setHeight(0);
                password3.setHeight(0);
                statusPwd3.setHeight(0);
                labelPwd3.setVisibility(View.INVISIBLE);
                password3.setVisibility(View.INVISIBLE);
                statusPwd3.setVisibility(View.INVISIBLE);

                //statusRegistration.setTextColor(Color.MAGENTA);
                //statusRegistration.setText("En cours ...");
                statusUsername.setText(null);

                button.setText(VALIDER_USER);
                button.setEnabled(true);

                if(s.length() == 0){
                    button.setEnabled(false);
                    //statusRegistration.setText(null);

                    statusUsername.setText(null);
                }

                return;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        //Password1 is the current password
        password1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //Show keyboard
                if (hasFocus) {
                    //show the keyboard
                    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    //WindowCompat.getInsetsController(getWindow(), password1).show(WindowInsetsCompat.Type.ime());

                    /*
                    password1.requestFocus();
                    password1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(password1, 0); //InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 300);
                    */

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    //imm.showSoftInput(password1, InputMethodManager.SHOW_IMPLICIT);  //SHOW_IMPLICIT); //SHOW_FORCED
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 1);
                }
            }
        });

        password1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                statusPwd1.setText(null);
                button.setText(VALIDER_PWD);
                button.setEnabled(false);
                boolean password1Status = validatePwd(s.toString());
                if(password1Status){
                    button.setEnabled(true);
                }

                //if (s.length() == 0) button.setEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        //password2
        password2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                //Show keyboard
                if (hasFocus) {
                    //show the keyboard
                    //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(password2, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        //the first new pwd is 'password2'
        password2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                statusPwd3.setText(null);
                button.setText(VALIDER_NEW_PWD);
                button.setEnabled(false); //it becomes 'true' if the new password is not already set.

                boolean password2Status = validatePwd(s.toString());
                if(password2Status){
                    password3.setEnabled(true);
                }

                //whatever we do the button is not enabled when input is added in 'password2'.
                // the 'password3' is enabled only when there is input in 'password2'
                //button.setEnabled(false);
                //button.setText(VALIDER_NEW_PWD);

                //this case is treated in 'validatePwd'
                //if(s.length() == 0){
                //    password3.setEnabled(false);
                //   password3.setText(null);
                //    statusPwd3.setText(null);
                //}
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        //Password3 the verification pwd
        password3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                button.setVisibility(View.VISIBLE);
                button.setEnabled(false);
                boolean password3Status = validatePwd(s.toString());
                if(password3Status){
                    button.setEnabled(true);
                }
                //this case is treated in 'validatePwd'
                //if(s.length() == 0){
                //    button.setEnabled(false);
                //    button.setVisibility(View.INVISIBLE);
                //    statusPwd3.setText(null);
                //}
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        //Quit
        quitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //button 'Quit' clicked
                //return to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
                endActivity("fail");
            }
        });
    }//end onStart

    //return to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
    private void endActivity(String status) {

        // Prepare data intent
        Intent intent = new Intent();
        //String username_ = (status.equals("success")) ? username.getText().toString() : "";
        intent.putExtra("status", status);

        // Activity finished ok, return the data to 'onActivityResult' of 'LoginActivity'
        // in 'Authentication' module which launched this intent.
        setResult(RESULT_CODE_RESET_PWD, intent); //the data are returned to 'onActivityResult' of 'LoginActivity' in 'Authentication' module which launched this intent.
        finish();//obligatoire
    }

    //when the 'back' button is pressed, we come here.
    @Override
    public void onBackPressed(){
        endActivity("fail");
    }

    @Override
    public void onRestart() {
        super.onRestart();
    }
    @Override
    public void onResume() {
        super.onResume();
    }


    private void gotoSignup(Context context, EditText username) {
        //notify the user that it will be direct to signup

        new AlertDialog.Builder(ChangePasswordActivity.this)
                .setMessage("Your will be direct to signup")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //startHandler();
                        //Redirect to 'FirstTimeLoginAactivty'
                        Intent intent = new Intent(context, FirstTimeLoginActivity.class);
                        intent.putExtra("username", username.getText().toString());
                        startActivity(intent);

                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        endActivity("fail");
                    }
                })
                .create()
                .show();
    }

    /**
     * Before to register locally in sqlite db and to avoid conflict :
     * 1st : get the user if any. If the user exist, do insert.
     * 2nd : update if the user already exist
     * @param user : the username to register.
     * @param newPassword : the password to register.
     * @return the number of rows inserted in db. It would be 1
     */
    private int locallyRegisterNewCredential(final EditText user, EditText newPassword) {


        String username = user.getText().toString();
        String password = newPassword.getText().toString();

        String date = String.valueOf(System.currentTimeMillis());

        JSONObject dateHistory_ = new JSONObject();
        try {
            dateHistory_.put("date_history", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject pwdHistory_ = new JSONObject();
        try {
            pwdHistory_.put("pwd_history", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //1st : get user with this 'username' if any.
        String[] mProjection = new String[] {
                //Credentials.COLUMN_ID,
                CredentialsContract.COLUMN_USERNAME,
                CredentialsContract.COLUMN_DATE_HISTORY,
                CredentialsContract.COLUMN_PWD_HISTORY
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                CredentialsContract.CONTENT_URI_CREDENTIALS,
                mProjection,
                CredentialsContract.COLUMN_USERNAME  + " =? ",

                new String[]{username},

                null
        );
        if(cursor == null || cursor.getCount() == 0) {
            //user not found then do insert.
            ContentValues values = new ContentValues();
            values.put(CredentialsContract.COLUMN_USERNAME, username);
            values.put(CredentialsContract.COLUMN_PWD,      password);
            values.put(CredentialsContract.COLUMN_DATE,     date);
            try {
                values.put(CredentialsContract.COLUMN_DATE_HISTORY, dateHistory_.getString("date_history"));
                values.put(CredentialsContract.COLUMN_PWD_HISTORY,  pwdHistory_.getString("pwd_history"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ContentResolver cr  = getContentResolver();
            int numRows = 0;

            // insert the user or replace it if it exist.cf 'FileDbContentProvider'
            Uri newRowUri = cr.insert(CredentialsContract.CONTENT_URI_CREDENTIALS, values);
            if(newRowUri != null) numRows = 1;

            return numRows;

        }else{
            //do update. get data from cursor
            cursor.moveToPosition(-1);

            String userName        = null;
            String dateHistory    = null;
            String pwdHistory     = null;

            while (cursor.moveToNext()) {
                userName    = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_USERNAME));
                dateHistory = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_DATE_HISTORY));
                pwdHistory  = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_PWD_HISTORY));
            }
            cursor.close();

            //In cursor, the string 'dateHistory' is a JSON string. Now, convert the string 'dateHistory'in cursor to JSON.
            // and append the JSON build from the supplied date
            JSONObject dateHistory__ = null;
            try {
                dateHistory__ = new JSONObject(dateHistory);
                dateHistory__.put("dateHistory_", dateHistory);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //In cursor, the string 'pwdHistory' is a JSON string. Now, convert the string 'pwdHistory'in cursor to JSON.
            // and append the JSON build from the supplied password
            JSONObject pwdHistory__ = null;
            try {
                pwdHistory__ = new JSONObject(pwdHistory);
                pwdHistory__.put("pwdHistory_", pwdHistory);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //do update
            ContentResolver cr  = getContentResolver();
            ContentValues values = new ContentValues();

            values.put(CredentialsContract.COLUMN_PWD,      password);
            values.put(CredentialsContract.COLUMN_DATE,     date);
            try {
                values.put(CredentialsContract.COLUMN_DATE_HISTORY, dateHistory_.getString("date_history"));
                values.put(CredentialsContract.COLUMN_PWD_HISTORY,  pwdHistory_.getString("pwd_history"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            int numRows = 0;
            numRows = cr.update(CredentialsContract.CONTENT_URI_CREDENTIALS,
                    values,
                    CredentialsContract.COLUMN_USERNAME + " =? ",
                    new String[]{userName});

            return numRows;
        }
    }

    private void RegisterNewCredentialInPreferences(final EditText username, EditText password) {
        if(!sharedPreferences.getBoolean("firstTime", true)) {
            //Here, 'firstTime' is false. It means that 'username' and 'password' exist.
            String username_ = sharedPreferences.getString("username", null);
            String password_ = sharedPreferences.getString("password", null);
            //LoginActivity.sharedPreferences.getString("password", null);

            boolean userFound = (username_.equals(username.getText().toString())) ? true : false;
            if(userFound){
                //Here, the user is found in the preferences, change his password.
                myEditor.putString("password", password.getText().toString());
                myEditor.commit();

                //The registration of the new password is ok
                statusRegistration.setTextColor(Color.GREEN);
                statusRegistration.setText("Success");

                //save the username and the new password in preferences.
                //LoginActivity.myEditor.putBoolean("firstTime", false);
                //LoginActivity.myEditor.putString("username", username_);
                //LoginActivity.myEditor.putString("password", password_);
                //LoginActivity.myEditor.commit();

                button.setText(SUIVANT);

            }else {
                    // The register new pwd failed.
                    statusRegistration.setTextColor(Color.RED);
                    statusRegistration.setText("Echec.");
                    button.setEnabled(false);
                    quitter.setVisibility(View.VISIBLE);
                    quitter.setEnabled(true);
            }
            //Here, the user is not found in the preferences do not change his password.
            //Todo tell the user that his new password is not registered.
            return;
        }

        //Here, 'firstTime' does not exist in this case it is equal to true or it exists and it is equal à true.
    }


    private void remoteRegisterNewCredential(EditText user, EditText newPassword) {
        String username = user.getText().toString();
        String password = newPassword.getText().toString();

        socket.emit("register_user_in_db",  username, password, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject status = ((JSONObject) args[0]);
                boolean status_ = false;
                if (status == null) {
                    endActivity("fail");
                    return;
                }
                try {
                    status_ = status.getBoolean("insert");
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (status_) {
                    //success
                    //The registration of the new password is successfull. Notify the user and go to chat
                    statusRegistration.setVisibility(View.VISIBLE);
                    statusRegistration.setTextColor(Color.GREEN);
                    statusRegistration.setText("The new password is registered Successfully");

                    //save the username and the new password in preferences.
                    myEditor.putBoolean("firstTime", false);
                    myEditor.putString("username", username);
                    myEditor.putString("password", password);
                    myEditor.commit();

                    //exception : 'only the original thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button.setText(SUIVANT);
                        }
                    });
                    return;
                }
                //failure, The register new paw failed.
                statusRegistration.setTextColor(Color.RED);
                statusRegistration.setText("Failure : The new password is not registered");
                button.setEnabled(false);
                quitter.setVisibility(View.VISIBLE);
                quitter.setEnabled(true);
                return;
            }
        });
    }

    private void remoteRegisterNewCredential_(EditText username, EditText password) {
        final String username_ = username.getText().toString();
        final String password_ = password.getText().toString();
        //Register remotely.
        //String credentials = Credentials.basic(username.getText().toString(), password.getText().toString());

        //convert password to base-64 string.
        String passwordEncoded = android.util.Base64.encodeToString(password_.getBytes(), android.util.Base64.DEFAULT);

        final Call<ResponseBody> call = userService.updateUser(username_, passwordEncoded);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if(response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        //System.out.println("body = "+body);
                        //System.out.println("Headers = "+response.headers());
                        //System.out.println("isSuccessful = "+response.isSuccessful()); //Returns true if code() is in the range [200..300).
                        if(body.equals("success")){
                            //The registration of the new password is ok
                            statusRegistration.setTextColor(Color.GREEN);
                            statusRegistration.setText("Success");

                            //save the username and the new password in preferences.
                            LoginActivity.myEditor.putBoolean("firstTime", false);
                            LoginActivity.myEditor.putString("username", username_);
                            LoginActivity.myEditor.putString("password", password_);
                            LoginActivity.myEditor.commit();

                            button.setText(SUIVANT);

                        }else {
                            // The register new paw failed.
                            statusRegistration.setTextColor(Color.RED);
                            statusRegistration.setText("Echec.");
                            button.setEnabled(false);
                            quitter.setVisibility(View.VISIBLE);
                            quitter.setEnabled(true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        e.getMessage();
                    }
                    //Save the state of 'rememberMe' of the checkBox
                    //myEditor.putBoolean("rememberMe", rememberMe);
                    //myEditor.commit();
                    // intent
                    //login start main activity
                    //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username", username);
                    //startActivity(intent);
                    //postLogin(LoginActivity.this, username);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                t.getMessage();
                return;
            }
        });

    }

    private void checkNewPwd(EditText password2, EditText password3) {
        int color = Color.RED;
        if((password2.length() == 0) || (password3.length() == 0)){
            statusPwd3.setTextColor(color);
            statusPwd3.setText("Champs vide.");
            return;
        }

        //Ce test est inclus dans le test suivant de comparaison (égalité)
        //if(password2.getText().length() != password3.getText().length()){
        //    statusPwd3.setTextColor(color);
        //    statusPwd3.setText("Les deux mots de passe ne sont pas de même longueur.");
        //    return;
        //}

        if(!password2.getText().toString().equals(password3.getText().toString())){
            statusPwd3.setTextColor(color);
            statusPwd3.setText("Les deux mots de passe ne correspondent pas.");
            return;
        }

        String passwordPattern = "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{6,8}";

        if(!password2.getText().toString().matches(passwordPattern) || !password3.getText().toString().matches(passwordPattern)){
            statusPwd3.setTextColor(color);
            statusPwd3.setText("Les deux mots de passe ne sont pas conformes.");
            return;
        }

        //check if the new password is already set localy. Do a local check in sqlite db
        // il vaut mieux faire ce test au debut de input de password3
        //asynchronous
        //remote check not used
        //isNewPasswordAlreadySet(username.getText().toString(), password2.getText().toString());

        //local check
        boolean exists = isNewPasswordAlreadySetLocaly(username.getText().toString(), password2.getText().toString());
        if(exists){
            //remain in this step until a new pwd doesn't exist.
            // disable the button 'validate new pwd'. it will be enabled when a new pwd is typed
            button.setEnabled(false);
            statusPwd3.setTextColor(Color.RED);
            statusPwd3.setText(" The new password already exists.");
            return;
        }
        //here, the new pwd doesn't exist. show the 'register' button.
        statusPwd3.setTextColor(Color.GREEN);
        statusPwd3.setText(" The new password is successful.");
        button.setText(REGISTER);
        password2.setEnabled(false);
        password3.setEnabled(false);
        return;
    }

    private boolean isNewPasswordAlreadySetLocaly(String username, String password){
        String[] mProjection   = new String[]
                {
                        CredentialsContract.COLUMN_PWD_HISTORY
                };
        String selection       = CredentialsContract.COLUMN_USERNAME + " =? ";
        String selectionArgs[] = new String[]{username};

        //Method : ContentProvider

        Cursor cursor = getContentResolver().query(
                CredentialsContract.CONTENT_URI_CREDENTIALS,
                mProjection,
                selection,
                selectionArgs,
                null
        );

        //if (cursor == null) return null;
        //if (cursor.getCount() == 0) return new HashMap<>();

        cursor.moveToPosition(-1);
        String pwdHistory = null;

        //int i = 0;
        //Map<String, Long> map = new HashMap<>();      //HashMap doesn't preserve any order
        //Map<String, Long> map = new LinkedHashMap<>();  //HashMap doesn't preserve any order
        //ArrayList<String> arrayList = new ArrayList<>();
        while (cursor.moveToNext()) {
            pwdHistory = cursor.getString((cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_PWD_HISTORY)));
            //from = cursor.getString(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_USER_ORIGINE));
            //to = cursor.getString(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_USER_TARGET));
            //time = cursor.getLong(cursor.getColumnIndexOrThrow(LastUsersContract.COLUMN_TIME));
            //arrayList.add(to);
            //if (from.equals(nickname)) map.put(to, time);
            //if (to.equals(nickname)) map.put(from, time);
        }
        //
        boolean contains = false;
        try {
            JSONArray jsonArray = new JSONArray(pwdHistory);
            //System.out.println("jsonArray = " + jsonArray);
            contains = jsonArray.toString().contains(password);
            //System.out.println("contains = " + contains + " jsonArray.toString() = " + jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        cursor.close();
        return contains;
    }

    //check the password remotely.
    private void isNewPasswordAlreadySet(String username, String password){
        socket.emit("is_new_password_already_set",  username, password, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject status = ((JSONObject) args[0]);
                boolean isAlredySet = false;
                if (status == null) {
                    endActivity("fail");
                    return;
                }
                try {
                    isAlredySet = status.getBoolean("alreadySet");
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (!isAlredySet) {
                    //it is not already set, success
                    //workaround : only the original thread that created a view hierarchy can touch its views.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusPwd3.setTextColor(Color.GREEN);
                            statusPwd3.setText("OK.");
                            statusPwd3.setText("The new password is successfull.");
                            button.setText(REGISTER);
                        }
                    });

                    return;
                }
                //The pwd is already set, failure.
                //workaround : only the original thread that created a view hierarchy can touch its views.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusPwd3.setTextColor(Color.RED);
                        statusPwd3.setText("The new password is already set.");
                        button.setVisibility(View.INVISIBLE);
                        button.setText(VALIDER_NEW_PWD);
                        //retry, enable pwd field
                        password2.setEnabled(true);
                    }
                });
            }
        });
    }

    private void gotoNext(EditText user) {
        String username = user.getText().toString();
        //Redirect
        postLogin(ChangePasswordActivity.this, username);
    }

    private void checkUsername(String username) {
        //This case is already handled by 'OnTextChange'
        if(username.length() == 0){
            statusUsername.setTextColor(Color.RED);
            statusUsername.setText("Champs vide.");
            return;
        }
        isUsernameExists(username);
    }

    private void isUsernameExists_(final String username) {
        statusUsername.setTextColor(Color.RED);
        statusUsername.setText("status");
    }

    private void isUsernameExists(final String username) {
        //local check. Get 'username' and 'password' from sharedPreferences.
        String username_ = null;

            username_ = sharedPreferences.getString("username", null);

            //LoginActivity.sharedPreferences.getString("password", null);
            boolean userFound = false;
            if(username_ != null) {
                userFound = (username_.equals(username)) ? true : false;

                //prepare the status
                int color = userFound ? Color.GREEN : Color.RED;
                String status = userFound ? "OK." : "L'utilisatur '" + username + "' n'existe pas.";

                statusUsername.setTextColor(color);
                statusUsername.setText(status);

                //statusRegistration.setTextColor(color);
                //statusRegistration.setText(status);
                //here 'userfound' is false. It is not found. Notify the user that he can do another try or signup

                if (!userFound) {
                    nbTry++;
                    if (nbTry < MAX_TRY) {
                        //allow another try
                        new AlertDialog.Builder(ChangePasswordActivity.this)
                                .setMessage("Another try or signup ?\n" + nbTry + "/" + MAX_TRY)
                                .setPositiveButton("Another try", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //startHandler();
                                        //Redirect to 'ChangePasswordAactivty'
                                        button.setText(VALIDER_USER);
                                        quitter.setVisibility(View.VISIBLE);
                                        ChangePasswordActivity.this.username.setText(null);
                                        ChangePasswordActivity.this.username.requestFocus();

                                        dialogInterface.dismiss();
                                    }
                                })
                                .setNegativeButton("Signup", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Redirect to 'FirstTimeLoginAactivty'
                                        Intent intent = new Intent(ChangePasswordActivity.this, FirstTimeLoginActivity.class);
                                        intent.putExtra("username", username);
                                        startActivity(intent);

                                        dialog.dismiss();
                                        endActivity("fail");

                                    }
                                })
                                .create()
                                .show();
                        return;
                    }
                    if (nbTry == MAX_TRY) {
                        //show dialog with signup
                        new AlertDialog.Builder(ChangePasswordActivity.this)
                                .setMessage("The number of attempts reached the limit, signup ? \n " + nbTry + "/" + MAX_TRY)
                                .setPositiveButton("Signup", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        Intent intent = new Intent(ChangePasswordActivity.this, FirstTimeLoginActivity.class);
                                        intent.putExtra("username", username);
                                        startActivity(intent);

                                        //button.setText(SIGNUP);
                                        //quitter.setVisibility(View.VISIBLE);
                                        //ChangePasswordActivity.this.username.setText(null);
                                        //ChangePasswordActivity.this.username.requestFocus();

                                        dialogInterface.dismiss();
                                        endActivity("fail");
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //end app
                                        dialog.dismiss();
                                        endActivity("fail");
                                    }
                                })
                                .create()
                                .show();
                        return;
                    }
                }

                //here the username is found in prefrences
                if (userFound) {
                    //freeze the 'username' field and set focus on 'password1'.
                    this.username.setEnabled(false);
                    password1.requestFocus();
                    button.setText(VALIDER_PWD);

                    labelPwd1.setHeight(labelPwd1Height);
                    password1.setHeight(password1height);
                    statusPwd1.setHeight(statusPwd1Height);

                    labelPwd1.setVisibility(View.VISIBLE);
                    password1.setVisibility(View.VISIBLE);
                    statusPwd1.setVisibility(View.VISIBLE);

                    password1.setEnabled(true);
                    password1.requestFocus();
                    button.setEnabled(false);

                    return;
                }
            }
            //here the username is null. It is the first time or the app has been deleted
            //do a remote check
            //it is asynchronous
            remoteCheckUsername(username);
    }

    private void remoteCheckCredentials(String username, String password) {
        //if the name entered in 'nickname' edit text already exists ?
        //As the user is new or he deleted the app, the local db is also deleted.
        //then ask the server if the user exists or not.

        JSONObject credential  = new JSONObject();
        try {
            credential.put("name", username);
            credential.put("pwd", password);

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
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(userExist[0]){

                    //The user exists in db.
                    //workaround "Only the original thread that created a view hierarchy can touch its views."
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //freeze the 'username' field and 'password' and set focus on 'password2'
                            // and show 'password3'.
                            ChangePasswordActivity.this.username.setEnabled(false);
                            ChangePasswordActivity.this.password1.setEnabled(false);
                            statusPwd1.setTextColor(Color.GREEN);
                            statusPwd1.setText("OK");

                            password2.requestFocus();
                            button.setText(VALIDER_NEW_PWD);
                            password2.setEnabled(true);

                            //show the 'password2=new password'
                            labelPwd2.setHeight(labelPwd2Height);
                            password2.setHeight(password2height);
                            statusPwd1.setHeight(statusPwd1Height);

                            labelPwd2.setVisibility(View.VISIBLE);
                            password2.setVisibility(View.VISIBLE);
                            statusPwd1.setVisibility(View.VISIBLE);

                            //show the password3
                            labelPwd3.setHeight(labelPwd3Height);
                            password3.setHeight(password3height);
                            statusPwd3.setHeight(statusPwd3Height);

                            labelPwd3.setVisibility(View.VISIBLE);
                            password3.setVisibility(View.VISIBLE);
                            statusPwd3.setVisibility(View.VISIBLE);

                            password3.setEnabled(false);

                            button.setEnabled(false);
                        }
                    });

                    return;
                    ////////////////
                    //callback.authenticateUser(user);

                    //The authentication is successful, end this activity and return to the activity
                    // which launched this intent 'NavigatorActivity' of 'app' module.
                    //endActivity("success");
                }else{
                    //the user does'nt exists in db or there is an error.
                    //give the user a try.
                    nbTry++;
                    if(nbTry == MAX_TRY){
                        //the number of try exceed the limit
                        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        //    finishAffinity(); //like finish();
                        //}


                        //Ban the user for one hour. Set starting time of ban.
                        long startBanTime     = new Date().getTime();
                        String applicationId  = getPackageName();

                        myEditor.putLong("startBanTime", startBanTime);
                        myEditor.putString("applicationId", applicationId);
                        myEditor.commit();

                        //notify the user, he exceed the number of tries allowed and he does wait
                        // for 1 hour before resuming.

                        // Workaround : 'Can't create handler inside thread that has not called Looper.prepare()
                        new Thread() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                //notify the user
                                new AlertDialog.Builder(ChangePasswordActivity.this)
                                        .setMessage("The number of attempts reached the limit ? \n " +
                                                "You will wait 1 hour before resuming.").
                                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                endActivity("fail");
                                            }
                                        })
                                        .setCancelable(false)
                                        .create().show();
                                Looper.loop();
                            }
                        }.start();

                        //SaveBanInfo(applicationId, startBanTime);

                        //disable views while showing dialog

                        //error : 'Only the original thread that created a view hierarchy can touch its views.'
                        //En effet, à cause du 'Ack' on est sur un autre thread ou processus
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //disable buttons
                                //edtUsername.setEnabled(false);
                                //edtPassword.setEnabled(false);
                                //btnLogin.setEnabled(false);
                                //btnQuit.setEnabled(false);
                                //btnRegister.setEnabled(false);
                            }
                        });
                        //endActivity("fail");
                        return;

                        ///////////////////////////////////////////////////////////////////////////
                        /*
                        new AlertDialog.Builder(LoginActivity.this).
                                setMessage("Number of tries exceed the limit.").
                                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //postLogin(LoginActivity.this, username, BAN);
                                        //finish();
                                        endActivity();
                                        //System.exit(0);
                                    }
                                }).create().show();
                        return;
                        */
                    }
                    //The max of tries is not reached. allow another try and delete the previous data
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //tvLoginStatus.setVisibility(View.VISIBLE);
                            //tvLoginStatus.setText(getResources().getString(R.string.login_status_failure));
                            //tvLoginStatus.setTextColor(Color.RED);

                            tryCounter.setVisibility(View.VISIBLE);

                            //tryCounter.setText((getResources().getString(R.string.number_of_tries)) + (nbTry) + "/" + MAX_TRY);
                            tryCounter.setText(nbTry + "/" + MAX_TRY);


                            quitter.setVisibility(View.VISIBLE);
                            ChangePasswordActivity.this.username.setEnabled(false);
                            ChangePasswordActivity.this.password1.setText(null);
                            ChangePasswordActivity.this.password1.setEnabled(false);
                            button.setText(REPEAT);
                            button.setEnabled(true);
                            ChangePasswordActivity.this.password1.requestFocus();
                            statusPwd1.setTextColor(Color.RED);//placer after 'password1.setText(null)'
                            statusPwd1.setText("Error : " + nbTry + "/" + MAX_TRY);

                            /*
                            edtUsername.setText(null);
                            edtPassword.setText(null);
                            edtUsername.requestFocus();
                            //ToDo : Show the keyboard
                            tryCounter.setVisibility(View.VISIBLE);
                            tryCounter.setText((nbTry + 1) + "/" + MAX_TRY);
                            */
                        }
                    });
                }
            }
        });

    }


    private void remoteCheckUsername(String username) {
        //if the name entered in 'nickname' edit text already exists ?
        //As the user is new or he deleted the app, the local db is also deleted.
        //then ask the server if the user exists or not.

        JSONObject credential  = new JSONObject();
        try {
            credential.put("name", username);
            credential.put("pwd", JSONObject.NULL);

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
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(userExist[0]){

                    //The user exists, check his pwd.
                    //////////////////
                    //freeze the 'username' field and set focus on 'password1'.

                    //workaround "Only the original thread that created a view hierarchy can touch its views."
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        ChangePasswordActivity.this.username.setEnabled(false);
                        ChangePasswordActivity.this.statusUsername.setTextColor(Color.GREEN);
                        ChangePasswordActivity.this.statusUsername.setText("OK");

                        password1.requestFocus();
                        button.setText(VALIDER_PWD);


                            labelPwd1.setHeight(labelPwd1Height);
                            password1.setHeight(password1height);
                            statusPwd1.setHeight(statusPwd1Height);

                            labelPwd1.setVisibility(View.VISIBLE);
                            password1.setVisibility(View.VISIBLE);
                            statusPwd1.setVisibility(View.VISIBLE);

                            password1.setEnabled(true);
                            password1.requestFocus();
                            button.setEnabled(false);
                        }
                    });

                    return;
                    ////////////////
                    //callback.authenticateUser(user);

                    //The authentication is successful, end this activity and return to the activity
                    // which launched this intent 'NavigatorActivity' of 'app' module.
                    //endActivity("success");
                }else{
                    //the user does'nt exists in db or there is an error.
                    //give the user a try.
                    nbTry++;
                    //prepare the status
                    int color = Color.RED;
                    String status = (nbTry == MAX_TRY) ? "max tries reached" : "The user '" + username + "' is not found.";

                    if(nbTry == MAX_TRY){
                        //the number of try exceed the limit
                        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        //    finishAffinity(); //like finish();
                        //}

                        //notify the user, he exceed the number of tries allowed and he does wait
                        // for 1 hour before resuming.

                        // Can't create handler inside thread that has not called Looper.prepare()
                        new Thread() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                //notify the user
                                new AlertDialog.Builder(ChangePasswordActivity.this)
                                        .setMessage("The number of attempts reached the limit ? \n " +
                                        "You will wait 1 hour before resuming.").
                                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                endActivity("fail");
                                            }
                                        })
                                        .setCancelable(false)
                                        .create().show();
                                Looper.loop();
                            }
                        }.start();

                        //Ban the user for one hour. Set starting time of ban.
                        long startBanTime     = new Date().getTime();
                        String applicationId  = getPackageName();

                        myEditor.putLong("startBanTime", startBanTime);
                        myEditor.putString("applicationId", applicationId);
                        myEditor.commit();

                        //SaveBanInfo(applicationId, startBanTime);

                        //disable views while showing dialog

                        //error : 'Only the original thread that created a view hierarchy can touch its views.'
                        //En effet, à cause du 'Ack' on est sur un autre thread ou processus
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //disable buttons
                                //edtUsername.setEnabled(false);
                                //edtPassword.setEnabled(false);
                                //btnLogin.setEnabled(false);
                                //btnQuit.setEnabled(false);
                                //btnRegister.setEnabled(false);
                            }
                        });
                        //endActivity("fail");
                        return;

                        ///////////////////////////////////////////////////////////////////////////
                        /*
                        new AlertDialog.Builder(LoginActivity.this).
                                setMessage("Number of tries exceed the limit.").
                                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        //postLogin(LoginActivity.this, username, BAN);
                                        //finish();
                                        endActivity();
                                        //System.exit(0);
                                    }
                                }).create().show();
                        return;
                        */
                    }
                    //The max of tries is not reached. allow another try and delete the previous data

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //tvLoginStatus.setVisibility(View.VISIBLE);
                            //tvLoginStatus.setText(getResources().getString(R.string.login_status_failure));
                            //tvLoginStatus.setTextColor(Color.RED);

                            tryCounter.setVisibility(View.VISIBLE);

                            //tryCounter.setText((getResources().getString(R.string.number_of_tries)) + (nbTry) + "/" + MAX_TRY);
                            tryCounter.setText(nbTry + "///////////" + MAX_TRY);

                            quitter.setVisibility(View.VISIBLE);

                            //'setText(null)' produit 'statusUsername.setText(null) cf username.addTextChange;
                            // tout affichage doit se faire apres 'username.setText(null)'
                            ChangePasswordActivity.this.username.setText(null);
                            ChangePasswordActivity.this.username.requestFocus();

                            statusUsername.setVisibility(View.VISIBLE);
                            statusUsername.setTextColor(Color.RED);
                            statusUsername.setText(status);

                            button.setText(VALIDER_USER);
                            button.setEnabled(false);

                            /*
                            edtUsername.setText(null);
                            edtPassword.setText(null);
                            edtUsername.requestFocus();
                            //ToDo : Show the keyboard
                            tryCounter.setVisibility(View.VISIBLE);
                            tryCounter.setText((nbTry + 1) + "/" + MAX_TRY);
                            */
                        }
                    });

                }
            }
        });
    }

    private void remoteCheckUsername_(final String username) {
        //remote check
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
                        int color = userFound ? Color.GREEN : Color.RED;
                        String status = userFound ? "OK." : "L'utilisatur '"+username+"' n'existe pas.";

                        statusUsername.setTextColor(color);
                        statusUsername.setText(status);

                        statusRegistration.setTextColor(color);
                        statusRegistration.setText(status);
                        //Redirect
                        if(!userFound) {
                            button.setText(SIGNUP);
                            quitter.setVisibility(View.VISIBLE);
                        }

                        //Enable pwd field.
                        if(userFound) {
                            button.setText(VALIDER_PWD);

                            labelPwd1.setHeight(labelPwd1Height);
                            password1.setHeight(password1height);
                            statusPwd1.setHeight(statusPwd1Height);

                            labelPwd1.setVisibility(View.VISIBLE);
                            password1.setVisibility(View.VISIBLE);
                            statusPwd1.setVisibility(View.VISIBLE);

                            password1.setEnabled(true);
                            password1.requestFocus();
                            button.setEnabled(false);
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

    //register credentials.
    private void register(EditText user, EditText pwd, long date_) {
        final String username = user.getText().toString();
        final String password = pwd.getText().toString();
        final String date     = String.valueOf(date_);

        //the 'registerNewPasswordLocally' is done after 'registerNewPasswordRemotely' is successful
        //'registerNewPasswordRemotely' is async
        registerNewPasswordRemotely(username, password, date);
        //registerNewPasswordLocally(username, password, date);
    }

    //register credentials in local sqlite db.
    private int registerNewPasswordLocally(String username, String newPassword, String date) {
        //get the current credentials
        String[] mProjection = new String[] {
                //CredentialsContract.COLUMN_ID,
                //CredentialsContract.COLUMN_USERNAME,
                //CredentialsContract.COLUMN_PWD,
                //CredentialsContract.COLUMN_DATE,
                CredentialsContract.COLUMN_DATE_HISTORY,
                CredentialsContract.COLUMN_PWD_HISTORY
        };

        //String tableName  = CredentialsContract.CREDENTIALS_TABLE_NAME;

        /*
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
        */
        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                CredentialsContract.CONTENT_URI_CREDENTIALS,
                mProjection,
                CredentialsContract.COLUMN_USERNAME  + " =? ",

                new String[]{username},

                null
        );
        if(cursor == null || cursor.getCount() == 0) throw new UnsupportedOperationException("'ChangePasswordActivity'. Unexpected return value : cursor null or query rowCount = 0"); ;

        cursor.moveToPosition(-1);

        //long id                = 0;
        //String oldPassword     = null;
        //String oldDate         = null;
        String dateHistory     = null;
        String pwdHistory      = null;      ;


        while (cursor.moveToNext()) {
            //id              = cursor.getLong(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_ID));
            //oldPassword     = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_PWD));
            //oldDate         = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_DATE));
            dateHistory     = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_DATE_HISTORY));
            pwdHistory      = cursor.getString(cursor.getColumnIndexOrThrow(CredentialsContract.COLUMN_PWD_HISTORY));


            // convert string 'dateHistory' to array
            try {
                JSONArray dateHistory_ = new JSONArray(dateHistory);
                dateHistory_.put(date);
                dateHistory = dateHistory_.toString();

                JSONArray pwdHistory_ = new JSONArray(pwdHistory);
                pwdHistory_.put(newPassword);
                pwdHistory = pwdHistory_.toString();

                /*
                String[] dateHistory__ = new String[dateHistory_.length()];

                for (int i = 0; i < dateHistory_.length(); i++) {
                    dateHistory__[i] = dateHistory_.getString(i);
                }
                //convert array '' to arrayList 'dateHistoryList' so we can append to it.
                List<String> dateHistoryList = new ArrayList<String>(Arrays.asList(dateHistory__));

                // append the suplied new password 'password' and the pwd History 'dateHistoryList'.

                ArrayList<String> mylist = new ArrayList<String>();
                mylist.add(mystring); //this adds an element to the list.
                */

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        cursor.close();

        //update the table 'chat_credentials' with the new password.
        int numRowsUpdated = updateCredentialsTable(username, newPassword, date, dateHistory, pwdHistory);
        return numRowsUpdated;

        /*
        //save new pwd
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
        */

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

        //ContentResolver cr  = getContentResolver();
        //int numRows = 0;

        // insert or replace the user in sqlite 'chat_credentials' table
        //Uri newRowUri = cr.insert(CredentialsContract.CONTENT_URI_CREDENTIALS, values);
        //if(newRowUri != null) numRows = 1;

        //return numRows;
    }

    //update lacal sqlite CredentialsTable.
    private int updateCredentialsTable(String username, String newPassword, String date, String dateHistory, String pwdHistory){
        ContentValues values = new ContentValues();
        values.put(CredentialsContract.COLUMN_PWD,          newPassword);
        values.put(CredentialsContract.COLUMN_DATE,         date);
        values.put(CredentialsContract.COLUMN_DATE_HISTORY, dateHistory);
        values.put(CredentialsContract.COLUMN_PWD_HISTORY,  pwdHistory);

        /*
        String[] mProjection = new String[] {
                //CredentialsContract.COLUMN_ID,
                //CredentialsContract.COLUMN_USERNAME,
                CredentialsContract.COLUMN_PWD,
                CredentialsContract.COLUMN_DATE,
                CredentialsContract.COLUMN_DATE_HISTORY,
                CredentialsContract.COLUMN_PWD_HISTORY
        };
        */

        String selection       = CredentialsContract.COLUMN_USERNAME + " = ?" ;
        String[] selectionArgs = new String[]{username}; //the values of place holders.

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
        numRows = cr.update(CredentialsContract.CONTENT_URI_CREDENTIALS,
                values,
                selection,
                selectionArgs);
        if(numRows != 1) throw new UnsupportedOperationException("'ChangePasswordActivity'. Unexpected return value : numrows updated = " + numRows); ;
        return numRows;
    }

    //register credentials in remote db (server).
    private void registerNewPasswordRemotely(String username, String password, String date) {

        //encode password
        //final String password64 = android.util.Base64.encodeToString(password.getBytes(), android.util.Base64.DEFAULT);

        /*
        JSONObject credential  = new JSONObject();
        try {
            credential.put("name", username);
            credential.put("pwd", password64);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        */

        socket.emit("register_user_in_db",  username, password, date, new Ack() {
            @Override
            public void call(Object... args) {

                JSONObject status = ((JSONObject) args[0]);
                boolean status_ = false;
                if (status == null) {
                    endActivity("fail");
                    return;
                }
                try {
                    status_ = status.getBoolean("insert");
                    //callback.doSomething(userExist[0]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (status_) {
                    //success
                    //The registration of the new password is successful. Notify the user and do
                    // the local registration
                    statusRegistration.setVisibility(View.VISIBLE);
                    statusRegistration.setTextColor(Color.GREEN);
                    statusRegistration.setText("The new password is registered Successfully");

                    //save the username and the new password in preferences.
                    myEditor.putBoolean("firstTime", false);
                    myEditor.putString("username", username);
                    myEditor.putString("password", password);
                    myEditor.commit();

                    //workaround : exception : 'only the original thread ...'
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //button.setText(SUIVANT);
                            int nbrowsUpdated = registerNewPasswordLocally(username, password, date);
                            if(nbrowsUpdated == 1){ //other values are treated in 'registerNewPasswordLocally'
                                //show the 'NEXT' button
                                button.setText(SUIVANT);
                                statusRegistration.setTextColor(Color.GREEN);
                                statusRegistration.setText("Password changed successful");
                                //freeze all the password field
                                password1.setEnabled(false);
                                password2.setEnabled(false);
                            }
                        }
                    });
                    return;
                }
                //failure, The register new paw failed.
                statusRegistration.setTextColor(Color.RED);
                statusRegistration.setText("Failure : The new password is not registered in the server");
                button.setEnabled(false);
                quitter.setVisibility(View.VISIBLE);
                quitter.setEnabled(true);
                return;
            }
        });
    }

    //not used
    private void register_(EditText user, EditText pwd) {

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
                            ChangePasswordActivity.this.username.setEnabled(false);
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
                        ChangePasswordActivity.this.statusRegistration.setText(status);
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
                ChangePasswordActivity.this.statusRegistration.setText(status);
                return;
            }
        });
    }

    private void postLogin(Context context, String username) {
        // Before lanch intent, clear credential fields.
        this.username.setText(null);
        this.username.requestFocus();
        password1.setText(null);
        password2.setText(null);
        password3.setText(null);

        //login start main activity
        //Intent intent = new Intent(context, MainActivity.class);
        //intent.putExtra("username", username);
        //startActivity(intent);

        //redirect user to chat and close activity with 'success' status.
        callback.resetPwd(username);

        endActivity("success");
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

    //check the current pwd 'password1' in local sqlite db
    private void checkPwd(EditText username, EditText password) {
        button.setEnabled(false);

        //this case 'length=0' is treated in password1 event
        if (password.length() == 0) {
            statusPwd1.setTextColor(Color.RED);
            statusPwd1.setText("empty field : no password.");
            password1.requestFocus();
            return;
        }
        boolean pwdStatus = validatePwd(password.getText().toString());
        if(!pwdStatus){
            button.setEnabled(true);
            statusPwd1.setTextColor(Color.GREEN);
            statusPwd1.setText("The password in not valid.");
            password1.requestFocus();
            return;
        }
        //here the password is valid (no input extra characters, ...), do a local password check.
        // Get 'username' and 'password' from sqlite3 local db.
        //String password_ = sharedPreferences.getString("password", null);
        boolean pwdExists = checkPwdLocally(username.getText().toString(), password.getText().toString());

            if (pwdExists) {
                //The password is ok. the pwd is found in local sqlite3 db
                //numberTry = 0;
                tryCounter.setVisibility(View.INVISIBLE);
                //Freeze the password
                password1.setEnabled(false);

                statusPwd1.setTextColor(Color.GREEN);
                statusPwd1.setText("OK");
                //make visible the new password field
                labelPwd2.setHeight(labelPwd2Height);
                password2.setHeight(password2height);
                labelPwd3.setHeight(labelPwd3Height);
                password3.setHeight(password3height);
                statusPwd3.setHeight(statusPwd3Height);

                labelPwd2.setVisibility(View.VISIBLE);
                password2.setVisibility(View.VISIBLE);
                labelPwd3.setVisibility(View.VISIBLE);
                password3.setVisibility(View.VISIBLE);
                statusPwd3.setVisibility(View.VISIBLE);

                button.setText(VALIDER_NEW_PWD);
                button.setEnabled(false);
                password2.requestFocus();

                //show the 'Quit' button
                quitter.setVisibility(View.VISIBLE);
            } else {
                //The pwd is not found in local sqlite db.
                //display the try counter
                tryCounter.setVisibility(View.VISIBLE);
                tryCounter.setText((numberTry + 1) + "/" + MAX_TRY);

                //Count the number of try.
                if (numberTry == NUMBER_TRY - 1) {

                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    //    finishAffinity(); //
                    //}else{
                    //    finish();
                    //}

                    //notify the user that it will be banished for one hour and return to the chat.

                    LoginActivity.myEditor.putLong("startBanTime", new Date().getTime());
                    LoginActivity.myEditor.commit();

                    //notify the user
                    new AlertDialog.Builder(ChangePasswordActivity.this).
                            setMessage("Nombre d'essais autorisés dépassé.\n" +
                                    " You will wait 1 hour before resuming").
                            setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    endActivity("fail");
                                    finish(); //finish this activity and return to the chat
                                    //System.exit(0);
                                }
                            }).create().show();
                    return;
                }
                // The pwd is not correct. allow another try.

                username.setEnabled(false);
                password1.setEnabled(false);
                password1.setText(null);
                button.setText(REPEAT); //put after 'password.setText(null);'
                button.setEnabled(true);
                statusPwd1.setTextColor(Color.RED);
                statusPwd1.setText("Echec : " + (numberTry + 1) + "/" + MAX_TRY);
                quitter.setVisibility(View.VISIBLE);
            }
    }

    //not used : retrofit
    private void remoteCheckCredential_(EditText username, EditText password) {

        ///////////////////////////////////////////////////////////////////////////////////////////
        //Avec la méthode ci-dessous, l'appel n'arrive pas au server.
        //
        //String basic = username+":"+password;
        //String authToken = android.util.Base64.encodeToString(basic.getBytes(), android.util.Base64.DEFAULT);
        //authToken_ = "Basic "+authToken;
        //String basic_ = "Basic %s";

        //final Call<ResponseBody> call = userService.valideAutorization(String.format(basic_, authToken), "azerty");
        //final Call<ResponseBody> call = userService.valideAutorization(authToken_, "azerty");
        //final Call<ResponseBody> call = userService.valideAutorization(basic, "azerty");
        ///////////////////////////////////////////////////////////////////////////////////////////
        String credentials = Credentials.basic(username.getText().toString(), password.getText().toString());

        final Call<ResponseBody> call = userService.valideAutorization(credentials, "azerty");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if(response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        //System.out.println("body = "+body);
                        //System.out.println("Headers = "+response.headers());
                        //System.out.println("isSuccessful = "+response.isSuccessful()); //Returns true if code() is in the range [200..300).
                        if(body.equals("success")){
                            //The password is ok
                            numberTry = 0;

                            statusPwd1.setTextColor(Color.GREEN);
                            statusPwd1.setText("OK");
                            //make visible the new password field
                            labelPwd2.setHeight(labelPwd2Height);
                            password2.setHeight(password2height);
                            labelPwd3.setHeight(labelPwd3Height);
                            password3.setHeight(password3height);
                            statusPwd3.setHeight(statusPwd3Height);

                            labelPwd2.setVisibility(View.VISIBLE);
                            password2.setVisibility(View.VISIBLE);
                            labelPwd3.setVisibility(View.VISIBLE);
                            password3.setVisibility(View.VISIBLE);
                            statusPwd3.setVisibility(View.VISIBLE);

                            button.setText(VALIDER_NEW_PWD);
                            button.setEnabled(false);
                        }else {
                            //Couunt the number of try
                            if (numberTry > NUMBER_TRY - 1) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    ChangePasswordActivity.this.finishAffinity(); //
                                }else{
                                    finish();
                                }
                                //Ban this user
                                LoginActivity.myEditor.putLong("startBanTime", new Date().getTime());
                                LoginActivity.myEditor.commit();
                                System.exit(0);
                            }

                            // The pwd is not correct.
                            statusPwd1.setTextColor(Color.RED);
                            statusPwd1.setText("Echec.");
                            button.setText(REPEAT); //another try
                            quitter.setVisibility(View.VISIBLE);


                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        e.getMessage();
                    }
                    //Save the state of 'rememberMe' of the checkBox
                    //myEditor.putBoolean("rememberMe", rememberMe);
                    //myEditor.commit();
                    // intent
                    //login start main activity
                    //Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    //intent.putExtra("username", username);
                    //startActivity(intent);
                    //postLogin(LoginActivity.this, username);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                t.getMessage();
                return;
            }
        });

    }


    private boolean checkPwdLocally(String username, String password) {
        boolean userFound = false;
        String[] mProjection = new String[] {
                CredentialsContract.COLUMN_ID,
                CredentialsContract.COLUMN_USERNAME,
                CredentialsContract.COLUMN_PWD,
                CredentialsContract.COLUMN_DATE,
                CredentialsContract.COLUMN_DATE_HISTORY,
                CredentialsContract.COLUMN_PWD_HISTORY
        };

        //Method : ContentProvider
        Cursor cursor = getContentResolver().query(
                CredentialsContract.CONTENT_URI_CREDENTIALS,
                mProjection,
                CredentialsContract.COLUMN_USERNAME  + " =?  AND " +
                        CredentialsContract.COLUMN_PWD       + " =?",

                new String[]{username, password},

                null
        );
        if(cursor == null)throw new UnsupportedOperationException("ChangePasswordActivity. Unexpected cursor value = null");
        if(cursor.getCount() == 0) userFound = false;
        if(cursor.getCount() == 1) userFound = true;
        cursor.close();
        return userFound;
    }
}