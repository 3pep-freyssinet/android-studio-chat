package com.example.aymen.androidchat;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.sql.MessagesContract;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;


public class MessageSwipeRecyclerView extends ItemTouchHelper.SimpleCallback {

    private static final String DELETED_FROM = "1"; //The message sent by 'from' to 'to' is deleted
    private static final String DELETED_TO = "2"; //The message sent by 'to' to 'from' is deleted
    private static final String DELETED = "1";
    private static final String NOT_DELETED = "0";

    //background and icon to show when an item is swipe.
    private final ColorDrawable background = new ColorDrawable(Color.RED);
    private final int delete_ = R.drawable.delete_24;
    private String delete;

    private final ChatBoxMessage chatBoxMessage;    //it needed by 'Snackbar' and to call interface in 'chatBoxMessage'
    private RecyclerView recyclerView;
    private ChatBoxAdapter chatBoxAdapter;
    private ArrayList<Message> messagesList;


    private ChatBoxAdapter.Attachment attachment;

    /**
     * Creates a Callback for the given drag and swipe allowance. These values serve as
     * defaults
     * and if you want to customize behavior per ViewHolder, you can override
     */
    public MessageSwipeRecyclerView(int dragDirs, int swipeDirs, ChatBoxMessage chatBoxMessage) {
        super(dragDirs, swipeDirs);
        this.chatBoxMessage = chatBoxMessage;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // this method is called when we swipe our item to right direction.
        // below line is to get the position in recycler view.
        int position = viewHolder.getAdapterPosition();

        //Get an item of 'card_layout.xml'
        TextView tv = viewHolder.itemView.findViewById(R.id.message_from_);

        viewHolder.itemView.findViewById(R.id.ll_message);

        //Get the recycler view  and adapter
        recyclerView = (RecyclerView) viewHolder.itemView.getParent();

        //'ChatBoxAdapter' is converted to array so it can be final in 'Snackbar'
        chatBoxAdapter = (ChatBoxAdapter) recyclerView.getAdapter();

        //Get the arraylist wich populate the recycler view
        messagesList = new ArrayList<>(chatBoxAdapter.messageList);

        //Get the attachment
        attachment = chatBoxAdapter.attachment;

        //Get the removed message to extracted infos
        Message removedMessage = messagesList.get(position);

        //Who is deleting the message
        String selectedNickname = chatBoxMessage.SelectedNickname;
        String nickname = ChatBoxMessage.Nickname;

        //set swipe direction
        if (nickname.equals(removedMessage.fromNickname)) this.setDefaultSwipeDirs(ItemTouchHelper.RIGHT);
        if (selectedNickname.equals(removedMessage.fromNickname)) this.setDefaultSwipeDirs(ItemTouchHelper.LEFT);

        if (nickname.equals(removedMessage.fromNickname)) delete = DELETED_FROM;
        if (selectedNickname.equals(removedMessage.fromNickname)) delete = DELETED_TO;

        /*
        //if(nbMessagesUpdatedRemotely != 1) throw new UnknownError("'Save deleted message remotely ' Unknown error");

        // below line is to notify that our item is removed from adapter.
        chatBoxAdapter[0].notifyItemRemoved(position); //the item is not removed

        //arrayList.add(position, deletedCourse);

        //build the new adapter after this change
        chatBoxAdapter[0] = new ChatBoxAdapter(attachment, messagesList);

        //set the adapter for the recycler view
        recyclerView.setAdapter(chatBoxAdapter[0]);

        //Scroll to the last position.
        recyclerView.scrollToPosition(chatBoxAdapter[0].getItemCount() - 1);
        */

        Snackbar snackbar = Snackbar.make(chatBoxMessage.getView(), "deleting message", Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Nothing is done. The message is not removed. Dismiss the snackbar
                //When we have this exception : "Only the original thread that created a view hierarchy can touch its views"
                //we use : 'runOnUiThread(new Runnable()'
                chatBoxMessage.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        chatBoxAdapter = new ChatBoxAdapter(attachment);

                        //set the adapter for the recycler view
                        recyclerView.setAdapter(chatBoxAdapter);

                        //Scroll to the last position.
                        recyclerView.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
                    }
                });
            }
        });
        snackbar.show();
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                    // Snackbar closed on its own. There is no action then
                    //Save the 'messagesList' locally in sqlite db
                    String ref = removedMessage.ref; //reference of the message

                    // Remove message from our array 'messagesList'.
                    messagesList.remove(position);

                    int nbMessagesUpdatedLocally = updateMessage(ref, delete);

                    if (nbMessagesUpdatedLocally != 1)
                        throw new UnknownError("'Save deleted message locally ' Unknown error");

                    //Save the 'messagesList' remotely in the server. The response from the server will be found
                    // at 'updateMessageRes'
                    chatBoxMessage.sendMessage.updateMessage(ref, delete);
                }
            }

            @Override
            public void onShown(Snackbar snackbar) {
                //
            }
        });
    }

    //update the message in sqlite db
    private int updateMessage(String ref, String deleted) {

        ContentValues values = new ContentValues();
        if (deleted.equals(DELETED_FROM)) {
            values.put(MessagesContract.COLUMN_DELETED_FROM, DELETED);
        }
        if (deleted.equals(DELETED_TO)) {
            values.put(MessagesContract.COLUMN_DELETED_TO, DELETED);
        }

        String selection = MessagesContract.COLUMN_REFERENCE + " = ? ";
        String[] selectionArgs = {ref};

        int nbRow = chatBoxMessage.getContext().getContentResolver().update(
                MessagesContract.CONTENT_URI_MESSAGES,
                values,
                selection,
                selectionArgs
        );
        return nbRow;
    }

    public void updateMessageRes(int nbRowsUpdated) {
        switch (nbRowsUpdated) {
            case 0:    //the message is not saved
                Snackbar snackbar = Snackbar.make(chatBoxMessage.getView(), "failure in deleting message", Snackbar.LENGTH_LONG);
                snackbar.show();
                break;

            case 1:
                //the message is saved, updated the adapter
                //When we have this exception : "Only the original thread that created a view hierarchy can touch its views"
                //we use : 'runOnUiThread(new Runnable()'
                chatBoxMessage.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        chatBoxAdapter = new ChatBoxAdapter(attachment);

                        //set the adapter for the recycler view
                        recyclerView.setAdapter(chatBoxAdapter);

                        //Scroll to the last position.
                        recyclerView.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
                    }
                });

                break;
            default:
                throw new UnsupportedOperationException("updateMessageRes : unexpected value : " + nbRowsUpdated);
        }
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        // dX (float) is the amount of horizontal displacement caused by user's action.
        // If the horizontal displacement is positive the item is being
        // swiped to the RIGHT, if it is negative the item is being
        // swiped to the LEFT.

        //cancel draw
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        boolean isCancelled = (dX == 0 && !isCurrentlyActive);
        if (isCancelled) {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
        }
        //delete icon
        Context context = viewHolder.itemView.getContext();
        Drawable icon = ContextCompat.getDrawable(context, delete_);

        //View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20; //so background is behind the rounded corners of itemView
        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconLeft = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
            int iconRight = itemView.getLeft() + iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            background.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
        } else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom());
        } else { // view is unSwiped
            background.setBounds(0, 0, 0, 0);
        }

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        int yPos = (int) ((itemView.getTop() + itemView.getHeight() / 2)  - ((paint.descent() + paint.ascent()) / 2));
        //c.drawText("DELETED", 300, yPos, paint);

        background.draw(c);
        icon.draw(c);
        if(dX > 0)c.drawText("DELETED", 300, yPos, paint);
        if(dX < 0)c.drawText("DELETED", itemView.getRight() - 400, yPos, paint);

    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, new Paint());
    }
}