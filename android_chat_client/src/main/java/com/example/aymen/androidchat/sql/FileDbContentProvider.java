package com.example.aymen.androidchat.sql;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Credentials;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FileDbContentProvider extends ContentProvider {


    private SQLiteDatabase db;
    FileDbHelper dbHelper;

    // Use an int for each URI we will run, this represents the different queries
    private static final int USERS              = 100;
    private static final int USER_ID            = 101;
    private static final int MESSAGES           = 200;
    private static final int MESSAGE_ID         = 201;
    private static final int LAST_USERS         = 300;
    private static final int LAST_USER_ID       = 301;
    private static final int PROFILE_HISTORY    = 400;
    private static final int PROFILE_HISTORY_ID = 401;
    private static final int CREDENTIALS        = 500;
    private static final int CREDENTIALS_ID     = 501;

    // authority is the symbolic name of your provider
    // To avoid conflicts with other providers, you should use
    // Internet domain ownership (in reverse) as the basis of your provider authority.
    // It must be the same as in manifest.
    private static final String AUTHORITY = FileContract.CONTENT_AUTHORITY;

    // create content URIs from the authority by appending path to database table
    public static final Uri BASE_CONTENT_URI =
            //Uri.parse("content://" + AUTHORITY + "/cache_table");
            Uri.parse("content://" + AUTHORITY);

    public static final Uri CONTENT_URI_USERS =
            BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_USERS).build();

    public static final Uri CONTENT_URI_MESSAGES =
            BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_MESSAGES).build();

    public static final Uri CONTENT_URI_LAST_USERS =
            BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_LAST_USERS).build();

    public static final Uri CONTENT_URI_PROFILE_HISTORY =
            BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_PROFILE_HISTORY).build();

    public static final Uri CONTENT_URI_CREDENTIALS =
            BASE_CONTENT_URI.buildUpon().appendPath(FileContract.PATH_CREDENTIALS).build();

    // These are special type prefixes that specify if a URI returns a list or a specific item.
    public static final String USERS_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_USERS  + "/" + FileContract.PATH_USERS;
    public static final String USERS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_USERS + "/" + FileContract.PATH_USERS;

    public static final String MESSAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_MESSAGES  + "/" + FileContract.PATH_MESSAGES;
    public static final String MESSAGES_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_MESSAGES + "/" + FileContract.PATH_MESSAGES;

    public static final String LAST_USERS_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_LAST_USERS  + "/" + FileContract.PATH_LAST_USERS;
    public static final String LAST_USERS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_LAST_USERS + "/" + FileContract.PATH_LAST_USERS;

    public static final String PROFILE_HISTORY_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_PROFILE_HISTORY  + "/" + FileContract.PATH_PROFILE_HISTORY;
    public static final String PROFILE_HISTORY_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_PROFILE_HISTORY + "/" + FileContract.PATH_PROFILE_HISTORY;

    public static final String CREDENTIALS_CONTENT_TYPE =
            "vnd.android.cursor.dir/" + CONTENT_URI_CREDENTIALS  + "/" + FileContract.PATH_CREDENTIALS;
    public static final String CREDENTIALS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/" + CONTENT_URI_CREDENTIALS + "/" + FileContract.PATH_CREDENTIALS;



    // A content URI pattern matches content URIs using wildcard characters:
    // *: Matches a string of any valid characters of any length.
    // #: Matches a string of numeric characters of any length.
    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(AUTHORITY, FileContract.PATH_USERS, USERS);
        uriMatcher.addURI(AUTHORITY, FileContract.PATH_USERS + "/#", USER_ID);

        uriMatcher.addURI(AUTHORITY, FileContract.PATH_MESSAGES, MESSAGES);
        uriMatcher.addURI(AUTHORITY, FileContract.PATH_MESSAGES + "/#", MESSAGE_ID);

        uriMatcher.addURI(AUTHORITY, FileContract.PATH_LAST_USERS, LAST_USERS);
        uriMatcher.addURI(AUTHORITY, FileContract.PATH_LAST_USERS + "/#", LAST_USER_ID);

        uriMatcher.addURI(AUTHORITY, FileContract.PATH_PROFILE_HISTORY, PROFILE_HISTORY);
        uriMatcher.addURI(AUTHORITY, FileContract.PATH_PROFILE_HISTORY + "/#", PROFILE_HISTORY_ID);

        uriMatcher.addURI(AUTHORITY, FileContract.PATH_CREDENTIALS, CREDENTIALS);
        uriMatcher.addURI(AUTHORITY, FileContract.PATH_CREDENTIALS + "/#", CREDENTIALS_ID);
    }

    public FileDbContentProvider() {
        super();
    }

    @Override
    public boolean onCreate() {
        // get access to the database helper
        dbHelper = new FileDbHelper(getContext());
        //If you do use SQLiteOpenHelper, make sure to avoid calling
        // android.database.sqlite.SQLiteOpenHelper.getReadableDatabase or
        // android.database.sqlite.SQLiteOpenHelper.getWritableDatabase from this method.
        // (Instead, override android.database.sqlite.SQLiteOpenHelper.onOpen to initialize
        // the database when it is first opened.)
        //SQLiteDatabase db = dbHelper.getWritableDatabase();
        //db = dbHelper.getWritableDatabase();
        //return (db == null)? false : true;
        return  true;

    }

    private long getId(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment != null) {
            try {
                return Long.parseLong(lastPathSegment);
            } catch (NumberFormatException e) {
                Log.e("aa Provider", "Number Format Exception : " + e);
            }
        }
        return -1;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        long id                 = getId(uri);
        SQLiteDatabase db       = dbHelper.getReadableDatabase();
        Cursor retCursor;
        switch(uriMatcher.match(uri)) {
            case USERS:
                try{
                    retCursor = db.query(
                            UsersContract.USERS_TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                }catch (SQLiteException e){
                    retCursor = null;
                }

                break;

            case USER_ID:
                long _id = ContentUris.parseId(uri);
                try{
                    retCursor = db.query(
                            UsersContract.USERS_TABLE_NAME,
                            projection,
                            UsersContract.COLUMN_ID + " = ?",
                            new String[]{String.valueOf(_id)},
                            null,
                            null,
                            sortOrder
                    );
                }catch (SQLiteException e){
                    retCursor = null;
                }
                break;

            case MESSAGES:
                retCursor = db.query(
                        MessagesContract.MESSAGES_TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MESSAGE_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        MessagesContract.MESSAGES_TABLE_NAME,
                        projection,
                        MessagesContract.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;

            case LAST_USERS:
                try{
                    retCursor = db.query(
                            LastUsersContract.LAST_USERS_TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                }catch (SQLiteException e){
                    retCursor = null;
                }

                break;

            case LAST_USER_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        LastUsersContract.LAST_USERS_TABLE_NAME,
                        projection,
                        LastUsersContract.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;

            case PROFILE_HISTORY:
                try{
                    retCursor = db.query(
                            ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                }catch (SQLiteException e){
                    retCursor = null;
                }
                break;

            case PROFILE_HISTORY_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME,
                        projection,
                        ProfileHistoryContract.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;

            case CREDENTIALS:
                try{
                    retCursor = db.query(
                            CredentialsContract.CREDENTIALS_TABLE_NAME,
                            projection,
                            selection,
                            selectionArgs,
                            null,
                            null,
                            sortOrder
                    );
                }catch (SQLiteException e){
                    retCursor = null;
                }
                break;

            case CREDENTIALS_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        CredentialsContract.CREDENTIALS_TABLE_NAME,
                        projection,
                        CredentialsContract.COLUMN_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;

            default:
                throw new UnsupportedOperationException("'Query' Unknown uri: " + uri);
        }

        // Set the notification URI for the cursor to the one passed into the function. This
        // causes the cursor to register a content observer to watch for changes that happen to
        // this URI and any of it's descendants. By descendants, we mean any URI that begins
        // with this path.
        if(retCursor != null)retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    //Return the MIME type corresponding to a content URI
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch(uriMatcher.match(uri)) {
            case USERS:
                return USERS_CONTENT_TYPE;
            case USER_ID:
                return USERS_CONTENT_ITEM_TYPE;

            case MESSAGES:
                return MESSAGES_CONTENT_TYPE;
            case MESSAGE_ID:
                return MESSAGES_CONTENT_ITEM_TYPE;

            case LAST_USERS:
                return LAST_USERS_CONTENT_TYPE;
            case LAST_USER_ID:
                return LAST_USERS_CONTENT_ITEM_TYPE;

            case PROFILE_HISTORY:
                return PROFILE_HISTORY_CONTENT_TYPE;
            case PROFILE_HISTORY_ID:
                return PROFILE_HISTORY_CONTENT_ITEM_TYPE;

            case CREDENTIALS:
                return CREDENTIALS_CONTENT_TYPE;
            case CREDENTIALS_ID:
                return CREDENTIALS_CONTENT_ITEM_TYPE;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long _id;
        Uri returnUri;
        switch (uriMatcher.match(uri)) {

            case USERS:
                    //_id = db.insert(UsersContract.USERS_TABLE_NAME, null, values);
                /*
                insertOrUpdateById(db, uri, "TEST",
                        values, "server_id");
                getContext().getContentResolver().notifyChange(uri, null, false);
                Uri uri1 = UsersContract.buildUserUri(0);
                */
                    _id = db.insertWithOnConflict(UsersContract.USERS_TABLE_NAME, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);

                    if(_id > 0){
                        returnUri =  ContentUris.withAppendedId(CONTENT_URI_USERS, _id);
                    } else{
                        returnUri = null;
                        //throw new UnsupportedOperationException("INSERT Table 'users' Unable to insert rows into: " + uri);
                    }
                    break;
            case MESSAGES:
                    //_id = db.insert(MessagesContract.MESSAGES_TABLE_NAME, null, values);
                    _id = db.insertWithOnConflict(MessagesContract.MESSAGES_TABLE_NAME, null,
                        values, SQLiteDatabase.CONFLICT_REPLACE);
                    if(_id > 0){
                        returnUri = ContentUris.withAppendedId(CONTENT_URI_MESSAGES, _id);
                    } else{
                        throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                    }
                    break;

            case LAST_USERS:
                //_id = db.insert(LastUsersContract.LAST_USERS_TABLE_NAME, null, values);
                _id = db.insertWithOnConflict(LastUsersContract.LAST_USERS_TABLE_NAME, null,
                        values, SQLiteDatabase.CONFLICT_REPLACE);

                if(_id > 0){
                    returnUri =  ContentUris.withAppendedId(CONTENT_URI_LAST_USERS, _id);
                } else{
                    returnUri = null;
                    //throw new UnsupportedOperationException("INSERT Table 'users' Unable to insert rows into: " + uri);
                }
                break;

            case PROFILE_HISTORY:
                //_id = db.insert(ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME, null, values);
                _id = db.insertWithOnConflict(ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME, null,
                        values, SQLiteDatabase.CONFLICT_IGNORE);

                if(_id > 0){
                    returnUri =  ContentUris.withAppendedId(CONTENT_URI_PROFILE_HISTORY, _id);
                } else{
                    returnUri = null;
                    //throw new UnsupportedOperationException("INSERT Table 'users' Unable to insert rows into: " + uri);
                }
                break;

            case CREDENTIALS:
                //_id = db.insert(ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME, null, values);
                _id = db.insertWithOnConflict(CredentialsContract.CREDENTIALS_TABLE_NAME, null,
                        values, SQLiteDatabase.CONFLICT_REPLACE);

                if(_id > 0){
                    returnUri =  ContentUris.withAppendedId(CONTENT_URI_CREDENTIALS, _id);
                } else{
                    returnUri = null;
                    //throw new UnsupportedOperationException("INSERT Table 'users' Unable to insert rows into: " + uri);
                }
                break;

                default:
                    throw new UnsupportedOperationException("INSERT Table 'messages' Unknown uri: " + uri);
            }

        return returnUri;
    }

    /**
     * In case of a conflict when inserting the values, another update query is sent.
     *
     * @param db     Database to insert to.
     * @param uri    Content provider uri.
     * @param table  Table to insert to.
     * @param values The values to insert to.
     * @param column Column to identify the object.
     * @throws SQLException
     */
    private void insertOrUpdateById(SQLiteDatabase db, Uri uri, String table,
                                    ContentValues values, String column) throws SQLException {
        try {
            db.insertOrThrow(table, null, values);
        } catch (SQLiteConstraintException e) {
            int nrRows = update(uri, values, column + "=?",
                    new String[]{values.getAsString(column)});
            if (nrRows == 0)throw e;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Number of rows effected
        int rows;
        switch (uriMatcher.match(uri)) {

            case USERS:
                rows = db.delete(UsersContract.USERS_TABLE_NAME, selection, selectionArgs);
                break;

            case MESSAGES:
                rows = db.delete(MessagesContract.MESSAGES_TABLE_NAME, selection, selectionArgs);
                break;

            case LAST_USERS:
                rows = db.delete(LastUsersContract.LAST_USERS_TABLE_NAME, selection, selectionArgs);
                break;

            case PROFILE_HISTORY:
                rows = db.delete(ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME, selection, selectionArgs);
                break;

            case CREDENTIALS:
                rows = db.delete(CredentialsContract.CREDENTIALS_TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("'Delete' Unknown uri: " + uri);
        }

        // Because null could delete all rows:
        if(selection == null || rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Number of rows affected
        int rows;
        switch (uriMatcher.match(uri)) {

            case USERS:
                rows = db.update(UsersContract.USERS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case MESSAGES:
                rows = db.update(MessagesContract.MESSAGES_TABLE_NAME, values, selection, selectionArgs);
                break;

            case LAST_USERS:
                rows = db.update(LastUsersContract.LAST_USERS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case PROFILE_HISTORY:
                rows = db.update(ProfileHistoryContract.PROFILE_HISTORY_TABLE_NAME, values, selection, selectionArgs);
                break;

            case CREDENTIALS:
                rows = db.update(CredentialsContract.CREDENTIALS_TABLE_NAME, values, selection, selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("'Delete' Unknown uri: " + uri);
        }

        // Because null could delete all rows:
        if(selection == null || rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }
}
