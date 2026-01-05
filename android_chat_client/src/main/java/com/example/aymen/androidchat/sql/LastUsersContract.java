package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class LastUsersContract implements BaseColumns {
    private static final String TEXT_TYPE    = " TEXT NOT NULL";
    private static final String TEXT_TYPE1   = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER NOT NULL";
    private static final String BLOB_TYPE    = " BLOB NOT NULL";

    private static final String COMMA_SEP = ",";

    public static final Uri CONTENT_URI_LAST_USERS =
            FileContract.BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_LAST_USERS).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String MESSAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_LAST_USERS  + "/" + FileContract.PATH_LAST_USERS;
    public static final String MESSAGES_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_LAST_USERS + "/" + FileContract.PATH_LAST_USERS;

    // Define the 'users' table schema
    public static final String LAST_USERS_TABLE_NAME    = "chat_last_users";
    public static final String COLUMN_ID                = "_id";
    public static final String COLUMN_USER_ORIGINE      = "nicknameorigine";
    public static final String COLUMN_USER_TARGET       = "nicknametarget";
    public static final String COLUMN_TIME              = "time";      //current connection time

    public static final String SQL_CREATE_ENTRIES_LAST_USERS =
            "CREATE TABLE " + LAST_USERS_TABLE_NAME + " ("       +
                    COLUMN_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT"  + COMMA_SEP +
                    COLUMN_USER_ORIGINE + TEXT_TYPE                             + COMMA_SEP +
                    COLUMN_USER_TARGET  + TEXT_TYPE    + " UNIQUE"              + COMMA_SEP +
                    COLUMN_TIME         + INTEGER_TYPE +
            " )";

    public static final String SQL_DELETE_ENTRIES_LAST_USERS =
            "DROP TABLE IF EXISTS " + LAST_USERS_TABLE_NAME;

    // Define a function to build a URI to find a specific 'user' by it's identifier
    public static Uri buildUserUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_LAST_USERS, id);
    }
}
