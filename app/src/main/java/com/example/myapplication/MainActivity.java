package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

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

    private static final String IMGBB_API_KEY = "76c9fac5d06abfb0d4990bdf6c25acd2"; // <-- Replace with your API key
    private static final String BASE_URL = "https://api.imgbb.com/";
    private static final String PREFS_NAME = "ImagePrefs";
    private static final String KEY_IMAGE_COUNT = "imageCount";

    private ImageView imageView;
    private Button selectImageButton;
    private Button uploadImageButton;
    private Button showSavedImagesButton;

    private Uri selectedImageUri;
    private ImgbbService imgbbService;
    private SharedPreferences sharedPreferences;
    private int imageCounter;

    // Launcher to get the result from the image gallery
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imageView.setImageURI(selectedImageUri);
                }
            });

    // Launcher to get the result from the ImageListActivity
    private final ActivityResultLauncher<Intent> imageListLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String selectedImageUrl = result.getData().getStringExtra("selectedImageUrl");
                    if (selectedImageUrl != null) {
                        downloadImage(selectedImageUrl);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        showSavedImagesButton = findViewById(R.id.showSavedImagesButton);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        imageCounter = sharedPreferences.getInt(KEY_IMAGE_COUNT, 0);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        imgbbService = retrofit.create(ImgbbService.class);

        selectImageButton.setOnClickListener(v -> openGallery());

        uploadImageButton.setOnClickListener(v -> uploadImage());

        showSavedImagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageListActivity.class);
            imageListLauncher.launch(intent);
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void downloadImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // Optional
                    .error(R.drawable.ic_launcher_foreground) // Optional
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Image downloaded successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(MainActivity.this, "Error downloading image", Toast.LENGTH_SHORT).show();
                            Log.e("PICASSO_ERROR", "Error: ", e);
                        }
                    });
        } else {
            Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show();
        }
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

                        // Save the image URL to SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("image_" + imageCounter, imageUrl);
                        imageCounter++;
                        editor.putInt(KEY_IMAGE_COUNT, imageCounter);
                        editor.apply();

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
