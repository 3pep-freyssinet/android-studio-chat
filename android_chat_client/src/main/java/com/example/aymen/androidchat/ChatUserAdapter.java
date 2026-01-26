package com.example.aymen.androidchat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class ChatUserAdapter extends RecyclerView.Adapter<ChatUserAdapter.MyViewHolder> {
    public List<ChatUser> chatUsers;
    private int selected_position = -1;
    private Context context;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public UserData userData;
    public  interface UserData {
        public void sendUserData(ChatUser chatUser);
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
    //constructor
    //'userData' is a reference to activity : 'TabChatActivity' which implements the 'UserData' interface.
    public ChatUserAdapter(UserData userData, FragmentActivity context) {
        this.userData   = userData;
        //this.chatUsers  = chatUsers;
        if(com.example.aymen.androidchat.ChatBoxUsers.UsersDataHolder.hasData()){
            this.chatUsers = com.example.aymen.androidchat.ChatBoxUsers.UsersDataHolder.getData();
        }
        this.context    = context;
    }

    @Override
    public int getItemCount() {
        return chatUsers.size();
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        final ChatUser chatUser = chatUsers.get(position);
        holder.nickname.setText(chatUser.getNickname() +" : ");
        holder.id = chatUser.getChatId();
        holder.timeConnection.setText(    "Connection at         : " + chatUser.getConnectedAt());
        holder.lastTimeConnection.setText("Last connection at : " + chatUser.getLastConnectedAt());

        //decode the profile image base 64
        String encodedImage = chatUser.getImageProfile();
        if( null == encodedImage){
            encodedImage = getBase64StringFromDrawable(context, R.drawable.avatar);
        }
        byte [] encodeByte  = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);

        //display a rounded bitmap
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int radius = w > h ? h : w; // set the smallest edge as radius.

        RoundedImageView  roundedImageView = new RoundedImageView(context);
        bitmap = roundedImageView.getCroppedBitmap(bitmap, radius);

        //bitmap.getConfig();
        //holder.imageProfile.setBackgroundColor(0);
        holder.imageProfile.setImageBitmap(bitmap);

        //status image. 'R.array.status' will be found in 'colors.xml'. The order will be found in 'ChatUser' object
        int[] status = context.getResources().getIntArray(R.array.status);
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

                //Send data to 'userData' in 'TabChatActivity' which implements the interface 'UserData' to show messages
                // for this selected user.
                userData.sendUserData(chatUser);

                //Send sms to user if it is offline
                if(chatUser.status == 0)

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
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatar);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }
}