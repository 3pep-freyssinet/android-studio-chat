package com.google.amara.chattab.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.amara.chattab.ChatUser;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users ORDER BY nickname")
    LiveData<List<ChatUser>> getFriendUsers();


    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    ChatUser getUserById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatUser user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChatUser> users);

    @Query("DELETE FROM users WHERE userId = :id")
    void delete(String id);

    @Query("SELECT COUNT(*) FROM users WHERE userId = :id")
    int exists(String id);

    @Query("UPDATE users SET relationStatus = :status WHERE userId = :userId")
    void updateRelationStatus(String userId, String status);

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteById(String userId);

    @Query("UPDATE users SET relationStatus = 'pending' WHERE userId = :fromId")
    void insertOrUpdatePending(String fromId);

    @Query("UPDATE users SET relationStatus = 'rejected' WHERE userId = :userId")
    void setRejected(String userId);

    @Query("UPDATE users SET relationStatus = 'accepted' WHERE userId = :userId")
    void setAccepted(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    LiveData<ChatUser> getUserByIdLive(String userId);
}