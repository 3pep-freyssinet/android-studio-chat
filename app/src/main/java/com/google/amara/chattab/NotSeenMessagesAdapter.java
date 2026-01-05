package com.google.amara.chattab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.ChatBoxMessage;
import com.example.aymen.androidchat.Message;
import com.example.aymen.androidchat.RoundedImageView;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class NotSeenMessagesAdapter extends RecyclerView.Adapter<NotSeenMessagesAdapter.MyViewHolder> {

    public List<NotSeenMessage>  notSeenMessages;
    public Context context;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public  class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView nickname, nbMessages;
        public ImageView imageProfile;

        public Context context;

        public MyViewHolder(View view) {
            super(view);
            imageProfile = (ImageView) view.findViewById(R.id.not_seen_message_image_profile);
            nickname     = (TextView) view.findViewById(R.id.not_seen_message_nickname);
            nbMessages   = (TextView) view.findViewById(R.id.not_seen_message_image_profile_counter_);

            context      = view.getContext();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //constructor
    public NotSeenMessagesAdapter(Context context, List<NotSeenMessage> notSeenMessages) {
        this.notSeenMessages = notSeenMessages;
        this.context         = context;
    }

    @Override
    public int getItemCount() {
        return notSeenMessages.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.not_seen_message_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final NotSeenMessage m = notSeenMessages.get(position);

        //set values
        holder.nickname.setText(String.valueOf(m.nickname ));
        holder.nbMessages.setText(String.valueOf(m.nbMessages ));

        //decode the profile image base 64
        String encodedImage = m.getImageProfile();
        if( null == encodedImage){
            encodedImage = getBase64StringFromDrawable(context, com.example.aymen.androidchat.R.drawable.avatar);
        }
        byte [] encodeByte  = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);

        //display a rounded bitmap
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int radius = w > h ? h : w; // set the smallest edge as radius.

        RoundedImageView roundedImageView = new RoundedImageView(context);
        bitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

        //bitmap.getConfig();
        //holder.imageProfile.setBackgroundColor(0);
        holder.imageProfile.setImageBitmap(bitmap);

        //setup clic event
        holder.imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //clic on image profile in 'notSeenMessages' fragment
                View view = holder.itemView;
                Snackbar.make(view, "Not seen messages clicked", Snackbar.LENGTH_LONG).show();
                Context s = NotSeenMessagesAdapter.this.context;
                ;
            }
        });
    }

    private String getBase64StringFromDrawable(Context context, int drawableInt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), com.example.aymen.androidchat.R.drawable.avatar);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    public Bitmap decodeBase64(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

        //Drawable drawable = myViewHolder.context.getResources().getDrawable(R.drawable.demo_carre_vert);
        //Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        //return bitmap;
    }
}