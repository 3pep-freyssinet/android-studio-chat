package com.google.amara.chattab;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MyHttpClient {

    private OkHttpClient client;

    public MyHttpClient(Context context) throws Exception {
        // Load the CA certificate
        KeyStore keyStore = KeyStore.getInstance("BKS");
        InputStream inputStream = context.getResources().openRawResource(R.raw.server);
        keyStore.load(inputStream, "azerty".toCharArray()); // Use your keystore password

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagerFactory.getTrustManagers()[0])
                .build();
    }

    public void makeRequest(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            // Handle the response
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

