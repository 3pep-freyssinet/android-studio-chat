package com.google.amara.chattab;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Created by
 */
@Entity(tableName = "users")
public class ChatUser implements Serializable,
                                 Parcelable {

    //Status constants
    public static final int userGone        = 0;    //red   bit  = 000
    public static final int userConnect     = 1;    //green bit  = 001
    public static final int userStandby     = 2;    //orange bit = 010
    public static final int userBlacklist   = 3;    //black  bit = 011
    //public static final int userAbsent    = 4;    //       bit = 100

    @PrimaryKey
    @NonNull
    public String userId;

    public boolean isFriend;  // optional but VERY useful
    public boolean isPending; // optional but VERY useful
    private boolean isRejected;

    public String nickname;             // nickname
    //public String chatId;               // id in the chat
    public String imageProfile;         //image encoded base 64 string.
    //public boolean firstTimeAccessDatabase;//limit access to db one time only

    @ColumnInfo(name = "status")
    //public int  status;          // no longer used. 0=gone, 1=connected,  2=standby, 3=blacklist
    public int         onlineStatus;    // from chat.users: online, offline, busy
    public String      relationStatus;  // from user_friends: pending, accepted, rejected

    public int    notSeenMessagesNumber;//number of not seen messages
    public String connectedAt;          //current connection time
    public String lastConnectedAt;      //last connection time
    public String disconnectedAt;       //disccnnection time
    public String blacklistAuthor;      // the author of blacklist


    public ChatUser() {}

    public ChatUser(@NonNull String userId,
                    String nickname,
                    //String chatId,
                    //String imageprofile,
                    //boolean firstTimeAccessDatabase,
                    //int status,
                    int onlineStatus,
                    String relationStatus,
                    int notSeenMessagesNumber
                    //String connectedAt,
                    //String disconnectedAt,
                    //String lastConnectedAt,
                    //String blacklistAuthor
    ) {
        this.userId   = userId;
        this.nickname = nickname;
        //this.imageProfile   = imageprofile;

        //this.chatId         = chatId;
        //this.firstTimeAccessDatabase = firstTimeAccessDatabase;
        //this.status = status;
        this.onlineStatus   = onlineStatus;
        this.relationStatus = relationStatus;
        this.notSeenMessagesNumber  = notSeenMessagesNumber;
        //this.connectedAt            = connectedAt;
        //this.disconnectedAt         = disconnectedAt;
        //this.lastConnectedAt        = lastConnectedAt;
        //this.blacklistAuthor        = blacklistAuthor;
    }

    //nickname
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    //id
    //public String getChatId() {
    //    return chatId;
    //}

    //public void setChatId(String chatId) {
    //    this.chatId = chatId;
    //}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    //image profile
    public String getImageProfile() {
        return imageProfile;
    }

    public void setImageProfile(String imageProfile) {
        this.imageProfile = imageProfile;
    }

    //first time access database
    /*
    public boolean getFirstTimeAccessDatabase() {
        return firstTimeAccessDatabase;
    }

    public void setFirstTimeAccessDatabase(boolean firstTimeAccessDatabase) {
        this.firstTimeAccessDatabase = firstTimeAccessDatabase;
    }
    */


    public int getOnlineStatus() {
        return onlineStatus;
    }
    public void setOnlineStatus(int onlineStatus){
        this.onlineStatus = onlineStatus;
    }

    public String getRelationStatus() {
        return relationStatus;
    }
    public void setRelationStatus(String relationStatus){
        this.relationStatus = relationStatus;
    }

    //Number of not seen messages
    public int getNotSeenMessagesNumber() {
        return notSeenMessagesNumber;
    }

    public void setNotSeenMessagesNumber(int notSeenMessagesNumber) {
        this.notSeenMessagesNumber = notSeenMessagesNumber;
    }

    //time connection
    public String getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(String connectedAt) {
        this.connectedAt = connectedAt;
    }

    //time disconnection
    public String getDisconnectedAt() {
        return disconnectedAt;
    }

    //disconnect time
    public void setDisconnectedAt(String disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    //last connection time
    public String getLastConnectedAt() {
        return lastConnectedAt;
    }

    public void setLastConnectedAt(String lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }


    //The blacklist author
    public String getBlacklistAuthor() {
        return blacklistAuthor;
    }

    public void setBlacklistAuthor(String blacklistAuthor) {
        this.blacklistAuthor = blacklistAuthor;
    }

    public boolean isPending() {
        return isPending;
    }
    public void setPending(boolean pending) {
        isPending = pending;
    }


    public boolean isRejected() {return isRejected; }
    public void setRejected(boolean rejected) {
        isRejected = rejected;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }

    //Parcelable methods
    public ChatUser(Parcel in) {
        this.nickname                = in.readString();
        this.imageProfile            = in.readString();
        this.userId                  = in.readString();
        //this.firstTimeAccessDatabase = in.readByte() != 0;; //in.readBoolean();
        //this.status                  = in.readInt();
        this.notSeenMessagesNumber   = in.readInt();
        this.connectedAt             = in.readString();
        this.disconnectedAt          = in.readString();
        this.lastConnectedAt         = in.readString();
        this.blacklistAuthor         = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nickname);
        dest.writeString(imageProfile);
        dest.writeString(userId);
        //dest.writeByte((byte) (firstTimeAccessDatabase ? 1 : 0));
        //dest.writeInt(status);
        dest.writeInt(notSeenMessagesNumber);
        dest.writeString(connectedAt);
        dest.writeString(disconnectedAt);
        dest.writeString(lastConnectedAt);
        dest.writeString(blacklistAuthor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChatUser> CREATOR = new Creator<ChatUser>() {
        @Override
        public ChatUser createFromParcel(Parcel in) {
            return new ChatUser(in);
        }

        @Override
        public ChatUser[] newArray(int size) {
            return new ChatUser[size];
        }
    };

    public boolean isAccepted() {
        return relationStatus.equals("accepted");
    }
}
