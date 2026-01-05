package com.google.amara.chattab;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.ChatBoxAdapter;
import com.example.aymen.androidchat.Message;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotSeenMessagesFragment extends Fragment {
    private RecyclerView notSeenMessagesRecycler;
    private NotSeenMessagesAdapter      notSeenMessagesAdapter;
    public  static ArrayList<NotSeenMessage>   notSeenMessagesList;        //used in 'notSeenMessagesAdapter'r = list of all not seen messages

   public static NotSeenMessagesFragment newInstance(JSONArray jsonArray){
       final NotSeenMessagesFragment fragment = new NotSeenMessagesFragment();
       final Bundle args = new Bundle();
       //convert jsonArray to arrayList
       //List<NotSeenMessage> arraylist = new ArrayList<NotSeenMessage>(Arrays.asList(jsonArray));

       List<NotSeenMessage> notSeenMessages = new ArrayList<NotSeenMessage>();
       if(jsonArray != null){
           for(int i = 0; i <= jsonArray.length() - 1; i++){

               try {
                   JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                   NotSeenMessage notSeenMessage = new Gson().fromJson(jsonArray.getJSONObject(i).toString(), NotSeenMessage.class);

               /*
               NotSeenMessage notSeenMessage = new NotSeenMessage(
                       jsonObject.getString("nickname"),
                       jsonObject.getString("nb"),
                       jsonObject.isNull("imageprofile") ? null : jsonObject.getString("imageprofile")
               );
               */
                   notSeenMessages.add(notSeenMessage);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
           }
       }

       args.putParcelableArrayList("jsonArray", (ArrayList<? extends Parcelable>) notSeenMessages);
       fragment.setArguments(args); //called before the fragment is attached
       return fragment;
   }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.not_seen_messages, container, false);

        notSeenMessagesRecycler = view.findViewById(R.id.not_seen_messages_list);

        //get extra from bundle
        notSeenMessagesList     = getArguments().getParcelableArrayList("jsonArray");

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        notSeenMessagesRecycler.setLayoutManager(mLayoutManager);
        notSeenMessagesRecycler.setItemAnimator(new DefaultItemAnimator());

        notSeenMessagesAdapter = new NotSeenMessagesAdapter(getActivity(), notSeenMessagesList);
        // notify the adapter to update the recycler view
        //notSeenMessagesAdapter.notifyDataSetChanged();
        notSeenMessagesRecycler.setAdapter(notSeenMessagesAdapter);

        return  view;
    }
}
