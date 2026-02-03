package com.google.amara.chattab.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.google.amara.chattab.ChatMessage;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessage message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChatMessage> messages);

    /*
    @Query("SELECT * FROM messages WHERE (id_from = :me AND id_to = :friend) OR (id_from = :friend AND id_to = :me) ORDER BY uid ASC")
    LiveData<List<ChatMessage>> getConversation(String me, String friend);
    */

    @Update
    void update(ChatMessage message);


    // 📥 Insert a message (sent or received)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessage message);

    // 📥 Insert list (useful when loading history later)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<ChatMessage> messages);

    // 🗑 Optional: clear conversation (not required now)
    @Query("DELETE FROM messages WHERE id_from = :me AND id_to = :friend OR id_from = :friend AND id_to = :me")
    void deleteConversation(String me, String friend);

    // 📖 Load conversation between 2 users
    @Query("SELECT * FROM messages " +
            "WHERE (id_from = :me AND id_to = :friend) " +
            "   OR (id_from = :friend AND id_to = :me) " +
            "ORDER BY sent_at ASC")
    LiveData<List<ChatMessage>> getConversation(String me, String friend);

    @Query("SELECT * FROM messages WHERE pending = 1 ORDER BY uid ASC")
    List<ChatMessage> getPendingMessages();

}
