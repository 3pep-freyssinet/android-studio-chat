package com.google.amara.chattab;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Executors;


public class ImageViewerActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "image_url";
    private String imageUrl;
    private PhotoView photoView;
    ImageView btnDownload;
    ImageView btnShare;
    ImageView btnOpenGallery;
    ImageView btnProfile;

    public static void start(Context context, String url) {
        Intent i = new Intent(context, ImageViewerActivity.class);
        i.putExtra(EXTRA_URL, url);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imageUrl       = getIntent().getStringExtra(EXTRA_URL);

        photoView      = findViewById(R.id.photoView);
        btnDownload    = findViewById(R.id.btnDownload);
        btnShare       = findViewById(R.id.btnShare);
        btnOpenGallery = findViewById(R.id.open_gallery);
        btnProfile     = findViewById(R.id.profile);

        //permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1001);
                return;
            }
        }



        Glide.with(this)
                .load(imageUrl)
                .into(photoView);

        btnDownload.setOnClickListener(v -> {
            downloadImage(imageUrl);
        });
        btnShare.setOnClickListener(v -> shareImage(imageUrl));
        btnOpenGallery.setOnClickListener(v -> openInGallery(imageUrl));
        btnProfile.setOnClickListener(v -> setAsProfilePicture());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadImage(imageUrl); // retry
        }
    }


    private void shareImage(String url_) {
        //simple
        /*
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(intent, "Share Image"));
         */

        new Thread(() -> {
            try {
                final URL url = new URL(imageUrl);
                InputStream input = url.openStream();

                File file = new File(getCacheDir(), "shared_image.jpg");
                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.close();
                input.close();

                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );

                runOnUiThread(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Image"));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void downloadImage(String imageUrl) {
        btnDownload.setEnabled(false);
        btnShare.setEnabled(false);
        btnOpenGallery.setEnabled(false);

        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                String fileName = "chat_image_" + System.currentTimeMillis() + ".jpg";

                OutputStream output;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // ✅ Android 10+
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    output = getContentResolver().openOutputStream(uri);

                } else {
                    // ✅ Android 7, 8, 9
                    File downloads = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);

                    if (!downloads.exists()) downloads.mkdirs();

                    File file = new File(downloads, fileName);
                    output = new FileOutputStream(file);
                }

                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.close();
                input.close();

                runOnUiThread(() ->
                        Toast.makeText(this, "Image saved to Downloads", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void openInGallery(String imageUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                InputStream input = url.openStream();

                File file = new File(getCacheDir(), "view_image.jpg");
                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.close();
                input.close();

                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );

                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setAsProfilePicture() {
        Toast.makeText(this, "Profile picture update coming soon 👤", Toast.LENGTH_SHORT).show();
    }


}