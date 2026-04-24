package com.google.amara.chattab.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.amara.chattab.entities.UserUiState;

import java.util.List;

@Dao
public interface UserUiStateDao {

    @Query("SELECT * FROM user_ui_state WHERE userId = :userId")
    UserUiState getByUserId(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserUiState state);

    @Query("SELECT * FROM user_ui_state")
    LiveData<List<UserUiState>> getAll();
}
