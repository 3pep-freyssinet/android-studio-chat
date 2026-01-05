package com.androidcodeman.simpleimagegallery.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import com.androidcodeman.simpleimagegallery.R;


/**
 * picture_Adapter's ViewHolder
 */

public class PicHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener{

    public ImageView picture, pictureCheck;
    //public CheckBox  checkBox;

    private ItemClickListener mClickListener;


    // parent activity = 'ImageDisplay' will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onLongItemClick(View view, int position);
        //void onChecBoxItemClick(View view, int position);
    };

    PicHolder(@NonNull View itemView, ItemClickListener itemClickListener) {
        super(itemView);
        mClickListener = itemClickListener;

        //checkBox = itemView.findViewById(R.id.itemCheckBox);
        //checkBox.setOnClickListener(this);
        //checkBox.setVisibility(View.INVISIBLE);

        picture  = itemView.findViewById(R.id.image);
        picture.setOnClickListener(this);       //simple click
        picture.setOnLongClickListener(this);   //long clic

        pictureCheck  = itemView.findViewById(R.id.iv_check);
    }

    //call 'ImageDisplay.onLongClick'
    @Override
    public boolean onLongClick(View view) {

        mClickListener.onLongItemClick(view, getAdapterPosition());
        return true;
    }


    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.image) {   //call 'ImageDisplay.onClick'
            mClickListener.onItemClick(view, getAdapterPosition());

            //case R.id.itemCheckBox :    //call 'ImageDisplay.oncheckboxClick
            //    mClickListener.onChecBoxItemClick(view, getAdapterPosition());
            //    break;
        }
    }

    // convenience method for getting data at click position.
    //String getItem(int id) {
    //    return mData.get(id);
    //}
}
