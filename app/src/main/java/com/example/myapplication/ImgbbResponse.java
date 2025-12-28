package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class ImgbbResponse {
    @SerializedName("data")
    public Data data;

    public static class Data {
        @SerializedName("url")
        public String url;
    }
}
