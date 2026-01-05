package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class FileContract {

    //The name of the database on the device and its version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "chat.db";

    public static final String CONTENT_AUTHORITY = "com.example.aymen.androidchat.sql.contentprovider";

    public static final Uri BASE_CONTENT_URI =
            Uri.parse("content://" + CONTENT_AUTHORITY);

     // A list of possible paths that will be appended to the base URI for each of the different tables.public static final String PATH_USERS           = "users";
    public static final String PATH_USERS           = "users";
    public static final String PATH_MESSAGES        = "messages";
    public static final String PATH_LAST_USERS      = "lastusers";
    public static final String PATH_PROFILE_HISTORY = "profilehistory";
    public static final String PATH_CREDENTIALS     = "credentials";
}