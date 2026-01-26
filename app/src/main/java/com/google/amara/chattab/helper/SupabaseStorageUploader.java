package com.google.amara.chattab.helper;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseStorageUploader {

    private static final String SUPABASE_URL     = "https://deamkajuoskfskmrgidb.supabase.co";
    private static final String SUPABASE_BUCKET  = "chat-images";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRlYW1rYWp1b3NrZnNrbXJnaWRiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkwNzg2OTcsImV4cCI6MjA4NDY1NDY5N30.d1AXxSYjgTOhUU_l8BdiFL-0b8uRcyMt8G_aIRViNEg";

    private static final OkHttpClient client = new OkHttpClient();

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(Exception e);
    }

    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        new Thread(() -> {
            try {
                // 1️⃣ Read file bytes from Uri
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                byte[] imageBytes       = readBytes(inputStream);

                // 2️⃣ Create unique file name
                String fileName = "img_" + System.currentTimeMillis() + ".jpg";

                // 3️⃣ Upload request
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + fileName)
                        .addHeader("apikey", SUPABASE_API_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                        .put(RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Upload failed: " + response.body().string());
                }

                // 4️⃣ Build public URL
                String publicUrl = SUPABASE_URL +
                        "/storage/v1/object/public/" +
                        SUPABASE_BUCKET + "/" + fileName;

                callback.onSuccess(publicUrl);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
