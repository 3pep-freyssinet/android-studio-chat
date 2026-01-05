package com.google.amara.chattab;

import android.content.SharedPreferences;

public class PreferencesHandler {
    public static SharedPreferences sharedPreferences;
    public static synchronized SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }

    public static synchronized void setSharedPreferences(SharedPreferences sharedPreferences){
        PreferencesHandler.sharedPreferences = sharedPreferences;
    }
}