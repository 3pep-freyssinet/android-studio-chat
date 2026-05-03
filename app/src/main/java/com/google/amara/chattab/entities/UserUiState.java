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

    // ✅ NEW
    public String relationStatus; // none, pending, accepted
    public boolean sentByMe;

    public UserUiState(@NonNull String userId,
                       long lastRejectedAt,
                       String relationStatus,
                       boolean sentByMe) {

        this.userId = userId;
        this.lastRejectedAt = lastRejectedAt;
        this.relationStatus = relationStatus;
        this.sentByMe = sentByMe;
    }
}
