package com.example.arkadygamza.rxcamera2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * Extended presenter's lifecycle methods for more complex presenters
 */
public interface AndroidLifecycle extends PresenterShortLifecycle {

    void onSaveInstanceState(@NonNull Bundle outState);

    void onStart();

    void onResume();

    void onPause();

    void onStop();

    void onActivityResult(int requestCode, int resultCode, Intent data);
}
