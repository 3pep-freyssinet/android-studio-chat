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

    @Query("UPDATE messages SET remoteUrl = :url WHERE localId = :localId")
    void updateRemoteUrl(String localId, String url);

    @Query("UPDATE messages SET sent_at = 'uploading...' WHERE localId = :localId")
    void markUploading(String localId);

    @Query("SELECT * FROM messages WHERE type = 'image' AND remoteUrl IS NULL AND pending = 1 ORDER BY uid ASC")
    List<ChatMessage> getPendingImageUploads();

    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    void updateStatus(String localId, String status);

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE id_from = :withUserId
        AND id_to = :myUserId
        AND status != 'seen'
     """)
    int countUnreadMessages(String withUserId, String myUserId);

    @Query("""
        UPDATE messages
        SET status = 'seen'
        WHERE id_from = :withUserId
        AND id_to = :myUserId
        AND status != 'seen'
""")
    void markConversationSeen(String withUserId, String myUserId);


    @Query("""
UPDATE messages SET
    remoteUrl = :remoteUrl,
    sent_at   = :sentAt,
    status    = :status,
    pending   = 0
WHERE localId = :localId
""")
    void confirmMessage(
            String localId,
            String remoteUrl,
            String sentAt,
            String status
    );


    @Query("""
        UPDATE messages
        SET
            remoteUrl = :remoteUrl,
            pending   = 0,
            sent_at   = :sentAt,
            status    = :status
        WHERE localId = :localId
    """)

        void confirmMessage_(
                String localId,
                String remoteUrl,
                String sentAt,
                String status
        );

    @Query("UPDATE messages SET status = :status WHERE serverId = :serverId")
    void updateStatusByServerId(int serverId, String status);

    @Query("""
        SELECT id_from, COUNT(*) as unreadCount
        FROM messages
        WHERE id_to = :me
        AND status != 'seen'
        GROUP BY id_from
""")
    List<UnreadCount> getUnreadCounts(String me);

}
