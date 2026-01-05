package com.google.amara.chattab;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.aymen.androidchat.ChatUser;

import java.io.Serializable;

/**
 * Created by aa
 */

public class NotSeenMessage implements Parcelable, Serializable {

    public String nickname;     //author
    public String nbMessages;   // number of not seen messages by this author
    public String imageProfile; // image profile of this author.

    public NotSeenMessage(){}

    public NotSeenMessage(String nickname, String nbMessages, String imageProfile) {
        this.nickname        = nickname;
        this.nbMessages      = nbMessages;
        this.imageProfile    = imageProfile;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNbMessages() {
        return nbMessages;
    }
    public void setNbMessages(String nbMessages) {
        this.nbMessages = nbMessages;
    }

    public String getImageProfile() {
        return imageProfile;
    }
    public void setImageProfile(String message) {
        this.imageProfile = imageProfile;
    }


    //Parcelable methods
    protected NotSeenMessage(Parcel in) {
        this.nickname       = in.readString();
        this.imageProfile   = in.readString();
        this.nbMessages     = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(nickname);
        dest.writeString(imageProfile);
        dest.writeString(nbMessages);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NotSeenMessage> CREATOR = new Creator<NotSeenMessage>() {
        @Override
        public NotSeenMessage createFromParcel(Parcel in) {
            return new NotSeenMessage(in);
        }

        @Override
        public NotSeenMessage[] newArray(int size) {
            return new NotSeenMessage[size];
        }
    };
}
