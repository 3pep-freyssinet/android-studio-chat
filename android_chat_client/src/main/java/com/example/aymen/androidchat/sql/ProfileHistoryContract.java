package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class ProfileHistoryContract implements BaseColumns {
    private static final String TEXT_TYPE    = " TEXT NOT NULL";
    private static final String TEXT_TYPE1   = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER NOT NULL";
    private static final String BLOB_TYPE    = " BLOB NOT NULL";
    private static final String BLOB_TYPE1   = " BLOB";

    private static final String COMMA_SEP = ",";

    public static final Uri CONTENT_URI_PROFILE_HISTORY =
            FileContract.BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_PROFILE_HISTORY).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String MESSAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_PROFILE_HISTORY  + "/" + FileContract.PATH_PROFILE_HISTORY;
    public static final String MESSAGES_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_PROFILE_HISTORY + "/" + FileContract.PATH_PROFILE_HISTORY;

    // Define the 'users' table schema
    public static final String PROFILE_HISTORY_TABLE_NAME = "chat_profile_history";
    public static final String COLUMN_ID                  = "_id";
    public static final String COLUMN_USER_NAME           = "nickname";
    public static final String COLUMN_URI_PROFILE         = "uriimageprofile"; //uri image profile
    public static final String COLUMN_TIME                = "time";           //date-time


    public static final String SQL_CREATE_ENTRIES_PROFILE_HISTORY =
            "CREATE TABLE " + PROFILE_HISTORY_TABLE_NAME + " ("       +
                    COLUMN_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT"  + COMMA_SEP +
                    COLUMN_USER_NAME    + TEXT_TYPE                             + COMMA_SEP +
                    COLUMN_URI_PROFILE  + TEXT_TYPE                             + COMMA_SEP +
                    COLUMN_TIME         + INTEGER_TYPE                                      +
            " )";

    public static final String SQL_DELETE_ENTRIES_PROFILE_HISTORY =
            "DROP TABLE IF EXISTS " + PROFILE_HISTORY_TABLE_NAME;

    // Define a function to build a URI to find a specific 'user' by it's identifier
    public static Uri buildUserUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_PROFILE_HISTORY, id);
    }
}
