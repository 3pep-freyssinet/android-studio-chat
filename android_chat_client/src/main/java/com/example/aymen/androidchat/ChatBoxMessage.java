package com.example.aymen.androidchat;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aymen.androidchat.sql.MessagesContract;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

//Ce fragment est défini dans 'TabChatActivity-SectionPager'.
public class ChatBoxMessage extends Fragment {

    private static final int INTENT_REQUEST_CODE   = 100;
    private static final int REQUEST_TAKE_PHOTO    = 200;
    private static final int CAMERA_PERM_CODE      = 300;
    private static final String SEEN                = "1";
    private static final String NOTSEEN             = "0";
    private static final int MAX_ATTACHEMENTS       = 1;

    private TextView nickname_, idNickname, connectionNickname;
    private TextView selectedNickname, idSelectedNickname, connectionSelectedNickname;

    private TextView tvAttachment, tvDownload;
    private TextView tvItemAttachment0;
    public ImageView send, attach, camera;

    private ImageView imageProfile;
    private ImageView statusImageView;
    private ImageView thumbnail;
    private TableLayout tableLayout;    //display attachments to send.
    private LinearLayout llAttachment;  //linear layout witch contain the table layout (above).

    public static String Nickname, IdNickname;
    public String SelectedNickname, SelectedIdNickname;

    public Button               btn;  //it is enabled in 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'.
    //private EditText          edt;
    private TextInputLayout     edtOutlinedTextField;
    private TextInputEditText   edt;
    private RecyclerView        messageRecycler;
    private ChatBoxAdapter      chatBoxAdapter;
    public  ArrayList<Message>  chatMessageList;        //used in adapter = list of all messages
    private ArrayList<Message>  allMessagesReceived;    //only received messages
    private ArrayList<Message>  allMessageSent;         //only sent messages
    private ArrayList<Message>  newMessages;            //only new messages
    private ArrayList<ChatUser> chatListUsers;          //list of users in chat
    private ArrayList<String>   attachmentsUriPaths;    //list of attachments uri paths.
    private ArrayList<String>   attachments;            //list of attachments filename.
    private ArrayList<Bitmap>   attachmentThumbs;       //list of attachments thumbs bitmap
    private Bitmap thumbImage;
    private Message message;
    private boolean uploadComplete        = false;
    private static final String PDF_TYPE  = "pdf" ;
    private static final String JPG_TYPE  = "jpg" ;
    private static final String MP3_TYPE  = "mp3" ;
    private static final String MP4_TYPE  = "mp4" ;
    private static final String TXT_TYPE  = "txt";
    private static final String JPEG_TYPE = "jpeg";

    private final int limit = 10;   //number of message retrieved from database

    //camera fields
    private Uri photoURI;
    private String imageFileName;
    private String currentPhotoPath;

    public static View customCircularProgress;  //used when downloadin messages from server

    public MessageSwipeRecyclerView messageSwipeRecyclerView;
    private int ii;
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //interface
    public SendMessage sendMessage;
    private String attachmentUriPath;
    private Activity mActivity;

    public void sendNetworkNotification(String networkStatus) {
        switch (networkStatus) {
            case "Wifi enabled":
            case "Mobile data enabled":
                //if(!nickname.getText().toString().isEmpty())btn.setEnabled(true);
                break;
            case "No internet is available":
                if(!isVisible())return;
                //Snackbar.make(getView(), "Status ChatBoxMessage : = " + networkStatus, Snackbar.LENGTH_LONG).show();

                androidx.appcompat.app.AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
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

    public interface SendMessage {
        void sendMessage(String toId, Message messageFrom, ArrayList<String> attachmentsUri);
        void uploadAttachment(String attachmentUriPath, Message messageFrom);
        void getMessagesFromServer(String SelectedNickname, String Nickname, String IdNickname);
        void updateMessage(String ref, String delete);

        //void chatBoxMessageFragmentAttached();
    }

    //interface
    private DownloadAttachment downloadAttachment;
    public interface DownloadAttachment {
        void downloadAttachment(String id);
    }

    public enum MessagesDataHolder {
        INSTANCE;

        private ArrayList<Message> messages;

        public static boolean hasData() {
            return INSTANCE.messages != null;
        }

        public static void setData(final ArrayList<Message> messages) {
            INSTANCE.messages = messages;
        }

        public static ArrayList<Message> getData() {
            final ArrayList<Message> retList = INSTANCE.messages;
            INSTANCE.messages = null;
            return retList;
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box_);

        int defaultValue = 0;
        int page = getIntent().getIntExtra("fragment_index", defaultValue);
        //viewPager.setCurrentItem(page);

    }
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null){

            ii = savedInstanceState.getInt("some_int");
            chatMessageList = savedInstanceState.getParcelableArrayList("chatMessageList");
            //System.out.println("************ onCreate ******chatMessageList = " + chatMessageList.size());
        }else{
            ii = 0;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //if(savedInstanceState != null) {
        //    System.out.println("************ onCreateView *****messageRecycler = " + (messageRecycler == null));
        //    System.out.println("************ onCreateView *****chatBoxAdapter = " + chatBoxAdapter.messageList);
        //}

        if(savedInstanceState != null) {

            //System.out.println("************onCreateView savedInstanceState *****************");
            this.Nickname         = savedInstanceState.getString("Nickname", null);
            this.SelectedNickname = savedInstanceState.getString("SelectedNickname", null);
            this.message          = savedInstanceState.getParcelable("message");

            chatMessageList       = savedInstanceState.getParcelableArrayList("chatMessageList");

            //camera
            String photoURI_      = savedInstanceState.getString("photoURI");
            this.photoURI = (photoURI_ != null)  ?  Uri.parse(photoURI_) : null;

            imageFileName         = savedInstanceState.getString("imageFileName");
            currentPhotoPath      = savedInstanceState.getString("currentPhotoPath");
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.chat_box_message, container, false);

        idNickname          = view.findViewById(R.id.id_nickname);
        nickname_           = view.findViewById(R.id.nickname_);
        connectionNickname  = view.findViewById(R.id.connection_nickname);

        idSelectedNickname  = view.findViewById(R.id.id_selected_nickname);
        selectedNickname    = view.findViewById(R.id.selected_nickname);
        connectionSelectedNickname = view.findViewById(R.id.connection_selected_nickname);

        imageProfile        = view.findViewById(R.id.imageView);
        statusImageView     = view.findViewById(R.id.status);

        //thumbnail           = view.findViewById(R.id.thumbnail);

        //messageFrom       = view.findViewById(R.id.message_from);
        //messageFromTime     = view.findViewById(R.id.message_from_time);
        //messageTo         = view.findViewById(R.id.message_to);
        //messageToTime       = view.findViewById(R.id.message_to_time);

        messageRecycler     = view.findViewById(R.id.message_list);
        edt                 = view.findViewById(R.id.edt_message);
        edtOutlinedTextField= view.findViewById(R.id.edtOutlinedTextField);
        tableLayout         = view.findViewById(R.id.tl_attachment);    //table layout for Attachment items
        send                = view.findViewById(R.id.iv_send);
        attach              = view.findViewById(R.id.iv_attach);         //join attachment to message
        camera              = view.findViewById(R.id.iv_camera);

        llAttachment        = view.findViewById(R.id.ll_attachment);    //linear layout for attachment, it contains a table layout to display attachments.
        customCircularProgress = view.findViewById(R.id.custom_circular_progress);

        /*
        //Not used
        messageRecycler.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                view.getParent();
                event.getX();
                v.getParent();
                RecyclerView rv     = (RecyclerView) v.findViewById(R.id.message_list);
                View item           = rv.findChildViewUnder(event.getX(), event.getY()); //finding the view that clicked , using coordinates X and Y
                int position        = rv.getChildLayoutPosition(item); //
                ChatBoxAdapter adapter = (ChatBoxAdapter) rv.getAdapter();
                List<Message> list = adapter.messageList;
                Message message    = list.get(position);

                //ne marche pas
                int swipeDirection = 0;
                //Set the swipe direction
                if(Nickname.equals(message.fromNickname))swipeDirection = ItemTouchHelper.RIGHT;
                if(Nickname.equals(message.toNickname))  swipeDirection = ItemTouchHelper.LEFT;

                //swipe recycler items left or right
                //swipeRecyclerView = new SwipeRecyclerView(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT , ChatBoxMessage.this);
                swipeRecyclerView = new SwipeRecyclerView(0, swipeDirection , ChatBoxMessage.this);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeRecyclerView);
                itemTouchHelper.attachToRecyclerView(messageRecycler);

                return false;
            }
        });
        */

        //messageSwipeRecyclerView = new MessageSwipeRecyclerView(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT , ChatBoxMessage.this);
        //ItemTouchHelper itemTouchHelper = new ItemTouchHelper(messageSwipeRecyclerView);
        //itemTouchHelper.attachToRecyclerView(messageRecycler);


    //tvDownload = view.findViewById(R.id.tv_download_attachment);

    /*
    tvDownload.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            downloadAttachment();
        }
    });
    */
    //edt event
    edt.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            send.setEnabled(false);      //send message
            send.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.send_gray));

            attach.setEnabled(false);    //add attachment
            attach.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.merge_icon_gray));

            camera.setEnabled(false);    //take photo
            camera.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.camera_icon_gray));

            //change the color or delete icon at the end of edt to gray color
            //ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(getActivity(), R.color.colorGray));
            //edtOutlinedTextField.setEndIconTintList(colorStateList);
            if (s.length() != 0) {
                send.setEnabled(true);
                send.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.send));

                attach.setEnabled(true); //join attachment to message
                attach.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.merge_icon));

                camera.setEnabled(true);
                camera.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.camera_icon));

                //change the color of delete icon at the end of 'edt' to red color
                ColorStateList colorStateList = ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.colorRed));
                edtOutlinedTextField.setEndIconTintList(colorStateList);

                //hide the 'hint' message
                edtOutlinedTextField.setHintEnabled(false);

                //Build the message
                message = new Message(Nickname, SelectedNickname,   //from=Nickname, to=SelectedNickname
                        edt.getText().toString(), new Date().getTime(), null, null,
                        ChatBoxMessage.SHA1(), null, "0", "0", "0");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    });

        /*
        //Send button Event
        btn.setEnabled(false); //at starting. It is enabled in 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'.
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performClick();
                //Send message to server
                //sendMessage.sendMessage(fromNickname_, idFromNickname_, toNickname_, idToNickname_, edt.getText().toString());
            }
        });
        */

        //Send button Event.
        //There are 2 conditions to enable 'send' button :
        // -- the attachment, if any' is uploaded in 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'.
        // -- the 'edt' is not empty
        // A message can have no attachment.
        send.setEnabled(false); //at starting. It will be enabled when there is a message in
        // 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'.
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast. makeText(getActivity(), "Send clicked", Toast. LENGTH_LONG).show();

                if(edt.getText().length() == 0){
                    //notify the user that the body of the message is empty
                    Snackbar.make(getView(), "The body of the message is empty.", Snackbar.LENGTH_LONG).show();

                    /*
                    new AlertDialog.Builder(getActivity()).
                            setMessage("The body of the message is empty").
                            setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    return;
                                }
                            }); //.setNegativeButton("Cancel", null).create().show();
                    */

                    return;
                }
                //Press the 'send' button once.
                send.setEnabled(false);
                send.setImageDrawable(getResources().getDrawable(R.drawable.send_gray));

                //is there an attachment ?
                if(attachments.size() != 0){
                    //The 'send' button is enabled in 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'
                    //if(!send.isEnabled())return;
                }

                performClick();

                //Disable 'Send', 'Attach' and 'Camera' buttons after sending the message. They are
                //enabled when there is a message.
                send.setEnabled(false);
                attach.setEnabled(false);
                camera.setEnabled(false);
                //Hide the layout containing the attaachment.
                //if(llAttachment.getVisibility() == View.VISIBLE)llAttachment.setVisibility(View.GONE);

                //Send message to server
                //sendMessage.sendMessage(fromNickname_, idFromNickname_, toNickname_, idToNickname_, edt.getText().toString());
            }
        });


        //Upload attachment. Select and pick one image from device gallery and send it to server.
        //An attachment may be any any document in Mediastore : images, audio, video and files.
        attach.setEnabled(false); //at startup. It will be enabled when there is a message.
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //the 'attachments' array list is reset in 'perform' method
                if(attachments.size() == MAX_ATTACHEMENTS){
                    Snackbar.make(getView(), "One attachment is allowed.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                //Select and pick one image from device gallery and send it attached with the message.
                String[] mimeTypes =
                        {"application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document",                    // .doc & .docx
                                "application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
                                "application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",              // .xls & .xlsx
                                "text/plain",
                                "application/pdf",
                                "application/zip",
                                "image/*",
                                "audio/*", "video/*"};

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI ); //the images are found in '//data/user/0/[package]/files'

                //Intent intent = new Intent(Intent.ACTION_PICK);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    //intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "image/*");
                    if (mimeTypes.length > 0) {
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    }
                } else {
                    String mimeTypesStr = "";
                    for (String mimeType : mimeTypes) {
                        mimeTypesStr += mimeType + "|";
                    }
                    intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1)); // le -1 pour retirer le dernier '|'
                }

                /****************
                specify first MIME-type in intent.SetType, all the rest via intent.PutExtra(Intent.ExtraMimeTypes, ...)
                List<string> mimeTypes = new List<string>();
                // Filling mimeTypes with needed MIME-type strings, with * allowed.

                if (mimeTypes.Count > 0){
                    // Specify first MIME type
                    intent.SetType(mimeTypes[0]);
                    mimeTypes.RemoveAt(0);
                }
                if (mimeTypes.Count > 0){
                    // Specify the rest MIME types but the first if any
                    intent.PutExtra(Intent.ExtraMimeTypes, mimeTypes.ToArray());
                }

                 **********************/

                //intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

                //intent.setType("image/jpg");
                //intent.setType("application/pdf");
                //intent.setType("image/*");
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivityForResult(intent, INTENT_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        //Camera button Event
        camera.setEnabled(false); //at starting. It will be enabled when there is a message. in 'TabChatActivity.socket.on("uploadFileComplete", new Emitter.Listener()'.
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast. makeText(getActivity(), "Camera clicked", Toast. LENGTH_LONG).show();
                if(attachments.size() == MAX_ATTACHEMENTS){
                    Snackbar.make(getView(), "One attachment is allowed.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                //take picture with camera
                dispatchTakePictureIntent();
            }
                //Send message to server
                //sendMessage.sendMessage(fromNickname_, idFromNickname_, toNickname_, idToNickname_, edt.getText().toString());
        });


        Bundle bundle   = getArguments();
        //Nickname    = "Nickname";
        if(bundle != null) {
            //Nickname   = bundle.getString("Nickname");
        }

        //chatMessageListFrom = new ArrayList<>();
        chatMessageList     = new ArrayList<>();
        //oldMessageListUser  = new ArrayList<>();
        allMessagesReceived = new ArrayList<>();
        //chatMessageSent     = new ArrayList<>();
        allMessageSent      = new ArrayList<>();
        newMessages         = new ArrayList<>();
        chatListUsers       = new ArrayList<>();
        attachmentsUriPaths = new ArrayList<>();    //attachments with paths to uris
        attachments         = new ArrayList<>();    // list of attachments filename
        attachmentThumbs    = new ArrayList<>();    // list of attachments thumb bitmap.

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        messageRecycler.setLayoutManager(mLayoutManager);
        messageRecycler.setItemAnimator(new DefaultItemAnimator());

        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity());

        messageRecycler.setAdapter(chatBoxAdapter);

        return view;

        /*
        int fragmentId = getIntent().getIntExtra("FRAGMENT_ID", 0);
        Bundle bundle = new Bundle();
        bundle.putInt("TARGET_FRAGMENT_ID", fragmentId);
        TabFragment tabFragment = new TabFragment();
        tabFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction().replace(R.id.confrag, tabFragment).commit();

        // Inside OnCreateView()
        int position = getArguments().getInt("TARGET_FRAGMENT_ID");
        viewpager.setCurrentItem(position);
        */
    }

    @Override
    public void onViewStateRestored(Bundle inState) {
        super.onViewStateRestored(inState);
        if(inState != null) {

                //System.out.println("************ onViewStateRestored *****************");
            this.Nickname         = inState.getString("Nickname", null);
            this.SelectedNickname = inState.getString("SelectedNickname", null);

            int ii            = inState.getInt("some_int", 0);
            String idNickname = inState.getString("idNickname", "xxxxxxxxxxx");
            String nickname_  = inState.getString("nickname_", "yyyyyyy");
            String connectionNickname = inState.getString("connectionNickname", "zzzzzz");
            this.idNickname.setText(idNickname);
            this.nickname_.setText(nickname_);
            this.connectionNickname.setText(connectionNickname);

            //System.out.println("*********************** onViewStateRestored " + ii);

            String idSelectedNickname = inState.getString("idSelectedNickname", "xxxxxxxxxxx");
            String selectedNickname   = inState.getString("selectedNickname", "yyyyyyy");
            String connectionSelectedNickname = inState.getString("connectionSelectedNickname", "zzzzzz");
            this.idSelectedNickname.setText(idSelectedNickname);
            this.selectedNickname.setText(selectedNickname);
            this.connectionSelectedNickname.setText(connectionSelectedNickname);

            chatMessageList = inState.getParcelableArrayList("chatMessageList");
            chatListUsers   = inState.getParcelableArrayList("chatListUsers");

            //Restore the recycler view
            Parcelable messageRecycler_ = inState.getParcelable("messageRecycler");
            this.messageRecycler.getLayoutManager().onRestoreInstanceState(messageRecycler_);

            //chatMessageList.subList(0, 5);
            //ArrayList<Message> arrayList = new ArrayList<>(chatMessageList.subList(0, 5));
            // chatMessageList.addAll(arrayList);

            MessagesDataHolder.setData(chatMessageList);
            chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity());
            messageRecycler.setAdapter(chatBoxAdapter);
            //Scroll to the last position.
            messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);

           // System.out.println("************ onViewStateRestored *****chatMessageList = " + chatMessageList.size());
            //System.out.println("************ onViewStateRestored *****messageRecycler = " + messageRecycler.getAdapter().getItemCount());
            //System.out.println("************ onViewStateRestored *****chatBoxAdapter = " + chatBoxAdapter.messageList);

        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException exception) {
                // Error occurred while creating the File
                exception.getMessage();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //N=24
                    photoURI = FileProvider.getUriForFile(getActivity(),
                            "com.example.aymen.androidchat.cameraFileprovider",
                            photoFile);
                } else {
                    photoURI =  Uri.fromFile(photoFile);
                }

                //In case of thumbnail do not use the following statement
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                //}catch(IllegalArgumentException ex){
            }
       }
    }

    //Create image file for camera
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(new Date());
        imageFileName = "JPEG_" + timeStamp + "_"; //il est tres important le dernier '_',. il permer de séparer le nombre aléatoire ajouté par  'createTempFile' plus bas.

        //the pictures will be found in device file explorer in : storage/sdcard0/Android/data/<package>/files/Pictures/....jpg
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);  //storage/emulated/0/Android/dta/<package>/files/Pictures dans gallery il y a creation dun dossier 'Pictures'

        //File storageDir = Environment.getExternalStorageDirectory();  //storage/emulated/0
        //File storageDir = getFilesDir();    //data/user/0/<package>/files dans le device file explorer, il y a : /data/data/<package>:files/....jpg
        //File storageDir = getActivity().getCacheDir();

        //Camera photos
        File storageDir   = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //File storageDir   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //File root = Environment.getExternalStorageDirectory();
        //File storageDir = new File(root.getAbsolutePath()+"/DCIM/");

        //File storageDir = new File(getFilesDir().getPath());  //ne marche pas :exception
        //File imagesFolder = new File(storageDir, "camera");
        //imagesFolder.mkdirs();
        //File imageFile = new File(storageDir, imageFileName+".jpg");

        //
        //Avant l'extension '.jpg', il a insertion automatique d'un nombre aleatoire qui disparait avec 'imageFile.deleteOnExit()'
        //
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                   imageFileName,     /* prefix */
                    ".jpg",     /* suffix */
                    storageDir        /* directory */
            );
        }catch (IOException ex){
            ex.getMessage();
        }

        // deletes file when the virtual machine terminate.
        imageFile.deleteOnExit();

        //  path used in mediastore column '_DATA'. See galleryAddPicture
        currentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    /**
     * Update the messages sent by 'fromNickname' to 'toNickname'. The 'seen' field of the message is set to 1
     * @param fromNickname  the user who send messages
     * @param toNickname    the user who receive the messages
     */
    public void updateMessages(String fromNickname, String toNickname){
        //Get the messages from sqlite db sent 'toNickname' to 'fromNickname'  wich are not seen
        //and set them seen
        Log.i("updateMessages", "from = " + fromNickname + " to = " + toNickname);

        ArrayList<Message> notSeenMessages = getMessagesNotSeen(fromNickname, toNickname);

        //in sqlite db, update all messages sent by 'fromNickname' to 'toNickname'. set 'seen' field = 1
        int updatedMessageNumber = updateMessagesNotSeen(fromNickname, toNickname);

        //ToDo : update the unseen messages in server, it is done in 'emit' in 'TabChatActivity.sendUserData'

        //if(notSeenMessages.isEmpty())getMessagesNotSeenFromServer(fromNickname, toNickname);

        for(Message message : notSeenMessages){
            message.seen = SEEN;
        }

        //update the adapter only if the selected = toNickname
        if(SelectedNickname == null)return;
        if(SelectedNickname.equals(toNickname)){

            //update the 'chatMessageList'. It is done above
            Iterator<Message> iterator = chatMessageList.iterator();
            while (iterator.hasNext()){
                Message message = iterator.next();
                if((message.fromNickname.equals(fromNickname)) & (message.toNickname.equals(toNickname))){
                    message.seen = SEEN;
                }
            }

            for(Message message : chatMessageList){
                message.seen = SEEN;
            }

            // Update the adapter.
            MessagesDataHolder.setData(chatMessageList);
            chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

            //set the adapter for the recycler view
            messageRecycler.setAdapter(chatBoxAdapter);

            // notify the adapter to update the recycler view.
            chatBoxAdapter.notifyDataSetChanged();

            //Scroll to the last position.
            messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
        }
    }

    //Set the fiels 'seen' = 1 for all messages sent by 'fromNickname' to 'toNickname'
    private int updateMessagesNotSeen(String fromNickname, String toNickname) {

        // update all the messages in table 'chat_messages' that are not seen to seen.

        ContentValues contentValues = new ContentValues();
        contentValues.put(MessagesContract.COLUMN_SEEN , SEEN);

        int rows = getContext().getContentResolver().update(MessagesContract.CONTENT_URI_MESSAGES,
                contentValues,
                MessagesContract.COLUMN_FROM + " =? AND "
                        +MessagesContract.COLUMN_TO + " =? AND "
                        +MessagesContract.COLUMN_SEEN + " LIKE ? ",

                new String[]{fromNickname, toNickname, NOTSEEN }
        );
        return  rows;
    }

    private ArrayList<Message> getMessagesNotSeen(String fromNickname, String toNickname) {
        ArrayList<Message> messagesNotSeen = new ArrayList();
        // Get all the messages from the database that are not seen and put them in ArrayList.

        String[] mProjection = new String[]
                {
                        MessagesContract.COLUMN_ID,
                        MessagesContract.COLUMN_FROM,
                        MessagesContract.COLUMN_TO,
                        MessagesContract.COLUMN_MESSAGE,
                        MessagesContract.COLUMN_REFERENCE,
                        MessagesContract.COLUMN_DATE,
                        MessagesContract.COLUMN_EXTRA,
                        MessagesContract.COLUMN_EXTRANAME,
                        MessagesContract.COLUMN_MIME,
                        MessagesContract.COLUMN_SEEN,
                        MessagesContract.COLUMN_DELETED_FROM,
                        MessagesContract.COLUMN_DELETED_TO
                };

        //Method : ContentProvider
        Cursor cursor = getContext().getContentResolver().query(
                MessagesContract.CONTENT_URI_MESSAGES,
                mProjection,
                MessagesContract.COLUMN_FROM  + " =? AND "  +
                        MessagesContract.COLUMN_TO    + " =? AND "  +
                        MessagesContract.COLUMN_SEEN  + " LIKE ? ",

                new String[]{fromNickname, toNickname, NOTSEEN },

                MessagesContract.COLUMN_DATE + " DESC");

        if (cursor != null) {
            cursor.moveToPosition(-1);
        }

        long id             = 0;
        String from         = null;
        String to           = null;
        String message      = null;
        String reference    = null;
        long date           = 0;
        String extra        = null;
        String extraName    = null;
        String mimeType     = null;
        String seen         = null;
        String deletedFrom  = null;
        String deletedTo    = null;


        int i = 0;
        while (cursor.moveToNext()) {
            id          = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_ID));
            from        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_FROM));
            to          = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_TO));
            message     = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MESSAGE));
            reference   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_REFERENCE));
            date        = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DATE));
            extra       = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRA));
            extraName   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRANAME));
            mimeType    = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MIME));
            seen        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_SEEN));
            deletedFrom = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_FROM));
            deletedTo   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_TO));


            // Build the 'Messages' object.
            Message message_ = new Message(from, to, message, date, extra, extraName, reference,
                    mimeType, seen, deletedFrom, deletedTo);

            messagesNotSeen.add(message_);
            i++;
            if(i >= limit)break;
        }
        cursor.close();
        return messagesNotSeen;
    }


    private void downloadAttachment() {
        //send a request do server.
        downloadAttachment.downloadAttachment("");
    }

    /**
     * Gallery picker
     * @param requestCode
     * @param resultCode
     * @param data
     */

    @SuppressLint("NewApi")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* back from : camera-photo
        The default Android camera application returns a non-null intent only when passing back a
        thumbnail in the returned Intent. If you pass EXTRA_OUTPUT with a URI to write to,
        it will return a null intent and the picture is in the URI that you passed in.
         */

        if ((requestCode != REQUEST_TAKE_PHOTO ) && (data == null)) return;

        //all media except camera
        if (requestCode == INTENT_REQUEST_CODE && null != data) {
            //if (requestCode == INTENT_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
            // Get the uri of the selected image. It is like : 'content://...filename.jpg'
            // selectedImageUri.getpath() is like '/document/3334-6339:DCIM/Camera/20170815_085513.jpg'
            Uri selectedImageUri = null;
            if (data != null) selectedImageUri = data.getData();

            //FileDescriptor fd = null;
            //try {
            //    fd = getActivity().getContentResolver().openFileDescriptor(selectedImageUri, "r").getFileDescriptor();   //'r' = read
            //} catch (FileNotFoundException e) {
            //    e.printStackTrace();
            //}

            ////////////////////////////////////////////////////////////////////////////////////////
            /*
            // DocumentProvider
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat && DocumentsContract.isDocumentUri(getActivity(), selectedImageUri)) {
                // ExternalStorageProvider
                if ("com.android.externalstorage.documents".equals(selectedImageUri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(selectedImageUri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        String s = Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if ("com.android.providers.downloads.documents".equals(selectedImageUri.getAuthority())) {

                    final String id = DocumentsContract.getDocumentId(selectedImageUri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    //return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if ("com.android.providers.media.documents".equals(selectedImageUri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(selectedImageUri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    Cursor cursor = null;
                    final String column = "_data";
                    final String[] projection = {
                            column
                    };

                    try {
                        cursor = getActivity().getContentResolver().query(selectedImageUri, projection, selection, selectionArgs,
                                null);
                        if (cursor != null && cursor.moveToFirst()) {
                            final int column_index = cursor.getColumnIndexOrThrow(column);
                            cursor.getString(column_index);
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    //return null;
                }
            }
            */
            ////////////////////////////////////////////////////////////////////////////////////////

            //No entry for ...
            //try {
            //    ParcelFileDescriptor fileDescriptor = getActivity().getContentResolver().openFileDescriptor(selectedImageUri, "r");
            //} catch (FileNotFoundException e) {
            //    e.printStackTrace();
            //}
            //getPath(selectedImageUri);
            ////////////////////////////////////////////////////////////////////////////////////////
            //Cette partie sert à savoir si 'selectedImageUri' est accessible ou pas
            InputStream is = null;
            try {
                is = getActivity().getContentResolver().openInputStream(selectedImageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Snackbar.make(getView(), "Cannot get image from gallery.", Snackbar.LENGTH_LONG).show();
                return;
            }catch(IllegalStateException e){
                e.printStackTrace();
                Snackbar.make(getView(), "Cannot get image from gallery.", Snackbar.LENGTH_LONG).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            ///////////////////////////////////////////////////////////////////////////////////////
            /*
            String[] projection = { MediaStore.Images.Media._ID };

            Cursor cursor2 = getActivity().getContentResolver().query(selectedImageUri, projection,
                    null, null, null);
            //if( cursor2 != null ) {
            //    int column_index = cursor2
            //            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            //    cursor2.moveToFirst();
            //    String path = cursor2.getString(column_index);
            //
            //}
            cursor2.close();
            */
            ////////////////////////////////////////////////////////////////////////////////////////
            // tests qui ne servent par la suite
            /*
            Bitmap bitmap0 = BitmapFactory.decodeFile(selectedImageUri.getPath());//for test only
            Bitmap bitmap1 = BitmapFactory.decodeFile(getPath(selectedImageUri));

            //Check if the uri point to a storage (internal or external)
            boolean isExternalStorageDocument  = "com.android.externalstorage.documents".equals(selectedImageUri.getAuthority());
            boolean isExternalStorageDocument_ =  isExternalStorageDocument(selectedImageUri);
            */
            ////////////////////////////////////////////////////////////////////////////////////////
            /*
            String filePath = "";
            // External Storage Provider
            String docId_ = DocumentsContract.getDocumentId(selectedImageUri); //like 3334-6339:Download/filename
            String[] split_ = docId_.split(":");
            String type = split_[0];

            if ("primary".equalsIgnoreCase(type)) {
                filePath =  Environment.getExternalStorageDirectory() + "/" + split_[1];
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    //getExternalMediaDirs() added in API 21
                    File[] external = getActivity().getExternalMediaDirs();
                    if (external.length > 1) {
                        filePath = external[1].getAbsolutePath();
                        filePath = filePath.substring(0, filePath.indexOf("Android")) + split_[1]; //like /storage/3334-6339/Download/filename
                    }
                } else {
                    filePath = "/storage/" + type + "/" + split_[1];
                }
            }
            */
            ////////////////////////////////////////////////////////////////////////////////////////
            //Get type of file. It is of the form : "media/format" for example : 'application/pdf'

            ////////////////////////////////////////////////////////////////////////////////////////
            /*
            String[] proj = { MediaStore.Images.Media.DATA };
            CursorLoader loader = new CursorLoader(mContext, contentUri, proj, null, null, null);
            Cursor cursor = loader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String result = cursor.getString(column_index);
            cursor.close();
             */
            ////////////////////////////////////////////////////////////////////////////////////////
            // for tests
            /*
            String path     = getPath(selectedImageUri);
            File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null,
                    null, null);


            cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ? ",
                    new String[] {"Pictures"},
                    MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
            int id_index            = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int data_index          = cursor.getColumnIndexOrThrow("_data");
            int title_index         = cursor.getColumnIndexOrThrow("title");
            int display_name_index  = cursor.getColumnIndexOrThrow("_display_name");
            int mime_type_index     = cursor.getColumnIndexOrThrow("mime_type");
            int bucket_index        = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID);
            //int document_index      = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DOCUMENT_ID);
            //int volume_index        = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.VOLUME_NAME);

            String id;
            String data2;
            String title;
            String display_name;
            String mime_type;
            String bucket;
            String document;
            String volume;

            //cursor.moveToFirst();
            cursor.moveToPosition(-1);
            System.out.println("********************************************************");
            while(cursor.moveToNext()){
                for(int i = 0; i < cursor.getColumnCount(); i++){
                    System.out.println(cursor.getColumnName(i)+" = "+cursor.getString(i));
                }
                */
                /*
                id = cursor.getString(id_index);
                long id_ = Long.parseLong(id);
                Uri uri1 = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
                String path1 = getPath(uri1);

                Bitmap bitmap9 = BitmapFactory.decodeFile(path1);

                data2 = cursor.getString(data_index);
                title = cursor.getString(title_index);
                display_name = cursor.getString(display_name_index);
                mime_type = cursor.getString(mime_type_index);
                bucket  = cursor.getString(bucket_index);
                //document  = cursor.getString(document_index);
                //volume  = cursor.getString(volume_index);

                System.out.println("data = "+data2+" title = "+title+" display_name = "+display_name+
                        " mime_type = "+mime_type+" bucket_number = "+bucket);
                */
                /*
            }
            cursor.close();

            System.out.println("********************************************************");
           */
            //int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            //cursor.moveToFirst();
            //String name_ = cursor.getString(nameIndex);
            ////////////////////////////////////////////////////////////////////////////////////////

            //Get mime type from 'selectedImageUri'
            //String fileType = getActivity().getContentResolver().getType(selectedImageUri);
            String mimeType = getMimeType(selectedImageUri);
            message.mimeType = mimeType;

            //Todo mimeType == null

            //Get type or extension from mime type.
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(mimeType); //"pdf", "jpg", "mp3", ...

            Bitmap attachmentThumbBitmap = null;
            switch(extension) {
                case PDF_TYPE:
                    //type = "pdf";
                    attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.pdf_icon);
                    break;
                case JPG_TYPE :         ////"jpg" OR "jpeg"
                case JPEG_TYPE :
                    //type = "jpg" OR "jpeg";
                    //Get the thumb from uri. only for bitmap
                    Bitmap attachmentThumbBitmap_ = null;
                    try {
                        attachmentThumbBitmap_ = getThumbnail(selectedImageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Get the thumbBitmap of the default 'jpg_icon' to use in case there is no thumnBitmap of the 'selectedUri'
                    attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.jpg_icon);
                    attachmentThumbBitmap = (attachmentThumbBitmap_== null) ? attachmentThumbBitmap : attachmentThumbBitmap_;
                    break;
                case MP3_TYPE:
                    //type = "mp3";
                    attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.mp3_icon);
                    break;
                case MP4_TYPE:
                    //type = "mp4";
                    attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.mp3_icon);
                    break;
                case TXT_TYPE:
                    //type = "txt";
                    attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                            R.drawable.txt_icon);
                    break;
                default:
                    // Not supported type, notify the user
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatBoxMessage.this.getActivity());
                    builder.setTitle("Type not supported");
                    builder.setMessage("Type not supported.");

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            });

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    //.setNegativeButton(android.R.string.no, null)
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    //.show();

                    AlertDialog alert = builder.create();
                    alert.show();
                    return;

            }//end switch

            attachmentUriPath = getPath(selectedImageUri);

            //Get diplay name of 'selectedImageUri' and assign it to  the textView name.
            String  undefined     = "undefined";
            String attachmentName = getDisplayname(selectedImageUri);
            attachmentName = (attachmentName == null) ? undefined : attachmentName; //the extension is included

            //Check the length of filename. According to db table column 'extraname' table = 'messages'.
            // The length is set to 50.
            //then, the filename without extension do not over 45 characters. It is assumed that the
            // extension is 5 characters, dot included like '.html', 'jpeg', 'mpeg', ...

            //Get the length of extension
            int extensionLength = extension.length();
            int attachmentNameLength = attachmentName.length() - (extensionLength + 1); //1 for the dot in extension
            if(!attachmentName.equals(null)){
                if(attachmentName.length() > 50){
                    attachmentName =
                            attachmentName.substring(0, attachmentNameLength); //attachmentNameLength est exclu, le 0 est inclu
                    attachmentName += "." + extension;
                }
            }

            if(attachmentName.length() > 50) attachmentName = "long filename";

            //update the image view
            //if(attachmentThumbBitmap != null){
            //thumbnail.setImageBitmap(attachmentThumbBitmap);
            //Get string encoded base-64 thumb
            String attachmentThumbString = bitmapToString(attachmentThumbBitmap); //x64

            //add the thumb image to 'Message'
            message.setExtra(attachmentThumbString);    //string base-64
            //message.setExtra("");
            message.setExtraName(attachmentName);       //the extension is included

            //Add selected uri path to attachment array list.
            attachmentsUriPaths.add(attachmentUriPath);

            //Add selected picture to attachment.
            //attachmentFiles.add(picturePath);   //---> Media.DATA
            //attachmentNames.add(pictureName);   //---> Media.DISPLAY_NAME

            //Display the flow layout containing the attachments.
            ChatBoxMessage.this.tableLayout.removeAllViews();

            attachments.add(attachmentName);
            attachmentThumbs.add(attachmentThumbBitmap);
            tableLayout.removeAllViews();
            displayAttachments(attachments, attachmentThumbs);

            //Send the attachment to server.
            //sendMessage.uploadAttachment(uriPath, message);//go to 'TabChatActivity.uploadAttachment'

            //Show the layout containing attachment
            llAttachment.setVisibility(View.VISIBLE);

            //Enable 'send' button to send message.
            send.setEnabled(true);

            //The flow layout containing the attachments is displayed when the button is clicked. See 'ChatBoxMessage.onPerform'.

        }
        //Camera
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == getActivity().RESULT_OK) {

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoURI);//'photoUri' is uri of camera photo built with file provider. see 'createImageFile'
            } catch (IOException e) {
                e.printStackTrace();
            }
            // To use in case of thumbnail
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //imageView.setImageBitmap(imageBitmap);

            //To use in case the image is saved in file
            //Scale the bitmap to 200x200 pixels and display it in imageView.
            //Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200,200, true);
            //imageView.setImageBitmap(resized);
            //setPic();

            //Add selected uri path to attachment array list.

            //attachmentsUriPaths.add(attachmentUriPath);
            ///attachmentUriPath = photoURI.getPath();
            //attachmentsUriPaths.add(attachmentUriPath);

            //add camera photo to gallery
            galleryAddPicture(getActivity());
        }
    }

    //'onActivityResult' camera part
    public void onActivityResultNext(){
        //from here, we deal with attachment below the 'Send' button
        //Get type of file. It is of the form : "media/format" for example : 'application/pdf'

        //String fileType = getActivity().getContentResolver().getType(photoURI);
        //MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        //String fileType_ = mimeTypeMap.getExtensionFromMimeType(fileType); //"pdf", "jpg", "mp3", ...

        //Set mime type. It is of the form : "media/format" for example : 'image/jpg', 'application/pdf'
        String mimeType = getMimeType(photoURI);

        //Get type or extension from mime type.
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(mimeType); //"pdf", "jpg", "mp3", ...

        message.mimeType = mimeType;
        //Default thumb
        Bitmap attachmentThumbBitmap = BitmapFactory.decodeResource(getActivity().getResources(),
                R.drawable.jpg_icon);

        //Get the thumb from camera photo
        Bitmap attachmentThumbBitmap_ = null;
        try {
            attachmentThumbBitmap_ = getThumbnail(photoURI);
        } catch (IOException e) {
            e.printStackTrace();
        }

        attachmentThumbBitmap = (attachmentThumbBitmap_== null) ? attachmentThumbBitmap : attachmentThumbBitmap_;


        //Bitmap bitmap1 = getBitmapFromUri(photoURI);
        //String uriPath  = getPath(photoURI);
        String uriPath    = currentPhotoPath;
        attachmentUriPath = uriPath;

        //Get the filename of the camera image from 'photoURI'
        //Assign the textView name the camera image name. The name is like 'xxx.jpg'
        String attachmentName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //N=24
            Cursor returnCursor =
                    getActivity().getContentResolver().query(photoURI, null, null, null, null);
            assert returnCursor != null;
            int nameIndex = returnCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();

            attachmentName = returnCursor.getString(nameIndex);         //extension is included
        }else{
            attachmentName = new File(photoURI.toString()).getName();   // extension included ?
        }

        //Remove the part (random number inserted in createTempFile). See 'createImageFile'.
        String[] parts = attachmentName.split("_");
        attachmentName = parts[0] + "_" + parts[1] + "_" + parts[2];    //the extension is not included

        //Check the length according to db table column 'extraname' table = 'messages'
        if(!attachmentName.equals(null)){   //012345.....xyz.html, entre le caractère 0 et z il y 45 caratères suivi de 5 caractères par exemple : '.html'
            if(attachmentName.length() > 45)attachmentName = attachmentName.substring(0, 45); //45 est exclu, le 0 est inclu
        }

        //Set back the removed extension
        attachmentName += "." + extension;

        //update the image view
        //if(attachmentThumbBitmap != null){
        //thumbnail.setImageBitmap(attachmentThumbBitmap);
        //Get string encoded base-64 from bitmap
        String attachmentThumbString = bitmapToString(attachmentThumbBitmap); //x64

        //add the thumb image to 'Message'
        message.setExtra(attachmentThumbString);    //string base-64

        message.setExtraName(attachmentName);       //extension is included

        //Add selected uri paths to attachment array list.
        attachmentsUriPaths.add(uriPath);

        //Add selected picture to attachment.
        //attachmentFiles.add(picturePath);   //---> Media.DATA
        //attachmentNames.add(pictureName);   //---> Media.DISPLAY_NAME

        //Show the layout containing the attachment.
        llAttachment.setVisibility(View.VISIBLE);

        //Display the flow layout containing the attachments.
        ChatBoxMessage.this.tableLayout.removeAllViews();

        attachments.add(attachmentName);
        attachmentThumbs.add(attachmentThumbBitmap);
        tableLayout.removeAllViews();

        //Display list of attachments to upload. The list is displayed below the 'Send' button.
        displayAttachments(attachments, attachmentThumbs);

        //The send is done in 'performClick()'

        // The 2 statements below are comments on 08-09-22
        //Send the attachment to server.
        //sendMessage.uploadAttachment(uriPath, message);//go to 'TabChatActivity.uploadAttachment'

        //Disable 'send' button while uploading the attachment. It will be enabled when the upload is complete.
        //send.setEnabled(false);
    }

    private String getDisplayname(Uri uri) {
        String displayName = null;
        if(ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())){
            Cursor cursor =
                    getActivity().getContentResolver().query(uri, null,
                            null, null, null);
            if(cursor != null){
                int name2 = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                displayName = cursor.getString(name2);
            }
        }else{
            displayName = uri.getLastPathSegment();
        }
        return displayName;
    }

    /**
     * Get mime type from uri. It is of the form : "media/format" for example : 'image/jpg', 'application/pdf'
     * @param uri The supplied uri
     * @return the mime type.
     */
    public String getMimeType(Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getActivity().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            //the extension 'jpeg', 'png', ...
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());

            //the mime type is like = 'image/jpeg'
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());

        }
        return mimeType;
    }

    //The system scans the SD card when it is mounted to find any new image (and other) files.
    // If you are programmatically adding a file, then you can use this class:
    //http://developer.android.com/reference/android/media/MediaScannerConnection.html

    //Add camera photo to gallery
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void galleryAddPicture(Context context) {
        /*********************************************************************************************************************************
         I had this issue on a completely different phone and version of Android, but both the issue and the explanation can be universal.
         In my case, it was the existence of the .nomedia file in the directory I was trying to see in the Gallery app.
         This is documented in the Android Developer documentation under Storage Options and, as of this writing appears in a sidebar
         as follows:
         Hiding your files from the Media Scanner
         Include an empty file named .nomedia in your external files directory (note the dot prefix in the filename).
         This prevents media scanner from reading your media files and providing them to other apps through the MediaStore content provider.
         In my case, some application that I installed must have put this file in my Download directory, hiding all of the images
         there from the Gallery app (and others as well).

         Solution: All I had to do was delete the .nomedia file from the directory and the contents became visible in the Gallery app. You can simply connect your phone to your computer and browse to the directory to accomplish this, or use a file manager on your phone configured to view hidden dot files.
         /*****************************************************************************************************************************
         //nothing showed in gallery
         Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
         File f = new File(currentPhotoPath);
         Uri contentUri = Uri.fromFile(f);
         mediaScanIntent.setData(contentUri);
         context.sendBroadcast(mediaScanIntent);


         //MediaScannerConnection.scanFile(this, new String[] { f.getPath() }, new String[] { "image/jpeg" }, null);
         */
          /*
        //nothing showed in gallery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File("file://"+ Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        } else {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }

        File f = new File(currentPhotoPath);
        MediaScannerConnection.scanFile(this,
                new String[] { f.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        */
        String[] parts = imageFileName.split("_");
        imageFileName = parts[0] + "_" + parts[1] + "_" + parts[2]; //pas d'extension

        //Mediastore method : working
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.ImageColumns.TITLE, imageFileName);
        contentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageFileName+".jpg");
        contentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Images.ImageColumns.DATA, currentPhotoPath);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.ImageColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        }

        ContentResolver resolver = this.getActivity().getContentResolver();
        Bitmap bitmap = null;
        Uri uri = null;
        try {
            // Requires permission WRITE_EXTERNAL_STORAGE
            bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            //dump the mediastore cursor content of this bitmap. The output will be in logcat.
            Cursor cursor = resolver.query(uri, null, null, null, null);
            DatabaseUtils.dumpCursor(cursor);
            cursor.close();
        } catch (Exception e) {
            e.getMessage();
            Snackbar.make(getView(), "The image is not added to gallery.", Snackbar.LENGTH_LONG).show();
            return;
        }

        OutputStream stream;
        try {
            stream = resolver.openOutputStream(uri);
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                throw new IOException("Error compressing the picture.");
            }
            stream.close();
        } catch (IOException e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            e.getMessage();
        }finally {
        }

        //Do next
        onActivityResultNext();
    }

    public static String getExternalSdCardPath() {
        String path = null;

        File sdCardFile = null;
        List<String> sdCardPossiblePath = Arrays.asList("external_sd", "ext_sd", "external", "extSdCard");

        for (String sdPath : sdCardPossiblePath) {
            File file = new File("/mnt/", sdPath);

            if (file.isDirectory() && file.canWrite()) {
                path = file.getAbsolutePath();

                String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
                File testWritable = new File(path, "test_" + timeStamp);

                if (testWritable.mkdirs()) {
                    testWritable.delete();
                }
                else {
                    path = null;
                }
            }
        }

        if (path != null) {
            sdCardFile = new File(path);
        }
        else {
            sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        }

        return sdCardFile.getAbsolutePath();
    }


    public  String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            }  finally {
            if (cursor != null)
                cursor.close();
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    //Display list of attachment to upload. The list is displayed below the 'Send' button.
    private void displayAttachments(final ArrayList<String> attachments, final ArrayList<Bitmap>attachmentThumbs) {
        //Build the attachment.
        //create dynamically a tablelayout and populate it with the textViews of the attachment.

        TableLayout.LayoutParams lparams = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, 0, 0.1f); //TableLayout.LayoutParams.WRAP_CONTENT);

        //tableLayout.getLayoutParams();
        tableLayout.removeAllViews();
        tableLayout.setLayoutParams(lparams);
        tableLayout.setBackgroundColor(Color.CYAN);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,  //WRAP_CONTENT ou MATCH_PARENT les deux cells collées
                TableRow.LayoutParams.WRAP_CONTENT);

        TextView textView       = new TextView(getActivity());
        TextView textViewDelete = new TextView(getActivity());
        ImageView imageView     = new ImageView(getActivity());
        textView.setText(null);
        textViewDelete.setText("");

        for (int i = 0; i < attachments.size(); i++) {

            TableRow tr = new TableRow(getActivity());
            //tr prend le params du parent qui est "TableLayout"
            lparams.setMargins(0, 0, 0, 0); //50
            tr.setLayoutParams(lparams);

            imageView.setLayoutParams(new TableRow.LayoutParams(0, 50 , 0.2f));
            imageView.setPadding(10, 0, 0, 0);
            imageView.setImageBitmap(attachmentThumbs.get(i));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                imageView.setBackground(getResources().getDrawable(R.color.colorYellow));
            }

            textView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f));
            textView.setPadding(10, 0, 0, 0);
            textView.setText(attachments.get(i));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                textView.setBackground(getResources().getDrawable(R.color.colorRedLight));
            }

            // delete icon
            textViewDelete.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.2f));
            textViewDelete.setPadding(10, 0, 10, 0);
            //textViewDelete.setText("XXXXXX");

            //Add trash icon to textView
            textViewDelete.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.trash36x36, 0, 0, 0);
            //textView.setCompoundDrawablePadding(this.getResources().getDimensionPixelOffset(R.dimen.small_padding));


            //Set Background. Alternate cyan and white colors.
            textViewDelete.setBackgroundColor(Color.WHITE);
            if((i % 2) == 0)textViewDelete.setBackgroundColor(Color.YELLOW);

            //Set Background. Alternate cyan and white colors.
            textView.setBackgroundColor(Color.WHITE);
            if((i % 2) == 0)textView.setBackgroundColor(Color.CYAN);


            //Clic on delete icon
            final int i_ = i; //i=position des attachments dans la liste
            textViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Set border on the row to delete. Ne sert pas, car il n'est pas vu car juste clic, il est effacé.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        tableLayout.getChildAt(i_).setBackground(getResources().getDrawable(R.drawable.border));
                    }

                    //Delete the attachment and update the table layout.
                    attachments.remove(i_);     //remove filename from the array list
                    attachmentThumbs.remove(i_); //remove icon from the array list.

                    //Rebuild the table layout
                    tableLayout.removeAllViews();
                    displayAttachments(attachments, attachmentThumbs);
                }
            });

            //params.weight = 1.0f;
            //params.setMargins(0, 0, 0, 50);
            //tr.addView(textView, params);
            tr.addView(imageView);
            tr.addView(textView);
            tr.addView(textViewDelete);

            tableLayout.addView(tr);
        }
    }


    public String bitmapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }


    public  Bitmap getThumbnail(Uri uri) throws FileNotFoundException, IOException {
        final int THUMBNAIL_SIZE = 50;
        InputStream input = getActivity().getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//
        input = getActivity().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    private Bitmap getBitmapFromUri(final Uri uri){
        if(uri==null)return null;
        ContentResolver contentResolver = getActivity().getContentResolver();
        if(contentResolver==null)return null;
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = contentResolver.openFileDescriptor(uri,"rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        if(fileDescriptor==null)return null;
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        try {
            parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    //Get path from uri
    private String getPath(final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if(isKitKat) {
            // MediaStore (and general)
            return getForApi19(uri);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @SuppressLint("NewApi")
    @TargetApi(19)
    private String getForApi19(Uri uri) {
        //Log.e(tag, "+++ API 19 URI :: " + uri);
        if (DocumentsContract.isDocumentUri(getActivity(), uri)) {
            //Log.e(tag, "+++ Document URI");
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                //Log.e(tag, "+++ External Document URI");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    //Log.e(tag, "+++ Primary External Document URI");
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }else{
                    // Handle non-primary volumes
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        //getExternalMediaDirs() added in API 21
                        File[] external = getActivity().getExternalMediaDirs();
                        if (external.length > 1) {
                            String filePath = external[1].getAbsolutePath();
                            return filePath.substring(0, filePath.indexOf("Android")) + split[1]; //like /storage/3334-6339/Download/filename
                        }
                    } else {
                        return "/storage/" + type + "/" + split[1];
                    }
                }
            }//end external storage
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                //Log.e(tag, "+++ Downloads External Document URI");
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                //Log.e(tag, "+++ Media Document URI");
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    //Log.e(tag, "+++ Image Media Document URI");
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    //Log.e(tag, "+++ Video Media Document URI");
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    //Log.e(tag, "+++ Audio Media Document URI");
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //Log.e(tag, "+++ No DOCUMENT URI :: CONTENT ");

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //Log.e(tag, "+++ No DOCUMENT URI :: FILE ");
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.Images.Media.DATA; //"MediaColumns.DATA"; ne marche pas // original "_data"; ne marche pas
        final String[] projection = {
                column
        };

        try {
            cursor = getActivity().getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private void performClick() {
        //Send the attachment to server only if the attachmentUriPath is not null.
        //when the upload of the attachment is successfully , we send the message content below in
        // 'performClickNext'
        System.out.println("performClick");

        if(attachmentUriPath!= null){
            System.out.println("performClick attachment");
            sendMessage.uploadAttachment(attachmentUriPath, message);//go to 'TabChatActivity.uploadAttachment'
        }else{
            //no attachments, send the message
            System.out.println("performClick no attachment");
            performClickNext_();
        }
    }

   //Send a message when no attachments or when the uploading of attachment is completed successful
    public void performClickNext_() {
        //Send message to server to save in pg database and dispatch it to the user. Next step is in
        //'TabChatActivity.sendMessage'
        ////sendMessage.sendMessage(fromNickname_, idFromNickname_, toNickname_, idToNickname_, edt.getText().toString());
        sendMessage.sendMessage(SelectedIdNickname, message, attachmentsUriPaths);
    }

    //the message and its attachment if any is uploaded  successfully , we comme here to update the
    // list of messages and display
    public void performClickNext() {
        //The body message is built in edt.
        newMessages.clear();//pour eviter l'accumulation. Dans 'displayReceivedMessage', il y a
        // newMessages.add(receivedMessage').
        //Soit, je supprime : newMessages.add(receivedMessage') dans 'displayReceivedMessage' et ici
        // dans 'performClick', je ne mets pas : 'newMessages.clear()'
        newMessages.add(message);
        allMessageSent.add(message);
        chatMessageList.add(message);   //'chatMessageList' is used in adapter

        //sort the messages by DateTime.
        Collections.sort(chatMessageList, new Comparator() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public int compare(Object o2, Object o1) {

                long x1 =  ((Message)o1).getTime();
                long x2 =  ((Message)o2).getTime();

                return  Long.compare(x2, x1);
            }
        });

        llAttachment.setVisibility(View.GONE);

        //update adapter
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //chatMessageList);

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        // notify the adapter to update the recycler view
        chatBoxAdapter.notifyDataSetChanged();

        //Scroll to the last position.
        messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);


        //Save the message content sent to user locally in sqlite db. The server saves this message when it is sent.
        //ArrayList<Uri> savedMessages = saveAllMessages();
        //'chatListUsers' means last session users
        //if ((savedMessages.size() != 1)) throw new UnsupportedOperationException("content message not saved locally");

        //Save only the message sent to 'SelectedNickname'
        ArrayList<Uri> savedMessage = saveMessages(SelectedNickname);
        if ((savedMessage.size() != 1)) throw new UnsupportedOperationException("content message not saved locally");

        //Clear the array list so that there is not accumulation of attachments.
        attachmentsUriPaths.clear();

        //clear the message editText
        edt.setText(null);

        //After clicking on button 'Send', the attachments are sent joigned to the message.
        // Now, clear attachment array list an rebuild the table layout.
        //The tablelayout is filled in 'onActivityResult' after clicking on 'attachment' button
        tableLayout.removeAllViews();
        attachments.clear();
        attachmentThumbs.clear();
        attachmentUriPath = null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            //System.out.println("************ onViewCreated *****************");
            //Restore the fragment's state here
            //String test = (String) savedInstanceState.get("test");
            //chatMessageList = (ArrayList<Message>) savedInstanceState.getSerializable("chatMessageList");
        }

        //update the views
        //fromNickname.setText(fromNickname_);
        //idFromNickname.setText(idFromNickname_);

        //toNickname.setText(toNickname_);
        //idToNickname.setText(idToNickname_);

        //Mix the two messages list
        //chatMessageList.addAll(chatMessageListFrom);

        //sort the final message list
        // sort DateTime typed list
        /*
        Collections.sort(chatMessageList, new Comparator() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public int compare(Object o2, Object o1) {

                long x1 =  ((Message)o1).getTime();
                long x2 =  ((Message)o2).getTime();

                return  Long.compare(x1, x2);
            }
        });
        */

        // add the new updated list of messages to the adapter.
        //chatBoxAdapter = new ChatBoxAdapter(chatMessageList);

        // notify the adapter to update the recycler view
        //chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        //messageRecycler.setAdapter(chatBoxAdapter);
    }

    private String getBase64StringFromDrawable(Context context, int drawableInt) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatar);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }
    
    public static String encodeTobase64(Bitmap image) {
        Bitmap immagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);
        return imageEncoded;
    }

    public static Bitmap decodeBase64(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    /**
     * Save the received message from 'selected user' to 'current user' who login. The save is done
     * in table 'chat_messages' in database 'chat.db'.
     * The database will be found in  'data/data/<package>/databases/chat.db
     * Return value is an uri of the added message.
     * @param receivedMessage the received message
     */
    //public ArrayList<Uri> saveReceivedMessage(String fromNickname) {
    public Uri saveReceivedMessage(Message receivedMessage) {
        //ArrayList<Uri> allRowsAdded = new ArrayList<Uri>();

        //Iterator<Message> iterator = newMessages.iterator(); //chatMessageList.iterator();
        //while(iterator.hasNext()){
            //Message message = iterator.next();
            //if(message.getToNickname().equals(nickname) || message.getFromNickname().equals(nickname)){
                ContentValues values = new ContentValues();
                values.put(MessagesContract.COLUMN_FROM, receivedMessage.fromNickname);
                values.put(MessagesContract.COLUMN_TO, receivedMessage.toNickname);
                values.put(MessagesContract.COLUMN_MESSAGE, receivedMessage.message);
                values.put(MessagesContract.COLUMN_REFERENCE, receivedMessage.ref);
                values.put(MessagesContract.COLUMN_DATE, receivedMessage.time);
                values.put(MessagesContract.COLUMN_EXTRA, receivedMessage.extra);
                values.put(MessagesContract.COLUMN_EXTRANAME, receivedMessage.extraName);
                values.put(MessagesContract.COLUMN_MIME, receivedMessage.mimeType);
                values.put(MessagesContract.COLUMN_SEEN, receivedMessage.seen);
                values.put(MessagesContract.COLUMN_DELETED_FROM, receivedMessage.deletedFrom);
                values.put(MessagesContract.COLUMN_DELETED_TO, receivedMessage.deletedTo);

                if(receivedMessage.extra == null){
                    values.putNull(MessagesContract.COLUMN_EXTRA);
                    values.putNull(MessagesContract.COLUMN_EXTRANAME);
                    values.putNull(MessagesContract.COLUMN_MIME);
                }
                //long newRowId = mDbHelper.insertFile(values);
                Uri newRowUri = getContext().getContentResolver().insert(MessagesContract.CONTENT_URI_MESSAGES, values);
                //allRowsAdded.add(newRowUri);
            //}
        //}
        //return allRowsAdded;
        return newRowUri;
    }



    /**
     * Save only the list of the new chat messages of the user 'nickname' in table 'chat_messages' in
     * sqlite database 'chat.db'.
     * the message is saved in the server in 'message_detection' event.
     * The old messages are already saved.
     * The database will be found in  'data/data/<package>/chat.db
     * Return value is an array list of uri of each message saved.
     */
    public ArrayList<Uri> saveMessages(String nickname) {
        ArrayList<Uri> allRowsAdded = new ArrayList<Uri>();

        Iterator<Message> iterator = newMessages.iterator(); //chatMessageList.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();
            if(message.getToNickname().equals(nickname) || message.getFromNickname().equals(nickname)){
                ContentValues values = new ContentValues();
                values.put(MessagesContract.COLUMN_FROM, message.fromNickname);
                values.put(MessagesContract.COLUMN_TO, message.toNickname);
                values.put(MessagesContract.COLUMN_MESSAGE, message.message);
                values.put(MessagesContract.COLUMN_REFERENCE, message.ref);
                values.put(MessagesContract.COLUMN_DATE, message.time);
                values.put(MessagesContract.COLUMN_EXTRA, message.extra);
                values.put(MessagesContract.COLUMN_EXTRANAME, message.extraName);
                values.put(MessagesContract.COLUMN_MIME, message.mimeType);
                values.put(MessagesContract.COLUMN_SEEN, message.seen);
                values.put(MessagesContract.COLUMN_DELETED_FROM, message.deletedFrom);
                values.put(MessagesContract.COLUMN_DELETED_TO, message.deletedTo);

                if(message.extra == null) {
                    values.putNull(MessagesContract.COLUMN_EXTRA);
                    values.putNull(MessagesContract.COLUMN_EXTRANAME);
                    values.putNull(MessagesContract.COLUMN_MIME);
                }

                //long newRowId = mDbHelper.insertFile(values);
                Uri newRowUri = mActivity.getContentResolver().insert(MessagesContract.CONTENT_URI_MESSAGES, values);
                allRowsAdded.add(newRowUri);
            }
        }

        //clear the array list 'newMessages' so there is not accumulation.
        //It is cleared here or in 'perform' where it is created.
        newMessages.clear();
        return allRowsAdded;
    }

    /**
     * Save the list of chat messages of all users in SQLITE database.
     * the SQLITE database will be found in  'data/data/<package>/database/chat.db
     * The SQLITE db is CLEARED when we CLEAR STORAGE of app after we do a long press on app and select 'info'.
     * return value is an array list of uris of all saved messages.
     */
    public ArrayList<Uri> saveAllMessages() {
        ArrayList<Uri> allRowsAdded = new ArrayList<>();
        ArrayList<Uri> rowsAdded    = new ArrayList<>();
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while(chatUserIterator.hasNext()){
            rowsAdded.clear();
            ChatUser chatUser = chatUserIterator.next();
            if(!chatUser.getNickname().equals(SelectedNickname))continue;
                ArrayList<Uri> arrayList = saveMessages(chatUser.getNickname());
                allRowsAdded.addAll(arrayList);
        }
        return allRowsAdded;
    }

    /**
     * Get messages sent from table and put them in ArrayList.
     * 'Nickname' send message to 'selectedNickname'
     * @return ArrayList of all messages in sqlite table.
     */
    private ArrayList <Message> getMessagesSent(String selectedNickname) {
        ArrayList<Message> messagesArrayList = new ArrayList();
        // Get all the messages from the database and put them in ArrayList.

        String[] mProjection = new String[]
            {
                MessagesContract.COLUMN_ID,
                MessagesContract.COLUMN_FROM,
                MessagesContract.COLUMN_TO,
                MessagesContract.COLUMN_MESSAGE,
                MessagesContract.COLUMN_REFERENCE,
                MessagesContract.COLUMN_DATE,
                MessagesContract.COLUMN_EXTRA,
                MessagesContract.COLUMN_EXTRANAME,
                MessagesContract.COLUMN_MIME,
                MessagesContract.COLUMN_SEEN,
                MessagesContract.COLUMN_DELETED_FROM,
                MessagesContract.COLUMN_DELETED_TO
            };

        //Method : ContentProvider
        Cursor cursor = getContext().getContentResolver().query(MessagesContract.CONTENT_URI_MESSAGES,
                mProjection,
                //FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=? OR "+FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=?" ,
                //new String[]{selectedNickname, Nickname, Nickname, selectedNickname},

                //FileContract.COLUMN_TO+"=? AND "+FileContract.COLUMN_FROM+"=?" ,
                MessagesContract.COLUMN_TO           +  " LIKE ? AND " +
                        MessagesContract.COLUMN_FROM         +  " LIKE ? AND " +
                        //MessagesContract.COLUMN_DELETED_TO   +  " != ? AND " +
                        MessagesContract.COLUMN_DELETED_FROM +  " != ? ",
                new String[]{selectedNickname, Nickname, "1" },

                MessagesContract.COLUMN_DATE + " DESC");

        //Method : queryBuilder
        //SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        //queryBuilder.setTables(FileContract.TABLE_NAME);
        //cursor = queryBuilder.query(db, mProjection, null,
        //        null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        long id             = 0;
        String from         = null;
        String to           = null;
        String message      = null;
        String reference    =null;
        long date           = 0;
        String extra        = null;
        String extraName    = null;
        String mimeType     = null;
        String seen         = null;
        String deletedFrom  = null;
        String deletedTo    = null;

        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            id          = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_ID));
            from        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_FROM));
            to          = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_TO));
            message     = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MESSAGE));
            reference   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_REFERENCE));
            date        = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DATE));
            extra       = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRA));
            extraName   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRANAME));
            mimeType    = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MIME));
            seen        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_SEEN));
            deletedFrom = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_FROM));
            deletedTo   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_TO));

            // Build the 'Messages' object.
            Message message_ = new Message(from, to, message, date, extra, extraName, reference,
                    mimeType, seen, deletedFrom, deletedTo);

            messagesArrayList.add(message_);
            i++;
            if(i >= limit)break;
        }
        cursor.close();
        return messagesArrayList;
    }


    /**
     * Get messages from table and put them in ArrayList.
     * 'Nickname' receive message from 'selectedNickname'
     * @return ArrayList of all messages in sqlite table.
     */
    private ArrayList <Message> getMessagesReceived(String SelectedNickname) {
        ArrayList<Message> messagesArrayList = new ArrayList();
        // Get all the messages received by 'SelectedNickname' from the database and put them in ArrayList.

        String[] projection = new String[]
            {
                MessagesContract.COLUMN_ID,
                MessagesContract.COLUMN_FROM,
                MessagesContract.COLUMN_TO,
                MessagesContract.COLUMN_MESSAGE,
                MessagesContract.COLUMN_REFERENCE,
                MessagesContract.COLUMN_DATE,
                MessagesContract.COLUMN_EXTRA,
                MessagesContract.COLUMN_EXTRANAME,
                MessagesContract.COLUMN_MIME,
                MessagesContract.COLUMN_SEEN,
                MessagesContract.COLUMN_DELETED_FROM,
                MessagesContract.COLUMN_DELETED_TO
           };

        //Method : ContentProvider
        Cursor cursor = getContext().getContentResolver().query(MessagesContract.CONTENT_URI_MESSAGES,
                projection,
                //FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=? OR "+FileContract.COLUMN_FROM+"=? AND "+FileContract.COLUMN_TO+"=?" ,
                //new String[]{selectedNickname, Nickname, Nickname, selectedNickname},

                //FileContract.COLUMN_TO+"=? AND "+FileContract.COLUMN_FROM+"=?" ,
                MessagesContract.COLUMN_TO + " LIKE ? AND " +
                        MessagesContract.COLUMN_FROM + " LIKE ? AND " +
                        MessagesContract.COLUMN_DELETED_TO + " != ? ",
                        //MessagesContract.COLUMN_DELETED_FROM + " != ? ",

                new String[]{Nickname, SelectedNickname, "1"},

                MessagesContract.COLUMN_DATE + " DESC");

        //Method : queryBuilder
        //SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        //queryBuilder.setTables(FileContract.TABLE_NAME);
        //cursor = queryBuilder.query(db, mProjection, null,
        //        null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }

        long id             = 0;
        String from         = null;
        String to           = null;
        String message      = null;
        String reference    =null;
        long date           = 0;
        String extra        = null;
        String extraName    = null;
        String mimeType     = null;
        String seen         = null;
        String deletedFrom  = null;
        String deletedTo    = null;

        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            id          = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_ID));
            from        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_FROM));
            to          = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_TO));
            message     = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MESSAGE));
            reference   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_REFERENCE));
            date        = cursor.getLong(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DATE));
            extra       = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRA));
            extraName   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_EXTRANAME));
            mimeType    = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_MIME));
            seen        = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_SEEN));
            deletedFrom = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_FROM));
            deletedTo   = cursor.getString(cursor.getColumnIndexOrThrow(MessagesContract.COLUMN_DELETED_TO));


            // Build the 'Messages' object.
            Message message_ = new Message(from, to, message, date, extra, extraName, reference,
                    mimeType, seen, deletedFrom, deletedTo);

            messagesArrayList.add(message_);
            i++;
            if(i >= limit)break;
        }
        cursor.close();
        return messagesArrayList;
    }


    //add a 'chatUser'user who joined, and last session 'ChatUser'users
    public void setChatListUsers(ChatUser chatUser) {
        //Get the status icon
        int[] statusArray = mActivity.getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable)statusImageView.getBackground();

        //test if reconnect
        boolean reconnect = false;
        Iterator<ChatUser> iterator = chatListUsers.iterator();//run over all users
        while(iterator.hasNext()){
            ChatUser chatUser_ = iterator.next();
            if(chatUser_.getNickname().equals(chatUser.getNickname())){//yhe 'chatUser_' exists
                reconnect = true;
                //update data of 'ChatUser' object.
                chatUser_.setChatId(chatUser.getChatId());
                chatUser_.setStatus(ChatUser.userConnect);
                chatUser_.setConnectedAt(chatUser.getConnectedAt());
                chatUser_.setDisconnectedAt(chatUser.getDisconnectedAt());
                SelectedIdNickname = chatUser.getChatId();
                this.idSelectedNickname.setText("id s : " + SelectedIdNickname);
                backgroundGradient.setColor(statusArray[ChatUser.userConnect]);

                //cas où est'SelectedNickname' est affiché, le mettre à jour.
                //if(SelectedNickname.equals(chatUser.getNickname()))SelectedIdNickname = chatUser.getId();
                //idSelectedNickname.setText(SelectedIdNickname);

                break;
            }
        }

        if(reconnect)edt.setEnabled(true);
        if(!reconnect)chatListUsers.add(chatUser); //add last session 'ChatUser'


        /*
        //add the user to 'chatListUsers' if it is not already added.
        Iterator<ChatUser> iterator = chatListUsers.iterator();//run over all users
        while(iterator.hasNext()){
            ChatUser chatUser_ = iterator.next();
            if(chatUser_.getNickname().equals(chatUser.getNickname())){
                //update data of 'ChatUser' object.
                chatUser_.setChatId(chatUser.getChatId());
                chatUser_.setStatus(ChatUser.userConnect);
                chatUser_.setConnectedAt(chatUser.getConnectedAt());
                chatUser_.setDisconnectedAt(chatUser.getDisconnectedAt());
                SelectedIdNickname = chatUser.getChatId();
                this.idSelectedNickname.setText("id s : "+SelectedIdNickname);
                backgroundGradient.setColor(statusArray[ChatUser.userConnect]);

                //cas où est'SelectedNickname' est affiché, le mettre à jour.
                //if(SelectedNickname.equals(chatUser.getNickname()))SelectedIdNickname = chatUser.getId();
                //idSelectedNickname.setText(SelectedIdNickname);

                break;
            }
        }
        */

        //update the display with the new status of each user.
        //Wen we meet exception "Only the original thread that created a view hierarchy can touch its views"
        //we do the following
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                //update adapter
                MessagesDataHolder.setData(chatMessageList);
                chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //chatMessageList);

                //set the adapter for the recycler view
                messageRecycler.setAdapter(chatBoxAdapter);

                // notify the adapter to update the recycler view
                chatBoxAdapter.notifyDataSetChanged();

                //Scroll to the last position.
                messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            mActivity =(Activity) context;
        }
        try {
            sendMessage         = (SendMessage) getActivity();
            downloadAttachment  = (DownloadAttachment) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Error in retrieving data. Please try again");
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        /*
        final int THUMBSIZE = 200;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.maison_20180508_153845_original);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            int bitmapSize = bitmap.getAllocationByteCount();
        }
        thumbImage = ThumbnailUtils.extractThumbnail(bitmap, THUMBSIZE, THUMBSIZE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            int thumbImageSize = thumbImage.getAllocationByteCount();
        }
        thumbnail.setImageBitmap(thumbImage);
         */
    }

    @Override
    public void onResume() {
        super.onResume();

            //System.out.println("************ onResume *****************");

        this.messageRecycler.getAdapter().getItemCount();

        // notify the adapter to update the recycler view
        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        this.messageRecycler.setAdapter(chatBoxAdapter);

        //Scroll to the last position.
        this.messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
        //sendMessage.chatBoxMessageFragmentAttached();
    }

    @Override
    public void onPause() {
        super.onPause();
       // System.out.println("*********** onPause  chatMessageList = " + chatMessageList.size());

    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("some_int", 10);

        outState.putString("Nickname", Nickname);
        outState.putString("SelectedNickname", SelectedNickname);
        outState.putParcelable("message", message);

        outState.putString("idNickname", (String) idNickname.getText());
        outState.putString("nickname_", (String) nickname_.getText());
        outState.putString("connectionNickname", (String) connectionNickname.getText());

        outState.putString("idSelectedNickname", (String) idSelectedNickname.getText());
        outState.putString("selectedNickname", (String) selectedNickname.getText());
        outState.putString("connectionSelectedNickname", (String) connectionSelectedNickname.getText());
        outState.putInt("layout", R.layout.chat_box_message);

        //camera
        if(photoURI != null)outState.putString("photoURI", photoURI.toString());
        if(imageFileName != null)outState.putString("imageFileName", imageFileName);
        if(currentPhotoPath != null)outState.putString("currentPhotoPath", currentPhotoPath);

        if (messageRecycler != null) {
            //Save recycler view
           // System.out.println("*********** onSaveInstanceState  messageRecycler = " + messageRecycler.getAdapter().getItemCount());
            //System.out.println("*********** onSaveInstanceState  chatMessageList = " + chatMessageList.size());

            Parcelable listState = messageRecycler.getLayoutManager().onSaveInstanceState();
            // putting recyclerview position
            outState.putParcelable("messageRecycler", listState);

            //List<Message> kk = chatMessageList.subList(chatMessageList.size() - 10, chatMessageList.size());
            //ArrayList<Message> jj = new ArrayList<Message>(kk);

            outState.putParcelableArrayList("chatMessageList", chatMessageList);
            outState.putParcelableArrayList("chatListUsers", chatListUsers);
        }
        super.onSaveInstanceState(outState);

        final int MAX_BUNDLE_SIZE = 512; //Ko
        long bundleSize = getBundleSize(outState);
        if (bundleSize > MAX_BUNDLE_SIZE * 1024) {
            outState.clear();
        }
    }

    private long getBundleSize(Bundle bundle) {
        long dataSize;
        Parcel obtain = Parcel.obtain();
        try {
            obtain.writeBundle(bundle);
            dataSize = obtain.dataSize();
        } finally {
            obtain.recycle();
        }
        return dataSize;
    }

    public void displayReceivedAttachment(String fromNickname, String toNickname, String attachmentImage) {
        //Find the message owned by 'nickname' and update the 'Message' Object.
        Iterator<Message> iterator = chatMessageList.iterator();
        while(iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getToNickname().equals(toNickname) || message.getFromNickname().equals(fromNickname)) {
                //Update this message
                message.extra = attachmentImage;    //It is thumb not attachment image
            }
        }
        //Save the attachment in db or system file

        // Update the adapter.
        //chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity(), chatMessageList); //messageListUser);

        // notify the adapter to update the recycler view
        //chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        //messageRecycler.setAdapter(chatBoxAdapter);

    }

    //We received that a user is standby.
    public void displayReceivedStandbyUser(String nickname) {

        /* Ajouter dans 'colors.xml' ou un fichier 'dummy.xml' les lignes suivantes :
        <array name="status">
            <item>#FFFF0000</item>
            <item>#FF00FF00</item>
            <item>#FF0000FF</item>
        </array>

        */
        int[] statusArray = getActivity().getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable)statusImageView.getBackground();
        //int status_ = ChatUser.userAbsent;
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while (chatUserIterator.hasNext()){
            ChatUser chatUser = chatUserIterator.next();
            if(chatUser.getNickname().equals(nickname)){
                chatUser.setStatus(ChatUser.userStandby);
                break;
            }
        }
        backgroundGradient.setColor(statusArray[ChatUser.userStandby]);

        // Update the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

        // notify the adapter to update the recycler view
        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        //Scroll to the last position.
        messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);

    }

    //We receive a come back from standby
    public void displayReceivedBackStandbyUser(String nickname) {
        //Change color status from orange to green
        int[] statusArray = getActivity().getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable) statusImageView.getBackground();
        //int status_ = ChatUser.userAbsent;
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while (chatUserIterator.hasNext()) {
            ChatUser chatUser = chatUserIterator.next();
            if (chatUser.getNickname().equals(nickname)) {
                chatUser.setStatus(ChatUser.userConnect);
                break;
            }
        }
        backgroundGradient.setColor(statusArray[ChatUser.userConnect]);

        // Update the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

        // notify the adapter to update the recycler view
        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        //Scroll to the last position.
        messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
    }

    //User is disconnect. He has pressed the 'Back' key.
    public void displayReceivedDiconnectUser(String nickname) {
        //Diasable.
        //edt.setEnabled(false);
        //send.setEnabled(false);
        attach.setEnabled(false);
        camera.setEnabled(false);

        //Change status from 'green' to 'red'
        int[] statusArray = getActivity().getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable)statusImageView.getBackground();
        //int status_ = ChatUser.userAbsent;
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while (chatUserIterator.hasNext()){
            ChatUser chatUser = chatUserIterator.next();
            if(chatUser.getNickname().equals(nickname)){
                chatUser.setStatus(ChatUser.userGone);
                break;
            }
        }

        backgroundGradient.setColor(statusArray[ChatUser.userGone]);

        // Update the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

        // notify the adapter to update the recycler view
        chatBoxAdapter.notifyDataSetChanged();

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        //Scroll to the last position.
        messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
    }

    public void displayReceivedData_(ChatUser selectedChatUser, ChatUser currentChatUser) {
        //update the textViews
        this.selectedNickname.setText("s : " + selectedChatUser.getNickname()); //s = selected
        this.idSelectedNickname.setText("id s : " + selectedChatUser.getChatId());
        this.connectionSelectedNickname.setText("Connected at : " + selectedChatUser.getConnectedAt());
        byte[] decodedString = Base64.decode(selectedChatUser.getImageProfile(), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        imageProfile.setImageBitmap(decodedByte);

        this.nickname_.setText("c : " + currentChatUser.getNickname()); // c = current
        this.idNickname.setText("id c :" + currentChatUser.getChatId());
        this.connectionNickname.setText("Connected at : " + currentChatUser.getConnectedAt());

        //update the global fields
        this.SelectedNickname = selectedChatUser.getNickname(); //selected nickname
        this.SelectedIdNickname = selectedChatUser.getChatId();       //selected id
        this.Nickname = currentChatUser.getNickname();  //current nickname
        this.IdNickname = currentChatUser.getChatId();        // current id

        //update the status image
        int[] status = getActivity().getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable) statusImageView.getBackground();
        int status_ = 0; //default status
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while (chatUserIterator.hasNext()) {
            ChatUser chatUser = chatUserIterator.next();
            if (chatUser.getNickname().equals(SelectedNickname)) {
                status_ = chatUser.getStatus();
                break;
            }
        }
        backgroundGradient.setColor(status[status_]);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //clear the adapter
        chatMessageList.clear();
        // add the new updated list of messages to the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment) getActivity()); //messageListUser);

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        // notify the adapter to update the recycler view.
        chatBoxAdapter.notifyDataSetChanged();
    }


    public static Bitmap getRoundedCornerImage(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242; //Color.RED; no effect to change color outside the
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);  //no effect to change (r,g, b) color. a=255, outside the circle become white.
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {

        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    //We come here from 'TabChatActivity.sendUserData' when a user is selected in left pane 'Connexion'
    //Display data when a user is selected in tab 'Connexion'.
    //Each time we come here, we get out messages from local sqlite db and call adapter to display them.

    public void displayReceivedData(ChatUser selectedChatUser, ChatUser currentChatUser) {
        //update the textViews
        this.selectedNickname.setText("s : " + selectedChatUser.getNickname()); //s = selected
        this.idSelectedNickname.setText("id s : " + selectedChatUser.getChatId());
        this.connectionSelectedNickname.setText("Connected at : " + selectedChatUser.getConnectedAt());
        
        String encodedString = selectedChatUser.getImageProfile();
        if(null == encodedString){
            encodedString = getBase64StringFromDrawable(getActivity(), R.drawable.avatar);
        }
        
        byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
        Bitmap decodedByte   = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        //resize the bitmap to have 200px width or height
        decodedByte = resizeBitmap(decodedByte, 200);

        //Display a rounded image
        int w = decodedByte.getWidth(), h = decodedByte.getHeight();
        int radius = w > h ? h : w; // set the smallest edge as radius.

        RoundedImageView  roundedImageView = new RoundedImageView(getActivity());
        Bitmap roundBitmap = roundedImageView.getCroppedBitmap(decodedByte, radius);
        imageProfile.setImageBitmap(roundBitmap);

        this.nickname_.setText("c : " + currentChatUser.getNickname()); // c = current
        this.idNickname.setText("id c :" + currentChatUser.getChatId());
        this.connectionNickname.setText("Connected at : " + currentChatUser.getConnectedAt());

        //update the global fields
        this.SelectedNickname   = selectedChatUser.getNickname(); //selected nickname
        this.SelectedIdNickname = selectedChatUser.getChatId();       //selected id
        this.Nickname           = currentChatUser.getNickname();  //current nickname
        this.IdNickname         = currentChatUser.getChatId();        // current id

        //update the status image
        int[] status = getActivity().getResources().getIntArray(R.array.status);
        GradientDrawable backgroundGradient = (GradientDrawable)statusImageView.getBackground();
        int status_ = 0 ; //default status
        Iterator<ChatUser> chatUserIterator = chatListUsers.iterator();
        while (chatUserIterator.hasNext()){
            ChatUser chatUser = chatUserIterator.next();
            if(chatUser.getNickname().equals(SelectedNickname)){
                status_ = chatUser.getStatus();
                break;
            }
        }
        backgroundGradient.setColor(status[status_]);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //clear the adapter
        chatMessageList.clear();
        // add the new updated list of messages to the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        // notify the adapter to update the recycler view.
        chatBoxAdapter.notifyDataSetChanged();
        ////////////////////////////////////////////////////////////////////////////////////////////

        //Get data from local SQLite database
        allMessagesReceived.clear();
        allMessageSent.clear();
        Iterator<ChatUser> iterator_ = chatListUsers.iterator();
        while(iterator_.hasNext()){
            ChatUser chatUser = iterator_.next();
            if(chatUser.getNickname().equals(SelectedNickname)){
                //if(chatUser.getFirstTimeAccessDatabase()){    //ne sert pas car limite l'acces à la db une seule fois.
                    //Get data from  sqlite database.
                    //chatUser.setFirstTimeAccessDatabase(false);  //not used
                    allMessagesReceived.addAll(getMessagesReceived(SelectedNickname));
                    allMessageSent.addAll(getMessagesSent(SelectedNickname));
                //}
                break;
            }
        }

        //We have done, we get all messages from SQLITE db then we will dispatch and extract the messages
        //for 'NickName' and 'SelectedNickname'

        chatMessageList.clear(); //double cf line 2574

        //Get the messages sent by the selected user to Nickname from 'allMessagesReceived' updated above.
        Iterator<Message> iterator = allMessagesReceived.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();
            if(message.getFromNickname().equals(SelectedNickname))chatMessageList.add(message);
        }
        //Get the messages sent by Nickname  to the selected user from 'allMessagesSent' updated above..
        iterator = allMessageSent.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();
            if(message.getToNickname().equals(SelectedNickname))chatMessageList.add(message);
        }

        //If no messages found in local SQLite table, get messages from server.
        if(chatMessageList.isEmpty()){
            //Notify the user
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    "Downloading messages from server",Snackbar.LENGTH_LONG).
                    show();

            //Get messages from server. They will be saved in 'getMessagesFromserverRes' method.
            getMessagesFromServer(SelectedNickname, Nickname, IdNickname);
            return;
        }

        //Set the received messages as 'seen'
        /*
        for(Message message : chatMessageList){
            //The following statement is slow
            //if(message.seen.equals(NOTSEEN)) message.seen = SEEN;
            if(message.fromNickname.equals(SelectedNickname) &
                    (message.toNickname.equals(Nickname))) message.seen = SEEN;
        }
        */

        //update the adapter of view pager
        //sectionsPagerAdapter.notifyDataSetChanged();
        //chatMessageList.removeAll(oldMessageListUser);
        //oldMessageListUser = chatMessageListFrom;
        //chatMessageList.addAll(chatMessageListFrom);

        //sort the 'chatMessageList' built above.
        Collections.sort(chatMessageList, new Comparator() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public int compare(Object o2, Object o1) {

                long x1 =  ((Message)o1).getTime();
                long x2 =  ((Message)o2).getTime();

                return  Long.compare(x2, x1);
            }
        });

        // add the new updated list of messages to the adapter.
       // System.out.println("*********** displayReceivedData chatMessageList = " + chatMessageList.size());
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

        //set the adapter for the recycler view
        messageRecycler.setAdapter(chatBoxAdapter);

        // notify the adapter to update the recycler view.
        chatBoxAdapter.notifyDataSetChanged();

        ////////////////////////////////////////////////////////////////////////////////////////////
        //RecyclerView.SmoothScroller smoothScroller = new
        //        LinearSmoothScroller(getActivity()) {
        //            @Override protected int getVerticalSnapPreference() {
        //                return LinearSmoothScroller.SNAP_TO_START;
        //            }
        //        };
        //smoothScroller.setTargetPosition(chatBoxAdapter.getItemCount() - 1);
        //messageRecycler.getLayoutManager().startSmoothScroll(smoothScroller);
        ////////////////////////////////////////////////////////////////////////////////////////////

        //Scroll to the last position.
       messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
        //messageRecycler.scrollToPosition(2);
        //messageRecycler.getLayoutManager().scrollToPosition(10);
        //messageRecycler.smoothScrollToPosition(10);
        //((LinearLayoutManager)messageRecycler.getLayoutManager()).scrollToPositionWithOffset(chatBoxAdapter.getItemCount() - 1,0);
        //messageRecycler.smoothScrollToPosition(chatBoxAdapter.getItemCount() - 1);
        //messageRecycler.smoothScrollToPosition(30);

    }//end displayReceivedData

    //Ask the server to send all messages sent by 'Nickname' to 'SelectedNickname'
    //See 'TabChatActivity.getMessagesFromServer'
    private void getMessagesFromServer(String SelectedNickname, String Nickname, String IdNickname) {
        sendMessage.getMessagesFromServer(SelectedNickname, Nickname, IdNickname);
    }

    //The server responds to the above query. Display the messages sent and save them.
    public void getMessagesFromServerRes(ArrayList<Message> messages) {

        if(messages.size() == 0){
            //hide progress bar launched in 'TabChatActivity.socket.emit('get_messages'
            customCircularProgress.setVisibility(View.GONE);
            return;
        }

        //fill the 'chatMessageList' with 'messages'
        chatMessageList.addAll(messages);
        allMessagesReceived.addAll(messages);
        allMessageSent.addAll(messages);

        /*
        //extract from 'messages' the received and the sent messages
        //Get the messages sent by 'fromNickname' to 'toNickname'
        Iterator<Message> iterator = messages.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();
            if(message.getFromNickname().equals(SelectedNickname))allMessageSent.add(message);
        }

        //Get the messages received by 'fromNicname' from'toNickname'
        iterator = messages.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();
            if(message.getToNickname().equals(SelectedNickname))allMessagesReceived.add(message);
        }
        */

        //Save the array list of messages in SQLite database if it is not empty
        ArrayList<Uri> arrayList = saveDownloadedMessages(messages);

        //Check
        if(arrayList.size() !=  messages.size()) return;

        //Todo : if(chatMessageList.size == saveDownloadedMessages(messages)) oK, all is right
        //Todo : else something is wrong in saving operation.

        //hide progress bar launched in 'TabChatActivity.socket.emit('get_messages'
        customCircularProgress.setVisibility(View.GONE);

        //sort the 'chatMessageList' built above.
        Collections.sort(chatMessageList, new Comparator() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public int compare(Object o2, Object o1) {

                long x1 =  ((Message)o1).getTime();
                long x2 =  ((Message)o2).getTime();

                return  Long.compare(x2, x1);
            }
        });

        //getActivity().runOnUiThread(new Runnable() {
            //public void run() {
                // add the new updated list of messages to the adapter.
               // System.out.println("**********getMessagesFromServerRes chatMessageList = " + chatMessageList.size() );
                MessagesDataHolder.setData(chatMessageList);
                chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment)getActivity()); //messageListUser);

                // notify the adapter to update the recycler view.
                chatBoxAdapter.notifyDataSetChanged();

                //set the adapter for the recycler view
                messageRecycler.setAdapter(chatBoxAdapter);

                // Scroll to the bottom of the adapter
                messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);

            //}
        //});
    }

    /**
     * Save the messages downloaded from server in SQLIte database
     * @param messages
     * @return array list of uri
     */
    private ArrayList<Uri> saveDownloadedMessages(ArrayList<Message> messages) {
        ArrayList<Uri> rowsAdded = new ArrayList<Uri>();

        Iterator<Message> iterator = messages.iterator();
        while(iterator.hasNext()){
            Message message = iterator.next();

            ContentValues values = new ContentValues();
            values.put(MessagesContract.COLUMN_FROM, message.fromNickname);
            values.put(MessagesContract.COLUMN_TO, message.toNickname);
            values.put(MessagesContract.COLUMN_MESSAGE, message.message);
            values.put(MessagesContract.COLUMN_REFERENCE, message.ref);
            values.put(MessagesContract.COLUMN_DATE, message.time);
            values.put(MessagesContract.COLUMN_EXTRA, message.extra);
            values.put(MessagesContract.COLUMN_EXTRANAME, message.extraName);
            values.put(MessagesContract.COLUMN_MIME, message.mimeType);
            values.put(MessagesContract.COLUMN_SEEN, message.seen);
            values.put(MessagesContract.COLUMN_DELETED_FROM, message.deletedFrom);
            values.put(MessagesContract.COLUMN_DELETED_TO, message.deletedTo);

            if(message.extra == null){
                values.putNull(MessagesContract.COLUMN_EXTRA);
                values.putNull(MessagesContract.COLUMN_EXTRANAME);
                values.putNull(MessagesContract.COLUMN_MIME);
            }

            //long newRowId = mDbHelper.insertFile(values);
            Uri newRowUri = getContext().getContentResolver().insert(MessagesContract.CONTENT_URI_MESSAGES, values);
            rowsAdded.add(newRowUri);
        }
        return rowsAdded;
    }

    //We comme here from 'TabChatActivity.socket.on("message", messageListener)' after receiving
    // a message from the server.
    // We come also here to display messages of the user selected in left pane 'Connexion'.
    //public void displayReceivedMessage(ArrayList<Message> allMessagesReceived){//tous les messages destinés à Nickname.
    public void displayReceivedMessage(Message receivedMessage) {//le dernier message destiné à Nickname.

        System.out.println("displayReceivedMessage : fromNickname = " + receivedMessage.fromNickname + "toNickname = " + receivedMessage.toNickname);

        //Save or update locally the received message
        Uri uri = saveReceivedMessage(receivedMessage);
        if(uri == null)throw new UnsupportedOperationException("unexpected value, insert or update message");

        //this.allMessagesReceived = allMessagesReceived;
        this.message = receivedMessage; //used in 'performNext()'
        this.newMessages.add(receivedMessage);
        this.allMessagesReceived.add(receivedMessage);
        this.allMessageSent.add(receivedMessage); //added 03-09-22

        //Get the messages sent from this user to Nickname. First thing to do, clear 'chatMessageList'.
        chatMessageList.clear();    //'chatMessageList' is used in adapter
        //chatMessageListFrom.clear();
        //oldMessageListUser.clear();

        //Get the messages sent to Nickname' by 'SelectedNickname'
         Iterator<Message> iterator = allMessagesReceived.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getFromNickname().equals(SelectedNickname)) chatMessageList.add(message);
        }

        //Get the messages sent by Nickname ---> from
        iterator = allMessageSent.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getToNickname().equals(SelectedNickname)) chatMessageList.add(message);
        }
        //update the adapter of view pager
        //sectionsPagerAdapter.notifyDataSetChanged();
        //chatMessageList.removeAll(oldMessageListUser);
        //oldMessageListUser = chatMessageListFrom;
        //chatMessageList.addAll(chatMessageListFrom);
        //chatMessageList.addAll(chatMessageSent);

        //sort
        Collections.sort(chatMessageList, new Comparator() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public int compare(Object o2, Object o1) {

                long x1 = ((Message) o1).getTime();
                long x2 = ((Message) o2).getTime();

                return Long.compare(x2, x1);
            }
        });

        // add the new updated list of messages to the adapter.
        MessagesDataHolder.setData(chatMessageList);
        chatBoxAdapter = new ChatBoxAdapter((ChatBoxAdapter.Attachment) getActivity()); //messageListUser);

        //set the adapter for the recycler view
        //Wen we meet exception "Only the original thread that created a view hierarchy can touch its views"
        //we do the following
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                messageRecycler.setAdapter(chatBoxAdapter);
                //Scroll to the last position.
                messageRecycler.scrollToPosition(chatBoxAdapter.getItemCount() - 1);
            }
        });

        // notify the adapter to update the recycler view
        if (!messageRecycler.isComputingLayout()) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                messageRecycler.post(new Runnable() {
                    @Override
                    public void run() {
                        chatBoxAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                chatBoxAdapter.notifyDataSetChanged();
            }
        }
    }

    public static String SHA1() {
        //test SHA1("123") will result in : '40bd001563085fc35165329ea1ff5c5ecbdbbeef'
        Random r = new Random();
        int a = Math.abs(r.nextInt());

        String timeString   = String.valueOf(new Date().getTime());
        String randomString = String.valueOf(a);
        String clearString  = timeString + randomString;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            return byteArrayToString(messageDigest.digest());
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static String byteArrayToString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return buffer.toString();
    }

    public  void  notifyUploadComplete(){
        uploadComplete = true;
        //btn.setEnabled(true);
        send.setEnabled(true);
    }
}