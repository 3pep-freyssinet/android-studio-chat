package com.google.amara.chattab.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_ui_state")
public class UserUiState {

    @PrimaryKey
    @NonNull
    public String userId;

    public long lastRejectedAt;

    public UserUiState(@NonNull String userId, long lastRejectedAt) {
        this.userId         = userId;
        this.lastRejectedAt = lastRejectedAt;
    }
}
