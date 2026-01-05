package com.google.amara.authenticationretrofit.remote;

public class ApiUtils {

    //public static final String BASE_URL = "http://192.168.20.110/demo/";
    public static final String BASE_URL = "http://localhost:8080/";     // Physical device. ça marche. Voir : C:/Users/aa/Accessing localhost of PC from USB connected Android mobile device.html
    //public static final String BASE_URL = "http://10.0.2.2:8080/";    // Emulator

    public static UserService getUserService(){
        return RetrofitClient.getClient(BASE_URL).create(UserService.class);
    }
}