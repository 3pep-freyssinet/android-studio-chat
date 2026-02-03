package com.google.amara.chattab.dao;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.amara.chattab.ChatMessage;

@Database(entities = {ChatMessage.class}, version = 1)
public abstract class ChatDatabase extends RoomDatabase {

    private static volatile ChatDatabase INSTANCE;

    public abstract MessageDao messageDao();

    public static ChatDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    ChatDatabase.class,
                    "chat_db"
            ).build();
        }
        return INSTANCE;
    }
}
