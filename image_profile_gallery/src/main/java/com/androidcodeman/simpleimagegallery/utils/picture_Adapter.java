package com.androidcodeman.simpleimagegallery.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidcodeman.simpleimagegallery.ImageDisplay;
import com.androidcodeman.simpleimagegallery.ImageProfileGalleryMainActivity;
import com.androidcodeman.simpleimagegallery.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;
import static androidx.core.view.ViewCompat.setTransitionName;

/**
 * A RecyclerView Adapter class that's populates a RecyclerView with images from
 * a folder on the device external storage
 */
public class picture_Adapter extends RecyclerView.Adapter<PicHolder> {

    private android.view.ActionMode actionMode;
    private ArrayList<pictureFacer> pictureList;
    private Context pictureContx;
    private final itemClickListener picListener;

    // true if the user is in selection mode, false otherwise.
    private boolean multiSelect   = false;
    private View head;  //card view
    private ViewGroup.LayoutParams originalLayoutParams;//layout of the card view

    // Keeps track of all the selected images
    private ArrayList<pictureFacer> selectedItems = new ArrayList<>();
    boolean[]checkBoxSelection_; //state of checkbox, checked (true), unchecked(false)

    /**
     * @param pictureList   ArrayList of pictureFacer objects
     * @param pictureContx  The Activities Context
     * @param picListener   An interface for listening to clicks on the RecyclerView's items
     */
    public picture_Adapter(ArrayList<pictureFacer> pictureList, Context pictureContx,
                           itemClickListener picListener) {
        this.pictureList  = pictureList;
        this.pictureContx = pictureContx;
        this.picListener  = picListener;
        //this.checkBoxSelection_    = new boolean[pictureList.size()]; //filled with 'false' default value.
        ImageDisplay imageDisplay = (ImageDisplay)pictureContx;//used to access 'multiSelect' below.
        this.multiSelect  = imageDisplay.multiSelect;
        this.actionMode   = null;
    }

    @NonNull
    @Override
    public PicHolder onCreateViewHolder(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View cell = inflater.inflate(R.layout.pic_holder_item, container, false);
        return new PicHolder(cell, (PicHolder.ItemClickListener)pictureContx);
    }

    @Override
    public void onBindViewHolder(@NonNull final PicHolder holder, final int position) {

        final pictureFacer image = pictureList.get(position);
        //holder.picture.setImageBitmap(image.);

        //if(!multiSelect)checkBoxSelection_[position] = false;

        //Default visibility = invisible
        //holder.checkBox.setVisibility(View.INVISIBLE);

        //Default visibility = invisible
        holder.pictureCheck.setVisibility(View.INVISIBLE);

        //Visibility in 'multiselect' mode = visible
        //if(multiSelect)holder.checkBox.setVisibility(View.VISIBLE);

        String uri_ = pictureList.get(position).getImageUri();
        Uri    uri  = Uri.parse(uri_);

        /*
        Bitmap bitmap = null;
        try {
            //exception : 'com.android.providers.downloads.DownloadStorageProvider
            // uri content://com.android.providers.downloads.documents/document/23 from pid=7615,
            // uid=10100 requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
            //workaroud : use 'OPEN_DOCUMENT' instead of 'GET_CONTENT' in intent

            //Failed to allocate a 51916812 byte allocation with 4194304 free bytes and 45MB until OOM
            //workaroud : use Glide
            bitmap = MediaStore.Images.Media.getBitmap(this.pictureContx.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.picture.setImageBitmap(bitmap);
        */


        Bitmap bitmap = BitmapFactory.decodeFile(image.getPicturePath());
        //holder.picture.setImageBitmap(bitmap);


        Glide.with(pictureContx)
                .load(image.getPicturePath()) //'image.getPicturePath()' is like '/storage/emulated/0/Android/data/com.google.amara.chattab/files/Pictures/1668869824258.jpg'
                .apply(new RequestOptions().centerCrop())
                .into(holder.picture);


        setTransitionName(holder.picture, String.valueOf(position) + "_image");
    }

    @Override
    public int getItemCount() {
        return pictureList.size();
    }
}