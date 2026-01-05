package com.google.amara.chattab.upload;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class FileUploadManager implements Serializable {
    private static final String TAG = "FileUploadManager";

    private long                mFileSize;
    private File                mFile;
    private BufferedInputStream stream;
    private String              mData;
    private int                 mBytesRead;
    private int                 i = 1;
    private Context             context;

    //Constructor
    public FileUploadManager(Context context){
        this.context   = context;
    }
    public boolean prepare(File file) {
        mFile     = file;
        mFileSize = mFile.length();
        ;
        try {
            stream = new BufferedInputStream(new FileInputStream(mFile));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "prepare error : ", e);
            return false;
        }

        return true;
    }

    public File CompressBitmapFile(File file){
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

          //Create a new image file and then return it.
          //file.createNewFile();
          //File file_ = new File("aa.jpg");
         String filePath = context.getFilesDir().getPath(); //data/user/0/[package]/files
         File imageFile  = new File(filePath, file.getName());

          FileOutputStream outputStream = new FileOutputStream(imageFile);

          selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);

          return imageFile;
      } catch (Exception e) {
        return null;
      }
    }

    public String getFileName() {
        return mFile.getName();
    }

    public long getFileSize() {
        return mFileSize;
    }

    public long getBytesRead() {
        return mBytesRead;
    }

    public String getData() {
        return mData;
    }

    public void read(int byteOffset) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int fileSize = (int)mFileSize;

        //int block    = fileSize / 100; //il y aura 100 parties complète + reste de la division qui va constituer une partie incomplète = 101 parties
        int block    = (fileSize % 100 == 0) ? fileSize / 100 : fileSize / 99;

        //int bufferSize = ( fileSize - byteOffset) < 4096  ? fileSize - byteOffset : 4096;
        //int bufferSize = ( fileSize - byteOffset) < 524288 * 2  ? fileSize - byteOffset : 524288 * 2; //qui marche au 21-05-22
        int bufferSize = ( fileSize - byteOffset) < block  ? fileSize - byteOffset : block;

        //int bufferSize = ( fileSize - byteOffset) < 4  ? fileSize - byteOffset : 4;
        byte[] buffer  = new byte[bufferSize];


        mBytesRead = stream.read(buffer, 0, buffer.length);

        byteBuffer.write(buffer, 0, mBytesRead);

        Log.v(TAG, "Read :" + mBytesRead);

        mData = Base64.encodeToString(byteBuffer.toByteArray(), Base64.DEFAULT);
    }

    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            stream = null;
        }
    }
}
