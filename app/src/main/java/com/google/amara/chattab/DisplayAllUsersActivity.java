package com.google.amara.chattab;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.ChatBoxActivity;
import com.example.aymen.androidchat.ChatBoxMessage;
import com.example.aymen.androidchat.ChatUser;
import com.example.aymen.androidchat.ChatUserAdapter;
import com.example.aymen.androidchat.Message;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//Ce fragment est défini dans 'TabChatActivity-SectionPager'
public class DisplayAllUsersActivity extends AppCompatActivity
                                     implements AllUsersAdapter.UserReference{

    private static final int INTENT_NEW_USER_RESULT_CODE = 301;

    //public class ChatBoxActivity extends AppCompatActivity {
    public RecyclerView     usersRecylerView;
    public List<ChatUser>   chatUserList;
    private ChatUserAdapter chatUserAdapter;

    public String           Nickname, Id;          //the current nickname and id values
    private JSONObject      Profile;               //the current image profile


    private TextView tvNbUsers;
    private ImageView imageProfile;

    private HashMap<String, String> connectedUsers = new HashMap<String, String>();

    public  AllUsersList         allUsersList;
    private FragmentActivity    fragmentActivity;
    private Activity            mActivity;
    private FloatingActionButton fab;
    private AllUsersAdapter     allUsersAdapter;
    private RecyclerView        allUsersRecylerView;

    public interface AllUsersList {
        public void getAllUsersList();
    }

    //private JSONObject people;
    //private String imageProfile;


    //@Override
    //protected void onCreate(Bundle savedInstanceState) {
    //    super.onCreate(savedInstanceState);
    //    setContentView(R.layout.activity_chat_box);

    //@Override
    protected void onCreate_(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        //get extra from intent
        //ArrayList<ChatUser> users = getIntent().getExtras().getParcelableArrayList("all_users");

        ChatUser user = getIntent().getExtras().getParcelable("current_user");

        if (TabChatActivity.DataHolder.hasData()) {
            List<Object> arrayField = TabChatActivity.DataHolder.getData();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        //get extra from intent
        //ArrayList<ChatUser> users = getIntent().getExtras().getParcelableArrayList("all_users");

        ChatUser user = getIntent().getExtras().getParcelable("current_user");

        List<ChatUser> users_     = null;
        ArrayList<ChatUser> users = null;
        if (TabChatActivity.DataHolder.hasData()) {
            users_ = (List<ChatUser>)(Object)TabChatActivity.DataHolder.getData();
            users  = new ArrayList<>(users_.size());
            users.addAll(users_);

            /*
            if (object != null) {
                List<ChatUser> ticketList = new ArrayList<>();
                for (Object result : object) {
                    String json = new Gson().toJson(result);
                    ChatUser model = new Gson().fromJson(json, ChatUser.class);
                    ticketList.add(model);
                }
            }
            */
        }

        allUsersRecylerView   = (RecyclerView)findViewById(R.id.all_users_recyclerView);
        tvNbUsers             = (TextView) findViewById(R.id.tv_nb_all_users);

        int size              = (users == null) ? 0 : users.size();
        tvNbUsers.setText( size + " users");

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        allUsersRecylerView.setLayoutManager(mLayoutManager);
        allUsersRecylerView.setItemAnimator(new DefaultItemAnimator());

        //update the adapter
        this.getCallingActivity();
        this.getApplication();
        if (this.getBaseContext() instanceof Activity){
            //mActivity =(Activity) context;
        }
        allUsersAdapter = new AllUsersAdapter(users, this);

        // notify the adapter to update the recycler view
        allUsersAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        allUsersRecylerView.setAdapter(allUsersAdapter);
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("some_int", 10);
        /*
        outState.putString("idNickname", (String) idNickname.getText());
        outState.putString("nickname_", (String) nickname_.getText());
        outState.putString("connectionNickname", (String) connectionNickname.getText());

        outState.putString("idSelectedNickname", (String) idSelectedNickname.getText());
        outState.putString("selectedNickname", (String) selectedNickname.getText());
        outState.putString("connectionSelectedNickname", (String) connectionSelectedNickname.getText());


        if (messageRecycler != null) {
            //Save recycler view
            System.out.println("*********** onSaveInstanceState  messageRecycler = " + messageRecycler.getAdapter().getItemCount());
            System.out.println("*********** onSaveInstanceState  chatMessageList = " + chatMessageList.size());

            Parcelable listState = messageRecycler.getLayoutManager().onSaveInstanceState();
            // putting recyclerview position
            outState.putParcelable("messageRecycler", listState);

            //List<Message> kk = chatMessageList.subList(chatMessageList.size() - 10, chatMessageList.size());
            //ArrayList<Message> jj = new ArrayList<Message>(kk);

            outState.putParcelableArrayList("chatMessageList", chatMessageList);
        }
        */
        super.onSaveInstanceState(outState);
    }



    @Override
    public void onResume() {
        super.onResume();
        //userNotification.fragmentAttached();
    }

    @Override
    public void onPause() {
        super.onPause();
        //userNotification.fragmentAttached();
    }

    @Override
    public void onStop() {
        super.onStop();
        //userNotification.fragmentAttached();
    }

    @Override
    public void onStart() {
        super.onStart();
        /*
        myRecylerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int i = 0;
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int i = 0;
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                int i = 0;
            }
        });
         */

        //tvNickname.setText(Nickname);

        // add the new updated list to the adapter. 'getActivity()' is a reference to activity which launch this fragment : 'TabChatActivity'.
        /*
        allUsersAdapter = new AllUsersAdapter(chatUserList, getActivity());

        // notify the adapter to update the recycler view
        allUsersAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        usersRecylerView.setAdapter(allUsersAdapter);
        */

    }//end onStart


    /*
    @Override
    public void finish() {
        // Prepare data intent to send back to 'MainActivity' which has sent this intent 'ChatBoxActivity'
        //Intent data = new Intent();
        //data.putExtra("chat_box_status", chatBoxStatus);
        //setResult(INTENT_RESULT_OK_CHATBOX_ACTIVITY, data); //the data are returned to 'onActivityResult' of 'MainActivity'.
        super.finish();
    }
    */
    @Override
    public void onDestroy() {
        super.onDestroy();
        //socket.emit("disconnect", Nickname);
        //socket.disconnect();
    }

    //@Override
    public void sendUserReference(ChatUser chatUser) {
        //Send this 'chatUser' to 'TabChatActivity.onActivityResult'.
        // Prepare data intent to send 'chatUser'
        Intent data = new Intent();
        data.putExtra("new_chat_user", (Parcelable) chatUser);

        setResult(INTENT_NEW_USER_RESULT_CODE, data); //the data are returned to 'onActivityResult' of 'TabChatAtivity' which lanched the intent.
        super.finish(); // obligatoire, sinon l'intent n'est pas envoyé
    }
}
