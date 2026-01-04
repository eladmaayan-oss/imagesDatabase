package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ImageListActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ImagePrefs";
    private static final String KEY_IMAGE_COUNT = "imageCount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);

        ListView imageListView = findViewById(R.id.imageListView);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int imageCount = sharedPreferences.getInt(KEY_IMAGE_COUNT, 0);

        if (imageCount == 0) {
            Toast.makeText(this, "No saved images.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayList<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            String url = sharedPreferences.getString("image_" + i, null);
            if (url != null) {
                imageUrls.add(url);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, imageUrls);
        imageListView.setAdapter(adapter);

        imageListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUrl = imageUrls.get(position);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedImageUrl", selectedUrl);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
