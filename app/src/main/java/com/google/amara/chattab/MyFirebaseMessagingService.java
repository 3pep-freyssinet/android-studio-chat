package com.google.amara.chattab;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG         = "FCMService";
    private static final String CHANNEL_ID  = "FCM_Channel";

    private interface sendFCMTokenToBackendCallback {
        void onSuccess();
        void onNotSuccess(String raison);
        void onFailure(String raison);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        try {
            /*
            senderName: senderName,
            message: preview,
            senderId: String(fromUserId),
            profileImageUrl: profileImageUr
             */

        // Log the message for debugging
        Log.d(TAG, "From: " + remoteMessage.getData().get("senderName"));
            String title      = "";
            String body       = "";
            String senderName = "";
            String senderId   = "";
            String icon       = "null";
            String messageId  = "";

        // Check if the message contains notification payload
        if (remoteMessage.getData().get("notification") != null) {
            //title = remoteMessage.getNotification().getTitle();
            //body  = remoteMessage.getNotification().getBody();
        }
        if (!remoteMessage.getData().isEmpty()) {
            // Extract the data payload
            //title       = remoteMessage.getData().get("title").equals(null)? "TITLE" : remoteMessage.getData().get("title");
            body        = remoteMessage.getData().get("message");
            senderName  = remoteMessage.getData().get("senderName");
            title       = "New message from "+ senderName;
            icon        = remoteMessage.getData().get("profileImageUrl");
            senderId    = remoteMessage.getData().get("senderId");
            messageId   = remoteMessage.getData().get("messageId");

            // Display the notification

            assert body != null;
            if (!body.isEmpty()) {
                sendNotification(title, senderId, senderName, messageId, body);

                Bitmap bitmap = BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.icon_24_24_transparent   // 👈 your test avatar
                );

                icon = "icon_24_24_transparent";
                //showNotification(title, body, icon); // Show the notification)
            }
        }

        // Handle data payload if any
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }
            // Call backend API (for example, to fetch additional data)
            //callBackendApi();

        } catch (Exception e) {
            Log.e(TAG, "Error in FirebaseMessagingService", e);
            sendErrorBroadcast("Failed to process notification. Please check your connection.");
        }
    }

    private void sendErrorBroadcast(String errorMessage) {
        Intent intent = new Intent("BACKEND_ERROR");
        intent.putExtra("error_message", errorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendNotification(String title, String senderId, String senderName,
                                  String messageId, String messageBody) {
        // 0. create intent
        Intent intent = new Intent(this, TabChatActivity.class);
        intent.putExtra("senderId", senderId);
        intent.putExtra("senderName", senderName);
        intent.putExtra("messageId", messageId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 1. Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        Bitmap bitmap = BitmapFactory.decodeResource(
                getResources(),
                MainApplication.senderAvatarId
        );

        bitmap = getCircularBitmap(bitmap);

        // 2. Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setSmallIcon(android.R.drawable.stat_notify_chat) //works
                .setSmallIcon(R.drawable.ic_notification_test)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setContentIntent(pendingIntent)
                .setLargeIcon(bitmap)
                .setColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // 3. Check permission and show
        if (hasNotificationPermission()) {
            showNotificationNow(notificationBuilder);
        } else {
            handleMissingPermission(title, messageBody);
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        Rect rect = new Rect(0, 0, width, height);
        RectF rectF = new RectF(rect);

        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private void handleMissingPermission(String title, String message) {
        Log.w(TAG, "Cannot show notification - permission missing");

        // Option 1: Store notification to show later when permission granted
        storePendingNotification(title, message);

        // Option 2: Send broadcast to request permission from Activity
        sendPermissionRequestBroadcast();
    }

    private void sendPermissionRequestBroadcast() {
        Intent intent = new Intent("NOTIFICATION_PERMISSION_NEEDED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void storePendingNotification(String title, String message) {
        // Implement using SharedPreferences or Room database
    }
    
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true; // Permission not required before Android 13
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showNotificationNow(NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(this)
                    .notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission revoked", e);
            // Optionally retry after requesting permission
        }
    }


    private int generateNotificationId() {
        return (int) System.currentTimeMillis(); // Unique ID for each notification
    }

    private boolean canShowNotification() {
        // Always allow on Android 12 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        // Check permission for Android 13+
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FCM Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Firebase Cloud Messaging Channel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


    // Method to display notification
    private void sendNotification_(String title, String messageBody) {
        // Create notification channel (Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "FCM Channel";
            String description = "Firebase Cloud Messaging Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent to open the app when the notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Create the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) //ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary)) // Set the color programmatically
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        //notificationManager.notify(0, notificationBuilder.build());
    }

    // Method to show notification
    private void showNotification(String title, String body, String icon) {
        // Create notification with NotificationCompat.Builder
        Bitmap bitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.circle1_xxl  // 👈 built-in Android icon
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "chat_channel")
                .setSmallIcon(android.R.drawable.stat_notify_chat)  // Icon from drawable resources
                .setContentTitle(title)
                .setContentText(body)
                //.setLargeIcon(bitmap)
                //.setColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary)) // Set the color programmatically
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    // Helper method to resolve icon name from drawable folder
    private int getIconResourceId(String iconName) {
        return getResources().getIdentifier(iconName, "drawable", getPackageName());
    }

    @Override
    public void onNewToken(String fcmToken) {
        Log.d(TAG, "Generating fcm token: " + fcmToken);

        // Send token to your server or save it locally
        //For example, if a user logs into the app, you might retrieve their userId after login and
        //String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        sendFCMTokenToBackend(fcmToken, new sendFCMTokenToBackendCallback(){
            @Override
            public void onSuccess() {
                //store the fcm token in 'SharedPreferences'
                SharedPreferences sharedPreferences = MainApplication.getSharedPreferences_();
                SharedPreferences.Editor editor     = sharedPreferences.edit();
                editor.putString("FCM_TOKEN", fcmToken);
                editor.apply();
            }

            @Override
            public void onNotSuccess(String reason) {
                showAlertDialog(getApplicationContext(), reason);
            }

            @Override
            public void onFailure(String reason) {
                showAlertDialog(getApplicationContext(), reason);
            }
        });
        //sharedPreferences = getSharedPreferences("myAppPrefs", MODE_PRIVATE);
        //SharedPreferences        sharedPreferences;

    }


    private void showAlertDialog(Context context, String message) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(activity)
                            .setTitle("Internal error")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                activity.finishAffinity(); // Close the app
                            })
                            .setCancelable(false)
                            .show();
                }
            });
        }
    }

    private void sendFCMTokenToBackend(String fcmToken, sendFCMTokenToBackendCallback callback) {
        String jwtToken = MainApplication.JWT_TOKEN;

        if (jwtToken == null) {
            Log.e("Auth", "No JWT token found");
            callback.onFailure("No JWT token found");
            return;
        }

        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // OkHttpClient instance
        //OkHttpClient client = new OkHttpClient();

        //OkHttpClient client = new OkHttpClient();
        OkHttpClient client = ((MainApplication) getApplicationContext()).getHttpClient();

        // Prepare the URL of your backend API (change this to your backend URL)
        String url = "https://android-notification.onrender.com/fcm/store-fcm-token";
        //String url = "http://localhost:5000/store-fcm-token";

        // Build the JSON object to send (containing the FCM token)

        JSONObject jsonObject = new JSONObject();
        try {
            //jsonObject.put("androidId", androidId);
            jsonObject.put("fcmToken", fcmToken);
            // Optionally, you can add more information, like userId, deviceId, etc.
            // jsonObject.put("userId", "your_user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Convert JSON object to String
        String jsonString = jsonObject.toString();

        // Create the request body with the JSON payload
        RequestBody body = RequestBody.create(jsonString, JSON);

        // Build the request
        Request request = new Request.Builder()
                .url(url)  // Backend URL
                .post(body)  // Send a POST request with the JSON body
                .addHeader("Authorization", "Bearer " + jwtToken)  // Optionally add an auth token
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                Log.e("FCM", "Failed to send token to backend");
                callback.onFailure("Failed to send token to backend");
                sendErrorBroadcast("Failed to connect to server. Please check your connection.");
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("FCM", "Unexpected code: " + response);
                    callback.onNotSuccess("Unexpected code:" + response);
                } else {
                    Log.d("FCM", "Token sent to backend successfully");
                    callback.onSuccess();
                }
            }
        });
    }
}

