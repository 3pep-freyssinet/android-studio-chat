package com.example.aymen.androidchat;

import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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

import com.example.aymen.androidchat.sql.MessagesContract;

import java.util.ArrayList;
import java.util.List;


public class ChatBoxAdapter  extends RecyclerView.Adapter<ChatBoxAdapter.MyViewHolder> {

    private static final String SEEN    = "1";
    private static final String NOTSEEN = "0";

    public List<Message>  messageList;
    public Attachment     attachment;
    private Context       globalContext;

    public  interface Attachment {
        public void getAttachment(String reference);
        public void displayAttachment(ImageView thumbAttachmentFrom);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public  class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView fromNickname, fromId;
        public TextView toNickname, toId;
        public TextView message;
        public TextView time;
        public TextView extra;
        public TextView messageFrom, messageFromTime, messageTo, messageToTime;
        public TextView messageFrom_, messageTo_;
        public ImageView messageFromSeen, messageToSeen;
        private TextView tvAttachmentFrom, tvAttachmentTo;
        private TextView linkAttachmentFrom, linkAttachmentTo;
        private ImageView thumbAttachmentFrom, thumbAttachmentTo;
        private ProgressBar downloadAttachmenProgressBar;
        public LinearLayout llMessage, llAttachmentItem, llAttachmentTo;
        private View flottantViewFrom, flottantViewTo;

        //private View view;

        public Context context;

        public MyViewHolder(View view) {
            super(view);
            //fromNickname = (TextView) view.findViewById(R.id.from_nickname);
            //fromId       = (TextView) view.findViewById(R.id.from_id);
            //toNickname   = (TextView) view.findViewById(R.id.to_nickname);
            //toId         = (TextView) view.findViewById(R.id.to_id);
            //message      = (TextView) view.findViewById(R.id.message);
            //time         = (TextView) view.findViewById(R.id.time);
            //extra        = (TextView) view.findViewById(R.id.extra);

            //messageFrom       = (TextView) view.findViewById(R.id.message_from);
            messageFromTime     = (TextView) view.findViewById(R.id.message_from_time);
            messageToTime       = (TextView) view.findViewById(R.id.message_to_time);
            messageFrom_        = (TextView) view.findViewById(R.id.message_from_);
            messageFromSeen     = (ImageView) view.findViewById(R.id.message_from_seen);
            messageToSeen       = (ImageView) view.findViewById(R.id.message_to_seen);
            messageTo_          = (TextView) view.findViewById(R.id.message_to_);
            thumbAttachmentFrom = (ImageView)view.findViewById(R.id.thumb_attachment_from);
            thumbAttachmentTo   = (ImageView)view.findViewById(R.id.thumb_attachment_to);
            linkAttachmentFrom  = (TextView) view.findViewById(R.id.link_attachment_from);
            linkAttachmentTo    = (TextView) view.findViewById(R.id.link_attachment_to);
            downloadAttachmenProgressBar = (ProgressBar)view.findViewById(R.id.download_attachment_progressBar);
            llMessage           = (LinearLayout)view.findViewById(R.id.ll_message);
            llAttachmentItem    = (LinearLayout)view.findViewById(R.id.ll_attachment_item);
            flottantViewFrom    = (View)view.findViewById(R.id.flottant_view_from);
            flottantViewTo      = (View)view.findViewById(R.id.flottant_view_to);

            //this.view         = view;
            context             = view.getContext();
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //constructor
    public ChatBoxAdapter(Attachment attachment) {
        this.attachment  = attachment;
        if(ChatBoxMessage.MessagesDataHolder.hasData()){
            this.messageList = ChatBoxMessage.MessagesDataHolder.getData();
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item, parent, false);
        globalContext = parent.getContext();
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final Message m = messageList.get(position);
        //holder.llAttachmentItem.setVisibility(View.GONE); //default

        //holder.fromNickname.setText(m.getFromNickname() +" : ");
        //holder.fromId.setText(m.getFromId() +" : ");
        //holder.toNickname.setText(m.getToNickname() +" : ");
        //holder.toId.setText(m.getToId() +" : ");
        //holder.message.setText(m.getMessage() );
        //holder.time.setText(String.valueOf(m.getTime() ));
        //holder.extra.setText(m.getExtra().toString() );

        //holder.messageFrom.setText(m.getFromNickname() +" : " );
        //holder.messageTo.setText(m.getToNickname() +" : " );

        //holder.itemView.getscrollToPosition(getItemCount() - 1);

        /*
        ColorDrawable colorDrawable = new ColorDrawable(0xFF00FF00);
        if(position % 2 == 0)colorDrawable = new ColorDrawable(0xFFFFFF00);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.view.setBackground(colorDrawable);  //holder.itemView.setBackground(colorDrawable);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.view.setBackground(colorDrawable);
        }
        */

        //default values
        holder.messageFrom_.setText(null);
        holder.messageTo_.setText(null );
        holder.messageFromSeen.setVisibility(View.GONE);
        holder.messageToSeen.setVisibility(View.GONE);

        if(ChatBoxMessage.Nickname.equals(m.getFromNickname())) {   //'from'
            holder.messageFromTime.setText(String.valueOf(m.getTime() ));

            //Set 'messageFrom_' weight width = 0.7
            holder.messageFrom_.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            holder.messageFrom_.setText(m.getMessage());

            //show the not seen messages in bold
            if(m.seen.equals("0")){
                holder.messageFrom_.setTypeface(null, Typeface.BOLD);
                holder.messageToSeen.setVisibility(View.INVISIBLE);
                //m.seen.equals("1");
                //update the unseen messages in sqlite db
                //int updatedMessageNumber = updateMessagesNotSeen(m.fromNickname, m.toNickname);
                //if(updatedMessageNumber == 0)
                //    throw new UnsupportedOperationException("The 'from' not seen message is not updated");
            }else{
                //show the check beside the message
                holder.messageFrom_.setTypeface(null, Typeface.NORMAL);
                holder.messageFromSeen.setVisibility(View.VISIBLE);
            }

            //holder.messageFrom_.setBackgroundColor(Color.parseColor ("#eefaee")); //green

            //String bitmapString = m.getExtra();

            //La position des bulles de messages (à gauche de l'écran pour 'from' et à droite pour 'to')
            //depend de deux vues vides. la vue 'flottantViewFrom' est placée au debut à gauche et
            //la vue 'flottantViewTo' est placée après 'linkAttachmentFrom'.
            //Selon leur largeur, elles vont pousser les vues qui leurs sont voisines.
            //la disposition des vues est la suivante|
            //flottantViewFrom |thumbAttachmentFrom|linkAttachmentFrom|flottantViewTo |thumbAttachmentTo|linkAttachmentTo

            //La bulle message a une largeur = 0.7 écran

            //Attachment items
            //if(!m.getExtra().equals("null")){
            if(m.getExtra() != null) {
                //Show the layout containing : flottantViewFrom |thumbAttachmentFrom|linkAttachmentFrom|flottantViewTo |thumbAttachmentTo|linkAttachmentTo
                holder.llAttachmentItem.setVisibility(View.VISIBLE);

                //La bulle 'From', weight width = 0.0. Note that the total width of : 'flottantViewFrom' +
                // 'thumbAttachmentFrom' + 'linkAttachmentFrom' = 0.7 as 'messageFrom_'.
                holder.flottantViewFrom.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.0f));

                //'thumbAttachmentFrom' weight width = 0.1
                holder.thumbAttachmentFrom.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.1f));

                ////'linkAttachmentFrom' weight width = 0.6
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f);
                params.gravity = Gravity.CENTER;
                params.leftMargin = 10;
                holder.linkAttachmentFrom.setLayoutParams(params);

                //holder.linkAttachmentFrom.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f));

                //La bulle 'To', sa largeur = 0.3
                holder.flottantViewTo.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.3f));
                holder.thumbAttachmentTo.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.0f));
                holder.linkAttachmentTo.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.0f));

                holder.thumbAttachmentFrom.setImageBitmap(null);

                //set bitmap thumb
                String bitmapString = m.getExtra();
                Bitmap bitmap = decodeBase64(bitmapString);
                holder.thumbAttachmentFrom.setImageBitmap(bitmap);

                //set links
                holder.linkAttachmentFrom.setText(m.getExtraName() );
                holder.linkAttachmentFrom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(holder.context, "link Attachment from : " + m.getFromNickname() + " To : " + m.getToNickname() + " clicked", Toast.LENGTH_SHORT).show();
                        //holder.downloadAttachmenProgressBar.setVisibility(View.VISIBLE);
                        attachment.getAttachment(m.ref);
                        //the attachment is downloaded in 'TabChatActivity.getAttachment' witch ask
                        // the server to download the attachment. Then the downloaded attachment is
                        //saved localy.
                        // The attachment is returned in 'socket.on("download_chunks")' where it is
                        // displayed in intent.

                        //display the attachment.
                        //attachment.displayAttachment(holder.thumbAttachmentFrom);
                    }
                });
            }
            // bulle 'From', no attachment
            holder.messageToTime.setText(null );
            holder.messageTo_.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.3f));
            //holder.llAttachmentTo.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 0.3f));

            holder.messageTo_.setText(null);
            holder.messageTo_.setBackgroundColor(0);
            holder.thumbAttachmentTo.setImageBitmap(null);
            holder.linkAttachmentTo.setText(null);

        }else{
            //Attachment 'To'
            holder.messageToTime.setText(String.valueOf(m.getTime() ));
            holder.messageTo_.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.7f));
            holder.messageTo_.setText(m.getMessage());

            //show the not seen messages in bold
            if(m.seen.equals("0")){
                //holder.messageFrom_.setTypeface(holder.messageFrom_.getTypeface(), Typeface.BOLD);
                holder.messageTo_.setTypeface(null, Typeface.BOLD);
                holder.messageToSeen.setVisibility(View.INVISIBLE);
                m.seen = "1";
                //update the unseen message that have reference = 'm.ref' in sqlite db
                int updatedMessageNumber = updateMessagesNotSeen(m.fromNickname, m.toNickname, m.ref);
                if(updatedMessageNumber == 0)
                    throw new UnsupportedOperationException("The 'to' not seen message is not updated");

            }else{
                //show the check beside the messages
                holder.messageToSeen.setVisibility(View.VISIBLE);
            }

            //if(!m.getExtra().equals("null")){
            if(m.getExtra() != null){
                //Show the layout containing : flottantViewFrom |thumbAttachmentFrom|linkAttachmentFrom|flottantViewTo |thumbAttachmentTo|linkAttachmentTo
                holder.llAttachmentItem.setVisibility(View.VISIBLE);
                holder.messageFrom_.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.0f));

                holder.flottantViewFrom.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.3f));
                holder.thumbAttachmentFrom.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.0f));
                holder.linkAttachmentFrom.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.0f));

                holder.flottantViewTo.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.0f));
                holder.thumbAttachmentTo.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 0.1f));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f);
                params.gravity = Gravity.CENTER;
                params.leftMargin = 10;
                holder.linkAttachmentTo.setLayoutParams(params);

                //holder.linkAttachmentTo.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f));

                //holder.messageTo_.setBackgroundColor(Color.parseColor ("#F2F7F9"));//blue

                String bitmapString = m.getExtra();
                Bitmap bitmap = decodeBase64(bitmapString);
                holder.thumbAttachmentTo.setImageBitmap(bitmap);

                holder.linkAttachmentTo.setText(m.getExtraName() );
                holder.linkAttachmentTo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(holder.context, "link Attachment to : "+m.getToNickname()+" from : "+m.getFromNickname()+" clicked", Toast.LENGTH_SHORT).show();
                        //holder.downloadAttachmenProgressBar.setVisibility(View.VISIBLE);
                        attachment.getAttachment(m.ref);
                        //the attachment is download in 'TabChatActivity.getAttachment' witch ask the server to download the attachment.
                        // The attachment is returned in 'socket.on("download_chunks")' where it is displayed in dialog.

                        //Enlarge the image view.
                        //attachment.displayAttachment(holder.thumbAttachmentTo);
                    }
                });
            }

            //bulle 'To' No attachment
            holder.messageFromTime.setText(null);
            holder.messageFrom_.setLayoutParams(new LinearLayout.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.3f));
            //holder.llAttachmentFrom.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 0.3f));

            holder.messageFrom_.setText(null);
            holder.messageFrom_.setBackgroundColor(0);
            holder.thumbAttachmentFrom.setImageBitmap(null);
            holder.linkAttachmentFrom.setText(null);
        }
    }

    private int updateMessagesNotSeen(String fromNickname, String toNickname, String ref) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(MessagesContract.COLUMN_SEEN , SEEN);

        int rows = globalContext.getContentResolver().update(MessagesContract.CONTENT_URI_MESSAGES,
                contentValues,
                MessagesContract.COLUMN_FROM     + " =? AND "
                        + MessagesContract.COLUMN_TO   + " =? AND "
                        + MessagesContract.COLUMN_REFERENCE + " LIKE ? ",

                new String[]{fromNickname, toNickname, ref }
        );
        return  rows;
    }

    public Bitmap decodeBase64(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);

        //Drawable drawable = myViewHolder.context.getResources().getDrawable(R.drawable.demo_carre_vert);
        //Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        //return bitmap;
    }
}