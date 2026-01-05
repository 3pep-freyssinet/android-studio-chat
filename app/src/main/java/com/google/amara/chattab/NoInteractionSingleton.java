package com.google.amara.chattab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;

public final class NoInteractionSingleton { //extends Handler {

    //This instance field must be private and static
    private static NoInteractionSingleton INSTANCE;
    private String info = "Initial info class";
    private Runnable r; //used in handler
    private Handler handler = new Handler();

    //constructor must be private
    private NoInteractionSingleton() {
        info = "Hello, I am a string part of Singleton class";

        r = new Runnable() {
            @Override
            public void run() {
                // do something when the delay of 1 minute is reached. The delay is set in 'handler.postDelayed' below.
            }
        };
        startHandler();
    }

    //this method must be public and static since it is called within other classes
    public static NoInteractionSingleton getInstance() {
        if(INSTANCE == null) INSTANCE = new NoInteractionSingleton();
        return INSTANCE;
    }

    public void stopHandler() {
        handler.removeCallbacks(r);
    }

    public void startHandler() {
        handler.postDelayed(r, 1 * 60 * 1000); //for 1 minute
    }

    // getters
    // and setters
}