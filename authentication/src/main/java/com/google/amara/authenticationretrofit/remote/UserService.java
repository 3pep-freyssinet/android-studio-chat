package com.google.amara.authenticationretrofit.remote;

import com.google.amara.authenticationretrofit.Book;
import com.google.amara.authenticationretrofit.model.ResObj;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Header;
import retrofit2.Call;
import retrofit2.http.Body;

import okhttp3.ResponseBody;

public interface UserService {
    //La valeur entre {} représente un paramètre et peut être accessible avec @Path du coté client et
    //@PathParam du côté serveur.

    @GET("login/{username}/{password}") // n'existe pas ou n'existe plus.
    Call login(@Path("username") String username, @Path("password") String password);

    //Check if username is already in database
    @POST("books/check/{username}")
    Call<ResponseBody> checkUser(@Path("username") String username);


    @POST("books/update/{username}:{password}")
    Call<ResponseBody> updateUser(@Path("username") String username, @Path("password") String password);

    @POST("books/registration")
    Call<ResponseBody> registerUser(@Body Book book);
    //Call<Book> registerUser(@Body Book book);

    //'Authorization' (attention à la casse) est un entête du header. C'est un dummy. 'message' est un 'message' qui accompagne le header. Il n'est pas obligatoire.
    @POST("books/authorization/{message}")
    Call<ResponseBody> valideAutorization(@Header("Authorization") String authorization, @Path ("message") String message );
}