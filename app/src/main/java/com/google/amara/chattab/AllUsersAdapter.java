package com.google.amara.chattab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.ChatUser;
import com.example.aymen.androidchat.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class AllUsersAdapter extends RecyclerView.Adapter<AllUsersAdapter.MyViewHolder> {
    public List<ChatUser> chatUsers;
    private int selected_position = -1;
    private Context context;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public UserReference userReference;
    public  interface UserReference {
        public void sendUserReference(ChatUser chatUser);
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    public  class MyViewHolder extends RecyclerView.ViewHolder {
        public String       id;
        public TextView     nickname;
        public TextView     timeConnection;
        public TextView     lastTimeConnection;
        public ImageView    imageProfile;
        private ImageView   statusView;
        private TextView    notSeenMessages;

        public MyViewHolder(View view) {
            super(view);
            nickname            = (TextView) view.findViewById(R.id.nickname);
            timeConnection      = (TextView) view.findViewById(R.id.time_connection);
            lastTimeConnection  = (TextView) view.findViewById(R.id.last_time_connection);
            imageProfile        = (ImageView) view.findViewById(R.id.image_profile);
            statusView          = (ImageView) view.findViewById(R.id.status_view);
            notSeenMessages     = (TextView) view.findViewById(R.id.tv_not_seen_messages);
        }
    }
////////////////////////////////////////////////////////////////////////////////////////////////////
    //'userData' is a reference to activity : 'TabChatActivity' which implements the 'UserData' interface.
    public AllUsersAdapter(List<ChatUser> chatUsers, DisplayAllUsersActivity activity) {
        this.chatUsers      = chatUsers;
        this.userReference  = activity;
        context             = activity.getApplicationContext();

        //userReference = context.
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_all_chat_user, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        final ChatUser chatUser = chatUsers.get(position);

        //set the default background of rows
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.itemView.setBackground(context.getResources().getDrawable(R.color.colorWhite));
                if(position % 2 == 0) {
                    holder.itemView.setBackground(context.getResources().getDrawable(R.color.colorCyan));
                }
        }
        holder.nickname.setText(chatUser.getNickname() +" : ");
        holder.id = chatUser.getChatId();
        holder.timeConnection.setText(    "Connection at         : " + chatUser.getConnectedAt());
        holder.lastTimeConnection.setText("Last connection at : " + chatUser.getLastConnectedAt());

        //decode the profile image base 64
        String encodedImage = chatUser.getImageProfile();
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

        //status image. 'R.array.status' will be found in 'colors.xml'. The order will be found in 'ChatUser' object
        int[] status = context.getResources().getIntArray(com.example.aymen.androidchat.R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable)holder.statusView.getBackground();
        backgroundGradient.setColor(status[chatUser.getStatus()]);

        //Not seen messages
        holder.notSeenMessages.setText( "Not seen : "+ chatUser.getNotSeenMessagesNumber());

        //events
        if (selected_position == position) {
            // do your stuff here like
            //Change selected item background color and Show sub item views
            holder.nickname.setTextColor(Color.RED);
        } else {
            // do your stuff here like
            //Change  unselected item background color and Hide sub item views
            holder.nickname.setTextColor(Color.GREEN);
        }
        holder.nickname.setText(chatUser.getNickname());
        holder.imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Reset the number of not seen messages locally. In the server it is done below in 'userData.sendUserData(chatUser)'
                chatUser.setNotSeenMessagesNumber(0);

                //Send data to 'sendUserReference' in 'TabChatActivity' which implements the interface 'userReference' to show messages
                // for this selected user.
                userReference.sendUserReference(chatUser);

                if(selected_position == position){
                    selected_position = -1;
                    notifyDataSetChanged();
                    return;
                }
                selected_position = position;
                notifyDataSetChanged();
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
}