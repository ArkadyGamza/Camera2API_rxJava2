package camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.arkadygamza.playwithcamera2.R;

import java.io.File;
import java.io.IOException;


public class CustomCamera2Activity extends AppCompatActivity {

    private static final String TAG = CustomCamera2Activity.class.getName();

    private CameraController mRxCameraController21;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_camera_activity);
        File outputDir = getCacheDir(); // context being the Activity pointer
        File outputFile = null;
        try {
            outputFile = File.createTempFile("prefix", "extension", outputDir);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.customCameraActivity_takePhoto).setOnClickListener(view -> mRxCameraController21.takePhoto());
        findViewById(R.id.customCameraActivity_switchCamera).setOnClickListener(view -> mRxCameraController21.switchCamera());


        mRxCameraController21 = new CameraController(
            this,
            mRxCamerController21Callback,
            outputFile.getAbsolutePath(),
            (AutoFitTextureView) findViewById(R.id.customCameraActivity_textureView),
            Configuration.ORIENTATION_PORTRAIT);
        mRxCameraController21.getLifecycle().onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRxCameraController21.getLifecycle().onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRxCameraController21.getLifecycle().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRxCameraController21.getLifecycle().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mRxCameraController21.getLifecycle().onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRxCameraController21.getLifecycle().onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRxCameraController21.getLifecycle().onDestroy();
    }

    private final CameraController.Callback mRxCamerController21Callback = new CameraController.Callback() {
        @Override
        public void onPhotoTaken(@NonNull String photoUrl, @NonNull Integer photoSourceType) {
//            setResult(Activity.RESULT_OK);
//            finish();
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
