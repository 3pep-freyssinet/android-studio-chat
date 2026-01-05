package com.google.amara.chattab;

import android.content.Context;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

public class SSLUtil {

    public static SSLContext getSSLContext(Context context) throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream keyStoreStream = context.getResources().openRawResource(R.raw.server);
        keyStore.load(keyStoreStream, "azerty".toCharArray());

        // Initialize TrustManagerFactory
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
}

