package com.example.aymen.androidchat;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.github.nkzawa.emitter.Emitter;
//import com.github.nkzawa.socketio.client.IO;
//import com.github.nkzawa.socketio.client.Socket;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//Ce fragment est défini dans 'TabChatActivity-SectionPager'
public class ChatBoxUsers extends Fragment {
    //public class ChatBoxUsers extends AppCompatActivity {
    public RecyclerView     userRecylerView;
    public List<ChatUser>   chatUserList;
    private ChatUserAdapter chatUserAdapter;

    public String           Nickname, Id;          //the current nickname and id values
    private JSONObject      Profile;               //the current image profile


    private TextView tvNbUsers;
    private ImageView imageProfile;

    private HashMap<String, String> connectedUsers = new HashMap<String, String>();

    public  UserNotification     userNotification;
    private AllUsersList         allUsersList;
    private FragmentActivity     fragmentActivity;
    private Activity             mActivity;
    private FloatingActionButton fab;

    public void sendNetworkNotification(String networkStatus) {
        switch (networkStatus) {
            case "Wifi enabled":
            case "Mobile data enabled":
                //if(!nickname.getText().toString().isEmpty())btn.setEnabled(true);
                break;
            case "No internet is available":
                //btn.setEnabled(false);

                if(!isVisible())return;
                //Snackbar.make(getView(), "Status ChatBoxUsers :  = " + networkStatus, Snackbar.LENGTH_LONG).show();

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setMessage("No Internet connection");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        getActivity().finish();
                    }
                }).create().show();

                break;
        }
    }

    public interface UserNotification {
        public void notifyBlacklist(String authorBlacklist,
                                    String blacklistedNickname,
                                    String blacklistedNicknameId,
                                    String blacklist);  //may be "blacklist" or "recover"
                                    //void fragmentAttached();
    }

    public interface AllUsersList {
        public void getAllUsersList();
    }

    public enum UsersDataHolder {
        INSTANCE;

        private List<ChatUser> users;

        public static boolean hasData() {
            return INSTANCE.users != null;
        }

        public static void setData(final List<ChatUser> users) {
            INSTANCE.users = users;
        }

        public static List<ChatUser> getData() {
            final List<ChatUser> retList = INSTANCE.users;
            INSTANCE.users = null;
            return retList;
        }
    }

    //private JSONObject people;
    //private String imageProfile;


    //@Override
    //protected void onCreate(Bundle savedInstanceState) {
    //    super.onCreate(savedInstanceState);
    //    setContentView(R.layout.activity_chat_box);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null){
            int ii = savedInstanceState.getInt("some_int");
        }else{
            int ii = 0;
        }

        //requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view      = inflater.inflate(R.layout.activity_chat_box, container, false);

        //connected users
        //imageProfile   = (ImageView) view.findViewById(R.id.imageView);
        tvNbUsers      = (TextView) view.findViewById(R.id.tv_nb_users);

        userRecylerView = (RecyclerView) view.findViewById(R.id.user_list);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        chatUserList    = new ArrayList<>();

        /*
        getActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback() {
            @Override
            public void handleOnBackPressed() {

            }
        });
        */

        /*
        //getFragmentManager().beginTransaction().replace(R.id.rl_activity_chat_box, this ).addToBackStack("chat_box_user").commit();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(this);
        Fragment newInstance = null;
        try {
            newInstance = this.getClass().newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        }

        ft.add(R.id.rl_activity_chat_box, newInstance);
        ft.commit();
        */

        // get the nickame of the user
        //Nickname= (String)getIntent().getExtras().getString(MainActivity.NICKNAME);

        Bundle bundle = getArguments();
        if (bundle != null) {
            this.Nickname = bundle.getString("Nickname");
            //chatUserList   = (ArrayList<ChatUser>) bundle.getSerializable("chat_user_list");
        }
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        userRecylerView.setLayoutManager(mLayoutManager);
        userRecylerView.setItemAnimator(new DefaultItemAnimator());

        UserSwipeRecyclerView chatUserSwipeRecyclerView = new UserSwipeRecyclerView(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT , ChatBoxUsers.this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(chatUserSwipeRecyclerView);
        itemTouchHelper.attachToRecyclerView(userRecylerView);

        //fab event
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Add a user", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                getFragmentManager()
                        .beginTransaction()
                        .add(new AllUsersFragment(), "all_users_fragment_tag")
                        .commitNow();

                //call 'getAllUsersList' in TabChatActivity
                allUsersList.getAllUsersList();

                //getFragmentManager().getFragments();

                //getFragmentManager().beginTransaction()
                //            .remove( new ChatBoxUsers())
                //            .commitNow();
                //getFragmentManager().beginTransaction()
                //        .show(new AllUsersFragment())
                //        .commitNow();
            }
        });

        return view;
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);
        if (inState != null) {
            //System.out.println("************ onViewStateRestored *****************");
        }
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            mActivity =(Activity) context;
        }
        try {
            userNotification = (UserNotification) getActivity();
            allUsersList     = (AllUsersList) getActivity();

        } catch (ClassCastException e) {
            throw new ClassCastException("Error in retrieving data. Please try again");
        }
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

    /* There is not a method 'onBackPressed' in fragment.
    //when the 'Back' button is pressed in fragment, the event is received in 'onBackPressed' of the parent.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    */

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
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);


    }//end onStart

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //tv1 = (TextView)view.findViewById(R.id.tv1);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            return stream.toByteArray();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    e.printStackTrace();
                    //Log.e(Helper.class.convertBitmapToByteArray(), "ByteArrayOutputStream was not closed");
                }
            }
        }
    }

    public void updateChatListUsers(String nickname, String idNickname) {
        //update the id
        Iterator<ChatUser> iterator = chatUserList.iterator();
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(nickname)) {
                chatUser.setStatus(ChatUser.userConnect);
                chatUser.setStatus(ChatUser.userConnect);
                chatUser.setChatId(idNickname);

                //update the adapter
                UsersDataHolder.setData(chatUserList);
                chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

                // notify the adapter to update the recycler view
                chatUserAdapter.notifyDataSetChanged();

                //set the adapter for the recycler view
                userRecylerView.setAdapter(chatUserAdapter);
                break;
            }
        }
    }

    //A new user is connecting or he is reconnecting
    public void displayReceivedNewUser(ChatUser newUser) {

        //test if reconnect and set the new id, status and 'connectionTime'.
        boolean reconnect = false;
        Iterator<ChatUser> iterator = chatUserList.iterator();
        while (iterator.hasNext()) {
            ChatUser chatUser_ = iterator.next();
            if (chatUser_.getNickname().equals(newUser.getNickname())) {
                reconnect = true;
                //update data
                chatUser_.setChatId(newUser.getChatId());
                chatUser_.setStatus(newUser.getStatus());
                chatUser_.setConnectedAt(newUser.getConnectedAt());
                chatUser_.setDisconnectedAt(newUser.getDisconnectedAt());
                chatUser_.setImageProfile(newUser.getImageProfile());
                break;
            }
        }

        //add the new user to the array list 'chatUserList' if it is the first connection.
        if (!reconnect) chatUserList.add(newUser);
        tvNbUsers.setText(chatUserList.size() + " users are connected");

        //il ya une erreur : getActivity is null
        //fragmentActivity  = getActivity();

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                // add the new updated list to the adapter. 'getActivity()' is a reference to activity which launch this fragment : 'TabChatActivity'.
                UsersDataHolder.setData(chatUserList);
                chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

                // notify the adapter to update the recycler view
                chatUserAdapter.notifyDataSetChanged();

                //set the adapter for the recycler view

                userRecylerView.setAdapter(chatUserAdapter);
            }
        });
    }

    //user 'nickname' has pressed the 'Back' key and is disconnect. Update his status and the adapter.
    public void displayReceivedDiconnectUser(String nickname) {
        Iterator<ChatUser> iterator = chatUserList.iterator();
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(nickname)) {
                //chatUserList.remove(chatUser);    //chatuserList est static. Si 'chatuser' est retiré, ses messages ne sont pas sauvegardés dans 'saveAllMessages' dans 'ChatBoxMessage'.
                chatUser.setStatus(ChatUser.userGone);
                break;
            }
        }

        // updated list to the adapter. 'getActivity()' is a reference to activity which launch this fragment : 'TabChatActivity'.
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);
    }

    /*
    @Override
    public void finish() {
        // Prepare data intent to send back to 'MainActivity' which has sent this intent 'ChatBoxUsers'
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

    public void displayReceivedStandbyUser(String nickname) {
        //user 'nickname'is standby
        Iterator<ChatUser> iterator = chatUserList.iterator();
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(nickname)) {
                chatUser.setStatus(ChatUser.userStandby);
                break;
            }
        }
        // updated list to the adapter. 'getActivity()' is a reference to activity which launch this fragment : 'TabChatActivity'.
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);
    }

    public void displayReceivedBackStandbyUser(String nickname) {
        //user 'nickname'is back from standby
        Iterator<ChatUser> iterator = chatUserList.iterator();
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(nickname)) {
                 chatUser.setStatus(ChatUser.userConnect);
                break;
            }
        }
        // updated list adapter. 'getActivity()' is a reference to activity which launch this fragment : 'TabChatActivity'.
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view.
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);
    }

    public void displayNotificationNotSeenMessage(Message message, String selectedNickname) {
        String from = message.fromNickname;
        String to   = message.toNickname;
        Iterator<ChatUser> iterator = chatUserList.iterator();
        if(from.equals(selectedNickname))return; //the user is reading the messages

        //Notify the 'to' user that he has received a new message from 'from'
        Snackbar.make(getView(),
                "You have received a new message from " + from,
                Snackbar.LENGTH_LONG).show();

        //update the number of not seen messages
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(from)) {
                //Get the current number of not seen messages and increment by 1.
                int i = chatUser.getNotSeenMessagesNumber();
                chatUser.setNotSeenMessagesNumber(i + 1);
                break;
            }
        }
        // update list adapter. 'getActivity()' is a reference to parent activity which launch this fragment : 'TabChatActivity'.
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view.
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);
    }

    public void displayReceivedBlacklist(String authorBlacklist, String statusBlacklist) {
        //received blacklist notification

        int status = 100; //dummy value
        String message = null;
        if((statusBlacklist.equals("blacklist"))){
            status  = ChatUser.userBlacklist;
            message = "You are blacklisted by : " + authorBlacklist;
        }
        if((statusBlacklist.equals("recover"))){
            status = ChatUser.userConnect;
            message = "You are covered by : " + authorBlacklist;
        }

        Iterator<ChatUser> iterator = chatUserList.iterator();


        //Notify the blcklisted user
        Snackbar.make(getView(),
                    message,
                    Snackbar.LENGTH_LONG)
                .show();

        //update the status
        while (iterator.hasNext()) {
            ChatUser chatUser = iterator.next();
            if (chatUser.getNickname().equals(authorBlacklist)) {
                chatUser.status = status;
                break;
            }
        }
        // update list adapter. 'getActivity()' is a reference to parent activity which launch this fragment : 'TabChatActivity'.
        UsersDataHolder.setData(chatUserList);
        chatUserAdapter = new ChatUserAdapter((ChatUserAdapter.UserData) getActivity(), getActivity());

        // notify the adapter to update the recycler view.
        chatUserAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        userRecylerView.setAdapter(chatUserAdapter);

    }
}
