package com.google.amara.chattab.session;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthSession {

    private static final String PREF_NAME = "auth_session";

    private static final String KEY_JWT      = "jwt_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID  = "user_id";

    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;

    private AuthSession() {} // no instance

    public static void init(Context context) {
        if (prefs == null) {
            //prefs = context.getApplicationContext()
            //        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            //'MyPref' ---> 'MyPref.xml' will be found in /data/data/<package>/shared_prefs
            prefs  = context.getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
            editor = prefs.edit();
        }
    }

    // ───────── JWT ─────────

    public static void saveJwtToken(String token) {
        prefs.edit().putString(KEY_JWT, token).apply();
    }

    public static String getJwtToken() {
        return prefs.getString(KEY_JWT, null);
    }

    public static void clearJwtToken() {
        prefs.edit().remove(KEY_JWT).apply();
    }

    // ───────── USER ─────────

    public static void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public static String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public static void clearAll() {
        prefs.edit().clear().apply();
    }
}

