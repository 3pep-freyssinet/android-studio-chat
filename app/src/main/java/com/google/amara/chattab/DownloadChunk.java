package com.google.amara.chattab;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.example.aymen.androidchat.ChatBoxMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DownloadChunk  extends DialogFragment implements DownloadProgressState {

    private Bitmap bitmap;
    public DownloadChunk() {
    }
    public static DownloadChunk newInstance(byte[] byteArrayBitmap) {
        DownloadChunk frag = new DownloadChunk();
        Bundle args = new Bundle();
        args.putByteArray("byteArrayBitmap", byteArrayBitmap);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.popup, container, false);
        ImageView imageView1 = (ImageView) view.findViewById(R.id.imagePopup);
        TextView tvShare     = (TextView) view.findViewById(R.id.tv_share);
        TextView tvDownload  = (TextView) view.findViewById(R.id.tv_download);
        TextView tvQuit      = (TextView) view.findViewById(R.id.tv_quit);

        //Event : Quit
        tvQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvQuit Clicked
                //Toast. makeText(TabChatActivity.this, "Quit clicked", Toast. LENGTH_LONG).show();
                DownloadChunk.this.dismiss();
            }
        });

        //Event : Share
        tvShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvShare Clicked
                Toast. makeText(getActivity(), "Share clicked", Toast. LENGTH_LONG).show();
                Uri uri = saveImage(bitmap);
                shareImageUri(uri);

                //shareBitmap(bitmap);
            }
        });

        //Event : Download
        tvDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tvDownload Clicked
                Toast. makeText(getActivity(), "Download clicked", Toast. LENGTH_LONG).show();
                //Uri uri = saveFileToExternalStorage(bitmap);
            }
        });

        //Get the screen size
        //DisplayMetrics dm = context.getResources().getDisplayMetrics();
        //int widthPixels = dm.widthPixels;

        //Get the dimensions of the screen
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        int screenWidth   = dm.widthPixels;
        int screenHeight  = dm.heightPixels;

        byte[] byteArrayBitmap = getArguments().getByteArray("byteArrayBitmap");
        bitmap = BitmapFactory.decodeByteArray(byteArrayBitmap, 0, byteArrayBitmap.length);

        //Create a bitmap with dimensions a physical screen size.
        Bitmap bitmapResized = Bitmap.createScaledBitmap(bitmap, screenWidth, screenWidth, true);

        imageView1.setImageBitmap(bitmap);
        getDialog().setTitle("title");

        // Show soft keyboard automatically and request focus to field
        //mEditText.requestFocus();
        //getDialog().getWindow().setSoftInputMode(
        //        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //Show the dialog
        getDialog().show();;

        return view;

    }

    /**
     * Saves the image as JPG to the app's cache directory. It is located at :/data/data/<package>/images
     * @param image Bitmap to save.
     * @return Uri of the saved file or null
     */
    private Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(getActivity().getCacheDir(), "images");
        //File imagesFolder = new File(getFilesDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(getActivity(), "com.google.amara.chattab.myFileprovider", file);

        } catch (IOException e) {
            //Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage());
            e.getMessage();
        }
        return uri;
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


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            //sendMessage         = (ChatBoxMessage.SendMessage) getActivity();
            //downloadAttachment  = (ChatBoxMessage.DownloadAttachment) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Error in retrieving data. Please try again");
        }
    }

    @Override
    public void progressState(int state) {

    }
}
