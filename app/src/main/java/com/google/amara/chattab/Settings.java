package com.google.amara.chattab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;

public class Settings extends    AppCompatActivity
                      implements BroadcastNotification,
                                 NetworkConnectionReceiver.ReceiverListener{

    public Socket socket;
    private static final int REQUEST_CODE_SETTINGS = 900;
    private static final int RESULT_CODE_SETTINGS  = 901;

    private BroadcastReceiver myReceiver           = null;

    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected  = new ArrayList<>();
    private ArrayList<String> permissions          = new ArrayList<>();

    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private static final int REQUEST_PERMISSIONS    = 100;
    private static final long BAN_TIME              = 3600000; //1 hour


    //@Override
    protected void onCreate_(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            //socket = IO.socket("https://android-chat-server.onrender.com/");
            socket = IO.socket("http://localhost:5000/");
            socket.connect();
            //for test only
            //socket.emit("chat_message", "Hello the world");

            socket.emit("get_ban_info", getPackageName());

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //test network connection
        ////////////////////////////////////////////////////////////////////////////////////////////
        //instantiate the broadcast service. When there is change in network, the receiver of the broadcast
        //will send notification to the mainActivity via the interface 'SendNetworkNotification' which
        //displays an alert dialog

        ////////////////////////////////////////////////////////////////////////////////////////////
        //airmode detect
        //int isAirPlaneModeOn = Settings.System.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        IntentFilter intentFilter = new IntentFilter("android.intent.action.ACTION_AIRPLANE_MODE_CHANGED");
        //IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("AirplaneMode", "Service state changed");
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////

        // for test. Remove comments in production mode
        myReceiver = new MyReceiver(this);
        broadcastIntent();

        /*  NO NEED TO DO 'checkConnection' BELOW IT IS DONE IN 'broadcastIntent' AVOVE  ***********
        //******************************************************************************************
        //Check internet connection and show the user a dialog
        if (!checkConnection()) {
            //Snackbar.make(MainActivity.this.findViewById(android.R.id.content), "No Internet connection", Snackbar.LENGTH_LONG).show();
            //return; //return 'onCreate' but does the next lifcycle phase 'onStart', 'onResume' ,...

            //Intent intent = new Intent(MainActivity.this, LoginActivity.class); //image_profile_gallery
            //intent.putExtra("data", "data"); //dummy
            //startActivityForResult(intent, REQUEST_CODE_LOGIN);

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setMessage("No Internet connection");
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            }).create().show();
            return;
        }
        */

        //set permissions and launch the 'NavigatorActivity'
        onConnection(); //uncomment in prod mode
    }

    //not used
    private boolean checkConnection() {

        // initialize intent filter
        IntentFilter intentFilter = new IntentFilter();

        // add action
        intentFilter.addAction("android.new.conn.CONNECTIVITY_CHANGE");

        // register receiver
        registerReceiver(new NetworkConnectionReceiver(), intentFilter);

        // Initialize listener
        NetworkConnectionReceiver.Listener = this;

        // Initialize connectivity manager
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Initialize network info
        @SuppressLint("MissingPermission")
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        // get connection status
        boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();

        // display snack bar
        //showSnackBar(isConnected);
        Snackbar.make(this.findViewById(android.R.id.content), "Connection", Snackbar.LENGTH_LONG).show();

        return true; //for test only. In production,uncomment the following statement
        //return isConnected;
    }

    private void broadcastIntent() {
        registerReceiver(myReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    protected void onResume() {
        super.onResume();
        // call method
        //checkConnection();

        //manage user interaction
        //startHandler(); //This statement cause the interaction dialog flicking
        Log.d("onResume", "onResume_restartActivity");
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
    protected void onStop() {

        super.onStop();
    }

    //the broadcast has sent network status.
    @Override
    public void sendNetworkNotification(String netWorkStatus) {

        View view = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(view, "Status MainActivity = " + netWorkStatus, Snackbar.LENGTH_LONG).show();
        //or
        Snackbar.make(findViewById(android.R.id.content), "Status MainActivity = " + netWorkStatus, Snackbar.LENGTH_LONG).show();

        //enable 'btn' to enter the chat only if there is connection.
        switch (netWorkStatus) {
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
                    if (ChatBoxUsers.class.isInstance(fragment)) index = i;
                    if (ChatBoxMessage.class.isInstance(fragment)) index_ = i;
                }
                */

                /*
                for(Fragment f0 : fragments){
                    if(ChatBoxUsers.class.isInstance(f0)) index = i;
                    if(ChatBoxMessage.class.isInstance(f0)) index_ = i;
                    i++;
                }

                ChatBoxUsers f = (ChatBoxUsers)fragments.get(index);
                ChatBoxMessage f_ = (ChatBoxMessage)fragments.get(index_);

                int x = 0;

                if((f != null) && (f.isVisible()))x = 0;

                 */

        }
    }

    private void onConnection() {

        //there is a connection. Check permissions
        //set permissions to request listed in the manifest
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);

        checkPermissions(permissions);
    }

    private void checkPermissions(ArrayList<String> permissions) {

        //if the permissions are already granted in device, permissionsToRequest.size() = 0;
        permissionsToRequest = permissionsToRequest(this.permissions);

        if(permissionsToRequest.isEmpty()){doNext(); return;}

        //check the permissions. The result will be found in 'onRequestPermissionsResult'.
        
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA},
                REQUEST_PERMISSIONS);// ActivityCompat.request permissions, the result will be found in 'onRequestpermissionsResult'

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
            case REQUEST_PERMISSIONS :
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                //The user has pressed the 'Deny' button on permission dialog.
                // Manage permissions rejected. Show a dialogue with two buttons : OK and Cancel
                //explaing why thes permissions are mandatory.
                if (!permissionsRejected.isEmpty()) {
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
                    doNext();
                    return;
                }

                break;
        }
    }


    @Override
    public void onNetworkChange(boolean isConnected) {
        // display snack bar
        //showSnackBar(isConnected);
    }

    //the permissions are granted in 'checkPermissions' we come here.
    private void doNext() {
        int i = 0;
        //login
        //Intent intent = new Intent(this, NavigatorActivity.class);
        //intent.putExtra("some_extra", "some_extra"); //not used, for illustration
        //startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        return;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        //Authentication, we come from 'NavigatorActivity' in 'app' moddule
        if (requestCode == REQUEST_CODE_SETTINGS &&   // send code
                resultCode == RESULT_CODE_SETTINGS && // receive code
                null != data) {                       //data sent from 'LoginActivity' in 'authentication' module.

            String status = data.getStringExtra("status");

            finish(); //end the app
            //finishAffinity();
            //System.exit(0);
        }
    }
}