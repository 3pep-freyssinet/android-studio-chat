package com.google.amara.chattab;

import android.os.AsyncTask;

import org.json.JSONObject;

import io.socket.client.Ack;
import io.socket.client.Socket;

public class UserAsyncTask extends AsyncTask<JSONObject, Void, JSONObject> {
    String username;
    Socket socket;

    //constructor
    public UserAsyncTask(Socket socket, String username){
        this.socket = socket;
        this.username = username;
    }
    @Override
    protected JSONObject doInBackground(JSONObject... jsonObjects) {
        final JSONObject[] user = new JSONObject[1];
        socket.emit("get_user", username, new Ack() {
            @Override
            public void call(Object... args) {
                user[0] = ((JSONObject) args[0]);
            }
        });
        return null;
    }

    protected void onPostExecute(JSONObject result) {
        //result is the final String;
        int i = 0;
    }
}
