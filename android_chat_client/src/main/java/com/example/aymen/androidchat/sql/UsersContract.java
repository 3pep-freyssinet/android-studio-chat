package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class UsersContract implements BaseColumns {
    private static final String TEXT_TYPE    = " TEXT NOT NULL";
    private static final String TEXT_TYPE1   = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER NOT NULL";
    private static final String BLOB_TYPE    = " BLOB NOT NULL";
    private static final String BLOB_TYPE1   = " BLOB";

    private static final String COMMA_SEP = ",";

    public static final Uri CONTENT_URI_USERS =
            FileContract.BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_USERS).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String MESSAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_USERS  + "/" + FileContract.PATH_USERS;
    public static final String MESSAGES_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_USERS + "/" + FileContract.PATH_USERS;

    // Define the 'users' table schema
    public static final String USERS_TABLE_NAME         = "chat_users";
    public static final String COLUMN_ID                = "_id";
    public static final String COLUMN_USER_NAME         = "nickname";
    //public static final String COLUMN_CHAT_ID         = "chatid";          //id in the chat
    public static final String COLUMN_PROFILE           = "imageprofile";    //thumb image string base-64
    public static final String COLUMN_STATUS            = "status";          //0=gone, 1=connect, 2=standby, 3=blacklist
    public static final String COLUMN_NOT_SEEN	        = "notseen";         //number of not seen messages
    public static final String COLUMN_CONNECTED         = "connected";       //current connection time
    public static final String COLUMN_DISCONNECTED_AT   = "disconnectedat";  //disconnection time
    public static final String COLUMN_BLACKLIST_AUTHOR  = "blacklistauthor"; //blacklist author


    public static final String SQL_CREATE_ENTRIES_USERS =
            "CREATE TABLE " + USERS_TABLE_NAME + " ("       +
                    COLUMN_ID                   + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    COLUMN_USER_NAME            + TEXT_TYPE + " UNIQUE" + COMMA_SEP +
                    //COLUMN_CHAT_ID            + TEXT_TYPE             + COMMA_SEP +
                    COLUMN_PROFILE              + BLOB_TYPE1            + COMMA_SEP +   //may be null
                    COLUMN_STATUS               + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_NOT_SEEN             + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_CONNECTED            + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_DISCONNECTED_AT      + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_BLACKLIST_AUTHOR     + TEXT_TYPE1                        +    //may be null
            " )";

    public static final String SQL_DELETE_ENTRIES_USERS =
            "DROP TABLE IF EXISTS " + USERS_TABLE_NAME;

    // Define a function to build a URI to find a specific 'user' by it's identifier
    public static Uri buildUserUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_USERS, id);
    }
}
