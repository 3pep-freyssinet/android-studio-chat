package com.example.aymen.androidchat;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by
 */

public class Message implements Serializable, Parcelable {

    public String fromNickname;
    public String toNickname;
    public String message ;
    public long time;
    public String extra;
    public String extraName;
    public String ref;
    public String mimeType;
    public String seen;
    public String deletedFrom;
    public String deletedTo;

    public  Message(){

    }
    public Message(String fromNickname, String toNickname, String message, long time, String extra, String extraName,
                   String ref, String mimeType, String seen, String deletedFrom, String deletedTo) {
        this.fromNickname = fromNickname;
        this.toNickname   = toNickname;
        this.message      = message;    //content
        this.time         = time;       //long
        this.extra        = extra;      //thumb image encoded string base-64
        this.extraName    = extraName;  //filename of attachment
        this.ref          = ref;        //reference of message
        this.mimeType     = mimeType;   //mime type of attachment joigned to this message
        this.seen         = seen;       //the message is read or seen
        this.deletedFrom  = deletedFrom;//the message sent by 'from' to 'to' is deleted
        this.deletedTo    = deletedTo;//the message sent by 'to' to 'from' is deleted
    }

    public String getFromNickname() {
        return fromNickname;
    }
    public void setFromNickname(String fromNickname) {
        this.fromNickname = fromNickname;
    }

    public String getToNickname() {
        return toNickname;
    }
    public void setToNickname(String toNickname) {
        this.toNickname = toNickname;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    public String getExtra() {
        return extra;
    }
    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getExtraName() {
        return extraName;
    }
    public void setExtraName(String extraName) {
        this.extraName = extraName;
    }

    public String getRef() {
        return ref;
    }
    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getSeen() {
        return seen;
    }
    public void setSeen(String seen) {
        this.seen = seen;
    }

    public String getDeletedFrom() {
        return deletedFrom;
    }
    public void setDeletedFrom(String deletedFrom) {
        this.deletedFrom = deletedFrom;
    }

    public String getDeletedTo() {
        return deletedTo;
    }
    public void setDeletedTo(String deletedTo) {
        this.deletedTo = deletedTo;
    }


    //Parcelable methods
    protected Message(Parcel in) {
        this.fromNickname            = in.readString();
        this.toNickname              = in.readString();
        this.message                 = in.readString();
        //this.firstTimeAccessDatabase = in.readByte() != 0;; //in.readBoolean();
        this.time                    = in.readLong();
        this.extra                  = in.readString();
        this.extraName              = in.readString();
        this.ref                    = in.readString();
        this.mimeType               = in.readString();
        this.seen                   = in.readString();
        this.deletedFrom            = in.readString();
        this.deletedTo              = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fromNickname);
        dest.writeString(toNickname);
        dest.writeString(message);
        //dest.writeByte((byte) (firstTimeAccessDatabase ? 1 : 0));
        dest.writeLong(time);
        dest.writeString(extra);
        dest.writeString(extraName);
        dest.writeString(ref);
        dest.writeString(mimeType);
        dest.writeString(seen);
        dest.writeString(deletedFrom);
        dest.writeString(deletedTo);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
