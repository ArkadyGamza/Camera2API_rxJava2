package com.example.arkadygamza.rxcamera2;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Very short version of presenter, mandatory methods so presenter knows when to start and stop
 */
public interface PresenterShortLifecycle {

    void onCreate(@Nullable Bundle saveState);

    void onDestroy();
}
