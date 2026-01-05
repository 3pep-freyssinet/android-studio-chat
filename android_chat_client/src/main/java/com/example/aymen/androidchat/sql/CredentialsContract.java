package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class CredentialsContract implements BaseColumns {
    private static final String TEXT_TYPE_NOT_NULL    = " TEXT NOT NULL";
    private static final String TEXT_TYPE             = " TEXT";
    private static final String VARCHAR_TYPE_NOT_NULL = " VARCHAR(" + 255 +") NOT NULL";
    private static final String INTEGER_TYPE          = " INTEGER NOT NULL";
    private static final String BLOB_TYPE             = " BLOB NOT NULL";
    private static final String BLOB_TYPE1            = " BLOB";

    private static final String COMMA_SEP = ",";

    public static final Uri CONTENT_URI_CREDENTIALS =
            FileContract.BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_CREDENTIALS).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String CREDENTIALS_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_CREDENTIALS  + "/" + FileContract.PATH_CREDENTIALS;
    public static final String CREDENTIALS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_CREDENTIALS + "/" + FileContract.PATH_CREDENTIALS;

    // Define the 'credentials' table schema
    public static final String CREDENTIALS_TABLE_NAME   = "chat_credentials";
    public static final String COLUMN_ID                = "_id";
    public static final String COLUMN_USERNAME         = "username";
    //public static final String COLUMN_CHAT_ID         = "chatid";          //id in the chat
    public static final String COLUMN_PWD               = "password";
    public static final String COLUMN_DATE              = "date";
    public static final String COLUMN_DATE_HISTORY      = "datehistory";
    public static final String COLUMN_PWD_HISTORY       = "pwdhistory";

    /*
    public static final String COLUMN_PROFILE           = "imageprofile";    //thumb image string base-64
    public static final String COLUMN_STATUS            = "status";          //0=gone, 1=connect, 2=standby, 3=blacklist
    public static final String COLUMN_NOT_SEEN	        = "notseen";         //number of not seen messages
    public static final String COLUMN_CONNECTED         = "connected";       //current connection time
    public static final String COLUMN_DISCONNECTED_AT   = "disconnectedat";  //disconnection time
    public static final String COLUMN_BLACKLIST_AUTHOR  = "blacklistauthor"; //blacklist author
    */

    public static final String SQL_CREATE_ENTRIES_CREDENTIALS =
            "CREATE TABLE " + CREDENTIALS_TABLE_NAME + " ("       +
                    COLUMN_ID                   + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    COLUMN_USERNAME             + TEXT_TYPE_NOT_NULL + " UNIQUE" + COMMA_SEP       +
                    COLUMN_PWD                  + TEXT_TYPE_NOT_NULL             + COMMA_SEP       +
                    COLUMN_DATE                 + TEXT_TYPE_NOT_NULL             + COMMA_SEP       +
                    COLUMN_DATE_HISTORY         + VARCHAR_TYPE_NOT_NULL          + COMMA_SEP       +
                    COLUMN_PWD_HISTORY          + VARCHAR_TYPE_NOT_NULL          +

                    /*
                    COLUMN_PROFILE              + BLOB_TYPE1            + COMMA_SEP +   //may be null
                    COLUMN_STATUS               + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_NOT_SEEN             + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_CONNECTED            + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_DISCONNECTED_AT      + INTEGER_TYPE          + COMMA_SEP +
                    COLUMN_BLACKLIST_AUTHOR     + TEXT_TYPE1                        +    //may be null
                    */
            " )";

    public static final String SQL_DELETE_ENTRIES_CREDENTIALS =
            "DROP TABLE IF EXISTS " + CREDENTIALS_TABLE_NAME;

    // Define a function to build a URI to find a specific 'user' by it's identifier
    public static Uri buildUserUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_CREDENTIALS, id);
    }
}
