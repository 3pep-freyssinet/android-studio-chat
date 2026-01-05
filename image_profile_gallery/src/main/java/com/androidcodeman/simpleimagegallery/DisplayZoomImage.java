package com.androidcodeman.simpleimagegallery;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.androidcodeman.simpleimagegallery.utils.CustomTextView;
import com.androidcodeman.simpleimagegallery.utils.pictureFacer;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class DisplayZoomImage extends AppCompatActivity {

    private final int INTENT_ZOOM_REQUEST_CODE = 100;

    private CustomTextView tvBack;
    public ImageView imageView, ivInfo, ivShare, ivCopy, ivDelete;
    private pictureFacer pictureFacer;
    private int total;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_picture);
        tvBack      = findViewById(R.id.tv_back);
        imageView   = findViewById(R.id.imageView);
        ivInfo      = findViewById(R.id.iv_info);
        ivShare     = findViewById(R.id.iv_share);
        ivCopy      = findViewById(R.id.iv_copy);
        ivDelete    = findViewById(R.id.iv_delete);

        // get the extras from intent
        total        = getIntent().getExtras().getInt("total");
        position     = getIntent().getExtras().getInt("position");
        pictureFacer = (pictureFacer)getIntent().getExtras().get("image");
    }

    @Override
    public void onStart() {
        super.onStart();

        //Get 'id' image not uri.
        String uri_     = pictureFacer.getImageUri();

        /*
        String id       = Uri.parse(pictureFacer.getImageUri()).getLastPathSegment();
        //get the uri of image and then get the bitmap to display
        Long id_        = Long.parseLong(id);
        Uri uri         = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id_);
        */

        Bitmap bitmap   = uriToBitmap(Uri.parse(uri_));
        if(bitmap == null){
            Drawable drawable = getResources().getDrawable(R.drawable.picture);
            imageView.setImageDrawable(drawable);
        }else{
            imageView.setImageBitmap(bitmap);
        }

        //not used.
        //ImageDisplay imageDisplay = ImageDisplay.instance;

        //show the info in top bar
        tvBack.setText(getResources().getString(R.string.info, position + 1, total));

        //tvBack event
        tvBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (performBack(v, event)) return true;
                return false;
            }
        });

        //ivInfo event
        ivInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long time = Long.parseLong(pictureFacer.getPictureDate());

                //SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy_HH-mm-ss");
                SimpleDateFormat formatter =  new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss", Locale.FRANCE);
                String dateString = formatter.format(new Date(time));

                performInfo(pictureFacer.getPicturName(),
                        pictureFacer.getPictureSize(),
                        dateString, //pictureFacer.getPictureDate(),
                        pictureFacer.getPicturePath());
            }
        });

        //ivShare event
        ivShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performShare(Uri.parse(uri_));
            }
        });

        //ivCopy event
        ivCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCopy();
            }
        });

        //ivDelete event
        ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performDelete(Uri.parse(uri_));
            }
        });
    }//end onStart

    //Get the image bitmap from mediastore and display it.
    private void performDelete(Uri uri) {
        ArrayList<Uri> arrayList = new ArrayList<>();
        arrayList.add(uri);

        deletePictureFromGallery(arrayList);    //contains a callback

    }

    private void performCopy() {
    }

    private void performShare(Uri uri) {
        shareImageUri(uri);
    }

    private void performInfo(String name, String size, String date, String path) {
        FragmentManager fmx = getSupportFragmentManager();
        android.app.FragmentManager fm = getFragmentManager();

        //InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance("Some Title");
        //infoDialogFragment.show(fm, "fragment_info_dialog_fragment");

        InfoAlertDialogFragment alertDialog = InfoAlertDialogFragment.newInstance("Some title");
        Bundle args = new Bundle();
        args.putString("title", "Details");
        args.putString("name", name);
        args.putString("size", size);
        args.putString("date", date);
        args.putString("path", path);
        alertDialog.setArguments(args);
        alertDialog.show(fmx, "fragment_alert");

        alertDialog.setCancelable(false);   //to prevent click outside the dialog causing dismiss.
    }

    private boolean performBack(View v, MotionEvent event) {
        final int DRAWABLE_LEFT     = 0;
        final int DRAWABLE_TOP      = 1;
        final int DRAWABLE_RIGHT    = 2;
        final int DRAWABLE_BOTTOM   = 3;
        v.performClick();
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            float a = event.getRawX();      //position of the clic.
            float b = tvBack.getRight();    //position of the right border of the view (tvInfo = textview+drawable)
            // the left border is placed at 0.

            //Case : The drawable is placed at right of the textView
            //int c = tvInfo.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();   //width of the drawable
            //The drawable is placed at right of the textView, then b-c = position of left border of the drawable
            // if x < b-c, the clic is on text; if x >= b-c the clic is on drawable.

            //case : The drawable is placed left of textview
            // int c = tvInfo.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width();
            //if x <= c, the clic is on drawable, if x > c the clic is on textview

            //here, the drawable is placed left of drawable
            int c = tvBack.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width();
            if(a <= c ) {
                // click on drawable placed at left of textview.
                onFinish("back");
                return true;
            }
        }
        return false;

         /*
        tvInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int[] textLocation = new int[2];
                    tvInfo.getLocationOnScreen(textLocation);

                    if (event.getRawX() <=  textLocation[0] + tvInfo.getTotalPaddingLeft()) {

                        // Left drawable was tapped

                        return true;
                    }


                    if (event.getRawX() >= textLocation[0] + tvInfo.getWidth() - tvInfo.getTotalPaddingRight()){

                        // Right drawable was tapped

                        return true;
                    }
                }
                return true;
            }
        });
        */
    }

    @Override
    public void onBackPressed(){ //the device 'back'
        onFinish("back");
    }

    //We come here after clicking the (textView+Drawable) icon on top left
    // or
    //After deleting one image.
    public void onFinish(String mode){
        //Send back the result of intent to 'ImageDisplay.onActivityResult'
        Intent intent = new Intent();
        //File pathDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //Uri resultUri = Uri.fromFile(pathDir); // the thing to return
        //intent.setData(resultUri);
        //intent.setData(uri);
        //intent.putStringArrayListExtra("uri_selected_pictures", selectedItems_);
        intent.putExtra("mode", mode);  //mode may be 'delete' or 'back'
        intent.putExtra("position", position);  //position picture deleted
        setResult(Activity.RESULT_OK, intent);

        super.finish();
    }

    /**
     * This Method gets the image in mediastore passing its id
     *
     * @param id String corresponding to the id of the image
     */
    public pictureFacer getImageById(String id){

        Uri allImagesuri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Images.ImageColumns._ID ,
                MediaStore.Images.ImageColumns.DATA ,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.DATE_ADDED
        };

        Cursor cursor = getContentResolver().query(allImagesuri,
                projection,
                MediaStore.Images.Media._ID + " like ? ",
                new String[] {"%"+id+"%"},
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        pictureFacer pic = new pictureFacer();

        try {
            pic.setPicturName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)));

            pic.setPicturePath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));

            pic.setPictureSize(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)));

            pic.setImageUri(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)));

            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return pic;
    }

    /**
     * Return bitmap from uri
     * @param uri uri
     * @return
     */
    private Bitmap uriToBitmap(Uri uri) {
        Bitmap bitmap = null;
        bitmap = CompressBitmapToBitmap(new File(getPath(uri)));

        bitmap = (bitmap != null) ? bitmap : null;
        return bitmap;
        /*
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {

            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            assert parcelFileDescriptor != null;
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            //works but throw 'OutOfMemory' Exception in case of huge bitmap (15 Mo).
            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            bitmap = null;
        }catch(OutOfMemoryError | IOException e){
            e.printStackTrace();
            bitmap = null;
        }finally{
            if(parcelFileDescriptor != null){
                parcelFileDescriptor.detachFd();
                try {
                    parcelFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    bitmap = null;
                }
            }
        }
        return bitmap;
        */
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private Bitmap decodeFile(String pathName) {
        // Decode image size
        BitmapFactory.Options opts = new BitmapFactory.Options();
        /*
         * If set to true, the decoder will return null (no bitmap), but the
         * out... fields will still be set, allowing the caller to query the
         * bitmap without having to allocate the memory for its pixels.
         */
        opts.inJustDecodeBounds = true;
        opts.inDither = false; // Disable Dithering mode
        opts.inPurgeable = true; // Tell to gc that whether it needs free
        // memory, the Bitmap can be cleared
        opts.inInputShareable = true; // Which kind of reference will be used to
        // recover the Bitmap data after being
        // clear, when it will be used in the
        // future

        //BitmapFactory.decodeFile(pathName, opts);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 75;

        // Find the correct scale value.
        int scale = 1;  //default scale value.

        if (opts.outHeight > REQUIRED_SIZE || opts.outWidth > REQUIRED_SIZE) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) opts.outHeight
                    / (float) REQUIRED_SIZE);
            final int widthRatio = Math.round((float) opts.outWidth
                    / (float) REQUIRED_SIZE);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            scale = heightRatio < widthRatio ? heightRatio : widthRatio;//
        }

        // Decode bitmap with inSampleSize set
        opts.inJustDecodeBounds = false;

        opts.inSampleSize = scale;

        Bitmap bitmap = BitmapFactory.decodeFile(pathName, opts).copy(
                Bitmap.Config.RGB_565, false);

        return bitmap;

    }

    /**
     * Get file in input, decompress it in a temporary file in cach and build a bitmap.
     * @param file input file
     * @return a decompressed bitmap.
     */

    public Bitmap CompressBitmapToBitmap(File file){
        try {
            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;

            // factor of downsizing the image
            FileInputStream inputStream = new FileInputStream(file);

            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;

            // Find the correct scale value. It should be the power of 2.

            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            //Get bitmap from file
            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            //Create an new image file and then return it.
            //file.createNewFile();
            //File file_ = new File("aa.jpg");

            //String filePath = getFilesDir().getPath(); //data/user/0/[package]/files
            String filePath = getCacheDir().getPath(); //data/user/0/[package]/cache

            String filename = (file.getName().length() > 50) ? file.getName().substring(0, 50) : file.getName();
            File imageFile  = new File(filePath, filename);

            FileOutputStream outputStream = new FileOutputStream(imageFile);

            assert selectedBitmap != null;
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);

            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            return bitmap;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shares the JPG image from Uri.
     * @param uri Uri of image to share.
     */
    private void shareImageUri(Uri uri){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        Intent chooser = Intent.createChooser(intent, "Chooser Title");
        chooser.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivity(chooser);
        //startActivity(intent);
    }

    /**
     * Delete a selection of pictures.
     * @param uris : A list of uris of the pictures.
     */
    private void deletePictureFromGallery(List<Uri> uris) {

        //Creating ArrayList
        List<String> pathList = new ArrayList<String>();
        Iterator<Uri> iterator = uris.iterator();
        while(iterator.hasNext()){
            Uri uri = iterator.next();
            String path = getPath(uri);
            //String path = uri.getPath();
            pathList.add(path);
        }

        //Converting the pathList to an array
        String[] paths = pathList.toArray(new String[0]);
        final int[] numberRowsDeleted = {0};    //cannot assign a value to ...
        try {
            MediaScannerConnection.scanFile(this, paths, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        /**
                         *
                         * @param path the path to the file that has been scanned.
                         * @param uri_  the Uri for the file if the scanning operation succeeded
                         *  and the file was added to the media database, or null if scanning failed.
                         */
                        public void onScanCompleted(String path, Uri uri_) {

                            if (uri_ != null) {
                                numberRowsDeleted[0] = getContentResolver().delete(uri_, null,
                                        null);
                            }

                            //Todo. If we arrive here, something is wrong, uri == null is not expected. Notify the user.
                            //ToDo notify user. if numberRowsDeleted = 0, no rows deleted, else the number
                            // of rows deleted = numberRowsDeleted.
                            String[] deletePictureMessage = {"Failed to delete picture from gallery",
                                    "Picture succesfully deleted from gallery"};

                            String message = (numberRowsDeleted[0] == 0) ? deletePictureMessage[0] : deletePictureMessage[1];

                            Snackbar.make(findViewById(android.R.id.content),
                                    message,
                                    Snackbar.LENGTH_LONG).show();

                            if(numberRowsDeleted[0] != 0) onFinish("delete"); //we could also  use an interface
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            numberRowsDeleted[0] = 0;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public String getPath(final Uri uri) {
        // check here to KITKAT or new version
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String selection = null;
        String[] selectionArgs = null;
        // DocumentProvider
        if (isKitKat) {
            // ExternalStorageProvider

            if (isExternalStorageDocument(uri)) {
                String docId = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    docId = DocumentsContract.getDocumentId(uri);
                }
                final String[] split = docId.split(":");
                final String type = split[0];

                String fullPath = getPathFromExtSD(split);
                if (fullPath != "") {
                    return fullPath;
                } else {
                    return null;
                }
            }

            // DownloadsProvider
            if (isDownloadsDocument(uri)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));


                                return getDataColumn(this, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }
                } else {
                    String id = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        id = DocumentsContract.getDocumentId(uri);
                    }

                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    Uri contentUri = null;
                    try {
                        contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {

                        return getDataColumn(this, contentUri, null, null);
                    }
                }
            }

            // MediaProvider
            if (isMediaDocument(uri)) {
                String docId = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    docId = DocumentsContract.getDocumentId(uri);
                }
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
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};

                return getDataColumn(this, contentUri, selection, selectionArgs);
            }

            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri);
            }

            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri);
            }

            //'content' 'scheme'
            if ("content".equalsIgnoreCase(uri.getScheme())) {

                if (isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(uri);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                    // return getFilePathFromURI(context,uri);
                    return copyFileToInternalStorage(uri, "userfiles");
                    // return getRealPathFromURI(context,uri);
                } else {
                    return getDataColumn(this, uri, null, null);
                }

            }
            //'file' 'scheme'
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } else { //Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT = 19

            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri);
            }

            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {
                        MediaStore.Images.Media.DATA
                };
                Cursor cursor = null;
                try {
                    cursor = getContentResolver()
                            .query(uri, projection, selection, selectionArgs, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);

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

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public boolean isWhatsAppFile(Uri uri) {
        return "com.whatsapp.provider.media".equals(uri.getAuthority());
    }

    private boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    private boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }

    private String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        return fullPath;
    }

    private String getDriveFilePath(Uri uri) {
        Uri returnUri = uri;
        Cursor returnCursor = this.getContentResolver().query(returnUri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(this.getCacheDir(), name);
        try {
            InputStream inputStream = this.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return file.getPath();
    }

    private String getFilePathForWhatsApp(Uri uri) {
        return copyFileToInternalStorage(uri, "whatsapp");
    }

    /***
     * Used for Android Q+
     * @param uri
     * @param newDirName if you want to create a directory, you can set this variable
     * @return
     */
    private String copyFileToInternalStorage(Uri uri, String newDirName) {
        Uri returnUri = uri;

        Cursor returnCursor = this.getContentResolver().query(returnUri, new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        }, null, null, null);


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        File output;
        if (!newDirName.equals("")) {
            File dir = new File(this.getFilesDir() + "/" + newDirName);
            if (!dir.exists()) {
                dir.mkdir();
            }
            output = new File(this.getFilesDir() + "/" + newDirName + "/" + name);
        } else {
            output = new File(this.getFilesDir() + "/" + name);
        }
        try {
            InputStream inputStream = this.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {

            Log.e("Exception", e.getMessage());
        }

        return output.getPath();
    }
}
