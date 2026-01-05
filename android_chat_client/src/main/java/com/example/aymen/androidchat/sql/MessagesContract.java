package com.example.aymen.androidchat.sql;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class MessagesContract implements BaseColumns {
    public static final String PATH_USERS    = "users";
    public static final String PATH_MESSAGES = "messages";

    private static final String TEXT_TYPE    = " TEXT NOT NULL";
    private static final String TEXT_TYPE1   = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER NOT NULL";
    private static final String BLOB_TYPE    = " BLOB NOT NULL";

    private static final String COMMA_SEP        = ",";
    public static final Uri CONTENT_URI_MESSAGES =
            FileContract.BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_MESSAGES).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String MESSAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_MESSAGES  + "/" + FileContract.PATH_MESSAGES;
    public static final String MESSAGES_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_MESSAGES + "/" + FileContract.PATH_MESSAGES;

    // Define the 'messages' table schema
    public static final String MESSAGES_TABLE_NAME = "chat_messages";
    public static final String COLUMN_ID        = "_id";
    public static final String COLUMN_FROM 	    = "frome";  //from est un mot clé
    public static final String COLUMN_TO 	    = "too";
    public static final String COLUMN_MESSAGE   = "message";
    public static final String COLUMN_REFERENCE = "reference";
    public static final String COLUMN_DATE 		= "date";
    public static final String COLUMN_EXTRA     = "extra";      //thumb image string base-64
    public static final String COLUMN_EXTRANAME = "extraname";  //filename of image from witch the thumb is extracted
    public static final String COLUMN_MIME      = "mime";       //mime type of attachment
    public static final String COLUMN_SEEN      = "seen";       //the message is seen = '1' or not = '0'
    public static final String COLUMN_DELETED_FROM = "deleted_from";    //the message is deleted by 'from', values : 0, 1
    public static final String COLUMN_DELETED_TO   = "deleted_to";     //the message is deleted by 'to', values : 0, 1

    public static final String SQL_CREATE_ENTRIES_MESSAGES =
            "CREATE TABLE " + MESSAGES_TABLE_NAME + " (" +
                    COLUMN_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    COLUMN_FROM        + TEXT_TYPE     + COMMA_SEP +
                    COLUMN_TO          + TEXT_TYPE     + COMMA_SEP +
                    COLUMN_MESSAGE     + TEXT_TYPE     + COMMA_SEP +
                    COLUMN_REFERENCE   + TEXT_TYPE + " UNIQUE "    + COMMA_SEP +
                    COLUMN_DATE        + INTEGER_TYPE  + COMMA_SEP +
                    COLUMN_EXTRA       + TEXT_TYPE1    + COMMA_SEP +  //may be null
                    COLUMN_EXTRANAME   + TEXT_TYPE1    + COMMA_SEP +  // may be null
                    COLUMN_MIME        + TEXT_TYPE1    + COMMA_SEP +  // may be null
                    COLUMN_SEEN        + TEXT_TYPE     + COMMA_SEP +
                    COLUMN_DELETED_FROM + TEXT_TYPE    + COMMA_SEP +
                    COLUMN_DELETED_TO   + TEXT_TYPE    +
            " )";


    public static final String SQL_DELETE_ENTRIES_MESSAGES =
            "DROP TABLE IF EXISTS " + MESSAGES_TABLE_NAME;

    // Define a function to build a URI to find a specific 'message' by it's identifier
    public static Uri buildMessageUri(long id){
        return ContentUris.withAppendedId(CONTENT_URI_MESSAGES, id);
    }
}
