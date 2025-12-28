package com.example.myapplication;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ImgbbService {
    @Multipart
    @POST("1/upload")
    Call<ImgbbResponse> uploadImage(
            @Query("key") String apiKey,
            @Part MultipartBody.Part image
    );
}
