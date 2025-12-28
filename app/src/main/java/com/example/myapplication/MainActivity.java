package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String IMGBB_API_KEY = "76c9fac5d06abfb0d4990bdf6c25acd2"; // <-- החלף במפתח ה-API שלך
    private static final String BASE_URL = "https://api.imgbb.com/";

    private ImageView imageView;
    private Button selectImageButton;
    private Button uploadImageButton;

    private Uri selectedImageUri;
    private ImgbbService imgbbService;

    // Launcher to get the result from the image gallery
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageView.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        imgbbService = retrofit.create(ImgbbService.class);

        selectImageButton.setOnClickListener(v -> openGallery());

        uploadImageButton.setOnClickListener(v -> uploadImage());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (IMGBB_API_KEY.equals("YOUR_API_KEY")) {
            Toast.makeText(this, "Please replace YOUR_API_KEY with your actual ImgBB API key", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            File tempFile = File.createTempFile("upload", ".tmp", getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            out.close();

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", tempFile.getName(), requestBody);

            Call<ImgbbResponse> call = imgbbService.uploadImage(IMGBB_API_KEY, imagePart);
            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

            call.enqueue(new Callback<ImgbbResponse>() {
                @Override
                public void onResponse(Call<ImgbbResponse> call, Response<ImgbbResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().data.url;
                        Toast.makeText(MainActivity.this, "Image Uploaded: " + imageUrl, Toast.LENGTH_LONG).show();
                        Log.d("UPLOAD_SUCCESS", "Image URL: " + imageUrl);
                    } else {
                        Toast.makeText(MainActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                        Log.e("UPLOAD_ERROR", "Response code: " + response.code());
                    }
                    tempFile.delete(); // Clean up the temp file
                }

                @Override
                public void onFailure(Call<ImgbbResponse> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Upload failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UPLOAD_FAILURE", "Error: ", t);
                    tempFile.delete(); // Clean up the temp file
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error preparing file for upload", Toast.LENGTH_SHORT).show();
            Log.e("FILE_ERROR", "Error: ", e);
        }
    }
}