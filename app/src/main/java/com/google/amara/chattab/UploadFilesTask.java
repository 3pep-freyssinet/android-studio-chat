package com.google.amara.chattab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.example.aymen.androidchat.Message;
import com.google.amara.chattab.upload.FileUploadManager;
import com.google.amara.chattab.upload.UploadFileMoreDataReqListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


//Class is extending AsyncTask because this class is going to perform a networking operation.
//AsyncTask<Params, Progress, Result> : doInBackground(String... params) ; onProgressUpdate(Void... values); nPostExecute(String result)
//Params — task's input data type
//Progress — how to inform the world about progress
//Result — task's output data type
public class UploadFilesTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "MainActivity";
    //private static final String URL = "http://192.168.0.103:8090";
    private static final String  URL = "http://10.0.2.2:3000";      //Emulator
    //private static final String URL = "http://localhost:3000";       //Device

    //private static final String UPLOAD_FILE_PATH = "/sdcard/com.irule.activity-1.apk"; // Make sure file exists ..
    //private static final String UPLOAD_FILE_PATH = "/data/data/com.google.amara.chattab/acrobat-trial.txt";
    private Context context;
    private FileUploadManager fileUploadManager;
    private Socket socket;
    private Uri uri;

    public interface Acknowledge{
        void acknowledge(JSONArray arguments);
    }
    public interface EventCallback{
        void onEvent(JSONArray argument, Acknowledge acknowledge);
    }
    //Constructor
    public UploadFilesTask(Context context, Socket socket, Uri uri ){
        this.context = context;
        this.socket  = socket;
        this.fileUploadManager = new FileUploadManager(context);
        this.uri = uri;
    }

    @Override
    protected String doInBackground(String... params) {
        //File file = context.getFilesDir();
        final String UPLOAD_FILE_PATH = getPathFromUri(uri);
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/acrobat_trial.txt";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/media2.mp4";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/media2.avi";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/avatar.jpg";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/test.txt";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/Avis_d_impot_2020_sur_les_revenus_2019.pdf";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/demo.jpg";
        //final String UPLOAD_FILE_PATH = context.getFilesDir().getPath()+"/IMG_20200526_191331.jpg";
        //final String UPLOAD_FILE_PATH = Environment.getExternalStorageDirectory().getPath()+"/Pictures/IMG_20200526_191331.jpg";

        //fileUploadManager.prepare(UPLOAD_FILE_PATH);

        // This function gets callback when server requests more data
        //setUploadFileMoreDataReqListener(uploadFileMoreDataReqListener);

        // This function will get a call back when upload completes
        //setUploadFileCompleteListener();

        // Tell server we are ready to start uploading ..

        JSONArray jsonArr = new JSONArray();
        JSONObject res    = new JSONObject();

        try {
            res.put("Name", fileUploadManager.getFileName());
            res.put("Size", fileUploadManager.getFileSize());
            jsonArr.put(res);

            // This will trigger node server 'uploadFileStart' function
            socket.emit("uploadFileStart", res); //jsonArr);

        } catch (JSONException e) {
            e.printStackTrace();
            //TODO: Log errors some where..
        }
        return null;
    }


    @SuppressLint("NewApi")
    private String getPathFromUri(Uri uri) {
        String filePath = "";
         String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    @Override
    protected void onPostExecute(String result) { }

    @Override
    protected void onPreExecute() { }

    @Override
    protected void onProgressUpdate(Void... values) { }

    private void setUploadFileCompleteListener() {

            EventCallback eventCallback = new EventCallback() {
                @Override
                public void onEvent(JSONArray argument, Acknowledge acknowledge) {
                    fileUploadManager.close();
                }
            };

            //mClient.addListener("uploadFileCompleteRes", eventCallback);

    }

    private UploadFileMoreDataReqListener uploadFileMoreDataReqListener = new UploadFileMoreDataReqListener() {

        @Override
        public void uploadChunck(int offset, int percent) {
            Log.v(TAG, String.format("Uploading %d completed. offset at: %d", percent,  offset));

            try {

                // Read the next chunk
                fileUploadManager.read(offset);// we can access 'getData()' and 'getBytesRead()'

                    JSONArray jsonArr = new JSONArray();
                    JSONObject res    = new JSONObject();

                    try {
                        res.put("Name", fileUploadManager.getFileName());
                        res.put("Data", fileUploadManager.getData());
                        res.put("chunkSize", fileUploadManager.getBytesRead());
                        jsonArr.put(res);

                        // This will trigger server 'uploadFileChuncks' function
                        socket.emit("uploadFileChuncks", jsonArr);
                    } catch (JSONException e) {
                        //TODO: Log errors some where..
                    }
            } catch (IOException e) {

            }

        }

        @Override
        public void err(JSONException e) {
            // TODO Auto-generated method stub

        }
    };

    private void setUploadFileMoreDataReqListener(UploadFileMoreDataReqListener callback ) {

            EventCallback eventCallback = new EventCallback() {
                @Override
                public void onEvent(JSONArray argument, Acknowledge acknowledge) {
                    for (int i = 0; i < argument.length(); i++) {
                        try {
                            JSONObject json_data = argument.getJSONObject(i);
                            int place   = json_data.getInt("Place");
                            int percent = json_data.getInt("Percent");

                            callback.uploadChunck(place, percent);
                            break;
                        } catch (JSONException e) {
                            callback.err(e);
                        }
                    }
                }
            };
            eventCallback.toString();
            //socket.addListener("uploadFileMoreDataReq", eventCallback);

    }
}