package com.arkadygamza.playwithcamera2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

public class ShowPhotoActivity extends AppCompatActivity{

    private ImageView mPhotoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_photo_activity);
        mPhotoView = findViewById(R.id.showPhotoActivity_photo);
        String photoUrl = IntentHelper.getPhotoUrl(getIntent());
        Picasso.get()
            .load(new File(photoUrl))
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .placeholder(R.drawable.ic_adb_black_24dp)
            .error(R.drawable.ic_error_outline_black_24dp)
            .into(mPhotoView);
    }

    public static class IntentHelper {
        private static final String EXTRA_PHOTO_URL = "EXTRA_PHOTO_URL";

        @NonNull
        public static Intent createIntent(@NonNull Context context, @NonNull String photoUrl){
            Intent intent = new Intent(context, ShowPhotoActivity.class);
            intent.putExtra(EXTRA_PHOTO_URL, photoUrl);
            return intent;
        }

        static String getPhotoUrl(@NonNull Intent intent){
            return intent.getStringExtra(EXTRA_PHOTO_URL);
        }
    }
}
