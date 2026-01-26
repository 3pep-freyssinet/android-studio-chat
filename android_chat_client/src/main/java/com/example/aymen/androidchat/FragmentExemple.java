package com.example.aymen.androidchat;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


//public class MainActivity extends Fragment implements ChatBoxMessage.SendMessage{
public class FragmentExemple extends Fragment{
//public class MainActivity extends AppCompatActivity {

    private Button btn;
    private EditText nickname;
    public static final String NICKNAME = "user nickname";

    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_client_activity_main);
        //call UI component  by id
        btn      = (Button) findViewById(R.id.enterchat) ;
        nickname = (EditText)findViewById(R.id.nickname);
    */


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.chat_client_activity_main, container, false);

        Bundle bundle = this.getArguments();

        if(bundle != null){
            int currentTab = bundle.getInt("tab");
        }

        //call UI component  by id
        btn      = (Button) view.findViewById(R.id.enter_chat) ;
        nickname = (EditText) view.findViewById(R.id.nickname);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //if the nickname is not empty go to chatbox activity and add the nickname to the intent extra
                int i = 0;
             if(!nickname.getText().toString().isEmpty()){
                 //Intent intent  = new Intent(getActivity(), ChatBoxUsers.class);
                 //intent.putExtra("fragment_index", 0); //pass zero for Fragment one.
                 //intent.putExtra(NICKNAME, nickname.getText().toString());
                 //startActivity(intent);
                 //getChildFragmentManager().getFragments();
                 //sendMessage.sendData(nickname.getText().toString());
             }
            }
        });
        return view;
    }

    /*
    @Override
    public void sendData(String data) {
        ChatBoxUsers fragmentB = new ChatBoxUsers (); //fragment receiver or target
        Bundle args = new Bundle();
        args.putString("data", data);
        fragmentB .setArguments(args);
        getFragmentManager().beginTransaction()
                .replace(R.id.rl_activity_chat_box, fragmentB ) //Basically in transaction.replace() the first parameter will be the id of the layout (linear layout, relative layout, ...) in which you want to place your fragment. And the second parameter should be the fragment.
                .commit();
    }
     */
}
