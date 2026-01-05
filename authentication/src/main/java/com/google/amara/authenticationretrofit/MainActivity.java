package com.google.amara.authenticationretrofit;

import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvGallery, tvMarks, tvNotes, tvMessages;
    private static final long BAN_TIME = 3600000; //1 hour
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_authentication);
        //setContentView(new myView(this));

       tvWelcome      = (TextView) findViewById(R.id.textView_welcome);

        Bundle extras = getIntent().getExtras();

        //Get username
        String username = null;
        if(extras != null){
            username = extras.getString("username");
        }

        //Get last connection time
        long lastConnextion = LoginActivity.sharedPreferences.getLong("lastConnection", 0);

        //Set the bundle extras to send with the intent.
        LoginActivity.myEditor.putLong("lastConnection", new Date().getTime());
        LoginActivity.myEditor.putBoolean("isConnected", isConnected());
        LoginActivity.myEditor.commit();
    }

    /**
     * Called when a ban occur.
     * @param context the context.
     * @param startBanTime the time when the ban starts. It is set after 3 unsuccessfull attemps to login.
     */
    private void banish(Context context, long startBanTime) {
        //Notify the user how many time it remains to have free access.
        long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
        long minutes  = remainingTime /  60000;
        long secondes = (remainingTime % 60000);
        int secondes_ = (int)(secondes / 1000);

        new AlertDialog.Builder(context).
                setMessage("Il reste : " + minutes + " minutes et " + secondes_ + " secondes.").
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).create().show();
    }

    private String getDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.FRENCH);
        return sdf.format(new Date(time));
    }

    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            connected = (info != null) && info.isAvailable() && info.isConnectedOrConnecting();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return connected; //Here connected = false.
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////////
