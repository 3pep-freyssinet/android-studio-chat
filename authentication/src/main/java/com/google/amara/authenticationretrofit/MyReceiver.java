package com.google.amara.authenticationretrofit;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

interface BroadcastNotification {
    public void sendNetworkNotification(String status);
}

public class MyReceiver extends BroadcastReceiver {
    Dialog   dialog;
    TextView nettext;
    BroadcastNotification broadcastNotification;

    public MyReceiver(){
        super();
    }

    //constructor
    public MyReceiver(BroadcastNotification broadcastNotification){
        this.broadcastNotification = broadcastNotification;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String status = NetworkUtil.getConnectivityStatusString(context);
        String action = intent.getAction();

        if(action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
            //notify the user
            broadcastNotification.sendNetworkNotification(status);
        }



        //MainActivity mainActivity = (MainActivity)broadcastNotification;
        //View view = mainActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        //Snackbar.make(view,"Settings saved successfully",Snackbar.LENGTH_LONG ).show();

        //Intent i = new Intent(context, BroadcastNotification1.class);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //context.startActivity(i);

  /* exception cannot show a AlertDialog inside 'onReceive'
        new AlertDialog.Builder(context)
                .setTitle("Connection status")
                .setMessage(status)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        

        //There is an exception : 'context cannot be cast to Activity'
        Activity activity = (MainActivity) context;
        View view = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(view,"Settings saved successfully",Snackbar.LENGTH_LONG ).show();

        dialog        = new Dialog(context, android.R.style.Theme_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.custom_dialog0);
        Button restartapp = (Button)dialog.findViewById(R.id.restartapp);
        nettext =(TextView)dialog.findViewById(R.id.nettext);

        restartapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity) context).finish();
                Log.d("clickedbutton","yes");
                Intent i = new Intent(context, MainActivity.class);
                context.startActivity(i);

            }
        });
        Log.d("network",status);
        if(status.isEmpty()||status.equals("No internet is available")||status.equals("No Internet Connection")) {
            status="No Internet Connection";
            dialog.show();
        }
        */

        //Toast.makeText(context, status, Toast.LENGTH_LONG).show();

    }
}
