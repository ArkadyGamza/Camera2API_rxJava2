package com.arkadygamza.playwithcamera2;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.arkadygamza.rxcamera2.CameraController;
import com.example.arkadygamza.rxcamera2.OpenCameraException;

import java.io.File;
import java.io.IOException;



public class CustomCamera2Activity extends AppCompatActivity {

    private static final String TAG = CustomCamera2Activity.class.getName();

    private CameraController mRxCameraController21;
    private View mFocusIndicator;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_camera_activity);
        File outputDir = getCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try {
            outputFile = File.createTempFile("prefix", ".jpg", outputDir);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.customCameraActivity_takePhoto).setOnClickListener(view -> mRxCameraController21.takePhoto());
        findViewById(R.id.customCameraActivity_switchCamera).setOnClickListener(view -> mRxCameraController21.switchCamera());
        mFocusIndicator = findViewById(R.id.customCameraActivity_focusIndicator);

        mRxCameraController21 = new CameraController(
            this,
            mRxCamerController21Callback,
            outputFile.getAbsolutePath(),
            findViewById(R.id.customCameraActivity_textureView),
            Configuration.ORIENTATION_PORTRAIT, this.getLifecycle());
    }

    private final CameraController.Callback mRxCamerController21Callback = new CameraController.Callback() {
        @Override
        public void onFocusStarted() {
            mFocusIndicator.setVisibility(View.VISIBLE);
            mFocusIndicator.setScaleX(1f);
            mFocusIndicator.setScaleY(1f);
            mFocusIndicator.animate()
                .scaleX(2f)
                .scaleY(2f)
                .setDuration(500)
                .start();
        }

        @Override
        public void onFocusFinished() {
            mFocusIndicator.setVisibility(View.GONE);
        }

        @Override
        public void onPhotoTaken(@NonNull String photoUrl, @NonNull Integer photoSourceType) {
            Intent intent = ShowPhotoActivity.IntentHelper.createIntent(CustomCamera2Activity.this, photoUrl);
            startActivity(intent);
        }

        @Override
        public void onCameraAccessException() {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        @Override
        public void onCameraOpenException(@Nullable OpenCameraException.Reason reason) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        @Override
        public void onException(Throwable throwable) {
            throwable.printStackTrace();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    };

}
