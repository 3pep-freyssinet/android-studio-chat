package com.androidcodeman.simpleimagegallery;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InfoAlertDialogFragment extends DialogFragment {
    public InfoAlertDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    public static InfoAlertDialogFragment newInstance(String title) {
        InfoAlertDialogFragment frag = new InfoAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String name  = getArguments().getString("name");
        String size  = getArguments().getString("size");
        String date  = getArguments().getString("date");
        String path  = getArguments().getString("path");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(title);

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getResources().getString(R.string.picture_name, name));
        stringBuffer.append("\n");
        int size_ = Integer.parseInt(size);
        final float mega = 1024 * 1024;
         float size__ = size_ / mega;

         //Display 'Mo' or 'Ko' as unity.
         int unity;
         if(size__ < 1.0){ 
            size__ = size__ * 1024;
            unity = R.string.picture_size_Ko;
             size = String.format("%.0f", size__);   //zero decimal
        }else{
            unity = R.string.picture_size_Mo;
             size = String.format("%.3f", size__);   //3 decimals
        }
        stringBuffer.append(getResources().getString(unity, size));

        stringBuffer.append("\n");
        stringBuffer.append(getResources().getString(R.string.picture_date, date));
        stringBuffer.append("\n");
        stringBuffer.append(getResources().getString(R.string.picture_path, path));

        alertDialogBuilder.setMessage(stringBuffer);

        //alertDialogBuilder.setCancelable(false);//to prevent click outside the dialog causing dismiss.

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.close),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // on success
                dialog.dismiss();
            }
        });

        /*
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if (dialog != null && dialog.isShowing()) {
                    if (dialog != null) {
                    dialog.dismiss();
                }
            }

        });
        */
        return alertDialogBuilder.create();
    }
}