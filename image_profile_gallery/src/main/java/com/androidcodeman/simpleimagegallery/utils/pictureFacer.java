package com.androidcodeman.simpleimagegallery.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Custom class for holding data of images on the device external storage
 */
public class pictureFacer implements Parcelable {

    private String  picturName;
    private String  picturePath;
    private String  pictureSize;
    private String  pictureDate;
    private String  imageUri;
    private Boolean selected = false;

    public pictureFacer(){ }

    public pictureFacer(String picturName, String picturePath, String pictureSize,
                        String pictureDate, String imageUri) {
        this.picturName  = picturName;
        this.picturePath = picturePath;
        this.pictureSize = pictureSize;
        this.pictureDate = pictureDate;
        this.imageUri    = imageUri;
    }

    //Keep the order sequence the same in 'writeToParsel' and 'pictureFacer(Parcel in)'
    //The type must match.
    public pictureFacer(Parcel in) {
        this.picturName  = in.readString();
        this.picturePath = in.readString();
        this.pictureSize = in.readString();
        this.pictureDate = in.readString();
        this.imageUri    = in.readString();
        this.selected    = in.readByte() != 0;
        //this.privacy = in.readInt();
    }

    //methods from 'Parcelable'
    public static final Creator<pictureFacer> CREATOR = new Creator<pictureFacer>() {
        @Override
        public pictureFacer createFromParcel(Parcel in) {
            return new pictureFacer(in);
        }

        @Override
        public pictureFacer[] newArray(int size) {
            return new pictureFacer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    //Keep the order sequence the same in 'writeToParsel' and 'pictureFacer(Parcel in)'
    //The type must match.
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(picturName);
        dest.writeString(picturePath);
        dest.writeString(pictureSize);
        dest.writeString(pictureDate);
        dest.writeString(imageUri);
        //dest.writeByte((byte) (selected == null ? 0 : selected ? 1 : 2));
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    //getter and setter
    public String getPicturName() {
        return picturName;
    }

    public void setPicturName(String picturName) {
        this.picturName = picturName;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public String getPictureSize() {
        return pictureSize;
    }

    public void setPictureSize(String pictureSize) {
        this.pictureSize = pictureSize;
    }

    public String getPictureDate() {
        return pictureDate;
    }

    public void setPictureDate(String pictureDate) {
        this.pictureDate = pictureDate;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
