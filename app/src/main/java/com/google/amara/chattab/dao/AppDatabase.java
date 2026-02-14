package com.google.amara.chattab.dao;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.amara.chattab.ChatMessage;

@Database(
        entities = {ChatMessage.class},
        version = 3,   // ⬅️ increment
        exportSchema = true
)

public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();

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

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // Rename column (SQLite supports this)
            db.execSQL(
                    "ALTER TABLE messages ADD COLUMN serverId INTEGER"
            );
        }
    };
}

