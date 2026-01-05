package com.google.amara.chattab;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.view.View;
import android.view.WindowManager;

import com.google.android.material.snackbar.Snackbar;

public class MainApplication extends Application {


    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String status = NetworkUtil.getConnectivityStatusString(context);
            String action = intent.getAction();

            if(action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
                //notify the user
                //test, remove comment in production
                //showNetworkStatus(status);
            }
        }
    };



    @Override
    public void onCreate(){
        super.onCreate();

        //register broadcast receiver
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
    }

    private void showNetworkStatus(String status) {
        //View view = getWindow().getDecorView().findViewById(android.R.id.content);
        //Snackbar.make(view, "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();
        //or
        //Snackbar.make(findViewById(android.R.id.content), "Status MainActivity = " + status, Snackbar.LENGTH_LONG).show();

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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { //sdk=26

                }

                //Notify the user
                AlertDialog.Builder b = new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Connection status")
                        .setMessage("No Internet connection.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                //finish();
                            }
                        });
                Dialog d = b.create();
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);          //2003
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);   //2006
                //d.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);                 //2002
                d.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);                   //2005
                d.show();

                /*
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setMessage("No Internet connection");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        //finish();
                    }
                });
                        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alertDialog.create().show();
                */

                break;
        }
    }
}
