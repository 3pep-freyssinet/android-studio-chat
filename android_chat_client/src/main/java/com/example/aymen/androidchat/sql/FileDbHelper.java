package com.example.aymen.androidchat.sql;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//******************************** Sqlite **********************************************************
// It comes with Android SDK and is located in /tools folder of your install!
// SQLite comes bundled with the Android SDK, that means that SQLite version is changing with the
// increasing of the API level.
//************************** Clear debug mode ******************************************************
//cd C:\Users\aa\AppData\Local\Android\Sdk\platform-tools
// adb -s 2c0162cd shell ---> '2c0162cd' est le serial of device found with the command 'adb devices'
//shell@android:/ $
//shell@android:/ $ am clear-debug-app
//************************  webcam  ****************************************************************
//C:\Users\aa\AppData\Local\Android\Sdk\tools>emulator -webcam-list
//        List of web cameras connected to the computer:
//        Camera 'webcam0' is connected to device 'AndroidEmulatorVC0' on channel 0 using pixel format 'BGR4'
//
// C:\Users\aa\AppData\Local\Android\Sdk\tools>emulator -camera-front webcam0 -avd New_Device_API_24
//
//******************************** Start camera ****************************************************
//C:\Users\aa\AppData\Local\Android\Sdk\platform-tools>adb -d shell am start -a android.media.action.IMAGE_CAPTURE
//        Starting: Intent { act=android.media.action.IMAGE_CAPTURE }
//
//************ SQLITE3 MANAGER FOR EMULATOR ********************************************************
// cd : c:/users/aa/eclipse/sdk/platform-tools  ou android studio : C:\Users\aa\AppData\Local\Android\Sdk\platform-tools
// run : adb shell
// adb -d shell  --- to run device
// adb -e shell  ---> to run emulator
//
// En cas de plusieurs devices : emulator : adb -s emulator-5554 shell ou adb -s <device-reference> shell
// ou device-reference et la reference du device dans 'adb devices'. Par exemple : adb -s emulator-5554 shell
//
// En cas de problème "permission denied" se connecter en tant que 'root' : adb root
// ..> adb root
// Si message : 'adbd cannot run as root in production builds', il faut 'rooter' le device.
// ..>adb root restarting adbd as root
// ...> adb shell
//
// Si le prompt se présente sous la forme : 'generic_x86:/ $' lancer adb en mode root.
// lancer adb en mode root :
// C:\Users\aa\AppData\Local\Android\Sdk\platform-tools>adb -s emulator-5554 root   ----> 'restarting adbd as root'
// lancer le shell adb : adb -s emulator-5554 shell
// generic_x86:/ #
// Faire attention au '#' dans le prompt.
//  root@generic:/ #
//  1|root@generic:/ # sqlite3 -help
// get version
// root@generic:/ # sqlite3 --version ----> 3.9.2 2017-07-21 07:45:23 69906880cee1f246cce494672402e0c7f29bd4ec19c437d26d603870d2bd625d
//
//sqlite> .exit
//sqlite> .help
//
//  Pour entrer dans la database, après le prompt "1|root@generic:/ #"
// (Saisir le chemin : /data/data/<package>/databases/<nom-de-la-base-de-données>
// Par exemple : 1|root@generic:/ # sqlite3 /data/data/<package>>/databases/FeedReader.db
//
// root@generic:/ # sqlite3 /data/data/com.google.amara.android_database (le promp se coupe et
// la ligne suivante apparait :
// sqlite> .exit
// Continuer la saisie.
// amara.android_database/databases/FeedReader.db
// amara.android_database/databases/FeedReader.db
//
// On arrive au prompt de sqlite.
// SQLite version 3.7.11 2012-03-20 11:35:50
// Enter ".help" for instructions
// Enter SQL statements terminated with a semi colon ";"
// sqlite>
// ***** Exemple1 **********************************************************************************
// sqlite> .database
// seq  name             file
//---  ---------------  ----------------------------------------------------------
// 0    main             /data/data/com.google.amara.android_database/databases/Fee
// sqlite>
// ***** schema ************************************************************************************
// sqlite> .schema <table name>
// ***** Exemple2***********************************************************************************
// sqlite> .tables
// android_metadata  student
// sqlite>
//
//**********Exemple 2-1 compter le nombre de records ***********************************************
// sqlite> select count(*) from cache_table;
//
//********* Exemple 3  SQL COMMAND *****************************************************************
// sqlite> SELECT * FROM student;
// *************************************************************************************************
// sqlite> SELECT _id, nom, prenom, code_postal FROM student WHERE _id=20;
//**************************************************************************************************
// sqlite> INSERT INTO student VALUES(103,'aaaa','bbb'); //add row
// *************************************************************************************************
// sqlite> DELETE FROM student WHERE _id > 102;  // 'id' est la clé primaire, dans le sql on écrit '_id'.
// sqlite> DELETE FROM student; //delete all rows
// *************************************************************************************************
// sqlite> UPDATE student set 'student_title'='toto' where _id=102; //update one column
//**************************************************************************************************
// sqlite> UPDATE student SET student_title='aaaa', student_content='bbbbb' WHERE _id=102; //update two column
//**************************************************************************************************
// sqlite> ALTER TABLE student ADD  date varchar(12);
//**************************************************************************************************
// sqlite> .schema
// CREATE TABLE android_metadata (locale TEXT);
// CREATE TABLE student (_id INTEGER PRIMARY KEY AUTOINCREMENT,student_title TEXT NOT NULL,student_content TEXT NOT NULL );
//**************************************************************************************************
//C:\Users\aa\eclipse\SDK\platform-tools>adb push c:\\users\aa\Documents\2ARCU.csv \data\data\com.google.amara.android_database\databases
//adb: error: failed to copy 'c:\\users\aa\Documents\2ARCU.csv' to '\data\data\com;google.amara.android_database\databases': Read-only file system
//**************************************************************************************************
//sqlite> drop table student;
//**************************************************************************************************
//      Sortie
//
//sqlite> .exit
//sqlite> .help
////////////////////////////////////////////////////////////////////////////////////////////////////

public class FileDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    //public static final int DATABASE_VERSION = 1;
    //public static final String DATABASE_NAME = "chat.db";
    //All databases are stored on the device folder : '/data/data/<package_name>/databases'.

    //Constructor*******************************************************************************************
    //DATABASE_VERSION
    //Number of the database (starting at 1); if the database is older, onUpgrade(SQLiteDatabase, int, int) will be used to upgrade the database;
    //if the database is newer, onDowngrade(SQLiteDatabase, int, int) will be used to downgrade the database
    //*********************************************************************************************************

    public FileDbHelper(Context context) {
        super(context, FileContract.DATABASE_NAME, null, FileContract.DATABASE_VERSION);
    }
    //*******************************************************************************************
        //The first time the method "getWritableDatabase" is called, the database will be opened and
        //the "onCreate" method is called.
        //******************************************************************************************
    public void onCreate(SQLiteDatabase db) {
        String SQLString               = MessagesContract.SQL_CREATE_ENTRIES_MESSAGES;              //'chat_messages'
        String SQLStringUsers          = UsersContract.SQL_CREATE_ENTRIES_USERS;                    //'chat_users'
        String SQLStringLastUsers      = LastUsersContract.SQL_CREATE_ENTRIES_LAST_USERS;           //'chat_last_users'
        String SQLStringProfileHistory = ProfileHistoryContract.SQL_CREATE_ENTRIES_PROFILE_HISTORY; //'chat_history_profile'
        String SQLStringCredentials    = CredentialsContract.SQL_CREATE_ENTRIES_CREDENTIALS;        //'chat_credentials'

        db.execSQL(SQLString);
        db.execSQL(SQLStringUsers);
        db.execSQL(SQLStringLastUsers);
        db.execSQL(SQLStringProfileHistory);
        db.execSQL(SQLStringCredentials);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over.
        db.execSQL(MessagesContract.SQL_DELETE_ENTRIES_MESSAGES);               //'chat_messages'
        db.execSQL(UsersContract.SQL_DELETE_ENTRIES_USERS);                     //'chat_users'
        db.execSQL(LastUsersContract.SQL_DELETE_ENTRIES_LAST_USERS);            //'chat_users'
        db.execSQL(ProfileHistoryContract.SQL_DELETE_ENTRIES_PROFILE_HISTORY);  //'chat_profile_history'
        db.execSQL(CredentialsContract.SQL_DELETE_ENTRIES_CREDENTIALS);         //'chat_credentiaals'
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    //Called when the database has been opened
    public void onOpen(SQLiteDatabase db){}
}
