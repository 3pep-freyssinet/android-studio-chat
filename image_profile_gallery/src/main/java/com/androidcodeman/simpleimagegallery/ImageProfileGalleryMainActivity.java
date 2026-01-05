package com.androidcodeman.simpleimagegallery;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.androidcodeman.simpleimagegallery.utils.MarginDecoration;
import com.androidcodeman.simpleimagegallery.utils.PicHolder;
import com.androidcodeman.simpleimagegallery.utils.imageFolder;
import com.androidcodeman.simpleimagegallery.utils.itemClickListener;
import com.androidcodeman.simpleimagegallery.utils.pictureFacer;
import com.androidcodeman.simpleimagegallery.utils.pictureFolderAdapter;
import com.androidcodeman.simpleimagegallery.utils.picture_Adapter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * The main Activity starts and loads all folders containing images in a RecyclerView
 * these folders are gotten from the MediaStore by the Method getPicturePaths()
 */
public class ImageProfileGalleryMainActivity extends AppCompatActivity
                                             implements itemClickListener {

    private static final int DISPLAY_IMAGE_REQUEST = 100;
    RecyclerView folderRecycler;
    TextView     empty;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    /**
     * Request the user for permission to access media files and read images on the device
     * this will be useful as from api 21 and above, if this check is not done the Activity will crash
     *
     * Setting up the RecyclerView and getting all folders that contain pictures from the device
     * the getPicturePaths() returns an ArrayList of imageFolder objects that is then used to
     * create a RecyclerView Adapter that is set to the RecyclerView
     *
     * @param savedInstanceState saving the activity state
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_gallery);

        ////////////////////////////////////////////////////////////////////////////////////////////
        //*** disable all the activities in the 'image_profile_gallery' manifest  and add them in the 'app' manifest  **
        ////////////////////////////////////////////////////////////////////////////////////////////

        //get extra from intent
        ArrayList<String> uris = getIntent().getExtras().getStringArrayList("imageProfileHistoryUri");
        int flags              = getIntent().getFlags();
        //Set permissions
        if(ContextCompat.checkSelfPermission(ImageProfileGalleryMainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(ImageProfileGalleryMainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        //__________________________________________________________________________________________
        /*
        Intent intent = new Intent();
        File pathDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Uri resultUri = Uri.fromFile(pathDir); // the thing to return
        intent.setData(resultUri);
        setResult(Activity.RESULT_OK, intent);
        finish();
        */
        //------------------------------------------------------------------------------------------
        empty = findViewById(R.id.empty);

        folderRecycler = findViewById(R.id.folderRecycler);
        folderRecycler.addItemDecoration(new MarginDecoration(this));
        folderRecycler.hasFixedSize();

        //get the list of folders in the device external storage. The folders contain images.
        //ArrayList<imageFolder> folds   = getPicturePaths(uris);
        ArrayList<pictureFacer> images = getImages(uris);

        Intent displayPicture = new Intent(ImageProfileGalleryMainActivity.this, ImageDisplay.class);
        displayPicture.putParcelableArrayListExtra("uri", images);
        getIntent().addFlags(flags);

        //move.putExtra("recyclerItemSize", getCardsOptimalWidth(4));
        //startActivity(move);
        startActivityForResult(displayPicture, DISPLAY_IMAGE_REQUEST);

        /*
        if(folds.isEmpty()){
            empty.setVisibility(View.VISIBLE);
        }else{
            RecyclerView.Adapter folderAdapter = new pictureFolderAdapter(folds, ImageProfileGalleryMainActivity.this,this);
            folderRecycler.setAdapter(folderAdapter);

        }
        changeStatusBarColor();
        */
    }

    /**
     * @return
     * gets all folders with pictures on the device and loads each of them in a custom object imageFolder
     * then returns an ArrayList of these custom objects
     */
    private ArrayList<imageFolder> getPicturePaths(ArrayList<String> uris){

        //get information
        //ne marche pas avec image provenant de download
        //FileInformation.getPath(this, Uri.parse(uris.get(0)));

        ArrayList<imageFolder> picFolders = new ArrayList<>();
        ArrayList<String>      picPaths   = new ArrayList<>();     //contains path of the pictures

        ///////////////////////////////////////
        //String[] ID_COLUMN = { BaseColumns._ID };  //marche

        String[] ID_COLUMN = {
                BaseColumns._ID,
                MediaStore.Images.ImageColumns._ID ,
                MediaStore.Images.ImageColumns.DATA ,
                MediaStore.Images.Media.DISPLAY_NAME,
                //MediaStore.Images.Media.DOCUMENT_ID, //exception : no such column
                //MediaStore.Images.Media.ORIGINAL_DOCUMENT_ID //exception : no such column
        };


        //API 29
        //MediaStore.Downloads.INTERNAL_CONTENT_URI;

        //ContentValues contentValues = new ContentValues();
        //contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, localPath);
        //contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        //contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
        //Uri uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);


        MediaStore.Files.getContentUri("external");

        //ne marche pas
        //getFileFromMediastore();

        Cursor cursor_ = MediaStore.Images.Media.query(getContentResolver(),
                //android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, //marche as the next statement
                //MediaStore.Images.Media.getContentUri("external"),    //marche as the previous statement
                Uri.parse(uris.get(0)),

                ID_COLUMN,
                null,
                null,
                null
        );

        cursor_.moveToFirst();

        String idd   = cursor_.getString(0);
        String[] ids = new String[1];
        ids[0]       = idd;

        /*
        //extract id from uris
        String[] ids = new String[uris.size()];
        int j = 0;
        for(String uri_ : uris){
            Uri uri = Uri.parse(uri_);
            //the uri is like : 'content://com.android.externalstorage.documents/document/3334-6339%3ADCIM%2FCamera%2F20170815_085513.jpg'
            //sometimes it is like ; content://....document/image:24
            // content://com.android.providers.media.documents/document/image%3A1630094132059  //device
            // content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fimage-fleurs.jpg  //emulator
            long rowId = Long.valueOf(uri.getLastPathSegment());
            long id__  = ContentUris.parseId(uri);
            ids[j]     = String.valueOf(rowId);
            j++;
        }
        */
        //query the mediastore
        Uri allImagesuri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        //String[] projection_ = { MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        //String[] uris_ = (String[]) uris.toArray(new String[uris.size()]);

        //set the '?' placeholders
        StringBuilder sb = new StringBuilder(ids.length * 2 - 1);
        sb.append("?");
        for (int i = 1; i < ids.length; i++) {
            sb.append(",?");
        }
        String param = sb.toString();

        String[] projection = {
                MediaStore.Images.ImageColumns._ID ,
                MediaStore.Images.ImageColumns.DATA ,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.BUCKET_ID
        };
        //String selection = MediaStore.Images.Media._ID + " IN ( " + param + " ) "; //" like? ";
        String selection = MediaStore.Images.ImageColumns._ID + " IN ( " + param + " ) "; //" like? ";
        String[] selectionArgs = ids;

        //String[] selectionArgs = {"%Pictures%"}; //{ "Camera"}; //"Camera"};
        String order = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC";

        Cursor cursor = this.getContentResolver().query(
                allImagesuri,
                projection,
                selection,
                selectionArgs,
                order
        );

        if(cursor == null)return null;
        try {
            if (cursor != null) {
                //cursor.moveToFirst();
                cursor.moveToPosition(-1);
            }
            while(cursor.moveToNext()){
                imageFolder folds = new imageFolder();
                String id         = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                long id_          = Long.parseLong(id);
                String name       = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String folder     = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
                String datapath   = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                //String folderpaths =  datapath.replace(name,"");
                String folderpaths = datapath.substring(0, datapath.lastIndexOf(folder+"/"));
                folderpaths = folderpaths + folder + "/";
                if (!picPaths.contains(folderpaths)) {

                    //System.out.println("folderpaths = "+datapath+" bucketName = "+folder);

                    picPaths.add(folderpaths);

                    //not used
                    //Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);

                    folds.setPath(folderpaths);
                    folds.setFolderName(folder);
                    folds.setFirstPic(datapath);//if the folder has only one picture this line helps to set it as first so as to avoid blank image in itemview
                    folds.addpics();            //incremente le nombre de 'imageFolder
                    picFolders.add(folds);
                }else{
                    for(int i = 0; i < picFolders.size(); i++){
                        if(picFolders.get(i).getPath().equals(folderpaths)){
                            //Instruction originale. 'setFirstPic(datapath)' met en première image
                            // la dernière du cursor. Comme les images sont classées par ordre chronologique
                            //( la plus récente en premier, la plus ancienne en dernier).
                            //Avec cette instruction la plus ancienne image se retrouve en premier.
                            //On pourrait garder cette instruction et faire la requette avec :
                            // order = MediaStore.Images.ImageColumns.DATE_TAKEN + " ASC";
                            //picFolders.get(i).setFirstPic(datapath);
                            picFolders.get(i).addpics();    //incremente le nombre de 'imageFolder'
                        }
                    }
                }
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(int i = 0; i < picFolders.size(); i++){
            Log.d("picture folders",picFolders.get(i).getFolderName()+" and path = "+picFolders.get(i).getPath()+" "+picFolders.get(i).getNumberOfPics());
            //System.out.println(picFolders.get(i).getFolderName()+" and path = "+picFolders.get(i).getPath()+" "+picFolders.get(i).getNumberOfPics());
        }

        //reverse order ArrayList
        ArrayList<imageFolder> reverseFolders = new ArrayList<>();

        for(int i = picFolders.size() - 1; i > reverseFolders.size() - 1; i--){
            reverseFolders.add(picFolders.get(i));
        }

        return picFolders;
    }

    //ne marche pas
    private void getFileFromMediastore() {
            try {
                String[] projection = {
                        BaseColumns._ID
                        //MediaStore.Files.FileColumns.TITLE,
                        //MediaStore.Files.FileColumns._ID,
                        //MediaStore.Files.FileColumns.PARENT,
                        //MediaStore.Files.FileColumns.DATA
                };// Can include more data for more details and check it.

                String selection = MediaStore.Files.FileColumns.MEDIA_TYPE+"=?";

                String[] selectionArgs = {"MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE"};

                String sortOrder = MediaStore.Images.Media._ID + " ASC";

                Cursor cursor = getContentResolver().query(
                        MediaStore.Files.getContentUri("\"external\""),
                        projection,
                        null, //selection,
                        null,   //selectionArgs,
                        null //sortOrder
                );

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            int filetitle  = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE);
                            int file_id    = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                            int fileparent = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT);
                            int filedata   = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

                            //Mediafileinfo info = new Mediafileinfo();
                            //info.setData(new File(new File(audioCursor.getString(filedata)).getParent()).getName());
                            //info.setTitle(audioCursor.getString(filetitle));
                            //info.set_id(audioCursor.getString(file_id));
                            //info.setParent(audioCursor.getString(fileparent));
                            // info.setData(audioCursor.getString(filedata));
                            //audioList.add(info);
                        } while (cursor.moveToNext());
                    }
                }
                assert cursor != null;
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }


    public ArrayList<pictureFacer> getImages(ArrayList<String> uris) {
        ArrayList<pictureFacer> images = new ArrayList<>();

        for (String uri : uris) {
            pictureFacer pic = new pictureFacer();
            pic.setImageUri(uri);
            //String path  = getPath(this, Uri.parse(uri));
            String path_ = getPathFromUriForApi19(this, Uri.parse(uri));
            pic.setPicturePath(path_);
            images.add(pic);
        }
        return images;
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
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs,
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

    //@SuppressLint("NewApi")
    //@TargetApi(19)
    private String getPathFromUriForApi19(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
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
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
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

    /**
     * This Method gets all the images in the folder paths passed as a String to the method and returns
     * and ArrayList of pictureFacer a custom object that holds data of a given image.
     *
     * @param uris
     */
    public ArrayList<pictureFacer> getImages_(ArrayList<String> uris) {

        ////////////////////////////////////////////////////////////////////////////////////////////
        //extract id from uris
        String[] ids = new String[uris.size()];
        int j = 0;
        for(String uri_ : uris){
            Uri uri = Uri.parse(uri_);
            long rowId = Long.valueOf(uri.getLastPathSegment());
            ids[j] = String.valueOf(rowId);
            j++;
        }

        //set the '?' placeholders
        StringBuilder sb = new StringBuilder(ids.length * 2 - 1);
        sb.append("?");
        for (int i = 1; i < ids.length; i++) {
            sb.append(",? ");
        }
        String param = sb.toString();

        ArrayList<pictureFacer> images = new ArrayList<>();

        Uri allImagesuri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN, //'DATE_ADDED' is not correct replaced by 'DATE_TAKEN'
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.ImageColumns._ID,
        };

        Cursor cursor = getContentResolver().query(
                allImagesuri,
                projection,
                MediaStore.Images.Media._ID + " IN ( " + param + " ) ", //" like? ";
                ids,

                MediaStore.Images.ImageColumns._ID + " DESC"   //DATE_TAKEN
        );

        try {
            cursor.moveToFirst();

            //Build the 'pictureFacer' object
            do {
                pictureFacer pic = new pictureFacer();

                //String date = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                String date = cursor.getString(2);
                pic.setPictureDate(date);

                //String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)));
                String name = cursor.getString(1);
                pic.setPicturName(name);

                //String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                String data = cursor.getString(0);
                //test
                //data = "/storage/3334-6339/DCIM/Camera/IMG_20221119_073916.jpg"; //ok
                //data = "/document/2200"; //les images sont blanches
                pic.setPicturePath(data);

                //String size = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                String size = cursor.getString(3);
                pic.setPictureSize(size);

                //String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String id = cursor.getString(4);
                long id_ = Long.parseLong(id);
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
                pic.setImageUri(uri.toString());

                images.add(pic);

            } while (cursor.moveToNext());
            cursor.close();

            /*
            //Reverse order
            ArrayList<pictureFacer> reSelection = new ArrayList<>();
            for(int i = images.size() - 1; i > -1; i--){
                reSelection.add(images.get(i));
            }
            images = reSelection;
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
        return images;
    }


    @Override
    public void onPicClicked(PicHolder holder, int position, ArrayList<pictureFacer> pics) {
        Toast.makeText(this, "onPicClicked", Toast.LENGTH_LONG);
    }

    /**
     * Each time an item in the 'pictureFolderAdapter' RecyclerView is clicked this method from the
     * implementation of the transitListerner in this activity is executed, this is possible because
     * this class is passed as a parameter in the creation of the RecyclerView's Adapter, see the
     * adapter class to understand better what is happening here.
     * @param pictureFolderPath a String corresponding to a folder path on the device external storage
     */
    @Override
    public void onPicClicked(String pictureFolderPath, String folderName) {
        //A folder is clicked
        Intent displayPicture = new Intent(ImageProfileGalleryMainActivity.this, ImageDisplay.class);
        displayPicture.putExtra("folderPath", pictureFolderPath);
        displayPicture.putExtra("folderName", folderName);

        //move.putExtra("recyclerItemSize", getCardsOptimalWidth(4));
        //startActivity(move);
        startActivityForResult(displayPicture, DISPLAY_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Here, we come back from 'ImageDisplay'. Send back the selected image to the caller 'MainActivity' in 'app' module
        if (resultCode == Activity.RESULT_OK) {
            //Send back the result we just received to caller (The application witch query the gallery)

            assert data != null;
            //'uriSelectedPictures' contient des uri sous forme de string
            ArrayList<String> uriSelectedPictures = data.getStringArrayListExtra("uri_selected_pictures");

            //return the data to 'MainActivity.onActivityResult' in 'app'.
            String mimeType = "image/*";
            String[] mimeTypeArray = new String[] { mimeType };

                final Intent intent = new Intent();
                //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (uriSelectedPictures.size() == 1) {

                    Uri uri = Uri.parse(uriSelectedPictures.get(0));
                    String path = getPathFromUriForApi19(this, uri);
                    //intent.setData(uri);
                    intent.putExtra("path", path);
                    //test
                    //Bitmap bitmap = null;
                    //try {
                    //    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(uriSelectedPictures.get(0)));
                    //} catch (IOException e) {
                    //    e.printStackTrace();
                    //}

                } else if (uriSelectedPictures.size() > 1) {
                    final ClipData clipData = new ClipData(
                            null, mimeTypeArray, new ClipData.Item(Uri.parse(uriSelectedPictures.get(0))));
                    for (int i = 1; i < uriSelectedPictures.size(); i++) {
                        clipData.addItem(new ClipData.Item(Uri.parse(uriSelectedPictures.get(i))));
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        intent.setClipData(clipData);
                    }else{
                        intent.setData(Uri.parse(uriSelectedPictures.get(0)));
                    }
                }
                setResult(Activity.RESULT_OK, intent);
                finish();
        }
    }

    /**
     * Default status bar height 24dp,with code API level 24
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void changeStatusBarColor() {
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        //Not working on Samsung 4.0.2
        //window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(),R.color.black));
    }

    public void onPause() {
        super.onPause();
    }

    public void onStop() {
        super.onStop();
    }
}
