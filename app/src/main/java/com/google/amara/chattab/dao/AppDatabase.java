package com.google.amara.chattab.dao;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.amara.chattab.ChatMessage;
import com.google.amara.chattab.ChatUser;

@Database(
        entities = {ChatMessage.class, ChatUser.class},
        version = 4,   // ⬅️ increment
        exportSchema = true
)

public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();
    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    /*
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "chat_database"
                            )
                            .fallbackToDestructiveMigration() // 🔥 OK during dev
                            .build();
                    */

                    /*
                    INSTANCE = Room.databaseBuilder(context, AppDatabase.class, "chat_db")
                            .addMigrations(MIGRATION_2_3)
                            .build();
                    */

                    INSTANCE = Room.databaseBuilder(context, AppDatabase.class, "chat_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // ✅ CREATE users table
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "userId TEXT NOT NULL, " +
                            "nickname TEXT, " +
                            "status INTEGER NOT NULL, " +
                            "isFriend INTEGER NOT NULL, " +
                            "PRIMARY KEY(userId))"
            );
        }
    };
}

