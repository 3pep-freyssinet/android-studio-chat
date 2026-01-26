package com.google.amara.chattab;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

//import com.google.amara.authenticationretrofit.ChangePasswordActivity;
//import com.google.amara.authenticationretrofit.FirstTimeLoginActivity;
//import com.google.amara.authenticationretrofit.LoginActivity;

import com.google.android.material.snackbar.Snackbar;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.Handshake;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NavigatorActivity extends    AppCompatActivity
                               implements //LoginActivity.Callback,
                                          //FirstTimeLoginActivity.Callback_,
                                          //ChangePasswordActivity.Callback__,
                                          BroadcastNotification{

    private static final int REQUEST_CODE_AUTH     = 700;
    private static final int RESULT_CODE_AUTH      = 701;
    private static final int REQUEST_CODE_CHAT     = 800;
    private static final int RESULT_CODE_CHAT      = 801;
    private static final int RESULT_CODE_SETTINGS  = 901;

    public Socket socket;
    public static String NICKNAME, NICKNAME_TEMP;
    ;
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    //private long connectedAt, lastConnectedAt, disconnectedAt;
    private static final long BAN_TIME = 3600000; //1 hour
    private EditText nickname;

    private boolean display;    //used in socket.on
    private String dispatch;   //used in socket.on

    private ProgressDialog progressDialog;

    interface Bidon{
        void toto(String s);
    }

    //@Override
    protected void onCreate_(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("****************** testing SSL *********************************");
        System.out.println("Default keystore type: " + KeyStore.getDefaultType());
        System.out.println("Default keystore: " + System.getProperty("javax.net.ssl.keyStore"));

        //new Thread(this::makeRequest).start();

        //makeSecureRequest(getBaseContext());
        /*
        try {
            MyHttpClient myHttpClient = new MyHttpClient(getBaseContext());
            myHttpClient.makeRequest("https://localhost:5000");
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        try {
            // Create the custom OkHttp client
            OkHttpClient okHttpClient = createCustomOkHttpClient();

            // Set custom OkHttp client to Socket.IO options
            IO.setDefaultOkHttpWebSocketFactory((okhttp3.WebSocket.Factory) okHttpClient);
            IO.setDefaultOkHttpCallFactory((Call.Factory) okHttpClient);

            IO.Options options = new IO.Options();
            options.transports = new String[]{WebSocket.NAME};
            options.callFactory = (Call.Factory) okHttpClient;
            options.webSocketFactory = (okhttp3.WebSocket.Factory) okHttpClient;

            // Connect to the server using the secured connection
            Socket mSocket = IO.socket("https://localhost:5000", options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //ca marche
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Perform network operation here
                URL url = null; // Use 10.0.2.2 for emulator
                try {
                    //url = new URL("https://10.0.2.2:3000/");
                    url = new URL("https://localhost:5000");

                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update UI with result here
                    }
                });
            }
        }).start();
    }

    private OkHttpClient createCustomOkHttpClient() throws Exception {
        // Load the self-signed certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput   = getResources().openRawResource(R.raw.server); // Load your certificate
        Certificate ca        = cf.generateCertificate(caInput);
        caInput.close();

        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CA in our KeyStore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Get the TrustManager (assumes only one trust manager is present)
        X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Build the custom OkHttpClient using the custom SSLSocketFactory and TrustManager
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)  // Set SSL Socket Factory
                .hostnameVerifier((hostname, session) -> true)  // Disable hostname verification if needed
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        return clientBuilder.build();
    }


    private SSLContext createSSLContext() throws Exception {
        // Load the self-signed certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = getResources().openRawResource(R.raw.server); // Your certificate file in res/raw/cert.pem
        Certificate ca = cf.generateCertificate(caInput);

        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CA in our KeyStore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Create an SSLContext that uses the TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return sslContext;
    }

    public void makeSecureRequest(Context context) {
        try {
            SSLContext sslContext = SSLUtil.getSSLContext(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            URL url = new URL("https://localhost:5000");
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");

            // Add any headers if needed
            // urlConnection.setRequestProperty("Header-Name", "Header-Value");

            int responseCode = urlConnection.getResponseCode();
            // Handle the response...

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        private void makeRequest() {
            try {
                // Load your self-signed certificate
                InputStream caInput = getResources().openRawResource(R.raw.server); // Change filename if necessary
                KeyStore keyStore = KeyStore.getInstance("BKS");
                keyStore.load(null, null);
                keyStore.setCertificateEntry("localhost",
                        CertificateFactory.getInstance("X.509").generateCertificate(caInput));

                // Create a TrustManager that trusts the CAs in our KeyStore
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);

                // Build OkHttpClient with the SSL context
                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                        .build();

                // Create a request to the Node.js server
                Request request = new Request.Builder()
                        .url("https://localhost:5000") // Use 10.0.2.2 to access localhost from emulator
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println(response.body().string());
                    } else {
                        System.out.println("Request failed: " + response.code());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    //////////////////////////////////////////////////////////////////////

    private char[] keystorepass = "azerty".toCharArray(); // If your keystore has a password, put it here


    private OkHttpClient createPinnedOkHttpClient() throws Exception {
         /*
        // Load the certificate from res/raw/render_cert.pem
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        //InputStream caInput   = getResources().openRawResource(R.raw.localhost); // localhost trusted against CA
        //InputStream caInput   = getResources().openRawResource(R.raw.localhost_cert); // localhost
        InputStream caInput   = getResources().openRawResource(R.raw.render_cert); // replace with your certificate

        //for test only
        //X509Certificate certificate = (X509Certificate) cf.generateCertificate(caInput);
        //System.out.println(certificate.getSubjectDN());

        Certificate ca        = cf.generateCertificate(caInput);
        caInput.close();

        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Pin the certificate (or public key) :
        //1- generate a self-signed certificate :
        // openssl req -x509 -newkey rsa:2048 -keyout localhost.key -out localhost.crt -days 365 -nodes -subj "/CN=localhost"
        //This creates 2 files :
        // 'localhost.key' : Private key file and
        // 'localhost.crt' : Self-signed certificate file.
        //2- openssl s_client -connect localhost:5000 -showcerts  | openssl x509 -outform PEM > localhost_cert.pem
        //3- openssl x509 -in server_cert.pem -noout -pubkey | openssl pkey -pubin -outform DER | openssl dgst -sha256 -binary | openssl enc -base64

         */
        // Set up logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                //.add("localhost:5000", "sha256/OwRplNXEzYMGW5HwvDTvlbY0sx6shBCm2ed2qS19puc=") // localhost cert hash
                //.add("localhost:5000", "sha256/xxxxxxxxxxxxxxxxxxxxxxxxxxx=") // localhost cert hash

                //.add("android-chat-server.onrender.com", "sha256/BMh9IOwlOFqSEHbPfWk50LL2QZvldSZ0aTgmlWwTW7g=") // render cert hash

                //.add("android-chat-server.onrender.com", "sha256/47DEmEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=") //26-09-24

                .add("android-chat-server.onrender.com", "sha256/=") // render cert hash

                .build();


        // Create an OkHttpClient with the SSL pinning
        return new OkHttpClient.Builder()
                //.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                .certificatePinner(certificatePinner)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)  // Add the logging interceptor  // Enable logging for debugging
                .build();
    }
    public class MyAsyncTask extends AsyncTask<Void, Void, String> {
        private CountDownLatch latch;

        public MyAsyncTask(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        protected String doInBackground(Void... params) {
            // Background task
            return "Task Finished!";
        }

        @Override
        protected void onPostExecute(String result) {
            // Task completed, count down the latch
            latch.countDown();
        }
    }


    public class MyOkHttpClient {
        private OkHttpClient client;

        public MyOkHttpClient() {
            //client = new OkHttpClient.Builder()
            //        .build();

            // Set up logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            //setup the pinner here or in 'network_security_config'.
            // now, it is included here since in the 'network_security_config.' the exception of wrong pin is not catched
            CertificatePinner certificatePinner = new CertificatePinner.Builder()
                    //.add("localhost:5000", "sha256/OwRplNXEzYMGW5HwvDTvlbY0sx6shBCm2ed2qS19puc=") // localhost cert hash
                    //.add("localhost:5000", "sha256/xxxxxxxxxxxxxxxxxxxxxxxxxxx=") // localhost cert hash

                    .add("android-chat-server.onrender.com", "sha256/BMh9IOwlOFqSEHbPfWk50LL2QZvldSZ0aTgmlWwTW7g=") // render cert hash

                    //.add("android-chat-server.onrender.com", "sha256/47DEmEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=") //26-09-24

                    //.add("android-chat-server.onrender.com", "sha256/=") // render cert hash

                    .build();


            // Create an OkHttpClient with the SSL pinning
            client =  new OkHttpClient.Builder()
                    //.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .certificatePinner(certificatePinner)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)  // Add the logging interceptor  // Enable logging for debugging
                    .build();
        }

        public void makeRequest(String url) {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    if (e instanceof SSLPeerUnverifiedException) {
                        // Handle certificate pinning failure
                        System.err.println("SSL Pinning failure: " + e.getMessage());

                        //hide the progess bar
                        //exception : 'Only the original thread that created a view hierarchy can touch its views'
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.hide();
                            }
                        });

                        // Inform user or log error
                        //exception : 'Can't create handler inside thread that has not called Looper.prepare()'
                        new Thread() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                new AlertDialog.Builder(NavigatorActivity.this)
                                        .setMessage("Check security failure")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //startHandler();
                                                dialogInterface.dismiss();

                                                //exit
                                                endActivity("fail");
                                            }
                                        })
                                        .create()
                                        .show();

                                Looper.loop();
                            }
                        }.start();

                    } else {
                        // Handle other types of failures
                        System.err.println("Request failed: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Get the handshake from the response
                    Handshake handshake = response.handshake();

                    // Get the peer certificates from the handshake
                    List<Certificate> peerCertificates = handshake.peerCertificates();

                    // Assuming the first certificate is the server's certificate
                    X509Certificate x509Certificate = (X509Certificate) peerCertificates.get(0);

                    // Get the expiration date
                    Date expirationDate = x509Certificate.getNotAfter();
                    System.out.println("Certificate Expiration Date: " + expirationDate);

                    // Check if the certificate is expiring soon
                    if (isCertificateExpiringSoon(expirationDate)) {
                        System.out.println("Warning: Certificate is expiring soon!");
                    }

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    // Handle the successful response
                    //stop progress bar
                    //error : 'Only the original thread that created a view hierarchy can touch its views.'
                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          progressDialog.hide();
                                      }
                    });

                    System.out.println("Response: " + response.body().string());

                    try {
                        // Set custom OkHttp client to Socket.IO options
                        IO.setDefaultOkHttpWebSocketFactory((okhttp3.WebSocket.Factory) client);
                        IO.setDefaultOkHttpCallFactory((Call.Factory) client);

                        IO.Options options           = new IO.Options();
                        options.secure               = true; // Use SSL
                        options.reconnection         = true; // Enable reconnection in case there is a connection lost
                        options.reconnectionAttempts = 5; // Limit reconnection attempts
                        options.reconnectionDelay    = 1000; // Delay between attempts (in ms)
                        options.transports           = new String[]{WebSocket.NAME};


                        //options.callFactory      = (Call.Factory) okHttpClient;
                        //options.webSocketFactory = (okhttp3.WebSocket.Factory) okHttpClient;

                        // Connect to the server using the secured connection
                        //socket = IO.socket("https://localhost:5000", options);
                        socket = IO.socket("https://android-chat-server.onrender.com/", options);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //redirect to resume 'onConnect'
                    onConnect1();
                }
            });
        }
    }

    private boolean isCertificateExpiringSoon(Date expirationDate) {
        // Set a threshold of, for example, 30 days to consider the certificate as expiring soon
        long threshold = 30L * 24 * 60 * 60 * 1000; // 30 days in milliseconds
        long currentTime = System.currentTimeMillis();
        return expirationDate.getTime() - currentTime < threshold;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //generate jwt token
        //String jwt = Jwts.builder().claim("emailId", "test123@gmail.com").claim("emailIdOTP", "123456")
        //        .claim("phoneNo", "1111111111")
        //        .signWith(SignatureAlgorithm.HS256, "secret".getBytes())
        //        .compact();


        //with ssl
        // there are 2 ways :
        // - trustManager : call the 'createPinnedOkHttpClient()' and use it in 'IO.Options' in
        //                 'options.callFactory' and 'options.webSocketFactory' see below.
        // - network security config : setup only this file


        // Create the custom OkHttp client
        //OkHttpClient okHttpClient = createCustomOkHttpClient();//no pinning
        //OkHttpClient okHttpClient = createPinnedOkHttpClient(); //pinning

        //setup a progress bar in the event the response from the server is slow
        //Setup progress dialog
        progressDialog = new ProgressDialog(NavigatorActivity.this);
        progressDialog.setTitle("Security check");
        progressDialog.setMessage("Starting...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        //test the pinner certificate and call "onConnect1".
        MyOkHttpClient okHttpClient = new MyOkHttpClient();
        okHttpClient.makeRequest("https://android-chat-server.onrender.com");
    }

    private void onConnect1(){
        int i = 0;

        /*
        //without ssl
        try {
            IO.Options options   = new IO.Options();
            options.forceNew     = true;
            options.secure       = true;
            options.reconnection = false;

            options.query        = "token=" + jwt;

            SocketSSL.set(options);

            //socket = IO.socket("http://10.0.2.2:5000");       //, options);        //emulator
            //socket = IO.socket("http://localhost:5000");      //, options);        //device
            //socket = IO.socket("http://192.168.43.57:5000");                       //device on network
            //socket = IO.socket("https://murmuring-garden-67075.herokuapp.com/");
            socket = IO.socket("https://android-chat-server.onrender.com/");
            //socket = IO.socket("https://tomcaty.helioho.st/");

            //IO.Options options  = new IO.Options();
            //SocketSSL.set(options);

            //opts.forceNew       = true;
            //opts.reconnection   = true;
            //opts.transports     = new String[]{WebSocket.NAME}; //'Polling.NAME' or 'WebSocket.NAME'
            //opts.transports     = new String[]{WebSocket.NAME};
            //socket = IO.socket("https://murmuring-garden-67075.herokuapp.com/", options);    //Heroku do not use port=5000

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        */


        //Not used. 'The class 'SocketHandler' is defined in 'app' module. the class 'LoginActivity' where
        // the class 'SocketHandler' is needed is defined in 'authenticate' module.there is no bridge between
        // 'app' module and 'authenticate' module.
        //The workaround is interface
        //SocketHandler.setSocket(socket);

        //'MyPref' ---> 'MyPref.xml' will be found in /data/data/<package>/shared_prefs
        preferences = getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor      = preferences.edit();

        //send socket to 'SocketHandler' class
        SocketHandler.setSocket(socket);

        //send socket and preferences to 'LoginActivity' in 'authentication' module.
        //LoginActivity.setCallback(socket, this, preferences);

        //send socket and preferences to 'FirstTimeActivity' in 'authentication' module.
        //FirstTimeLoginActivity.setCallback(socket, this, preferences);

        //send socket and preferences to 'ChangePasswordActivity' in 'authentication' module.
        //ChangePasswordActivity.setCallback(socket, this, preferences);


        //listen to events sent by socket
        //socket.on(Socket.EVENT_CONNECT_ERROR, mConnectionErrorListener);
        socket.on(Socket.EVENT_CONNECT, mConnectionListener);

        //listen to events sent by the server
        setEmitterListener_get_user_ban_back();

        socket.connect();

        onConnection();

        //test the connection
        //if (socket.connected()) {
        //    onConnection();
        //}
    }

    private void onConnection(){
       //here, there is connection

       //not used
       //Set Preferences
       //PreferencesHandler.setSharedPreferences(preferences);

       //Set time of connection
       //connectedAt     = new Date().getTime();
       //lastConnectedAt = 0;
       //disconnectedAt  = 0;

       // get ban info from preferences
       long startBanTime = preferences.getLong("start_Ban_Time", 0L);
       if (startBanTime != 0) {

           long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
           if (remainingTime >= 0L) {
               //the ban is not ended, notify the user and exit.
               banish(startBanTime);
               return;
           } else {
               //the ban is ended. Remove info from the Preferences and login again
               editor.remove("start_Ban_Time");
               editor.commit();

               //Get 'Remember me' from 'Preferences'
               boolean rememberMe = preferences.getBoolean("remember_me", false);
               if(rememberMe){
                   //redirect to 'MainActivity'.Go to the 'MainActivity' in 'app' module.
                   Intent intent = new Intent(NavigatorActivity.this, MainActivity.class);
                   intent.putExtra("NICKNAME", NICKNAME);
                   startActivityForResult(intent, REQUEST_CODE_CHAT);
               }else{
                   //do login
                   //Intent intent = new Intent(this, com.google.amara.authenticationretrofit.LoginActivity.class);
                   //intent.putExtra("authentication", "authentication"); //not used, for illustration
                   //startActivityForResult(intent, REQUEST_CODE_AUTH);
               }
               return;
           }
       }

       //Here, 'startBanTime == 0', the user is not banished. He is allowed to chat.

       //get data from 'Preferences'.
       NICKNAME      = preferences.getString("NICKNAME", null);
       NICKNAME_TEMP = NICKNAME; //copy of 'NICKNAME'

       //nickname.setEnabled(false); //test mode 'true', in production mode 'false'

       if (null != NICKNAME) {
           //Get 'Remember me' from 'Preferences'
           boolean rememberMe = preferences.getBoolean("remember_me", false);
           if(rememberMe){
               //redirect to 'MainActivity' in 'app' module.
               Intent intent = new Intent(NavigatorActivity.this, MainActivity.class);
               intent.putExtra("NICKNAME", NICKNAME);
               intent.putExtra("origin", "navigator");
               startActivityForResult(intent, REQUEST_CODE_CHAT);
           }else{
               //do login
               //Intent intent = new Intent(this, com.google.amara.authenticationretrofit.LoginActivity.class);
               //intent.putExtra("authentication", "authentication"); //not used, for illustration
               //startActivityForResult(intent, REQUEST_CODE_AUTH);
           }

           //Fill the editText with the 'NICKNAME' found in preferences and disable the edt 'nickname'
           //nickname.setText(NICKNAME);

           nextOncreate();

           //get not seen messages and display the authors of theses messages if any
           //display = true;
           //dispatch = null; //"next_on_create";
           //socket.emit("get_all_not_seen_messages", NICKNAME);// the result will be found in socket.on('get_all_not_seen_messages_res', result)

       } else {
           //the 'NICKNAME' is not found in preferences. It is the first time or the account has been deleted.
           //If it is deleted, perhaps the user has been banished.
           //Get ban info from the server. The response is found just below in
           // 'setEmitterListener_get_user_ban_back()'
           socket.emit("get_ban_info", getPackageName());//the result will be found in 'setEmitterListener_get_user_ban_back'
       }
   }

   private void nextOncreate() { }

    //get ban info from the server
    private void setEmitterListener_get_user_ban_back() {
        socket.on("get_ban_info_back", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONArray ban_ = ((JSONArray) args[0]);
                        if (ban_ == null) {
                            //do login
                            //Intent intent = new Intent(NavigatorActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            return;
                        }

                        String startBanTime = null;
                        String packageId = null;
                        try {
                            JSONObject ban = (JSONObject) ban_.get(0);
                            packageId      = ban.isNull("packageid") ? null : ban.getString("packageid");
                            startBanTime   = ban.isNull("startbantime") ? null : ban.getString("startbantime");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (packageId == null) {
                            //do login
                            //Intent intent = new Intent(NavigatorActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            return;
                        }

                        //here there is packageId, show the user o dialog and exit.
                        long remainingTime = BAN_TIME - (new Date().getTime() - Long.parseLong(startBanTime));
                        if (remainingTime >= 0L) {
                            //the ban is not ended, notify the user and exit.
                            banish(Long.parseLong(startBanTime));
                            return;
                        } else {
                            //the ban is ended. Remove info from the Preferences and login again
                            editor.remove("startBanTime");
                            editor.commit();

                            //login
                            //Intent intent = new Intent(NavigatorActivity.this, com.google.amara.authenticationretrofit.LoginActivity.class);
                            //intent.putExtra("authentication", "authentication"); //not used, for illustration
                            //startActivityForResult(intent, REQUEST_CODE_AUTH);
                            return;
                        }
                    }
                });
            }
        });
    }

    /**
     * Called when a ban occur.
     *
     * @param startBanTime the time when the ban starts. It is set after 3 unsuccessful attempts to login.
     */
    private void banish(long startBanTime) {
        //Notify the user how many time it remains to have free access.
        long remainingTime = BAN_TIME - (new Date().getTime() - startBanTime);
        long minutes = remainingTime / 60000;
        long secondes = (remainingTime % 60000);
        int secondes_ = (int) (secondes / 1000);

        new AlertDialog.Builder(this).
                setMessage("It remains : " + minutes + " minutes et " + secondes_ + " secondes to log").
                setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        endActivity("fail");
                    }
                }).create().show();
        return;
    }

    //we come here when 'LoginActivity' send the name of user who authenticate successfully.
    //@Override
    public void authenticateUser(String username) {
        //put the username in preferences
        editor.putString("NICKNAME",username );
        editor.commit();

        //go to the 'MainActivity' in 'app' module.
        Intent intent = new Intent(NavigatorActivity.this, MainActivity.class);
        intent.putExtra("NICKNAME", username);
        startActivityForResult(intent, REQUEST_CODE_CHAT);
        return;
    }

    //we come here when  'FirstTimeActivity' register successfully a user.
    //redirect user to chat
    //@Override
    public void registerUser(String username) {

        //put the user name in preferences
        editor.putString("NICKNAME", username);
        editor.commit();

        //go to the 'MainActivity' in 'app' module.
        Intent intent = new Intent(NavigatorActivity.this, MainActivity.class);
        intent.putExtra("NICKNAME", username);
        startActivityForResult(intent, REQUEST_CODE_CHAT);
        return;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;

        String status = data.getStringExtra("status");
        int i = 0;
        //Authentication, we come from 'LoginActivity' in 'Authentication' module
        if (requestCode == REQUEST_CODE_AUTH &&   // send code
                resultCode == RESULT_CODE_AUTH && // receive code
                null != data) {                   //data sent from 'LoginActivity' in 'authentication' module.
                                                  // or
                                                  // 'FirstTimeLoginActivity' in 'authentication' module.
            //redirect to chat home page only in 'success' case.
            if(data.getStringExtra("status").equals("success")){
                String NICKNAME = data.getStringExtra("NICKNAME");
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("NICKNAME", NICKNAME);
                startActivityForResult(intent, REQUEST_CODE_CHAT);
                return;
            }
            //here the 'status'='fail'
            //go back to the 'onActivityResult' of 'Settings' activity witch has called this activity.
            endActivity(status);
        }

        //Chat, we come from 'MainActivity' in 'app' module. The intent is started above in 'registerUser'.
        if (requestCode == REQUEST_CODE_CHAT &&   // send code
                resultCode == RESULT_CODE_CHAT && // receive code
                null != data) {//data sent from 'LoginActivity' in 'authentication' mudule
               //go back to the 'onActivityResult' of 'Settings' activity witch has called this activity.
            endActivity(status);
            //finish();

            //String status = data.getStringExtra("status");
            //if (status.equals("fail")) super.finish(); //end the app
        }
    }

    private void endActivity(String status) {
        // Prepare data intent
        Intent intent = new Intent();
        intent.putExtra("status", status);

        // Activity finished ok, return the data to 'onActivityResult' of 'Settings' in 'App' module.
        setResult(RESULT_CODE_SETTINGS, intent); //the data are returned to 'onActivityResult' of 'Settings' in 'App' module which launched this intent.
        finish();//obligatoire
    }

    private void endChatActivity(String status){
        // chat Activity finished, return the data to 'onActivityResult' of 'Settings' in 'App' module.
    }


    Emitter.Listener mConnectionErrorListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Error connection, notify the user
                    AlertDialog.Builder builder = new AlertDialog.Builder(NavigatorActivity.this);
                    builder.setTitle("Connection error");
                    builder.setMessage("No connection with the server");

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            socket.close();
                            dialog.dismiss();
                            finish();
                            return;
                        }
                    });

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    //.setNegativeButton(android.R.string.no, null)
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    //.show();

                    AlertDialog alert = builder.create();
                    //alert.show();
                    //"has leaked window DecorView@71fac27[] that was originally added here" exception

                }
            });
        }
    };
    Emitter.Listener mConnectionListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(NavigatorActivity.this.findViewById(android.R.id.content), "Connection", Snackbar.LENGTH_LONG).show();

                    //there is connection
                    //onConnection();
                }
            });
        }
    };

    Emitter.Listener mDisconnectionListener =  new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(NavigatorActivity.this.findViewById(android.R.id.content), "Disconnect", Snackbar.LENGTH_LONG).show();

                    //Get time disconnect and save it in prefrences
                    long disconnectionTime = new Date().getTime();
                    //Todo : the 'disconnectionTime' perhaps it is not equal to the time stored in the server in the event 'socket.on('disconnect''

                    //Save in 'preferences' the 'disconnectionTime'.
                    MainActivity.editor.putLong("disconnected_at", disconnectionTime);
                    MainActivity.editor.commit();

                    socket.close();
                    finish();
                    return;
                }
            });
        }
    };

    //the broadcast has sent network status.
    @Override
    public void sendNetworkNotification(String netWorkStatus) {

        View view = getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(view, "Status MainActivity = " + netWorkStatus, Snackbar.LENGTH_LONG).show();
        //or
        Snackbar.make(findViewById(android.R.id.content), "Status MainActivity = " + netWorkStatus, Snackbar.LENGTH_LONG).show();

        //enable 'btn' to enter the chat only if there is connection.
        switch (netWorkStatus) {
            case "Wifi enabled":
            case "Mobile data enabled":
                //onConnection();
                break;
            case "No internet is available":
                //btn.setEnabled(false);

                //Get list of fragments
                //List<Fragment> fragments = getSupportFragmentManager().getFragments();

                //Notify the user
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setMessage("No Internet connection");
                alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                }).create().show();

                break;
        }
    }

    //The pwd has been reset. Redirect the user to chat
    //@Override
    public void resetPwd(String username) {

        //put the user name in preferences
        editor.putString("NICKNAME", username);
        editor.commit();

        //go to the 'MainActivity' in 'app' module.
        Intent intent = new Intent(NavigatorActivity.this, MainActivity.class);
        intent.putExtra("NICKNAME", username);
        startActivityForResult(intent, REQUEST_CODE_CHAT);
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}

