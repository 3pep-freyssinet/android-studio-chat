package com.google.amara.chattab;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CheckPermissions {

    public static boolean hasPermission(int PERMISSION_REQUEST, String permission, Context context) {
        if (ContextCompat.checkSelfPermission(context,permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission) &&
                    ContextCompat.checkSelfPermission(context,permission)
                            != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{permission},
                        PERMISSION_REQUEST);
            }
            return false;
        } else {
            return true;
        }
    }
}
